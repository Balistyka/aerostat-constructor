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
        // Поля зворотного підбору не зберігаються у проєкті, тому їх пропускаємо.
        val stored = Field.entries - setOf(Field.TARGET_PAYLOAD, Field.FLIGHT_HOURS, Field.LIFT_LOSS_RATE, Field.ASPECT_RATIO)
        var input = ProjectInput()
        stored.forEach { f -> input = Calculator.withValue(input, f, "${f.ordinal},5") }
        stored.forEach { f -> assertEquals("${f.ordinal},5", Calculator.rawValue(input, f)) }
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

class AtmosphereTest {
    @Test fun seaLevelMatchesStandard() {
        val a = Atmosphere.airAt(0.0)
        assertEquals(288.15, a.temperatureK, 1e-6)
        assertEquals(101325.0, a.pressurePa, 1e-6)
        assertEquals(1.225, a.density, 1e-3)
    }

    @Test fun tenKilometresMatchesStandard() {
        val a = Atmosphere.airAt(10_000.0)
        assertEquals(223.15, a.temperatureK, 1e-6)
        assertEquals(26_436.0, a.pressurePa, 60.0)
        assertEquals(0.4127, a.density, 2e-3)
    }

    @Test fun stratosphereIsIsothermal() {
        assertEquals(216.65, Atmosphere.temperatureK(15_000.0), 1e-6)
        assertTrue(Atmosphere.pressurePa(15_000.0) < Atmosphere.pressurePa(11_000.0))
    }

    @Test fun gasDensitiesAtSeaLevel() {
        val t = 288.15
        assertEquals(0.0852, Atmosphere.density(101325.0, t, GasType.HYDROGEN.molarMass), 1e-3)
        assertEquals(0.1691, Atmosphere.density(101325.0, t, GasType.HELIUM.molarMass), 1e-3)
    }

    @Test fun pressureFallsWithAltitude() {
        var prev = Atmosphere.pressurePa(0.0)
        for (h in 1..20) {
            val p = Atmosphere.pressurePa(h * 1000.0)
            assertTrue("Тиск має спадати на $h км", p < prev)
            prev = p
        }
    }
}

class LiftTest {
    private fun case(volume: Double = 73.03, gas: GasType = GasType.HYDROGEN, mass: Double? = 4.4, ballast: Double = 12.5) =
        Lift.calculate(
            gas = gas, envelopeVolume = volume, groundTempC = 15.0, groundPressureHPa = 1013.25,
            targetAltitudeM = 10_000.0, ballastPercent = ballast, envelopeMass = mass,
            cylinderVolumeL = 50.0, cylinderPressureAtm = 150.0, cylinderResidualAtm = 3.0,
        )

    @Test fun grossLiftAtAltitude() {
        val r = case()
        // 73,03 × (0,4127 − 0,0287) ≈ 28,0 кг
        assertEquals(28.0, r.grossLiftTarget, 0.3)
    }

    @Test fun groundFillIsSmallerThanEnvelope() {
        val r = case()
        assertTrue(r.fillVolumeGround < r.envelopeVolume)
        // Відношення об'ємів дорівнює відношенню густин газу
        assertEquals(r.gasDensityTarget / r.gasDensityGround, r.fillVolumeGround / r.envelopeVolume, 1e-9)
        assertEquals(24.6, r.fillVolumeGround, 0.4)
    }

    @Test fun cylinderOutputAndCount() {
        val r = case()
        // 50 л × (150 − 3) атм = 7,35 м³
        assertEquals(7.35, r.cylinderOutput!!, 0.01)
        assertEquals(r.fillVolumeGround / r.cylinderOutput!!, r.cylinderCount!!, 1e-9)
        assertEquals(3.3, r.cylinderCount!!, 0.2)
    }

    @Test fun payloadIsLiftMinusBallastAndEnvelope() {
        val r = case()
        assertEquals(r.grossLiftTarget * 0.125, r.ballastMass!!, 1e-9)
        assertEquals(r.grossLiftTarget - r.ballastMass!! - 4.4, r.payload!!, 1e-9)
        assertEquals(20.2, r.payload!!, 0.4)
    }

