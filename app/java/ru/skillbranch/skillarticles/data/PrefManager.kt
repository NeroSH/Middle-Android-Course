package ru.skillbranch.skillarticles.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import ru.skillbranch.skillarticles.App
import ru.skillbranch.skillarticles.data.adapters.UserJsonAdapter
import ru.skillbranch.skillarticles.data.delegates.PrefDelegate
import ru.skillbranch.skillarticles.data.delegates.PrefObjDelegate

val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

class PrefManager(context: Context = App.applicationContext()) {

    val dataStore = context.dataStore
    private val errorHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("M_PrefManager", "${throwable.message}")
    }
    internal val scope = CoroutineScope(SupervisorJob() + errorHandler)

    var isBigText: Boolean by PrefDelegate(false)
    var isDarkMode: Boolean by PrefDelegate(false)

    val settings: LiveData<AppSettings>
        get() {
            val isDarkMode =
                dataStore.data.map { it[booleanPreferencesKey(this::isDarkMode.name)] ?: false }
            val isBigText =
                dataStore.data.map { it[booleanPreferencesKey(this::isBigText.name)] ?: false }

            return isDarkMode.zip(isBigText) { darkMode, bigText -> AppSettings(darkMode, bigText) }
                .onEach { Log.d("M_PrefManager-", "settings $it") }
                .distinctUntilChanged()
                .asLiveData()

        }

    var testInt by PrefDelegate(Int.MAX_VALUE)
    var testLong by PrefDelegate(Long.MAX_VALUE)
    var testDouble by PrefDelegate(Double.MAX_VALUE)
    var testFloat by PrefDelegate(Float.MAX_VALUE)
    var testString by PrefDelegate("test")
    var testBoolean by PrefDelegate(false)
    var testUser by PrefObjDelegate(UserJsonAdapter())
}