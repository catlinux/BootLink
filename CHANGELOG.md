# Registro de cambios

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/)
y versionado según [SemVer](https://semver.org/lang/es/).

## [Sin publicar]

### Añadido

- Plan de implementación en `.agents/plans/bootlink/bootlink.PLAN.md`.
- Documentación inicial del proyecto.
- Andamiaje del proyecto Android con Gradle y Jetpack Compose: módulo `app` con Kotlin,
  Material 3, tema claro y oscuro, minSdk 26, targetSdk 36 y versión 0.1.0, que compila con
  `gradlew.bat assembleDebug` y genera un APK con la pantalla inicial todavía vacía.
- Capa de datos en el paquete `datos`: modelos `AppInstalada` (paquete, etiqueta e icono) y
  `AppConfigurada` (paquete, orden, retardo y si está activa), el enum `ModoArranque`
  (`NOTIFICACION` por defecto) y `Preferencias`, que guarda la lista y el modo en DataStore
  como JSON, con pruebas unitarias sin dispositivo ni emulador: sobre un almacén temporal de
  verdad y, para las pruebas que necesitan varias escrituras seguidas, sobre un almacén en
  memoria.
- Consulta de las aplicaciones lanzables con `PackageManager.queryIntentActivities()` y un
  bloque `<queries>` de MAIN/LAUNCHER en el manifiesto: no se declara el permiso restringido de
  Google Play que obliga a justificar la consulta de todos los paquetes con un vídeo, y
  `RepositorioApps` excluye la propia app y ordena el resultado por etiqueta.
- Arranque automático al encender el teléfono, en el paquete `arranque`: `ReceptorArranque` atiende
  `BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED` y `QUICKBOOT_POWERON` (con `RECEIVE_BOOT_COMPLETED`) y
  levanta `ServicioArranque`, un servicio en primer plano de tipo `shortService` —el único que
  Android 15 permite desde `BOOT_COMPLETED` junto con `specialUse`, y que se cierra solo antes de
  agotar su tiempo—, declarado con el permiso `FOREGROUND_SERVICE`.
- Lanzamiento de las apps configuradas con dos estrategias intercambiables tras una interfaz común
  (`EstrategiaLanzamiento`), porque Android bloquea por defecto que una app en segundo plano abra
  actividades de otras apps: de un toque, con una notificación que lleva un `PendingIntent` a
  `LanzadorActivity` (una actividad sin interfaz, fuera del historial de tareas, que abre las apps
  en cuanto el usuario toca el aviso; necesita `POST_NOTIFICATIONS` en Android 13 o superior y es
  la vía sin permisos especiales), y automática, con una ventana `TYPE_APPLICATION_OVERLAY` que se
  espera a ver visible antes de lanzar (`SYSTEM_ALERT_WINDOW`, exigido también desde Android 15);
  si falta el permiso de superposición, si la ventana no llega a mostrarse o si el sistema no deja
  levantar el servicio, se avisa con la notificación en lugar de fallar en silencio.
- `AppLauncher` respeta el orden, el retardo y la marca de activa de cada app, omite sin romper la
  secuencia las apps desinstaladas y no usa WorkManager ni AlarmManager: el disparador es el
  receptor de arranque.

### Arreglado

- Las pruebas unitarias ya arrancan en Windows: se quitó `-Dfile.encoding=UTF-8` de
  `gradle.properties`, porque con esa opción el demonio de Gradle escribía en UTF-8 el archivo
  temporal con la ruta de clases de los procesos de prueba y el lanzador de Java 17 lo leía con la
  codificación de Windows; la 'ò' de la carpeta «Apps Mòbil» se corrompía por el camino y las
  pruebas fallaban con `ClassNotFoundException`.
