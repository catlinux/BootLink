package com.catlinux.bootlink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.catlinux.bootlink.ui.tema.TemaBootLink

/**
 * Pantalla principal de la app.
 *
 * En esta primera tarea solo está el andamiaje: se muestra el nombre de la app y un aviso
 * temporal. Cuando exista el selector de aplicaciones, este aviso desaparecerá.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Con targetSdk 35 o superior la app dibuja detrás de las barras del sistema.
        enableEdgeToEdge()
        setContent {
            TemaBootLink {
                PantallaInicio()
            }
        }
    }
}

/** Pantalla inicial vacía: aún no hay ninguna app configurada para lanzar. */
@Composable
private fun PantallaInicio(modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize()) { espacioInterior ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(espacioInterior),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.CenterVertically,
            ),
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.inicio_sin_apps),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VistaPreviaPantallaInicio() {
    TemaBootLink {
        PantallaInicio()
    }
}
