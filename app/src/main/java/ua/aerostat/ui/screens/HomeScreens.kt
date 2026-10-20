@file:OptIn(ExperimentalMaterial3Api::class)

package ua.aerostat.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ua.aerostat.core.Calculator
import ua.aerostat.core.Numbers
import ua.aerostat.core.Project
import ua.aerostat.ui.Uk
import ua.aerostat.ui.components.AeroTopBar
import ua.aerostat.ui.components.ConfirmDialog
import ua.aerostat.ui.components.ContentMaxWidth
import ua.aerostat.ui.components.EmptyState
import ua.aerostat.ui.components.Overline
import ua.aerostat.ui.components.StatusBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ukLocale = Locale("uk", "UA")
fun formatDate(ms: Long): String = SimpleDateFormat("dd.MM.yyyy HH:mm", ukLocale).format(Date(ms))

@Composable
fun Loading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun CenteredColumn(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth()) { content() }
    }
}

@Composable
fun HomeScreen(
    projects: List<Project>?,
    onNewProject: () -> Unit,
    onInverse: () -> Unit,
    onOpenProject: (String) -> Unit,
    onAllProjects: () -> Unit,
) {
    CenteredColumn {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Overline(Uk.APP_OVERLINE)
            Text(Uk.HOME_TITLE, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.testTag("home_title"))
            Text(Uk.HOME_SUBTITLE, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(Uk.NEW_PROJECT, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(Uk.NEW_PROJECT_TEXT, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f))
                    Button(onClick = onNewProject, modifier = Modifier.testTag("new_project")) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.padding(start = 6.dp))
                        Text(Uk.CREATE)
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(Uk.INVERSE_CARD, style = MaterialTheme.typography.titleLarge)
                    Text(Uk.INVERSE_CARD_TEXT, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = onInverse, modifier = Modifier.testTag("open_inverse")) {
                        Text(Uk.INVERSE_OPEN)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Uk.RECENT, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (!projects.isNullOrEmpty()) TextButton(onClick = onAllProjects) { Text(Uk.ALL_PROJECTS) }
            }
            when {
                projects == null -> Text(Uk.LOADING, color = MaterialTheme.colorScheme.onSurfaceVariant)
                projects.isEmpty() -> EmptyState(Uk.NO_PROJECTS_TITLE, Uk.NO_PROJECTS_TEXT)
                else -> projects.sortedByDescending { it.updatedAt }.take(3).forEach { p ->
                    ProjectCard(p, onOpen = { onOpenProject(p.id) })
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(Uk.VERSION_LINE, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ProjectCard(
    project: Project,
    onOpen: () -> Unit,
    onDuplicate: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val result = remember(project.input) { Calculator.calculate(project.input) }
    var menu by remember { mutableStateOf(false) }
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().testTag("project_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(Modifier.padding(start = 20.dp, top = 16.dp, bottom = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(project.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${project.input.shape.title} · ${project.input.variant.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    val mass = result.totalMass
                    Text(
                        if (mass != null) "${Numbers.format(mass, 3)} кг" else Uk.RESULT_NOT_COMPUTED,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "${Uk.UPDATED} ${formatDate(project.updatedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (onDuplicate != null || onDelete != null) {
                Box {
                    IconButton(onClick = { menu = true }, modifier = Modifier.testTag("project_menu")) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Дії з проєктом")
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(Uk.OPEN) }, onClick = { menu = false; onOpen() })
                        if (onDuplicate != null) DropdownMenuItem(text = { Text(Uk.DUPLICATE) }, onClick = { menu = false; onDuplicate() })
                        if (onDelete != null) DropdownMenuItem(
                            text = { Text(Uk.DELETE) },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                            onClick = { menu = false; onDelete() },
                            modifier = Modifier.testTag("menu_delete"),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectsScreen(
    projects: List<Project>?,
    onOpen: (String) -> Unit,
    onNew: () -> Unit,
    onDuplicate: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var toDelete by remember { mutableStateOf<Project?>(null) }
    CenteredColumn {
        Column(Modifier.fillMaxSize()) {
            AeroTopBar(Uk.PROJECTS_TITLE, onBack = null) {
                IconButton(onClick = onNew) { Icon(Icons.Filled.Add, contentDescription = Uk.NEW_PROJECT) }
            }
            when {
                projects == null -> Loading()
                projects.isEmpty() -> EmptyState(Uk.NO_PROJECTS_TITLE, Uk.NO_PROJECTS_TEXT) {
                    Button(onClick = onNew) { Text(Uk.NEW_PROJECT) }
                }
                else -> LazyColumn(
                    Modifier.fillMaxSize().testTag("projects_list"),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(projects.sortedByDescending { it.updatedAt }, key = { it.id }) { p ->
                        ProjectCard(
                            p,
                            onOpen = { onOpen(p.id) },
                            onDuplicate = { onDuplicate(p.id) },
                            onDelete = { toDelete = p },
                        )
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }
    toDelete?.let { p ->
        ConfirmDialog(
            title = Uk.DELETE_TITLE,
            text = "«${p.name}». ${Uk.DELETE_TEXT}",
            confirm = Uk.DELETE,
            onConfirm = { onDelete(p.id); toDelete = null },
            onDismiss = { toDelete = null },
        )
    }
}

@Composable
internal fun StatusLine(project: Project) {
    StatusBadge(project.input.shape.status)
}
