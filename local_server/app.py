from __future__ import annotations

import json
import os
import secrets
import sqlite3
from datetime import UTC, datetime, timedelta
from functools import wraps
from pathlib import Path
from typing import Any
from urllib.parse import urlparse

from authlib.integrations.flask_client import OAuth
from flask import Flask, jsonify, redirect, render_template, request, session, url_for


BASE_DIR = Path(__file__).resolve().parent
PROJECT_DIR = BASE_DIR.parent
DATA_DIR = BASE_DIR / "data"
DB_PATH = DATA_DIR / "easy2.db"
SESSION_SECRET_PATH = DATA_DIR / "server_secret.txt"

GOOGLE_DISCOVERY_URL = "https://accounts.google.com/.well-known/openid-configuration"
ADMIN_SESSION_KEY = "admin_user_id"
LOGIN_NEXT_KEY = "admin_login_next"
LOGIN_NOTICE_KEY = "admin_login_notice"
DEFAULT_LOGIN_NEXT = "/admin"


def utc_now_iso() -> str:
    return datetime.now(UTC).isoformat()


def load_or_create_secret_key() -> str:
    env_secret = os.environ.get("EASY2_SERVER_SECRET_KEY", "").strip()
    if env_secret:
        return env_secret

    DATA_DIR.mkdir(parents=True, exist_ok=True)
    if SESSION_SECRET_PATH.exists():
        existing_secret = SESSION_SECRET_PATH.read_text(encoding="utf-8").strip()
        if existing_secret:
            return existing_secret

    generated_secret = secrets.token_hex(32)
    SESSION_SECRET_PATH.write_text(generated_secret, encoding="utf-8")
    return generated_secret


