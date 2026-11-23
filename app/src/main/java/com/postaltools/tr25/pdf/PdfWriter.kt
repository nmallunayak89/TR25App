package com.postaltools.tr25.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlin.math.max

/**
 * A small top-to-bottom layout engine on top of Android's PdfDocument.
 * Every block measures itself first, so text wraps inside its box and a block that
 * does not fit moves to the next page (tables repeat their header there).
 * Units are PDF points; A4 = 595 x 842.
 */
class PdfWriter(
    private val doc: PdfDocument,
    private val totalPages: Int,
    private val continuationTitle: String,
) {
    val pageW = 595
    val pageH = 842
    val left = 36f
    val right = 559f
    private val top = 32f
    private val bottom = 800f
    val width get() = right - left

    private var page: PdfDocument.Page? = null
    private lateinit var canvas: Canvas
    var y = top
        private set
    var pageNo = 0
        private set

    // Palatino-style face (close to Book Antiqua); falls back to the phone's serif font.
    private val regular: Typeface = Fonts.regular ?: Typeface.create(Typeface.SERIF, Typeface.NORMAL)
    private val boldFace: Typeface = Fonts.bold ?: Typeface.create(Typeface.SERIF, Typeface.BOLD)
    private val italicFace: Typeface = Fonts.italic ?: Typeface.create(Typeface.SERIF, Typeface.ITALIC)

    private val line = Paint().apply {
        color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 0.6f; isAntiAlias = true
    }
    private val faint = Paint().apply {
        color = Color.rgb(120, 120, 120); style = Paint.Style.STROKE; strokeWidth = 0.4f
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(1.2f, 1.6f), 0f)
    }

    fun paint(size: Float, bold: Boolean = false, italic: Boolean = false) = TextPaint().apply {
        isAntiAlias = true
        color = Color.BLACK
        textSize = size
        typeface = when { bold -> boldFace; italic -> italicFace; else -> regular }
    }

    fun measure(text: String, p: TextPaint, w: Float, align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL): StaticLayout {
        val t = text.ifEmpty { " " }
        return StaticLayout.Builder.obtain(t, 0, t.length, p, max(1, w.toInt()))
            .setAlignment(align)
            .setIncludePad(false)
            .setLineSpacing(0.5f, 1f)
            .build()
    }

    private fun draw(l: StaticLayout, x: Float, yy: Float) {
        canvas.save(); canvas.translate(x, yy); l.draw(canvas); canvas.restore()
    }

    // ---------------- pages ----------------

    fun newPage() {
        finishPage()
        pageNo++
        val p = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create())
        page = p
        canvas = p.canvas
        y = top
        val footer = if (totalPages > 0) "Page $pageNo of $totalPages" else "Page $pageNo"
        draw(measure(footer, paint(7f), width, Layout.Alignment.ALIGN_CENTER), left, pageH - 26f)
        if (pageNo > 1) {
            val l = measure(continuationTitle, paint(7.5f, italic = true), width)
            draw(l, left, y); y += l.height + 2f
            canvas.drawLine(left, y, right, y, line); y += 6f
        }
    }

    fun finishPage() {
        page?.let { doc.finishPage(it) }
        page = null
    }

    /** Starts a new page if [h] points will not fit on the current one. */
    fun ensure(h: Float) {
        if (page == null || y + h > bottom) newPage()
    }

    fun gap(h: Float) { y += h }

    fun rule(thick: Boolean = false) {
        ensure(4f)
        val p = if (thick) Paint(line).apply { strokeWidth = 1.1f } else line
        canvas.drawLine(left, y, right, y, p); y += 5f
    }

    // ---------------- text blocks ----------------

    fun paragraph(
        text: String, size: Float = 8.5f, bold: Boolean = false, italic: Boolean = false,
        align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL, indent: Float = 0f, after: Float = 3f,
        keepWithNext: Float = 0f,
    ) {
        val l = measure(text, paint(size, bold, italic), width - indent, align)
        // Bold item headings keep at least [keepWithNext] points of what follows on the same page.
        ensure(l.height.toFloat() + if (bold) max(keepWithNext, 22f) else keepWithNext)
        draw(l, left + indent, y)
        y += l.height + after
    }

    /** "Label : value" with the value in bold on a dotted fill line, like a filled-in form. */
    fun field(label: String, value: String, labelWidth: Float = 150f, indent: Float = 0f, size: Float = 8.5f) {
        val lw = labelWidth
        val vx = left + indent + lw + 6f
        val vw = right - vx
        val lLabel = measure(label, paint(size), lw)
        val lVal = measure(value, paint(size, bold = true), vw)
        val h = max(lLabel.height, lVal.height).toFloat()
        ensure(h + 4f)
        draw(lLabel, left + indent, y)
        draw(lVal, vx + 2f, y + h - lVal.height) // value sits on the fill line, level with the label's last line
        val ly = y + h + 1.2f
        canvas.drawLine(vx, ly, right, ly, faint)
        y += h + 5f
    }

    /** Part B style line: description on the left, "Rs. amount" in a right-hand column. */
    fun amountLine(label: String, amount: String?, indent: Float = 0f, bold: Boolean = false, size: Float = 8.5f) {
        val amtW = 110f
        val lw = width - indent - amtW - 8f
        val lLabel = measure(label, paint(size, bold), lw)
        val amtText = amount?.let { "Rs. $it" } ?: ""
        val lAmt = measure(amtText, paint(size, bold = true), amtW, Layout.Alignment.ALIGN_OPPOSITE)
        val h = max(lLabel.height, lAmt.height).toFloat()
        ensure(h + 4f)
        draw(lLabel, left + indent, y)
        if (amount != null) {
            draw(lAmt, right - amtW, y + h - lAmt.height)
            canvas.drawLine(right - amtW, y + h + 1.2f, right, y + h + 1.2f, faint)
        }
        y += h + 5f
    }

    /** Two captions placed apart, with blank space above for a signature. */
    fun signatureRow(leftText: String, rightText: String, space: Float = 30f, size: Float = 8.5f) {
        val half = width / 2f - 10f
        val l1 = measure(leftText, paint(size, bold = true), half)
        val l2 = measure(rightText, paint(size, bold = true), half, Layout.Alignment.ALIGN_OPPOSITE)
        val h = max(l1.height, l2.height).toFloat()
        ensure(space + h)
        y += space
        draw(l1, left, y)
        draw(l2, right - half, y)
        y += h + 4f
    }

    /** Puts text at the right edge of the current line without moving down (e.g. "Sub-Bill No."). */
    fun rightText(text: String, size: Float = 8.5f, bold: Boolean = false) {
        ensure(12f)
        val l = measure(text, paint(size, bold), width, Layout.Alignment.ALIGN_OPPOSITE)
        draw(l, left, y)
    }

    // ---------------- tables ----------------

    data class Col(
        val title: String,
        val weight: Float,
        val align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        val group: String? = null,
        val num: String? = null,
    )

    fun table(cols: List<Col>, rows: List<List<String>>, size: Float = 7.5f) {
        val pad = 2.5f
        val total = cols.sumOf { it.weight.toDouble() }.toFloat()
        val ws = cols.map { it.weight / total * width }
        val xs = ws.runningFold(left) { acc, w -> acc + w }.dropLast(1)
        val hp = paint(size - 0.5f, bold = true)
        val bp = paint(size)
        val np = paint(6.5f)
        val center = Layout.Alignment.ALIGN_CENTER

        // ---- header measurement ----
        val hasGroups = cols.any { it.group != null }
        val titleL = cols.mapIndexed { i, c -> measure(c.title, hp, ws[i] - 2 * pad, center) }
        var h1 = 0f
        if (hasGroups) {
            var i = 0
            while (i < cols.size) {
                val g = cols[i].group
                if (g == null) { i++; continue }
                var j = i
                while (j + 1 < cols.size && cols[j + 1].group == g) j++
                val gw = (i..j).sumOf { ws[it].toDouble() }.toFloat()
                h1 = max(h1, measure(g, hp, gw - 2 * pad, center).height + 2 * pad)
                i = j + 1
            }
        }
        val h2 = cols.indices.filter { cols[it].group != null }
            .maxOfOrNull { titleL[it].height + 2 * pad } ?: 0f
        val hUngrouped = cols.indices.filter { cols[it].group == null }
            .maxOfOrNull { titleL[it].height + 2 * pad } ?: 0f
        val headerH = max(h1 + h2, hUngrouped)
        val subH = headerH - h1
        val numH = if (cols.any { it.num != null }) 10f else 0f

        fun drawHeader() {
            val y0 = y
            var i = 0
            while (i < cols.size) {
                val g = cols[i].group
                if (g == null) {
                    canvas.drawRect(xs[i], y0, xs[i] + ws[i], y0 + headerH, line)
                    val l = titleL[i]
                    draw(l, xs[i] + pad, y0 + (headerH - l.height) / 2f)
                    i++
                } else {
                    var j = i
                    while (j + 1 < cols.size && cols[j + 1].group == g) j++
                    val gx = xs[i]; val gw = xs[j] + ws[j] - gx
                    canvas.drawRect(gx, y0, gx + gw, y0 + h1, line)
                    val gl = measure(g, hp, gw - 2 * pad, center)
                    draw(gl, gx + pad, y0 + (h1 - gl.height) / 2f)
                    for (k in i..j) {
                        canvas.drawRect(xs[k], y0 + h1, xs[k] + ws[k], y0 + headerH, line)
                        val l = titleL[k]
                        draw(l, xs[k] + pad, y0 + h1 + (subH - l.height) / 2f)
                    }
                    i = j + 1
                }
            }
            y = y0 + headerH
            if (numH > 0f) {
                cols.forEachIndexed { k, c ->
                    canvas.drawRect(xs[k], y, xs[k] + ws[k], y + numH, line)
                    val l = measure(c.num ?: "", np, ws[k] - 2 * pad, center)
                    draw(l, xs[k] + pad, y + (numH - l.height) / 2f)
                }
                y += numH
            }
        }

        val bodyRows = rows.ifEmpty { listOf(List(cols.size) { "" }) }
        val measured = bodyRows.map { r ->
            val ls = cols.indices.map { k -> measure(r.getOrElse(k) { "" }, bp, ws[k] - 2 * pad, cols[k].align) }
            ls to max(14f, (ls.maxOf { it.height } + 2 * pad))
        }

        ensure(headerH + numH + measured.first().second)
        drawHeader()
        for ((ls, rh) in measured) {
            if (y + rh > bottom) {
                newPage()
                drawHeader()
            }
            ls.forEachIndexed { k, l ->
                canvas.drawRect(xs[k], y, xs[k] + ws[k], y + rh, line)
                draw(l, xs[k] + pad, y + pad)
            }
            y += rh
        }
        y += 5f
    }
}
