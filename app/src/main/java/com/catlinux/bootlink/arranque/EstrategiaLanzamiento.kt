package com.catlinux.bootlink.arranque

/**
 * Etiqueta única de todo el paquete `arranque` en Logcat.
 *
 * Un arranque completo deja un rastro corto y fácil de seguir con
 * `adb logcat -s BootLinkArranque`; que todas las clases escriban con la misma etiqueta es lo que
 * permite leerlo como una sola historia (qué estrategia se eligió, qué apps se abrieron y cuáles
 * se omitieron).
 */
internal const val ETIQUETA_ARRANQUE = "BootLinkArranque"

/**
 * Cómo se consigue que el teléfono permita abrir las apps configuradas.
 *
 * Android bloquea por defecto que una app en segundo plano lance la actividad de otra app
 * (*Background Activity Launch*). Hay tres vías válidas, y esta interfaz es lo único que
 * [AppLauncher] conoce de ellas:
 *
 * - [EstrategiaNotificacion]: se avisa con una notificación y las apps se abren cuando el usuario
 *   la toca. No necesita permisos especiales y es la que admite Google Play sin preguntas.
 * - [EstrategiaConfirmacion]: se abre un diálogo que pregunta si abrir las apps; si el usuario
 *   acepta, las abre una actividad de BootLink ([ConfirmacionActivity]), que ya está en primer
 *   plano. Necesita el permiso de superposición, que es lo que permite abrir esa actividad desde
 *   segundo plano.
 * - [EstrategiaOverlay]: se muestra una ventana superpuesta de tipo
 *   [android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY] y, mientras es visible, la
 *   app puede lanzar las demás. Necesita el permiso de superposición.
 *
 * Añadir una estrategia nueva (por ejemplo, para un fabricante que se salte las reglas) no obliga
 * a tocar el orquestador: basta con implementar esta interfaz.
 */
interface EstrategiaLanzamiento {

    /**
     * Ejecuta la estrategia para que las apps acaben abiertas.
     *
     * @param lanzarApps secuencia real de apertura: abre las apps una detrás de otra, en orden y
     *   con su retardo. La estrategia la ejecuta cuando el teléfono ya está en condiciones de
     *   permitirlo (en el modo automático, cuando la ventana superpuesta es visible) y no la toca
     *   cuando la decisión es del usuario: en el modo de notificación, porque el que abre BootLink
     *   es él al tocar el aviso ([LanzadorActivity]), y en el de confirmación, porque primero tiene
     *   que contestar al diálogo ([ConfirmacionActivity]).
     * @return qué ha pasado, para poder registrarlo y, más adelante, mostrarlo en el diagnóstico.
     */
    suspend fun lanzar(lanzarApps: suspend () -> Unit): ResultadoLanzamiento
}

/** Desenlace de una estrategia de lanzamiento, útil para el registro y el diagnóstico. */
enum class ResultadoLanzamiento {
    /** Las apps se han abierto sin que el usuario tuviera que hacer nada (modo automático). */
    LANZADAS,

    /** Se ha dejado una notificación: las apps se abrirán cuando el usuario la toque. */
    AVISADO_CON_NOTIFICACION,

    /** No había nada que abrir: la lista está vacía o todas las apps están desactivadas. */
    NADA_QUE_LANZAR,

    /**
     * No se ha podido avisar porque falta el permiso de notificaciones, que en Android 13 o
     * superior concede el usuario. La pantalla de diagnóstico lo reflejará.
     */
    SIN_PERMISO_NOTIFICACIONES,

    /**
     * Se ha preguntado al usuario con un diálogo ([ConfirmacionActivity]) y todavía no ha
     * contestado: las apps se abrirán solo si pulsa «Abrir». Es el desenlace del modo de
     * confirmación, el único en el que la decisión no la toma ni BootLink ni una notificación.
     */
    ESPERANDO_CONFIRMACION,
}
