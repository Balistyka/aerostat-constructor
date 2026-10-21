@file:OptIn(ExperimentalMaterial3Api::class)

package ua.aerostat.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ua.aerostat.core.HelpCategory
import ua.aerostat.core.HelpTopic
import ua.aerostat.core.HelpTopics
import ua.aerostat.ui.Uk
import ua.aerostat.ui.components.AeroTopBar
import ua.aerostat.ui.components.BrandLogo
import ua.aerostat.ui.components.BrigadeBadge
import ua.aerostat.ui.components.CircuitDivider
import ua.aerostat.ui.components.HelpDialog
import ua.aerostat.ui.components.Overline
import ua.aerostat.ui.components.SectionCard
import ua.aerostat.ui.theme.ThemeMode

@Composable
fun ReferenceScreen() {
    var open by remember { mutableStateOf<HelpTopic?>(null) }
    val grouped = remember { HelpTopics.all.groupBy { it.category } }
    CenteredColumn {
        Column(Modifier.fillMaxSize()) {
            AeroTopBar(Uk.REFERENCE_TITLE, onBack = null, brand = true)
            LazyColumn(
                Modifier.fillMaxSize().testTag("reference_list"),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(Uk.REFERENCE_SUBTITLE, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HelpCategory.entries.forEach { cat ->
                    val topics = grouped[cat].orEmpty()
                    if (topics.isNotEmpty()) {
                        item(key = "h_${cat.name}") { Overline(cat.title, Modifier.padding(top = 12.dp)) }
                        items(topics, key = { it.id }) { t ->
                            Card(
                                onClick = { open = t },
                                modifier = Modifier.fillMaxWidth().testTag("topic_${t.id}"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                shape = MaterialTheme.shapes.large,
                            ) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(t.title, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            t.definition, style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
    open?.let { HelpDialog(it) { open = null } }
}

@Composable
fun SettingsScreen(themeMode: ThemeMode, onThemeMode: (ThemeMode) -> Unit) {
    CenteredColumn {
        Column(Modifier.fillMaxSize()) {
            AeroTopBar(Uk.SETTINGS_TITLE, onBack = null, brand = true)
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SectionCard(title = Uk.SETTINGS_THEME) {
                    ThemeMode.entries.forEach { m ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(selected = themeMode == m, onClick = { onThemeMode(m) }, role = Role.RadioButton)
                                .padding(vertical = 6.dp)
                                .testTag("theme_${m.name}"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = themeMode == m, onClick = null)
                            Text(m.title, modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
                SectionCard(title = Uk.SETTINGS_PURPOSE) { Text(Uk.PURPOSE_TEXT) }
                SectionCard(title = Uk.LIMITS_TITLE) { Text(Uk.LIMITS_TEXT) }
                SectionCard(title = Uk.SETTINGS_ABOUT) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        BrandLogo(Modifier.width(96.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(Uk.APP_NAME, style = MaterialTheme.typography.titleMedium)
                            BrigadeBadge(dense = true)
                            Text(Uk.VERSION_LINE, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    CircuitDivider(nodes = 2)
                    Text(Uk.ABOUT_TEXT, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
