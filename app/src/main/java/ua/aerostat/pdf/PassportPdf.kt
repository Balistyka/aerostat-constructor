package ua.aerostat.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import ua.aerostat.core.ReportSection
import java.io.OutputStream

/** PDF-паспорт оболонки українською (A4, системний шрифт з кирилицею). */
object PassportPdf {
    private const val PAGE_W = 595 // A4, пункти
    private const val PAGE_H = 842
    private const val MARGIN = 48f
    private const val FOOTER_H = 36f

    fun write(context: Context, uri: Uri, projectName: String, sections: List<ReportSection>) {
        val out = context.contentResolver.openOutputStream(uri) ?: error("Немає доступу до файлу")
        out.use { write(it, projectName, sections) }
    }

    fun write(out: OutputStream, projectName: String, sections: List<ReportSection>) {
        val doc = PdfDocument()
        val w = Writer(doc, projectName)
        try {
            w.title("Паспорт оболонки", projectName)
            sections.forEach { w.section(it) }
            w.finish()
            doc.writeTo(out)
        } finally {
            doc.close()
        }
    }

    private class Writer(private val doc: PdfDocument, private val projectName: String) {
        private val accent = Color.rgb(43, 125, 89)
        private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(16, 24, 32); textSize = 10.5f; typeface = Typeface.SANS_SERIF }
        private val muted = Paint(text).apply { color = Color.rgb(85, 99, 106) }
        private val bold = Paint(text).apply { typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
        private val h1 = Paint(bold).apply { textSize = 22f }
        private val h2 = Paint(bold).apply { textSize = 13f; color = accent }
        private val small = Paint(muted).apply { textSize = 8.5f }
        private val line = Paint().apply { color = Color.rgb(205, 215, 211); strokeWidth = 0.8f }

        private var pageNo = 0
        private var page: PdfDocument.Page? = null
        private lateinit var canvas: Canvas
        private var y = 0f
        private val contentW = PAGE_W - 2 * MARGIN

        init { newPage() }

        private fun newPage() {
            page?.let { finishPage(it) }
            pageNo++
            val p = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            page = p
            canvas = p.canvas
            y = MARGIN
            canvas.drawText("Аеростат Конструктор · Паспорт оболонки", MARGIN, y, small)
            y += 18f
        }

        private fun finishPage(p: PdfDocument.Page) {
            val c = p.canvas
            val fy = PAGE_H - MARGIN + 12f
            c.drawLine(MARGIN, fy - 14f, PAGE_W - MARGIN, fy - 14f, line)
            c.drawText("Розрахункова оцінка. Не підтверджує придатність до польоту чи безпечність.", MARGIN, fy, small)
            val label = "Сторінка $pageNo"
            c.drawText(label, PAGE_W - MARGIN - small.measureText(label), fy, small)
            doc.finishPage(p)
        }

        fun finish() {
            page?.let { finishPage(it) }
            page = null
        }

        private fun ensure(h: Float) {
            if (y + h > PAGE_H - MARGIN - FOOTER_H) newPage()
        }

        fun title(t: String, sub: String) {
            ensure(60f)
            y += 16f
            canvas.drawText(t, MARGIN, y, h1)
            y += 20f
            wrap(sub, contentW, bold).forEach { canvas.drawText(it, MARGIN, y, bold); y += 15f }
            canvas.drawRect(MARGIN, y, MARGIN + 56f, y + 3f, Paint().apply { color = accent })
            y += 18f
        }

        fun section(s: ReportSection) {
            ensure(40f)
            y += 8f
            canvas.drawText(s.title, MARGIN, y, h2)
            y += 8f
            canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, line)
            y += 14f
            val labelW = contentW * 0.48f
            val valueX = MARGIN + labelW + 12f
            val valueW = contentW - labelW - 12f
            s.rows.forEach { (k, v) ->
                val kl = wrap(k, labelW, muted)
                val vl = wrap(v, valueW, text)
                val n = maxOf(kl.size, vl.size)
                ensure(n * 14f + 4f)
                for (i in 0 until n) {
                    kl.getOrNull(i)?.let { canvas.drawText(it, MARGIN, y, muted) }
                    vl.getOrNull(i)?.let { canvas.drawText(it, valueX, y, text) }
                    y += 14f
                }
                y += 3f
            }
            s.paragraphs.forEach { p ->
                val lines = wrap("• $p", contentW, text)
                lines.forEach { ensure(14f); canvas.drawText(it, MARGIN, y, text); y += 14f }
                y += 4f
            }
            s.table?.let { t ->
                val colW = contentW / t.headers.size
                ensure(30f)
                t.headers.forEachIndexed { i, h -> canvas.drawText(h, MARGIN + i * colW, y, bold) }
                y += 6f
                canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, line)
                y += 12f
                t.rows.forEach { r ->
                    ensure(14f)
                    r.forEachIndexed { i, c -> canvas.drawText(c, MARGIN + i * colW, y, text) }
                    y += 14f
                }
            }
            y += 6f
        }

        private fun wrap(s: String, width: Float, paint: Paint): List<String> {
            val out = mutableListOf<String>()
            s.split('\n').forEach { para ->
                var cur = ""
                para.split(' ').forEach { word ->
                    val cand = if (cur.isEmpty()) word else "$cur $word"
                    if (paint.measureText(cand) <= width) {
                        cur = cand
                    } else {
                        if (cur.isNotEmpty()) out += cur
                        // Дуже довге слово — розбиваємо посимвольно.
                        var wpart = word
                        while (paint.measureText(wpart) > width && wpart.length > 1) {
                            var cut = wpart.length - 1
                            while (cut > 1 && paint.measureText(wpart.substring(0, cut)) > width) cut--
                            out += wpart.substring(0, cut)
                            wpart = wpart.substring(cut)
                        }
                        cur = wpart
                    }
                }
                out += cur
            }
            return out
        }
    }
}
