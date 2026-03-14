package com.erpnext.pos.printing

import com.erpnext.pos.domain.printing.model.PrinterCapabilities
import com.erpnext.pos.domain.printing.model.PrinterFamily
import com.erpnext.pos.domain.printing.model.PrinterLanguage
import com.erpnext.pos.domain.printing.model.PrinterProfile
import com.erpnext.pos.domain.printing.model.PrinterTarget
import com.erpnext.pos.domain.printing.model.TransportType
import com.erpnext.pos.printing.application.PrinterTargetResolver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PrinterTargetResolverTest {
  private val resolver = PrinterTargetResolver()

  @Test
  fun `resolve uses preferred tcp target when configured`() {
    val resolved =
        resolver.resolve(
            PrinterProfile(
                id = "1",
                name = "Front desk",
                family = PrinterFamily.RECEIPT,
                language = PrinterLanguage.ESC_POS,
                supportedTransports = setOf(TransportType.TCP_RAW, TransportType.BT_SPP),
                preferredTransport = TransportType.TCP_RAW,
                host = "192.168.1.40",
                port = 9100,
                capabilities = PrinterCapabilities(),
            )
        )

    assertEquals(TransportType.TCP_RAW, resolved.transportType)
    assertTrue(resolved.target is PrinterTarget.TcpRaw)
  }
}
