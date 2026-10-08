package com.catlinux.bootlink.ui.pantallas

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

/** Lado que ocupa el icono de una app en las listas. */
private val LADO_PREDETERMINADO = 40.dp

/**
 * Icono de una aplicación, dibujado a partir del [Drawable] que devuelve el sistema.
 *
 * Compose no dibuja un `Drawable` directamente: hay que convertirlo antes a mapa de bits. La
 * conversión se guarda con [remember], así que solo se repite si cambia el icono o el tamaño, no
 * en cada recomposición. No hace falta ninguna biblioteca de imágenes nueva para esto: `toBitmap`
 * ya viene en core-ktx.
 *
 * @param icono icono de la app, o null si la app ya no está instalada.
 * @param descripcion texto que leen los lectores de pantalla, o null si el icono es decorativo
 *   (el nombre de la app ya está al lado, así que no hace falta repetirlo).
 * @param modifier modificador que se aplica al icono o al hueco que lo sustituye.
 * @param lado lado del cuadrado que ocupa el icono.
 */
@Composable
internal fun IconoApp(
    icono: Drawable?,
    descripcion: String?,
    modifier: Modifier = Modifier,
    lado: Dp = LADO_PREDETERMINADO,
) {
    if (icono == null) {
        // Sin icono (app ya desinstalada): se reserva el mismo hueco para no desalinear la fila.
        Spacer(modifier = modifier.size(lado))
        return
    }

    val densidad = LocalDensity.current
    val mapaBits = remember(icono, lado, densidad) {
        val ladoEnPuntos = with(densidad) { lado.roundToPx() }
        icono.toBitmap(width = ladoEnPuntos, height = ladoEnPuntos).asImageBitmap()
    }

    Image(
        bitmap = mapaBits,
        contentDescription = descripcion,
        modifier = modifier.size(lado),
    )
}
