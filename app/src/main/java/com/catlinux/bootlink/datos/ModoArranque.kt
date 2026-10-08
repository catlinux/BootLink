package com.catlinux.bootlink.datos

/**
 * Cómo se lanzan las aplicaciones elegidas cuando el teléfono termina de arrancar.
 *
 * - [CONFIRMAR]: se pregunta al usuario con un diálogo centrado en la pantalla, que abre las apps
 *   si pulsa «Abrir» y no hace nada si pulsa «Cancelar». Es el valor por defecto porque es el modo
 *   más explícito de los tres: nada se abre sin que el usuario lo haya aceptado, y se ve en el
 *   momento. Necesita el permiso de superposición, porque Android no deja abrir la actividad del
 *   diálogo desde segundo plano sin una ventana visible (ver [necesitaSuperposicion]).
 * - [NOTIFICACION]: se publica una notificación y las apps se abren cuando el usuario la toca. No
 *   necesita ningún permiso especial y funciona en cualquier fabricante.
 * - [AUTOMATICO]: se lanzan solas, aprovechando el permiso de superposición (*overlay*).
 */
enum class ModoArranque {
    CONFIRMAR,
    NOTIFICACION,
    AUTOMATICO,
    ;

    /**
     * true si el modo necesita el permiso de superposición (`SYSTEM_ALERT_WINDOW`) para funcionar.
     *
     * [CONFIRMAR] y [AUTOMATICO] abren actividades desde el servicio de arranque, y Android solo se
     * lo permite a quien tiene una ventana visible: sin el permiso, ni el diálogo ni la ventana
     * superpuesta llegan a mostrarse, así que quien elige esos modos tiene que concederlo. El modo
     * [NOTIFICACION] no lo necesita, porque ahí es el propio usuario quien abre BootLink al tocar
     * el aviso, y ese toque ya es la ventana visible.
     */
    val necesitaSuperposicion: Boolean
        get() = this == CONFIRMAR || this == AUTOMATICO

    companion object {
        /** Modo que se usa mientras el usuario no haya elegido otro, y ante un valor desconocido. */
        val PREDETERMINADO: ModoArranque = CONFIRMAR

        /**
         * Convierte en un modo el texto guardado en DataStore. Un valor ausente o desconocido
         * (por ejemplo, escrito por una versión futura de la app) cae en [PREDETERMINADO] en
         * lugar de provocar un error.
         */
        fun desdeNombre(nombre: String?): ModoArranque =
            entries.firstOrNull { it.name == nombre } ?: PREDETERMINADO
    }
}
