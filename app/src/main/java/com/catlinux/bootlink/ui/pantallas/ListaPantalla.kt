package com.catlinux.bootlink.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.catlinux.bootlink.R
import com.catlinux.bootlink.datos.AppConfigurada
import com.catlinux.bootlink.datos.AppInstalada
import com.catlinux.bootlink.ui.BootLinkViewModel

/** Dígitos que se admiten en el campo del retardo: hasta 999999 ms (algo más de 16 minutos). */
private const val MAX_DIGITOS_RETARDO = 6

/**
 * Pantalla principal de BootLink: las apps que se lanzarán al arrancar el teléfono, en su orden.
 *
 * Cada fila se puede reordenar, activar o desactivar, quitar y ajustar su retardo. El botón
 * flotante lleva al selector para añadir apps nuevas, y la barra de arriba a los ajustes (el modo de
 * arranque) y al diagnóstico (los permisos que falten).
 *
 * La pantalla no habla con DataStore ni con PackageManager: lee el estado de [modelo] y le pide
 * los cambios a él.
 *
 * @param modelo estado de la aplicación y operaciones sobre la lista.
 * @param alPulsarAnadir qué hacer al pulsar el botón de añadir (abrir el selector).
 * @param alAbrirAjustes qué hacer al pulsar el icono de ajustes.
 * @param alAbrirDiagnostico qué hacer al pulsar el icono de diagnóstico.
 * @param modifier modificador que se aplica al andamiaje de la pantalla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaPantalla(
    modelo: BootLinkViewModel,
    alPulsarAnadir: () -> Unit,
    alAbrirAjustes: () -> Unit,
    alAbrirDiagnostico: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val apps by modelo.appsConfiguradas.collectAsStateWithLifecycle()
    val instaladas by modelo.appsInstaladas.collectAsStateWithLifecycle()

    // El icono y el nombre visibles no se guardan: se leen del sistema. Se piden al abrir la
    // pantalla y, mientras llegan, las filas se pintan con el nombre del paquete.
    LaunchedEffect(Unit) { modelo.cargarAppsInstaladas() }

    // Índice por paquete para no recorrer la lista entera de instaladas en cada fila.
    val catalogo = remember(instaladas) { instaladas.associateBy(AppInstalada::paquete) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = alAbrirDiagnostico) {
                        Icon(
                            imageVector = Icons.Filled.Build,
                            contentDescription = stringResource(R.string.lista_diagnostico),
                        )
                    }
                    IconButton(onClick = alAbrirAjustes) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.lista_ajustes),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = alPulsarAnadir) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.lista_anadir),
                )
            }
        },
    ) { espacioInterior ->
        if (apps.isEmpty()) {
            SinAppsConfiguradas(modifier = Modifier.padding(espacioInterior))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(espacioInterior),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(items = apps, key = { _, app -> app.paquete }) { indice, app ->
                    FilaApp(
                        app = app,
                        instalada = catalogo[app.paquete],
                        puedeSubir = indice > 0,
                        puedeBajar = indice < apps.lastIndex,
                        alSubir = { modelo.reordenar(paquetesAlMover(apps, indice, indice - 1)) },
                        alBajar = { modelo.reordenar(paquetesAlMover(apps, indice, indice + 1)) },
                        alCambiarActiva = { activa -> modelo.cambiarActiva(app.paquete, activa) },
                        alCambiarRetardo = { retardo -> modelo.cambiarRetardo(app.paquete, retardo) },
                        alQuitar = { modelo.quitar(app.paquete) },
                    )
                }
            }
        }
    }
}

/** Lista vacía: se explica que todavía no hay nada configurado en vez de dejarla en blanco. */
@Composable
private fun SinAppsConfiguradas(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.CenterVertically,
        ),
    ) {
        Text(
            text = stringResource(R.string.lista_vacia_titulo),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.lista_vacia_descripcion),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Una fila de la lista: icono, nombre, interruptor de activa, botones para moverla o quitarla y el
 * campo del retardo.
 *
 * @param app la app configurada, tal y como está guardada.
 * @param instalada la app tal y como la ve el sistema, o null si ya no está instalada; en ese caso
 *   se muestra su paquete y se puede quitar igualmente.
 * @param puedeSubir si la fila no es la primera.
 * @param puedeBajar si la fila no es la última.
 * @param alSubir mueve la fila una posición hacia arriba.
 * @param alBajar mueve la fila una posición hacia abajo.
 * @param alCambiarActiva activa o desactiva la app.
 * @param alCambiarRetardo guarda el retardo nuevo, en milisegundos.
 * @param alQuitar saca la app de la lista.
 */
@Composable
private fun FilaApp(
    app: AppConfigurada,
    instalada: AppInstalada?,
    puedeSubir: Boolean,
    puedeBajar: Boolean,
    alSubir: () -> Unit,
    alBajar: () -> Unit,
    alCambiarActiva: (Boolean) -> Unit,
    alCambiarRetardo: (Long) -> Unit,
    alQuitar: () -> Unit,
) {
    val nombre = instalada?.etiqueta ?: app.paquete
    val detalle = if (instalada == null) {
        stringResource(R.string.lista_no_instalada)
    } else {
        app.paquete
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoApp(icono = instalada?.icono, descripcion = null)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                ) {
                    Text(
                        text = nombre,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = detalle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Switch(checked = app.activa, onCheckedChange = alCambiarActiva)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                CampoRetardo(
                    valorMs = app.retardoMs,
                    alCambiar = alCambiarRetardo,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = alSubir, enabled = puedeSubir) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.lista_subir),
                    )
                }
                IconButton(onClick = alBajar, enabled = puedeBajar) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.lista_bajar),
                    )
                }
                IconButton(onClick = alQuitar) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.lista_quitar),
                    )
                }
            }
        }
    }
}

/**
 * Campo del retardo de una app, en milisegundos. Solo admite dígitos y guarda cada cambio, así que
 * lo que se ve es siempre lo que hay guardado (los ceros a la izquierda desaparecen solos).
 *
 * @param valorMs retardo guardado.
 * @param alCambiar guarda el retardo nuevo.
 */
@Composable
private fun CampoRetardo(
    valorMs: Long,
    alCambiar: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var texto by remember(valorMs) { mutableStateOf(valorMs.toString()) }

    OutlinedTextField(
        value = texto,
        onValueChange = { entrada ->
            val digitos = entrada.filter { it.isDigit() }.take(MAX_DIGITOS_RETARDO)
            texto = digitos
            alCambiar(digitos.toLongOrNull() ?: 0L)
        },
        modifier = modifier,
        label = { Text(text = stringResource(R.string.lista_retardo_etiqueta)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

/**
 * Orden nuevo de paquetes tras mover la fila [desde] a la posición [hasta]. Es lo que espera
 * `Preferencias.reordenar`, que trabaja con la secuencia de paquetes y no con índices.
 */
private fun paquetesAlMover(apps: List<AppConfigurada>, desde: Int, hasta: Int): List<String> {
    val paquetes = apps.map { it.paquete }.toMutableList()
    paquetes.add(hasta, paquetes.removeAt(desde))
    return paquetes
}

