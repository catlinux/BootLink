# BootLink — Plan de implementación

Documento ejecutable por una sesión nueva sin contexto previo.

## Qué es

App Android que, al arrancar o reiniciar el teléfono, lanza las aplicaciones que el
usuario haya elegido en una lista visual. Sin root. Caso de uso real del autor: una app
de sincronización de contraseñas y otra de captura de gastos desde notificaciones, que
hoy debe abrir a mano y se le olvidan.

## Restricción central (la que define la arquitectura)

Android bloquea por defecto que una app en segundo plano lance Activities de otra app
(*Background Activity Launch*, BAL). Las únicas excepciones aplicables son:

1. **Overlay**: tener `SYSTEM_ALERT_WINDOW` **y**, desde Android 15, una ventana
   `TYPE_APPLICATION_OVERLAY` **visible** antes de lanzar.
2. **PendingIntent de notificación**: el usuario toca una notificación. No requiere
   permisos especiales y es plenamente compatible con Play Store.

Decisión: **implementar ambos modos**, seleccionables por el usuario en ajustes.
`MODO_NOTIFICACION` es el valor por defecto por ser el robusto y sin permisos.

Fuentes: https://developer.android.com/guide/components/activities/background-starts ·
https://developer.android.com/about/versions/15/behavior-changes-15

## Decisiones de diseño tomadas (no reabrir sin motivo)

- **Nunca `QUERY_ALL_PACKAGES`.** Es permiso restringido en Play y obliga a declaración
  con vídeo. En su lugar, `<queries>` con `ACTION_MAIN` + `CATEGORY_LAUNCHER` y
  `queryIntentActivities()`: devuelve justo las apps lanzables, que es lo que el
  selector necesita. Esto mantiene el selector visual sin arriesgar la publicación.
- **Toda la lógica de elusión del BAL vive en `AppLauncher` y sus dos estrategias.**
  Si Google cambia las reglas, se toca un solo punto.
- Sin red, sin analítica, sin publicidad, sin dependencias de terceros más allá de
  AndroidX. Así la ficha de Play no necesita declarar recogida de datos.
- Persistencia con DataStore (Proto/Preferences), no SharedPreferences.
- `minSdk 26`, `targetSdk 36`, `compileSdk 36`. Play exige API 36 para apps nuevas
  desde el 31-08-2026.

## Arquitectura

```
app/src/main/java/com/catlinux/bootlink/
├── MainActivity.kt                 UI raíz (Compose) + navegación
├── ui/
│   ├── pantallas/ListaPantalla.kt      apps configuradas, reordenar, lanzar ahora
│   ├── pantallas/SelectorPantalla.kt   selector visual con icono, nombre y buscador
│   ├── pantallas/AjustesPantalla.kt    modo de arranque, retardos
│   ├── pantallas/DiagnosticoPantalla.kt  permisos que faltan y fabricante agresivo
│   └── tema/                            Material 3, claro y oscuro
├── datos/
│   ├── AppInstalada.kt             modelo: paquete, etiqueta, icono
│   ├── AppConfigurada.kt           modelo: paquete, orden, retardoMs, activa
│   ├── RepositorioApps.kt          consulta de apps lanzables del sistema
│   └── Preferencias.kt             DataStore: lista configurada + modo + ajustes
├── arranque/
│   ├── ReceptorArranque.kt         BOOT_COMPLETED, LOCKED_BOOT_COMPLETED, QUICKBOOT
│   ├── ServicioArranque.kt         foreground service tipo shortService
│   ├── AppLauncher.kt              orquesta la secuencia con retardos
│   ├── EstrategiaOverlay.kt        overlay visible + lanzamiento automático
│   └── EstrategiaNotificacion.kt   notificación con PendingIntent de un toque
└── util/
    └── Fabricante.kt               detección Xiaomi/Huawei/Samsung/Oppo + ajustes
```

## Hoja de ruta por tareas (cada una delegable)

| # | Tarea | Entregable | Esfuerzo |
|---|---|---|---|
| 1 | Andamiaje Gradle | Proyecto que compila en vacío, Compose, versión 0.1.0 | medio |
| 2 | Datos y repositorio | Modelos, DataStore, consulta de apps lanzables + test | medio |
| 3 | UI selector y lista | Selector con iconos y buscador, lista reordenable | alto |
| 4 | Arranque y lanzamiento | Receptor, servicio y las dos estrategias | alto |
| 5 | Ajustes y diagnóstico | Modo, retardos, avisos de permisos y fabricante | medio |
| 6 | Documentación y Play | README, docs, ficha, privacidad, firma del APK | medio |

## Criterio de «terminado» global

- `gradlew assembleDebug` compila sin errores.
- En un móvil real: se eligen dos apps de la lista, se reinicia y se abren
  (automático con overlay concedido; de un toque en modo notificación).
- `adb shell am broadcast -a android.intent.action.BOOT_COMPLETED -p com.catlinux.bootlink`
  reproduce el arranque sin reiniciar.
- Sin `QUERY_ALL_PACKAGES` en el manifiesto final.

## Pendiente de decisión del usuario

- Licencia (tratar con la Skill `licencias`; sin `LICENSE` es «todos los derechos reservados»).
- Push al remoto `git@github-catlinux:catlinux/BootLink.git` (requiere su aprobación).
