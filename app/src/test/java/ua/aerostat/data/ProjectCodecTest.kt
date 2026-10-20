package ua.aerostat.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.aerostat.core.EnvelopeVariant
import ua.aerostat.core.FilmMaterial
import ua.aerostat.core.Project
import ua.aerostat.core.ProjectInput
import ua.aerostat.core.ShapeType

/** Збереження та відновлення проєктів (серіалізація сховища). */
class ProjectCodecTest {
    private val projects = listOf(
        Project(
            id = "a", name = "Метеозонд «Ранок»", createdAt = 1, updatedAt = 2,
            input = ProjectInput(
                shape = ShapeType.CYLINDER, variant = EnvelopeVariant.OPEN_APPENDIX,
                diameter = "2,5", cylinderLength = "7,", material = FilmMaterial.CUSTOM, density = "931,5",
                thicknessUm = "", notes = "Шов 2 см\nперевірити плівку",
            ),
        ),
        Project(id = "b", name = "Куля", createdAt = 3, updatedAt = 4),
    )

    @Test
    fun roundTripKeepsEverythingExactly() {
        val decoded = ProjectCodec.decode(ProjectCodec.encode(projects))
        assertEquals(projects, decoded)
    }

    @Test
    fun keepsRawInputWithComma() {
        val d = ProjectCodec.decode(ProjectCodec.encode(projects)).first()
        assertEquals("7,", d.input.cylinderLength)
        assertEquals("", d.input.thicknessUm)
    }

    @Test
    fun emptyOrCorruptDataGivesEmptyList() {
        assertTrue(ProjectCodec.decode(null).isEmpty())
        assertTrue(ProjectCodec.decode("").isEmpty())
        assertTrue(ProjectCodec.decode("{не json").isEmpty())
    }

    @Test
    fun unknownFieldsAndMissingFieldsTolerated() {
        val json = """{"version":1,"projects":[{"id":"x","name":"Стара","createdAt":0,"updatedAt":0,"future":true,
            "input":{"shape":"SLEEVE","diameter":"4"}}]}"""
        val p = ProjectCodec.decode(json).single()
        assertEquals(ShapeType.SLEEVE, p.input.shape)
        assertEquals("4", p.input.diameter)
        assertEquals(ProjectInput().thicknessUm, p.input.thicknessUm)
    }

    @Test
    fun unknownEnumValueFallsBackToDefault() {
        val json = """{"projects":[{"id":"x","name":"Н","createdAt":0,"updatedAt":0,"input":{"shape":"TORUS"}}]}"""
        val p = ProjectCodec.decode(json).single()
        assertEquals(ShapeType.SPHERE, p.input.shape)
    }
}
