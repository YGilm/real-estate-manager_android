package com.example.my_project.ui.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import com.example.my_project.data.model.Transaction
import com.example.my_project.data.model.TxType
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object ReportPdfGenerator {

    private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val createdFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale("ru"))

    // A4 at ~72dpi
    private const val PAGE_W = 595
    private const val PAGE_H = 842

    // Theme
    private val PRIMARY = Color.parseColor("#1E5AA8")
    private val PRIMARY_DARK = Color.parseColor("#16457F")
    private val INCOME = Color.parseColor("#1B8E4B")
    private val EXPENSE = Color.parseColor("#C62828")
    private val NEUTRAL_BG = Color.parseColor("#F7F8FA")
    private val BORDER = Color.parseColor("#DADDE2")
    private val TEXT_MUTED = Color.parseColor("#5B6775")
    private val ROW_ALT = Color.parseColor("#F3F5F8")

    /**
     * Красивый PDF-отчёт по выбранному периоду.
     * transactionsInPeriod должны быть уже отфильтрованы по датам (и по includeFuture, если нужно).
     */
    fun createPeriodPdfReport(
        context: Context,
        propertyName: String,
        from: LocalDate,
        to: LocalDate,
        transactionsInPeriod: List<Transaction>,
        totals: Totals,
        avgNetPerMonth: Double,
        includeFuture: Boolean = false
    ): File {
        val safeFrom = if (from.isAfter(to)) to else from
        val safeTo = if (from.isAfter(to)) from else to

        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val fileName = "report_${safeFrom}_$safeTo.pdf"
        val outFile = File(reportsDir, fileName)

        val doc = PdfDocument()

        val margin = 32f
        val left = margin
        val right = PAGE_W - margin
        val width = right - left

        // Typography
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            isFakeBoldText = true
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 11f
        }
        val h1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 14f
            isFakeBoldText = true
        }
        val h2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 12f
            isFakeBoldText = true
        }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10.5f
        }
        val bodyMuted = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TEXT_MUTED
            textSize = 10.0f
        }
        val small = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TEXT_MUTED
            textSize = 9.2f
        }
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = BORDER
            strokeWidth = 1f
        }
        val fillNeutral = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = NEUTRAL_BG
            style = Paint.Style.FILL
        }
        val fillAlt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ROW_ALT
            style = Paint.Style.FILL
        }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = BORDER
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        val primaryFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PRIMARY
            style = Paint.Style.FILL
        }
        val primaryDarkFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PRIMARY_DARK
            style = Paint.Style.FILL
        }

        // Table column widths (detail table)
        val colDateW = 78f
        val colTypeW = 62f
        val colAmountW = 92f
        val colGap = 10f
        val colNoteW = width - colDateW - colTypeW - colAmountW - (colGap * 3)

        // Sort + group
        val txSorted = transactionsInPeriod.sortedWith(
            compareByDescending<Transaction> { it.date }.thenByDescending { it.amount }
        )

        val groups: List<YearMonth> = txSorted
            .map { YearMonth.from(it.date) }
            .distinct()
            .sortedDescending()

        val txByMonth: Map<YearMonth, List<Transaction>> = groups.associateWith { ym ->
            txSorted.filter { YearMonth.from(it.date) == ym }
        }

        data class MonthSum(val ym: YearMonth, val income: Double, val expense: Double) {
            val net: Double get() = income - expense
        }

        val monthSums = groups.map { ym ->
            var inc = 0.0
            var exp = 0.0
            for (t in txByMonth[ym].orEmpty()) {
                if (t.type == TxType.INCOME) inc += t.amount else exp += t.amount
            }
            MonthSum(ym, inc, exp)
        }

        // Page context
        class PageCtx(var pageNumber: Int) {
            var page: PdfDocument.Page =
                doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create())
            var y: Float = margin
            val canvas get() = page.canvas
        }

        fun finishPage(ctx: PageCtx) {
            // Footer
            val footerY = PAGE_H - margin + 6f

            // separator
            ctx.canvas.drawLine(left, PAGE_H - margin - 10f, right, PAGE_H - margin - 10f, line)

            val created = "Сформировано: ${LocalDate.now().format(createdFmt)}"
            ctx.canvas.drawText(created, left, footerY, small)

            val pageText = "Стр. ${ctx.pageNumber}"
            val w = small.measureText(pageText)
            ctx.canvas.drawText(pageText, right - w, footerY, small)

            doc.finishPage(ctx.page)
        }

        /**
         * Новая страница БЕЗ синей шапки (после первой).
         * Оставляем только маленькую строку (для ориентира) без заливки.
         */
        fun newPagePlainHeader(ctx: PageCtx): PageCtx {
            finishPage(ctx)
            val next = PageCtx(ctx.pageNumber + 1)

            val headerText = "$propertyName • ${safeFrom.format(dateFmt)} — ${safeTo.format(dateFmt)}"
            val headerY = margin
            next.canvas.drawText(headerText, left, headerY, small)
            next.canvas.drawLine(left, headerY + 8f, right, headerY + 8f, line)

            next.y = headerY + 18f
            return next
        }

        fun ensureSpace(ctx: PageCtx, need: Float): PageCtx {
            return if (ctx.y + need > PAGE_H - margin - 24f) {
                newPagePlainHeader(ctx)
            } else ctx
        }

        fun roundRect(ctx: PageCtx, rect: RectF, radius: Float, fill: Paint, strokePaint: Paint? = null) {
            ctx.canvas.drawRoundRect(rect, radius, radius, fill)
            strokePaint?.let { ctx.canvas.drawRoundRect(rect, radius, radius, it) }
        }

        fun drawTopHeader(ctx0: PageCtx): PageCtx {
            val ctx = ctx0

            // Big colored header (ONLY first page)
            val barH = 92f
            ctx.canvas.drawRect(left, margin - 6f, right, margin - 6f + barH, primaryFill)
            ctx.canvas.drawRect(left, margin - 6f + barH - 10f, right, margin - 6f + barH, primaryDarkFill)

            val title = "Отчёт по доходам и расходам"
            ctx.canvas.drawText(title, left + 16f, margin - 6f + 28f, titlePaint)

            val sub1 = "Объект: $propertyName"
            val sub2 = "Период: ${safeFrom.format(dateFmt)} — ${safeTo.format(dateFmt)}"
            ctx.canvas.drawText(sub1, left + 16f, margin - 6f + 52f, subtitlePaint)
            ctx.canvas.drawText(sub2, left + 16f, margin - 6f + 72f, subtitlePaint)

            val note = if (includeFuture) "Включая будущие транзакции" else "Будущие транзакции не учитываются"
            ctx.canvas.drawText(note, right - 16f - subtitlePaint.measureText(note), margin - 6f + 72f, subtitlePaint)

            ctx.y = margin - 6f + barH + 18f
            return ctx
        }

        fun drawSummaryCards(ctx0: PageCtx): PageCtx {
            var ctx = ctx0
            ctx = ensureSpace(ctx, 120f)

            ctx.canvas.drawText("Итоги", left, ctx.y + h1.textSize, h1)
            ctx.y += 18f

            val gap = 10f
            val cardH = 54f
            val cardW = (width - gap) / 2f

            fun card(
                x: Float,
                yTop: Float,
                title: String,
                value: String,
                accentColor: Int
            ) {
                val rect = RectF(x, yTop, x + cardW, yTop + cardH)
                roundRect(ctx, rect, 12f, fillNeutral, stroke)

                val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
                ctx.canvas.drawCircle(x + 14f, yTop + 18f, 5.2f, dot)

                ctx.canvas.drawText(title, x + 26f, yTop + 21f, bodyMuted)
                val vPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    textSize = 13f
                    isFakeBoldText = true
                }
                ctx.canvas.drawText(value, x + 14f, yTop + 43f, vPaint)
            }

            val incomeText = moneyFormatPlain(totals.income)
            val expenseText = moneyFormatPlain(totals.expense)
            val netText = moneyFormatPlain(totals.total)
            val avgText = moneyFormatPlain(avgNetPerMonth)

            val row1Y = ctx.y
            card(left, row1Y, "Доход", incomeText, INCOME)
            card(left + cardW + gap, row1Y, "Расход", expenseText, EXPENSE)

            val row2Y = row1Y + cardH + gap
            val netAccent = if (totals.total >= 0) INCOME else EXPENSE
            card(left, row2Y, "Реальная чистая выручка", netText, netAccent)
            card(left + cardW + gap, row2Y, "Средняя чистая выручка / мес", avgText, PRIMARY_DARK)

            ctx.y = row2Y + cardH + 18f
            return ctx
        }

        fun monthNameRu(ym: YearMonth): String {
            val ru = Locale("ru")
            val m = ym.month.getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, ru)
            val cap = m.replaceFirstChar { if (it.isLowerCase()) it.titlecase(ru) else it.toString() }
            return "$cap ${ym.year}"
        }

        fun drawMonthSummaryTable(ctx0: PageCtx): PageCtx {
            var ctx = ctx0
            ctx = ensureSpace(ctx, 140f)

            ctx.canvas.drawText("Сводка по месяцам", left, ctx.y + h1.textSize, h1)
            ctx.y += 22f // ↑ УВЕЛИЧИЛИ ОТСТУП ПОСЛЕ ЗАГОЛОВКА

            val tableTop = ctx.y
            val headerH = 26f
            val rowH = 24f

            val colMonthW = width * 0.40f
            val colIncW = width * 0.20f
            val colExpW = width * 0.20f
            val colNetW = width - colMonthW - colIncW - colExpW

            roundRect(ctx, RectF(left, tableTop, right, tableTop + headerH), 10f, fillNeutral, stroke)

            val hb = tableTop + 18f
            ctx.canvas.drawText("Месяц", left + 10f, hb, h2)
            ctx.canvas.drawText("Доход", left + colMonthW + 10f, hb, h2)
            ctx.canvas.drawText("Расход", left + colMonthW + colIncW + 10f, hb, h2)
            ctx.canvas.drawText("Итог", right - colNetW + 10f, hb, h2)

            ctx.y = tableTop + headerH

            if (monthSums.isEmpty()) {
                ctx = ensureSpace(ctx, 42f)
                ctx.canvas.drawText("Нет данных за период.", left, ctx.y + body.textSize, bodyMuted)
                ctx.y += 26f
                return ctx
            }

            fun drawRight(text: String, xRight: Float, paint: Paint, base: Float) {
                val w = paint.measureText(text)
                ctx.canvas.drawText(text, xRight - 10f - w, base, paint)
            }

            monthSums.forEachIndexed { idx, ms ->
                ctx = ensureSpace(ctx, rowH + 6f)

                val yTop = ctx.y
                if (idx % 2 == 1) {
                    ctx.canvas.drawRect(RectF(left, yTop, right, yTop + rowH), fillAlt)
                }
                ctx.canvas.drawLine(left, yTop + rowH, right, yTop + rowH, line)

                val base = yTop + 16.5f
                ctx.canvas.drawText(monthNameRu(ms.ym), left + 10f, base, body)

                val inc = moneyFormatPlain(ms.income)
                val exp = moneyFormatPlain(ms.expense)
                val net = moneyFormatPlain(ms.net)

                val incPaint = Paint(body).apply { color = INCOME }
                val expPaint = Paint(body).apply { color = EXPENSE }
                val netPaint = Paint(body).apply { color = if (ms.net >= 0) INCOME else EXPENSE; isFakeBoldText = true }

                drawRight(inc, left + colMonthW + colIncW, incPaint, base)
                drawRight(exp, left + colMonthW + colIncW + colExpW, expPaint, base)
                drawRight(net, right, netPaint, base)

                ctx.y += rowH
            }

            ctx.y += 18f // чуть больше “воздуха” после таблицы
            return ctx
        }

        fun wrapText(
            text: String,
            paint: Paint,
            maxWidth: Float,
            maxLines: Int
        ): List<String> {
            val cleaned = text.replace("\n", " ").trim()
            if (cleaned.isBlank()) return listOf("—")

            val tp = TextPaint(paint)
            val words = cleaned.split(Regex("\\s+"))
            val lines = ArrayList<String>(min(8, maxLines))

            var current = StringBuilder()

            fun flush() {
                if (current.isNotEmpty()) {
                    lines.add(current.toString())
                    current = StringBuilder()
                }
            }

            for (w in words) {
                val candidate = if (current.isEmpty()) w else "${current} $w"
                if (tp.measureText(candidate) <= maxWidth) {
                    current.clear()
                    current.append(candidate)
                } else {
                    flush()
                    if (lines.size >= maxLines) break

                    if (tp.measureText(w) > maxWidth) {
                        val chopped = TextUtils.ellipsize(w, tp, maxWidth, TextUtils.TruncateAt.END).toString()
                        lines.add(chopped)
                    } else {
                        current.append(w)
                    }
                }
                if (lines.size >= maxLines) break
            }
            flush()

            if (lines.size == maxLines) {
                val last = lines.last()
                lines[lines.lastIndex] = TextUtils.ellipsize(last, tp, maxWidth, TextUtils.TruncateAt.END).toString()
            }

            return if (lines.isEmpty()) listOf("—") else lines
        }

        fun drawMonthGroupHeader(ctx0: PageCtx, ym: YearMonth, sum: MonthSum): PageCtx {
            var ctx = ctx0
            ctx = ensureSpace(ctx, 44f)

            val top = ctx.y
            val h = 30f

            val rect = RectF(left, top, right, top + h)
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EEF3FF") }
            roundRect(ctx, rect, 12f, fill, stroke)

            val label = monthNameRu(ym)
            ctx.canvas.drawText(label, left + 12f, top + 20f, h2)

            val net = sum.net
            val netText = "Итог: ${moneyFormatPlain(net)}"
            val netPaint = Paint(h2).apply { color = if (net >= 0) INCOME else EXPENSE }
            val w = netPaint.measureText(netText)
            ctx.canvas.drawText(netText, right - 12f - w, top + 20f, netPaint)

            ctx.y = top + h + 10f
            return ctx
        }

        fun drawTransactionRow(ctx0: PageCtx, idx: Int, t: Transaction): PageCtx {
            var ctx = ctx0

            val dateText = t.date.format(dateFmt)
            val typeText = if (t.type == TxType.INCOME) "Доход" else "Расход"
            val amountText = if (t.type == TxType.INCOME) "+${moneyFormatPlain(t.amount)}" else "-${moneyFormatPlain(t.amount)}"
            val note = t.note?.trim().orEmpty().ifBlank { "—" }

            val lineHeight = 13.5f
            val maxNoteLines = 3
            val wrapped = wrapText(note, body, colNoteW, maxNoteLines)
            val linesCount = max(1, wrapped.size)
            val rowH = (linesCount * lineHeight) + 10f

            ctx = ensureSpace(ctx, rowH + 6f)

            val top = ctx.y
            if (idx % 2 == 1) {
                ctx.canvas.drawRect(RectF(left, top - 2f, right, top + rowH), fillAlt)
            }

            val base = top + 16.5f

            // Date
            ctx.canvas.drawText(dateText, left + 10f, base, body)

            // Type colored
            val typePaint = Paint(body).apply {
                color = if (t.type == TxType.INCOME) INCOME else EXPENSE
                isFakeBoldText = true
            }
            ctx.canvas.drawText(typeText, left + 10f + colDateW + colGap, base, typePaint)

            // Note wrapped
            val noteX = left + 10f + colDateW + colGap + colTypeW + colGap
            var nb = base
            for (lineText in wrapped) {
                ctx.canvas.drawText(lineText, noteX, nb, body)
                nb += lineHeight
            }

            // Amount right aligned
            val amountPaint = Paint(body).apply {
                color = if (t.type == TxType.INCOME) INCOME else EXPENSE
                isFakeBoldText = true
            }
            val aw = amountPaint.measureText(amountText)
            ctx.canvas.drawText(amountText, right - 10f - aw, base, amountPaint)

            // divider
            ctx.canvas.drawLine(left, top + rowH, right, top + rowH, line)

            ctx.y = top + rowH + 6f
            return ctx
        }

        // ---------- Build PDF ----------

        var ctx = PageCtx(1)
        ctx = drawTopHeader(ctx) // только на первой странице синяя шапка

        ctx = drawSummaryCards(ctx)
        ctx = drawMonthSummaryTable(ctx)

        ctx = ensureSpace(ctx, 40f)
        ctx.canvas.drawText("Детализация транзакций", left, ctx.y + h1.textSize, h1)
        ctx.y += 22f // ↑ УВЕЛИЧИЛИ ОТСТУП ПОСЛЕ ЗАГОЛОВКА

        if (txSorted.isEmpty()) {
            ctx = ensureSpace(ctx, 40f)
            ctx.canvas.drawText("Нет транзакций в выбранном периоде.", left, ctx.y + body.textSize, bodyMuted)
            ctx.y += 24f
        } else {
            groups.forEach { ym ->
                val monthTx = txByMonth[ym].orEmpty()
                val sum = monthSums.firstOrNull { it.ym == ym } ?: MonthSum(ym, 0.0, 0.0)

                ctx = drawMonthGroupHeader(ctx, ym, sum)
                // ШАПКУ "Дата / Тип / Комментарий / Сумма" УБРАЛИ — больше не рисуем

                monthTx.forEachIndexed { i, t ->
                    val beforePage = ctx.pageNumber
                    ctx = drawTransactionRow(ctx, i, t)

                    // Если произошёл переход страницы — повторяем только заголовок месяца (без табличной шапки)
                    if (ctx.pageNumber != beforePage) {
                        ctx = drawMonthGroupHeader(ctx, ym, sum)
                    }
                }

                ctx = ensureSpace(ctx, 12f)
                ctx.y += 6f
            }
        }

        finishPage(ctx)

        FileOutputStream(outFile).use { fos ->
            doc.writeTo(fos)
        }
        doc.close()

        return outFile
    }
}