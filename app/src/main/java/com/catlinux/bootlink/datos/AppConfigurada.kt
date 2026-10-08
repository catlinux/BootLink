package com.catlinux.bootlink.datos

import kotlinx.serialization.Serializable

/**
 * Una aplicación que el usuario ha elegido para lanzar al arrancar.
 *
 * Es el modelo que se persiste, serializado a JSON dentro de DataStore. No guarda ni la
 * etiqueta ni el icono: se vuelven a leer del sistema cuando la interfaz los necesita, así que
 * la lista guardada sigue siendo válida aunque la app cambie de nombre o de icono.
 *
 * @param paquete nombre del paquete de la app, por ejemplo `com.ejemplo.mensajeria`.
 * @param orden posición en la secuencia de lanzamiento, de 0 en adelante.
 * @param retardoMs espera antes de lanzar esta app, en milisegundos (0 = sin espera).
 * @param activa si es false la app permanece en la lista pero no se lanza.
 */
@Serializable
data class AppConfigurada(
    val paquete: String,
    val orden: Int = 0,
    val retardoMs: Long = 0L,
    val activa: Boolean = true,
)
