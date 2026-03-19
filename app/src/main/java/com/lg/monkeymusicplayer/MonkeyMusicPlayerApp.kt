import android.app.Application

class MonkeyMusicPlayerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLogger.initialize() // Initialize your AppLogger
        GlobalExceptionHandler.setup() // Setup the Global Exception Handler
    }
}