package com.catlinux.bootlink.ui.pantallas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.catlinux.bootlink.R
import com.catlinux.bootlink.datos.AppInstalada
import com.catlinux.bootlink.ui.BootLinkViewModel

/**
 * Pantalla que ofrece las aplicaciones instaladas para añadirlas a la lista de arranque.
 *
 * Las apps se piden al sistema a través de [BootLinkViewModel], que consulta PackageManager fuera
 * del hilo principal y publica el resultado. El buscador filtra por nombre sin distinguir
 * mayúsculas y minúsculas. Las que ya están configuradas se marcan y no se pueden volver a añadir;
 * al tocar una nueva se guarda con `Preferencias.anadir` y se vuelve a la pantalla principal.
 *
 * @param modelo estado de la aplicación y consultas al sistema.
 * @param alVolver qué hacer al pulsar la flecha de volver o después de añadir una app.
 * @param modifier modificador que se aplica al andamiaje de la pantalla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorPantalla(
    modelo: BootLinkViewModel,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val instaladas by modelo.appsInstaladas.collectAsStateWithLifecycle()
    val configuradas by modelo.appsConfiguradas.collectAsStateWithLifecycle()
    val cargando by modelo.cargandoInstaladas.collectAsStateWithLifecycle()

    // El texto buscado se recuerda aunque la pantalla se vuelva a crear (un giro del teléfono).
    var busqueda by rememberSaveable { mutableStateOf("") }

    // Al abrir el selector se vuelve a preguntar al sistema: puede haber apps nuevas instaladas.
    LaunchedEffect(Unit) { modelo.cargarAppsInstaladas() }

    val yaAnadidas = remember(configuradas) { configuradas.map { it.paquete }.toSet() }
    val filtradas = remember(instaladas, busqueda) {
        val texto = busqueda.trim()
        if (texto.isEmpty()) {
            instaladas
        } else {
            instaladas.filter { app -> app.etiqueta.contains(texto, ignoreCase = true) }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.selector_titulo)) },
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
                .padding(espacioInterior),
        ) {
            OutlinedTextField(
                value = busqueda,
                onValueChange = { texto -> busqueda = texto },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                label = { Text(text = stringResource(R.string.selector_buscar)) },
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (busqueda.isNotEmpty()) {
                        IconButton(onClick = { busqueda = "" }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = stringResource(R.string.selector_limpiar),
                            )
                        }
                    }
                },
            )

            when {
                cargando && instaladas.isEmpty() -> {
                    Mensaje(texto = stringResource(R.string.selector_cargando))
                }

                filtradas.isEmpty() -> {
                    Mensaje(texto = stringResource(R.string.selector_sin_resultados))
                }

                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(items = filtradas, key = { app -> app.paquete }) { app ->
                            FilaSeleccionable(
                                app = app,
                                yaAnadida = app.paquete in yaAnadidas,
                                alPulsar = {
                                    modelo.anadir(app.paquete)
                                    alVolver()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Una app instalada dentro del selector.
 *
 * @param app la app del sistema.
 * @param yaAnadida si ya está en la lista de arranque; entonces se marca y deja de ser pulsable.
 * @param alPulsar añade la app a la lista de arranque.
 */
@Composable
private fun FilaSeleccionable(
    app: AppInstalada,
    yaAnadida: Boolean,
    alPulsar: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !yaAnadida, onClick = alPulsar)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconoApp(icono = app.icono, descripcion = null)
        Text(
            text = app.etiqueta,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (yaAnadida) {
            Text(
                text = stringResource(R.string.selector_ya_anadida),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** Texto centrado que ocupa la pantalla; se usa para «cargando» y «sin resultados». */
@Composable
private fun Mensaje(texto: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = texto,
            modifier = Modifier.padding(24.dp),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

