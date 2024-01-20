package forex.service.rates.interpreters

import cats.Id
import forex.config.RatesConfig
import forex.domain.Rate.Pair
import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.services.OneFrameService
import forex.services.oneframe.errors.Error.OneFrameLookupFailed
import forex.services.rates.errors.Error
import forex.services.rates.errors.Error.{ CouldNotCheckRateOfSameCurrencies, OneFrameIsUnavailable }
import forex.services.rates.interpreters.OneFrameRatesLive
import forex.util.TimeProvider
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock

import java.time.{ Instant, OffsetDateTime, ZoneOffset }
import scala.concurrent.duration.DurationInt
import scala.jdk.DurationConverters.ScalaDurationOps
import scala.language.postfixOps

class OneFrameRatesLiveSuite extends AnyWordSpec with Matchers {

  private val now  = Instant.now()
  private val pair = Pair(Currency.Usd, Currency.Jpy)
  private val rate = Rate(pair, Price(BigDecimal(2)), Timestamp(OffsetDateTime.ofInstant(now, ZoneOffset.UTC)))

  private val timeProvider      = TimeProvider.fixed[Id](now)
  private val config            = RatesConfig(5 minutes)
  private val oneFrameService   = mock[OneFrameService[Id]]
  private val oneFrameRatesLive = new OneFrameRatesLive(oneFrameService, config, timeProvider)

  "OneFrameRatesLive" should {

    "return rates" in {
      when(oneFrameService.getExchangeRates(Set(pair))).thenReturn(Right(Map(pair -> rate)))

      val actual = oneFrameRatesLive.get(pair)

      actual shouldEqual Right(rate)
    }

    "return error" when {

      "rate is outdated" in {
        val outdatedRate = rate.copy(
          timestamp =
            Timestamp(OffsetDateTime.ofInstant(now.minus(config.ttl.toJava).minus(config.ttl.toJava), ZoneOffset.UTC))
        )
        when(oneFrameService.getExchangeRates(Set(pair))).thenReturn(Right(Map(pair -> outdatedRate)))

        val actual = oneFrameRatesLive.get(pair)

        actual shouldEqual Left(Error.ExchangeRateIsOutdated(outdatedRate, config.ttl))
      }

      "from currency == to currency" in {
        val actual = oneFrameRatesLive.get(Pair(Currency.Usd, Currency.Usd))

        actual shouldEqual Left(CouldNotCheckRateOfSameCurrencies)
      }

      "one frame returned error" in {
        when(oneFrameService.getExchangeRates(Set(pair))).thenReturn(Left(OneFrameLookupFailed("Error")))

        val actual = oneFrameRatesLive.get(pair)

        actual shouldEqual Left(OneFrameIsUnavailable("Error"))
      }
    }
  }
}
