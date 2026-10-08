# BootLink

Aplicación Android que lanza las apps que elijas cuando enciendes o reinicias el
teléfono. Sin root y sin escribir rutas: eliges las aplicaciones de una lista con sus
iconos, en el orden que quieras.

Nace de un problema concreto: hay apps que deben estar en marcha para hacer su trabajo
—sincronizar una base de datos de contraseñas, leer notificaciones para registrar
gastos— y que hay que abrir a mano después de cada reinicio. Es fácil olvidarse.

> **Estado del proyecto:** en desarrollo (0.1.0). Todavía no es funcional.

## Cómo funciona, y por qué importa

Android no permite que una app en segundo plano abra pantallas de otra app. Es una
protección deliberada del sistema contra el secuestro de la pantalla, y no se puede
«desactivar». Solo hay dos caminos legítimos, y BootLink implementa los dos para que
elijas:

| Modo | Qué hace | Qué necesita |
|---|---|---|
| **Notificación** (predeterminado) | Al arrancar el móvil aparece una notificación; al tocarla se abren tus apps | Nada especial. Robusto y compatible con Google Play |
| **Automático** | Las apps se abren solas al arrancar, mostrando un aviso breve en pantalla | El permiso «Mostrar sobre otras apps», que concedes tú a mano |

El modo automático necesita ese aviso visible porque, desde Android 15, el permiso de
superposición solo exime del bloqueo si hay una ventana superpuesta a la vista. No es un
defecto de BootLink: es la condición que impone el sistema.

## Requisitos

- Android 8.0 (API 26) o superior.
- Para compilarlo: JDK 17 y el SDK de Android con la plataforma 36.

## Instalación

Todavía no hay versiones publicadas. Para compilarlo desde el código:

```sh
git clone git@github-catlinux:catlinux/BootLink.git
cd BootLink
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Uso

1. Abre BootLink y toca **Añadir app**.
2. Elige de la lista las aplicaciones que quieres arrancar.
3. Ordénalas y, si lo necesitas, pon un retardo entre una y otra.
4. En **Ajustes**, elige el modo de arranque.
5. Usa **Lanzar ahora** para comprobarlo sin reiniciar el teléfono.

Si tu móvil es Xiaomi, Huawei, Oppo o Samsung, el sistema puede cerrar BootLink por su
cuenta para ahorrar batería. La pantalla de **Diagnóstico** te dice qué ajuste cambiar en
tu modelo.

## Privacidad

BootLink no se conecta a internet, no recoge datos y no incluye publicidad ni analítica.
La lista de apps que configuras se guarda solo en tu teléfono.

Para mostrarte las apps instaladas consulta únicamente las que tienen icono en el menú de
aplicaciones. No pide el permiso restringido que da acceso a la lista completa de paquetes
del sistema.

## Créditos y licencia

**BootLink** está desarrollado y mantenido por **CatLinux**.
Copyright (C) 2026 CatLinux.

Licencia [WNCL-1.0](LICENSE): puedes usarlo, estudiarlo, modificarlo y compartirlo gratis
con fines no comerciales. Venderlo o cualquier uso comercial requiere permiso por escrito.

Las copias y los forks deben conservar esta atribución y enlazar a
[https://github.com/catlinux/BootLink](https://github.com/catlinux/BootLink).

### Material de terceros

Este proyecto no contiene material de terceros. Las dependencias de Jetpack Compose y
AndroidX se descargan como librerías de Google (Apache 2.0) y conservan su propia
licencia.
