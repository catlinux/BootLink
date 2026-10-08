package com.catlinux.bootlink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.catlinux.bootlink.ui.BootLinkViewModel
import com.catlinux.bootlink.ui.pantallas.AjustesPantalla
import com.catlinux.bootlink.ui.pantallas.DiagnosticoPantalla
import com.catlinux.bootlink.ui.pantallas.ListaPantalla
import com.catlinux.bootlink.ui.pantallas.SelectorPantalla
import com.catlinux.bootlink.ui.tema.TemaBootLink

/** Rutas del grafo de navegación. Son nombres fijos: ninguna lleva argumentos todavía. */
private object Rutas {
    /** Pantalla principal: la lista de apps que se lanzan al arrancar. */
    const val LISTA = "lista"

    /** Selector de aplicaciones instaladas, al que se llega con el botón de añadir. */
    const val SELECTOR = "selector"

    /** Ajustes: el modo de arranque (aviso o automático). */
    const val AJUSTES = "ajustes"

    /** Diagnóstico: los permisos que faltan y los avisos del fabricante. */
    const val DIAGNOSTICO = "diagnostico"
}

/**
 * Actividad única de BootLink.
 *
 * Solo prepara el tema y el grafo de navegación; el contenido de cada pantalla vive en
 * `ui.pantallas`. El estado se pide a un único [BootLinkViewModel] compartido por las cuatro
 * pantallas, para que navegar entre ellas no vuelva a leer DataStore.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Con targetSdk 35 o superior la app dibuja detrás de las barras del sistema.
        enableEdgeToEdge()
        setContent {
            TemaBootLink {
                NavegacionBootLink()
            }
        }
    }
}

/**
 * Grafo de navegación de la app: empieza en la lista y desde ahí se puede ir al selector, a los
 * ajustes y al diagnóstico.
 *
 * El ViewModel se crea aquí, fuera de las pantallas, así que es el mismo objeto para todo el
 * grafo: lo que se añade en el selector aparece ya en la lista al volver, y el modo elegido en los
 * ajustes lo ve el diagnóstico al instante.
 */
@Composable
private fun NavegacionBootLink() {
    val modelo: BootLinkViewModel = viewModel()
    val controlador = rememberNavController()

    NavHost(navController = controlador, startDestination = Rutas.LISTA) {
        composable(route = Rutas.LISTA) {
            ListaPantalla(
                modelo = modelo,
                alPulsarAnadir = { controlador.navigate(Rutas.SELECTOR) },
                alAbrirAjustes = { controlador.navigate(Rutas.AJUSTES) },
                alAbrirDiagnostico = { controlador.navigate(Rutas.DIAGNOSTICO) },
            )
        }

        composable(route = Rutas.SELECTOR) {
            SelectorPantalla(
                modelo = modelo,
                alVolver = { controlador.popBackStack() },
            )
        }

        composable(route = Rutas.AJUSTES) {
            AjustesPantalla(
                modelo = modelo,
                alVolver = { controlador.popBackStack() },
            )
        }

        composable(route = Rutas.DIAGNOSTICO) {
            DiagnosticoPantalla(
                modelo = modelo,
                alVolver = { controlador.popBackStack() },
            )
        }
    }
}
