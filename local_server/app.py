from __future__ import annotations

import os
import secrets
import sqlite3
from datetime import UTC, datetime
from pathlib import Path
from typing import Any

from flask import Flask, jsonify, redirect, render_template, request, url_for


BASE_DIR = Path(__file__).resolve().parent
DATA_DIR = BASE_DIR / "data"
DB_PATH = DATA_DIR / "easy2.db"

app = Flask(__name__)


def utc_now_iso() -> str:
    return datetime.now(UTC).isoformat()


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
            CREATE INDEX IF NOT EXISTS idx_locations_client_recorded_at
            ON locations(client_id, recorded_at DESC)
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


@app.route("/")
def index() -> Any:
    return redirect(url_for("admin_dashboard"))


@app.route("/admin")
@app.route("/dashboard")
def admin_dashboard() -> Any:
    return render_template("dashboard.html", embedded=request.args.get("embedded") == "1")


@app.get("/api/health")
def api_health() -> Any:
    ensure_database()
    with get_connection() as connection:
        total_clients = connection.execute("SELECT COUNT(*) FROM clients").fetchone()[0]
    return jsonify(
        {
            "status": "ok",
            "database": str(DB_PATH),
            "total_clients": total_clients,
            "server_time": utc_now_iso(),
        }
    )


@app.get("/api/admin/overview")
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
            "SELECT id, auth_token FROM clients WHERE id = ?",
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
