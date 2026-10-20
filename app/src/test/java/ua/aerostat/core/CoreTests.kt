package ua.aerostat.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class FilmMassTest {
    @Test
    fun massIsAreaTimesThicknessInMetersTimesDensity() {
        // 10 м² × 50 мкм × 920 кг/м³ = 10 × 0,00005 × 920 = 0,46 кг
        assertEquals(0.46, FilmMass.mass(10.0, 50.0, 920.0), 1e-12)
    }

    @Test
    fun micrometersAreConvertedToMeters() {
        // 1 м² × 1 000 000 мкм (=1 м) × 1 кг/м³ = 1 кг
        assertEquals(1.0, FilmMass.mass(1.0, 1_000_000.0, 1.0), 1e-12)
    }

    @Test
    fun zeroAreaGivesZeroMass() {
        assertEquals(0.0, FilmMass.mass(0.0, 30.0, 920.0), 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeAreaIsRejected() {
        FilmMass.mass(-1.0, 30.0, 920.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeThicknessIsRejected() {
        FilmMass.mass(1.0, -30.0, 920.0)
    }

    @Test
    fun largeNumbersStayFinite() {
        val m = FilmMass.mass(1e9, 1000.0, 3000.0)
        assertEquals(3e9, m, 1.0)
    }
}

class NumbersTest {
    private fun value(s: String) = (Numbers.parse(s) as ParsedNumber.Value).value

    @Test fun decimalComma() = assertEquals(1.5, value("1,5"), 0.0)
    @Test fun decimalDot() = assertEquals(1.5, value("1.5"), 0.0)
    @Test fun spacesAsThousands() = assertEquals(12345.5, value("12 345,5"), 0.0)
    @Test fun nonBreakingSpaces() = assertEquals(12345.0, value("12 345"), 0.0)
    @Test fun leadingComma() = assertEquals(0.5, value(",5"), 0.0)
    @Test fun trailingComma() = assertEquals(6.0, value("6,"), 0.0)
    @Test fun negativeParsed() = assertEquals(-2.0, value("-2"), 0.0)
    @Test fun unicodeMinus() = assertEquals(-2.0, value("−2"), 0.0)
    @Test fun empty() = assertEquals(ParsedNumber.Empty, Numbers.parse("  "))
    @Test fun twoSeparatorsInvalid() = assertEquals(ParsedNumber.Invalid, Numbers.parse("1,2,3"))
    @Test fun lettersInvalid() = assertEquals(ParsedNumber.Invalid, Numbers.parse("abc"))
    @Test fun exponentInvalid() = assertEquals(ParsedNumber.Invalid, Numbers.parse("1e5"))
    @Test fun onlyCommaInvalid() = assertEquals(ParsedNumber.Invalid, Numbers.parse(","))
    @Test fun onlyMinusInvalid() = assertEquals(ParsedNumber.Invalid, Numbers.parse("-"))
    @Test fun hugeNumberParsedFinite() = assertTrue(value("9".repeat(30)) > 1e29)

    @Test fun formatUsesComma() = assertEquals("1,50", Numbers.format(1.5))
    @Test fun formatGroupsThousands() = assertEquals("12 345,68", Numbers.format(12345.678))
    @Test fun formatZeroDecimals() = assertEquals("7", Numbers.format(7.0, 0))
    @Test fun formatNegative() = assertEquals("−3,10", Numbers.format(-3.1))
    @Test fun formatRoundsHalfUp() = assertEquals("0,13", Numbers.format(0.125))
    @Test fun formatNaN() = assertEquals("—", Numbers.format(Double.NaN))
    @Test fun toInputTrimsZeros() = assertEquals("1,5", Numbers.toInput(1.5))
    @Test fun toInputInteger() = assertEquals("920", Numbers.toInput(920.0))
}

class ValidationTest {
    @Test fun okValue() {
        val c = Validation.check(Field.DIAMETER, "3,5")
        assertTrue(c.isOk); assertEquals(3.5, c.value!!, 0.0)
    }
    @Test fun emptyRequired() = assertEquals("Введіть значення", Validation.check(Field.DIAMETER, "").error)
    @Test fun emptyOptionalIsOk() {
        val c = Validation.check(Field.GORE_COUNT, "")
        assertTrue(c.isOk); assertNull(c.value)
    }
    @Test fun negative() = assertEquals("Значення не може бути від'ємним", Validation.check(Field.THICKNESS, "-5").error)
    @Test fun zeroNotAllowed() = assertEquals("Значення має бути більше нуля", Validation.check(Field.DIAMETER, "0").error)
    @Test fun zeroAllowedForWaste() = assertTrue(Validation.check(Field.WASTE, "0").isOk)
    @Test fun zeroAllowedForFittings() = assertTrue(Validation.check(Field.FITTINGS, "0,0").isOk)
    @Test fun integerRequired() = assertEquals("Потрібне ціле число", Validation.check(Field.LAYERS, "1,5").error)
    @Test fun tooLarge() = assertTrue(Validation.check(Field.DIAMETER, "999999999999").error!!.startsWith("Занадто велике"))
    @Test fun tooSmall() = assertTrue(Validation.check(Field.THICKNESS, "0,5").error!!.startsWith("Занадто мале"))
    @Test fun invalidText() = assertTrue(Validation.check(Field.DENSITY, "92o").error!!.startsWith("Некоректне число"))
    @Test fun everyFieldHasRule() = Field.entries.forEach { assertNotNull(Validation.rules[it]) }
}

class GeometryTest {
    @Test fun sphere() {
        assertEquals(PI, Geometry.sphereSurface(1.0), 1e-12)
        assertEquals(PI / 6, Geometry.sphereVolume(1.0), 1e-12)
        assertEquals(28.274, Geometry.sphereSurface(3.0), 1e-3)
        assertEquals(14.137, Geometry.sphereVolume(3.0), 1e-3)
    }

    @Test fun cylinder() {
        assertEquals(37.699, Geometry.cylinderLateral(2.0, 6.0), 1e-3)
        assertEquals(PI, Geometry.discArea(2.0), 1e-12)
        assertEquals(18.850, Geometry.cylinderVolume(2.0, 6.0), 1e-3)
    }

    @Test fun sleeve() {
        assertEquals(24.0, Geometry.sleeveWallArea(1.5, 8.0), 1e-12)
        assertEquals(3.0 / PI, Geometry.sleeveEquivalentDiameter(1.5), 1e-12)
        // Верхня оцінка = об'єм циліндра з периметром 2W
        val d = Geometry.sleeveEquivalentDiameter(1.5)
        assertEquals(Geometry.cylinderVolume(d, 8.0), Geometry.sleeveVolumeUpperBound(1.5, 8.0), 1e-9)
    }

    @Test fun minGores() {
        assertEquals(7, Geometry.minGoreCount(3.0, 1.46))
        // Рівно вміщується: πD/B = 4 → 4 клини
        assertEquals(4, Geometry.minGoreCount(4.0 / PI, 1.0))
    }

    @Test fun goreTableAreaMatchesSphere() {
        val d = 3.0
        val n = 12
        val t = Geometry.goreTable(d, n, stations = 2000)
        // Інтеграл ширини клина методом трапецій × N ≈ πD²
        var area = 0.0
        for (i in 1 until t.stations.size) {
            val a = t.stations[i - 1]; val b = t.stations[i]
            area += (a.width + b.width) / 2 * (b.distance - a.distance)
        }
        assertEquals(Geometry.sphereSurface(d), area * n, 1e-4)
        assertEquals(0.0, t.stations.first().width, 1e-12)
        assertEquals(0.0, t.stations.last().width, 1e-9)
        assertEquals(PI * d / n, t.maxWidth, 1e-12)
        assertEquals(PI * d / 2, t.length, 1e-12)
    }
}

class CalculatorTest {
    private val sphere = ProjectInput(
        shape = ShapeType.SPHERE, diameter = "3", thicknessUm = "30", density = "920",
        layers = "1", wastePercent = "10", seamCm = "2", rollWidth = "1,5", fittingsKg = "0,2",
    )

    @Test fun sphereMass() {
        val r = Calculator.calculate(sphere)
        assertFalse(r.hasErrors)
        val blank = PI * 9 * 1.1
        assertEquals(blank, r.blankArea!!, 1e-9)
        assertEquals(blank * 30e-6 * 920, r.filmMass!!, 1e-9)
        assertEquals(r.filmMass!! + 0.2, r.totalMass!!, 1e-12)
        assertEquals(7, r.gores!!.count)
        assertEquals(ModelStatus.VALIDATED, r.status)
    }

    @Test fun layersMultiplyArea() {
        val one = Calculator.calculate(sphere).blankArea!!
        val two = Calculator.calculate(sphere.copy(layers = "2")).blankArea!!
        assertEquals(one * 2, two, 1e-9)
    }

    @Test fun userGoreCountTooSmall() {
        val r = Calculator.calculate(sphere.copy(goreCount = "4"))
        assertTrue(r.errors[Field.GORE_COUNT]!!.contains("щонайменше 7"))
        assertNull(r.gores)
    }

    @Test fun userGoreCountAccepted() {
        val r = Calculator.calculate(sphere.copy(goreCount = "16"))
        assertEquals(16, r.gores!!.count)
    }

    @Test fun rollNarrowerThanSeams() {
        val r = Calculator.calculate(sphere.copy(rollWidth = "0,1", seamCm = "6"))
        assertNotNull(r.errors[Field.ROLL_WIDTH])
    }

    @Test fun invalidInputGivesNoMass() {
        val r = Calculator.calculate(sphere.copy(thicknessUm = "-30"))
        assertTrue(r.hasErrors)
        assertNull(r.filmMass)
    }

    @Test fun cylinderArea() {
        val r = Calculator.calculate(
            sphere.copy(shape = ShapeType.CYLINDER, diameter = "2", cylinderLength = "6", wastePercent = "0"),
        )
        assertEquals(PI * 2 * 6 + 2 * PI, r.blankArea!!, 1e-9)
    }

    @Test fun sleeveAreaIsTwoWalls() {
        val r = Calculator.calculate(
            sphere.copy(shape = ShapeType.SLEEVE, sleeveFlatWidth = "1,5", sleeveLength = "8", wastePercent = "0"),
        )
        assertEquals(24.0, r.blankArea!!, 1e-9)
        assertEquals(ModelStatus.APPROXIMATE, r.status)
    }

    @Test fun openVariantAddsAppendix() {
        val closed = Calculator.calculate(sphere.copy(wastePercent = "0")).blankArea!!
        val open = Calculator.calculate(
            sphere.copy(wastePercent = "0", variant = EnvelopeVariant.OPEN_APPENDIX, appendixDiameter = "0,3", appendixLength = "1"),
        )
        assertEquals(closed + PI * 0.3, open.blankArea!!, 1e-9)
    }

    @Test fun gatheredEnvelopeAreaIsTwoWalls() {
        val r = Calculator.calculate(
            sphere.copy(
                shape = ShapeType.GATHERED, envelopeWidth = "6", envelopeHeight = "10",
                gatherPart = "1,1", wastePercent = "0",
            ),
        )
        assertFalse(r.hasErrors)
        // 2 × 6 × 10 = 120 м²
        assertEquals(120.0, r.blankArea!!, 1e-9)
        assertEquals(120.0 * 30e-6 * 920, r.filmMass!!, 1e-9)
        assertEquals(ModelStatus.APPROXIMATE, r.status)
    }

    @Test fun gatheredEnvelopeWorkingHeightAndVolume() {
        val r = Calculator.calculate(
            sphere.copy(shape = ShapeType.GATHERED, envelopeWidth = "6", envelopeHeight = "10", gatherPart = "1,1"),
        )
        assertEquals(8.9, r.geometry.first { it.label.startsWith("Робоча висота") }.value, 1e-9)
        // Верхня оцінка об'єму: W² × (H − h) / π
        assertEquals(36.0 * 8.9 / PI, r.geometry.first { it.label.startsWith("Об'єм") }.value, 1e-9)
    }

    @Test fun gatherPartCannotExceedHeight() {
        val r = Calculator.calculate(
            sphere.copy(shape = ShapeType.GATHERED, envelopeWidth = "6", envelopeHeight = "10", gatherPart = "10"),
        )
        assertNotNull(r.errors[Field.GATHER_PART])
    }

    @Test fun gatherPartZeroIsAllowed() {
        val r = Calculator.calculate(
            sphere.copy(shape = ShapeType.GATHERED, envelopeWidth = "6", envelopeHeight = "10", gatherPart = "0"),
        )
        assertFalse(r.hasErrors)
        assertEquals(10.0, r.geometry.first { it.label.startsWith("Робоча висота") }.value, 1e-9)
    }

    @Test fun pearIsUnavailable() {
        val r = Calculator.calculate(sphere.copy(shape = ShapeType.PEAR))
        assertEquals(ModelStatus.UNAVAILABLE, r.status)
        assertNull(r.blankArea); assertNull(r.filmMass); assertNull(r.totalMass)
        assertTrue(r.unavailableReason!!.startsWith("Розрахунок поки недоступний"))
    }

    @Test fun resetKeepsShape() {
        val changed = sphere.copy(shape = ShapeType.CYLINDER, diameter = "9", variant = EnvelopeVariant.OPEN_APPENDIX)
        val reset = changed.resetValues()
        assertEquals(ShapeType.CYLINDER, reset.shape)
        assertEquals(EnvelopeVariant.OPEN_APPENDIX, reset.variant)
        assertEquals(ProjectInput().diameter, reset.diameter)
    }

    @Test fun withValueRoundTrip() {
        var input = ProjectInput()
        Field.entries.forEach { f -> input = Calculator.withValue(input, f, "${f.ordinal},5") }
        Field.entries.forEach { f -> assertEquals("${f.ordinal},5", Calculator.rawValue(input, f)) }
    }
}

class ReportTest {
    private val project = Project("1", "Тестова куля", 0, 0, ProjectInput())

    @Test fun passportContainsKeySections() {
        val r = Calculator.calculate(project.input)
        val titles = Report.sections(project, r, "20.10.2026").map { it.title }
        assertTrue("Вихідні параметри" in titles)
        assertTrue("Геометрія" in titles)
        assertTrue("Площа і маса" in titles)
        assertTrue("Застереження" in titles)
    }

    @Test fun plainTextHasDisclaimerAndCommaNumbers() {
        val r = Calculator.calculate(project.input)
        val text = Report.plainText(project, r, "20.10.2026")
        assertTrue(text.contains(Report.DISCLAIMER))
        assertTrue(text.contains("Маса плівки:"))
        assertTrue(Regex("\\d,\\d{3} кг").containsMatchIn(text))
    }

    @Test fun noFlightSuitabilityClaims() {
        val r = Calculator.calculate(project.input)
        val text = Report.plainText(project, r, "20.10.2026").lowercase()
        assertFalse(text.contains("придатна до польоту"))
        assertFalse(text.contains("безпечна конструкція"))
    }
}
