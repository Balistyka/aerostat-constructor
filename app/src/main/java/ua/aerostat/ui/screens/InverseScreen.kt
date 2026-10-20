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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ua.aerostat.core.Field
import ua.aerostat.core.FilmMaterial
import ua.aerostat.core.GasType
import ua.aerostat.core.HelpTopic
import ua.aerostat.core.HelpTopics
import ua.aerostat.core.Inverse
import ua.aerostat.core.InverseInput
import ua.aerostat.core.InverseResult
import ua.aerostat.core.Numbers
import ua.aerostat.core.ProjectInput
import ua.aerostat.core.ShapeType
import ua.aerostat.ui.Uk
import ua.aerostat.ui.components.AeroTopBar
import ua.aerostat.ui.components.HelpDialog
import ua.aerostat.ui.components.NumberField
import ua.aerostat.ui.components.SectionCard
import ua.aerostat.ui.components.StatusBadge
import ua.aerostat.ui.components.ValueRow
import ua.aerostat.ui.theme.NumberStyle

private val targetFields = listOf(Field.TARGET_PAYLOAD, Field.TARGET_ALTITUDE, Field.FLIGHT_HOURS, Field.LIFT_LOSS_RATE)
private val materialFields = listOf(Field.THICKNESS, Field.DENSITY, Field.LAYERS, Field.WASTE, Field.FITTINGS)
private val gasFields = listOf(Field.FILL_FACTOR, Field.BALLAST_PERCENT, Field.GROUND_TEMP, Field.GROUND_PRESSURE)

@Composable
fun InverseScreen(
    input: InverseInput,
    onChange: ((InverseInput) -> InverseInput) -> Unit,
    onCreateProject: (ProjectInput) -> Unit,
    onBack: () -> Unit,
) {
    val result = remember(input) { Inverse.solve(input) }
    var help by remember { mutableStateOf<HelpTopic?>(null) }

    CenteredColumn {
        Column(Modifier.fillMaxSize()) {
            AeroTopBar(Uk.INVERSE_TITLE, onBack)
            BoxWithConstraints(Modifier.fillMaxSize().imePadding()) {
                val wide = maxWidth >= 720.dp
                if (wide) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Column(
                            Modifier.weight(1.1f).verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) { InverseForm(input, result, onChange) { help = it } }
                        Column(
                            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) { InverseResultCard(result, onCreateProject) { help = it } }
                    }
                } else {
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        InverseForm(input, result, onChange) { help = it }
                        InverseResultCard(result, onCreateProject) { help = it }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
    help?.let { HelpDialog(it) { help = null } }
}

@Composable
private fun InverseForm(
    input: InverseInput,
    result: InverseResult,
    onChange: ((InverseInput) -> InverseInput) -> Unit,
    onHelp: (HelpTopic) -> Unit,
) {
    @Composable
    fun field(f: Field) = NumberField(
        label = f.label,
        unit = f.unit,
        value = Inverse.rawValue(input, f),
        error = result.errors[f],
        onValueChange = { text -> onChange { Inverse.withValue(it, f, text.take(20)) } },
        onHelp = { onHelp(HelpTopics.forField(f)) },
        placeholder = if (f == Field.FLIGHT_HOURS) "не враховувати" else null,
        tag = "inv_${f.name}",
    )

    SectionCard(title = Uk.INVERSE_SECTION_TARGET, onHelp = { onHelp(HelpTopics.general.first { it.id == "inverse" }) }) {
        targetFields.forEach { field(it) }
    }

    SectionCard(title = Uk.INVERSE_SECTION_SHAPE) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ShapeType.entries.forEach { s ->
                FilterChip(
                    selected = input.shape == s,
                    onClick = { onChange { i -> i.copy(base = i.base.copy(shape = s)) } },
                    label = { Text(s.title) },
                    modifier = Modifier.testTag("inv_shape_${s.name}"),
                )
            }
        }
        StatusBadge(input.shape.status)
        Inverse.fields(input).filter { it !in targetFields && it !in materialFields && it !in gasFields }
            .forEach { field(it) }
    }

    SectionCard(title = Uk.SECTION_MATERIAL) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FilmMaterial.entries.forEach { m ->
                FilterChip(
                    selected = input.base.material == m,
                    onClick = {
                        onChange { i ->
                            val d = m.density
                            if (d != null) i.copy(base = i.base.copy(material = m, density = Numbers.toInput(d)))
                            else i.copy(base = i.base.copy(material = m))
                        }
                    },
                    label = { Text(m.title) },
                    modifier = Modifier.testTag("inv_material_${m.name}"),
                )
            }
        }
        materialFields.forEach { field(it) }
    }

    SectionCard(title = Uk.SECTION_GAS) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            GasType.entries.forEach { g ->
                FilterChip(
                    selected = input.base.gas == g,
                    onClick = { onChange { i -> i.copy(base = i.base.copy(gas = g)) } },
                    label = { Text(g.title) },
                    modifier = Modifier.testTag("inv_gas_${g.name}"),
                )
            }
        }
        gasFields.forEach { field(it) }
    }
}

@Composable
private fun InverseResultCard(
    result: InverseResult,
    onCreateProject: (ProjectInput) -> Unit,
    onHelp: (HelpTopic) -> Unit,
) {
    SectionCard(
        title = Uk.INVERSE_SECTION_RESULT,
        onHelp = { onHelp(HelpTopics.general.first { it.id == "inverse" }) },
        modifier = Modifier.testTag("inverse_result"),
    ) {
        val s = result.solution
        when {
            result.hasErrors -> Text(Uk.INVERSE_FIX, color = MaterialTheme.colorScheme.error)
            s == null -> {
                Text(Uk.INVERSE_IMPOSSIBLE, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                result.failureText?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("inverse_failure"))
                }
            }
            else -> {
                Text(s.mainDimensionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${Numbers.format(s.mainDimension)} м",
                    style = NumberStyle.copy(fontSize = MaterialTheme.typography.displaySmall.fontSize),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("inverse_dimension"),
                )
                s.secondDimensionLabel?.let { label ->
                    ValueRow(label, "${Numbers.format(s.secondDimension!!)} м")
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ValueRow(Uk.INVERSE_VOLUME, "${Numbers.format(s.workingVolume)} м³")
                ValueRow(Uk.INVERSE_FILM_AREA, "${Numbers.format(s.filmArea)} м²")
                ValueRow(Uk.INVERSE_FILM_MASS, "${Numbers.format(s.filmMass, 3)} кг")
                ValueRow(Uk.INVERSE_ENVELOPE_MASS, "${Numbers.format(s.envelopeMass, 3)} кг")
                ValueRow(Uk.INVERSE_GROSS, "${Numbers.format(s.grossLift)} кг")
                ValueRow(Uk.INVERSE_BALLAST, "${Numbers.format(s.ballastMass)} кг")
                if (s.durationReserve > 0) ValueRow(Uk.INVERSE_RESERVE, "${Numbers.format(s.durationReserve)} кг")
                ValueRow(Uk.INVERSE_PAYLOAD, "${Numbers.format(s.payload)} кг", emphasize = true)
                Button(
                    onClick = { onCreateProject(s.project) },
                    modifier = Modifier.fillMaxWidth().testTag("inverse_create"),
                ) { Text(Uk.INVERSE_CREATE, modifier = Modifier.padding(vertical = 4.dp)) }
            }
        }
        result.notes.forEach {
            Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (s != null) {
            result.assumptions.forEach {
                Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
