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

### Arreglado

- Las pruebas unitarias ya arrancan en Windows: se quitó `-Dfile.encoding=UTF-8` de
  `gradle.properties`, porque con esa opción el demonio de Gradle escribía en UTF-8 el archivo
  temporal con la ruta de clases de los procesos de prueba y el lanzador de Java 17 lo leía con la
  codificación de Windows; la 'ò' de la carpeta «Apps Mòbil» se corrompía por el camino y las
  pruebas fallaban con `ClassNotFoundException`.
