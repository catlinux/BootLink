package com.catlinux.bootlink.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.catlinux.bootlink.R
import com.catlinux.bootlink.datos.ModoArranque
import com.catlinux.bootlink.ui.BootLinkViewModel

/**
 * Pantalla de ajustes de BootLink: cómo se abren las apps cuando el teléfono arranca.
 *
 * Hay tres modos: «Preguntar al arrancar» (el predeterminado, que enseña un diálogo al encender el
 * teléfono), «Aviso discreto» (una notificación que el usuario toca) y «Automático» (las apps se
 * abren solas). El modo se guarda en cuanto se elige, aunque falten permisos: si se elige uno de los
 * que necesitan la superposición sin tenerla concedida, se explica qué falta y se ofrece abrir los
 * ajustes del sistema, pero la elección no se bloquea (hasta que se conceda, el arranque seguirá
 * avisando con una notificación).
 *
 * @param modelo estado de la aplicación y guardado de las preferencias.
 * @param alVolver qué hacer al pulsar la flecha de volver.
 * @param modifier modificador que se aplica al andamiaje de la pantalla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesPantalla(
    modelo: BootLinkViewModel,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val modo by modelo.modoArranque.collectAsStateWithLifecycle()
    val estado by modelo.diagnostico.collectAsStateWithLifecycle()

    // El permiso se puede haber concedido en los ajustes del sistema, así que al volver a la
    // pantalla se comprueba otra vez antes de seguir avisando de que falta.
    LifecycleResumeEffect(Unit) {
        modelo.actualizarDiagnostico()
        onPauseOrDispose { }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.ajustes_titulo)) },
                navigationIcon = {
                    IconButton(onClick = alVolver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.volver),
                        )
                    }
                },
            )
        },
    ) { espacioInterior ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(espacioInterior)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.ajustes_modo_explicacion),
                style = MaterialTheme.typography.bodyMedium,
            )

            OpcionModo(
                titulo = stringResource(R.string.ajustes_modo_confirmar_titulo),
                descripcion = stringResource(R.string.ajustes_modo_confirmar_descripcion),
                seleccionado = modo == ModoArranque.CONFIRMAR,
                alElegir = { modelo.cambiarModoArranque(ModoArranque.CONFIRMAR) },
            )

            OpcionModo(
                titulo = stringResource(R.string.ajustes_modo_notificacion_titulo),
                descripcion = stringResource(R.string.ajustes_modo_notificacion_descripcion),
                seleccionado = modo == ModoArranque.NOTIFICACION,
                alElegir = { modelo.cambiarModoArranque(ModoArranque.NOTIFICACION) },
            )

            OpcionModo(
                titulo = stringResource(R.string.ajustes_modo_automatico_titulo),
                descripcion = stringResource(R.string.ajustes_modo_automatico_descripcion),
                seleccionado = modo == ModoArranque.AUTOMATICO,
                alElegir = { modelo.cambiarModoArranque(ModoArranque.AUTOMATICO) },
            )

            // El aviso sale con los dos modos que necesitan superposición: el de confirmación (sin
            // ella Android no deja abrir la actividad del diálogo desde segundo plano) y el
            // automático (sin ella no se puede dibujar la ventana que abre las apps).
            if (modo.necesitaSuperposicion && !estado.superposicionPermitida) {
                AvisoSuperposicion(alConceder = { modelo.abrirAjustesSuperposicion() })
            }
        }
    }
}

/**
 * Una de las tres formas de arrancar la app. Toda la fila es pulsable y el botón de radio solo marca
 * lo elegido (no recibe pulsaciones propias para no repetir la misma acción dos veces).
 *
 * @param titulo nombre del modo.
 * @param descripcion qué hace ese modo, en palabras del usuario.
 * @param seleccionado si es el modo guardado.
 * @param alElegir guarda este modo.
 */
@Composable
private fun OpcionModo(
    titulo: String,
    descripcion: String,
    seleccionado: Boolean,
    alElegir: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = seleccionado,
                role = Role.RadioButton,
                onClick = alElegir,
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = seleccionado, onClick = null)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = descripcion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Aviso de que el modo elegido necesita el permiso de superposición. Es un aviso, no un impedimento:
 * el modo ya está guardado y funcionará en cuanto se conceda el permiso.
 *
 * @param alConceder abre los ajustes del sistema en la pantalla de ese permiso.
 */
@Composable
private fun AvisoSuperposicion(alConceder: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.ajustes_superposicion_titulo),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.ajustes_superposicion_descripcion),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = alConceder) {
                Text(text = stringResource(R.string.ajustes_superposicion_boton))
            }
        }
    }
}
