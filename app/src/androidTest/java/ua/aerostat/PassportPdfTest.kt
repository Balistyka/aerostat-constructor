package ua.aerostat

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ua.aerostat.core.Calculator
import ua.aerostat.core.EnvelopeVariant
import ua.aerostat.core.Project
import ua.aerostat.core.ProjectInput
import ua.aerostat.core.Report
import ua.aerostat.core.ShapeType
import ua.aerostat.pdf.PassportPdf
import java.io.File

/** PDF-паспорт генерується на пристрої і є коректним PDF-файлом. */
@RunWith(AndroidJUnit4::class)
class PassportPdfTest {
    @Test
    fun writesValidPdf() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val project = Project(
            "pdf", "Метеорологічна куля D 3,2 м", 0, 0,
            ProjectInput(shape = ShapeType.SPHERE, variant = EnvelopeVariant.OPEN_APPENDIX, diameter = "3,2",
                thicknessUm = "25", notes = "Перевірити товщину плівки мікрометром перед розкроєм."),
        )
        val result = Calculator.calculate(project.input)
        val dir = File(ctx.filesDir, "shots").apply { mkdirs() }
        val file = File(dir, "passport_example.pdf")
        file.outputStream().use { PassportPdf.write(it, project.name, Report.sections(project, result, "20.10.2026 20:30")) }
        val bytes = file.readBytes()
        assertTrue("PDF порожній", bytes.size > 2000)
        assertEquals("%PDF", String(bytes, 0, 4))
    }
}
