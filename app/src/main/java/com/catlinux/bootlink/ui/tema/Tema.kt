package com.catlinux.bootlink.ui.tema

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val EsquemaClaro = lightColorScheme(
    primary = PrimarioClaro,
    onPrimary = SobrePrimarioClaro,
    secondary = SecundarioClaro,
    onSecondary = SobreSecundarioClaro,
    background = FondoClaro,
    onBackground = SobreFondoClaro,
    surface = SuperficieClara,
    onSurface = SobreSuperficieClara,
)

private val EsquemaOscuro = darkColorScheme(
    primary = PrimarioOscuro,
    onPrimary = SobrePrimarioOscuro,
    secondary = SecundarioOscuro,
    onSecondary = SobreSecundarioOscuro,
    background = FondoOscuro,
    onBackground = SobreFondoOscuro,
    surface = SuperficieOscura,
    onSurface = SobreSuperficieOscura,
)

/**
 * Tema de la app: Material 3 con variante clara y oscura.
 *
 * Por defecto sigue el tema del sistema ([isSystemInDarkTheme]). Si el teléfono lo permite
 * (Android 12 o superior) usa además los colores dinámicos del fondo de pantalla, que es lo
 * que se espera de una app integrada en el sistema.
 *
 * @param modoOscuro si es true se usa la paleta oscura.
 * @param colorDinamico si es true y el sistema lo permite, se toman los colores del usuario.
 * @param contenido la interfaz que se dibuja con este tema.
 */
@Composable
fun TemaBootLink(
    modoOscuro: Boolean = isSystemInDarkTheme(),
    colorDinamico: Boolean = true,
    contenido: @Composable () -> Unit,
) {
    val esquemaColores = when {
        colorDinamico && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val contexto = LocalContext.current
            if (modoOscuro) {
                dynamicDarkColorScheme(contexto)
            } else {
                dynamicLightColorScheme(contexto)
            }
        }

        modoOscuro -> EsquemaOscuro
        else -> EsquemaClaro
    }

    MaterialTheme(
        colorScheme = esquemaColores,
        content = contenido,
    )
}
