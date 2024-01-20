package forex.services.rates

import forex.domain.Rate

import scala.concurrent.duration.Duration

object errors {

  sealed trait Error

  object Error {

    final case class OneFrameIsUnavailable(debugMsg: String) extends Error
    final case class ExchangeRateIsOutdated(rate: Rate, rateTtl: Duration) extends Error
    final case object CouldNotCheckRateOfSameCurrencies extends Error
  }
}
