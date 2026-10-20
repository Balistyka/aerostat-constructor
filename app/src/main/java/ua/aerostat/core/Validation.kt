package ua.aerostat.core

/** Правила перевірки числового поля. */
data class FieldRule(
    val min: Double,
    val max: Double,
    val allowZero: Boolean = false,
    val integer: Boolean = false,
    val optional: Boolean = false,
    val negativeAllowed: Boolean = false,
)

/** Результат перевірки поля: або значення, або повідомлення українською. */
data class FieldCheck(val value: Double?, val error: String?) {
    val isOk get() = error == null
}

object Validation {
    val rules: Map<Field, FieldRule> = mapOf(
        Field.DIAMETER to FieldRule(min = 0.1, max = 50.0),
        Field.CYLINDER_LENGTH to FieldRule(min = 0.1, max = 200.0),
        Field.SLEEVE_FLAT_WIDTH to FieldRule(min = 0.05, max = 20.0),
        Field.SLEEVE_LENGTH to FieldRule(min = 0.1, max = 200.0),
        Field.ENVELOPE_WIDTH to FieldRule(min = 0.05, max = 20.0),
        Field.ENVELOPE_HEIGHT to FieldRule(min = 0.1, max = 200.0),
        Field.GATHER_PART to FieldRule(min = 0.0, max = 50.0, allowZero = true),
        Field.GORE_COUNT to FieldRule(min = 3.0, max = 200.0, integer = true, optional = true),
        Field.APPENDIX_DIAMETER to FieldRule(min = 0.01, max = 5.0),
        Field.APPENDIX_LENGTH to FieldRule(min = 0.0, max = 20.0, allowZero = true),
        Field.DENSITY to FieldRule(min = 100.0, max = 3000.0),
        Field.THICKNESS to FieldRule(min = 1.0, max = 1000.0),
        Field.LAYERS to FieldRule(min = 1.0, max = 10.0, integer = true),
        Field.WASTE to FieldRule(min = 0.0, max = 100.0, allowZero = true),
        Field.SEAM to FieldRule(min = 0.0, max = 20.0, allowZero = true),
        Field.ROLL_WIDTH to FieldRule(min = 0.1, max = 20.0),
        Field.FITTINGS to FieldRule(min = 0.0, max = 1000.0, allowZero = true),
        Field.FILL_FACTOR to FieldRule(min = 1.0, max = 100.0),
        Field.GROUND_TEMP to FieldRule(min = -60.0, max = 60.0, allowZero = true, negativeAllowed = true),
        Field.GROUND_PRESSURE to FieldRule(min = 800.0, max = 1100.0),
        Field.TARGET_ALTITUDE to FieldRule(min = 0.0, max = 20.0, allowZero = true),
        Field.BALLAST_PERCENT to FieldRule(min = 0.0, max = 50.0, allowZero = true),
        Field.CYLINDER_VOLUME to FieldRule(min = 1.0, max = 500.0),
        Field.CYLINDER_PRESSURE to FieldRule(min = 1.0, max = 400.0),
        Field.CYLINDER_RESIDUAL to FieldRule(min = 0.0, max = 100.0, allowZero = true),
    )

    fun check(field: Field, raw: String): FieldCheck = check(raw, rules.getValue(field), field.unit)

    fun check(raw: String, rule: FieldRule, unit: String = ""): FieldCheck {
        val u = if (unit.isBlank()) "" else " $unit"
        return when (val p = Numbers.parse(raw)) {
            ParsedNumber.Empty ->
                if (rule.optional) FieldCheck(null, null) else FieldCheck(null, "Введіть значення")
            ParsedNumber.Invalid -> FieldCheck(null, "Некоректне число. Приклад: 1,5")
            is ParsedNumber.Value -> {
                val v = p.value
                when {
                    v < 0 && !rule.negativeAllowed -> FieldCheck(null, "Значення не може бути від'ємним")
                    v < rule.min && rule.negativeAllowed ->
                        FieldCheck(null, "Занадто мале значення (не менше ${Numbers.format(rule.min, 0)}$u)")
                    v == 0.0 && !rule.allowZero -> FieldCheck(null, "Значення має бути більше нуля")
                    rule.integer && v != Math.floor(v) -> FieldCheck(null, "Потрібне ціле число")
                    v > rule.max -> FieldCheck(null, "Занадто велике значення (не більше ${Numbers.format(rule.max, if (rule.integer) 0 else 2)}$u)")
                    v < rule.min && !(v == 0.0 && rule.allowZero) ->
                        FieldCheck(null, "Занадто мале значення (не менше ${Numbers.format(rule.min, if (rule.integer) 0 else 2)}$u)")
                    else -> FieldCheck(v, null)
                }
            }
        }
    }
}
