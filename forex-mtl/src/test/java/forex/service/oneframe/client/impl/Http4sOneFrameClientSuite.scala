package forex.service.oneframe.client.impl

import cats.effect.{ConcurrentEffect, ContextShift, IO}
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import forex.config.{OneFrameCacheConfig, OneFrameConfig, OneFrameRetryConfig}
import forex.services.oneframe.client.OneFrameProtocol
import forex.services.oneframe.client.OneFrameProtocol.{CurrencyDto, CurrencyExchangeRateDto, CurrencyPairDto}
import forex.services.oneframe.client.impl.Http4sOneFrameClient
import forex.services.oneframe.errors.Error.OneFrameLookupFailed
import forex.test.WiremockFixture
import io.circe.syntax._
import org.http4s.Status.{NotFound, Ok}
import org.http4s.blaze.client.BlazeClientBuilder
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatest.wordspec.AnyWordSpec

import java.time.OffsetDateTime
import scala.concurrent.ExecutionContext
import scala.language.postfixOps

class Http4sOneFrameClientSuite extends AnyWordSpec with Matchers with WiremockFixture with OneFrameProtocol {

  implicit private val ec: ExecutionContext    = ExecutionContext.global
  implicit private val shift: ContextShift[IO] = IO.contextShift(ec)

  private val apiKey = "10dc303535874aeccc86a8251e6992f5"

  private val time = OffsetDateTime.now()

  private val oneFrameRate =
    CurrencyExchangeRateDto(CurrencyDto.Usd, CurrencyDto.Jpy, BigDecimal(0), BigDecimal(1), BigDecimal(2), time)

  "Http4sOneFrameClientSuite" should {

    "return Ok response" when {

      "One Frame respond with Ok" in withWiremock { wiremock =>
        val request = WireMock
          .get(s"/rates?pair=${CurrencyDto.Usd.code}${CurrencyDto.Jpy.code}")
          .withHeader("token", equalTo(apiKey))

        val response = WireMock
          .aResponse()
          .withBody(Seq(oneFrameRate).asJson.noSpaces)
          .withHeader("Content-Type", "application/json")
          .withStatus(Ok.code)

        wiremock.stubFor(request.willReturn(response))

        val actual = createClient[IO](wiremock.port())
          .use(_.getExchangeRates(Seq(CurrencyPairDto(CurrencyDto.Usd, CurrencyDto.Jpy))))
          .unsafeRunSync()

        actual shouldEqual Right(Seq(oneFrameRate))
      }
    }

    "return error response" when {

      "One Frame respond with Nok" in withWiremock { wiremock =>
        val request = WireMock
          .get(s"/rates?pair=${CurrencyDto.Usd.code}${CurrencyDto.Jpy.code}")
          .withHeader("token", equalTo(apiKey))

        val response = WireMock
          .aResponse()
          .withBody("Error")
          .withStatus(NotFound.code)

        wiremock.stubFor(request.willReturn(response))

        val port = wiremock.port()

        val actual = createClient[IO](port)
          .use(_.getExchangeRates(Seq(CurrencyPairDto(CurrencyDto.Usd, CurrencyDto.Jpy))))
          .unsafeRunSync()

        actual shouldEqual Left(OneFrameLookupFailed(s"unexpected HTTP status: 404 Not Found for request GET http://localhost:$port/rates?pair=USDJPY"))
      }
    }
  }

  private def createClient[F[_]: ConcurrentEffect](port: Int) =
    BlazeClientBuilder[F](ExecutionContext.global).resource
      .map(it => Http4sOneFrameClient.createSimple[F](it, createConfig(port)))

  private def createConfig(port: Int) = OneFrameConfig(
    s"http://localhost:$port",
    apiKey,
    OneFrameCacheConfig(
      ttl = 5 minutes
    ),
    OneFrameRetryConfig(
      maxDuration = 5 seconds,
      maxRetries = 5
    )
  )
}
