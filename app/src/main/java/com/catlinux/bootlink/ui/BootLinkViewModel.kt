package com.catlinux.bootlink.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.catlinux.bootlink.datos.AppConfigurada
import com.catlinux.bootlink.datos.AppInstalada
import com.catlinux.bootlink.datos.Preferencias
import com.catlinux.bootlink.datos.RepositorioApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Estado de la interfaz de BootLink: la lista de apps configuradas y el catálogo de aplicaciones
 * instaladas que ofrece el selector.
 *
 * Es el único punto por el que pasan las pantallas de Compose: aquí se habla con [Preferencias]
 * (DataStore) y con [RepositorioApps] (PackageManager), y las pantallas solo leen el estado y
 * piden cambios. Ninguna pantalla instancia DataStore ni PackageManager por su cuenta, y las
 * consultas al sistema se hacen fuera del hilo principal.
 */
class BootLinkViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {

    /** Preferencias de la app: la lista configurada vive en DataStore. */
    private val preferencias = Preferencias.desde(aplicacion)

    /** Consulta de las aplicaciones instaladas que se pueden lanzar. */
    private val repositorio = RepositorioApps(aplicacion)

    /**
     * Apps configuradas, en su orden de lanzamiento, tal y como están guardadas. Cualquier
     * escritura (añadir, quitar, reordenar, cambiar el retardo…) vuelve por aquí y repinta la
     * pantalla, sin que la interfaz tenga que refrescar nada a mano.
     */
    val appsConfiguradas: StateFlow<List<AppConfigurada>> = preferencias.apps.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(TIEMPO_SIN_SUSCRIPTORES_MS),
        initialValue = emptyList(),
    )

    private val _appsInstaladas = MutableStateFlow<List<AppInstalada>>(emptyList())

    /** Apps instaladas que se pueden añadir, ya consultadas al sistema. */
    val appsInstaladas: StateFlow<List<AppInstalada>> = _appsInstaladas.asStateFlow()

    private val _cargandoInstaladas = MutableStateFlow(false)

    /**
     * true mientras se está consultando al sistema la lista de apps instaladas. El selector lo usa
     * para avisar de que todavía está buscando.
     */
    val cargandoInstaladas: StateFlow<Boolean> = _cargandoInstaladas.asStateFlow()

    /**
     * Lee del sistema las aplicaciones instaladas lanzables y las publica en [appsInstaladas].
     *
     * El trabajo de PackageManager no es suspendente, así que se hace en [Dispatchers.IO] en lugar
     * del hilo principal. Si ya hay una consulta en marcha no se lanza otra.
     */
    fun cargarAppsInstaladas() {
        if (_cargandoInstaladas.value) return
        viewModelScope.launch {
            _cargandoInstaladas.value = true
            try {
                _appsInstaladas.value = withContext(Dispatchers.IO) { repositorio.appsLanzables() }
            } finally {
                _cargandoInstaladas.value = false
            }
        }
    }

    /**
     * Añade una app al final de la lista. El orden que se pasa es la posición siguiente a la
     * última; si la app ya estaba configurada no se duplica (lo decide [Preferencias.anadir]).
     */
    fun anadir(paquete: String) {
        viewModelScope.launch {
            val orden = listaGuardada().size
            preferencias.anadir(
                AppConfigurada(paquete = paquete, orden = orden, activa = true),
            )
        }
    }

    /** Quita una app de la lista; deja de lanzarse al arrancar. */
    fun quitar(paquete: String) {
        viewModelScope.launch {
            preferencias.quitar(paquete)
        }
    }

    /** Activa o desactiva una app sin sacarla de la lista. */
    fun cambiarActiva(paquete: String, activa: Boolean) {
        viewModelScope.launch {
            preferencias.guardar(
                listaGuardada().map { app ->
                    if (app.paquete == paquete) app.copy(activa = activa) else app
                },
            )
        }
    }

    /** Cambia los milisegundos que se esperan antes de lanzar una app. */
    fun cambiarRetardo(paquete: String, retardoMs: Long) {
        viewModelScope.launch {
            preferencias.guardar(
                listaGuardada().map { app ->
                    if (app.paquete == paquete) app.copy(retardoMs = retardoMs) else app
                },
            )
        }
    }

    /**
     * Deja la lista en el orden de paquetes recibido: es el orden nuevo que produce la pantalla al
     * mover una fila con los botones de subir y bajar.
     */
    fun reordenar(paquetesEnOrden: List<String>) {
        viewModelScope.launch {
            preferencias.reordenar(paquetesEnOrden)
        }
    }

    /**
     * Lista guardada, leída de DataStore en el momento de escribir. Se lee aquí y no del estado de
     * la pantalla porque la escritura es lo que manda: si el estado todavía no se ha cargado
     * (la app acaba de abrirse), guardar sobre él borraría la configuración existente.
     */
    private suspend fun listaGuardada(): List<AppConfigurada> = preferencias.apps.first()

    private companion object {
        /**
         * Tiempo que se mantiene vivo el estado cuando ninguna pantalla lo está mirando. Cinco
         * segundos dan margen de sobra para volver del selector sin releer DataStore.
         */
        const val TIEMPO_SIN_SUSCRIPTORES_MS = 5_000L
    }
}
