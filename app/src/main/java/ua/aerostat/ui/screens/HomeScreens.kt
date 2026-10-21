@file:OptIn(ExperimentalMaterial3Api::class)

package ua.aerostat.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ua.aerostat.core.Calculator
import ua.aerostat.core.Numbers
import ua.aerostat.core.Project
import ua.aerostat.ui.Uk
import ua.aerostat.ui.components.AeroTopBar
import ua.aerostat.ui.components.BrandMark
import ua.aerostat.ui.components.CircuitDivider
import ua.aerostat.ui.components.ConfirmDialog
import ua.aerostat.ui.components.ContentMaxWidth
import ua.aerostat.ui.components.EmptyState
import ua.aerostat.ui.components.HeroHeader
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
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 600.dp
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HeroHeader(
                    title = Uk.HOME_TITLE,
                    subtitle = Uk.HOME_SUBTITLE,
                    titleTag = "home_title",
                    logoWidth = if (wide) 132.dp else 104.dp,
                )
                CircuitDivider()

                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ActionCard(
                            Uk.NEW_PROJECT, Uk.NEW_PROJECT_TEXT, Uk.CREATE, Icons.Filled.Add,
                            primary = true, onClick = onNewProject, tag = "new_project", modifier = Modifier.weight(1f),
                        )
                        ActionCard(
                            Uk.INVERSE_CARD, Uk.INVERSE_CARD_TEXT, Uk.INVERSE_OPEN, Icons.Filled.Search,
                            primary = false, onClick = onInverse, tag = "open_inverse", modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    ActionCard(
                        Uk.NEW_PROJECT, Uk.NEW_PROJECT_TEXT, Uk.CREATE, Icons.Filled.Add,
                        primary = true, onClick = onNewProject, tag = "new_project",
                    )
                    ActionCard(
                        Uk.INVERSE_CARD, Uk.INVERSE_CARD_TEXT, Uk.INVERSE_OPEN, Icons.Filled.Search,
                        primary = false, onClick = onInverse, tag = "open_inverse",
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Text(Uk.RECENT, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    if (!projects.isNullOrEmpty()) {
                        TextButton(onClick = onAllProjects) {
                            Text(Uk.ALL_PROJECTS)
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                when {
                    projects == null -> Text(Uk.LOADING, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    projects.isEmpty() -> EmptyState(Uk.NO_PROJECTS_TITLE, Uk.NO_PROJECTS_TEXT)
                    else -> projects.sortedByDescending { it.updatedAt }.take(3).forEach { p ->
                        ProjectCard(p, onOpen = { onOpenProject(p.id) })
                    }
                }

                Spacer(Modifier.height(4.dp))
                CircuitDivider(nodes = 2)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BrandMark(Modifier.size(26.dp), alpha = 0.75f)
                    Column {
                        Text(
                            Uk.BRIGADE_FULL,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            Uk.VERSION_LINE,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    text: String,
    action: String,
    icon: ImageVector,
    primary: Boolean,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (primary) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = if (primary) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    icon, contentDescription = null,
                    tint = if (primary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (primary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (primary) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (primary) {
                Button(onClick = onClick, modifier = Modifier.testTag(tag)) { Text(action) }
            } else {
                OutlinedButton(onClick = onClick, modifier = Modifier.testTag(tag)) { Text(action) }
            }
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(Modifier.padding(start = 20.dp, top = 16.dp, bottom = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(project.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${project.input.shape.title} · ${project.input.variant.title}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    val payload = result.lift?.payload
                    val mass = result.totalMass
                    Text(
                        when {
                            payload != null -> "${Numbers.format(payload)} кг вантажу"
                            mass != null -> "${Numbers.format(mass, 3)} кг"
                            else -> Uk.RESULT_NOT_COMPUTED
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    StatusBadge(result.status)
                }
                Text(
                    "${Uk.UPDATED} ${formatDate(project.updatedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
            AeroTopBar(Uk.PROJECTS_TITLE, onBack = null, brand = true) {
                IconButton(onClick = onNew) { Icon(Icons.Filled.Add, contentDescription = Uk.NEW_PROJECT) }
            }
            when {
                projects == null -> Loading()
                projects.isEmpty() -> EmptyState(Uk.NO_PROJECTS_TITLE, Uk.NO_PROJECTS_TEXT) {
                    Button(onClick = onNew) { Text(Uk.NEW_PROJECT) }
                }
                else -> LazyColumn(
                    Modifier.fillMaxSize().testTag("projects_list"),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
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
internal fun ProjectStatus(project: Project) {
    Overline(project.input.shape.title)
}
