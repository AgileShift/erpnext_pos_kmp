package com.erpnext.pos.printing.formatting

import com.erpnext.pos.domain.printing.model.PrintAlignment
import com.erpnext.pos.domain.printing.model.ReceiptDocument
import com.erpnext.pos.domain.printing.model.ReceiptLine

class ReceiptFormatter {
  fun format(document: ReceiptDocument, lineWidth: Int): List<String> {
    val result = mutableListOf<String>()

    if (document.header.lines.isNotEmpty()) {
      result += document.header.lines.map { align(it, lineWidth, document.header.alignment) }
      result += ""
    }

    document.bodyLines.forEach { line ->
      result += wrapLine(line, lineWidth)
    }

    result += "-".repeat(lineWidth)
    document.totals.subTotal?.let { result += formatLine(ReceiptLine("Subtotal", it), lineWidth) }
    document.totals.tax?.let { result += formatLine(ReceiptLine("Tax", it), lineWidth) }
    result += formatLine(ReceiptLine("TOTAL", document.totals.total, emphasis = true), lineWidth)

    if (document.footer.lines.isNotEmpty()) {
      result += ""
      result += document.footer.lines.map { align(it, lineWidth, document.footer.alignment) }
    }

    return result
  }

  private fun wrapLine(line: ReceiptLine, width: Int): List<String> {
    if (line.right.isBlank()) return wrapSingleColumn(line.left, width)

    val right = line.right.trim().take(width)
    val availableLeft = (width - right.length - 1).coerceAtLeast(1)
    val leftChunks = wrapSingleColumn(line.left, availableLeft)
    if (leftChunks.isEmpty()) return listOf(right.padStart(width))

    return leftChunks.mapIndexed { index, chunk ->
      if (index == leftChunks.lastIndex) {
        val spaces = (width - chunk.length - right.length).coerceAtLeast(1)
        chunk + " ".repeat(spaces) + right
      } else {
        chunk
      }
    }
  }

  private fun wrapSingleColumn(text: String, width: Int): List<String> {
    val words = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.isEmpty()) return listOf("")

    val lines = mutableListOf<String>()
    var current = ""
    words.forEach { word ->
      val candidate = if (current.isBlank()) word else "$current $word"
      if (candidate.length <= width) {
        current = candidate
      } else {
        if (current.isNotBlank()) {
          lines += current
        }
        current =
            if (word.length <= width) {
              word
            } else {
              word.chunked(width).also { chunks -> lines += chunks.dropLast(1) }.last()
            }
      }
    }
    if (current.isNotBlank()) lines += current
    return lines
  }

  private fun formatLine(line: ReceiptLine, width: Int): String {
    val left = line.left.trim()
    val right = line.right.trim()
    if (right.isBlank()) return left.take(width)

    val spaces = (width - left.length - right.length).coerceAtLeast(1)
    return (left + " ".repeat(spaces) + right).take(width)
  }

  private fun align(text: String, width: Int, alignment: PrintAlignment): String {
    val clean = text.trim().take(width)
    return when (alignment) {
      PrintAlignment.START -> clean
      PrintAlignment.CENTER -> {
        val leftPad = ((width - clean.length) / 2).coerceAtLeast(0)
        " ".repeat(leftPad) + clean
      }
      PrintAlignment.END -> clean.padStart(width)
    }
  }
}
