package com.catlinux.bootlink.arranque

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log

/**
 * Tercera estrategia: se le pregunta al usuario antes de abrir nada.
 *
 * Abre [ConfirmacionActivity], una actividad con interfaz que muestra un diálogo centrado con dos
 * botones: «Abrir», que lanza las apps configuradas, y «Cancelar», que no lanza nada. Es el modo
 * más claro para quien quiere ver que el arranque ha ocurrido, sin depender de que se toque una
 * notificación ni de que las apps se abran por sorpresa.
 *
 * **Por qué necesita el permiso de superposición.** Android bloquea que un servicio en segundo
 * plano abra actividades, y esta estrategia hace justo eso. El permiso de superposición
 * (`SYSTEM_ALERT_WINDOW`) es una de las excepciones que Android acepta, así que se comprueba antes
 * de intentarlo; si falta, se recurre a [EstrategiaNotificacion], que no necesita ningún permiso.
 * Es el mismo criterio que sigue [EstrategiaOverlay], solo que aquí la ventana visible no es una
 * View superpuesta sino la propia actividad del diálogo.
 *
 * **Quién ejecuta la secuencia.** La función [lanzar] recibe la secuencia de apertura, pero aquí no
 * la ejecuta: todavía no hay ninguna decisión del usuario. La ejecuta [ConfirmacionActivity] si el
 * usuario pulsa «Abrir», llamando a [AppLauncher.lanzarAhora], que es el mismo camino que usa
 * [LanzadorActivity] al tocar la notificación.
 */
class EstrategiaConfirmacion(private val contexto: Context) : EstrategiaLanzamiento {

    override suspend fun lanzar(lanzarApps: suspend () -> Unit): ResultadoLanzamiento {
        if (!Settings.canDrawOverlays(contexto)) {
            Log.w(ETIQUETA_ARRANQUE, "Sin permiso de superposición: se recurre a la notificación.")
            return EstrategiaNotificacion(contexto).lanzar(lanzarApps)
        }
        return try {
            contexto.startActivity(intencion())
            Log.i(
                ETIQUETA_ARRANQUE,
                "Diálogo de confirmación abierto: se espera a que el usuario decida.",
            )
            ResultadoLanzamiento.ESPERANDO_CONFIRMACION
        } catch (error: RuntimeException) {
            // Si el sistema rechaza abrir el diálogo (por ejemplo, porque ha bloqueado el
            // lanzamiento en segundo plano), no se deja al usuario sin nada: aviso de un toque.
            Log.e(ETIQUETA_ARRANQUE, "No se ha podido abrir el diálogo de confirmación.", error)
            EstrategiaNotificacion(contexto).lanzar(lanzarApps)
        }
    }

    /**
     * La actividad va en su propia tarea ([Intent.FLAG_ACTIVITY_NEW_TASK]), porque quien la lanza
     * es un servicio y no una actividad. El resto de comodidades (que no aparezca en «recientes»,
     * que no se mezcle con la pantalla principal) están declaradas en el manifiesto.
     */
    private fun intencion(): Intent =
        Intent(contexto, ConfirmacionActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
