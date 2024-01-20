package forex.service.oneframe.interpreters

import cats.Id
import cats.implicits.catsSyntaxEitherId
import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.domain.Rate.Pair
import forex.services.OneFrameError
import forex.services.oneframe.client.OneFrameClient
import forex.services.oneframe.client.OneFrameProtocol.{ CurrencyDto, CurrencyExchangeRateDto, CurrencyPairDto }
import forex.services.oneframe.interpreters.OneFrameLive
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock

import java.time.OffsetDateTime

class OneFrameLiveSuite extends AnyWordSpec with Matchers {

  private val time         = OffsetDateTime.now()
  private val pair         = Pair(Currency.Usd, Currency.Jpy)
  private val oneFramePair = CurrencyPairDto(CurrencyDto.Usd, CurrencyDto.Jpy)
  private val expected     = Rate(pair, Price(BigDecimal(2)), Timestamp(time))
  private val oneFrameRate =
    CurrencyExchangeRateDto(CurrencyDto.Usd, CurrencyDto.Jpy, BigDecimal(0), BigDecimal(1), BigDecimal(2), time)

  private val oneFrameClient = mock[OneFrameClient[Id]]
  private val oneFrameLive   = new OneFrameLive[Id](oneFrameClient)

  "OneFrameLive" should {

    "getExchangeRates" in {
      when(oneFrameClient.getExchangeRates(Set(oneFramePair))).thenReturn(Seq(oneFrameRate).asRight[OneFrameError])

      val actual = oneFrameLive.getExchangeRates(Set(pair))

      actual shouldEqual Right(Map(expected.pair -> expected))
    }
  }
}
