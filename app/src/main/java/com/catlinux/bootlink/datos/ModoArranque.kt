package com.catlinux.bootlink.datos

/**
 * Cómo se lanzan las aplicaciones elegidas cuando el teléfono termina de arrancar.
 *
 * - [NOTIFICACION]: se publica una notificación y las apps se abren cuando el usuario la toca.
 *   Es el valor por defecto porque no necesita ningún permiso especial y funciona en cualquier
 *   fabricante.
 * - [AUTOMATICO]: se lanzan solas, aprovechando el permiso de superposición (*overlay*).
 */
enum class ModoArranque {
    NOTIFICACION,
    AUTOMATICO,
    ;

    companion object {
        /** Modo que se usa mientras el usuario no haya elegido otro. */
        val PREDETERMINADO: ModoArranque = NOTIFICACION

        /**
         * Convierte en un modo el texto guardado en DataStore. Un valor ausente o desconocido
         * (por ejemplo, escrito por una versión futura de la app) cae en [PREDETERMINADO] en
         * lugar de provocar un error.
         */
        fun desdeNombre(nombre: String?): ModoArranque =
            entries.firstOrNull { it.name == nombre } ?: PREDETERMINADO
    }
}
