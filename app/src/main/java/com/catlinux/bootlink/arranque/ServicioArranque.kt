package com.catlinux.bootlink.arranque

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.catlinux.bootlink.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano que abre las apps configuradas cuando el teléfono termina de arrancar.
 *
 * Lo levanta [ReceptorArranque] y existe por dos motivos:
 *
 * - Android da por bueno el trabajo que hace un servicio en primer plano tras el arranque, así que
 *   el sistema no mata a BootLink a mitad de la secuencia.
 * - El tipo declarado es `shortService`, el único pensado para una tarea corta que no se puede
 *   aplazar, y que junto con `specialUse` es lo que Android 15 permite lanzar desde un receptor de
 *   `BOOT_COMPLETED`. Los tipos `dataSync`, `camera`, `mediaPlayback`, `phoneCall`,
 *   `mediaProjection` y `microphone` provocarían `ForegroundServiceStartNotAllowedException`.
 *   `shortService` no necesita permiso de tipo propio: basta con `FOREGROUND_SERVICE`.
 *
 * El trabajo de verdad está en [AppLauncher]; aquí solo se le da un rato de primer plano y se
 * cierra el servicio en cuanto termina, para no acercarse al límite de unos tres minutos.
 */
class ServicioArranque : Service() {

    /** Ámbito del trabajo en curso; se cancela al cerrar el servicio. */
    private val trabajo = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Se entra aquí en cuanto el receptor arranca el servicio. La notificación mínima obligatoria se
     * publica ya, antes de leer nada, porque el sistema exige llamar a `startForeground()` en los
     * primeros segundos y si no, cierra la app.
     */
    override fun onCreate() {
        super.onCreate()
        startForeground(ID_NOTIFICACION, notificacion())
    }

    override fun onStartCommand(intencion: Intent?, opciones: Int, idArranque: Int): Int {
        val accion = intencion?.action
        trabajo.launch {
            try {
                Log.i(ETIQUETA_ARRANQUE, "Arranque ($accion): se lee la configuración guardada.")
                AppLauncher(applicationContext).lanzarAlArrancar()
            } catch (error: Exception) {
                // Un almacén aún cerrado por el arranque o un permiso retirado no pueden tumbar la
                // app: se registra y el servicio se cierra igual.
                Log.e(ETIQUETA_ARRANQUE, "No se ha podido completar el arranque.", error)
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(idArranque)
            }
        }
        // Si el sistema mata el servicio no interesa recrearlo: ese arranque ya pasó.
        return START_NOT_STICKY
    }

    /**
     * El `shortService` tiene un límite de unos tres minutos (retardos muy largos configurados por
     * el usuario podrían agotarlo). El sistema avisa aquí antes de dar la app por parada, y lo único
     * que hay que hacer es cerrar el servicio sin más. Esta variante es la de Android 14; en Android
     * 15 el sistema llama a la de dos parámetros, que no delega en esta.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onTimeout(idArranque: Int) {
        Log.w(ETIQUETA_ARRANQUE, "Se ha agotado el tiempo del servicio de arranque: se cierra.")
        cerrarServicio(idArranque)
    }

    /** Variante de Android 15 en adelante, que además informa del tipo de servicio en primer plano. */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onTimeout(idArranque: Int, tipoServicio: Int) {
        Log.w(
            ETIQUETA_ARRANQUE,
            "Se ha agotado el tiempo del servicio de arranque (tipo $tipoServicio): se cierra.",
        )
        cerrarServicio(idArranque)
    }

    /** Cierre ordenado: se cancela el trabajo a medias, se quita el aviso y se para el servicio. */
    private fun cerrarServicio(idArranque: Int) {
        trabajo.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf(idArranque)
    }

    override fun onDestroy() {
        trabajo.cancel()
        super.onDestroy()
    }

    /** El servicio no ofrece enlace: lo lanza el sistema, nadie se conecta a él. */
    override fun onBind(intencion: Intent?): IBinder? = null

    /** Notificación discreta y sin sonido que Android exige mientras el servicio está en marcha. */
    private fun notificacion(): Notification {
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(
            NotificationChannel(
                ID_CANAL,
                getString(R.string.arranque_canal_servicio),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        return NotificationCompat.Builder(this, ID_CANAL)
            .setSmallIcon(R.drawable.ic_aviso_arranque)
            .setContentTitle(getString(R.string.arranque_servicio_titulo))
            .setContentText(getString(R.string.arranque_servicio_texto))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private companion object {
        /** Canal propio del servicio: importancia baja, sin sonido ni vibración. */
        const val ID_CANAL = "arranque_servicio"

        /** Identificador fijo de la notificación obligatoria del servicio. */
        const val ID_NOTIFICACION = 1002
    }
}
