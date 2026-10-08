package com.catlinux.bootlink.arranque

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.catlinux.bootlink.R
import com.catlinux.bootlink.ui.tema.TemaBootLink
import kotlinx.coroutines.launch

/**
 * Actividad que pregunta si abrir las apps configuradas al arrancar el teléfono.
 *
 * Es la mitad visible del modo de confirmación: la abre [EstrategiaConfirmacion] desde el servicio
 * de arranque con el permiso de superposición ya concedido, y muestra un diálogo centrado con dos
 * botones:
 *
 * - «Abrir»: lanza las apps con [AppLauncher.lanzarAhora] desde [lifecycleScope] (la secuencia
 *   tiene retardos y hay que esperarla sin bloquear la interfaz) y se cierra al terminar. Igual que
 *   [LanzadorActivity], mantiene la actividad viva mientras dura la secuencia: es la corrutina la
 *   que cierra la actividad, no el toque del botón.
 * - «Cancelar»: se cierra sin abrir nada. El botón de atrás y un toque fuera del diálogo cuentan
 *   como cancelar, porque `AlertDialog` avisa por `onDismissRequest`.
 *
 * **Decisiones tomadas** (las dos formas que planteaba la tarea eran válidas):
 *
 * 1. *Compose con `AlertDialog` en vez de una pantalla propia.* El diálogo ya viene centrado, con
 *    sus márgenes y su sombra, y es exactamente lo que se quiere comunicar: una pregunta puntual,
 *    no una pantalla de la app. Además reutiliza [TemaBootLink], así que sale con los colores del
 *    tema del sistema igual que el resto de la app.
 * 2. *Tema translúcido* (`Theme.BootLink.Dialogo`, ver `themes.xml`). Es lo coherente con el punto
 *    anterior: la ventana de la actividad no pinta un fondo de pantalla completa y lo que se ve es
 *    el diálogo en el centro, sobre lo que hubiera debajo. Así queda claro que BootLink ha hecho
 *    algo y que espera una respuesta, sin que parezca que ha tomado el control del teléfono.
 *
 * En el manifiesto va con `excludeFromRecents` (no deja una entrada en «recientes»: es una pregunta
 * de una sola vez), `taskAffinity` vacío (no se mezcla con la tarea de la pantalla principal) y
 * `exported="false"` (solo la abre BootLink). No lleva `noHistory`: si el usuario deja el diálogo
 * sin contestar y abre otra cosa, al volver sigue teniéndolo delante.
 */
class ConfirmacionActivity : ComponentActivity() {

    /**
     * true mientras se están abriendo las apps, para que un segundo toque en «Abrir» (o uno
     * nervioso con doble pulsación) no arranque la secuencia dos veces.
     */
    private var abriendo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(ETIQUETA_ARRANQUE, "El teléfono ha arrancado: se pregunta si abrir las apps.")
        setContent {
            TemaBootLink {
                DialogoConfirmacion(
                    alAbrir = { abrirApps() },
                    alCancelar = { cancelar() },
                )
            }
        }
    }

    /** Abre las apps configuradas y cierra la actividad, pase lo que pase. */
    private fun abrirApps() {
        if (abriendo) {
            return
        }
        abriendo = true
        Log.i(ETIQUETA_ARRANQUE, "El usuario ha aceptado: se abren las apps configuradas.")
        lifecycleScope.launch {
            try {
                AppLauncher(applicationContext).lanzarAhora()
            } catch (error: Exception) {
                Log.e(ETIQUETA_ARRANQUE, "No se han podido abrir las apps configuradas.", error)
            } finally {
                finish()
            }
        }
    }

    /** Cierra la actividad sin abrir nada: el usuario ha dicho que no. */
    private fun cancelar() {
        Log.i(ETIQUETA_ARRANQUE, "El usuario ha cancelado: no se abre ninguna app.")
        finish()
    }
}

/**
 * El diálogo, con el texto en `strings.xml` para que no quede ninguna frase escrita en el código.
 *
 * @param alAbrir qué hacer si el usuario acepta.
 * @param alCancelar qué hacer si el usuario rechaza o descarta el diálogo.
 */
@Composable
private fun DialogoConfirmacion(alAbrir: () -> Unit, alCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text(stringResource(R.string.arranque_confirmacion_titulo)) },
        text = { Text(stringResource(R.string.arranque_confirmacion_texto)) },
        confirmButton = {
            TextButton(onClick = alAbrir) {
                Text(stringResource(R.string.arranque_confirmacion_abrir))
            }
        },
        dismissButton = {
            TextButton(onClick = alCancelar) {
                Text(stringResource(R.string.arranque_confirmacion_cancelar))
            }
        },
    )
}
