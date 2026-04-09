# Servidor local Easy 2

Servidor local en Python para registrar teléfonos cliente, guardar su última ubicación en SQLite y mostrarla en un panel web para administradores.

## Qué incluye

- API para registrar clientes.
- API para recibir ubicaciones.
- Base de datos SQLite en `local_server/data/easy2.db`.
- Dashboard web en `/dashboard` con lista de clientes, mapa e histórico reciente.

## Arranque rápido

```bash
cd "/Users/raul/AndroidStudioProjects/Easy 2/local_server"
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python app.py
```

El servidor escucha por defecto en `0.0.0.0:8000`.

## Cómo conectar el teléfono

1. Averigua la IP local del ordenador que ejecuta el servidor.
2. En el `setup` del teléfono cliente usa esa dirección en el campo servidor.
3. Ejemplo: `http://192.168.1.50:8000`
4. En el teléfono administrador usa la misma dirección.

## Endpoints

- `GET /api/health`
- `POST /api/clients/register`
- `POST /api/clients/<client_id>/location`
- `GET /api/clients`
- `GET /api/clients/<client_id>?limit=100`
- `GET /dashboard`

## Variables opcionales

- `EASY2_SERVER_HOST`
- `EASY2_SERVER_PORT`
