package com.catlinux.bootlink.util

import android.os.Build

/**
 * Fabricantes cuyo sistema Android pone trabas al arranque de aplicaciones en segundo plano.
 *
 * Muchos sistemas (MIUI, EMUI, ColorOS, One UI…) cierran las apps que se levantan solas al encender
 * el teléfono para ahorrar batería, así que BootLink puede no llegar a abrir nada. La pantalla de
 * diagnóstico avisa de ello y ofrece abrir los ajustes propios de la marca.
 *
 * La detección es *best-effort*: se hace con las palabras que cada marca escribe en
 * [Build.MANUFACTURER] (Xiaomi, por ejemplo, escribe `Xiaomi` en unos modelos y `Redmi` o `Poco` en
 * otros), sin consultar nada por la red. [OTRO] significa «marca no reconocida», no «marca que no
 * restringe»: un fabricante que no esté en la lista puede restringir el arranque igualmente.
 */
enum class Fabricante {
    /** Xiaomi, Redmi y Poco: MIUI e HyperOS. */
    XIAOMI,

    /** Huawei: EMUI. */
    HUAWEI,

    /** Oppo, Realme y OnePlus: ColorOS y OxygenOS comparten los ajustes de inicio automático. */
    OPPO,

    /** Samsung: One UI, con su propio gestor de batería. */
    SAMSUNG,

    /** Cualquier otra marca, reconocida o no. */
    OTRO,
    ;

    /**
     * true si el sistema de esta marca cierra las apps en segundo plano, así que conviene avisar de
     * que puede hacer falta activar un ajuste propio del teléfono.
     */
    val restringeArranque: Boolean
        get() = this != OTRO

    companion object {
        /**
         * Palabras que identifican cada marca dentro de `Build.MANUFACTURER`, ya en minúsculas. Un
         * mismo fabricante puede aparecer con varias, porque las gamas se anuncian con su nombre.
         */
        private val CLAVES: Map<Fabricante, List<String>> = linkedMapOf(
            XIAOMI to listOf("xiaomi", "redmi", "poco"),
            HUAWEI to listOf("huawei"),
            OPPO to listOf("oppo", "realme", "oneplus"),
            SAMSUNG to listOf("samsung"),
        )

        /**
         * Marca a la que corresponde el texto de `Build.MANUFACTURER`. Se compara en minúsculas y
         * por contenido, porque el valor real cambia entre modelos (`Xiaomi`, `XIAOMI`, `samsung`…).
         * Un texto nulo, vacío o desconocido devuelve [OTRO].
         */
        fun desde(manufacturer: String?): Fabricante {
            val texto = manufacturer?.lowercase().orEmpty()
            if (texto.isEmpty()) return OTRO
            return CLAVES.entries
                .firstOrNull { (_, claves) -> claves.any { palabra -> texto.contains(palabra) } }
                ?.key
                ?: OTRO
        }

        /** Marca del teléfono en el que corre la app. */
        fun actual(): Fabricante = desde(Build.MANUFACTURER)
    }
}
