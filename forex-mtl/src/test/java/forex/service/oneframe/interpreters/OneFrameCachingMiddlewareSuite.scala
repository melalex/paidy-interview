package forex.service.oneframe.interpreters

import cats.effect.IO
import forex.domain.Rate.Pair
import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.services.oneframe
import forex.services.oneframe.errors.Error.OneFrameLookupFailed
import forex.services.oneframe.interpreters.OneFrameCachingMiddleware
import forex.util.TimeProvider
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock

import java.time.OffsetDateTime

class OneFrameCachingMiddlewareSuite extends AnyWordSpec with Matchers {

  private val delegate = mock[oneframe.Algebra[IO]]

  private val time = OffsetDateTime.now()
  private val pair = Pair(Currency.Usd, Currency.Jpy)
  private val rate = Rate(pair, Price(BigDecimal(2)), Timestamp(time))

  "OneFrameCachingMiddleware" should {

    "return cached value" in {
      when(delegate.getExchangeRates(OneFrameCachingMiddleware.AllCurrencyPairs))
        .thenReturn(IO(Right(Map(pair -> rate))))

      val actual = OneFrameCachingMiddleware[IO](delegate, TimeProvider.real())
        .flatMap(it => it.getExchangeRates(Set(pair)))
        .unsafeRunSync()

      actual shouldEqual Right(Map(pair -> rate))
    }

    "refresh cache" in {
      when(delegate.getExchangeRates(OneFrameCachingMiddleware.AllCurrencyPairs))
        .thenReturn(IO(Left(OneFrameLookupFailed(""))), IO(Right(Map(pair -> rate))))

      val actual = for {
        caching <- OneFrameCachingMiddleware[IO](delegate, TimeProvider.real())
        _ <- caching.refreshCache()
        res <- caching.getExchangeRates(Set(pair))
      } yield res

      actual.unsafeRunSync() shouldEqual Right(Map(pair -> rate))
    }
  }
}
