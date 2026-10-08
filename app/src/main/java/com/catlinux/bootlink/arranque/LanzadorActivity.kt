package com.catlinux.bootlink.arranque

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Actividad sin interfaz que abre las apps configuradas y se cierra.
 *
 * Es la segunda mitad del modo de notificación: su único cometido es existir para que el sistema
 * considere a BootLink en primer plano. Cuando el usuario toca el aviso de arranque, el
 * [android.app.PendingIntent] que lleva dentro abre esta actividad; con la ventana ya visible,
 * Android sí permite abrir las apps de otras apps, así que aquí se lanza [AppLauncher.lanzarAhora]
 * y se termina.
 *
 * No dibuja nada (tema translúcido), no aparece en el historial de tareas ni en «recientes», y su
 * cometido termina en cuanto se han abierto las apps, para que el usuario se quede viendo la que
 * acaba de arrancar.
 */
class LanzadorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(ETIQUETA_ARRANQUE, "El usuario ha tocado el aviso: se abren las apps configuradas.")
        lifecycleScope.launch {
            try {
                AppLauncher(applicationContext).lanzarAhora()
            } catch (error: Exception) {
                // Esta actividad no tiene interfaz donde avisar: se registra y se cierra igual.
                Log.e(ETIQUETA_ARRANQUE, "No se han podido abrir las apps configuradas.", error)
            } finally {
                finish()
            }
        }
    }
}
