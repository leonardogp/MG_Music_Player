package com.lg.monkeymusicplayer.core.cast

import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.MediaIntentReceiver
import com.google.android.gms.cast.framework.media.NotificationOptions

/**
 * CastOptionsProvider — configura el Cast SDK para Monkey Music Player.
 *
 * Registrado en AndroidManifest.xml como meta-data de la Application:
 * ```xml
 * <meta-data
 *     android:name="com.google.android.gms.cast.framework.OPTIONS_PROVIDER_CLASS_NAME"
 *     android:value="com.lg.monkeymusicplayer.core.cast.CastOptionsProvider" />
 * ```
 *
 * Usa el Default Media Receiver de Google (CC1AD845) que acepta streams de audio
 * sin necesidad de una app Cast personalizada. Para una experiencia branded completa,
 * reemplazar con un App ID registrado en la Google Cast SDK Developer Console.
 */
class CastOptionsProvider : OptionsProvider {

    companion object {
        /** Default Media Receiver App ID — funciona sin registro en Cast Console. */
        const val APP_ID = "CC1AD845"
    }

    override fun getCastOptions(context: Context): CastOptions {
        val notificationOptions = NotificationOptions.Builder()
            .setActions(
                listOf(
                    MediaIntentReceiver.ACTION_SKIP_PREV,
                    MediaIntentReceiver.ACTION_TOGGLE_PLAYBACK,
                    MediaIntentReceiver.ACTION_SKIP_NEXT,
                    MediaIntentReceiver.ACTION_STOP_CASTING
                ),
                intArrayOf(1, 2, 3)  // índices de las acciones en la notificación
            )
            .setTargetActivityClassName(
                "com.lg.monkeymusicplayer.ui.MainActivity"
            )
            .build()

        val mediaOptions = CastMediaOptions.Builder()
            .setNotificationOptions(notificationOptions)
            .build()

        return CastOptions.Builder()
            .setReceiverApplicationId(APP_ID)
            .setCastMediaOptions(mediaOptions)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null
}
