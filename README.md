# Easy 2

Easy 2 es una aplicación Android pensada para simplificar el uso del smartphone en personas mayores. La app convierte el teléfono en una experiencia más clara, segura y guiada mediante un launcher propio, llamadas simplificadas, accesos rápidos y utilidades cotidianas.

## Resumen

Los smartphones suelen concentrar demasiadas opciones, iconos pequeños y flujos complejos. Easy 2 propone una alternativa centrada en la accesibilidad:

- interfaz principal limpia y en pantalla completa
- información esencial visible de un vistazo
- contactos importantes siempre a mano
- configuración guiada paso a paso
- herramientas útiles integradas en el mismo entorno

## Propuesta de valor

Easy 2 busca mejorar la autonomía digital de personas mayores o usuarios con baja familiaridad tecnológica. En lugar de obligar a adaptarse al móvil tradicional, adapta el móvil a las necesidades del usuario.

Sus pilares son:

- simplicidad: menos pasos para hacer tareas frecuentes
- seguridad: SOS, información médica y contactos prioritarios
- personalización: nombre, edad, color del tema y accesos directos
- accesibilidad: botones grandes, textos claros y navegación directa

## Funcionalidades principales

### 1. Launcher accesible

La pantalla principal funciona como launcher del dispositivo e integra:

- hora y fecha en gran tamaño
- batería y estado de carga
- clima actual según ubicación
- nombre y edad del usuario
- accesos directos a aplicaciones favoritas
- acceso rápido a información médica, ajustes y SOS

### 2. Configuración inicial guiada

La app incluye un flujo de onboarding en 4 pasos para dejar el teléfono listo:

- datos personales
- elección de color del tema
- información médica
- selección de 4 contactos principales

También permite editar la configuración más adelante, ordenar accesos directos y abrir ajustes del teléfono o del teclado.

### 3. Contactos prioritarios y llamadas sencillas

Easy 2 muestra los contactos en una cuadrícula visual y permite:

- priorizar 4 contactos clave
- personalizar su imagen
- elegir entre varios números si un contacto tiene más de uno
- confirmar la llamada antes de iniciarla

Además, incorpora:

- marcador propio
- acceso rápido a emergencias
- pantalla de llamada en curso
- integración con `InCallService` para una experiencia de llamada más controlada

### 4. Utilidades integradas

Desde la pantalla principal se puede abrir un panel de utilidades con:

- calendario de lectura sencilla
- bloc de notas persistente
- escáner de alimentos

El escáner usa la cámara para leer códigos de barras y, si el código incluye una fecha GS1, muestra la fecha de caducidad o de consumo preferente.

### 5. Teclado propio

La app incluye un teclado personalizado con teclas grandes, pensado para acompañar el flujo accesible de configuración y edición de notas.

### 6. Despertar por movimiento

Easy 2 incorpora un servicio en segundo plano que detecta cuándo el usuario coge el teléfono para encender la pantalla y mostrar el launcher.

## Diferenciales del proyecto

- No depende de un backend propio para funcionar en su núcleo.
- Combina accesibilidad, personalización y utilidades en una sola app.
- Aprovecha capacidades nativas de Android como launcher, teclado, marcador y servicios de llamada.
- Propone una experiencia enfocada en autonomía, no solo en interfaz visual.

## Stack técnico

- Java 11
- Android SDK
- AndroidX AppCompat
- RecyclerView
- Material Components
- CameraX
- ML Kit Barcode Scanning
- `SharedPreferences` para persistencia local
- almacenamiento interno para imágenes personalizadas de contactos
- Open-Meteo para el clima en tiempo real
- `InputMethodService`, `InCallService`, `TelecomManager`, `SensorManager` y `LocationManager`

## Arquitectura resumida

Algunas piezas clave del proyecto:

- `MainActivity`: launcher principal, clima, accesos directos y contactos
- `SetupActivity`: configuración inicial guiada
- `ShortcutOrganizerActivity`: reordenación y gestión de accesos directos
- `DialerActivity`, `CallConfirmationActivity` y `CallActivity`: flujo de llamada
- `Easy2InCallService`: integración con llamadas del sistema
- `UtilitiesActivity`: acceso al calendario, notas y escáner
- `SeniorCalendarActivity`: calendario simplificado
- `NotesActivity`: notas persistentes
- `FoodScannerActivity` y `Gs1DateParser`: lectura de códigos y extracción de fechas
- `Easy2InputMethodService`: teclado propio
- `PickupMonitorService`: detección de movimiento y despertar de pantalla
- `LauncherPreferences` y `ShortcutStorage`: persistencia local

## Público objetivo

Esta app está especialmente orientada a:

- personas mayores
- usuarios con baja alfabetización digital
- familias y cuidadores que quieran simplificar el uso del móvil
- contextos donde la rapidez y claridad de acceso sean prioritarias

## Permisos que utiliza

Para ofrecer toda la experiencia, Easy 2 solicita permisos de:

- contactos
- ubicación
- cámara
- llamadas
- servicio en primer plano

Cada permiso responde a una función concreta de la app:

- contactos: mostrar y llamar a contactos guardados
- ubicación: cargar el clima real del lugar donde está el usuario
- cámara: escanear códigos de barras de alimentos
- llamadas: iniciar y gestionar llamadas desde la propia interfaz

## Limitaciones actuales

- El clima depende de conexión a internet y permisos de ubicación.
- El escáner solo puede mostrar fecha si el código incorpora esa información en formato GS1.
- Algunas funciones de llamada requieren que Easy 2 sea configurada como app de teléfono por defecto.
- La experiencia completa mejora si el usuario también activa Easy 2 como launcher y como teclado.

## Cómo ejecutar el proyecto

### Requisitos

- Android Studio
- JDK 11
- dispositivo o emulador con Android 11 o superior

### Configuración

1. Abre el proyecto en Android Studio.
2. Espera a que Gradle sincronice las dependencias.
3. Ejecuta la app en un dispositivo físico o emulador.
4. Concede permisos de contactos, ubicación y cámara cuando la app los solicite.
5. Para probar toda la experiencia, configura Easy 2 como:
   - launcher predeterminado
   - app de teléfono
   - teclado activo

### Compilación por terminal

```bash
./gradlew assembleDebug
```

## Guion breve para una presentación

Si quieres presentar la app en clase, demo o portfolio, este orden funciona bien:

1. Explica el problema: los smartphones tradicionales no están pensados para muchos usuarios mayores.
2. Enseña la configuración inicial guiada: nombre, edad, color, datos médicos y contactos.
3. Muestra el launcher: hora, batería, clima, accesos rápidos y contactos visuales.
4. Abre un contacto y enseña la confirmación de llamada.
5. Enseña el botón SOS y el marcador simplificado.
6. Abre el panel de utilidades y muestra calendario, notas y escáner de alimentos.
7. Cierra con la idea principal: una experiencia móvil más simple, segura y autónoma.

## Mensaje de presentación en una frase

> Easy 2 transforma un smartphone convencional en una interfaz accesible y segura, diseñada para que las personas mayores usen el móvil con más autonomía y menos fricción.

## Posibles mejoras futuras

- recordatorios y medicación
- integración con asistencia auditiva
- mayor personalización visual
- sincronización opcional de datos
- métricas de uso para cuidadores o familiares, respetando privacidad

## Estado del proyecto

Easy 2 se encuentra planteada como una solución funcional centrada en accesibilidad, simplificación de interacción y apoyo a tareas cotidianas desde un entorno Android unificado.
