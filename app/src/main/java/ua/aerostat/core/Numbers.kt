package ua.aerostat.core

import java.math.BigDecimal
import java.math.RoundingMode

/** Результат розбору числового поля. */
sealed interface ParsedNumber {
    data object Empty : ParsedNumber
    data object Invalid : ParsedNumber
    data class Value(val value: Double) : ParsedNumber
}

object Numbers {
    private const val NBSP = ' '
    private const val NARROW_NBSP = ' '

    /**
     * Розбирає число, введене користувачем.
     * Підтримує десяткову кому і крапку, пробіли як роздільники тисяч, знак «−» або «-».
     * Експоненційний запис, кілька роздільників та інші символи вважаються помилкою.
     */
    fun parse(raw: String): ParsedNumber {
        val s = raw.trim()
            .replace(" ", "")
            .replace(NBSP.toString(), "")
            .replace(NARROW_NBSP.toString(), "")
            .replace('−', '-')
        if (s.isEmpty()) return ParsedNumber.Empty
        if (!Regex("^-?\\d*([.,]\\d*)?$").matches(s)) return ParsedNumber.Invalid
        val normalized = s.replace(',', '.')
        if (normalized == "-" || normalized == "." || normalized == "-.") return ParsedNumber.Invalid
        val v = normalized.toDoubleOrNull() ?: return ParsedNumber.Invalid
        if (v.isNaN() || v.isInfinite()) return ParsedNumber.Invalid
        return ParsedNumber.Value(v)
    }

    /** Форматує число з десятковою комою та нерозривними пробілами між тисячами. */
    fun format(value: Double, decimals: Int = 2): String {
        if (value.isNaN() || value.isInfinite()) return "—"
        val bd = BigDecimal(value).setScale(decimals, RoundingMode.HALF_UP)
        val plain = bd.abs().toPlainString()
        val intPart = plain.substringBefore('.')
        val fracPart = if (decimals > 0) plain.substringAfter('.', "") else ""
        val grouped = intPart.reversed().chunked(3).joinToString(NBSP.toString()).reversed()
        val sign = if (bd.signum() < 0) "−" else ""
        return if (fracPart.isEmpty()) sign + grouped else "$sign$grouped,$fracPart"
    }

    /** Значення для поля вводу (без групування), з комою. */
    fun toInput(value: Double, decimals: Int = 3): String {
        val bd = BigDecimal(value).setScale(decimals, RoundingMode.HALF_UP).stripTrailingZeros()
        return bd.toPlainString().replace('.', ',')
    }
}