    @Test fun heliumLiftsLessThanHydrogen() {
        assertTrue(case(gas = GasType.HELIUM).grossLiftTarget < case(gas = GasType.HYDROGEN).grossLiftTarget)
    }

    @Test fun heavierEnvelopeMeansSmallerPayload() {
        assertTrue(case(mass = 10.0).payload!! < case(mass = 4.4).payload!!)
    }

    @Test fun noEnvelopeMassMeansNoPayload() {
        assertNull(case(mass = null).payload)
    }

    @Test fun liftAtGroundUsesPartialFill() {
        val r = case()
        assertTrue(r.grossLiftGround > 0)
        // Маса газу стала, тому підйомна маса біля землі близька до висотної
        assertEquals(r.grossLiftTarget, r.grossLiftGround, 2.0)
    }

    @Test fun zeroBallastGivesLargerPayload() {
        assertTrue(case(ballast = 0.0).payload!! > case(ballast = 12.5).payload!!)
    }
}

class CalculatorLiftTest {
    private val envelope = ProjectInput(
        shape = ShapeType.GATHERED, variant = EnvelopeVariant.OPEN_APPENDIX,
        envelopeWidth = "6", envelopeHeight = "10", gatherPart = "1,1",
        thicknessUm = "30", density = "920", layers = "1", wastePercent = "10", seamCm = "2",
        fittingsKg = "0,5", fillFactor = "72", targetAltitude = "10",
    )

    @Test fun liftIsComputedForEnvelope() {
        val r = Calculator.calculate(envelope)
        assertFalse(r.hasErrors)
        val lift = r.lift!!
        assertEquals(73.0, lift.envelopeVolume, 1.0)
        assertEquals(28.0, lift.grossLiftTarget, 0.5)
        assertNotNull(lift.payload)
    }

    @Test fun fillFactorScalesVolume() {
        val full = Calculator.calculate(envelope.copy(fillFactor = "100")).lift!!.envelopeVolume
        val part = Calculator.calculate(envelope.copy(fillFactor = "50")).lift!!.envelopeVolume
        assertEquals(full / 2, part, 1e-9)
    }

    @Test fun pearHasNoLift() {
        assertNull(Calculator.calculate(envelope.copy(shape = ShapeType.PEAR)).lift)
    }

    @Test fun invalidAltitudeBlocksLift() {
        val r = Calculator.calculate(envelope.copy(targetAltitude = "99"))
        assertNotNull(r.errors[Field.TARGET_ALTITUDE])
        assertNull(r.lift)
    }

    @Test fun negativeTemperatureIsAccepted() {
        val r = Calculator.calculate(envelope.copy(groundTemp = "-20"))
        assertFalse(r.hasErrors)
        assertNotNull(r.lift)
    }
}

class CylinderPresetTest {
    @Test fun defaultProjectUsesNineCubicMetreCylinder() {
        val p = ProjectInput()
        assertEquals(CylinderPreset.STANDARD_9, p.cylinder)
        assertEquals("45", p.cylinderVolume)
        assertEquals("200", p.cylinderPressure)
    }

    @Test fun presetsHaveSaneOutput() {
        CylinderPreset.entries.filter { it.volumeL != null }.forEach { c ->
            val out = c.volumeL!! / 1000.0 * c.pressureAtm!!
            assertTrue("${c.name}: $out", out in 4.0..12.0)
        }
    }

    @Test fun standardNineGivesNineCubicMetres() {
        val c = CylinderPreset.STANDARD_9
        assertEquals(9.0, c.volumeL!! / 1000.0 * c.pressureAtm!!, 1e-9)
    }

    @Test fun defaultProjectCylinderCountIsComputed() {
        val r = Calculator.calculate(
            ProjectInput(shape = ShapeType.GATHERED, envelopeWidth = "6", envelopeHeight = "10", gatherPart = "1,1"),
        )
        val lift = r.lift!!
        // 45 л × (200 − 3) атм = 8,865 м³ з балона
        assertEquals(8.865, lift.cylinderOutput!!, 0.01)
        assertEquals(lift.fillVolumeGround / lift.cylinderOutput!!, lift.cylinderCount!!, 1e-9)
    }

