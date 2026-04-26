package com.lg.monkeymusicplayer.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

/**
 * Persiste la lista de rutas de directorio excluidas del escaneo de música.
 *
 * Almacenamiento: SharedPreferences con serialización JSON simple.
 * Se eligió SharedPreferences en lugar de Room porque:
 *  - Es una lista plana de strings sin relaciones ni queries.
 *  - No requiere migración de esquema.
 *  - La lectura es síncrona y se necesita en el arranque del scanner.
 *
 * Carpetas pre-excluidas por defecto:
 *  - WhatsApp/Media/WhatsApp Audio  (audios recibidos por WhatsApp)
 *  - WhatsApp Business (si existe)
 *
 * El usuario puede agregar cualquier ruta absoluta adicional y
 * eliminar cualquier exclusión incluyendo las de defecto.
 */
class ExcludedFoldersRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _excludedFolders = MutableStateFlow<List<String>>(emptyList())
    val excludedFolders: StateFlow<List<String>> = _excludedFolders.asStateFlow()

    init {
        // Cargamos desde preferencias después de que _excludedFolders haya sido instanciado
        _excludedFolders.value = loadFromPrefs()
    }

    // ── API pública ──────────────────────────────────────────────────────────

    fun addFolder(path: String) {
        val normalized = path.trimEnd('/')
        if (normalized.isBlank()) return
        val current = _excludedFolders.value.toMutableList()
        if (!current.contains(normalized)) {
            current.add(normalized)
            save(current)
        }
    }

    fun removeFolder(path: String) {
        val current = _excludedFolders.value.toMutableList()
        current.remove(path)
        save(current)
    }

    /**
     * Verifica si un path de archivo debe ser excluido del escaneo.
     * Compara si el path comienza con alguna de las rutas excluidas.
     */
    fun isExcluded(filePath: String): Boolean {
        return _excludedFolders.value.any { excluded ->
            filePath.startsWith(excluded)
        }
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    private fun save(folders: List<String>) {
        val json = JSONArray(folders).toString()
        prefs.edit().putString(KEY_FOLDERS, json).apply()
        _excludedFolders.value = folders.toList()
    }

    private fun loadFromPrefs(): List<String> {
        val json = prefs.getString(KEY_FOLDERS, null)
        return if (json != null) {
            val arr = JSONArray(json)
            List(arr.length()) { arr.getString(it) }
        } else {
            // Primera vez: cargar carpetas excluidas por defecto
            val defaults = buildDefaultExclusions()
            save(defaults)
            defaults
        }
    }

    companion object {
        private const val PREFS_NAME = "excluded_folders"
        private const val KEY_FOLDERS = "folders"

        // Rutas conocidas de WhatsApp en Android
        // External storage puede estar en /sdcard, /storage/emulated/0, etc.
        // Usamos solo el segmento de ruta relativo al storage root para mayor compatibilidad.
        val WHATSAPP_PATHS = listOf(
            "WhatsApp/Media/WhatsApp Audio",
            "WhatsApp Business/Media/WhatsApp Business Audio",
            "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Audio",
            "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/WhatsApp Business Audio"
        )

        /**
         * Construye las rutas absolutas de exclusión por defecto combinando
         * las rutas de almacenamiento externo conocidas con los segmentos de WhatsApp.
         */
        @Suppress("DEPRECATION")
        fun buildDefaultExclusions(): List<String> {
            val root = Environment.getExternalStorageDirectory().absolutePath
            return WHATSAPP_PATHS.map { "$root/$it" }
        }
    }
}
