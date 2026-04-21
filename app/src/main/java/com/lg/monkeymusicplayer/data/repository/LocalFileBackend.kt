package com.lg.monkeymusicplayer.data.repository

import android.content.Context
import android.net.Uri
import com.lg.monkeymusicplayer.core.result.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LocalFileBackend — implementación de [CloudBackend] que usa el sistema de archivos local.
 *
 * ## Comportamiento
 * El backup se escribe en `getExternalFilesDir()/monkey_cloud_backup.json`.
 * Este archivo puede sincronizarse automáticamente si la carpeta de la app
 * está incluida en la cuenta de Google Drive del usuario (Backup & Sync).
 *
 * ## Uso manual
 * El usuario puede compartir el archivo manualmente desde [CloudSyncScreen]
 * usando `Intent.ACTION_SEND` con el URI del archivo.
 *
 * ## Limitaciones
 * - No tiene sincronización en tiempo real.
 * - Requiere que el dispositivo destino tenga acceso al mismo archivo.
 * Para sincronización real entre dispositivos, implementar [CloudBackend]
 * con Firebase Storage o Google Drive REST API.
 */
@Singleton
class LocalFileBackend @Inject constructor(
    @ApplicationContext private val context: Context
) : CloudBackend {

    companion object {
        const val FILENAME = "monkey_cloud_backup.json"
    }

    private fun getBackupFile(): File {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        return File(dir, FILENAME)
    }

    override suspend fun upload(sourceUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val dest = getBackupFile()
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.Error("Cannot read source URI")
            Timber.d("LocalFileBackend: uploaded to ${dest.absolutePath}")
            Result.Success(dest.absolutePath)
        } catch (e: Exception) {
            Timber.e(e, "LocalFileBackend: upload failed")
            Result.Error(e.message ?: "Upload failed")
        }
    }

    override suspend fun download(destinationUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val src = getBackupFile()
            if (!src.exists()) return@withContext Result.Error("No local backup found")
            context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                src.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.Error("Cannot write to destination URI")
            Timber.d("LocalFileBackend: downloaded from ${src.absolutePath}")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "LocalFileBackend: download failed")
            Result.Error(e.message ?: "Download failed")
        }
    }

    override suspend fun hasBackup(): Boolean = withContext(Dispatchers.IO) {
        getBackupFile().exists()
    }

    /** URI del archivo para compartir vía `Intent.ACTION_SEND`. */
    fun getShareableUri(): Uri? {
        val file = getBackupFile()
        if (!file.exists()) return null
        return runCatching {
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }.getOrElse { Uri.fromFile(file) }
    }
}
