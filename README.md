# Easy 2

<div align="center">

### Un smartphone menos hostil, más humano y realmente usable para personas mayores

<p>
  <img src="https://img.shields.io/badge/Android-App-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android App">
  <img src="https://img.shields.io/badge/Java-App-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java App">
  <img src="https://img.shields.io/badge/MVP-Funcional-111111?style=for-the-badge" alt="MVP funcional">
  <img src="https://img.shields.io/badge/Focus-Accesibilidad%20real-2457F5?style=for-the-badge" alt="Accesibilidad real">
</p>

</div>

> Easy 2 transforma un móvil Android convencional en una experiencia clara, segura y guiada, diseñada para reducir fricción, aumentar autonomía y devolver confianza digital a personas mayores.

---

## El problema

Los smartphones actuales están optimizados para usuarios expertos: interfaces densas, demasiadas opciones, flujos largos, iconos pequeños y decisiones constantes.

Para muchas personas mayores, eso no es una molestia menor. Es una barrera directa.

Easy 2 nace para resolver eso desde la raíz: no añadiendo una capa visual encima del caos, sino replanteando la experiencia completa del dispositivo.

## La idea en una frase

**Si usar un smartphone exige aprender demasiadas cosas, entonces el problema no es la persona: es la interfaz.**

## Qué hace diferente a Easy 2

- **No es solo una app:** puede actuar como launcher, marcador, teclado y capa principal de interacción.
- **No simplifica solo la estética:** simplifica decisiones, pasos, errores y puntos de bloqueo.
- **No depende del cloud para su núcleo:** la experiencia principal funciona desde el propio dispositivo.
- **No piensa solo en el usuario final:** incorpora un modo administrador con panel web local para acompañamiento y supervisión.

---

## TL;DR para jueces

- **Impacto social claro:** resuelve una fricción real de accesibilidad digital.
- **Ambición técnica alta:** launcher, dialer, `InCallService`, teclado propio, sensores, cámara, ubicación y panel local.
- **Producto coherente:** todo está integrado en una misma experiencia, no son features sueltas.
- **Demo potente:** en pocos minutos se entiende, se siente y se recuerda.

---

## Experiencia de producto

### 1. Launcher accesible

La pantalla principal sustituye el arranque clásico por una interfaz limpia, grande y directa.

Incluye:

- hora y fecha en gran tamaño
- batería y estado de carga
- clima actual según ubicación
- nombre y edad del usuario
- accesos directos visibles
- acceso inmediato a ajustes, SOS, información médica y utilidades

### 2. Configuración guiada

Easy 2 incorpora un onboarding en pasos cortos para dejar el teléfono listo sin menús confusos.

Permite configurar:

- datos personales
- color del tema
- información médica
- contactos prioritarios
- servidor remoto opcional para modo administrador

### 3. Llamadas realmente simples

La app rediseña la comunicación para eliminar fricción en una acción crítica.

Incluye:

- contactos favoritos en formato visual
- elección de número cuando un contacto tiene varios
- confirmación antes de llamar
- marcador propio
- integración con `InCallService`
- acceso rápido a emergencia

### 4. Utilidades útiles de verdad

En lugar de llenar la interfaz con decenas de herramientas, Easy 2 integra solo lo que aporta valor inmediato:

- calendario de lectura sencilla
- bloc de notas persistente
- escáner de alimentos con lectura de códigos de barras
- interpretación de fechas GS1 cuando están presentes en el código

### 5. Teclado propio

El proyecto incluye un teclado personalizado orientado a legibilidad y control, pensado para acompañar el resto de la experiencia accesible.

### 6. Activación por movimiento

Easy 2 detecta cuándo el usuario coge el dispositivo y reactiva la pantalla para mostrar el launcher sin pasos extra.

### 7. Panel de administrador local

Además de la app Android, el proyecto incluye un servidor local opcional para registrar dispositivos cliente y visualizar información desde una web de administración.

Ese panel permite:

- alta de dispositivos
- recepción de ubicación
- historial de actividad
- panel web embebido en la app de administrador
- autenticación con Google en la interfaz web local

---

## Por qué esto impresiona en una hackathon

Easy 2 no se queda en una demo bonita. Toca varias capas complejas del ecosistema Android y las unifica en un mismo producto:

- experiencia de launcher
- flujo de llamadas
- teclado personalizado
- gestión de permisos delicados
- integración de cámara y ML Kit
- uso de sensores y ubicación
- almacenamiento local
- panel de administración local con backend propio

**Eso convierte el proyecto en una propuesta completa, no en una simple feature aislada.**

---

## Qué problema resuelve para el usuario

| Necesidad real | Cómo responde Easy 2 |
|---|---|
| Ver información importante sin buscarla | Launcher claro, a pantalla completa y con jerarquía visual |
| Llamar rápido y sin errores | Contactos prioritarios, confirmación y marcador simplificado |
| No perderse en ajustes complejos | Configuración guiada paso a paso |
| Recordar información crítica | Acceso rápido a datos médicos y notas |
| Usar el móvil con menos ansiedad | Botones grandes, menos decisiones y navegación directa |
| Acompañamiento por parte de familiares o cuidadores | Modo administrador con panel local opcional |

