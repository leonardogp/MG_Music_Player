package com.lg.monkeymusicplayer.data.repository

import android.net.Uri
import com.lg.monkeymusicplayer.core.result.Result

/**
 * CloudBackend — contrato para cualquier backend de almacenamiento en nube.
 *
 * ## Backends disponibles
 * - [LocalFileBackend]: escribe en `getExternalFilesDir()`. Activo por defecto.
 *   El archivo puede sincronizarse manualmente con Drive/Dropbox.
 *
 * ## Backends futuros (implementar esta interfaz)
 * - `FirebaseStorageBackend`: usa Firebase Storage SDK.
 * - `GoogleDriveBackend`: usa Google Drive REST API con OAuth2.
 * - `DropboxBackend`: usa Dropbox SDK.
 *
 * Para cambiar el backend activo, modificar el binding en `AppModule`:
 * ```kotlin
 * @Provides @Singleton
 * fun provideCloudBackend(...): CloudBackend = FirebaseStorageBackend(...)
 * ```
 */
interface CloudBackend {
    /**
     * Sube el contenido del [sourceUri] al backend de nube.
     * @param sourceUri URI del archivo de backup local (generado por BackupRepository).
     * @return [Result.Success] con la URL/URI remota, o [Result.Error].
     */
    suspend fun upload(sourceUri: Uri): Result<String>

    /**
     * Descarga el backup del backend y lo escribe en [destinationUri].
     * @param destinationUri URI local donde escribir el backup descargado.
     * @return [Result.Success] si la descarga fue exitosa, o [Result.Error].
     */
    suspend fun download(destinationUri: Uri): Result<Unit>

    /** True si hay un backup disponible en el backend. */
    suspend fun hasBackup(): Boolean
}
