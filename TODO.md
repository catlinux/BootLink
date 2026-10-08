# Pendiente

## Prioridad alta

- Andamiaje del proyecto Gradle con Compose.
- Capa de datos: modelos, DataStore y consulta de apps lanzables.
- Selector visual de apps y lista configurable.
- Arranque: receptor, servicio y las dos estrategias de lanzamiento.

## Prioridad media

- Ajustes (modo de arranque, retardos) y pantalla de diagnóstico.
- Documentación de instalación y uso.

## Prioridad baja

- Publicación en Google Play: ficha, política de privacidad y firma.
- Icono propio de la aplicación.

## Decisiones pendientes del usuario

- Licencia del proyecto (sin `LICENSE` es «todos los derechos reservados»).
- Primer push al remoto de GitHub.

## Descartado

- `QUERY_ALL_PACKAGES`: permiso restringido en Play que exige declaración con vídeo.
  Se usa `<queries>` con `CATEGORY_LAUNCHER`, que basta para el selector.
- Requerir root o ADB: el objetivo es instalar y listo.
