@file:OptIn(ExperimentalMaterial3Api::class)

package ua.aerostat.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import ua.aerostat.core.EnvelopeVariant
import ua.aerostat.core.ShapeType
import ua.aerostat.ui.Uk
import ua.aerostat.ui.components.AeroTopBar
import ua.aerostat.ui.components.StatusBadge

@Composable
fun WizardScreen(
    defaultName: String,
    onCreate: (String, ShapeType, EnvelopeVariant) -> Unit,
    onBack: () -> Unit,
) {
    var shape by rememberSaveable { mutableStateOf(ShapeType.GATHERED) }
    var variant by rememberSaveable { mutableStateOf(EnvelopeVariant.CLOSED) }
    var name by rememberSaveable { mutableStateOf(defaultName) }

    CenteredColumn {
        Column(Modifier.fillMaxSize()) {
            AeroTopBar(Uk.WIZARD_TITLE, onBack = onBack)
            Column(
                Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(Uk.WIZARD_STEP_SHAPE, style = MaterialTheme.typography.titleLarge)
                ShapeType.entries.forEach { s ->
                    ChoiceCard(
                        title = s.title,
                        text = s.description,
                        selected = shape == s,
                        onClick = { shape = s },
                        tag = "shape_${s.name}",
                    ) { StatusBadge(s.status) }
                }
                Text(Uk.WIZARD_STEP_VARIANT, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
                EnvelopeVariant.entries.forEach { v ->
                    ChoiceCard(v.title, v.description, variant == v, { variant = v }, "variant_${v.name}")
                }
                Text(Uk.WIZARD_STEP_NAME, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(80) },
                    label = { Text(Uk.PROJECT_NAME) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("wizard_name"),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    shape = MaterialTheme.shapes.medium,
                )
                Button(
                    onClick = { onCreate(name, shape, variant) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("wizard_create"),
                ) { Text(Uk.WIZARD_CREATE, modifier = Modifier.padding(vertical = 6.dp)) }
            }
        }
    }
}

@Composable
fun ChoiceCard(
    title: String,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
    extra: (@Composable () -> Unit)? = null,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(top = 2.dp, end = 12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                extra?.invoke()
            }
        }
    }
}
