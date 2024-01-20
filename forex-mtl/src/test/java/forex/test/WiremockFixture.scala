package forex.test

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration

import java.net.ServerSocket
import scala.util.Try

trait WiremockFixture {

  protected def withWiremock(test: WireMockServer => Any): Unit = {
    val server = new WireMockServer(WireMockConfiguration.options.port(freePort))

    try {
      server.start()

      test(server)
      ()
    } finally {
      server.stop()
    }
  }

  private def freePort: Int = {
    val socket = new ServerSocket(0)
    socket.setReuseAddress(true)
    val port = socket.getLocalPort
    Try(socket.close())
    port
  }
}
