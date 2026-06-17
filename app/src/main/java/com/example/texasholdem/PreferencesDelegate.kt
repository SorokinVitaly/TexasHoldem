package com.example.texasholdem

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


interface PrefsOwner {
    val prefs: SharedPreferences
}

class PreferencesDelegate<T>(val keyName: String, val defaultValue: T) : ReadWriteProperty<PrefsOwner, T> {
    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: PrefsOwner, property: KProperty<*>): T =
        with(thisRef.prefs) {
            when (defaultValue) {
                is Boolean -> getBoolean(keyName, defaultValue)
                is Int -> getInt(keyName, defaultValue)
                is Long -> getLong(keyName, defaultValue)
                is Float -> getFloat(keyName, defaultValue)
                is String -> getString(keyName, defaultValue)
                else -> throw IllegalArgumentException("Unsupported type")
            }
        } as T

    override fun setValue(thisRef: PrefsOwner, property: KProperty<*>, value: T) {
        thisRef.prefs.edit {
            when (value) {
                is Boolean -> putBoolean(keyName, value)
                is Int -> putInt(keyName, value)
                is Long -> putLong(keyName, value)
                is Float -> putFloat(keyName, value)
                is String -> putString(keyName, value)
                else -> throw IllegalArgumentException("Unsupported type")
            }
        }
    }
}