package com.catlinux.bootlink.arranque

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Recibe el aviso de que el teléfono ha terminado de arrancar y pone en marcha el trabajo.
 *
 * Atiende tres acciones porque cada familia de teléfonos avisa a su manera:
 *
 * - `BOOT_COMPLETED`: el arranque normal, el que avisa todo el mundo.
 * - `LOCKED_BOOT_COMPLETED`: el arranque antes de que el usuario desbloquee. El receptor es
 *   `directBootAware` y por eso se le entrega, pero la configuración del usuario vive en el
 *   almacén cifrado con sus credenciales, así que todavía no se puede leer. No se hace nada: unos
 *   segundos después llegará `BOOT_COMPLETED`, ya desbloqueado, y ese es el que abre las apps.
 * - `QUICKBOOT_POWERON`: el «reinicio rápido» que usan algunos fabricantes (HTC, LG y compañía)
 *   y que nunca envía `BOOT_COMPLETED`.
 *
 * El receptor no hace trabajo pesado: levanta [ServicioArranque], que es quien lee la
 * configuración y decide con qué estrategia se abren las apps.
 */
class ReceptorArranque : BroadcastReceiver() {

    override fun onReceive(contexto: Context, intencion: Intent) {
        when (val accion = intencion.action) {
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> Log.i(
                ETIQUETA_ARRANQUE,
                "El teléfono ha arrancado todavía bloqueado: se espera a BOOT_COMPLETED.",
            )

            Intent.ACTION_BOOT_COMPLETED, ACCION_QUICKBOOT_POWERON -> arrancarServicio(
                contexto,
                accion,
            )

            else -> Log.w(ETIQUETA_ARRANQUE, "Acción de arranque desconocida: $accion.")
        }
    }

    /**
     * Levanta el servicio en primer plano.
     *
     * Arrancar un servicio en primer plano desde el receptor de arranque está permitido (la
     * recepción de `BOOT_COMPLETED` es una de las excepciones al bloqueo de arranque en segundo
     * plano). Aun así, algunos fabricantes lo restringen: si el sistema lo rechaza, en lugar de
     * dejar al usuario sin nada se publica aquí mismo el aviso de un toque, que no necesita ningún
     * permiso especial.
     */
    private fun arrancarServicio(contexto: Context, accion: String) {
        val intencion = Intent(contexto, ServicioArranque::class.java).setAction(accion)
        try {
            ContextCompat.startForegroundService(contexto, intencion)
            Log.i(ETIQUETA_ARRANQUE, "Servicio de arranque en marcha ($accion).")
        } catch (error: RuntimeException) {
            // ForegroundServiceStartNotAllowedException y familia: el sistema no nos deja.
            Log.e(ETIQUETA_ARRANQUE, "El sistema no ha dejado arrancar el servicio.", error)
            EstrategiaNotificacion(contexto).avisar()
        }
    }

    private companion object {
        /**
         * Reinicio rápido de algunos fabricantes. No es una constante del SDK: el fabricante la
         * envía como cadena suelta y aquí se atiende tal cual.
         */
        const val ACCION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
    }
}
