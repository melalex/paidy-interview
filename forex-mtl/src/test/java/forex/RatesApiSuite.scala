package forex

import cats.effect.{ Async, IO, Resource }
import forex.domain.{ Currency, Price, Timestamp }
import forex.domain.Rate.AllCurrencyPairs
import forex.http.rates.Protocol.GetApiResponse
import forex.services.oneframe.Converter.convertToOneFramePair
import forex.services.oneframe.client.OneFrameProtocol.CurrencyExchangeRateDto
import forex.test.TiFixture
import io.circe._
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.OffsetDateTime
import scala.io.Source

class RatesApiSuite extends AnyWordSpec with Matchers with TiFixture with EitherValues {

  private val oneFrameResponse = Async.memoize {
    Resource
      .fromAutoCloseable(IO(Source.fromResource("one-frame/all-pairs.json")))
      .map(_.getLines().mkString("\n"))
      .map(parser.decode[Seq[CurrencyExchangeRateDto]])
      .evalMap(IO.fromEither)
      .use(it => IO(it))
  }

  "RatesApi" should {

    "return exchange rate" in withAppUpAndRunning(prepareWireMock) { endpoint =>
      val actual = endpoint.getExchangeRate(Currency.Aud, Currency.Cad).unsafeRunSync()

      actual.value shouldEqual GetApiResponse(
        Currency.Aud,
        Currency.Cad,
        Price(BigDecimal("0.71810472617368925")),
        Timestamp(OffsetDateTime.parse("2024-01-20T19:27:20.205Z"))
      )
    }
  }

  private def prepareWireMock(endpoint: WireMockEndpoint) =
    oneFrameResponse
      .flatMap(identity)
      .map(it => endpoint.stubOneFrameGetRateResponse(AllCurrencyPairs.map(convertToOneFramePair), it))
}
