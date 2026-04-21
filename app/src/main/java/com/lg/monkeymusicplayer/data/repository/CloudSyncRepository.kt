package com.lg.monkeymusicplayer.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.lg.monkeymusicplayer.core.result.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CloudSyncRepository — orquesta la sincronización entre [BackupRepository] y [CloudBackend].
 *
 * Actúa como coordinador entre dos contratos:
 * - [BackupRepository]: serializa/deserializa los datos de Room a/desde archivo local.
 * - [CloudBackend]: transfiere el archivo local al/desde el almacenamiento remoto.
 *
 * El backend activo se inyecta vía Hilt. Para cambiar a Firebase o Drive,
 * solo cambiar el binding en AppModule sin tocar esta clase.
 */
@Singleton
class CloudSyncRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupRepository: BackupRepository,
    private val cloudBackend: CloudBackend
) {
    companion object {
        private const val TEMP_FILENAME = "monkey_sync_temp.json"
    }

    suspend fun upload(): Result<Uri> = withContext(Dispatchers.IO) {
        val tempFile = getTempFile()
        try {
            val tempUri = getUri(tempFile)
                ?: return@withContext Result.Error("FileProvider not available")
            val exportResult = backupRepository.exportBackup(tempUri)
            if (exportResult is Result.Error) return@withContext Result.Error(exportResult.message)
            val uploadResult = cloudBackend.upload(tempUri)
            if (uploadResult is Result.Error) return@withContext Result.Error(uploadResult.message)
            Timber.d("CloudSync: upload OK")
            Result.Success(tempUri)
        } catch (e: Exception) {
            Timber.e(e, "CloudSync: upload failed")
            Result.Error(e.message ?: "Unknown error during upload")
        } finally {
            tempFile.delete()
        }
    }

    suspend fun download(): Result<Unit> = withContext(Dispatchers.IO) {
        val tempFile = getTempFile()
        try {
            if (!cloudBackend.hasBackup()) return@withContext Result.Error("No cloud backup found")
            val tempUri = getUri(tempFile)
                ?: return@withContext Result.Error("FileProvider not available")
            val downloadResult = cloudBackend.download(tempUri)
            if (downloadResult is Result.Error) return@withContext Result.Error(downloadResult.message)
            val importResult = backupRepository.importBackup(tempUri)
            if (importResult is Result.Error) return@withContext Result.Error(importResult.message)
            Timber.d("CloudSync: download + import OK")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "CloudSync: download failed")
            Result.Error(e.message ?: "Unknown error during download")
        } finally {
            tempFile.delete()
        }
    }

    suspend fun hasCloudBackup(): Boolean = cloudBackend.hasBackup()

    private fun getTempFile(): File = File(context.filesDir, TEMP_FILENAME)

    private fun getUri(file: File): Uri? = runCatching {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrElse { Uri.fromFile(file) }
}
