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

- Publicación en Google Play: pendiente de comprobar si la cuenta de desarrollador de
  Marc es anterior al 13-11-2023 (libraría del requisito de 12 testers durante 14 días
  seguidos antes de producción). Si no lo es, hace falta reclutar testers o aparcarlo.
  Falta también: ficha de Play, política de privacidad publicada en una URL, firma de
  release propia (hoy solo existe el APK debug) y las declaraciones de permisos
  sensibles (`SYSTEM_ALERT_WINDOW`, `specialUse`, ya justificado en el manifiesto).

## Decisiones pendientes del usuario

- Nada pendiente de licencia ni de push: ya resuelto.

## Descartado

- "Minimizar tras abrir" (traer BootLink de vuelta a primer plano tras lanzar cada
  app, para no dejarla en pantalla): Android no permite minimizar ni cerrar la tarea
  de otra app desde fuera, por diseño de seguridad. Lo único posible sería lanzar y,
  tras un margen, volver a traer BootLink (o el launcher) a primer plano, tapando la
  app visualmente sin detener su proceso. El usuario lo descartó por ahora: prefiere
  esperar una solución mejor en vez de ese parche parcial.

- `QUERY_ALL_PACKAGES`: permiso restringido en Play que exige declaración con vídeo.
  Se usa `<queries>` con `CATEGORY_LAUNCHER`, que basta para el selector.
- Requerir root o ADB: el objetivo es instalar y listo.
