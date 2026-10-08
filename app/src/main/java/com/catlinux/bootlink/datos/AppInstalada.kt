package com.catlinux.bootlink.datos

import android.graphics.drawable.Drawable

/**
 * Una aplicación instalada en el teléfono que el usuario puede elegir para lanzar al arrancar.
 *
 * Es un modelo de solo lectura que se construye al consultar el sistema y nunca se persiste:
 * el icono vive en memoria y no tiene sentido escribirlo en disco. Para guardar la elección se
 * usa [AppConfigurada], que solo lleva el paquete.
 *
 * @param paquete nombre del paquete de la app, por ejemplo `com.catlinux.bootlink`.
 * @param etiqueta nombre visible de la app, tal y como lo muestra el sistema.
 * @param icono icono de la app. Los dos nombres anteriores identifican la app; este campo se
 *   compara por identidad, así que dos consultas distintas producen objetos no iguales.
 */
data class AppInstalada(
    val paquete: String,
    val etiqueta: String,
    val icono: Drawable,
)
