package com.catlinux.bootlink.arranque

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import com.catlinux.bootlink.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Estrategia automática: se muestra un aviso superpuesto y las apps se abren solas.
 *
 * Android solo deja abrir la actividad de otra app desde segundo plano a quien tiene una ventana
 * visible. Con el permiso de superposición (`SYSTEM_ALERT_WINDOW`) se puede poner una ventana de
 * tipo [WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY], y **desde Android 15 ese permiso ya
 * no basta**: la ventana tiene que estar además visible antes de lanzar. Por eso aquí no se lanza
 * nada al añadir la vista: se espera a que el sistema confirme que se ve, y solo entonces se
 * ejecuta la secuencia de [AppLauncher].
 *
 * La vista se retira siempre al terminar, también si algo falla, y si no se puede mostrar (sin
 * permiso, sin `WindowManager` o sin llegar a ser visible) se recurre a [EstrategiaNotificacion] en
 * lugar de dejar al usuario sin nada.
 *
 * @param contexto contexto con el que se pide el `WindowManager` y se comprueba el permiso.
 */
class EstrategiaOverlay(private val contexto: Context) : EstrategiaLanzamiento {

    override suspend fun lanzar(lanzarApps: suspend () -> Unit): ResultadoLanzamiento =
        withContext(Dispatchers.Main.immediate) { lanzarConVentanaSuperpuesta(lanzarApps) }

    /**
     * Trabajo real de la estrategia, siempre en el hilo principal.
     *
     * No es un capricho: crear la ventana de una vista exige un `Looper`, y el servicio lee la
     * configuración en un hilo de trabajo que no lo tiene. Además la ventana tiene que seguir viva
     * mientras se abren las apps, así que toda la secuencia se queda en este hilo (los retardos son
     * suspensiones de corrutina, no bloquean la interfaz).
     */
    private suspend fun lanzarConVentanaSuperpuesta(
        lanzarApps: suspend () -> Unit,
    ): ResultadoLanzamiento {
        // Segunda comprobación del permiso: [AppLauncher] ya lo mira al elegir estrategia, pero el
        // usuario puede haberlo retirado entre medias y aquí no cuesta nada.
        if (!Settings.canDrawOverlays(contexto)) {
            Log.w(ETIQUETA_ARRANQUE, "Sin permiso de superposición: se recurre a la notificación.")
            return avisarEnSuLugar(lanzarApps)
        }

        val aviso = crearAviso()
        var gestorVentanas: WindowManager? = null
        var anadido = false
        return try {
            val gestor = contexto.getSystemService(WindowManager::class.java)
            gestorVentanas = gestor
            gestor.addView(aviso, parametros())
            anadido = true
            if (esperarAVisible(aviso)) {
                Log.i(ETIQUETA_ARRANQUE, "Ventana superpuesta visible: se abren las apps.")
                lanzarApps()
                ResultadoLanzamiento.LANZADAS
            } else {
                // El sistema no la ha dado por visible: lanzar ahora lo ignoraría en silencio.
                Log.w(ETIQUETA_ARRANQUE, "La ventana superpuesta no se ha hecho visible a tiempo.")
                avisarEnSuLugar(lanzarApps)
            }
        } catch (error: RuntimeException) {
            // SecurityException si el permiso se retiró justo ahora, BadTokenException si la
            // ventana no se pudo crear. En ninguno de los dos casos hay que dejar al usuario a
            // oscuras: se avisa con una notificación.
            Log.e(ETIQUETA_ARRANQUE, "No se ha podido mostrar la ventana superpuesta.", error)
            avisarEnSuLugar(lanzarApps)
        } finally {
            // La ventana se retira siempre, con éxito o con error, antes de devolver el resultado.
            val gestor = gestorVentanas
            if (anadido && gestor != null) retirarVista(gestor, aviso)
        }
    }

