package com.catlinux.bootlink.ui

import android.Manifest
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.catlinux.bootlink.datos.AppConfigurada
import com.catlinux.bootlink.datos.AppInstalada
import com.catlinux.bootlink.datos.ModoArranque
import com.catlinux.bootlink.datos.Preferencias
import com.catlinux.bootlink.datos.RepositorioApps
import com.catlinux.bootlink.util.AjustesDelSistema
import com.catlinux.bootlink.util.Fabricante
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

    /** Nombre de paquete de BootLink: es lo que piden los Intents de los ajustes del sistema. */
    private val paquete: String = aplicacion.packageName

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
     * Modo de arranque guardado: aviso al usuario o lanzamiento automático. Es lo que elige la
     * pantalla de ajustes y lo que mira la de diagnóstico para saber si el permiso de superposición
     * hace falta.
     */
    val modoArranque: StateFlow<ModoArranque> = preferencias.modoArranque.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(TIEMPO_SIN_SUSCRIPTORES_MS),
        initialValue = ModoArranque.PREDETERMINADO,
    )

    private val _diagnostico = MutableStateFlow(estadoDelSistema())

    /**
     * Permisos y fabricante tal y como los ve el sistema ahora mismo. La pantalla de diagnóstico lo
     * muestra entero y la de ajustes lo usa para avisar de que falta la superposición. Las dos lo
     * vuelven a leer con [actualizarDiagnostico] cada vez que se vuelve a ellas, porque para
     * conceder un permiso el usuario sale de la app y tarda un rato en volver.
     */
    val diagnostico: StateFlow<EstadoDiagnostico> = _diagnostico.asStateFlow()

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
     * Guarda el modo de arranque elegido. La falta de permisos no bloquea la elección: si el
     * usuario elige el automático sin la superposición concedida, se le avisa y se le ofrece
     * concederla, pero el modo queda guardado igualmente.
     */
    fun cambiarModoArranque(modo: ModoArranque) {
        viewModelScope.launch {
            preferencias.guardarModoArranque(modo)
        }
    }

    /**
     * Vuelve a leer del sistema los permisos y el fabricante, y publica el resultado en
     * [diagnostico]. Se llama al abrir las pantallas de ajustes y de diagnóstico y cada vez que se
     * vuelve a ellas desde los ajustes del sistema.
     */
    fun actualizarDiagnostico() {
        _diagnostico.value = estadoDelSistema()
    }

    /** Abre los ajustes de notificaciones de la app (el permiso POST_NOTIFICATIONS, Android 13+). */
    fun abrirAjustesNotificaciones() {
        abrirAjuste(AjustesDelSistema.intentNotificaciones(paquete))
    }

    /** Abre la pantalla donde se concede el permiso de superposición del modo automático. */
    fun abrirAjustesSuperposicion() {
        abrirAjuste(AjustesDelSistema.intentSuperposicion(paquete))
    }

    /**
     * Abre los ajustes propios del fabricante para permitir el arranque de las apps en segundo plano
     * (en Samsung, su gestor de batería). Si este modelo no tiene esa pantalla, se abre la ficha de
     * BootLink en los ajustes del sistema, que siempre existe.
     */
    fun abrirAjustesFabricante() {
        val respaldo = AjustesDelSistema.intentDetallesApp(paquete)
        val ajustesDelFabricante = AjustesDelSistema.intentFabricante(_diagnostico.value.fabricante)
        abrirAjuste(ajustesDelFabricante ?: respaldo, respaldo)
    }

    /**
     * Lee los permisos que necesita la app y la marca del teléfono. Son comprobaciones baratas (una
     * consulta de permisos, una de operaciones de la app y las constantes de compilación), así que
     * no hace falta salir del hilo principal.
     */
    private fun estadoDelSistema(): EstadoDiagnostico {
        val contexto = getApplication<Application>()
        val exigeNotificaciones = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        return EstadoDiagnostico(
            notificacionesExigidas = exigeNotificaciones,
            // Hasta Android 13 no hay permiso que pedir: se da por hecho que se pueden publicar.
            notificacionesPermitidas = !exigeNotificaciones || ContextCompat.checkSelfPermission(
                contexto,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED,
            superposicionPermitida = Settings.canDrawOverlays(contexto),
            fabricante = Fabricante.actual(),
            nombreFabricante = Build.MANUFACTURER.replaceFirstChar { letra -> letra.uppercase() },
        )
    }

    /**
     * Lanza una pantalla de ajustes del sistema. El contexto de la aplicación no es una actividad,
     * así que el Intent necesita su propia tarea. Si esa pantalla no existe en este teléfono (los
     * ajustes de cada marca cambian de un modelo a otro), se abre [respaldo] en su lugar en vez de
     * dejar al usuario sin salida.
     */
    private fun abrirAjuste(intent: Intent, respaldo: Intent? = null) {
        val contexto = getApplication<Application>()
        try {
            contexto.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (error: ActivityNotFoundException) {
            if (respaldo != null) {
                contexto.startActivity(respaldo.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
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

/**
 * Estado del sistema que necesitan las pantallas de ajustes y de diagnóstico.
 *
 * @param notificacionesExigidas true si esta versión de Android pide el permiso de notificaciones a
 *   mano (Android 13 o superior).
 * @param notificacionesPermitidas true si la app puede publicar notificaciones. En versiones
 *   anteriores a Android 13 no hay permiso que pedir, así que también es true.
 * @param superposicionPermitida true si el usuario ha concedido a BootLink dibujar sobre otras apps.
 * @param fabricante la marca del teléfono, para saber si su sistema restringe el arranque.
 * @param nombreFabricante la marca tal y como la declara el sistema, para los avisos al usuario.
 */
data class EstadoDiagnostico(
    val notificacionesExigidas: Boolean,
    val notificacionesPermitidas: Boolean,
    val superposicionPermitida: Boolean,
    val fabricante: Fabricante,
    val nombreFabricante: String,
)