---

## Arquitectura del proyecto

### App Android

Piezas principales:

- `MainActivity`: launcher principal
- `SetupActivity`: onboarding y configuración
- `DialerActivity`, `CallConfirmationActivity`, `CallActivity`: flujo de llamadas
- `Easy2InCallService`: UI propia para llamadas del sistema
- `UtilitiesActivity`: entrada a utilidades
- `SeniorCalendarActivity`: calendario simplificado
- `NotesActivity`: notas persistentes
- `FoodScannerActivity` + `Gs1DateParser`: escáner y lectura de fechas
- `Easy2InputMethodService`: teclado personalizado
- `PickupMonitorService`: detección de movimiento
- `LauncherPreferences` y `ShortcutStorage`: persistencia y personalización
- `AdminActivity`: acceso embebido al panel web del administrador

### Backend local opcional

El directorio `local_server/` aporta una capa de acompañamiento y supervisión:

- API HTTP para registrar clientes
- SQLite para persistencia local
- dashboard web para administración
- autenticación con Google en la web
- integración con la app Android en modo administrador

---

## Stack técnico

### Android

- Java
- Android SDK
- AndroidX AppCompat
- RecyclerView
- Material Components
- CameraX
- ML Kit Barcode Scanning
- `SharedPreferences`
- almacenamiento interno

### APIs y capacidades nativas

- `InputMethodService`
- `InCallService`
- `TelecomManager`
- `LocationManager`
- `SensorManager`
- `WebView`

### Servidor local

- Python
- Flask
- Authlib
- SQLite

---

## Decisiones de producto inteligentes

- **Offline-first en el núcleo:** el valor principal no depende de un backend externo.
- **Backend opcional:** el panel de administración amplía la solución, pero no bloquea el uso base.
- **Accesibilidad como arquitectura, no como parche:** el diseño funcional influye en navegación, llamadas, teclado y utilidades.
- **Uso de capacidades nativas con propósito:** cada permiso responde a una funcionalidad concreta y visible.

---

## Privacidad y permisos

Easy 2 solicita permisos porque ofrece funciones concretas, no porque recoja datos sin propósito.

| Permiso | Motivo |
|---|---|
| `READ_CONTACTS` | mostrar y llamar a contactos guardados |
| `CALL_PHONE` | iniciar llamadas desde la propia interfaz |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | obtener clima y, si se activa, soporte al panel local |
| `CAMERA` | escanear códigos de barras de alimentos |
| `FOREGROUND_SERVICE` | soporte a procesos persistentes del dispositivo |
| `WAKE_LOCK` | reactivar pantalla en la experiencia de uso |
| `INTERNET` | clima y comunicación con el servidor local opcional |

---

## Demo recomendada para jurado

Si solo tienes 2 o 3 minutos, esta es la secuencia ideal:

1. Presenta el problema: “un smartphone normal sigue siendo demasiado complejo para muchas personas mayores”.
2. Enseña el onboarding guiado.
3. Abre el launcher y deja que se vea la jerarquía visual.
4. Entra a un contacto favorito y muestra la llamada simplificada.
5. Enseña el acceso a SOS e información médica.
6. Abre utilidades y enseña calendario, notas y escáner.
7. Cierra con el modo administrador y el panel local, si quieres reforzar la visión de acompañamiento.

---

## Cómo ejecutar el proyecto

### Requisitos

- Android Studio
- JDK
- dispositivo físico o emulador compatible

### App Android

1. Abre el proyecto en Android Studio.
2. Espera a que Gradle sincronice dependencias.
3. Ejecuta la app.
4. Concede permisos cuando se soliciten.
5. Para ver la experiencia completa, configura Easy 2 como:
   - launcher predeterminado
   - app de teléfono
   - teclado activo

### Compilación por terminal

```bash
./gradlew assembleDebug
```

### Servidor local opcional

```bash
cd local_server
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python app.py
```

El panel de administración quedará disponible en:

```text
http://IP_DEL_SERVIDOR:8000/admin
```

---

## Estado actual

Easy 2 es un **MVP funcional con una visión de producto muy clara**:

- experiencia principal ya integrada
- componentes diferenciales implementados
- valor social evidente
- margen real de evolución hacia un producto de mayor alcance

---

## Roadmap natural

- recordatorios y medicación
- personalización visual avanzada
- métricas de uso respetuosas con la privacidad
- integración con cuidadores y familiares
- mejoras de accesibilidad auditiva y cognitiva

---

## Cierre

Easy 2 no intenta enseñar a una persona mayor a comportarse como un usuario experto.

**Hace justo lo contrario: adapta el smartphone al ritmo, contexto y necesidades reales de quien lo usa.**

Y ahí está su valor.
