package ua.aerostat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ua.aerostat.core.Project
import ua.aerostat.ui.theme.ThemeMode
import java.io.IOException

@Serializable
data class ProjectsFile(val version: Int = 1, val projects: List<Project> = emptyList())

/** JSON-кодек сховища проєктів. Пошкоджені дані не призводять до падіння. */
object ProjectCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    fun encode(projects: List<Project>): String = json.encodeToString(ProjectsFile.serializer(), ProjectsFile(projects = projects))

    fun decode(text: String?): List<Project> {
        if (text.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(ProjectsFile.serializer(), text).projects
        } catch (e: Exception) {
            emptyList()
        }
    }
}

private val Context.aerostatStore: DataStore<Preferences> by preferencesDataStore(name = "aerostat")

/** Локальне сховище на пристрої (DataStore). Працює офлайн. */
class ProjectStore(context: Context) {
    private val store = context.applicationContext.aerostatStore
    private val projectsKey = stringPreferencesKey("projects_json")
    private val themeKey = stringPreferencesKey("theme_mode")

    private val data: Flow<Preferences> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val projects: Flow<List<Project>> = data.map { ProjectCodec.decode(it[projectsKey]) }

    val themeMode: Flow<ThemeMode> = data.map { prefs ->
        ThemeMode.entries.firstOrNull { it.name == prefs[themeKey] } ?: ThemeMode.DARK
    }

    suspend fun saveProjects(projects: List<Project>) {
        store.edit { it[projectsKey] = ProjectCodec.encode(projects) }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[themeKey] = mode.name }
    }
}
