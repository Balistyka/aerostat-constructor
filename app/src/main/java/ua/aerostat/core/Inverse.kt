package ua.aerostat.core

import kotlin.math.PI
import kotlin.math.ceil

/**
 * Зворотний розрахунок: відомі корисний вантаж, висота й (за потреби) тривалість —
 * потрібно підібрати розміри оболонки заданої конструкції.
 */
data class InverseInput(
    val base: ProjectInput = ProjectInput(shape = ShapeType.GATHERED),
    val targetPayload: String = "20",
    val flightHours: String = "",
    val lossRatePerDay: String = "0",
    val aspectRatio: String = "3",
) {
    val shape get() = base.shape
}

/** Чому підбір неможливий. */
enum class InverseFailure { TOO_HEAVY_FILM, RESERVE_TOO_BIG, OUT_OF_RANGE, NO_MODEL }

data class InverseSolution(
    val shape: ShapeType,
    /** Головний підібраний розмір: діаметр (сфера, циліндр) або висота (рукав, конверт). */
    val mainDimension: Double,
    val mainDimensionLabel: String,
    val secondDimension: Double?,
    val secondDimensionLabel: String?,
    val workingVolume: Double,
    val filmArea: Double,
    val filmMass: Double,
    val envelopeMass: Double,
    val grossLift: Double,
    val ballastMass: Double,
    val durationReserve: Double,
    val payload: Double,
    val project: ProjectInput,
)

data class InverseResult(
    val errors: Map<Field, String>,
    val solution: InverseSolution?,
    val failure: InverseFailure?,
    val failureText: String?,
    val minimumWidth: Double?,
    val notes: List<String>,
    val assumptions: List<String>,
) {
    val hasErrors get() = errors.isNotEmpty()
}

object Inverse {

    /** Поля, які вводить користувач для зворотного розрахунку. */
    fun fields(input: InverseInput): List<Field> {
        val target = listOf(Field.TARGET_PAYLOAD, Field.TARGET_ALTITUDE, Field.FLIGHT_HOURS, Field.LIFT_LOSS_RATE)
        val shapeFields = when (input.shape) {
            ShapeType.GATHERED -> listOf(Field.ENVELOPE_WIDTH, Field.GATHER_PART)
            ShapeType.SLEEVE -> listOf(Field.SLEEVE_FLAT_WIDTH)
            ShapeType.CYLINDER -> listOf(Field.ASPECT_RATIO)
            ShapeType.SPHERE -> emptyList()
            ShapeType.PEAR -> emptyList()
        }
        val material = listOf(Field.THICKNESS, Field.DENSITY, Field.LAYERS, Field.WASTE, Field.FITTINGS)
        val gas = listOf(Field.FILL_FACTOR, Field.BALLAST_PERCENT, Field.GROUND_TEMP, Field.GROUND_PRESSURE)
        return target + shapeFields + material + gas
    }

    fun rawValue(input: InverseInput, field: Field): String = when (field) {
        Field.TARGET_PAYLOAD -> input.targetPayload
        Field.FLIGHT_HOURS -> input.flightHours
        Field.LIFT_LOSS_RATE -> input.lossRatePerDay
        Field.ASPECT_RATIO -> input.aspectRatio
        else -> Calculator.rawValue(input.base, field)
    }

    fun withValue(input: InverseInput, field: Field, value: String): InverseInput = when (field) {
        Field.TARGET_PAYLOAD -> input.copy(targetPayload = value)
        Field.FLIGHT_HOURS -> input.copy(flightHours = value)
        Field.LIFT_LOSS_RATE -> input.copy(lossRatePerDay = value)
        Field.ASPECT_RATIO -> input.copy(aspectRatio = value)
        else -> input.copy(base = Calculator.withValue(input.base, field, value))
    }

    val assumptions = listOf(
        "Розміри підібрано так, щоб на розрахунковій висоті лишався заданий корисний вантаж.",
        "Запас на тривалість польоту = підйомна маса × втрата за добу × години / 24; коефіцієнт втрати задає користувач за власним досвідом.",
        "Оболонка вважається відкритою: на висоті вона наповнена повністю, газ розширюється до тиску довкілля.",
        "Не враховано намерзання, вологу, вітрове навантаження, аеродинаміку та запас на нерівномірність наповнення.",
        "Результат — розрахункова оцінка, а не підтвердження придатності конструкції до польоту.",
    )

