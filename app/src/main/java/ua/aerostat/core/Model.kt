package ua.aerostat.core

import kotlinx.serialization.Serializable

/** Тип оболонки. */
enum class ShapeType(val title: String, val description: String, val status: ModelStatus) {
    GATHERED(
        "Конверт, зібраний знизу",
        "Прямокутний рукав (конверт): зверху заварений, знизу зібраний у горловину. Площа й маса — точно, об'єм — лише верхня оцінка.",
        ModelStatus.APPROXIMATE,
    ),
    SPHERE(
        "Сферична оболонка",
        "Куля з клинів (гор). Ідеальна сфера: площа, об'єм, розкрій клинів.",
        ModelStatus.VALIDATED,
    ),
    CYLINDER(
        "Циліндрична оболонка",
        "Бічна поверхня з полотен і два плоскі круглі днища.",
        ModelStatus.VALIDATED,
    ),
    SLEEVE(
        "Поліетиленовий рукав",
        "Готовий рукав із рулону, заварений з кінців. Об'єм — лише наближена оцінка.",
        ModelStatus.APPROXIMATE,
    ),
    PEAR(
        "Грушоподібна оболонка",
        "Геометричну модель ще не перевірено. Розрахунок поки недоступний.",
        ModelStatus.UNAVAILABLE,
    ),
}

/** Статус валідації фізичної/геометричної моделі. */
enum class ModelStatus(val title: String, val explanation: String) {
    VALIDATED(
        "Перевірена геометрична модель",
        "Стандартні формули ідеальної геометричної форми. Реальна наповнена оболонка може відрізнятися.",
    ),
    APPROXIMATE(
        "Наближена модель",
        "Оцінка з явно вказаними припущеннями. Не використовуйте як точне значення.",
    ),
    UNAVAILABLE(
        "Розрахунок поки недоступний",
        "Для цієї конструкції немає перевіреної геометричної моделі.",
    ),
}

/** Варіант оболонки щодо газу. */
enum class EnvelopeVariant(val title: String, val description: String) {
    CLOSED("Закрита оболонка", "Герметична оболонка без отвору."),
    OPEN_APPENDIX(
        "Відкрита з рукавом",
        "Оболонка з рукавом (апендиксом) для пасивного відведення надлишку газу.",
    ),
}

/** Пресети матеріалу плівки. Густини орієнтовні — перевіряйте паспорт матеріалу. */
enum class FilmMaterial(val title: String, val density: Double?) {
    LDPE("LDPE (ПЕВТ)", 920.0),
    LLDPE("LLDPE (ЛПЕНТ)", 925.0),
    HDPE("HDPE (ПЕНТ)", 950.0),
    CUSTOM("Власний матеріал", null),
}

/** Типові газові балони. Значення орієнтовні — за потреби вводьте свої. */
enum class CylinderPreset(val title: String, val volumeL: Double?, val pressureAtm: Double?) {
    STANDARD_9("9 м³ · 200 атм", 45.0, 200.0),
    L40_150("40 л · 150 атм", 40.0, 150.0),
    L40_200("40 л · 200 атм", 40.0, 200.0),
    L50_150("50 л · 150 атм", 50.0, 150.0),
    L50_200("50 л · 200 атм", 50.0, 200.0),
    CUSTOM("Власний балон", null, null),
}

/**
 * Вихідні дані проєкту. Числові поля зберігаються так, як їх ввів користувач
 * (із комою), щоб відновлення проєкту повертало точно введений текст.
 */
@Serializable
data class ProjectInput(
    val shape: ShapeType = ShapeType.SPHERE,
    val variant: EnvelopeVariant = EnvelopeVariant.CLOSED,
    // Геометрія
    val diameter: String = "3",
    val cylinderLength: String = "6",
    val sleeveFlatWidth: String = "1,5",
    val sleeveLength: String = "8",
    val goreCount: String = "",
    val envelopeWidth: String = "6",
    val envelopeHeight: String = "10",
    val gatherPart: String = "1,1",
    val appendixDiameter: String = "0,3",
    val appendixLength: String = "1",
    // Матеріал
    val material: FilmMaterial = FilmMaterial.LDPE,
    val density: String = "920",
    val thicknessUm: String = "30",
    val layers: String = "1",
    val wastePercent: String = "10",
    val seamCm: String = "2",
    val rollWidth: String = "1,5",
    val fittingsKg: String = "0,2",
    // Газ і підйом
    val gas: GasType = GasType.HYDROGEN,
    val fillFactor: String = "100",
    val groundTemp: String = "15",
    val groundPressure: String = "1013",
    val targetAltitude: String = "10",
    val ballastPercent: String = "12",
    val cylinder: CylinderPreset = CylinderPreset.STANDARD_9,
    val cylinderVolume: String = "45",
    val cylinderPressure: String = "200",
    val cylinderResidual: String = "3",
    val notes: String = "",
) {
    /** Скидає значення до типових для обраного типу, зберігаючи тип і варіант. */
    fun resetValues(): ProjectInput = ProjectInput(shape = shape, variant = variant)
}

@Serializable
data class Project(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val input: ProjectInput = ProjectInput(),
)

/** Ключі полів (для помилок і довідок). */
enum class Field(val label: String, val unit: String) {
    DIAMETER("Діаметр", "м"),
    CYLINDER_LENGTH("Довжина циліндра", "м"),
    SLEEVE_FLAT_WIDTH("Ширина рукава у складеному вигляді", "м"),
    SLEEVE_LENGTH("Довжина рукава", "м"),
    ENVELOPE_WIDTH("Ширина конверта у складеному вигляді", "м"),
    ENVELOPE_HEIGHT("Висота конверта", "м"),
    GATHER_PART("Частина під збір знизу", "м"),
    GORE_COUNT("Кількість клинів", "шт."),
    APPENDIX_DIAMETER("Діаметр рукава-апендикса", "м"),
    APPENDIX_LENGTH("Довжина рукава-апендикса", "м"),
    DENSITY("Густина матеріалу", "кг/м³"),
    THICKNESS("Товщина плівки", "мкм"),
    LAYERS("Кількість шарів", "шт."),
    WASTE("Припуски та відходи", "%"),
    SEAM("Припуск на шов", "см"),
    ROLL_WIDTH("Ширина рулону", "м"),
    FITTINGS("Маса кріплень", "кг"),
    FILL_FACTOR("Коефіцієнт наповнення", "%"),
    GROUND_TEMP("Температура біля землі", "°C"),
    GROUND_PRESSURE("Тиск біля землі", "гПа"),
    TARGET_ALTITUDE("Розрахункова висота", "км"),
    BALLAST_PERCENT("Баласт від підйомної маси", "%"),
    CYLINDER_VOLUME("Об'єм балона", "л"),
    CYLINDER_PRESSURE("Тиск у балоні", "атм"),
    CYLINDER_RESIDUAL("Залишковий тиск у балоні", "атм"),
    TARGET_PAYLOAD("Потрібний корисний вантаж", "кг"),
    FLIGHT_HOURS("Тривалість польоту", "год"),
    LIFT_LOSS_RATE("Втрата підйому за добу", "%"),
    ASPECT_RATIO("Довжина відносно діаметра", "раз"),
}
