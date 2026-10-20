package ua.aerostat

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ua.aerostat.data.ProjectStore

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class MainFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun waitTag(tag: String) = rule.waitUntilAtLeastOneExists(hasTestTag(tag), 20_000)

    private fun createProject(shapeTag: String, name: String) {
        waitTag("new_project")
        rule.onNodeWithTag("nav_home").performClick()
        rule.onNodeWithTag("new_project").performScrollTo().performClick()
        waitTag("wizard_create")
        rule.onNodeWithTag(shapeTag).performScrollTo().performClick()
        rule.onNodeWithTag("wizard_name").performScrollTo().performTextReplacement(name)
        // Ховаємо клавіатуру, щоб вона не перекривала кнопку створення.
        Espresso.closeSoftKeyboard()
        rule.waitForIdle()
        rule.onNodeWithTag("wizard_create").performClick()
        waitTag("project_name")
    }

    @Test
    fun homeIsInUkrainian() {
        waitTag("home_title")
        rule.onNodeWithTag("home_title").assertTextEquals("Конструктор оболонок")
        rule.onNodeWithText("Новий проєкт").assertIsDisplayed()
        rule.onNodeWithText("Довідник").assertIsDisplayed()
    }

    @Test
    fun sphereProjectComputesFilmMass() {
        createProject("shape_SPHERE", "Куля тест")
        rule.onNodeWithTag("field_DIAMETER").performScrollTo().performTextReplacement("4")
        rule.onNodeWithTag("field_THICKNESS").performScrollTo().performTextReplacement("30")
        rule.onNodeWithTag("field_WASTE").performScrollTo().performTextReplacement("10")
        rule.onNodeWithTag("open_results").performScrollTo().performClick()
        waitTag("film_mass")
        // π × 4² × 1,1 × 30·10⁻⁶ × 920 = 1,526 кг
        rule.onNodeWithTag("film_mass").assertTextEquals("1,526 кг")
    }

    @Test
    fun decimalCommaAccepted() {
        createProject("shape_SPHERE", "Кома тест")
        rule.onNodeWithTag("field_DIAMETER").performScrollTo().performTextReplacement("2,5")
        rule.onNodeWithTag("open_results").performScrollTo().performClick()
        waitTag("film_mass")
        // π × 6,25 × 1,1 × 30·10⁻⁶ × 920 = 0,596 кг
        rule.onNodeWithTag("film_mass").assertTextEquals("0,596 кг")
    }

    @Test
    fun negativeValueShowsError() {
        createProject("shape_CYLINDER", "Помилка тест")
        rule.onNodeWithTag("field_THICKNESS").performScrollTo().performTextReplacement("-5")
        rule.waitUntil(10_000) {
            rule.onAllNodesWithTag("field_THICKNESS_error", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("field_THICKNESS_error", useUnmergedTree = true)
            .assertTextContains("від'ємним", substring = true)
    }

    @Test
    fun gatheredEnvelopeComputesFilmMass() {
        createProject("shape_GATHERED", "Конверт тест")
        rule.onNodeWithTag("field_ENVELOPE_WIDTH").performScrollTo().performTextReplacement("6")
        rule.onNodeWithTag("field_ENVELOPE_HEIGHT").performScrollTo().performTextReplacement("10")
        rule.onNodeWithTag("field_GATHER_PART").performScrollTo().performTextReplacement("1,1")
        rule.onNodeWithTag("field_THICKNESS").performScrollTo().performTextReplacement("30")
        rule.onNodeWithTag("field_WASTE").performScrollTo().performTextReplacement("10")
        rule.onNodeWithTag("open_results").performScrollTo().performClick()
        waitTag("film_mass")
        // 2 × 6 × 10 × 1,1 × 30·10⁻⁶ × 920 = 3,643 кг
        rule.onNodeWithTag("film_mass").assertTextEquals("3,643 кг")
    }

    @Test
    fun liftAndPayloadAreComputed() {
        createProject("shape_GATHERED", "Підйом тест")
        rule.onNodeWithTag("field_ENVELOPE_WIDTH").performScrollTo().performTextReplacement("6")
        rule.onNodeWithTag("field_ENVELOPE_HEIGHT").performScrollTo().performTextReplacement("10")
        rule.onNodeWithTag("field_GATHER_PART").performScrollTo().performTextReplacement("1,1")
        rule.onNodeWithTag("field_FILL_FACTOR").performScrollTo().performTextReplacement("72")
        rule.onNodeWithTag("field_TARGET_ALTITUDE").performScrollTo().performTextReplacement("10")
        rule.onNodeWithTag("open_results").performScrollTo().performClick()
        waitTag("lift_card")
        rule.onNodeWithTag("payload").assertTextContains("кг", substring = true)
        rule.onNodeWithText("Потрібно балонів").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun inversePicksEnvelopeAndCreatesProject() {
        waitTag("open_inverse")
        rule.onNodeWithTag("open_inverse").performScrollTo().performClick()
        waitTag("inverse_result")
        rule.onNodeWithTag("inv_TARGET_PAYLOAD").performScrollTo().performTextReplacement("20")
        rule.onNodeWithTag("inv_TARGET_ALTITUDE").performScrollTo().performTextReplacement("10")
        Espresso.closeSoftKeyboard()
        rule.waitForIdle()
        rule.onNodeWithTag("inverse_dimension").performScrollTo().assertTextContains("м", substring = true)
        rule.onNodeWithTag("inverse_create").performScrollTo().performClick()
        // Відкривається звичайний проєкт із підібраними розмірами
        waitTag("project_name")
        rule.onNodeWithTag("field_ENVELOPE_HEIGHT").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun inverseReportsImpossibleNarrowSleeve() {
        waitTag("open_inverse")
        rule.onNodeWithTag("open_inverse").performScrollTo().performClick()
        waitTag("inverse_result")
        rule.onNodeWithTag("inv_ENVELOPE_WIDTH").performScrollTo().performTextReplacement("0,5")
        Espresso.closeSoftKeyboard()
        rule.waitForIdle()
        rule.onNodeWithTag("inverse_failure").performScrollTo()
            .assertTextContains("Потрібна ширина понад", substring = true)
    }

    @Test
    fun pearShowsUnavailable() {
        createProject("shape_PEAR", "Груша тест")
        waitTag("unavailable")
        rule.onNodeWithTag("unavailable").assertTextContains("Розрахунок поки недоступний", substring = true)
    }

    @Test
    fun helpOpensAndCloses() {
        waitTag("nav_reference")
        rule.onNodeWithTag("nav_reference").performClick()
        waitTag("topic_film_mass")
        rule.onNodeWithTag("topic_film_mass").performClick()
        waitTag("help_dialog")
        rule.onNodeWithText("ФОРМУЛА").assertIsDisplayed()
        rule.onNodeWithTag("help_close").performClick()
        rule.waitUntilDoesNotExist(hasTestTag("help_dialog"), 5_000)
    }

    @Test
    fun fieldHelpOpensFromEditor() {
        createProject("shape_SLEEVE", "Рукав тест")
        rule.onNodeWithContentDescription("Довідка: Товщина плівки").performScrollTo().performClick()
        waitTag("help_dialog")
        rule.onNodeWithText("1 мкм = 0,000001 м = 10⁻⁶ м.").assertIsDisplayed()
        rule.onNodeWithTag("help_close").performClick()
        rule.waitUntilDoesNotExist(hasTestTag("help_dialog"), 5_000)
    }

    @Test
    fun projectIsPersistedToStorage() {
        val name = "Збереження ${System.currentTimeMillis()}"
        createProject("shape_CYLINDER", name)
        rule.onNodeWithTag("field_DIAMETER").performScrollTo().performTextReplacement("1,75")
        rule.waitForIdle()
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        var ok = false
        val deadline = System.currentTimeMillis() + 10_000
        while (!ok && System.currentTimeMillis() < deadline) {
            val saved = runBlocking { ProjectStore(ctx).projects.first() }
            ok = saved.any { it.name == name && it.input.diameter == "1,75" }
            if (!ok) Thread.sleep(200)
        }
        assertTrue("Проєкт не збережено у сховищі", ok)
        // Після перестворення активності проєкт видно у списку.
        rule.activityRule.scenario.recreate()
        waitTag("project_name")
        rule.onNodeWithTag("back").performClick()
        waitTag("nav_projects")
        rule.onNodeWithTag("nav_projects").performClick()
        rule.waitUntilAtLeastOneExists(androidx.compose.ui.test.hasText(name), 10_000)
    }
}
