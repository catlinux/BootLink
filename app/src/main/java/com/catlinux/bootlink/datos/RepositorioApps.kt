package com.catlinux.bootlink.datos

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build

/**
 * Consulta qué aplicaciones instaladas se pueden lanzar: las que tienen icono en el menú de
 * aplicaciones del teléfono.
 *
 * No se usa el permiso restringido de Google Play que permite ver todos los paquetes
 * instalados (obliga a justificarlo con un vídeo en la ficha de la tienda). En su lugar, el
 * manifiesto declara un bloque `<queries>` con la acción MAIN y la categoría LAUNCHER, y aquí
 * se consulta con [PackageManager.queryIntentActivities], que devuelve exactamente las apps
 * lanzables que el selector necesita.
 *
 * @param contexto contexto de la app; se usa para consultar el sistema.
 */
class RepositorioApps(private val contexto: Context) {

    private val gestorPaquetes: PackageManager
        get() = contexto.packageManager

    /** Paquete de BootLink: no tiene sentido ofrecerse a sí misma para lanzar. */
    private val paquetePropio: String = contexto.packageName

    /**
     * Aplicaciones lanzables del sistema, ordenadas por etiqueta (ignorando mayúsculas y, a
     * igualdad de nombre, por paquete para que el orden sea siempre el mismo). La propia app
     * queda fuera. Si una app tiene varias activities lanzables, aparece una sola vez.
     */
    fun appsLanzables(): List<AppInstalada> {
        val intencion = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return consultarActividades(intencion)
            .asSequence()
            .mapNotNull(::aAppInstalada)
            .filter { it.paquete != paquetePropio }
            .distinctBy { it.paquete }
            .sortedWith(porEtiqueta)
            .toList()
    }

    /**
     * Consulta la intención con la sobrecarga que corresponde a la versión del teléfono: desde
     * Android 13 (API 33) la variante moderna usa [PackageManager.ResolveInfoFlags].
     */
    private fun consultarActividades(intencion: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gestorPaquetes.queryIntentActivities(
                intencion,
                PackageManager.ResolveInfoFlags.of(0L),
            )
        } else {
            @Suppress("DEPRECATION")
            gestorPaquetes.queryIntentActivities(intencion, 0)
        }

    /** Convierte un resultado del sistema en el modelo de la capa de datos. */
    private fun aAppInstalada(info: ResolveInfo): AppInstalada? {
        val actividad = info.activityInfo ?: return null
        return AppInstalada(
            paquete = actividad.packageName,
            etiqueta = info.loadLabel(gestorPaquetes).toString(),
            icono = info.loadIcon(gestorPaquetes),
        )
    }

    private companion object {
        /** Alfabético e insensible a mayúsculas, con el paquete como desempate estable. */
        val porEtiqueta: Comparator<AppInstalada> =
            compareBy<AppInstalada, String>(String.CASE_INSENSITIVE_ORDER) { it.etiqueta }
                .thenBy { it.paquete }
    }
}
