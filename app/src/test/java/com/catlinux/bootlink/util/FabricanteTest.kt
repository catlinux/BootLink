package com.catlinux.bootlink.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de la detección de [Fabricante] sin dispositivo ni emulador: se prueba [Fabricante.desde]
 * con los textos que escriben las marcas en `Build.MANUFACTURER`, que es lo único que decide la
 * detección (el constructor de la app solo le pasa el valor real del sistema).
 */
class FabricanteTest {

    @Test
    fun `reconoce Xiaomi con sus tres gamas`() {
        assertEquals(Fabricante.XIAOMI, Fabricante.desde("Xiaomi"))
        assertEquals(Fabricante.XIAOMI, Fabricante.desde("XIAOMI"))
        assertEquals(Fabricante.XIAOMI, Fabricante.desde("Redmi"))
        assertEquals(Fabricante.XIAOMI, Fabricante.desde("POCO"))
    }

    @Test
    fun `reconoce Huawei`() {
        assertEquals(Fabricante.HUAWEI, Fabricante.desde("Huawei"))
        assertEquals(Fabricante.HUAWEI, Fabricante.desde("HUAWEI"))
    }

    @Test
    fun `reconoce Oppo con las marcas del mismo grupo`() {
        assertEquals(Fabricante.OPPO, Fabricante.desde("OPPO"))
        assertEquals(Fabricante.OPPO, Fabricante.desde("realme"))
        assertEquals(Fabricante.OPPO, Fabricante.desde("OnePlus"))
    }

    @Test
    fun `reconoce Samsung`() {
        assertEquals(Fabricante.SAMSUNG, Fabricante.desde("samsung"))
        assertEquals(Fabricante.SAMSUNG, Fabricante.desde("Samsung"))
    }

    @Test
    fun `el texto del sistema no tiene por que ser exacto`() {
        // Algunos modelos añaden texto alrededor de la marca; se compara por contenido.
        assertEquals(Fabricante.XIAOMI, Fabricante.desde("Xiaomi Communications Co., Ltd."))
    }

    @Test
    fun `una marca desconocida o ausente cae en Otro`() {
        assertEquals(Fabricante.OTRO, Fabricante.desde("Google"))
        assertEquals(Fabricante.OTRO, Fabricante.desde(""))
        assertEquals(Fabricante.OTRO, Fabricante.desde(null))
    }

    @Test
    fun `solo las marcas conocidas avisan de restricciones de arranque`() {
        assertTrue(Fabricante.XIAOMI.restringeArranque)
        assertTrue(Fabricante.HUAWEI.restringeArranque)
        assertTrue(Fabricante.OPPO.restringeArranque)
        assertTrue(Fabricante.SAMSUNG.restringeArranque)
        assertFalse(Fabricante.OTRO.restringeArranque)
    }
}
