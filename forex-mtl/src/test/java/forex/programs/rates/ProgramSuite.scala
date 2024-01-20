package forex.programs.rates

import cats.Id
import cats.implicits.catsSyntaxOptionId
import forex.domain.Rate.Pair
import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.programs.rates.Program
import forex.programs.rates.Protocol.GetRatesRequest
import forex.programs.rates.errors.Error.{
  CouldNotCheckRateOfSameCurrencies,
  ExchangeRateIsOutdated,
  OneFrameIsUnavailable,
  RequiredParamIsMissing,
  UnknownCurrency
}
import forex.services.RatesService
import forex.services.rates.errors.{ Error => RatesServiceError }
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock

import java.time.OffsetDateTime
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class ProgramSuite extends AnyWordSpec with Matchers {

  private val now             = OffsetDateTime.now()
  private val pair            = Pair(Currency.Usd, Currency.Jpy)
  private val rate            = Rate(pair, Price(BigDecimal(2)), Timestamp(now))
  private val unknownCurrency = "None"
  private val rateTtl         = 5 minutes

  private val service = mock[RatesService[Id]]
  private val program = Program[Id](service)

  "Program" should {

    "return rates" in {
      when(service.get(pair)).thenReturn(Right(rate))

      val actual = program.get(GetRatesRequest(pair.from.code.some, pair.to.code.some))

      actual shouldEqual Right(rate)
    }

    "return error" when {

      "from is " + unknownCurrency in {
        val actual = program.get(GetRatesRequest(None, pair.to.code.some))

        actual shouldEqual Left(RequiredParamIsMissing("from"))
      }

      "to is " + unknownCurrency in {
        val actual = program.get(GetRatesRequest(pair.from.code.some, None))

        actual shouldEqual Left(RequiredParamIsMissing("to"))
      }

      "from has incorrect format" in {
        val actual = program.get(GetRatesRequest(unknownCurrency.some, pair.to.code.some))

        actual shouldEqual Left(UnknownCurrency(unknownCurrency))
      }

      "to has incorrect format" in {
        val actual = program.get(GetRatesRequest(pair.from.code.some, unknownCurrency.some))

        actual shouldEqual Left(UnknownCurrency(unknownCurrency))
      }

      "service returned OneFrameIsUnavailable" in {
        when(service.get(pair)).thenReturn(Left(RatesServiceError.OneFrameIsUnavailable("Error")))

        val actual = program.get(GetRatesRequest(pair.from.code.some, pair.to.code.some))

        actual shouldEqual Left(OneFrameIsUnavailable("Error"))
      }

      "service returned ExchangeRateIsOutdated" in {
        when(service.get(pair)).thenReturn(Left(RatesServiceError.ExchangeRateIsOutdated(rate, rateTtl)))

        val actual = program.get(GetRatesRequest(pair.from.code.some, pair.to.code.some))

        actual shouldEqual Left(ExchangeRateIsOutdated(rate, rateTtl))
      }

      "service returned CouldNotCheckRateOfSameCurrencies" in {
        when(service.get(pair)).thenReturn(Left(RatesServiceError.CouldNotCheckRateOfSameCurrencies))

        val actual = program.get(GetRatesRequest(pair.from.code.some, pair.to.code.some))

        actual shouldEqual Left(CouldNotCheckRateOfSameCurrencies)
      }
    }
  }
}
