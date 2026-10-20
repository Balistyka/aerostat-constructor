package ua.aerostat.core

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sin

/** Один рядок результату з одиницею вимірювання. */
data class ResultRow(val label: String, val value: Double, val unit: String, val decimals: Int = 2) {
    val formatted: String get() = Numbers.format(value, decimals) + if (unit.isBlank()) "" else " $unit"
}

/** Точка викрійки клина: відстань уздовж осі клина від полюса та повна ширина клина. */
data class GoreStation(val distance: Double, val width: Double)

data class GoreTable(
    val count: Int,
    val length: Double,
    val maxWidth: Double,
    val stations: List<GoreStation>,
)

data class CalcResult(
    val shape: ShapeType,
    val variant: EnvelopeVariant,
    val status: ModelStatus,
    val relevantFields: List<Field>,
    val errors: Map<Field, String>,
    val geometry: List<ResultRow>,
    val layout: List<ResultRow>,
    val gores: GoreTable?,
    val envelopeArea: Double?,
    val appendixArea: Double?,
    val blankArea: Double?,
    val filmMass: Double?,
    val fittingsMass: Double?,
    val totalMass: Double?,
    val density: Double?,
    val assumptions: List<String>,
    val unavailableReason: String?,
) {
    val hasErrors get() = errors.isNotEmpty()
}

object FilmMass {
    /**
     * Маса плівки: площа (м²) × товщина (мкм → м) × густина (кг/м³) = кг.
     */
    fun mass(areaM2: Double, thicknessUm: Double, densityKgM3: Double): Double {
        require(areaM2 >= 0 && thicknessUm >= 0 && densityKgM3 >= 0) { "Від'ємні значення неприпустимі" }
        return areaM2 * (thicknessUm * 1e-6) * densityKgM3
    }
}

object Geometry {
    fun sphereSurface(d: Double) = PI * d * d
    fun sphereVolume(d: Double) = PI * d * d * d / 6.0
    fun cylinderLateral(d: Double, l: Double) = PI * d * l
    fun discArea(d: Double) = PI * d * d / 4.0
    fun cylinderVolume(d: Double, l: Double) = PI * d * d / 4.0 * l
    /** Стінки рукава: дві площини шириною W і довжиною L. */
    fun sleeveWallArea(flatWidth: Double, length: Double) = 2.0 * flatWidth * length
    /** Еквівалентний діаметр рукава: периметр 2W = πD. */
    fun sleeveEquivalentDiameter(flatWidth: Double) = 2.0 * flatWidth / PI
    /** Верхня оцінка об'єму рукава як ідеального циліндра того ж периметра. */
    fun sleeveVolumeUpperBound(flatWidth: Double, length: Double) = flatWidth * flatWidth * length / PI
    fun appendixArea(d: Double, l: Double) = PI * d * l

    /** Мінімальна кількість клинів, щоб найширше місце клина з припусками вмістилося в рулон. */
    fun minGoreCount(d: Double, usableRollWidth: Double): Int = ceil(PI * d / usableRollWidth - 1e-9).toInt()

    /** Плоский клин з синусоїдальним профілем ширини: w(s) = (πD/N)·sin(2s/D). */
    fun goreTable(d: Double, count: Int, stations: Int = 10): GoreTable {
        val length = PI * d / 2.0
        val maxWidth = PI * d / count
        val list = (0..stations).map { k ->
            val s = length * k / stations
            GoreStation(s, maxWidth * sin(PI * k / stations))
        }
        return GoreTable(count, length, maxWidth, list)
    }
}

object Calculator {

