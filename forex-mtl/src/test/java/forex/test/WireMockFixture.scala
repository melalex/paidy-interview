package forex.test

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration

import scala.util.Try

trait WireMockFixture {

  protected def withWireMock(test: WireMockServer => Any): Unit = {
    val server = new WireMockServer(WireMockConfiguration.options.port(freePort[Try].get))

    try {
      server.start()

      test(server)
      ()
    } finally {
      server.stop()
    }
  }
}
