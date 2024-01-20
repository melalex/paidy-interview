package forex.programs.rates

import forex.domain.Rate
import forex.services.rates.errors.{ Error => RatesServiceError }

import scala.concurrent.duration.Duration

object errors {

  sealed trait Error extends Exception

  object Error {

    final case class OneFrameIsUnavailable(msg: String) extends Error

    final case class ExchangeRateIsOutdated(rate: Rate, rateTtl: Duration) extends Error

    final case class UnknownCurrency(currency: String) extends Error

    final case class RequiredParamIsMissing(param: String) extends Error

    final case object CouldNotCheckRateOfSameCurrencies extends Error
  }

  def toProgramError(error: RatesServiceError): Error = error match {
    case RatesServiceError.OneFrameIsUnavailable(msg)        => Error.OneFrameIsUnavailable(msg)
    case RatesServiceError.ExchangeRateIsOutdated(rate, ttl) => Error.ExchangeRateIsOutdated(rate, ttl)
    case RatesServiceError.CouldNotCheckRateOfSameCurrencies => Error.CouldNotCheckRateOfSameCurrencies
  }
}
