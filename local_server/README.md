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
- Si no hay sesión iniciada, la web redirige a `/login`.

## Login con Google

La web del administrador puede crear cuenta y entrar usando Google OAuth.

1. Crea un cliente OAuth 2.0 de tipo web en Google Cloud Console.
2. Añade como redirect URI autorizada `http://IP_DEL_SERVIDOR:8000/auth/google/callback`.
3. Si usas un dominio o una IP distinta, añade también esa URL real.
4. Configura estas variables antes de arrancar el servidor:

```bash
export EASY2_GOOGLE_CLIENT_ID="tu-client-id"
export EASY2_GOOGLE_CLIENT_SECRET="tu-client-secret"
```

Tambien puedes dejar el JSON descargado de Google en la raiz del proyecto con nombre tipo `client_secret_....json` y el servidor lo leerá automáticamente.

Error habitual:

- Si Google muestra `Error 403: org_internal`, el cliente OAuth pertenece a una app de tipo interno de Google Workspace.
- En ese caso solo pueden entrar cuentas de esa misma organización.
- La solución es usar una cuenta del Workspace propietario o crear otro cliente OAuth web en un proyecto con pantalla de consentimiento de tipo externo.
- Si sigues viendo el mismo error, revisa que el servidor no esté cargando un `client_secret_*.json` antiguo desde la raíz del proyecto y usa `EASY2_GOOGLE_CLIENT_JSON` para apuntar al JSON correcto.

Variables útiles:

- `EASY2_GOOGLE_AUTO_CREATE=1`
- `EASY2_ADMIN_ALLOWED_EMAILS=admin1@gmail.com,admin2@gmail.com`
- `EASY2_ADMIN_ALLOWED_DOMAINS=tu-dominio.com`
- `EASY2_GOOGLE_CLIENT_JSON=/ruta/al/client_secret_xxx.json`
- `EASY2_PUBLIC_BASE_URL=http://192.168.1.50:8000`
- `EASY2_SERVER_SECRET_KEY=una-clave-larga-y-fija`

Notas:

- Si `EASY2_GOOGLE_AUTO_CREATE` está activo y no limitas emails o dominios, cualquier cuenta de Google con email verificado podrá crear usuario administrador.
- `EASY2_PUBLIC_BASE_URL` ayuda a generar la callback correcta cuando el servidor se publica con otra IP, otro puerto o un dominio.
- Para cerrar sesión usa `/logout`.

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
- `GET /login`
- `GET /auth/google`
- `GET /auth/google/callback`
- `GET /logout`
- `GET /admin`
- `GET /dashboard`

## Variables opcionales

- `EASY2_SERVER_HOST`
- `EASY2_SERVER_PORT`
- `EASY2_GOOGLE_CLIENT_ID`
- `EASY2_GOOGLE_CLIENT_SECRET`
- `EASY2_GOOGLE_CLIENT_JSON`
- `EASY2_PUBLIC_BASE_URL`
- `EASY2_SERVER_SECRET_KEY`
- `EASY2_GOOGLE_AUTO_CREATE`
- `EASY2_ADMIN_ALLOWED_EMAILS`
- `EASY2_ADMIN_ALLOWED_DOMAINS`
