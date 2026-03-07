package com.github.origamiwolf.lomp.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OracleRepository(private val context: Context) {

    companion object {
        private val ORACLE_FOLDER_URI_KEY = stringPreferencesKey("oracle_folder_uri")
    }

    val folderUriFlow: Flow<Uri?> = context.dataStore.data
        .map { preferences ->
            preferences[ORACLE_FOLDER_URI_KEY]?.let { Uri.parse(it) }
        }

    suspend fun saveFolderUri(uri: Uri) {
        context.dataStore.edit { preferences ->
            preferences[ORACLE_FOLDER_URI_KEY] = uri.toString()
        }
    }

    suspend fun clearFolderUri() {
        context.dataStore.edit { preferences ->
            preferences.remove(ORACLE_FOLDER_URI_KEY)
        }
    }
}