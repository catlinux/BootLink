package com.catlinux.bootlink.datos

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Pruebas de [Preferencias] sin dispositivo ni emulador: el DataStore es el de verdad sobre un
 * archivo de una carpeta temporal, y las corrutinas corren con un despachador de prueba. Las
 * pruebas que necesitan escribir varias veces seguidas sobre el mismo almacén usan
 * [AlmacenEnMemoria], que explica por qué.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PreferenciasTest {

    /** Carpeta temporal que JUnit crea y borra en cada prueba. */
    @get:Rule
    val carpetaTemporal = TemporaryFolder()

    private val despachador = UnconfinedTestDispatcher()

    /** Ámbito que usaría la app en producción; aquí vive lo justo para estas pruebas. */
    private val ambito = CoroutineScope(despachador + SupervisorJob())

    @After
    fun cerrarAmbito() {
        ambito.cancel()
    }

    @Test
    fun sinNadaGuardadoNoHayAppsYElModoEsNotificacion() = runTest(despachador) {
        val preferencias = crearPreferencias()

        assertEquals(emptyList<AppConfigurada>(), preferencias.apps.first())
        assertEquals(ModoArranque.NOTIFICACION, preferencias.modoArranque.first())
    }

    @Test
    fun guardaYRecuperaLaListaDeApps() = runTest(despachador) {
        val preferencias = crearPreferencias()
        // Al guardar, el orden se renumera desde 0; los demás campos se conservan tal cual.
        val apps = listOf(
            app("com.ejemplo.mensajeria", retardoMs = 500L),
            app("com.ejemplo.gastos", orden = 1, activa = false),
        )

        preferencias.guardar(apps)

        assertEquals(apps, preferencias.apps.first())
    }

    @Test
    fun guardarRenumeraElOrdenDeLaLista() = runTest(despachador) {
        val preferencias = crearPreferencias()

        preferencias.guardar(
            listOf(app("com.ejemplo.segunda", orden = 7), app("com.ejemplo.primera", orden = 3)),
        )

        val guardadas = preferencias.apps.first()
        assertEquals(listOf("com.ejemplo.segunda", "com.ejemplo.primera"), guardadas.map { it.paquete })
        assertEquals(listOf(0, 1), guardadas.map { it.orden })
    }

    @Test
    fun anadirPoneLaAppAlFinalYNoDuplicaPaquetes() = runTest(despachador) {
        // Tres escrituras sobre el mismo almacén: ver la nota de [AlmacenEnMemoria].
        val preferencias = crearPreferenciasEnMemoria()

        preferencias.anadir(app("com.ejemplo.primera", orden = 42))
        preferencias.anadir(app("com.ejemplo.segunda"))
        preferencias.anadir(app("com.ejemplo.primera", retardoMs = 900L))

        val guardadas = preferencias.apps.first()
        assertEquals(listOf("com.ejemplo.primera", "com.ejemplo.segunda"), guardadas.map { it.paquete })
        assertEquals(listOf(0, 1), guardadas.map { it.orden })
        assertEquals(0L, guardadas.first().retardoMs)
    }

    @Test
    fun quitarEliminaLaAppYRenumeraElResto() = runTest(despachador) {
        val preferencias = crearPreferenciasEnMemoria()
        preferencias.guardar(
            listOf(app("com.ejemplo.uno"), app("com.ejemplo.dos"), app("com.ejemplo.tres")),
        )

        preferencias.quitar("com.ejemplo.dos")

        val guardadas = preferencias.apps.first()
        assertEquals(listOf("com.ejemplo.uno", "com.ejemplo.tres"), guardadas.map { it.paquete })
        assertEquals(listOf(0, 1), guardadas.map { it.orden })
    }

    @Test
    fun quitarUnaAppQueNoEstaNoCambiaNada() = runTest(despachador) {
        val preferencias = crearPreferenciasEnMemoria()
        preferencias.guardar(listOf(app("com.ejemplo.uno")))

        preferencias.quitar("com.ejemplo.desconocida")

        assertEquals(listOf("com.ejemplo.uno"), preferencias.apps.first().map { it.paquete })
    }

    @Test
    fun reordenarAplicaElOrdenRecibido() = runTest(despachador) {
        val preferencias = crearPreferenciasEnMemoria()
        preferencias.guardar(
            listOf(app("com.ejemplo.uno"), app("com.ejemplo.dos"), app("com.ejemplo.tres")),
        )

        preferencias.reordenar(listOf("com.ejemplo.tres", "com.ejemplo.uno", "com.ejemplo.dos"))

        val guardadas = preferencias.apps.first()
        assertEquals(
            listOf("com.ejemplo.tres", "com.ejemplo.uno", "com.ejemplo.dos"),
            guardadas.map { it.paquete },
        )
        assertEquals(listOf(0, 1, 2), guardadas.map { it.orden })
    }

    @Test
    fun reordenarDejaAlFinalLasAppsQueNoSeMencionan() = runTest(despachador) {
        val preferencias = crearPreferenciasEnMemoria()
        preferencias.guardar(
            listOf(app("com.ejemplo.uno"), app("com.ejemplo.dos"), app("com.ejemplo.tres")),
        )

        preferencias.reordenar(listOf("com.ejemplo.tres"))

        assertEquals(
            listOf("com.ejemplo.tres", "com.ejemplo.uno", "com.ejemplo.dos"),
            preferencias.apps.first().map { it.paquete },
        )
    }

    @Test
    fun guardaYRecuperaElModoDeArranque() = runTest(despachador) {
        val preferencias = crearPreferencias()

        preferencias.guardarModoArranque(ModoArranque.AUTOMATICO)

        assertEquals(ModoArranque.AUTOMATICO, preferencias.modoArranque.first())
    }

    @Test
    fun laListaYElModoSeVenDesdeCualquierInstanciaSobreElMismoAlmacen() = runTest(despachador) {
        val almacen = crearAlmacenEnMemoria()
        val quienEscribe = Preferencias(almacen)
        val quienLee = Preferencias(almacen)

        quienEscribe.guardar(listOf(app("com.ejemplo.uno")))
        quienEscribe.guardarModoArranque(ModoArranque.AUTOMATICO)

        assertEquals(listOf("com.ejemplo.uno"), quienLee.apps.first().map { it.paquete })
        assertEquals(ModoArranque.AUTOMATICO, quienLee.modoArranque.first())
    }

    @Test
    fun unaListaEstropeadaNoRompeLaLectura() = runTest(despachador) {
        val almacen = crearAlmacen()
        // Se escribe basura directamente en la clave, como si el archivo se hubiera dañado.
        almacen.edit { datos ->
            datos[Preferencias.CLAVE_APPS] = "{esto no es una lista de apps}"
        }

        assertEquals(emptyList<AppConfigurada>(), Preferencias(almacen).apps.first())
    }

    @Test
    fun unModoDesconocidoCaeEnElPredeterminado() = runTest(despachador) {
        val almacen = crearAlmacen()
        almacen.edit { datos ->
            datos[Preferencias.CLAVE_MODO] = "MODO_DE_UNA_VERSION_FUTURA"
        }

        assertEquals(ModoArranque.NOTIFICACION, Preferencias(almacen).modoArranque.first())
    }

    /**
     * DataStore de verdad sobre un archivo temporal: no hace falta ningún dispositivo. Solo sirve
     * para escribir una vez; para varias escrituras, ver [AlmacenEnMemoria].
     */
    private fun crearAlmacen(
        nombreArchivo: String = "bootlink.preferences_pb",
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = ambito,
        produceFile = { File(carpetaTemporal.root, nombreArchivo) },
    )

    private fun crearPreferencias(
        nombreArchivo: String = "bootlink.preferences_pb",
    ): Preferencias = Preferencias(crearAlmacen(nombreArchivo))

    /** Almacén en memoria para las pruebas que necesitan escribir varias veces. */
    private fun crearAlmacenEnMemoria(): DataStore<Preferences> = AlmacenEnMemoria()

    private fun crearPreferenciasEnMemoria(): Preferencias = Preferencias(crearAlmacenEnMemoria())

    /** App configurada de ejemplo, con los mismos valores por defecto que el modelo. */
    private fun app(
        paquete: String,
        orden: Int = 0,
        retardoMs: Long = 0L,
        activa: Boolean = true,
    ): AppConfigurada = AppConfigurada(
        paquete = paquete,
        orden = orden,
        retardoMs = retardoMs,
        activa = activa,
    )
}

