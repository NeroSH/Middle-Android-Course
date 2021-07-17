package ru.skillbranch.skillarticles.data.delegates

import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ru.skillbranch.skillarticles.data.PrefManager
import ru.skillbranch.skillarticles.data.adapters.JsonAdapter
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PrefObjDelegate<T>(
    private val adapter: JsonAdapter<T>,
    private val customKey: String? = null
) {
    operator fun provideDelegate(
        thisRef: PrefManager,
        prop: KProperty<*>
    ): ReadWriteProperty<PrefManager, T?> {
        val key = PrefDelegate("").createKey(customKey ?: prop.name, "")

        return object : ReadWriteProperty<PrefManager, T?> {
            private var storedValue: T? = null

            override fun getValue(thisRef: PrefManager, property: KProperty<*>): T? {
                if (storedValue == null) {
                    val flowValue = thisRef.dataStore.data
                        .map { it[key] }
                    storedValue = runBlocking {
                        flowValue.firstOrNull()?.let { adapter.fromJson(it) }
                    }
                }
                return storedValue
            }

            override fun setValue(thisRef: PrefManager, property: KProperty<*>, value: T?) {
                storedValue = value
                thisRef.scope.launch {
                    thisRef.dataStore.edit {
                        it[key] = adapter.toJson(value)
                    }
                }
            }
        }
    }
}