    fun relevantFields(input: ProjectInput): List<Field> {
        val geometry = when (input.shape) {
            ShapeType.SPHERE -> listOf(Field.DIAMETER, Field.ROLL_WIDTH, Field.SEAM, Field.GORE_COUNT)
            ShapeType.CYLINDER -> listOf(Field.DIAMETER, Field.CYLINDER_LENGTH, Field.ROLL_WIDTH, Field.SEAM)
            ShapeType.SLEEVE -> listOf(Field.SLEEVE_FLAT_WIDTH, Field.SLEEVE_LENGTH, Field.SEAM)
            ShapeType.PEAR -> emptyList()
        }
        val appendix = if (input.variant == EnvelopeVariant.OPEN_APPENDIX && input.shape != ShapeType.PEAR)
            listOf(Field.APPENDIX_DIAMETER, Field.APPENDIX_LENGTH) else emptyList()
        val material = listOf(Field.DENSITY, Field.THICKNESS, Field.LAYERS, Field.WASTE, Field.FITTINGS)
        return geometry + appendix + material
    }

    fun rawValue(input: ProjectInput, field: Field): String = when (field) {
        Field.DIAMETER -> input.diameter
        Field.CYLINDER_LENGTH -> input.cylinderLength
        Field.SLEEVE_FLAT_WIDTH -> input.sleeveFlatWidth
        Field.SLEEVE_LENGTH -> input.sleeveLength
        Field.GORE_COUNT -> input.goreCount
        Field.APPENDIX_DIAMETER -> input.appendixDiameter
        Field.APPENDIX_LENGTH -> input.appendixLength
        Field.DENSITY -> input.density
        Field.THICKNESS -> input.thicknessUm
        Field.LAYERS -> input.layers
        Field.WASTE -> input.wastePercent
        Field.SEAM -> input.seamCm
        Field.ROLL_WIDTH -> input.rollWidth
        Field.FITTINGS -> input.fittingsKg
    }

    fun withValue(input: ProjectInput, field: Field, value: String): ProjectInput = when (field) {
        Field.DIAMETER -> input.copy(diameter = value)
        Field.CYLINDER_LENGTH -> input.copy(cylinderLength = value)
        Field.SLEEVE_FLAT_WIDTH -> input.copy(sleeveFlatWidth = value)
        Field.SLEEVE_LENGTH -> input.copy(sleeveLength = value)
        Field.GORE_COUNT -> input.copy(goreCount = value)
        Field.APPENDIX_DIAMETER -> input.copy(appendixDiameter = value)
        Field.APPENDIX_LENGTH -> input.copy(appendixLength = value)
        Field.DENSITY -> input.copy(density = value)
        Field.THICKNESS -> input.copy(thicknessUm = value)
        Field.LAYERS -> input.copy(layers = value)
        Field.WASTE -> input.copy(wastePercent = value)
        Field.SEAM -> input.copy(seamCm = value)
        Field.ROLL_WIDTH -> input.copy(rollWidth = value)
        Field.FITTINGS -> input.copy(fittingsKg = value)
    }