/**
 * Almacén de DataStore en memoria, para las pruebas que escriben más de una vez sobre el mismo
 * almacén.
 *
 * El DataStore de verdad no sirve para eso en Windows: al escribir renombra el archivo temporal
 * sobre el definitivo, y ese renombrado (`java.io.File.renameTo`, que es lo que usa su
 * implementación para Android) falla si el destino ya existe. La primera escritura crea el archivo
 * y funciona, pero la segunda falla con «Unable to rename … .tmp». En un dispositivo Android sí
 * funciona, porque allí el renombrado sustituye el archivo: el código de producción es correcto,
 * pero no se puede ejercitar dos veces seguidas en esta máquina. Por eso las pruebas que solo
 * escriben una vez usan el DataStore de verdad sobre un archivo temporal, y las que necesitan
 * varias escrituras usan este almacén, que recibe y devuelve los mismos objetos [Preferences] que
 * la app maneja de verdad. Estas pruebas escriben en serie, así que no hace falta el bloqueo que sí
 * tiene el DataStore de verdad.
 */
private class AlmacenEnMemoria(
    inicial: Preferences = emptyPreferences(),
) : DataStore<Preferences> {

    private val estado = MutableStateFlow(inicial)

    override val data: Flow<Preferences> = estado

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences,
    ): Preferences {
        val nuevo = transform(estado.value).toPreferences()
        estado.value = nuevo
        return nuevo
    }
}
