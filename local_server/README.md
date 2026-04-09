# Servidor local Easy 2

Servidor local en Python para registrar teléfonos cliente, guardar su última ubicación en SQLite y mostrarla en una web de administrador.

## Qué incluye

- API para registrar clientes.
- API para recibir ubicaciones.
- Base de datos SQLite en `local_server/data/easy2.db`.
- Web de administrador en `/admin` y `/dashboard` con:
  - resumen del sistema
  - lista filtrable de clientes
  - mapa e histórico reciente
  - actividad reciente del servidor
  - edición básica de cliente
  - borrado de histórico o eliminación completa de cliente

## Arranque rápido

```bash
cd "/Users/raul/AndroidStudioProjects/Easy 2/local_server"
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python app.py
```

El servidor escucha por defecto en `0.0.0.0:8000`.

## Acceso a la web de administrador

- Navegador: `http://IP_DEL_SERVIDOR:8000/admin`
- Compatibilidad anterior: `http://IP_DEL_SERVIDOR:8000/dashboard`
- La app Android en modo administrador abre la versión embebida de esta misma web.

## Cómo conectar el teléfono

1. Averigua la IP local del ordenador que ejecuta el servidor.
2. En el `setup` del teléfono cliente usa esa dirección en el campo servidor.
3. Ejemplo: `http://192.168.1.50:8000`
4. En el teléfono administrador usa la misma dirección.

## Endpoints

- `GET /api/health`
- `GET /api/admin/overview`
- `POST /api/clients/register`
- `POST /api/clients/<client_id>/location`
- `GET /api/clients`
- `GET /api/clients/<client_id>?limit=100`
- `PATCH /api/clients/<client_id>`
- `DELETE /api/clients/<client_id>/history`
- `DELETE /api/clients/<client_id>`
- `GET /admin`
- `GET /dashboard`

## Variables opcionales

- `EASY2_SERVER_HOST`
- `EASY2_SERVER_PORT`
