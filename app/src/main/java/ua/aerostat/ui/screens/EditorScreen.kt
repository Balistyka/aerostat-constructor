@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package ua.aerostat.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import ua.aerostat.core.CalcResult
import ua.aerostat.core.Calculator
import ua.aerostat.core.EnvelopeVariant
import ua.aerostat.core.Field
import ua.aerostat.core.FilmMaterial
import ua.aerostat.core.HelpTopic
import ua.aerostat.core.HelpTopics
import ua.aerostat.core.Numbers
import ua.aerostat.core.ParsedNumber
import ua.aerostat.core.Project
import ua.aerostat.core.ProjectInput
import ua.aerostat.core.ShapeType
import ua.aerostat.ui.Uk
import ua.aerostat.ui.components.AeroTopBar
import ua.aerostat.ui.components.ConfirmDialog
import ua.aerostat.ui.components.HelpDialog
import ua.aerostat.ui.components.NumberField
import ua.aerostat.ui.components.SectionCard
import ua.aerostat.ui.components.StatusBadge
import ua.aerostat.ui.components.ValueRow

private val materialFields = setOf(
    Field.DENSITY, Field.THICKNESS, Field.LAYERS, Field.WASTE, Field.SEAM, Field.ROLL_WIDTH, Field.FITTINGS,
)

