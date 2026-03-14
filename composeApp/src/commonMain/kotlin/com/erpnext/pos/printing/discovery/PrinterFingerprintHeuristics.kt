package com.erpnext.pos.printing.discovery

import com.erpnext.pos.domain.printing.model.PrinterFamily
import com.erpnext.pos.domain.printing.model.PrinterLanguage

/**
 * Heuristic-only fingerprinting from the user-visible Bluetooth name.
 *
 * We deliberately keep this vendor-agnostic and conservative: the output is a
 * suggestion for pre-filling the form, not a guaranteed capability contract.
 */
data class PrinterFingerprintHints(
    val displayName: String,
    val brandHint: String? = null,
    val modelHint: String? = null,
    val familyHint: PrinterFamily = PrinterFamily.RECEIPT,
    val languageHint: PrinterLanguage = PrinterLanguage.ESC_POS,
    val paperWidthMmHint: Int? = null,
    val charactersPerLineHint: Int? = null,
    val codePageHint: String? = null,
    val confidenceLabel: String = "Suggested from Bluetooth name",
)

object PrinterFingerprintHeuristics {
  private val printerNameTokens =
      listOf(
          "BIXOLON",
          "SPP-",
          "SPP ",
          "SRP-",
          "EPSON",
          "TM-",
          "ZEBRA",
          "ZD",
          "ZT",
          "GK",
          "XPRINTER",
          "XP-",
          "3NSTAR",
          "RPT",
          "POS-",
          "PRINTER",
          "LABEL",
          "TSC",
          "TSP",
      )

  private val nonPrinterNameTokens =
      listOf(
          "JBL",
          "SPEAKER",
          "HEADSET",
          "HEADPHONE",
          "EARBUD",
          "BUDS",
          "AIRPODS",
          "WATCH",
          "PHONE",
          "LAPTOP",
          "KEYBOARD",
          "MOUSE",
          "GAMEPAD",
          "CONTROLLER",
          "CAR",
          "AUDIO",
          "TV",
      )

  fun looksLikePrinterName(rawName: String?): Boolean {
    val upper = rawName.orEmpty().trim().uppercase()
    return printerNameTokens.any { token -> token in upper }
  }

  fun looksLikeNonPrinterName(rawName: String?): Boolean {
    val upper = rawName.orEmpty().trim().uppercase()
    return nonPrinterNameTokens.any { token -> token in upper }
  }

  fun fromDeviceName(rawName: String?): PrinterFingerprintHints {
    val normalized = rawName.orEmpty().trim()
    val upper = normalized.uppercase()
    val fallbackName = normalized.ifBlank { "Bluetooth printer" }

    val brand =
        when {
          "SPP-" in upper || "SPP " in upper -> "Bixolon"
          "EPSON" in upper || "TM-" in upper -> "Epson"
          "BIXOLON" in upper || "SRP-" in upper -> "Bixolon"
          "ZEBRA" in upper || "ZD" in upper || "GK" in upper || "ZT" in upper -> "Zebra"
          "STAR" in upper || "TSP" in upper || "MCP" in upper -> "Star"
          "3NSTAR" in upper || "RPT" in upper -> "3nStar"
          "XPRINTER" in upper || "XP-" in upper || "XP-".replace("-", "") in upper -> "XPrinter"
          "POS-" in upper || "MPT" in upper || "PT-" in upper -> "Generic POS"
          else -> null
        }

    val family =
        when {
          ("ZEBRA" in upper || "LABEL" in upper || "ZD" in upper || "GK" in upper || "ZT" in upper) &&
              "SPP-" !in upper && "SPP " !in upper ->
              PrinterFamily.LABEL
          else -> PrinterFamily.RECEIPT
        }

    val language =
        when (family) {
          PrinterFamily.LABEL -> PrinterLanguage.ZPL
          PrinterFamily.RECEIPT -> PrinterLanguage.ESC_POS
        }

    val width =
        when {
          "58" in upper || "58MM" in upper -> 58
          "SPP-R310" in upper || "SPP R310" in upper -> 80
          "80" in upper || "80MM" in upper || "TM-T20" in upper || "TM-T88" in upper || "SRP-350" in upper -> 80
          "ZD" in upper || "GK" in upper || "ZT" in upper -> 100
          else -> if (family == PrinterFamily.RECEIPT) 80 else null
        }

    val chars =
        when (width) {
          58 -> 32
          80 -> 48
          else -> null
        }

    val codePage =
        when {
          family == PrinterFamily.LABEL -> null
          "EPSON" in upper || "TM-" in upper || "XPRINTER" in upper || "XP-" in upper -> "CP437"
          else -> "CP437"
        }

    val model =
        normalized
            .split(" ", "-", "_")
            .windowed(size = 2, step = 1, partialWindows = true)
            .firstOrNull { tokens ->
                  tokens.any { token -> token.any(Char::isDigit) } ||
                  tokens.any { token -> token.uppercase() in setOf("TM", "SRP", "SPP", "ZD", "ZT", "GK", "XP", "TSP") }
            }
            ?.joinToString(" ")
            ?.trim()
            ?.takeIf { it.isNotBlank() && it != fallbackName }

    return PrinterFingerprintHints(
        displayName = fallbackName,
        brandHint = brand,
        modelHint = model,
        familyHint = family,
        languageHint = language,
        paperWidthMmHint = width,
        charactersPerLineHint = chars,
        codePageHint = codePage,
        confidenceLabel =
            if (brand != null || model != null) {
              "Suggested from Bluetooth name"
            } else {
              "Generic suggestion"
            },
    )
  }
}
