#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VENV_DIR="$SCRIPT_DIR/.venv"
PYTHON_BIN="${PYTHON_BIN:-python3}"
HOST="${EASY2_SERVER_HOST:-0.0.0.0}"
PORT="${EASY2_SERVER_PORT:-8000}"
DETACH="${EASY2_SERVER_DETACH:-0}"
LOG_DIR="$SCRIPT_DIR/run"
PID_FILE="$LOG_DIR/server.pid"
LOG_FILE="$LOG_DIR/server.log"

if ! command -v "$PYTHON_BIN" >/dev/null 2>&1; then
  echo "No se ha encontrado $PYTHON_BIN en el sistema."
  exit 1
fi

cd "$SCRIPT_DIR"

if [ ! -d "$VENV_DIR" ]; then
  echo "Creando entorno virtual en $VENV_DIR"
  "$PYTHON_BIN" -m venv "$VENV_DIR"
else
  echo "El entorno virtual ya existe en $VENV_DIR"
fi

# shellcheck disable=SC1091
source "$VENV_DIR/bin/activate"

echo "Actualizando pip"
python -m pip install --upgrade pip

if [ -f "$SCRIPT_DIR/requirements.txt" ]; then
  echo "Instalando dependencias desde requirements.txt"
  pip install -r "$SCRIPT_DIR/requirements.txt"
else
  echo "No existe requirements.txt en $SCRIPT_DIR"
  exit 1
fi

mkdir -p "$LOG_DIR"

if [ "$DETACH" = "1" ]; then
  if [ -f "$PID_FILE" ]; then
    EXISTING_PID="$(cat "$PID_FILE" 2>/dev/null || true)"
    if [ -n "$EXISTING_PID" ] && kill -0 "$EXISTING_PID" >/dev/null 2>&1; then
      echo
      echo "El servidor ya está ejecutándose."
      echo "PID: $EXISTING_PID"
      echo "URL: http://127.0.0.1:$PORT"
      echo "Log: $LOG_FILE"
      exit 0
    fi
    rm -f "$PID_FILE"
  fi
fi

if lsof -nP -iTCP:"$PORT" -sTCP:LISTEN >/dev/null 2>&1; then
  echo "El puerto $PORT ya está en uso. Libéralo o cambia EASY2_SERVER_PORT."
  exit 1
fi

echo
echo "Entorno preparado correctamente."

if [ "$DETACH" = "1" ]; then
  echo "Arrancando el servidor local en segundo plano..."

  nohup env \
    EASY2_SERVER_HOST="$HOST" \
    EASY2_SERVER_PORT="$PORT" \
    EASY2_SERVER_DEBUG=0 \
    python app.py >> "$LOG_FILE" 2>&1 &

  SERVER_PID=$!
  echo "$SERVER_PID" > "$PID_FILE"

  sleep 2

  if ! kill -0 "$SERVER_PID" >/dev/null 2>&1; then
    echo "No se ha podido arrancar el servidor."
    echo "Revisa el log: $LOG_FILE"
    rm -f "$PID_FILE"
    exit 1
  fi

  echo "Servidor en ejecución."
  echo "PID: $SERVER_PID"
  echo "URL local: http://127.0.0.1:$PORT"
  echo "URL red: http://$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null || echo localhost):$PORT"
  echo "Log: $LOG_FILE"
  exit 0
fi

echo "Arrancando el servidor local en primer plano..."
echo "URL local: http://127.0.0.1:$PORT"
echo "URL red: http://$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null || echo localhost):$PORT"
exec env \
  EASY2_SERVER_HOST="$HOST" \
  EASY2_SERVER_PORT="$PORT" \
  EASY2_SERVER_DEBUG=0 \
  python app.py
