package forex.services.rates

import forex.services.{ OneFrameError, RatesError }
import forex.services.oneframe.errors.Error

trait Converter {

  def toRatesError(source: OneFrameError): RatesError = source match {
    case Error.OneFrameLookupFailed(message) => errors.Error.OneFrameIsUnavailable(message)
  }
}

object Converter extends Converter