    /** Recurso de emergencia: el aviso de un toque, que no necesita ningún permiso especial. */
    private suspend fun avisarEnSuLugar(lanzarApps: suspend () -> Unit): ResultadoLanzamiento =
        EstrategiaNotificacion(contexto).lanzar(lanzarApps)

    /**
     * La vista superpuesta: un texto discreto sobre fondo oscuro, centrado arriba del todo y con
     * margen de sobra para no tapar la barra de estado. Se construye a mano porque no lleva nada
     * de Compose y así no hace falta ni un archivo de diseño.
     */
    private fun crearAviso(): View {
        val densidad = contexto.resources.displayMetrics.density
        val margen = (MARGEN_DP * densidad).toInt()
        val texto = TextView(contexto).apply {
            setText(R.string.arranque_superposicion_texto)
            setTextColor(Color.WHITE)
            textSize = TAMANO_TEXTO_SP
            setPadding(margen, margen / 2, margen, margen / 2)
            background = GradientDrawable().apply {
                setColor(COLOR_FONDO)
                cornerRadius = MARGEN_DP * densidad
            }
        }
        return FrameLayout(contexto).apply { addView(texto) }
    }

    /**
     * Parámetros de la ventana superpuesta: no recibe toques ni el foco, porque es solo un aviso y
     * los toques del usuario deben seguir llegando a lo que hay debajo, y el fondo translúcido es
     * lo que permite que se vean las esquinas redondeadas del aviso.
     */
    private fun parametros(): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (MARGEN_DP * contexto.resources.displayMetrics.density).toInt()
        }

    /**
     * Espera a que la ventana esté de verdad en pantalla, que es la condición que pone Android 15
     * para poder lanzar actividades desde segundo plano.
     *
     * En vez de fiarse de que `addView()` haya vuelto, se comprueba el estado real de la vista: la
     * ventana cuenta como visible solo cuando está enganchada a la ventana de la app, con
     * `getWindowVisibility()` en `VISIBLE` y la vista realmente mostrada. La espera tiene tope para
     * que un fabricante raro no deje el servicio colgado.
     *
     * @return `true` si la ventana ha llegado a ser visible antes del tope de tiempo.
     */
    private suspend fun esperarAVisible(aviso: View): Boolean =
        withTimeoutOrNull(TIEMPO_MAXIMO_VISIBLE_MS) {
            while (!visibleEnPantalla(aviso)) {
                delay(INTERVALO_COMPROBACION_MS)
            }
            true
        } ?: false

    /** Las tres condiciones que dan una vista por visible: enganchada, mostrada y con ventana. */
    private fun visibleEnPantalla(aviso: View): Boolean =
        aviso.isAttachedToWindow &&
            aviso.windowVisibility == View.VISIBLE &&
            aviso.isShown

    /** Retira la vista; si el sistema ya la había quitado por su cuenta, no es un fallo. */
    private fun retirarVista(gestorVentanas: WindowManager, aviso: View) {
        try {
            gestorVentanas.removeView(aviso)
            Log.i(ETIQUETA_ARRANQUE, "Ventana superpuesta retirada.")
        } catch (error: IllegalArgumentException) {
            Log.w(ETIQUETA_ARRANQUE, "La ventana superpuesta ya no estaba puesta.", error)
        }
    }

    private companion object {
        /** Tope de espera a que la ventana superpuesta se vea; si no, se avisa con notificación. */
        const val TIEMPO_MAXIMO_VISIBLE_MS = 2_000L

        /** Cada cuánto se comprueba la visibilidad mientras se espera. */
        const val INTERVALO_COMPROBACION_MS = 20L

        /** Margen y radio de las esquinas del aviso, en píxeles independientes de densidad. */
        const val MARGEN_DP = 12f

        /** Tamaño del texto del aviso; el mismo que el cuerpo de texto de Material 3. */
        const val TAMANO_TEXTO_SP = 14f

        /** Gris casi opaco: se lee en cualquier fondo sin tapar del todo lo que hay debajo. */
        val COLOR_FONDO: Int = Color.argb(230, 32, 32, 32)
    }
}
