@file:OptIn(ExperimentalMaterial3Api::class)

package ua.aerostat.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.aerostat.core.CalcResult
import ua.aerostat.core.Calculator
import ua.aerostat.core.HelpTopic
import ua.aerostat.core.HelpTopics
import ua.aerostat.core.Numbers
import ua.aerostat.core.Project
import ua.aerostat.core.Report
import ua.aerostat.pdf.PassportPdf
import ua.aerostat.ui.Uk
import ua.aerostat.ui.components.AeroTopBar
import ua.aerostat.ui.components.HelpDialog
import ua.aerostat.ui.components.SectionCard
import ua.aerostat.ui.components.StatusBadge
import ua.aerostat.ui.components.ValueRow
import ua.aerostat.ui.theme.NumberStyle

@Composable
fun ResultsScreen(project: Project?, onBack: () -> Unit) {
    if (project == null) {
        Column { AeroTopBar(Uk.RESULTS_TITLE, onBack); Text(Uk.NOT_FOUND, Modifier.padding(20.dp)) }
        return
    }
    val result = remember(project.input) { Calculator.calculate(project.input) }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var help by remember { mutableStateOf<HelpTopic?>(null) }

    fun copy(label: String, value: String) {
        clipboard.setText(AnnotatedString("$label: $value"))
        scope.launch { snackbar.showSnackbar("${Uk.COPIED}: $label") }
    }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val sections = Report.sections(project, result, formatDate(System.currentTimeMillis()))
                    PassportPdf.write(context, uri, project.name, sections)
                }.isSuccess
            }
            snackbar.showSnackbar(if (ok) Uk.PDF_SAVED else Uk.PDF_FAILED)
        }
    }

    CenteredColumn {
        Column(Modifier.fillMaxSize()) {
            AeroTopBar(Uk.RESULTS_TITLE, onBack)
            BoxWithConstraints(Modifier.weight(1f)) {
                val wide = maxWidth >= 720.dp
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(project.name, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "${project.input.shape.title} · ${project.input.variant.title}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    StatusBadge(result.status)

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                val safe = project.name.replace(Regex("[^\\p{L}\\p{N}_-]+"), "_").trim('_').ifEmpty { "проєкт" }
                                pdfLauncher.launch("${Uk.PDF_FILE_PREFIX}_$safe.pdf")
                            },
                            modifier = Modifier.weight(1f).testTag("export_pdf"),
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null)
                            Spacer(Modifier.padding(start = 6.dp)); Text(Uk.EXPORT_PDF)
                        }
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(Report.plainText(project, result, formatDate(System.currentTimeMillis()))))
                                scope.launch { snackbar.showSnackbar(Uk.COPIED) }
                            },
                            modifier = Modifier.weight(1f).testTag("copy_all"),
                        ) { Text(Uk.COPY_ALL) }
                    }

                    if (result.unavailableReason != null) {
                        SectionCard(title = Uk.RESULT_GEOMETRY, onHelp = { help = HelpTopics.general.first { it.id == "pear" } }) {
                            Text(result.unavailableReason, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("unavailable"))
                        }
                    } else if (wide) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                MassCard(result, ::copy) { help = it }
                                LiftCard(result, ::copy) { help = it }
                                LayoutCard(result, ::copy)
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                GeometryCard(result, ::copy)
                                GasStateCard(result, ::copy)
                                GoresCard(result)
                            }
                        }
                    } else {
                        MassCard(result, ::copy) { help = it }
                        LiftCard(result, ::copy) { help = it }
                        GeometryCard(result, ::copy)
                        GasStateCard(result, ::copy)
                        LayoutCard(result, ::copy)
                        GoresCard(result)
                    }

                    if (result.errors.isNotEmpty()) {
                        SectionCard(title = Uk.FIX_ERRORS) {
                            result.errors.forEach { (f, e) -> ValueRow(f.label, e) }
                        }
                    }
                    SectionCard(title = Uk.RESULT_ASSUMPTIONS) {
                        result.assumptions.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                    }
                    SectionCard(title = Uk.RESULT_DISCLAIMER) {
                        Text(result.status.explanation, style = MaterialTheme.typography.bodyMedium)
                        Text(Report.DISCLAIMER, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(Uk.TAP_TO_COPY, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.CenterHorizontally))
        }
    }
    help?.let { HelpDialog(it) { help = null } }
}

