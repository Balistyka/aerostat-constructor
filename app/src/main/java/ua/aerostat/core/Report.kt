package ua.aerostat.core

/** Розділ паспорта оболонки (спільний для PDF і копіювання тексту). */
data class ReportSection(
    val title: String,
    val rows: List<Pair<String, String>> = emptyList(),
    val paragraphs: List<String> = emptyList(),
    val table: ReportTable? = null,
)

data class ReportTable(val headers: List<String>, val rows: List<List<String>>)

object Report {
    const val DISCLAIMER =
        "Документ містить лише розрахункові оцінки геометрії та маси матеріалу. " +
            "Він не підтверджує придатність оболонки до польоту, її міцність чи безпечність конструкції."

    fun inputRows(input: ProjectInput): List<Pair<String, String>> {
        val rows = mutableListOf(
            "Тип оболонки" to input.shape.title,
            "Варіант" to input.variant.title,
            "Матеріал" to input.material.title,
        )
        for (f in Calculator.relevantFields(input)) {
            val raw = Calculator.rawValue(input, f).trim()
            val shown = when {
                raw.isEmpty() && f == Field.GORE_COUNT -> "автоматично"
                raw.isEmpty() -> "—"
                else -> when (val p = Numbers.parse(raw)) {
                    is ParsedNumber.Value -> Numbers.toInput(p.value, 4).let { "$it ${f.unit}" }
                    else -> raw
                }
            }
            rows += f.label to shown
        }
        return rows
    }

    fun sections(project: Project, result: CalcResult, dateText: String): List<ReportSection> {
        val list = mutableListOf<ReportSection>()
        list += ReportSection(
            "Проєкт",
            rows = listOf("Назва" to project.name, "Дата" to dateText, "Статус моделі" to result.status.title),
        )
        list += ReportSection("Вихідні параметри", rows = inputRows(project.input))

        if (result.unavailableReason != null) {
            list += ReportSection("Геометрія", paragraphs = listOf(result.unavailableReason))
        } else {
            list += ReportSection("Геометрія", rows = result.geometry.map { it.label to it.formatted })
            val massRows = mutableListOf<Pair<String, String>>()
            result.blankArea?.let { massRows += "Площа заготовки" to "${Numbers.format(it)} м²" }
            result.filmMass?.let { massRows += "Маса плівки" to "${Numbers.format(it, 3)} кг" }
            result.fittingsMass?.let { massRows += "Маса кріплень" to "${Numbers.format(it, 3)} кг" }
            result.totalMass?.let { massRows += "Маса оболонки разом" to "${Numbers.format(it, 3)} кг" }
            if (massRows.isEmpty()) massRows += "Маса" to "не обчислено — перевірте вихідні параметри"
            list += ReportSection("Площа і маса", rows = massRows)
            result.lift?.let { l ->
                val rows = mutableListOf(
                    "Підйомний газ" to l.gas.title,
                    "Робочий об'єм оболонки" to "${Numbers.format(l.envelopeVolume)} м³",
                    "Розрахункова висота" to "${Numbers.format(l.target.altitude / 1000.0)} км",
                    "Температура на висоті" to "${Numbers.format(l.target.temperatureK - 273.15, 1)} °C",
                    "Тиск на висоті" to "${Numbers.format(l.target.pressurePa / 100.0, 0)} гПа",
                    "Густина повітря на висоті" to "${Numbers.format(l.target.density, 3)} кг/м³",
                    "Густина газу на висоті" to "${Numbers.format(l.gasDensityTarget, 4)} кг/м³",
                    "Підйомна маса на висоті" to "${Numbers.format(l.grossLiftTarget)} кг",
                    "Підйомна маса біля землі" to "${Numbers.format(l.grossLiftGround)} кг",
                    "Газу вдути біля землі" to "${Numbers.format(l.fillVolumeGround)} м³",
                )
                l.cylinderOutput?.let { rows += "Вихід з одного балона" to "${Numbers.format(it)} м³" }
                l.cylinderCount?.let { rows += "Потрібно балонів" to Numbers.format(it) }
                l.ballastMass?.let { rows += "Баласт" to "${Numbers.format(it)} кг" }
                l.payload?.let { rows += "Корисний вантаж" to "${Numbers.format(it)} кг" }
                list += ReportSection("Газ, підйомна маса і вантаж", rows = rows)
            }
            if (result.layout.isNotEmpty()) {
                list += ReportSection("Розкрій", rows = result.layout.map { it.label to it.formatted })
            }
            result.gores?.let { g ->
                list += ReportSection(
                    "Викрійка клина (N = ${g.count})",
                    paragraphs = listOf("Відстань — від полюса вздовж осі клина; ширина — повна ширина клина без припусків."),
                    table = ReportTable(
                        headers = listOf("Відстань, м", "Ширина, м"),
                        rows = g.stations.map { listOf(Numbers.format(it.distance, 3), Numbers.format(it.width, 3)) },
                    ),
                )
            }
        }
        if (result.errors.isNotEmpty()) {
            list += ReportSection(
                "Помилки вводу",
                rows = result.errors.map { (f, e) -> f.label to e },
            )
        }
        list += ReportSection("Припущення", paragraphs = result.assumptions)
        if (project.input.notes.isNotBlank()) {
            list += ReportSection("Примітки", paragraphs = listOf(project.input.notes.trim()))
        }
        list += ReportSection("Застереження", paragraphs = listOf(result.status.explanation, DISCLAIMER))
        return list
    }

    /** Текст для копіювання в буфер обміну. */
    fun plainText(project: Project, result: CalcResult, dateText: String): String = buildString {
        appendLine("Аеростат Конструктор — паспорт оболонки")
        for (s in sections(project, result, dateText)) {
            appendLine()
            appendLine(s.title.uppercase())
            s.rows.forEach { (k, v) -> appendLine("$k: $v") }
            s.table?.let { t ->
                appendLine(t.headers.joinToString(" | "))
                t.rows.forEach { appendLine(it.joinToString(" | ")) }
            }
            s.paragraphs.forEach { appendLine("• $it") }
        }
    }.trimEnd()
}
