package ua.aerostat

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Знімки екранів для візуальної перевірки (вузький і розгорнутий екран). */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val prefix = InstrumentationRegistry.getArguments().getString("shotPrefix") ?: "phone"
    private val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "shots").apply { mkdirs() }

    private fun shot(name: String, node: SemanticsNodeInteraction = rule.onRoot()) {
        rule.waitForIdle()
        Thread.sleep(400)
        val bmp = node.captureToImage().asAndroidBitmap()
        File(dir, "${prefix}_$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun waitTag(tag: String) = rule.waitUntilAtLeastOneExists(hasTestTag(tag), 10_000)

    @Test
    fun captureMainScreens() {
        waitTag("home_title")
        shot("01_home")
        rule.onNodeWithTag("new_project").performScrollTo().performClick()
        waitTag("wizard_create")
        shot("02_wizard")
        rule.onNodeWithTag("wizard_name").performScrollTo().performTextReplacement("Конверт 6 × 10 м")
        rule.onNodeWithTag("wizard_create").performScrollTo().performClick()
        waitTag("field_ENVELOPE_WIDTH")
        rule.onNodeWithTag("field_ENVELOPE_WIDTH").performScrollTo().performTextReplacement("6")
        rule.onNodeWithTag("field_ENVELOPE_HEIGHT").performScrollTo().performTextReplacement("10")
        rule.onNodeWithTag("field_GATHER_PART").performScrollTo().performTextReplacement("1,1")
        rule.onNodeWithTag("project_name").performScrollTo()
        shot("03_editor_top")
        rule.onNodeWithTag("field_THICKNESS").performScrollTo().performTextReplacement("-5")
        shot("04_editor_error")
        rule.onNodeWithTag("field_THICKNESS").performTextReplacement("25")
        rule.onNodeWithTag("open_results").performScrollTo()
        shot("05_editor_bottom")
        rule.onNodeWithTag("open_results").performClick()
        waitTag("mass_card")
        shot("06_results")
        rule.onNodeWithTag("back").performClick()
        rule.onNodeWithTag("back").performClick()
        waitTag("nav_reference")
        rule.onNodeWithTag("nav_reference").performClick()
        waitTag("topic_film_mass")
        shot("07_reference")
        rule.onNodeWithTag("topic_film_mass").performClick()
        waitTag("help_dialog")
        shot("08_help", rule.onNodeWithTag("help_dialog"))
        rule.onNodeWithTag("help_close").performClick()
        rule.onNodeWithTag("nav_projects").performClick()
        waitTag("projects_list")
        shot("09_projects")
        rule.onNodeWithTag("nav_settings").performClick()
        waitTag("theme_LIGHT")
        rule.onNodeWithTag("theme_LIGHT").performClick()
        shot("10_settings_light")
        rule.onNodeWithTag("nav_home").performClick()
        waitTag("home_title")
        shot("11_home_light")
        rule.onNodeWithTag("nav_settings").performClick()
        waitTag("theme_DARK")
        rule.onNodeWithTag("theme_DARK").performClick()
    }
}
