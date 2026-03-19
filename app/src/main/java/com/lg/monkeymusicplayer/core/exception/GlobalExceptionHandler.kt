package com.lg.monkeymusicplayer.core.exception

class GlobalExceptionHandler {

    class MusicPlayerException(message: String?) : Exception(message)
    class AudioSessionException(message: String?) : Exception(message)
    class PermissionException(message: String?) : Exception(message)
    class MusicScanException(message: String?) : Exception(message)
}