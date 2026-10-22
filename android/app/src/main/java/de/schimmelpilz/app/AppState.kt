package de.schimmelpilz.app

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.schimmelpilz.app.data.Api
import de.schimmelpilz.app.data.ApiException
import de.schimmelpilz.app.data.CaseFile
import de.schimmelpilz.app.data.InboxItem
import de.schimmelpilz.app.data.SessionCookieJar
import de.schimmelpilz.app.data.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class SchimmelpilzApp : Application()

enum class ThemeMode { System, Light, Dark }

/** App-wide state: session, language, theme and the cached case/inbox lists every tab shares. */
class AppState(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("schimmelpilz", Context.MODE_PRIVATE)
    val api = Api(BuildConfig.API_BASE_URL, SessionCookieJar(app.getSharedPreferences("session", Context.MODE_PRIVATE)))

    val lang = MutableStateFlow(prefs.getString("lang", null) ?: if (Locale.getDefault().language == "de") "de" else "en")
    val themeMode = MutableStateFlow(runCatching { ThemeMode.valueOf(prefs.getString("theme", "System")!!) }.getOrDefault(ThemeMode.System))

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user
    /** null while the stored session is being checked at start-up. */
    val sessionChecked = MutableStateFlow(false)

    val cases = MutableStateFlow<List<CaseFile>>(emptyList())
    val inbox = MutableStateFlow<List<InboxItem>>(emptyList())
    val loadError = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            _user.value = runCatching { api.me() }.getOrNull()
            sessionChecked.value = true
            if (_user.value != null) refresh()
        }
    }

    fun setLang(value: String) { lang.value = value; prefs.edit { putString("lang", value) } }
    fun setTheme(value: ThemeMode) { themeMode.value = value; prefs.edit { putString("theme", value.name) } }

    fun signedIn(user: User) { _user.value = user; refresh() }

    /** After the server deleted the account: forget everything locally, back to the welcome screen. */
    fun accountDeleted() { _user.value = null; cases.value = emptyList(); inbox.value = emptyList() }

    fun signOut() = viewModelScope.launch {
        api.logout(); _user.value = null; cases.value = emptyList(); inbox.value = emptyList()
    }

    fun refresh() = viewModelScope.launch {
        try {
            cases.value = api.cases().filter { it.archivedAt == null }
            inbox.value = api.inbox()
            loadError.value = null
        } catch (error: ApiException) {
            if (error.status == 401) _user.value = null else loadError.value = error.detail
        }
    }
}