@Composable
fun EditorScreen(
    project: Project?,
    onBack: () -> Unit,
    onInputChange: ((ProjectInput) -> ProjectInput) -> Unit,
    onRename: (String) -> Unit,
    onOpenResults: () -> Unit,
) {
    if (project == null) {
        Column { AeroTopBar(Uk.EDITOR_TITLE, onBack); Text(Uk.NOT_FOUND, Modifier.padding(20.dp)) }
        return
    }
    val input = project.input
    val result = remember(input) { Calculator.calculate(input) }
    var help by remember { mutableStateOf<HelpTopic?>(null) }
    var confirmReset by remember { mutableStateOf(false) }

    CenteredColumn {
        Column(Modifier.fillMaxSize()) {
            AeroTopBar(Uk.EDITOR_TITLE, onBack) {
                IconButton(onClick = { confirmReset = true }, modifier = Modifier.testTag("reset")) {
                    Icon(Icons.Filled.Refresh, contentDescription = Uk.RESET)
                }
            }
            BoxWithConstraints(Modifier.fillMaxSize().imePadding()) {
                val wide = maxWidth >= 720.dp
                if (wide) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Column(
                            Modifier.weight(1.15f).verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) { EditorForm(project, result, onInputChange, onRename) { help = it } }
                        Column(
                            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) { LiveSummary(result, onOpenResults) }
                    }
                } else {
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        EditorForm(project, result, onInputChange, onRename) { help = it }
                        LiveSummary(result, onOpenResults)
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    help?.let { HelpDialog(it) { help = null } }
    if (confirmReset) {
        ConfirmDialog(
            title = Uk.RESET_TITLE, text = Uk.RESET_TEXT, confirm = Uk.RESET_CONFIRM,
            onConfirm = {
                onInputChange { it.resetValues().copy(notes = it.notes) }
                confirmReset = false
            },
            onDismiss = { confirmReset = false },
        )
    }
}

@Composable
private fun EditorForm(
    project: Project,
    result: CalcResult,
    onInputChange: ((ProjectInput) -> ProjectInput) -> Unit,
    onRename: (String) -> Unit,
    onHelp: (HelpTopic) -> Unit,
) {
    val input = project.input
    OutlinedTextField(
        value = project.name,
        onValueChange = { onRename(it.take(80)) },
        label = { Text(Uk.PROJECT_NAME) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("project_name"),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
        shape = MaterialTheme.shapes.medium,
    )

    SectionCard(title = Uk.SECTION_SHAPE) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ShapeType.entries.forEach { s ->
                FilterChip(
                    selected = input.shape == s,
                    onClick = { onInputChange { it.copy(shape = s) } },
                    label = { Text(s.title) },
                    modifier = Modifier.testTag("chip_shape_${s.name}"),
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            EnvelopeVariant.entries.forEach { v ->
                FilterChip(
                    selected = input.variant == v,
                    onClick = { onInputChange { it.copy(variant = v) } },
                    label = { Text(v.title) },
                    modifier = Modifier.testTag("chip_variant_${v.name}"),
                )
            }
        }
        StatusBadge(result.status)
        Text(result.status.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    val geometryFields = result.relevantFields.filter { it !in materialFields }
    if (result.unavailableReason != null) {
        SectionCard(title = Uk.SECTION_DIMENSIONS, onHelp = { onHelp(HelpTopics.general.first { it.id == "pear" }) }) {
            Text(result.unavailableReason, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("unavailable"))
        }
    } else if (geometryFields.isNotEmpty()) {
        SectionCard(title = Uk.SECTION_DIMENSIONS) {
            geometryFields.forEach { f -> FieldInput(f, input, result, onInputChange, onHelp) }
        }
    }

    SectionCard(title = Uk.SECTION_MATERIAL) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FilmMaterial.entries.forEach { m ->
                FilterChip(
                    selected = input.material == m,
                    onClick = {
                        onInputChange {
                            val d = m.density
                            if (d != null) it.copy(material = m, density = Numbers.toInput(d)) else it.copy(material = m)
                        }
                    },
                    label = { Text(m.title) },
                    modifier = Modifier.testTag("chip_material_${m.name}"),
                )
            }
        }
        result.relevantFields.filter { it in materialFields }.forEach { f ->
            FieldInput(f, input, result, onInputChange, onHelp)
        }
    }

    SectionCard(title = Uk.SECTION_NOTES) {
        OutlinedTextField(
            value = input.notes,
            onValueChange = { v -> onInputChange { it.copy(notes = v.take(2000)) } },
            placeholder = { Text(Uk.NOTES_HINT) },
            modifier = Modifier.fillMaxWidth().testTag("notes"),
            minLines = 2,
            shape = MaterialTheme.shapes.medium,
        )
    }
    Text(Uk.AUTOSAVED, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun FieldInput(
    field: Field,
    input: ProjectInput,
    result: CalcResult,
    onInputChange: ((ProjectInput) -> ProjectInput) -> Unit,
    onHelp: (HelpTopic) -> Unit,
) {
    NumberField(
        label = field.label,
        unit = field.unit,
        value = Calculator.rawValue(input, field),
        error = result.errors[field],
        onValueChange = { text ->
            val t = text.take(20)
            onInputChange { cur ->
                var next = Calculator.withValue(cur, field, t)
                if (field == Field.DENSITY && cur.material != FilmMaterial.CUSTOM) {
                    val preset = cur.material.density
                    val typed = (Numbers.parse(t) as? ParsedNumber.Value)?.value
                    if (typed != preset) next = next.copy(material = FilmMaterial.CUSTOM)
                }
                next
            }
        },
        onHelp = { onHelp(HelpTopics.forField(field)) },
        placeholder = if (field == Field.GORE_COUNT) Uk.GORES_AUTO else null,
        tag = "field_${field.name}",
    )
}

@Composable
private fun LiveSummary(result: CalcResult, onOpenResults: () -> Unit) {
    SectionCard(title = Uk.LIVE_RESULT, modifier = Modifier.testTag("live_summary")) {
        when {
            result.unavailableReason != null -> Text(ua.aerostat.core.ModelStatus.UNAVAILABLE.title, color = MaterialTheme.colorScheme.error)
            result.hasErrors && result.filmMass == null -> Text(Uk.FIX_ERRORS, color = MaterialTheme.colorScheme.error)
            else -> {
                result.filmMass?.let { ValueRow(Uk.RESULT_FILM_MASS, "${Numbers.format(it, 3)} кг", emphasize = true) }
                result.blankArea?.let { ValueRow(Uk.RESULT_BLANK_AREA, "${Numbers.format(it)} м²") }
                result.totalMass?.let { ValueRow(Uk.RESULT_TOTAL, "${Numbers.format(it, 3)} кг") }
                if (result.hasErrors) Text(Uk.FIX_ERRORS, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
        Button(onClick = onOpenResults, modifier = Modifier.fillMaxWidth().testTag("open_results")) {
            Text(Uk.OPEN_RESULTS, modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}
