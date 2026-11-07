package app.lembar

import android.app.Application
import android.content.Context
import app.lembar.data.AppDatabase
import app.lembar.data.Repository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("lembar", Context.MODE_PRIVATE)
    private val _theme = MutableStateFlow(
        runCatching { ThemeMode.valueOf(sp.getString("theme", null) ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM),
    )
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()

    fun setTheme(mode: ThemeMode) {
        sp.edit().putString("theme", mode.name).apply()
        _theme.value = mode
    }
}

class LembarApp : Application() {
    lateinit var repository: Repository
        private set
    lateinit var prefs: Prefs
        private set

    private val openChannel = Channel<String>(Channel.BUFFERED)

    /** Page ids to open, e.g. a page just created from text shared by another app. */
    val openRequests: Flow<String> = openChannel.receiveAsFlow()

    fun openPage(id: String) {
        openChannel.trySend(id)
    }

    override fun onCreate() {
        super.onCreate()
        repository = Repository(AppDatabase.build(this))
        prefs = Prefs(this)
    }
}
