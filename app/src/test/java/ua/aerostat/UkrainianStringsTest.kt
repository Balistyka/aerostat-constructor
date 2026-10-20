package ua.aerostat

import org.junit.Assert.assertTrue
import org.junit.Test
import ua.aerostat.core.Calculator
import ua.aerostat.core.EnvelopeVariant
import ua.aerostat.core.Field
import ua.aerostat.core.FilmMaterial
import ua.aerostat.core.HelpCategory
import ua.aerostat.core.HelpTopics
import ua.aerostat.core.ModelStatus
import ua.aerostat.core.Project
import ua.aerostat.core.ProjectInput
import ua.aerostat.core.Report
import ua.aerostat.core.ShapeType
import ua.aerostat.core.Validation
import ua.aerostat.ui.Uk
import ua.aerostat.ui.theme.ThemeMode

/** Перевіряє, що всі тексти інтерфейсу українські, без англомовних заглушок. */
class UkrainianStringsTest {
    private val cyrillic = Regex("[А-Яа-яІіЇїЄєҐґ]")
    private val latinWord = Regex("\\b[A-Za-z]{3,}\\b")
    private val allowedLatin = setOf("LDPE", "LLDPE", "HDPE", "PDF", "sin")
    private val forbidden = listOf("todo", "lorem", "placeholder", "fixme", "tbd", "coming soon", "error", "result", "null")
    private val russianOnly = Regex("[ЫыЭэЪъЁё]")

    private fun check(where: String, s: String, requireCyrillic: Boolean = true) {
        assertTrue("$where: порожній рядок", s.isNotBlank())
        if (requireCyrillic) assertTrue("$where: немає кирилиці: «$s»", cyrillic.containsMatchIn(s))
        assertTrue("$where: російські літери: «$s»", !russianOnly.containsMatchIn(s))
        latinWord.findAll(s).forEach { m ->
            assertTrue("$where: англомовне слово «${m.value}» у «$s»", m.value in allowedLatin)
        }
        forbidden.forEach { f -> assertTrue("$where: заглушка «$f»", !s.lowercase().contains(f)) }
    }

    @Test
    fun uiStringsObject() {
        Uk::class.java.declaredFields
            .filter { it.type == String::class.java && java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .also { assertTrue("Немає рядків", it.size > 50) }
            .forEach { f -> f.isAccessible = true; check("Uk.${f.name}", f.get(null) as String) }
    }

    @Test
    fun enums() {
        ShapeType.entries.forEach { check(it.name, it.title); check(it.name, it.description) }
        ModelStatus.entries.forEach { check(it.name, it.title); check(it.name, it.explanation) }
        EnvelopeVariant.entries.forEach { check(it.name, it.title); check(it.name, it.description) }
        FilmMaterial.entries.forEach { check(it.name, it.title) }
        Field.entries.forEach { check(it.name, it.label); check(it.name, it.unit, requireCyrillic = false) }
        HelpCategory.entries.forEach { check(it.name, it.title) }
        ThemeMode.entries.forEach { check(it.name, it.title) }
    }

    @Test
    fun helpTopics() {
        HelpTopics.all.forEach { t ->
            check("${t.id}.title", t.title)
            check("${t.id}.definition", t.definition)
            t.assumptions?.let { check("${t.id}.assumptions", it) }
            t.example?.let { check("${t.id}.example", it) }
        }
        Field.entries.forEach { HelpTopics.forField(it) }
    }

    @Test
    fun validationMessages() {
        listOf("", "abc", "-1", "0", "1,5", "1e9", "99999999").forEach { raw ->
            Field.entries.forEach { f -> Validation.check(f, raw).error?.let { check("${f.name}[$raw]", it) } }
        }
    }

    @Test
    fun reportText() {
        ShapeType.entries.forEach { shape ->
            EnvelopeVariant.entries.forEach { v ->
                val p = Project("1", "Перевірка", 0, 0, ProjectInput(shape = shape, variant = v))
                val r = Calculator.calculate(p.input)
                Report.sections(p, r, "01.01.2026").forEach { s ->
                    check("section", s.title)
                    s.rows.forEach { (k, _) -> check("row", k) }
                    s.paragraphs.forEach { check("para", it) }
                }
                r.assumptions.forEach { check("assumption", it) }
            }
        }
    }
}
