package ua.aerostat.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ua.aerostat.core.EnvelopeVariant
import ua.aerostat.core.InverseInput
import ua.aerostat.core.Project
import ua.aerostat.core.ProjectInput
import ua.aerostat.core.ShapeType
import ua.aerostat.data.ProjectStore
import ua.aerostat.ui.theme.ThemeMode
import java.util.UUID

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = ProjectStore(app)

    /**
     * Стан Compose (snapshot), щоб текстові поля оновлювалися синхронно під час введення.
     * null — ще завантажується.
     */
    var projects by mutableStateOf<List<Project>?>(null)
        private set

    private val pendingSave = MutableStateFlow<List<Project>?>(null)

    /** Стан екрана зворотного підбору. */
    var inverse by mutableStateOf(InverseInput())
        private set

    val themeMode: StateFlow<ThemeMode> =
        store.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.DARK)

    init {
        viewModelScope.launch { projects = store.projects.first() }
        // Автозбереження: кожна зміна зберігається, проміжні стани пропускаються.
        viewModelScope.launch {
            pendingSave.filterNotNull().conflate().collect { store.saveProjects(it) }
        }
    }

    private fun mutate(block: (List<Project>) -> List<Project>) {
        val current = projects ?: return
        val next = block(current)
        projects = next
        pendingSave.value = next
    }

    fun project(id: String): Project? = projects?.firstOrNull { it.id == id }

    fun nextDefaultName(): String = "Проєкт ${(projects?.size ?: 0) + 1}"

    fun create(name: String, shape: ShapeType, variant: EnvelopeVariant): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val p = Project(
            id = id,
            name = name.trim().ifEmpty { nextDefaultName() },
            createdAt = now,
            updatedAt = now,
            input = ProjectInput(shape = shape, variant = variant),
        )
        mutate { listOf(p) + it }
        return id
    }

    fun updateInput(id: String, transform: (ProjectInput) -> ProjectInput) = mutate { list ->
        list.map { if (it.id == id) it.copy(input = transform(it.input), updatedAt = System.currentTimeMillis()) else it }
    }

    fun rename(id: String, name: String) = mutate { list ->
        list.map { if (it.id == id) it.copy(name = name, updatedAt = System.currentTimeMillis()) else it }
    }

    fun delete(id: String) = mutate { list -> list.filterNot { it.id == id } }

    fun duplicate(id: String): String? {
        val src = project(id) ?: return null
        val now = System.currentTimeMillis()
        val copy = src.copy(id = UUID.randomUUID().toString(), name = "${src.name} (копія)", createdAt = now, updatedAt = now)
        mutate { listOf(copy) + it }
        return copy.id
    }

    fun updateInverse(transform: (InverseInput) -> InverseInput) {
        inverse = transform(inverse)
    }

    /** Створює проєкт із розмірів, підібраних зворотним розрахунком. */
    fun createFromInput(input: ProjectInput): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val p = Project(id = id, name = "Підбір: ${nextDefaultName().lowercase()}", createdAt = now, updatedAt = now, input = input)
        mutate { listOf(p) + it }
        return id
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { store.setThemeMode(mode) }
    }
}
