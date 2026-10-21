package ua.aerostat.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ua.aerostat.R
import ua.aerostat.ui.Uk

/** Акцентний градієнт застосунку. */
@Composable
fun accentBrush(): Brush = Brush.linearGradient(
    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
)

@Composable
fun numberBrush(): Brush = Brush.linearGradient(
    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)),
)

/** Кольоровий логотип підрозділу. */
@Composable
fun BrandLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.logo_rd412),
        contentDescription = Uk.BRIGADE,
        modifier = modifier,
    )
}

/** Монохромна мітка логотипа — для панелей і водяного знака. */
@Composable
fun BrandMark(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary, alpha: Float = 1f) {
    Image(
        painter = painterResource(R.drawable.logo_mark),
        contentDescription = null,
        colorFilter = ColorFilter.tint(color.copy(alpha = alpha)),
        modifier = modifier,
    )
}

/** Підпис підрозділу у вигляді чипа. */
@Composable
fun BrigadeBadge(modifier: Modifier = Modifier, dense: Boolean = false) {
    Row(
        modifier
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), CircleShape)
            .padding(horizontal = if (dense) 10.dp else 14.dp, vertical = if (dense) 4.dp else 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BrandMark(Modifier.size(if (dense) 14.dp else 18.dp))
        Text(
            Uk.BRIGADE,
            style = if (dense) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * Роздільник у стилі друкованої плати: лінія з вузлами, як на емблемі.
 */
@Composable
fun CircuitDivider(modifier: Modifier = Modifier, nodes: Int = 3) {
    val line = MaterialTheme.colorScheme.outlineVariant
    val node = MaterialTheme.colorScheme.primary
    Canvas(modifier.fillMaxWidth().height(14.dp)) {
        val y = size.height / 2
        drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5f)
        val step = size.width / (nodes + 1)
        for (i in 1..nodes) {
            val x = step * i
            drawCircle(node, radius = 4.5f, center = Offset(x, y))
            drawCircle(node.copy(alpha = 0.25f), radius = 9f, center = Offset(x, y))
            if (i % 2 == 1) {
                drawLine(line, Offset(x, y), Offset(x, y - size.height / 2), strokeWidth = 1.5f)
            }
        }
    }
}

/** Велика плитка з ключовим числом. */
@Composable
fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Boolean = true,
    caption: String? = null,
    valueTag: String? = null,
) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.large)
            .background(
                if (accent) Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ) else Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainerHigh),
                ),
            )
            .border(
                1.dp,
                if (accent) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant,
                MaterialTheme.shapes.large,
            )
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            modifier = if (valueTag != null) Modifier.testTag(valueTag) else Modifier,
            style = TextStyle(
                brush = if (accent) numberBrush() else null,
                fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                fontWeight = MaterialTheme.typography.headlineMedium.fontWeight,
                fontFeatureSettings = "tnum",
            ),
            color = if (accent) Color.Unspecified else MaterialTheme.colorScheme.onSurface,
        )
        caption?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Шапка з логотипом, назвою підрозділу і текстом. */
@Composable
fun HeroHeader(
    title: String,
    subtitle: String,
    titleTag: String? = null,
    logoWidth: Dp = 108.dp,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ),
            )
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f), MaterialTheme.shapes.extraLarge),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            BrandLogo(Modifier.width(logoWidth))
            Column(
                Modifier.padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BrigadeBadge(dense = true)
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = if (titleTag != null) Modifier.testTag(titleTag) else Modifier,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                content()
            }
        }
    }
}
