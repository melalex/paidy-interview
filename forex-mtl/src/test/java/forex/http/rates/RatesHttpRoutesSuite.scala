package forex.http.rates

import cats.Monad
import cats.data.OptionT
import cats.effect.IO
import cats.implicits._
import forex.domain.Rate.Pair
import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.http.Problems.Problem
import forex.http.rates.Protocol.GetApiResponse
import forex.programs.RatesProgram
import forex.programs.rates.Protocol.GetRatesRequest
import forex.programs.rates.errors.Error._
import org.http4s.Status.Successful
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{ EntityDecoder, Request, Response, Uri }
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{ EitherValues, OptionValues }
import org.scalatestplus.mockito.MockitoSugar.mock

import java.time.OffsetDateTime
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class RatesHttpRoutesSuite extends AnyWordSpec with Matchers with OptionValues with EitherValues {

  private val now         = OffsetDateTime.now()
  private val pair        = Pair(Currency.Usd, Currency.Jpy)
  private val from        = pair.from.code
  private val to          = pair.to.code
  private val rate        = Rate(pair, Price(BigDecimal(2)), Timestamp(now))
  private val apiResponse = GetApiResponse(pair.from, pair.to, rate.price, rate.timestamp)
  private val rateTtl     = 5 minutes
  private val anyStr      = "None"

  private val program         = mock[RatesProgram[IO]]
  private val ratesHttpRoutes = new RatesHttpRoutes(program)

  "RestHttpRoutes" should {

    "return rates" in {
      when(program.get(GetRatesRequest(from.some, to.some))).thenReturn(IO(Right(rate)))

      val actual = executeRequest[IO, Problem, GetApiResponse](ratesHttpRoutes.routes(getExchangeRateRequest(from, to)))
        .unsafeRunSync()

      actual shouldEqual Right(apiResponse)
    }

    "return problem" when {

      "One Frame service is unavailable" in {
        when(program.get(GetRatesRequest(from.some, to.some))).thenReturn(IO(Left(OneFrameIsUnavailable(""))))

        val actual =
          executeRequest[IO, Problem, GetApiResponse](ratesHttpRoutes.routes(getExchangeRateRequest(from, to)))
            .unsafeRunSync()

        actual shouldEqual Left(
          Problem(
            `type` = "https://example.com/probs/one-frame-is-unavailable",
            tittle = "Service is currently unavailable",
            status = 503,
            detail = "Service is currently unavailable. Try again letter.",
            instance = "/rates"
          )
        )
      }

      "exchange rate is outdated" in {
        when(program.get(GetRatesRequest(from.some, to.some)))
          .thenReturn(IO(Left(ExchangeRateIsOutdated(rate, rateTtl))))

        val actual =
          executeRequest[IO, Problem, GetApiResponse](ratesHttpRoutes.routes(getExchangeRateRequest(from, to)))
            .unsafeRunSync()

        actual shouldEqual Left(
          Problem(
            `type` = "https://example.com/probs/exchange-rate-is-outdated",
            tittle = "Exchange rate is outdated",
            status = 500,
            detail = s"The latest known exchange rate is [ $rate ]. But it's older then [ ${rateTtl.toSeconds} ] sec",
            instance = s"/rates"
          )
        )
      }

      "unknown currency was provided" in {
        when(program.get(GetRatesRequest(from.some, to.some))).thenReturn(IO(Left(UnknownCurrency(anyStr))))

        val actual =
          executeRequest[IO, Problem, GetApiResponse](ratesHttpRoutes.routes(getExchangeRateRequest(from, to)))
            .unsafeRunSync()

        actual shouldEqual Left(
          Problem(
            `type` = "https://example.com/probs/unknown-currency",
            tittle = "Wrong currency format",
            status = 400,
            detail = s"[ $anyStr ] is not valid currency",
            instance = s"/rates"
          )
        )
      }

      "required param is missing" in {
        when(program.get(GetRatesRequest(from.some, to.some))).thenReturn(IO(Left(RequiredParamIsMissing(anyStr))))

        val actual =
          executeRequest[IO, Problem, GetApiResponse](ratesHttpRoutes.routes(getExchangeRateRequest(from, to)))
            .unsafeRunSync()

        actual shouldEqual Left(
          Problem(
            `type` = "https://example.com/probs/required-param-is-missing",
            tittle = "Required parameter is missing",
            status = 400,
            detail = s"Required parameter [ $anyStr ] is missing",
            instance = s"/rates"
          )
        )
      }

      "'from' and 'to' params are equal" in {
        when(program.get(GetRatesRequest(from.some, to.some))).thenReturn(IO(Left(CouldNotCheckRateOfSameCurrencies)))

        val actual =
          executeRequest[IO, Problem, GetApiResponse](ratesHttpRoutes.routes(getExchangeRateRequest(from, to)))
            .unsafeRunSync()

        actual shouldEqual Left(
          Problem(
            `type` = "https://example.com/probs/could-not-check-rate-of-same-currencies",
            tittle = "Could not check rates of same currency pair",
            status = 400,
            detail = "[ from ] query param should not equal [ to ] query param",
            instance = s"/rates"
          )
        )
      }
    }
  }

  private def executeRequest[F[_]: Monad, A, B](
      resp: OptionT[F, Response[F]]
  )(implicit ad: EntityDecoder[F, A], bd: EntityDecoder[F, B]): F[Either[A, B]] =
    resp.value
      .map(_.value)
      .flatMap {
        case Successful(value) => bd.decode(value, strict = false).map(_.asRight[A]).value
        case error             => ad.decode(error, strict = false).map(_.asLeft[B]).value
      }
      .map(_.value)

  private def getExchangeRateRequest[F[_]](from: String, to: String) = Request[F](
    uri = Uri().withPath(path"/rates").withQueryParam("from", from).withQueryParam("to", to)
  )
}