def load_google_oauth_credentials() -> dict[str, str]:
    client_id = os.environ.get("EASY2_GOOGLE_CLIENT_ID", "").strip()
    client_secret = os.environ.get("EASY2_GOOGLE_CLIENT_SECRET", "").strip()
    json_path = os.environ.get("EASY2_GOOGLE_CLIENT_JSON", "").strip()

    if client_id and client_secret:
        return {
            "client_id": client_id,
            "client_secret": client_secret,
            "json_path": "",
        }

    candidate_paths: list[Path] = []
    if json_path:
        candidate_paths.append(Path(json_path).expanduser())
    else:
        candidate_paths.extend(sorted(PROJECT_DIR.glob("client_secret_*.json")))
        candidate_paths.extend(sorted(BASE_DIR.glob("client_secret_*.json")))

    seen_paths: set[Path] = set()
    for candidate_path in candidate_paths:
        resolved_path = candidate_path.resolve()
        if resolved_path in seen_paths or not resolved_path.is_file():
            continue
        seen_paths.add(resolved_path)

        try:
            payload = json.loads(resolved_path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            continue

        web_payload = payload.get("web")
        if not isinstance(web_payload, dict):
            continue

        json_client_id = str(web_payload.get("client_id", "")).strip()
        json_client_secret = str(web_payload.get("client_secret", "")).strip()
        redirect_uris = web_payload.get("redirect_uris")
        json_redirect_uri = ""
        if isinstance(redirect_uris, list) and redirect_uris:
            json_redirect_uri = str(redirect_uris[0]).strip()
        if not json_client_id or not json_client_secret:
            continue

        return {
            "client_id": client_id or json_client_id,
            "client_secret": client_secret or json_client_secret,
            "json_path": str(resolved_path),
            "redirect_uri": json_redirect_uri,
        }

    return {
        "client_id": client_id,
        "client_secret": client_secret,
        "json_path": "",
        "redirect_uri": "",
    }


app = Flask(__name__)
google_oauth_credentials = load_google_oauth_credentials()
configured_public_base_url = os.environ.get("EASY2_PUBLIC_BASE_URL", "").strip()
configured_redirect_uri = google_oauth_credentials.get("redirect_uri", "").strip()
derived_public_base_url = ""
if not configured_public_base_url and configured_redirect_uri:
    parsed_redirect_uri = urlparse(configured_redirect_uri)
    if parsed_redirect_uri.scheme and parsed_redirect_uri.netloc:
        derived_public_base_url = (
            f"{parsed_redirect_uri.scheme}://{parsed_redirect_uri.netloc}"
        )
app.secret_key = load_or_create_secret_key()
app.config.update(
    SESSION_COOKIE_NAME="easy2_admin_session",
    SESSION_COOKIE_HTTPONLY=True,
    SESSION_COOKIE_SAMESITE="Lax",
    PERMANENT_SESSION_LIFETIME=timedelta(days=14),
    GOOGLE_CLIENT_ID=google_oauth_credentials["client_id"],
    GOOGLE_CLIENT_SECRET=google_oauth_credentials["client_secret"],
    GOOGLE_CLIENT_JSON_PATH=google_oauth_credentials["json_path"],
    GOOGLE_REDIRECT_URI=configured_redirect_uri,
    PUBLIC_BASE_URL=configured_public_base_url or derived_public_base_url,
)

oauth = OAuth(app)
GOOGLE_OAUTH_ENABLED = bool(
    app.config["GOOGLE_CLIENT_ID"] and app.config["GOOGLE_CLIENT_SECRET"]
)
if GOOGLE_OAUTH_ENABLED:
    oauth.register(
        name="google",
        server_metadata_url=GOOGLE_DISCOVERY_URL,
        client_kwargs={"scope": "openid email profile"},
    )


def ensure_database() -> None:
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    with sqlite3.connect(DB_PATH) as connection:
        connection.execute(
            """
            CREATE TABLE IF NOT EXISTS clients (
                id TEXT PRIMARY KEY,
                auth_token TEXT NOT NULL UNIQUE,
                display_name TEXT NOT NULL,
                device_model TEXT NOT NULL,
                age TEXT DEFAULT '',
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                last_seen_at TEXT,
                last_latitude REAL,
                last_longitude REAL,
                last_accuracy REAL,
                last_provider TEXT,
                last_battery_percent INTEGER,
                last_is_charging INTEGER DEFAULT 0
            )
            """
        )
        connection.execute(
            """
            CREATE TABLE IF NOT EXISTS locations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                client_id TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                accuracy REAL,
                provider TEXT,
                battery_percent INTEGER,
                is_charging INTEGER DEFAULT 0,
                recorded_at TEXT NOT NULL,
                FOREIGN KEY(client_id) REFERENCES clients(id) ON DELETE CASCADE
            )
            """
        )
        connection.execute(
            """
            CREATE TABLE IF NOT EXISTS admin_users (
                id TEXT PRIMARY KEY,
                google_sub TEXT NOT NULL UNIQUE,
                email TEXT NOT NULL UNIQUE,
                full_name TEXT NOT NULL,
                picture_url TEXT DEFAULT '',
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                last_login_at TEXT NOT NULL,
                is_active INTEGER DEFAULT 1
            )
            """
        )
        connection.execute(
            """
            CREATE INDEX IF NOT EXISTS idx_locations_client_recorded_at
            ON locations(client_id, recorded_at DESC)
            """
        )
        connection.execute(
            """
            CREATE INDEX IF NOT EXISTS idx_admin_users_email
            ON admin_users(email)
            """
        )
        connection.commit()


def get_connection() -> sqlite3.Connection:
    connection = sqlite3.connect(DB_PATH)
    connection.row_factory = sqlite3.Row
    connection.execute("PRAGMA foreign_keys = ON")
    return connection


def parse_iso_datetime(value: str | None) -> datetime | None:
    if not value:
        return None

    try:
        parsed = datetime.fromisoformat(value)
    except ValueError:
        return None

    if parsed.tzinfo is None:
        return parsed.replace(tzinfo=UTC)
    return parsed.astimezone(UTC)


def parse_csv_env(name: str) -> set[str]:
    raw_value = os.environ.get(name, "")
    return {item.strip().lower() for item in raw_value.split(",") if item.strip()}


def is_true_env(name: str, default: bool = False) -> bool:
    raw_value = os.environ.get(name, "").strip().lower()
    if not raw_value:
        return default
    return raw_value in {"1", "true", "yes", "on"}


def normalize_email(value: str | None) -> str:
    return (value or "").strip().lower()


def sanitize_base_url(value: str | None) -> str:
    normalized = (value or "").strip()
    if not normalized:
        return ""

    while normalized.endswith("/"):
        normalized = normalized[:-1]
    return normalized


def external_url_for(endpoint: str, **values: Any) -> str:
    relative_path = url_for(endpoint, _external=False, **values)
    public_base_url = sanitize_base_url(app.config.get("PUBLIC_BASE_URL", ""))
    if public_base_url:
        return public_base_url + relative_path
    return url_for(endpoint, _external=True, **values)


def current_request_target() -> str:
    query_string = request.query_string.decode("utf-8", errors="ignore").strip()
    if query_string:
        return sanitize_next_value(f"{request.path}?{query_string}")
    return sanitize_next_value(request.path)


def current_request_path_with_query() -> str:
    query_string = request.query_string.decode("utf-8", errors="ignore").strip()
    if query_string:
        return f"{request.path}?{query_string}"
    return request.path


def sanitize_next_value(value: str | None) -> str:
    candidate = (value or "").strip()
    if not candidate:
        return DEFAULT_LOGIN_NEXT

    parsed = urlparse(candidate)
    if parsed.scheme or parsed.netloc:
        return DEFAULT_LOGIN_NEXT
    if not candidate.startswith("/") or candidate.startswith("//"):
        return DEFAULT_LOGIN_NEXT
    return candidate


def build_signup_policy_summary() -> str:
    if not is_true_env("EASY2_GOOGLE_AUTO_CREATE", default=True):
        return "El alta automática con Google está desactivada."

    allowed_emails = sorted(parse_csv_env("EASY2_ADMIN_ALLOWED_EMAILS"))
    allowed_domains = sorted(parse_csv_env("EASY2_ADMIN_ALLOWED_DOMAINS"))

    if not allowed_emails and not allowed_domains:
        return "Cualquier cuenta de Google con email verificado puede crear cuenta."

    parts: list[str] = []
    if allowed_domains:
        parts.append("dominios permitidos: " + ", ".join(allowed_domains))
    if allowed_emails:
        parts.append("emails permitidos: " + ", ".join(allowed_emails))
    return "Alta automática limitada a " + " | ".join(parts) + "."


def can_auto_create_admin(email: str) -> bool:
    if not is_true_env("EASY2_GOOGLE_AUTO_CREATE", default=True):
        return False

    normalized_email = normalize_email(email)
    if not normalized_email:
        return False

    allowed_emails = parse_csv_env("EASY2_ADMIN_ALLOWED_EMAILS")
    allowed_domains = parse_csv_env("EASY2_ADMIN_ALLOWED_DOMAINS")
    if not allowed_emails and not allowed_domains:
        return True

    domain = normalized_email.split("@", 1)[1] if "@" in normalized_email else ""
    return normalized_email in allowed_emails or domain in allowed_domains


def describe_google_oauth_source() -> str:
    json_path = str(app.config.get("GOOGLE_CLIENT_JSON_PATH", "")).strip()
    if json_path:
        return f"JSON detectado automaticamente: {Path(json_path).name}"

    if app.config.get("GOOGLE_CLIENT_ID") and app.config.get("GOOGLE_CLIENT_SECRET"):
        return "variables EASY2_GOOGLE_CLIENT_ID / EASY2_GOOGLE_CLIENT_SECRET"

    return ""


def admin_row_to_dict(row: sqlite3.Row | None) -> dict[str, Any] | None:
    if row is None:
        return None

    return {
        "id": row["id"],
        "google_sub": row["google_sub"],
        "email": row["email"],
        "full_name": row["full_name"],
        "picture_url": row["picture_url"] or "",
        "created_at": row["created_at"],
        "updated_at": row["updated_at"],
        "last_login_at": row["last_login_at"],
        "is_active": bool(row["is_active"]),
    }


def count_admin_users() -> int:
    ensure_database()
    with get_connection() as connection:
        return connection.execute("SELECT COUNT(*) FROM admin_users").fetchone()[0]


def get_current_admin() -> dict[str, Any] | None:
    admin_user_id = str(session.get(ADMIN_SESSION_KEY, "")).strip()
    if not admin_user_id:
        return None

    ensure_database()
    with get_connection() as connection:
        row = connection.execute(
            """
            SELECT *
            FROM admin_users
            WHERE id = ? AND is_active = 1
            """,
            (admin_user_id,),
        ).fetchone()

    if row is None:
        session.pop(ADMIN_SESSION_KEY, None)
        return None

    return admin_row_to_dict(row)


def set_login_notice(message: str, is_error: bool = False) -> None:
    session[LOGIN_NOTICE_KEY] = {
        "text": message,
        "type": "error" if is_error else "info",
    }


def pop_login_notice() -> dict[str, str] | None:
    notice = session.pop(LOGIN_NOTICE_KEY, None)
    if not isinstance(notice, dict):
        return None
    text = str(notice.get("text", "")).strip()
    notice_type = str(notice.get("type", "info")).strip() or "info"
    if not text:
        return None
    return {"text": text, "type": notice_type}


def get_google_client():
    if not GOOGLE_OAUTH_ENABLED:
        return None
    return oauth.create_client("google")


def client_row_to_dict(row: sqlite3.Row) -> dict[str, Any]:
    last_seen_at = row["last_seen_at"]
    last_seen_dt = parse_iso_datetime(last_seen_at)

    is_online = False
    if last_seen_dt is not None:
        is_online = (datetime.now(UTC) - last_seen_dt).total_seconds() <= 45

    return {
        "id": row["id"],
        "display_name": row["display_name"],
        "device_model": row["device_model"],
        "age": row["age"] or "",
        "created_at": row["created_at"],
        "updated_at": row["updated_at"],
        "last_seen_at": row["last_seen_at"],
        "last_latitude": row["last_latitude"],
        "last_longitude": row["last_longitude"],
        "last_accuracy": row["last_accuracy"],
        "last_provider": row["last_provider"] or "",
        "last_battery_percent": row["last_battery_percent"],
        "last_is_charging": bool(row["last_is_charging"]),
        "is_online": is_online,
    }


def location_row_to_dict(row: sqlite3.Row) -> dict[str, Any]:
    return {
        "id": row["id"],
        "client_id": row["client_id"] if "client_id" in row.keys() else "",
        "display_name": row["display_name"] if "display_name" in row.keys() else "",
        "device_model": row["device_model"] if "device_model" in row.keys() else "",
        "latitude": row["latitude"],
        "longitude": row["longitude"],
        "accuracy": row["accuracy"],
        "provider": row["provider"] or "",
        "battery_percent": row["battery_percent"],
        "is_charging": bool(row["is_charging"]),
        "recorded_at": row["recorded_at"],
    }


def require_json() -> dict[str, Any]:
    payload = request.get_json(silent=True)
    if not isinstance(payload, dict):
        raise ValueError("El cuerpo JSON es obligatorio.")
    return payload


def admin_session_required(view):
    @wraps(view)
    def wrapped_view(*args: Any, **kwargs: Any) -> Any:
        admin_user = get_current_admin()
        if admin_user is not None:
            return view(*args, **kwargs)

        if request.path.startswith("/api/"):
            return jsonify({"error": "Necesitas iniciar sesión como administrador."}), 401

        return redirect(url_for("login", next=current_request_target()))

    return wrapped_view


@app.before_request
def redirect_localhost_alias_to_public_base() -> Any | None:
    public_base_url = sanitize_base_url(app.config.get("PUBLIC_BASE_URL", ""))
    if not public_base_url or request.method not in {"GET", "HEAD"}:
        return None
    if request.path.startswith("/api/"):
        return None

    public_base = urlparse(public_base_url)
    request_hostname = (request.host.split(":", 1)[0] or "").strip().lower()
    if public_base.hostname != "localhost" or request_hostname != "127.0.0.1":
        return None

    return redirect(public_base_url + current_request_path_with_query())


@app.route("/")
def index() -> Any:
    return redirect(url_for("admin_dashboard"))


@app.get("/login")
def login() -> Any:
    if get_current_admin() is not None:
        return redirect(sanitize_next_value(request.args.get("next")))

    next_value = sanitize_next_value(
        request.args.get("next") or session.get(LOGIN_NEXT_KEY)
    )
    session[LOGIN_NEXT_KEY] = next_value

    return render_template(
        "login.html",
        embedded=request.args.get("embedded") == "1" or "embedded=1" in next_value,
        next_path=next_value,
        oauth_enabled=GOOGLE_OAUTH_ENABLED,
        oauth_source=describe_google_oauth_source(),
        notice=pop_login_notice(),
        signup_policy_summary=build_signup_policy_summary(),
        admin_count=count_admin_users(),
    )


@app.get("/auth/google")
def auth_google() -> Any:
    if not GOOGLE_OAUTH_ENABLED:
        set_login_notice(
            "Faltan EASY2_GOOGLE_CLIENT_ID y EASY2_GOOGLE_CLIENT_SECRET en el servidor.",
            is_error=True,
        )
        return redirect(url_for("login"))

    next_value = sanitize_next_value(
        request.args.get("next") or session.get(LOGIN_NEXT_KEY)
    )
    session[LOGIN_NEXT_KEY] = next_value

    google_client = get_google_client()
    if google_client is None:
        set_login_notice("Google OAuth no está disponible en este arranque.", is_error=True)
        return redirect(url_for("login"))

    return google_client.authorize_redirect(external_url_for("auth_google_callback"))


@app.get("/auth/google/callback")
def auth_google_callback() -> Any:
    if not GOOGLE_OAUTH_ENABLED:
        set_login_notice(
            "Google OAuth no está configurado en este servidor.",
            is_error=True,
        )
        return redirect(url_for("login"))

    google_client = get_google_client()
    if google_client is None:
        set_login_notice("Google OAuth no está disponible en este arranque.", is_error=True)
        return redirect(url_for("login"))

    try:
        token = google_client.authorize_access_token()
    except Exception:
        set_login_notice("No se pudo completar el acceso con Google.", is_error=True)
        return redirect(url_for("login"))

    user_info = token.get("userinfo")
    if not isinstance(user_info, dict):
        try:
            user_info = google_client.get("userinfo").json()
        except Exception:
            user_info = {}

    google_sub = str(user_info.get("sub", "")).strip()
    email = normalize_email(user_info.get("email"))
    full_name = str(user_info.get("name", "")).strip() or email or "Administrador"
    picture_url = str(user_info.get("picture", "")).strip()
    email_verified = bool(user_info.get("email_verified"))

    if not google_sub or not email or not email_verified:
        set_login_notice(
            "Google no devolvió un email verificado para esta cuenta.",
            is_error=True,
        )
        return redirect(url_for("login"))

    now = utc_now_iso()
    ensure_database()
    with get_connection() as connection:
        admin_row = connection.execute(
            """
            SELECT *
            FROM admin_users
            WHERE google_sub = ? OR lower(email) = ?
            ORDER BY CASE WHEN google_sub = ? THEN 0 ELSE 1 END
            LIMIT 1
            """,
            (google_sub, email, google_sub),
        ).fetchone()

        if admin_row is None:
            if not can_auto_create_admin(email):
                set_login_notice(
                    "Tu cuenta de Google no está autorizada para crear usuario administrador.",
                    is_error=True,
                )
                return redirect(url_for("login"))

            admin_user_id = f"adm_{secrets.token_hex(4)}"
            connection.execute(
                """
                INSERT INTO admin_users (
                    id,
                    google_sub,
                    email,
                    full_name,
                    picture_url,
                    created_at,
                    updated_at,
                    last_login_at,
                    is_active
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1)
                """,
                (
                    admin_user_id,
                    google_sub,
                    email,
                    full_name,
                    picture_url,
                    now,
                    now,
                    now,
                ),
            )
            connection.commit()
        else:
            if not bool(admin_row["is_active"]):
                set_login_notice(
                    "Tu cuenta de administrador está desactivada.",
                    is_error=True,
                )
                return redirect(url_for("login"))

            admin_user_id = admin_row["id"]
            connection.execute(
                """
                UPDATE admin_users
                SET google_sub = ?,
                    email = ?,
                    full_name = ?,
                    picture_url = ?,
                    updated_at = ?,
                    last_login_at = ?
                WHERE id = ?
                """,
                (
                    google_sub,
                    email,
                    full_name,
                    picture_url,
                    now,
                    now,
                    admin_user_id,
                ),
            )
            connection.commit()

    session[ADMIN_SESSION_KEY] = admin_user_id
    session.permanent = True
    next_path = sanitize_next_value(session.pop(LOGIN_NEXT_KEY, DEFAULT_LOGIN_NEXT))
    return redirect(next_path)


@app.get("/logout")
def logout() -> Any:
    session.pop(ADMIN_SESSION_KEY, None)
    session.pop(LOGIN_NEXT_KEY, None)
    set_login_notice("Sesión cerrada correctamente.")
    return redirect(url_for("login"))


@app.route("/admin")
@app.route("/dashboard")
@admin_session_required
def admin_dashboard() -> Any:
    return render_template(
        "dashboard.html",
        embedded=request.args.get("embedded") == "1",
        admin_user=get_current_admin(),
    )


@app.get("/api/health")
def api_health() -> Any:
    ensure_database()
    with get_connection() as connection:
        total_clients = connection.execute("SELECT COUNT(*) FROM clients").fetchone()[0]
        total_admin_users = connection.execute(
            "SELECT COUNT(*) FROM admin_users"
        ).fetchone()[0]
    return jsonify(
        {
            "status": "ok",
            "database": str(DB_PATH),
            "total_clients": total_clients,
            "total_admin_users": total_admin_users,
            "google_login_enabled": GOOGLE_OAUTH_ENABLED,
            "server_time": utc_now_iso(),
        }
    )


@app.get("/api/admin/overview")
@admin_session_required
def api_admin_overview() -> Any:
    ensure_database()
    with get_connection() as connection:
        client_rows = connection.execute(
            """
            SELECT *
            FROM clients
            ORDER BY
                CASE WHEN last_seen_at IS NULL THEN 1 ELSE 0 END,
                last_seen_at DESC,
                display_name COLLATE NOCASE ASC
            """
        ).fetchall()
        total_locations = connection.execute(
            "SELECT COUNT(*) FROM locations"
        ).fetchone()[0]
        recent_activity_rows = connection.execute(
            """
            SELECT
                locations.id,
                locations.client_id,
                clients.display_name,
                clients.device_model,
                locations.latitude,
                locations.longitude,
                locations.accuracy,
                locations.provider,
                locations.battery_percent,
                locations.is_charging,
                locations.recorded_at
            FROM locations
            INNER JOIN clients ON clients.id = locations.client_id
            ORDER BY locations.recorded_at DESC
            LIMIT 12
            """
        ).fetchall()

    clients = [client_row_to_dict(row) for row in client_rows]
    online_clients = sum(1 for client in clients if client["is_online"])
    clients_with_location = sum(
        1
        for client in clients
        if client["last_latitude"] is not None and client["last_longitude"] is not None
    )
    latest_sync_at = max(
        (client["last_seen_at"] for client in clients if client["last_seen_at"]),
        default="",
    )
    newest_client_at = max(
        (client["created_at"] for client in clients if client["created_at"]),
        default="",
    )

    return jsonify(
        {
            "stats": {
                "total_clients": len(clients),
                "online_clients": online_clients,
                "offline_clients": max(0, len(clients) - online_clients),
                "clients_with_location": clients_with_location,
                "total_locations": total_locations,
                "latest_sync_at": latest_sync_at,
                "newest_client_at": newest_client_at,
                "server_time": utc_now_iso(),
                "database_path": str(DB_PATH),
            },
            "recent_activity": [location_row_to_dict(row) for row in recent_activity_rows],
        }
    )


@app.post("/api/clients/register")
def register_client() -> Any:
    try:
        payload = require_json()
    except ValueError as exc:
        return jsonify({"error": str(exc)}), 400

    auth_token = str(payload.get("auth_token", "")).strip()
    display_name = str(payload.get("display_name", "")).strip() or "Cliente Easy 2"
    device_model = str(payload.get("device_model", "")).strip() or "Android"
    age = str(payload.get("age", "")).strip()

    if not auth_token:
        return jsonify({"error": "Falta auth_token."}), 400

    ensure_database()
    now = utc_now_iso()
    created = False

    with get_connection() as connection:
        existing_row = connection.execute(
            "SELECT id FROM clients WHERE auth_token = ?",
            (auth_token,),
        ).fetchone()

        if existing_row:
            client_id = existing_row["id"]
            connection.execute(
                """
                UPDATE clients
                SET display_name = ?, device_model = ?, age = ?, updated_at = ?
                WHERE id = ?
                """,
                (display_name, device_model, age, now, client_id),
            )
        else:
            client_id = f"cli_{secrets.token_hex(4)}"
            created = True
            connection.execute(
                """
                INSERT INTO clients (
                    id,
                    auth_token,
                    display_name,
                    device_model,
                    age,
                    created_at,
                    updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                (client_id, auth_token, display_name, device_model, age, now, now),
            )

        connection.commit()

    return jsonify(
        {
            "client_id": client_id,
            "created": created,
            "dashboard_url": url_for("admin_dashboard", _external=False),
        }
    )


@app.post("/api/clients/<client_id>/location")
def save_location(client_id: str) -> Any:
    try:
        payload = require_json()
    except ValueError as exc:
        return jsonify({"error": str(exc)}), 400

    auth_token = str(payload.get("auth_token", "")).strip()
    if not auth_token:
        return jsonify({"error": "Falta auth_token."}), 400

    try:
        latitude = float(payload["latitude"])
        longitude = float(payload["longitude"])
    except (KeyError, TypeError, ValueError):
        return jsonify({"error": "Latitude y longitude son obligatorios."}), 400

    accuracy_value = payload.get("accuracy")
    try:
        accuracy = float(accuracy_value) if accuracy_value not in (None, "") else None
    except (TypeError, ValueError):
        accuracy = None

    provider = str(payload.get("provider", "")).strip()
    device_model = str(payload.get("device_model", "")).strip()
    battery_percent_value = payload.get("battery_percent")
    try:
        battery_percent = int(battery_percent_value) if battery_percent_value is not None else None
    except (TypeError, ValueError):
        battery_percent = None

    is_charging = bool(payload.get("is_charging", False))
    recorded_at_ms = payload.get("recorded_at")
    if isinstance(recorded_at_ms, (int, float)):
        recorded_at = datetime.fromtimestamp(recorded_at_ms / 1000, UTC).isoformat()
    else:
        recorded_at = utc_now_iso()

    ensure_database()
    with get_connection() as connection:
        client_row = connection.execute(
            "SELECT id, auth_token, device_model FROM clients WHERE id = ?",
            (client_id,),
        ).fetchone()
        if client_row is None:
            return jsonify({"error": "Cliente no encontrado."}), 404
        if client_row["auth_token"] != auth_token:
            return jsonify({"error": "auth_token incorrecto."}), 403

        connection.execute(
            """
            INSERT INTO locations (
                client_id,
                latitude,
                longitude,
                accuracy,
                provider,
                battery_percent,
                is_charging,
                recorded_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (
                client_id,
                latitude,
                longitude,
                accuracy,
                provider,
                battery_percent,
                int(is_charging),
                recorded_at,
            ),
        )
        connection.execute(
            """
            UPDATE clients
            SET updated_at = ?,
                device_model = ?,
                last_seen_at = ?,
                last_latitude = ?,
                last_longitude = ?,
                last_accuracy = ?,
                last_provider = ?,
                last_battery_percent = ?,
                last_is_charging = ?
            WHERE id = ?
            """,
            (
                utc_now_iso(),
                device_model or client_row["device_model"],
                recorded_at,
                latitude,
                longitude,
                accuracy,
                provider,
                battery_percent,
                int(is_charging),
                client_id,
            ),
        )
        connection.commit()

    return jsonify({"ok": True, "client_id": client_id, "recorded_at": recorded_at})


@app.get("/api/clients")
@admin_session_required
def list_clients() -> Any:
    ensure_database()
    with get_connection() as connection:
        rows = connection.execute(
            """
            SELECT *
            FROM clients
            ORDER BY
                CASE WHEN last_seen_at IS NULL THEN 1 ELSE 0 END,
                last_seen_at DESC,
                display_name COLLATE NOCASE ASC
            """
        ).fetchall()

    return jsonify([client_row_to_dict(row) for row in rows])


@app.get("/api/clients/<client_id>")
@admin_session_required
def get_client(client_id: str) -> Any:
    limit = request.args.get("limit", default=50, type=int)
    limit = max(1, min(limit, 200))

    ensure_database()
    with get_connection() as connection:
        client_row = connection.execute(
            "SELECT * FROM clients WHERE id = ?",
            (client_id,),
        ).fetchone()
        if client_row is None:
            return jsonify({"error": "Cliente no encontrado."}), 404

        history_stats_row = connection.execute(
            """
            SELECT
                COUNT(*) AS total_points,
                MIN(recorded_at) AS first_recorded_at,
                MAX(recorded_at) AS last_recorded_at,
                AVG(accuracy) AS average_accuracy
            FROM locations
            WHERE client_id = ?
            """,
            (client_id,),
        ).fetchone()
        history_rows = connection.execute(
            """
            SELECT *
            FROM locations
            WHERE client_id = ?
            ORDER BY recorded_at DESC
            LIMIT ?
            """,
            (client_id, limit),
        ).fetchall()

    history = [location_row_to_dict(row) for row in history_rows]
    history.reverse()
    return jsonify(
        {
            "client": client_row_to_dict(client_row),
            "history": history,
            "metrics": {
                "total_points": history_stats_row["total_points"] or 0,
                "loaded_points": len(history),
                "first_recorded_at": history_stats_row["first_recorded_at"] or "",
                "last_recorded_at": history_stats_row["last_recorded_at"] or "",
                "average_accuracy": history_stats_row["average_accuracy"],
            },
        }
    )


@app.patch("/api/clients/<client_id>")
@admin_session_required
def update_client(client_id: str) -> Any:
    try:
        payload = require_json()
    except ValueError as exc:
        return jsonify({"error": str(exc)}), 400

    display_name = payload.get("display_name")
    age = payload.get("age")

    if display_name is None and age is None:
        return jsonify({"error": "No hay cambios para guardar."}), 400

    display_name_value = None
    if display_name is not None:
        display_name_value = str(display_name).strip()
        if not display_name_value:
            return jsonify({"error": "El nombre no puede estar vacío."}), 400

    age_value = None
    if age is not None:
        age_value = str(age).strip()

    ensure_database()
    with get_connection() as connection:
        client_row = connection.execute(
            "SELECT * FROM clients WHERE id = ?",
            (client_id,),
        ).fetchone()
        if client_row is None:
            return jsonify({"error": "Cliente no encontrado."}), 404

        connection.execute(
            """
            UPDATE clients
            SET display_name = ?,
                age = ?,
                updated_at = ?
            WHERE id = ?
            """,
            (
                display_name_value if display_name_value is not None else client_row["display_name"],
                age_value if age_value is not None else (client_row["age"] or ""),
                utc_now_iso(),
                client_id,
            ),
        )
        connection.commit()

        updated_row = connection.execute(
            "SELECT * FROM clients WHERE id = ?",
            (client_id,),
        ).fetchone()

    return jsonify({"ok": True, "client": client_row_to_dict(updated_row)})


@app.delete("/api/clients/<client_id>/history")
@admin_session_required
def clear_client_history(client_id: str) -> Any:
    ensure_database()
    with get_connection() as connection:
        client_row = connection.execute(
            "SELECT * FROM clients WHERE id = ?",
            (client_id,),
        ).fetchone()
        if client_row is None:
            return jsonify({"error": "Cliente no encontrado."}), 404

        removed_points = connection.execute(
            "SELECT COUNT(*) FROM locations WHERE client_id = ?",
            (client_id,),
        ).fetchone()[0]

        connection.execute(
            "DELETE FROM locations WHERE client_id = ?",
            (client_id,),
        )
        connection.execute(
            """
            UPDATE clients
            SET updated_at = ?,
                last_seen_at = NULL,
                last_latitude = NULL,
                last_longitude = NULL,
                last_accuracy = NULL,
                last_provider = NULL,
                last_battery_percent = NULL,
                last_is_charging = 0
            WHERE id = ?
            """,
            (utc_now_iso(), client_id),
        )
        connection.commit()

    return jsonify(
        {
            "ok": True,
            "client_id": client_id,
            "removed_points": removed_points,
        }
    )


@app.delete("/api/clients/<client_id>")
@admin_session_required
def delete_client(client_id: str) -> Any:
    ensure_database()
    with get_connection() as connection:
        client_row = connection.execute(
            "SELECT display_name FROM clients WHERE id = ?",
            (client_id,),
        ).fetchone()
        if client_row is None:
            return jsonify({"error": "Cliente no encontrado."}), 404

        removed_points = connection.execute(
            "SELECT COUNT(*) FROM locations WHERE client_id = ?",
            (client_id,),
        ).fetchone()[0]
        connection.execute("DELETE FROM locations WHERE client_id = ?", (client_id,))
        connection.execute("DELETE FROM clients WHERE id = ?", (client_id,))
        connection.commit()

    return jsonify(
        {
            "ok": True,
            "client_id": client_id,
            "display_name": client_row["display_name"],
            "removed_points": removed_points,
        }
    )


if __name__ == "__main__":
    ensure_database()
    host = os.environ.get("EASY2_SERVER_HOST", "0.0.0.0")
    port = int(os.environ.get("EASY2_SERVER_PORT", "8000"))
    debug_env = os.environ.get("EASY2_SERVER_DEBUG", "").strip().lower()
    debug = debug_env in {"1", "true", "yes", "on"}
    app.run(host=host, port=port, debug=debug, use_reloader=debug)