    fun calculate(input: ProjectInput): CalcResult {
        val fields = relevantFields(input)
        val errors = linkedMapOf<Field, String>()
        val v = mutableMapOf<Field, Double?>()
        for (f in fields) {
            val c = Validation.check(f, rawValue(input, f))
            if (c.error != null) errors[f] = c.error else v[f] = c.value
        }

        val assumptions = mutableListOf(
            "Маса плівки = площа заготовки × товщина (м) × густина (кг/м³).",
            "Площа заготовки = площа деталей × кількість шарів × (1 + припуски/100).",
            "Густина пресетів орієнтовна; для точного результату введіть значення з паспорта плівки.",
        )

        if (input.shape == ShapeType.PEAR) {
            return CalcResult(
                shape = input.shape, variant = input.variant, status = ModelStatus.UNAVAILABLE,
                relevantFields = fields, errors = errors, geometry = emptyList(), layout = emptyList(),
                gores = null, envelopeArea = null, appendixArea = null, blankArea = null,
                filmMass = null, fittingsMass = v[Field.FITTINGS], totalMass = null,
                density = v[Field.DENSITY], assumptions = assumptions,
                unavailableReason = "Розрахунок поки недоступний: геометричну модель грушоподібної оболонки ще не перевірено. " +
                    "Площа та маса не обчислюються, щоб не подавати неперевірені значення.",
            )
        }

        val seamM = (v[Field.SEAM] ?: 0.0) / 100.0
        val rollWidth = v[Field.ROLL_WIDTH]
        if (rollWidth != null && Field.ROLL_WIDTH in fields && rollWidth - 2 * seamM <= 0.0) {
            errors[Field.ROLL_WIDTH] = "Ширина рулону має бути більшою за два припуски на шов"
        }

        val geometry = mutableListOf<ResultRow>()
        val layout = mutableListOf<ResultRow>()
        var gores: GoreTable? = null
        var envelopeArea: Double? = null
        val layers = v[Field.LAYERS]?.toInt()

        when (input.shape) {
            ShapeType.SPHERE -> {
                val d = v[Field.DIAMETER]
                if (d != null) {
                    val area = Geometry.sphereSurface(d)
                    envelopeArea = area
                    geometry += ResultRow("Діаметр", d, "м")
                    geometry += ResultRow("Висота", d, "м")
                    geometry += ResultRow("Довжина екватора", PI * d, "м")
                    geometry += ResultRow("Площа поверхні сфери", area, "м²")
                    geometry += ResultRow("Об'єм ідеальної сфери", Geometry.sphereVolume(d), "м³")
                    if (rollWidth != null && Field.ROLL_WIDTH !in errors && Field.SEAM !in errors) {
                        val usable = rollWidth - 2 * seamM
                        val minN = Geometry.minGoreCount(d, usable)
                        val userN = v[Field.GORE_COUNT]?.toInt()
                        if (userN != null && userN < minN) {
                            errors[Field.GORE_COUNT] = "Клин ширший за рулон: потрібно щонайменше $minN клинів"
                        } else if (Field.GORE_COUNT !in errors) {
                            val n = userN ?: max(minN, 6)
                            val table = Geometry.goreTable(d, n)
                            gores = table
                            val cutLength = table.length + 2 * seamM
                            layout += ResultRow("Кількість клинів", n.toDouble(), "шт.", 0)
                            layout += ResultRow("Довжина клина (по меридіану)", table.length, "м")
                            layout += ResultRow("Найбільша ширина клина", table.maxWidth, "м", 3)
                            layout += ResultRow("Ширина викрійки з припусками", table.maxWidth + 2 * seamM, "м", 3)
                            if (layers != null) {
                                val rollLen = n * cutLength * layers
                                layout += ResultRow("Потрібна довжина рулону", rollLen, "м")
                                layout += ResultRow("Площа використаного рулону", rollLen * rollWidth, "м²")
                            }
                        }
                    }
                }
                assumptions += "Сфера ідеальна; клини мають синусоїдальний профіль ширини w(s) = (πD/N)·sin(2s/D)."
                assumptions += "Для малої кількості клинів наповнена форма відхиляється від сфери (гранчастість)."
                assumptions += "Об'єм — для ідеальної сфери, без урахування деформації плівки."
            }
            ShapeType.CYLINDER -> {
                val d = v[Field.DIAMETER]
                val l = v[Field.CYLINDER_LENGTH]
                if (d != null && l != null) {
                    val lateral = Geometry.cylinderLateral(d, l)
                    val ends = 2 * Geometry.discArea(d)
                    envelopeArea = lateral + ends
                    geometry += ResultRow("Діаметр", d, "м")
                    geometry += ResultRow("Довжина", l, "м")
                    geometry += ResultRow("Площа бічної поверхні", lateral, "м²")
                    geometry += ResultRow("Площа двох днищ", ends, "м²")
                    geometry += ResultRow("Загальна площа деталей", lateral + ends, "м²")
                    geometry += ResultRow("Об'єм ідеального циліндра", Geometry.cylinderVolume(d, l), "м³")
                    if (rollWidth != null && Field.ROLL_WIDTH !in errors && Field.SEAM !in errors && layers != null) {
                        val usable = rollWidth - 2 * seamM
                        val strips = ceil(PI * d / usable - 1e-9).toInt()
                        val discStrips = ceil(d / usable - 1e-9).toInt()
                        val rollLen = (strips * (l + 2 * seamM) + 2 * discStrips * (d + 2 * seamM)) * layers
                        layout += ResultRow("Полотен бічної поверхні", strips.toDouble(), "шт.", 0)
                        layout += ResultRow("Довжина полотна з припусками", l + 2 * seamM, "м")
                        layout += ResultRow("Полотен на одне днище", discStrips.toDouble(), "шт.", 0)
                        layout += ResultRow("Потрібна довжина рулону", rollLen, "м")
                        layout += ResultRow("Площа використаного рулону", rollLen * rollWidth, "м²")
                    }
                }
                assumptions += "Циліндр ідеальний, днища плоскі круглі; під тиском днища випинаються, тому реальний об'єм дещо більший."
                assumptions += "Площа днищ — площа кругів; обрізки від вирізання кругів враховуйте у припусках."
            }
            ShapeType.SLEEVE -> {
                val w = v[Field.SLEEVE_FLAT_WIDTH]
                val l = v[Field.SLEEVE_LENGTH]
                if (w != null && l != null) {
                    val area = Geometry.sleeveWallArea(w, l)
                    envelopeArea = area
                    geometry += ResultRow("Ширина у складеному вигляді", w, "м")
                    geometry += ResultRow("Периметр рукава", 2 * w, "м")
                    geometry += ResultRow("Еквівалентний діаметр", Geometry.sleeveEquivalentDiameter(w), "м")
                    geometry += ResultRow("Довжина", l, "м")
                    geometry += ResultRow("Площа стінок (2 × ширина × довжина)", area, "м²")
                    geometry += ResultRow("Об'єм, верхня оцінка", Geometry.sleeveVolumeUpperBound(w, l), "м³")
                    if (layers != null && Field.SEAM !in errors) {
                        layout += ResultRow("Потрібна довжина рукава з рулону", (l + 2 * seamM) * layers, "м")
                    }
                }
                assumptions += "Об'єм рукава — верхня оцінка як для циліндра того ж периметра; заварені сплющені кінці зменшують реальний об'єм."
                assumptions += "Площа заготовки рукава не дорівнює площі поверхні наповненої оболонки."
            }
            ShapeType.PEAR -> Unit
        }

        var appendixArea: Double? = null
        if (input.variant == EnvelopeVariant.OPEN_APPENDIX) {
            val ad = v[Field.APPENDIX_DIAMETER]
            val al = v[Field.APPENDIX_LENGTH]
            if (ad != null && al != null) {
                appendixArea = Geometry.appendixArea(ad, al)
                geometry += ResultRow("Площа рукава-апендикса", appendixArea, "м²")
            }
            assumptions += "Рукав-апендикс — циліндрична трубка; пасивне відведення газу не моделюється."
        }

        val thickness = v[Field.THICKNESS]
        val density = v[Field.DENSITY]
        val waste = v[Field.WASTE]
        val fittings = v[Field.FITTINGS]
        val blankArea = if (envelopeArea != null && layers != null && waste != null &&
            (input.variant == EnvelopeVariant.CLOSED || appendixArea != null)
        ) (envelopeArea + (appendixArea ?: 0.0)) * layers * (1 + waste / 100.0) else null
        val filmMass = if (blankArea != null && thickness != null && density != null)
            FilmMass.mass(blankArea, thickness, density) else null
        val total = if (filmMass != null && fittings != null) filmMass + fittings else null

        return CalcResult(
            shape = input.shape, variant = input.variant, status = input.shape.status,
            relevantFields = fields, errors = errors, geometry = geometry, layout = layout, gores = gores,
            envelopeArea = envelopeArea, appendixArea = appendixArea, blankArea = blankArea,
            filmMass = filmMass, fittingsMass = fittings, totalMass = total, density = density,
            assumptions = assumptions, unavailableReason = null,
        )
    }
}
