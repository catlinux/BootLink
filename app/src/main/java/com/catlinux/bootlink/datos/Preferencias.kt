package com.catlinux.bootlink.datos

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persistencia de BootLink: qué apps se lanzan al arrancar y en qué orden, el retardo de cada
 * una y el modo de arranque.
 *
 * Se usa DataStore (nunca SharedPreferences). La lista viaja como un JSON en un solo valor, lo
 * que permite leerla y escribirla de golpe sin inventar claves numeradas.
 *
 * La clase recibe el almacén ya construido: la app real lo crea con [Preferencias.desde] y las
 * pruebas unitarias, con un archivo temporal, así que no hace falta ningún dispositivo.
 *
 * @param almacen almacén de DataStore sobre el que se lee y se escribe.
 */
class Preferencias(private val almacen: DataStore<Preferences>) {

    /** La serialización ignora los campos que no conozca, para poder leer datos antiguos. */
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Aplicaciones elegidas, en el orden de lanzamiento ([AppConfigurada.orden]). Mientras el
     * usuario no configure nada, la lista está vacía.
     */
    val apps: Flow<List<AppConfigurada>> = almacen.data.map { preferencias ->
        decodificar(preferencias[CLAVE_APPS])
    }

    /**
     * Modo de arranque elegido. Si no hay nada guardado o el valor no se reconoce, devuelve
     * [ModoArranque.PREDETERMINADO], que es el modo sin permisos.
     */
    val modoArranque: Flow<ModoArranque> = almacen.data.map { preferencias ->
        ModoArranque.desdeNombre(preferencias[CLAVE_MODO])
    }

    /**
     * Sustituye la lista entera y renumera el campo [AppConfigurada.orden] para que vaya de 0 a
     * n-1 en el orden recibido.
     */
    suspend fun guardar(apps: List<AppConfigurada>) {
        val renumeradas = apps.mapIndexed { indice, app -> app.copy(orden = indice) }
        almacen.edit { preferencias ->
            preferencias[CLAVE_APPS] = json.encodeToString(renumeradas)
        }
    }

    /**
     * Añade una app al final de la lista. Si su paquete ya estaba configurado no se duplica:
     * el usuario que vuelve a elegirla conserva el retardo y el estado que ya tenía.
     */
    suspend fun anadir(app: AppConfigurada) {
        val guardadas = appsGuardadas()
        if (guardadas.any { it.paquete == app.paquete }) return
        guardar(guardadas + app.copy(orden = guardadas.size))
    }

    /** Quita una app de la lista por su paquete; el resto se renumera sin dejar huecos. */
    suspend fun quitar(paquete: String) {
        val guardadas = appsGuardadas()
        val restantes = guardadas.filterNot { it.paquete == paquete }
        if (restantes.size == guardadas.size) return
        guardar(restantes)
    }

    /**
     * Reordena la lista según la secuencia de paquetes recibida, que es el orden nuevo (el que
     * produce la interfaz al arrastrar una fila). Los paquetes que no aparezcan en ella se
     * quedan al final, conservando su orden anterior.
     */
    suspend fun reordenar(paquetesEnOrden: List<String>) {
        val guardadas = appsGuardadas()
        val posicion = paquetesEnOrden.withIndex().associate { (indice, paquete) -> paquete to indice }
        val reordenadas = guardadas.sortedWith(
            compareBy({ posicion[it.paquete] ?: Int.MAX_VALUE }, { it.orden }),
        )
        guardar(reordenadas)
    }

    /** Guarda el modo de arranque: notificación de un toque o lanzamiento automático. */
    suspend fun guardarModoArranque(modo: ModoArranque) {
        almacen.edit { preferencias ->
            preferencias[CLAVE_MODO] = modo.name
        }
    }

    /** Lectura puntual de la lista guardada, para las operaciones que la modifican. */
    private suspend fun appsGuardadas(): List<AppConfigurada> = apps.first()

    /**
     * Convierte el texto guardado en la lista de apps. Si el dato estuviera estropeado se
     * devuelve una lista vacía: es preferible perder la configuración a que la app no arranque.
     */
    private fun decodificar(texto: String?): List<AppConfigurada> {
        if (texto.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<AppConfigurada>>(texto).sortedBy { it.orden }
        } catch (error: SerializationException) {
            emptyList()
        }
    }

    companion object {
        /**
         * Claves de DataStore. Son visibles para las pruebas, que necesitan escribir en ellas
         * un valor estropeado a propósito para comprobar que la app no se rompe.
         */
        internal val CLAVE_APPS = stringPreferencesKey("apps_configuradas")
        internal val CLAVE_MODO = stringPreferencesKey("modo_arranque")

        /** Instancia real de la app: un solo almacén por proceso, como exige DataStore. */
        fun desde(contexto: Context): Preferencias =
            Preferencias(contexto.applicationContext.almacenBootLink)
    }
}

/** Nombre del archivo de DataStore dentro de la carpeta privada de la app. */
private const val NOMBRE_ALMACEN = "bootlink"

/**
 * Almacén de DataStore de BootLink, compartido por toda la app y creado de forma perezosa. Se
 * declara a nivel de archivo porque el delegado debe ser único para el mismo archivo de disco.
 */
private val Context.almacenBootLink: DataStore<Preferences> by preferencesDataStore(
    name = NOMBRE_ALMACEN,
)
