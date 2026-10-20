package ua.aerostat.core

import kotlin.math.exp
import kotlin.math.pow

/** Підйомний газ. Молярні маси — за таблицею атомних мас. */
enum class GasType(val title: String, val molarMass: Double) {
    HYDROGEN("Водень (H₂)", 0.0020159),
    HELIUM("Гелій (He)", 0.0040026),
}

/** Стан атмосфери на заданій висоті. */
data class AirState(val altitude: Double, val temperatureK: Double, val pressurePa: Double, val density: Double)

/**
 * Міжнародна стандартна атмосфера (МСА) до 20 км.
 * Тропосфера 0–11 км: лінійне падіння температури 6,5 °C/км.
 * Нижня стратосфера 11–20 км: ізотермічний шар 216,65 K.
 */
object Atmosphere {
    const val R = 8.31446261815324          // Дж/(моль·К)
    const val G = 9.80665                   // м/с²
    const val M_AIR = 0.0289647             // кг/моль
    const val T0 = 288.15                   // K
    const val P0 = 101325.0                 // Па
    const val LAPSE = 0.0065                // K/м
    const val TROPOPAUSE = 11000.0          // м

    fun temperatureK(altitudeM: Double): Double =
        if (altitudeM <= TROPOPAUSE) T0 - LAPSE * altitudeM else 216.65

    fun pressurePa(altitudeM: Double): Double =
        if (altitudeM <= TROPOPAUSE) {
            P0 * (1 - LAPSE * altitudeM / T0).pow(G * M_AIR / (R * LAPSE))
        } else {
            val p11 = P0 * (1 - LAPSE * TROPOPAUSE / T0).pow(G * M_AIR / (R * LAPSE))
            p11 * exp(-G * M_AIR * (altitudeM - TROPOPAUSE) / (R * 216.65))
        }

    /** Густина ідеального газу: ρ = p·M/(R·T). */
    fun density(pressurePa: Double, temperatureK: Double, molarMass: Double): Double =
        pressurePa * molarMass / (R * temperatureK)

    fun airAt(altitudeM: Double): AirState {
        val t = temperatureK(altitudeM)
        val p = pressurePa(altitudeM)
        return AirState(altitudeM, t, p, density(p, t, M_AIR))
    }

    /** Стан біля землі за введеними температурою (°C) і тиском (гПа). */
    fun ground(temperatureC: Double, pressureHPa: Double): AirState {
        val t = temperatureC + 273.15
        val p = pressureHPa * 100.0
        return AirState(0.0, t, p, density(p, t, M_AIR))
    }
}

/** Результат розрахунку газу та підйомної сили. */
data class LiftResult(
    val gas: GasType,
    val envelopeVolume: Double,       // робочий об'єм оболонки, м³
    val ground: AirState,
    val target: AirState,
    val gasDensityGround: Double,
    val gasDensityTarget: Double,
    val fillVolumeGround: Double,     // скільки газу вдувати біля землі, м³
    val grossLiftGround: Double,      // кг
    val grossLiftTarget: Double,      // кг
    val envelopeMass: Double?,        // плівка + кріплення, кг
    val ballastMass: Double?,         // кг
    val payload: Double?,             // кг
    val cylinderOutput: Double?,      // м³ з одного балона за умов біля землі
    val cylinderCount: Double?,
)

object Lift {

    /**
     * Розрахунок для відкритої оболонки, що повністю наповнюється на розрахунковій висоті.
     * Маса газу стала; біля землі оболонка наповнена частково.
     */
    fun calculate(
        gas: GasType,
        envelopeVolume: Double,
        groundTempC: Double,
        groundPressureHPa: Double,
        targetAltitudeM: Double,
        ballastPercent: Double,
        envelopeMass: Double?,
        cylinderVolumeL: Double?,
        cylinderPressureAtm: Double?,
        cylinderResidualAtm: Double?,
    ): LiftResult {
        val ground = Atmosphere.ground(groundTempC, groundPressureHPa)
        val target = Atmosphere.airAt(targetAltitudeM)

        val gasGround = Atmosphere.density(ground.pressurePa, ground.temperatureK, gas.molarMass)
        val gasTarget = Atmosphere.density(target.pressurePa, target.temperatureK, gas.molarMass)

        // Маса газу стала: V_земля × ρ_земля = V_висота × ρ_висота
        val fillGround = envelopeVolume * gasTarget / gasGround
        val liftGround = fillGround * (ground.density - gasGround)
        val liftTarget = envelopeVolume * (target.density - gasTarget)

        val ballast = liftTarget * ballastPercent / 100.0
        val payload = if (envelopeMass != null) liftTarget - ballast - envelopeMass else null

        val perCylinder = if (cylinderVolumeL != null && cylinderPressureAtm != null) {
            val usableAtm = (cylinderPressureAtm - (cylinderResidualAtm ?: 0.0)).coerceAtLeast(0.0)
            // Ізотермічне розширення до тиску біля землі
            cylinderVolumeL / 1000.0 * usableAtm * 101325.0 / ground.pressurePa
        } else null
        val cylinders = if (perCylinder != null && perCylinder > 0) fillGround / perCylinder else null

        return LiftResult(
            gas = gas, envelopeVolume = envelopeVolume, ground = ground, target = target,
            gasDensityGround = gasGround, gasDensityTarget = gasTarget,
            fillVolumeGround = fillGround, grossLiftGround = liftGround, grossLiftTarget = liftTarget,
            envelopeMass = envelopeMass, ballastMass = ballast, payload = payload,
            cylinderOutput = perCylinder, cylinderCount = cylinders,
        )
    }

    val assumptions = listOf(
        "Підйомна сила за законом Архімеда: F = V × (ρ_повітря − ρ_газу); тут подано як масу в кг.",
        "Гази вважаються ідеальними: ρ = p·M/(R·T); домішки повітря й вологи не враховано.",
        "Атмосфера на висоті — за моделлю МСА; реальна погода відрізняється, тому результат орієнтовний.",
        "Оболонка вважається відкритою: газ розширюється до тиску довкілля, температура газу дорівнює температурі повітря.",
        "Маса газу стала: біля землі оболонка наповнена частково, повністю — на розрахунковій висоті.",
        "Вихід з балона — ізотермічне розширення до тиску біля землі; реальний вихід залежить від охолодження балона.",
        "Баласт задано у відсотках від підйомної маси на висоті; алгоритми скидання баласту й керування польотом не реалізовано.",
    )
}