@Composable
private fun MassCard(result: CalcResult, copy: (String, String) -> Unit, onHelp: (HelpTopic) -> Unit) {
    SectionCard(title = Uk.RESULT_MASS, onHelp = { onHelp(HelpTopics.filmMass) }, modifier = Modifier.testTag("mass_card")) {
        val film = result.filmMass
        if (film == null) {
            Text(Uk.RESULT_NOT_COMPUTED + ". " + Uk.FIX_ERRORS, color = MaterialTheme.colorScheme.error)
        } else {
            val filmText = "${Numbers.format(film, 3)} кг"
            Text(Uk.RESULT_FILM_MASS, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                filmText,
                style = NumberStyle.copy(fontSize = MaterialTheme.typography.displaySmall.fontSize),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("film_mass").clickable { copy(Uk.RESULT_FILM_MASS, filmText) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            result.blankArea?.let { val v = "${Numbers.format(it)} м²"; ValueRow(Uk.RESULT_BLANK_AREA, v, { copy(Uk.RESULT_BLANK_AREA, v) }) }
            result.density?.let { val v = "${Numbers.format(it, 0)} кг/м³"; ValueRow("Густина", v, { copy("Густина", v) }) }
            result.fittingsMass?.let { val v = "${Numbers.format(it, 3)} кг"; ValueRow(Uk.RESULT_FITTINGS, v, { copy(Uk.RESULT_FITTINGS, v) }) }
            result.totalMass?.let { val v = "${Numbers.format(it, 3)} кг"; ValueRow(Uk.RESULT_TOTAL, v, { copy(Uk.RESULT_TOTAL, v) }, emphasize = true) }
            Text(
                "Площа заготовки ≠ площа поверхні наповненої оболонки.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LiftCard(result: CalcResult, copy: (String, String) -> Unit, onHelp: (HelpTopic) -> Unit) {
    val l = result.lift ?: return
    SectionCard(
        title = Uk.RESULT_LIFT,
        onHelp = { onHelp(HelpTopics.general.first { it.id == "payload" }) },
        modifier = Modifier.testTag("lift_card"),
    ) {
        val payload = l.payload
        if (payload != null) {
            Text(Uk.LIFT_PAYLOAD, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${Numbers.format(payload)} кг",
                style = NumberStyle.copy(fontSize = MaterialTheme.typography.displaySmall.fontSize),
                color = if (payload > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("payload"),
            )
            if (payload <= 0) {
                Text(Uk.LIFT_NEGATIVE, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        val rows = buildList {
            add(Uk.LIFT_WORKING_VOLUME to "${Numbers.format(l.envelopeVolume)} м³")
            add(Uk.LIFT_TARGET to "${Numbers.format(l.grossLiftTarget)} кг")
            add(Uk.LIFT_GROUND to "${Numbers.format(l.grossLiftGround)} кг")
            add(Uk.LIFT_FILL_GROUND to "${Numbers.format(l.fillVolumeGround)} м³")
            l.envelopeMass?.let { add(Uk.LIFT_ENVELOPE_MASS to "${Numbers.format(it, 3)} кг") }
            l.ballastMass?.let { add(Uk.LIFT_BALLAST to "${Numbers.format(it)} кг") }
            l.cylinderOutput?.let { add(Uk.LIFT_CYL_OUTPUT to "${Numbers.format(it)} м³") }
            l.cylinderCount?.let { add(Uk.LIFT_CYL_COUNT to Numbers.format(it)) }
        }
        rows.forEach { (k, v) -> ValueRow(k, v, { copy(k, v) }) }
    }
}

@Composable
private fun GasStateCard(result: CalcResult, copy: (String, String) -> Unit) {
    val l = result.lift ?: return
    SectionCard(title = "${Uk.RESULT_GAS_STATE} · ${l.gas.title}") {
        val rows = listOf(
            Uk.LIFT_AIR_GROUND to "${Numbers.format(l.ground.density, 3)} кг/м³",
            Uk.LIFT_GAS_GROUND to "${Numbers.format(l.gasDensityGround, 4)} кг/м³",
            Uk.LIFT_TEMP_TARGET to "${Numbers.format(l.target.temperatureK - 273.15, 1)} °C",
            Uk.LIFT_PRESSURE_TARGET to "${Numbers.format(l.target.pressurePa / 100.0, 0)} гПа",
            Uk.LIFT_AIR_TARGET to "${Numbers.format(l.target.density, 3)} кг/м³",
            Uk.LIFT_GAS_TARGET to "${Numbers.format(l.gasDensityTarget, 4)} кг/м³",
        )
        rows.forEach { (k, v) -> ValueRow(k, v, { copy(k, v) }) }
    }
}

@Composable
private fun GeometryCard(result: CalcResult, copy: (String, String) -> Unit) {
    if (result.geometry.isEmpty()) return
    SectionCard(title = Uk.RESULT_GEOMETRY) {
        result.geometry.forEach { r -> ValueRow(r.label, r.formatted, { copy(r.label, r.formatted) }) }
    }
}

@Composable
private fun LayoutCard(result: CalcResult, copy: (String, String) -> Unit) {
    if (result.layout.isEmpty()) return
    SectionCard(title = Uk.RESULT_LAYOUT) {
        result.layout.forEach { r -> ValueRow(r.label, r.formatted, { copy(r.label, r.formatted) }) }
    }
}

@Composable
private fun GoresCard(result: CalcResult) {
    val g = result.gores ?: return
    SectionCard(title = "${Uk.RESULT_GORES} (N = ${g.count})") {
        Text(Uk.RESULT_GORES_NOTE, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth()) {
            Text(Uk.RESULT_DISTANCE, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            Text(Uk.RESULT_WIDTH, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.End)
        }
        g.stations.forEach { s ->
            Row(Modifier.fillMaxWidth()) {
                Text(Numbers.format(s.distance, 3), Modifier.weight(1f), style = NumberStyle.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize))
                Text(
                    Numbers.format(s.width, 3), Modifier.weight(1f),
                    style = NumberStyle.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize), textAlign = TextAlign.End,
                )
            }
        }
    }
}