    fun solve(input: InverseInput): InverseResult {
        val fields = fields(input)
        val errors = linkedMapOf<Field, String>()
        val v = mutableMapOf<Field, Double>()
        for (f in fields) {
            val c = Validation.check(f, rawValue(input, f))
            if (c.error != null) errors[f] = c.error else c.value?.let { v[f] = it }
        }
        val notes = mutableListOf<String>()

        if (input.shape == ShapeType.PEAR) {
            return InverseResult(
                errors, null, InverseFailure.NO_MODEL,
                "Розрахунок поки недоступний: геометричну модель грушоподібної оболонки не перевірено.",
                null, notes, assumptions,
            )
        }
        if (errors.isNotEmpty()) {
            return InverseResult(errors, null, null, null, null, notes, assumptions)
        }

        val payloadTarget = v.getValue(Field.TARGET_PAYLOAD)
        val altitudeM = v.getValue(Field.TARGET_ALTITUDE) * 1000.0
        val fill = v.getValue(Field.FILL_FACTOR) / 100.0
        val ballastFraction = v.getValue(Field.BALLAST_PERCENT) / 100.0
        val hours = v[Field.FLIGHT_HOURS] ?: 0.0
        val lossPerDay = v.getValue(Field.LIFT_LOSS_RATE) / 100.0
        val durationFraction = lossPerDay * hours / 24.0
        if (hours > 0 && lossPerDay == 0.0) {
            notes += "Втрату підйому за добу не задано, тому тривалість не впливає на розміри."
        }
        val reserveFraction = ballastFraction + durationFraction
        if (reserveFraction >= 1.0) {
            return InverseResult(
                errors, null, InverseFailure.RESERVE_TOO_BIG,
                "Баласт разом із запасом на тривалість перевищує всю підйомну масу. Зменште відсоток баласту, " +
                    "тривалість або втрату за добу.",
                null, notes, assumptions,
            )
        }

        val air = Atmosphere.airAt(altitudeM)
        val gasDensity = Atmosphere.density(air.pressurePa, air.temperatureK, input.base.gas.molarMass)
        val deltaRho = air.density - gasDensity

        val k = v.getValue(Field.LAYERS) * (1 + v.getValue(Field.WASTE) / 100.0)
        val sigma = v.getValue(Field.THICKNESS) * 1e-6 * v.getValue(Field.DENSITY)   // кг/м²
        val fittings = v.getValue(Field.FITTINGS)
        val appendixArea = if (input.base.variant == EnvelopeVariant.OPEN_APPENDIX) {
            val d = Validation.check(Field.APPENDIX_DIAMETER, input.base.appendixDiameter).value
            val l = Validation.check(Field.APPENDIX_LENGTH, input.base.appendixLength).value
            if (d != null && l != null) Geometry.appendixArea(d, l) else 0.0
        } else 0.0

        // Корисна частка підйомної маси після баласту й запасу на тривалість
        val usable = deltaRho * fill * (1 - reserveFraction)

        return when (input.shape) {
            ShapeType.GATHERED, ShapeType.SLEEVE -> {
                val width = if (input.shape == ShapeType.GATHERED) v.getValue(Field.ENVELOPE_WIDTH)
                else v.getValue(Field.SLEEVE_FLAT_WIDTH)
                val gather = if (input.shape == ShapeType.GATHERED) v.getValue(Field.GATHER_PART) else 0.0
                // Підйом на метр висоти мінус маса плівки на метр висоти
                val liftPerMetre = width * width / PI * usable
                val massPerMetre = 2 * width * k * sigma
                val slope = liftPerMetre - massPerMetre
                val minWidth = 2 * PI * k * sigma / usable
                if (slope <= 0) {
                    InverseResult(
                        errors, null, InverseFailure.TOO_HEAVY_FILM,
                        "За ширини ${Numbers.format(width)} м плівка важча за підйом, тому жодна висота не дасть вантажу. " +
                            "Потрібна ширина понад ${Numbers.format(minWidth)} м або тонша чи легша плівка.",
                        minWidth, notes, assumptions,
                    )
                } else {
                    val needed = payloadTarget + fittings + appendixArea * k * sigma + liftPerMetre * gather
                    val height = needed / slope
                    if (height > 200.0) {
                        InverseResult(
                            errors, null, InverseFailure.OUT_OF_RANGE,
                            "Потрібна висота ${Numbers.format(height)} м перевищує межу 200 м. " +
                                "Візьміть ширший рукав, тоншу плівку або менший вантаж.",
                            minWidth, notes, assumptions,
                        )
                    } else {
                        notes += "Мінімальна ширина рукава для цієї плівки — ${Numbers.format(minWidth)} м."
                        if (height > 8 * width) {
                            notes += "Висота більша за ширину у ${Numbers.format(height / width, 1)} рази: " +
                                "така витягнута оболонка погано тримає форму. Візьміть ширший рукав."
                        }
                        build(input, v, height, width, gather, air, gasDensity, fill, ballastFraction, durationFraction, notes)
                    }
                }
            }
            ShapeType.SPHERE, ShapeType.CYLINDER -> {
                val ratio = if (input.shape == ShapeType.CYLINDER) v.getValue(Field.ASPECT_RATIO) else 0.0
                // payload(D) монотонно зростає: об'єм ~ D³, площа ~ D²
                fun payloadOf(d: Double): Double {
                    val volume = if (input.shape == ShapeType.SPHERE) Geometry.sphereVolume(d)
                    else Geometry.cylinderVolume(d, ratio * d)
                    val area = if (input.shape == ShapeType.SPHERE) Geometry.sphereSurface(d)
                    else Geometry.cylinderLateral(d, ratio * d) + 2 * Geometry.discArea(d)
                    return volume * usable - (area + appendixArea) * k * sigma - fittings
                }
                val maxD = 50.0
                if (payloadOf(maxD) < payloadTarget) {
                    InverseResult(
                        errors, null, InverseFailure.OUT_OF_RANGE,
                        "Навіть за діаметра ${Numbers.format(maxD)} м вантаж не досягається. " +
                            "Зменште вантаж, висоту, баласт або візьміть тоншу плівку.",
                        null, notes, assumptions,
                    )
                } else {
                    var lo = 0.01
                    var hi = maxD
                    repeat(200) {
                        val mid = (lo + hi) / 2
                        if (payloadOf(mid) < payloadTarget) lo = mid else hi = mid
                    }
                    val d = ceil(hi * 100) / 100.0
                    build(input, v, d, null, 0.0, air, gasDensity, fill, ballastFraction, durationFraction, notes)
                }
            }
            ShapeType.PEAR -> InverseResult(errors, null, InverseFailure.NO_MODEL, null, null, notes, assumptions)
        }
    }

