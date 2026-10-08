# Registro de cambios

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/)
y versionado según [SemVer](https://semver.org/lang/es/).

## [Sin publicar]

## [0.5.0] - 2026-10-08

### Añadido

- Tercer modo de arranque, «Preguntar al arrancar», y nuevo modo predeterminado de las instalaciones
  nuevas: cuando el teléfono termina de arrancar, el servicio abre `ConfirmacionActivity`, una
  actividad con interfaz que muestra un diálogo centrado con dos botones, «Abrir» —lanza las apps
  configuradas y se cierra— y «Cancelar» —se cierra sin abrir nada—. Es el modo más explícito de los
  tres: se ve que el arranque ha ocurrido sin depender de que el usuario toque una notificación ni de
  que las apps se abran por sorpresa. `EstrategiaConfirmacion` es la tercera implementación de
  `EstrategiaLanzamiento`, `ModoArranque` pasa a tener tres valores (`CONFIRMAR`, `NOTIFICACION` y
  `AUTOMATICO`) con la propiedad `necesitaSuperposicion`, y `AppLauncher.lanzarAhora()` gana un
  segundo camino: hasta ahora solo lo usaba el aviso de un toque.
- El diálogo se declara en el manifiesto con `excludeFromRecents` y `taskAffinity` vacío (no deja
  entrada en «recientes» ni se mezcla con la tarea de la pantalla principal) y usa un tema de ventana
  translúcido, para que lo que se vea sea el diálogo y no una pantalla de la app.

### Cambiado

- El modo de confirmación usa el mismo permiso de superposición que el automático, porque Android
  solo deja abrir actividades desde segundo plano a quien tiene una ventana visible —aquí, la propia
  actividad del diálogo—; si falta, se recurre al aviso de un toque en lugar de fallar en silencio.
- Los tres modos de la pantalla de ajustes se llaman ahora «Preguntar al arrancar», «Aviso discreto»
  (antes «Avisarme con una notificación») y «Automático» (antes «Abrirlas automáticamente»), y cada
  uno lleva debajo una frase que explica qué hace y qué permiso necesita.
- El aviso de permiso de superposición de los ajustes y la comprobación del diagnóstico salen también
  con el modo de confirmación, no solo con el automático, porque los dos necesitan ese permiso.

## [0.4.2] - 2026-10-08

### Añadido

- Icono propio de BootLink, con el mismo rayo blanco que usan las notificaciones sobre tres
  cuadrados redondeados translúcidos (las apps que el rayo enciende al arrancar) y un fondo con
  degradado morado e índigo. Se declara como icono adaptativo de Android
  (`mipmap-anydpi-v26/ic_launcher.xml` y `ic_launcher_round.xml`, con las capas del degradado y del
  rayo en `drawable/`), así que el lanzador del teléfono decide la forma final —círculo, cuadrado
  redondeado, gota…— y la app deja de llevar el icono genérico del sistema.

## [0.4.1] - 2026-10-08

### Corregido

- El arranque automático no funcionaba en Android 16 y 17: el servicio en primer plano declaraba el
  tipo `shortService`, que esos sistemas no permiten levantar desde un receptor de `BOOT_COMPLETED`,
  así que `startForeground()` lanzaba
  `android.app.ForegroundServiceStartNotAllowedException: FGS type shortService not allowed to start from BOOT_COMPLETED!`.
  Ahora el servicio se declara como `specialUse` —el tipo que sí se admite desde `BOOT_COMPLETED`,
  junto con `location`, `health`, `connectedDevice`, `remoteMessaging` y `systemExempted`—, con el
  permiso `FOREGROUND_SERVICE_SPECIAL_USE` y la propiedad `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` que
  exige su justificación, y `startForeground()` pasa el tipo explícito del API 29 en adelante. Como
  `specialUse` no tiene el límite de tiempo de `shortService`, el servicio sigue cerrándose solo en
  cuanto termina.
- La pantalla principal avisa de los permisos que falten: un aviso sobre la lista, que lleva al
  diagnóstico al tocarlo y se vuelve a comprobar cada vez que la pantalla pasa a primer plano (no
  solo al abrir la app), para que el usuario no tenga que entrar al diagnóstico por su cuenta.

## [0.4.0] - 2026-10-08

### Añadido

- Pantalla de ajustes, a la que se llega con el icono de ajustes de la barra superior de la lista:
  elige entre avisar con una notificación al arrancar y abrir las apps automáticamente. El modo se
  guarda en cuanto se elige, sin bloquear la elección si falta algún permiso.
- Aviso en la propia pantalla de ajustes cuando se elige el modo automático y todavía no está
  concedido el permiso de superposición, con un botón que abre la pantalla del sistema donde se
  concede. Al volver de los ajustes del sistema, el aviso se actualiza solo.
- Pantalla de diagnóstico, a la que se llega con el icono de diagnóstico de la barra superior de la
  lista: comprueba el permiso de notificaciones (en Android 13 o superior) y el de superposición
  (solo si el modo guardado es el automático), explica en palabras qué falta y ofrece un botón para
  arreglarlo.
- Aviso del fabricante en el diagnóstico: en Xiaomi, Huawei, Oppo (y sus marcas derivadas) y
  Samsung se explica que su sistema suele cerrar las apps que se abren solas y se ofrece abrir sus
  ajustes propios de inicio automático o de batería. Si esa pantalla no existe en el teléfono, se
  abre la ficha de BootLink en los ajustes del sistema.
- `util.Fabricante` reconoce la marca a partir de `Build.MANUFACTURER` y `util.AjustesDelSistema`
  reúne los Intents que llevan a los ajustes de notificaciones, de superposición, de la ficha de la
  app y a los propios de cada fabricante, para que las pantallas de Compose no conozcan ni paquetes
  ni componentes del sistema.

## [0.3.0] - 2026-10-08

### Añadido

- Interfaz de la app con Jetpack Compose y Navigation Compose: la pantalla principal muestra la
  lista de apps configuradas y el selector de aplicaciones instaladas, y se navega entre las dos
  con el botón flotante de añadir y la flecha de volver.
- En la lista, cada app configurada se puede reordenar con los botones de subir y bajar, activar y
  desactivar con un interruptor, quitar y ajustar su retardo en milisegundos. Si una app
  configurada ya no está instalada se sigue viendo, con su paquete como nombre, y se puede quitar
  igualmente; cuando no hay ninguna, la pantalla explica que aún no hay apps configuradas en lugar
  de quedarse en blanco.
- El selector lista las aplicaciones instaladas lanzables con su icono y su nombre, con un buscador
  que filtra por nombre sin distinguir mayúsculas y minúsculas, marca las que ya están en la lista
  y añade la elegida al final, volviendo a la pantalla principal.
- `BootLinkViewModel` es el único punto que habla con la capa de datos y con el sistema: las
  pantallas de Compose no instancian DataStore ni PackageManager.

## [0.2.0] - 2026-10-08

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
