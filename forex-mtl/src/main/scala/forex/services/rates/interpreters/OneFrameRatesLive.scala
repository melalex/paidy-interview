package forex.services.rates.interpreters

import cats.Monad
import cats.data.EitherT
import cats.effect.Clock
import cats.implicits._
import forex.config.RatesConfig
import forex.domain.Rate
import forex.services.OneFrameService
import forex.services.rates.Algebra
import forex.services.rates.Converter.toRatesError
import forex.services.rates.errors.Error
import forex.services.rates.errors.Error.CouldNotCheckRateOfSameCurrencies

import java.time.{ Duration, Instant }
import java.util.concurrent.TimeUnit
import scala.jdk.DurationConverters.JavaDurationOps

class OneFrameRatesLive[F[_]](client: OneFrameService[F], config: RatesConfig)(implicit F: Monad[F], clock: Clock[F])
    extends Algebra[F] {

  override def get(pair: Rate.Pair): F[Error Either Rate] =
    if (pair.to == pair.from) F.pure(Left(CouldNotCheckRateOfSameCurrencies))
    else
      EitherT(client.getExchangeRates(Set(pair)))
        .map(it => it(pair))
        .leftMap(toRatesError)
        .flatMapF(validateRateTimestamp)
        .value

  private def validateRateTimestamp(rate: Rate) =
    clock
      .realTime(TimeUnit.SECONDS)
      .map(Instant.ofEpochSecond)
      .map(it => Duration.between(rate.timestamp.value.toInstant, it).toScala)
      .map(it => it > config.ttl)
      .map {
        case true  => Error.ExchangeRateIsOutdated(rate, config.ttl).asLeft[Rate]
        case false => rate.asRight[Error]
      }
}
