package com.catlinux.bootlink.arranque

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.catlinux.bootlink.R

/**
 * Estrategia de un toque: se avisa con una notificación y las apps se abren cuando el usuario
 * toca el aviso.
 *
 * Es el modo por defecto porque no necesita ningún permiso especial y es el único que Google Play
 * admite sin justificación: Android permite que una app abra la actividad de otra cuando el propio
 * usuario acaba de interactuar con ella, y tocar una notificación es exactamente eso. El aviso lleva
 * un [PendingIntent] que abre [LanzadorActivity], la actividad de BootLink en la que ya se puede
 * lanzar la secuencia ([AppLauncher.lanzarAhora]).
 *
 * En Android 13 o superior el usuario puede tener el permiso de notificaciones denegado: entonces no
 * hay forma de avisar y se devuelve [ResultadoLanzamiento.SIN_PERMISO_NOTIFICACIONES] para que la
 * pantalla de diagnóstico lo muestre.
 *
 * @param contexto contexto con el que se consulta el permiso y se publica el aviso.
 */
class EstrategiaNotificacion(private val contexto: Context) : EstrategiaLanzamiento {

    /** Aquí no hay nada que lanzar en nombre del usuario: solo se deja el aviso. */
    override suspend fun lanzar(lanzarApps: suspend () -> Unit): ResultadoLanzamiento = avisar()

    /**
     * Publica la notificación de aviso.
     *
     * No es `suspend` a propósito: publicar una notificación no espera a nada, y así el mismo aviso
     * puede usarse también desde el receptor de arranque cuando el sistema no deja levantar el
     * servicio en primer plano (fabricantes que lo bloquean pese a estar permitido).
     *
     * @return [ResultadoLanzamiento.AVISADO_CON_NOTIFICACION] si el aviso queda publicado, o
     *   [ResultadoLanzamiento.SIN_PERMISO_NOTIFICACIONES] si falta el permiso.
     */
    @SuppressLint("MissingPermission") // El permiso se comprueba justo antes, en puedeNotificar().
    fun avisar(): ResultadoLanzamiento {
        if (!puedeNotificar()) {
            Log.w(ETIQUETA_ARRANQUE, "Sin permiso de notificaciones: no se puede avisar.")
            return ResultadoLanzamiento.SIN_PERMISO_NOTIFICACIONES
        }
        asegurarCanal()
        NotificationManagerCompat.from(contexto).notify(ID_NOTIFICACION, notificacion())
        Log.i(ETIQUETA_ARRANQUE, "Aviso publicado: las apps se abrirán al tocarlo.")
        return ResultadoLanzamiento.AVISADO_CON_NOTIFICACION
    }

    /**
     * En Android 13 (API 33) o superior el aviso necesita el permiso `POST_NOTIFICATIONS`. En
     * versiones anteriores lo concede el sistema de entrada.
     */
    private fun puedeNotificar(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                contexto,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    /** Crea el canal del aviso; repetirlo no tiene ningún efecto si ya existe. */
    private fun asegurarCanal() {
        val gestor = contexto.getSystemService(NotificationManager::class.java) ?: return
        val canal = NotificationChannel(
            ID_CANAL,
            contexto.getString(R.string.arranque_canal_aviso),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = contexto.getString(R.string.arranque_canal_aviso_descripcion)
        }
        gestor.createNotificationChannel(canal)
    }

    /**
     * El aviso: se cierra solo al tocarlo (a partir de ahí manda [LanzadorActivity]) y explica en
     * su texto qué va a pasar, porque quien lo ve acaba de encender el teléfono.
     */
    private fun notificacion(): Notification {
        val texto = contexto.getString(R.string.arranque_aviso_texto)
        return NotificationCompat.Builder(contexto, ID_CANAL)
            .setSmallIcon(R.drawable.ic_aviso_arranque)
            .setContentTitle(contexto.getString(R.string.arranque_aviso_titulo))
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(intencionDelLanzador())
            .build()
    }

    /**
     * El toque del usuario, convertido en permiso del sistema para que BootLink pase a primer plano
     * y, ya con ventana visible, pueda abrir las apps configuradas.
     */
    private fun intencionDelLanzador(): PendingIntent {
        val intencion = Intent(contexto, LanzadorActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            contexto,
            CODIGO_PETICION,
            intencion,
            // FLAG_IMMUTABLE es obligatorio desde Android 12; el PendingIntent siempre es el mismo.
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        /** Canal del aviso: importancia normal, con sonido, porque pide una acción del usuario. */
        const val ID_CANAL = "arranque_aviso"

        /** Identificador fijo: si el teléfono arranca dos veces, el aviso se sustituye, no se suma. */
        const val ID_NOTIFICACION = 1001

        /** Código del PendingIntent; solo hay uno por app, así que basta con un número fijo. */
        const val CODIGO_PETICION = 1
    }
}