    @Test fun customPresetKeepsOwnValues() {
        val p = ProjectInput(cylinder = CylinderPreset.CUSTOM, cylinderVolume = "12", cylinderPressure = "300")
        val r = Calculator.calculate(p.copy(shape = ShapeType.SPHERE, diameter = "3"))
        assertEquals(12.0 / 1000.0 * (300.0 - 3.0), r.lift!!.cylinderOutput!!, 0.02)
    }
}

class InverseTest {
    private val base = ProjectInput(
        shape = ShapeType.GATHERED, gas = GasType.HYDROGEN,
        envelopeWidth = "6", gatherPart = "1,1",
        thicknessUm = "30", density = "920", layers = "1", wastePercent = "10",
        fittingsKg = "0,5", fillFactor = "72", targetAltitude = "10",
        ballastPercent = "12", groundTemp = "15", groundPressure = "1013",
    )
    private val input = InverseInput(base = base, targetPayload = "20", lossRatePerDay = "0")

    private fun solved(i: InverseInput) = Inverse.solve(i).solution ?: error("Немає розв'язку")

    /** Головна перевірка: підібрана оболонка справді дає заданий вантаж. */
    private fun assertRoundTrip(i: InverseInput, target: Double) {
        val s = solved(i)
        val forward = Calculator.calculate(s.project)
        val lift = forward.lift!!
        val ballast = lift.grossLiftTarget * (Numbers.parse(s.project.ballastPercent) as ParsedNumber.Value).value / 100.0
        val payload = lift.grossLiftTarget - ballast - s.durationReserve - forward.totalMass!!
        assertTrue("Вантаж $payload менший за заданий $target", payload >= target - 0.02)
        assertTrue("Вантаж $payload значно більший за заданий $target", payload <= target + 1.5)
        assertEquals(payload, s.payload, 1e-6)
    }

    @Test fun gatheredRoundTrip() = assertRoundTrip(input, 20.0)

    @Test fun sleeveRoundTrip() = assertRoundTrip(
        input.copy(base = base.copy(shape = ShapeType.SLEEVE, sleeveFlatWidth = "6")), 20.0,
    )

    @Test fun sphereRoundTrip() = assertRoundTrip(
        input.copy(base = base.copy(shape = ShapeType.SPHERE, fillFactor = "100")), 20.0,
    )

    @Test fun cylinderRoundTrip() = assertRoundTrip(
        input.copy(base = base.copy(shape = ShapeType.CYLINDER, fillFactor = "100"), aspectRatio = "3"), 20.0,
    )

    @Test fun smallPayloadRoundTrip() = assertRoundTrip(input.copy(targetPayload = "1,5"), 1.5)

    @Test fun gatheredHeightMatchesPractice() {
        // Конверт 6 м під 20 кг на 10 км має вийти близьким до 10 м висоти
        val s = solved(input)
        assertEquals("Висота конверта", s.mainDimensionLabel)
        assertTrue("Висота ${s.mainDimension} поза очікуваним діапазоном", s.mainDimension in 8.0..14.0)
        assertEquals(6.0, s.secondDimension!!, 1e-9)
    }

    @Test fun narrowSleeveIsImpossible() {
        val r = Inverse.solve(input.copy(base = base.copy(envelopeWidth = "0,5")))
        assertEquals(InverseFailure.TOO_HEAVY_FILM, r.failure)
        assertNull(r.solution)
        assertNotNull(r.minimumWidth)
        assertTrue(r.failureText!!.contains("Потрібна ширина понад"))
    }

    @Test fun widthAboveMinimumWorks() {
        val min = Inverse.solve(input.copy(base = base.copy(envelopeWidth = "0,5"))).minimumWidth!!
        // Трохи ширше за мінімум висота ще завелика, а із запасом розв'язок уже є
        assertNotNull(Inverse.solve(input.copy(base = base.copy(envelopeWidth = Numbers.toInput(min * 6)))).solution)
        assertEquals(
            InverseFailure.OUT_OF_RANGE,
            Inverse.solve(input.copy(base = base.copy(envelopeWidth = Numbers.toInput(min * 1.5)))).failure,
        )
    }