    /** Готує проєкт із підібраними розмірами і перевіряє його прямим розрахунком. */
    private fun build(
        input: InverseInput,
        v: Map<Field, Double>,
        main: Double,
        width: Double?,
        gather: Double,
        air: AirState,
        gasDensity: Double,
        fill: Double,
        ballastFraction: Double,
        durationFraction: Double,
        notes: MutableList<String>,
    ): InverseResult {
        val rounded = ceil(main * 100) / 100.0
        var project = input.base
        val label: String
        var second: Double? = null
        var secondLabel: String? = null
        when (input.shape) {
            ShapeType.GATHERED -> {
                project = project.copy(envelopeHeight = Numbers.toInput(rounded), envelopeWidth = Numbers.toInput(width!!))
                label = "Висота конверта"
                second = width; secondLabel = "Ширина конверта (задана)"
            }
            ShapeType.SLEEVE -> {
                project = project.copy(sleeveLength = Numbers.toInput(rounded), sleeveFlatWidth = Numbers.toInput(width!!))
                label = "Довжина рукава"
                second = width; secondLabel = "Ширина рукава (задана)"
            }
            ShapeType.SPHERE -> {
                project = project.copy(diameter = Numbers.toInput(rounded))
                label = "Діаметр сфери"
            }
            ShapeType.CYLINDER -> {
                val len = ceil(rounded * v.getValue(Field.ASPECT_RATIO) * 100) / 100.0
                project = project.copy(diameter = Numbers.toInput(rounded), cylinderLength = Numbers.toInput(len))
                label = "Діаметр циліндра"
                second = len; secondLabel = "Довжина циліндра"
            }
            ShapeType.PEAR -> label = ""
        }
        project = project.copy(
            targetAltitude = Numbers.toInput(v.getValue(Field.TARGET_ALTITUDE)),
            ballastPercent = Numbers.toInput(v.getValue(Field.BALLAST_PERCENT)),
            fillFactor = Numbers.toInput(v.getValue(Field.FILL_FACTOR)),
            groundTemp = Numbers.toInput(v.getValue(Field.GROUND_TEMP)),
            groundPressure = Numbers.toInput(v.getValue(Field.GROUND_PRESSURE)),
            thicknessUm = Numbers.toInput(v.getValue(Field.THICKNESS)),
            density = Numbers.toInput(v.getValue(Field.DENSITY)),
            layers = Numbers.toInput(v.getValue(Field.LAYERS)),
            wastePercent = Numbers.toInput(v.getValue(Field.WASTE)),
            fittingsKg = Numbers.toInput(v.getValue(Field.FITTINGS)),
            gatherPart = if (input.shape == ShapeType.GATHERED) Numbers.toInput(gather) else project.gatherPart,
        )

        // Самоперевірка прямим розрахунком
        val forward = Calculator.calculate(project)
        val lift = forward.lift
        val volume = lift?.envelopeVolume ?: 0.0
        val gross = lift?.grossLiftTarget ?: volume * (air.density - gasDensity)
        val envelopeMass = forward.totalMass ?: 0.0
        val ballast = gross * ballastFraction
        val reserve = gross * durationFraction
        val payload = gross - ballast - reserve - envelopeMass
        if (reserve > 0) notes += "Запас на тривалість: ${Numbers.format(reserve)} кг підйомної маси."

        return InverseResult(
            errors = emptyMap(),
            solution = InverseSolution(
                shape = input.shape,
                mainDimension = rounded, mainDimensionLabel = label,
                secondDimension = second, secondDimensionLabel = secondLabel,
                workingVolume = volume,
                filmArea = forward.blankArea ?: 0.0,
                filmMass = forward.filmMass ?: 0.0,
                envelopeMass = envelopeMass,
                grossLift = gross,
                ballastMass = ballast,
                durationReserve = reserve,
                payload = payload,
                project = project,
            ),
            failure = null, failureText = null, minimumWidth = null, notes = notes, assumptions = assumptions,
        )
    }
}
