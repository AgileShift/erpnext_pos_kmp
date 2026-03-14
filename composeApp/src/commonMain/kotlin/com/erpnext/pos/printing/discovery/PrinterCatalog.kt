package com.erpnext.pos.printing.discovery

object PrinterCatalog {
  private val modelsByBrand =
      linkedMapOf(
          "Bixolon" to listOf("SPP-R200", "SPP-R310", "SPP-R410", "SRP-330", "SRP-350", "SRP-E300", "Custom"),
          "Epson" to listOf("TM-T20", "TM-T82", "TM-T88", "TM-m30", "Custom"),
          "Star" to listOf("TSP100", "TSP143", "mC-Print3", "Custom"),
          "XPrinter" to listOf("XP-58", "XP-80", "XP-Q200", "Custom"),
          "Zebra" to listOf("ZD220", "ZD230", "GK420d", "ZT230", "Custom"),
          "3nStar" to listOf("RPT008", "RPT015", "RPT008B", "Custom"),
          "Generic POS" to listOf("58mm receipt printer", "80mm receipt printer", "Generic label printer", "Custom"),
      )

  fun brands(currentBrand: String): List<String> =
      buildList {
        addAll(modelsByBrand.keys)
        if (currentBrand.isNotBlank() && currentBrand !in this) {
          add(currentBrand)
        }
      }

  fun models(brand: String, currentModel: String): List<String> =
      buildList {
        addAll(modelsByBrand[brand].orEmpty())
        if (currentModel.isNotBlank() && currentModel !in this) {
          add(0, currentModel)
        }
      }
}
