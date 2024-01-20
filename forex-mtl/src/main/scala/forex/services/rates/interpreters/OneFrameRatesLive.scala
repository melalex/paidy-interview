package forex.services.rates.interpreters

import cats.Monad
import cats.data.EitherT
import cats.implicits._
import forex.config.RatesConfig
import forex.domain.Rate
import forex.services.OneFrameService
import forex.services.rates.Algebra
import forex.services.rates.Converter.toRatesError
import forex.services.rates.errors.Error
import forex.services.rates.errors.Error.CouldNotCheckRateOfSameCurrencies
import forex.util.TimeProvider

import java.time.Duration
import scala.jdk.DurationConverters.JavaDurationOps

class OneFrameRatesLive[F[_]](oneFrameService: OneFrameService[F], config: RatesConfig, timeProvider: TimeProvider[F])(
    implicit F: Monad[F]
) extends Algebra[F] {

  override def get(pair: Rate.Pair): F[Error Either Rate] =
    if (pair.to == pair.from) F.pure(Left(CouldNotCheckRateOfSameCurrencies))
    else
      EitherT(oneFrameService.getExchangeRates(Set(pair)))
        .map(it => it(pair))
        .leftMap(toRatesError)
        .flatMapF(validateRateTimestamp)
        .value

  private def validateRateTimestamp(rate: Rate) =
    timeProvider.now
      .map(it => Duration.between(rate.timestamp.value.toInstant, it).toScala)
      .map(it => it > config.ttl)
      .map {
        case true  => Error.ExchangeRateIsOutdated(rate, config.ttl).asLeft[Rate]
        case false => rate.asRight[Error]
      }
}
