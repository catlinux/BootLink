package com.catlinux.bootlink.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.catlinux.bootlink.R
import com.catlinux.bootlink.ui.BootLinkViewModel

/**
 * Pantalla de diagnóstico: qué le falta a BootLink para poder arrancar las apps.
 *
 * Comprueba tres cosas, cada una con su estado y, si procede, su botón para arreglarla:
 *
 * - El permiso de notificaciones (Android 13 o superior), sin el cual no se ve el aviso de arranque.
 * - El permiso de superposición, solo si el modo guardado es uno de los que lo necesitan («Preguntar
 *   al arrancar» o «Automático»): los dos abren actividades desde el servicio de arranque, y sin
 *   ventana superpuesta Android no las deja verse. El modo «Aviso discreto» no lo usa.
 * - La marca del teléfono, porque las capas propias de Xiaomi, Huawei, Oppo y Samsung cierran las
 *   apps en segundo plano: se avisa y se ofrece abrir los ajustes de esa marca.
 *
 * Todo se lee del sistema a través de [BootLinkViewModel] y se vuelve a leer cada vez que la
 * pantalla pasa a primer plano: al conceder un permiso el usuario se va a los ajustes del sistema y
 * vuelve.
 *
 * @param modelo estado de la aplicación y comprobaciones del sistema.
 * @param alVolver qué hacer al pulsar la flecha de volver.
 * @param modifier modificador que se aplica al andamiaje de la pantalla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticoPantalla(
    modelo: BootLinkViewModel,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val modo by modelo.modoArranque.collectAsStateWithLifecycle()
    val estado by modelo.diagnostico.collectAsStateWithLifecycle()

    // Al volver de los ajustes del sistema (donde se conceden los permisos) se comprueba de nuevo.
    LifecycleResumeEffect(Unit) {
        modelo.actualizarDiagnostico()
        onPauseOrDispose { }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.diagnostico_titulo)) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.diagnostico_intro),
                style = MaterialTheme.typography.bodyMedium,
            )

            val faltaNotificaciones = estado.notificacionesExigidas && !estado.notificacionesPermitidas
            TarjetaDiagnostico(
                titulo = stringResource(R.string.diagnostico_notificaciones_titulo),
                detalle = when {
                    !estado.notificacionesExigidas ->
                        stringResource(R.string.diagnostico_notificaciones_no_hacen_falta)

                    estado.notificacionesPermitidas ->
                        stringResource(R.string.diagnostico_notificaciones_ok)

                    else -> stringResource(R.string.diagnostico_notificaciones_pendiente)
                },
                correcto = !faltaNotificaciones,
                textoBoton = if (faltaNotificaciones) {
                    stringResource(R.string.diagnostico_notificaciones_boton)
                } else {
                    null
                },
                alPulsarBoton = { modelo.abrirAjustesNotificaciones() },
            )

            // Solo tiene sentido con los modos que usan la superposición: el de confirmación (la
            // actividad del diálogo) y el automático (la ventana que abre las apps).
            if (modo.necesitaSuperposicion) {
                TarjetaDiagnostico(
                    titulo = stringResource(R.string.diagnostico_superposicion_titulo),
                    detalle = if (estado.superposicionPermitida) {
                        stringResource(R.string.diagnostico_superposicion_ok)
                    } else {
                        stringResource(R.string.diagnostico_superposicion_pendiente)
                    },
                    correcto = estado.superposicionPermitida,
                    textoBoton = if (estado.superposicionPermitida) {
                        null
                    } else {
                        stringResource(R.string.diagnostico_superposicion_boton)
                    },
                    alPulsarBoton = { modelo.abrirAjustesSuperposicion() },
                )
            }

            // La marca del teléfono no se puede cambiar, pero sí su ajuste propio de batería o de
            // inicio automático. Si no es de las conocidas se dice igualmente, para que no parezca
            // que la comprobación se ha olvidado.
            if (estado.fabricante.restringeArranque) {
                TarjetaDiagnostico(
                    titulo = stringResource(
                        R.string.diagnostico_fabricante_titulo,
                        estado.nombreFabricante,
                    ),
                    detalle = stringResource(
                        R.string.diagnostico_fabricante_detalle,
                        estado.nombreFabricante,
                    ),
                    correcto = false,
                    textoBoton = stringResource(
                        R.string.diagnostico_fabricante_boton,
                        estado.nombreFabricante,
                    ),
                    alPulsarBoton = { modelo.abrirAjustesFabricante() },
                )
            } else {
                TarjetaDiagnostico(
                    titulo = stringResource(
                        R.string.diagnostico_fabricante_titulo_ok,
                        estado.nombreFabricante,
                    ),
                    detalle = stringResource(R.string.diagnostico_fabricante_detalle_ok),
                    correcto = true,
                    textoBoton = null,
                    alPulsarBoton = {},
                )
            }
        }
    }
}

/**
 * Una comprobación del diagnóstico: icono y título, el estado en palabras y, si hay algo que hacer,
 * el botón que abre los ajustes correspondientes. Las comprobaciones pendientes se pintan sobre el
 * color de aviso del tema, para que se distingan de un simple vistazo.
 *
 * @param titulo qué se comprueba.
 * @param detalle el estado, ya en palabras («Concedido», «Sin él, el modo automático…»).
 * @param correcto true si no hay nada que arreglar.
 * @param textoBoton texto del botón, o null si no hay nada que hacer.
 * @param alPulsarBoton abre los ajustes del sistema que resuelven lo que falta.
 */
@Composable
private fun TarjetaDiagnostico(
    titulo: String,
    detalle: String,
    correcto: Boolean,
    textoBoton: String?,
    alPulsarBoton: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (correcto) {
            CardDefaults.cardColors()
        } else {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (correcto) {
                        Icons.Filled.CheckCircle
                    } else {
                        Icons.Filled.Warning
                    },
                    contentDescription = null,
                )
                Text(
                    text = titulo,
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                text = detalle,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (textoBoton != null) {
                Button(onClick = alPulsarBoton) {
                    Text(text = textoBoton)
                }
            }
        }
    }
}
