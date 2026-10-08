package com.catlinux.bootlink.arranque

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import com.catlinux.bootlink.datos.AppConfigurada
import com.catlinux.bootlink.datos.ModoArranque
import com.catlinux.bootlink.datos.Preferencias
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * Orquesta el lanzamiento de las apps configuradas al terminar de arrancar el teléfono.
 *
 * Es el único punto que conoce las tres formas de esquivar el bloqueo de Android a lanzar
 * actividades desde segundo plano ([EstrategiaLanzamiento]) y el único que abre las apps de
 * verdad. Si Google cambia las reglas, se toca aquí y en las estrategias, no en el resto de la app.
 *
 * La secuencia respeta el orden guardado, el retardo de cada app y su marca de activa, y las apps
 * que el usuario haya desinstalado se omiten sin romper el resto de la secuencia.
 *
 * @param contexto contexto con el que se consultan las preferencias y el sistema; conviene pasar
 *   el contexto de la aplicación, porque sobrevive a cualquier actividad y servicio.
 */
class AppLauncher(private val contexto: Context) {

    /** Configuración guardada por el usuario: lista de apps y modo de arranque. */
    private val preferencias = Preferencias.desde(contexto)

    private val gestorPaquetes: PackageManager
        get() = contexto.packageManager

    /**
     * Se llama al terminar el arranque del teléfono, desde [ServicioArranque].
     *
     * Lee la configuración y elige estrategia según el modo guardado. Los retardos configurados por
     * el usuario son su responsabilidad: la secuencia puede durar lo que sumen, porque el servicio
     * `specialUse` que la hospeda (ver [ServicioArranque]) no tiene límite de tiempo.
     *
     * @return el desenlace de la estrategia elegida, o [ResultadoLanzamiento.NADA_QUE_LANZAR] si no
     *   hay ninguna app activa.
     */
    suspend fun lanzarAlArrancar(): ResultadoLanzamiento {
        val apps = appsActivas()
        if (apps.isEmpty()) {
            Log.i(ETIQUETA_ARRANQUE, "No hay apps activas que abrir: no se hace nada.")
            return ResultadoLanzamiento.NADA_QUE_LANZAR
        }
        Log.i(ETIQUETA_ARRANQUE, "Apps activas para el arranque: ${apps.size}.")
        return estrategiaElegida().lanzar { lanzarEnSecuencia(apps) }
    }

    /**
     * Abre las apps ahora mismo, sin estrategia ninguna.
     *
     * La usa [LanzadorActivity] cuando el usuario toca la notificación de aviso y
     * [ConfirmacionActivity] cuando el usuario pulsa «Abrir» en el diálogo del modo de
     * confirmación: en los dos casos BootLink ya está en primer plano y tiene ventana visible, así
     * que el sistema sí permite lanzar las demás apps.
     *
     * @return [ResultadoLanzamiento.LANZADAS], o [ResultadoLanzamiento.NADA_QUE_LANZAR] si no hay
     *   apps activas.
     */
    suspend fun lanzarAhora(): ResultadoLanzamiento {
        val apps = appsActivas()
        if (apps.isEmpty()) {
            Log.i(ETIQUETA_ARRANQUE, "El usuario ha pedido abrir las apps, pero no hay activas.")
            return ResultadoLanzamiento.NADA_QUE_LANZAR
        }
        lanzarEnSecuencia(apps)
        return ResultadoLanzamiento.LANZADAS
    }

    /**
     * Estrategia que corresponde al modo guardado: una por cada forma de arrancar.
     *
     * Los modos que necesitan el permiso de superposición ([ModoArranque.necesitaSuperposicion]: el
     * de confirmación y el automático) solo se intentan si el permiso está concedido **en este
     * momento**: aunque se concediera antes, el usuario puede retirarlo, y sin una ventana visible
     * el sistema ignora el lanzamiento. En ese caso, y en el modo de aviso discreto, se avisa con
     * una notificación en lugar de fallar en silencio.
     */
    private suspend fun estrategiaElegida(): EstrategiaLanzamiento {
        val modo = preferencias.modoArranque.first()
        if (modo.necesitaSuperposicion && !Settings.canDrawOverlays(contexto)) {
            Log.w(
                ETIQUETA_ARRANQUE,
                "Modo ${modo.name} sin permiso de superposición: se recurre a la notificación.",
            )
            return EstrategiaNotificacion(contexto)
        }
        return when (modo) {
            ModoArranque.NOTIFICACION -> {
                Log.i(ETIQUETA_ARRANQUE, "Modo ${modo.name}: se avisa con una notificación.")
                EstrategiaNotificacion(contexto)
            }

            ModoArranque.CONFIRMAR -> {
                Log.i(ETIQUETA_ARRANQUE, "Modo ${modo.name}: se pregunta si abrir las apps.")
                EstrategiaConfirmacion(contexto)
            }

            ModoArranque.AUTOMATICO -> {
                Log.i(ETIQUETA_ARRANQUE, "Modo ${modo.name}: se lanza con ventana superpuesta.")
                EstrategiaOverlay(contexto)
            }
        }
    }

    /** Apps configuradas y activas, en el orden guardado. */
    private suspend fun appsActivas(): List<AppConfigurada> =
        preferencias.apps.first()
            .filter { it.activa }
            .sortedBy { it.orden }

    /**
     * Abre las apps una detrás de otra.
     *
     * El filtro y el orden se repiten aquí a propósito: esta secuencia también se ejecuta desde
     * [LanzadorActivity] y [ConfirmacionActivity], que no pasan por [lanzarAlArrancar], y así el
     * comportamiento es idéntico se entre por donde se entre.
     */
    private suspend fun lanzarEnSecuencia(apps: List<AppConfigurada>) {
        apps.filter { it.activa }
            .sortedBy { it.orden }
            .forEach { app ->
                if (app.retardoMs > 0L) delay(app.retardoMs)
                abrir(app.paquete)
            }
    }

    /**
     * Abre una app por su paquete.
     *
     * Se usa `getLaunchIntentForPackage()`, que devuelve la intención con la que el sistema abre
     * esa app desde el menú de aplicaciones; el bloque `<queries>` del manifiesto es lo que permite
     * consultarlo sin el permiso restringido de Google Play. Si la app ya no está instalada el
     * resultado es nulo y se omite: que el usuario desinstale algo no puede romper el arranque.
     */
    private fun abrir(paquete: String) {
        val intencion = gestorPaquetes.getLaunchIntentForPackage(paquete)
        if (intencion == null) {
            Log.w(ETIQUETA_ARRANQUE, "La app $paquete ya no está instalada: se omite.")
            return
        }
        // Obligatorio al lanzar desde fuera de una actividad: la app se abre en su propia tarea.
        intencion.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            contexto.startActivity(intencion)
            Log.i(ETIQUETA_ARRANQUE, "App abierta: $paquete.")
        } catch (error: ActivityNotFoundException) {
            // El paquete está, pero el sistema ya no sabe abrirlo (por ejemplo, una app
            // deshabilitada a medias). Se omite y la secuencia continúa.
            Log.w(ETIQUETA_ARRANQUE, "El sistema no puede abrir $paquete: se omite.", error)
        }
    }
}
