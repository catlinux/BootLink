package com.catlinux.bootlink.util

import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri

/**
 * Intents con los que BootLink lleva al usuario a los ajustes del sistema que le faltan.
 *
 * Están todos juntos aquí para que el ViewModel solo tenga que lanzarlos y para que las pantallas
 * de Compose no sepan nada de paquetes ni de componentes del sistema.
 */
object AjustesDelSistema {

    /**
     * Ajustes de notificaciones de la app, que es donde se concede POST_NOTIFICATIONS en Android 13
     * o superior.
     */
    fun intentNotificaciones(paquete: String): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, paquete)

    /** Pantalla del permiso de superposición, el que necesita el modo automático. */
    fun intentSuperposicion(paquete: String): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$paquete".toUri())

    /** Ficha de BootLink en los ajustes generales del sistema; sirve de último recurso. */
    fun intentDetallesApp(paquete: String): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$paquete".toUri())

    /**
     * Pantalla propia del fabricante donde se permite el inicio automático de las apps (en Samsung,
     * el gestor de batería). Devuelve null para [Fabricante.OTRO].
     *
     * Son componentes concretos, no una acción estándar: cada marca los cambia de un modelo a otro
     * y de una versión a otra, así que quien lance el Intent debe estar preparado para que esa
     * pantalla no exista en este teléfono.
     */
    fun intentFabricante(fabricante: Fabricante): Intent? {
        val destino = when (fabricante) {
            Fabricante.XIAOMI -> INICIO_AUTOMATICO_MIUI
            Fabricante.HUAWEI -> INICIO_AUTOMATICO_EMUI
            Fabricante.OPPO -> INICIO_AUTOMATICO_COLOROS
            Fabricante.SAMSUNG -> GESTOR_BATERIA_SAMSUNG
            Fabricante.OTRO -> null
        }
        return destino?.let { componente -> Intent().setComponent(componente) }
    }

    /** Inicio automático de MIUI e HyperOS (Xiaomi, Redmi y Poco). */
    private val INICIO_AUTOMATICO_MIUI = ComponentName(
        "com.miui.securitycenter",
        "com.miui.permcenter.autostart.AutoStartManagementActivity",
    )

    /** Gestor de inicio de EMUI (Huawei). */
    private val INICIO_AUTOMATICO_EMUI = ComponentName(
        "com.huawei.systemmanager",
        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
    )

    /** Lista de apps con inicio automático de ColorOS (Oppo, Realme y OnePlus). */
    private val INICIO_AUTOMATICO_COLOROS = ComponentName(
        "com.coloros.safecenter",
        "com.coloros.safecenter.startupapp.StartupAppListActivity",
    )

    /** Gestor de batería de One UI (Samsung), donde se limita lo que corre en segundo plano. */
    private val GESTOR_BATERIA_SAMSUNG = ComponentName(
        "com.samsung.android.lool",
        "com.samsung.android.sm.ui.battery.BatteryActivity",
    )
}
