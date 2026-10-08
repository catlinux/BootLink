# Pendiente

Las seis tareas del plan inicial (andamiaje, datos, arranque, interfaz, ajustes y
diagnóstico) están completas: BootLink ya compila, arranca con el teléfono y lanza
las apps configuradas con las dos estrategias. Falta probarlo en un dispositivo real.

## Prioridad alta

- Probar en un teléfono real: reiniciar y comprobar que se abren las apps elegidas,
  en modo notificación y en modo automático.
- Icono propio de la aplicación (hoy usa el icono por defecto de Android Studio).

## Prioridad media

- Comprobar el aviso de fabricante (Xiaomi/Huawei/Oppo/Samsung) en un teléfono de
  cada marca si se puede: los Intents a los ajustes propios no están verificados
  en dispositivo real, solo revisados en el código.

## Prioridad baja

- Publicación en Google Play: ficha, política de privacidad y firma de la app.

## Decisiones pendientes del usuario

- Nada pendiente de licencia ni de push: ya resuelto.

## Descartado

- `QUERY_ALL_PACKAGES`: permiso restringido en Play que exige declaración con vídeo.
  Se usa `<queries>` con `CATEGORY_LAUNCHER`, que basta para el selector.
- Requerir root o ADB: el objetivo es instalar y listo.
