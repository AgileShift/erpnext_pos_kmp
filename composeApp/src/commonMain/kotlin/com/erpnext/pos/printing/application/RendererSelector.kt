package com.erpnext.pos.printing.application

import com.erpnext.pos.domain.printing.errors.PrintingError
import com.erpnext.pos.domain.printing.model.PrintDocument
import com.erpnext.pos.domain.printing.model.PrinterProfile
import com.erpnext.pos.domain.printing.ports.PrintRenderer
import com.erpnext.pos.utils.AppLogger

class RendererSelector(
    private val renderers: List<PrintRenderer>,
) {
  fun select(profile: PrinterProfile, document: PrintDocument): PrintRenderer {
    AppLogger.info(
        "RendererSelector.select -> renderers=${renderers.map { it::class.simpleName }}, profile=${profile.name}, family=${profile.family}, language=${profile.language}, document=${document::class.simpleName}"
    )
    renderers.forEach { renderer ->
      AppLogger.info(
          "RendererSelector.select -> ${renderer::class.simpleName}.supports=${renderer.supports(profile, document)}"
      )
    }
    return renderers.firstOrNull { it.supports(profile, document) }
        ?: throw PrintingError.UnsupportedRenderer(
            buildString {
              append(profile.name)
              append(" / ")
              append(document::class.simpleName)
              append(" (family=")
              append(profile.family)
              append(", language=")
              append(profile.language)
              append(")")
            }
        )
  }
}