    @Test fun ballastAndReserveCannotExceedLift() {
        val r = Inverse.solve(input.copy(base = base.copy(ballastPercent = "50"), flightHours = "48", lossRatePerDay = "30"))
        assertEquals(InverseFailure.RESERVE_TOO_BIG, r.failure)
    }

    @Test fun durationWithoutLossRateChangesNothing() {
        val withTime = Inverse.solve(input.copy(flightHours = "24"))
        assertEquals(solved(input).mainDimension, withTime.solution!!.mainDimension, 1e-9)
        assertTrue(withTime.notes.any { it.contains("не впливає") })
    }

    @Test fun longerFlightNeedsBiggerEnvelope() {
        val short = solved(input.copy(flightHours = "6", lossRatePerDay = "6"))
        val long = solved(input.copy(flightHours = "48", lossRatePerDay = "6"))
        assertTrue(long.mainDimension > short.mainDimension)
        assertTrue(long.durationReserve > short.durationReserve)
    }

    @Test fun higherAltitudeNeedsBiggerEnvelope() {
        val low = solved(input.copy(base = base.copy(targetAltitude = "3")))
        val high = solved(input.copy(base = base.copy(targetAltitude = "15")))
        assertTrue(high.mainDimension > low.mainDimension)
    }

    @Test fun heliumNeedsBiggerEnvelopeThanHydrogen() {
        val h2 = solved(input)
        val he = solved(input.copy(base = base.copy(gas = GasType.HELIUM)))
        assertTrue(he.mainDimension > h2.mainDimension)
    }

    @Test fun heavierPayloadNeedsBiggerEnvelope() {
        assertTrue(solved(input.copy(targetPayload = "40")).mainDimension > solved(input).mainDimension)
    }

    @Test fun thinnerFilmNeedsSmallerEnvelope() {
        val thin = solved(input.copy(base = base.copy(thicknessUm = "15")))
        assertTrue(thin.mainDimension < solved(input).mainDimension)
    }

    @Test fun pearHasNoModel() {
        val r = Inverse.solve(input.copy(base = base.copy(shape = ShapeType.PEAR)))
        assertEquals(InverseFailure.NO_MODEL, r.failure)
    }

    @Test fun hugePayloadIsOutOfRange() {
        val r = Inverse.solve(input.copy(base = base.copy(shape = ShapeType.SPHERE, targetAltitude = "20"), targetPayload = "5000"))
        assertEquals(InverseFailure.OUT_OF_RANGE, r.failure)
        assertNull(r.solution)
    }

    @Test fun invalidInputIsReported() {
        val r = Inverse.solve(input.copy(targetPayload = "-5"))
        assertTrue(r.hasErrors)
        assertNotNull(r.errors[Field.TARGET_PAYLOAD])
        assertNull(r.solution)
    }

    @Test fun inverseReportsCylinders() {
        val s = solved(input)
        assertNotNull(s.cylinderOutput)
        assertTrue(s.cylinderCount!! > 0)
        assertEquals(s.fillVolumeGround / s.cylinderOutput!!, s.cylinderCount!!, 1e-9)
    }

    @Test fun producedProjectKeepsShapeAndGas() {
        val s = solved(input.copy(base = base.copy(gas = GasType.HELIUM)))
        assertEquals(ShapeType.GATHERED, s.project.shape)
        assertEquals(GasType.HELIUM, s.project.gas)
        assertEquals("1,1", s.project.gatherPart)
    }

    @Test fun cylinderLengthFollowsAspectRatio() {
        val s = solved(input.copy(base = base.copy(shape = ShapeType.CYLINDER, fillFactor = "100"), aspectRatio = "4"))
        assertEquals(s.mainDimension * 4, s.secondDimension!!, 0.02)
    }

    @Test fun everyInverseFieldHasHelp() {
        Inverse.fields(input).forEach { HelpTopics.forField(it) }
        listOf(Field.TARGET_PAYLOAD, Field.FLIGHT_HOURS, Field.LIFT_LOSS_RATE, Field.ASPECT_RATIO)
            .forEach { assertNotNull(HelpTopics.forField(it)) }
    }
}
