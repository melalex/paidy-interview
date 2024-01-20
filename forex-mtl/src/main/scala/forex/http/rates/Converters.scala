package forex.http.rates

import forex.domain._
import forex.http.Problems.{ Problem, Problematic }
import forex.programs.rates.errors._

object Converters {
  import Protocol._

  implicit private[rates] val problematicForRatesErrors: Problematic[Error] = {
    case Error.OneFrameIsUnavailable(_) =>
      Problem(
        `type` = "https://example.com/probs/one-frame-is-unavailable",
        tittle = "Service is currently unavailable",
        status = 503,
        detail = "Service is currently unavailable. Try again letter.",
        instance = "/rates"
      )
    case Error.ExchangeRateIsOutdated(rate, rateTtl) =>
      Problem(
        `type` = "https://example.com/probs/exchange-rate-is-outdated",
        tittle = "Exchange rate is outdated",
        status = 500,
        detail = s"The latest known exchange rate is [ $rate ]. But it's older then [ ${rateTtl.toSeconds} ] sec",
        instance = s"/rates"
      )
    case Error.UnknownCurrency(currency) =>
      Problem(
        `type` = "https://example.com/probs/unknown-currency",
        tittle = "Wrong currency format",
        status = 400,
        detail = s"[ $currency ] is not valid currency",
        instance = s"/rates"
      )
    case Error.RequiredParamIsMissing(param) =>
      Problem(
        `type` = "https://example.com/probs/required-param-is-missing",
        tittle = "Required parameter is missing",
        status = 400,
        detail = s"Required parameter [ $param ] is missing",
        instance = s"/rates"
      )
    case Error.CouldNotCheckRateOfSameCurrencies =>
      Problem(
        `type` = "https://example.com/probs/could-not-check-rate-of-same-currencies",
        tittle = "Could not check rates of same currency pair",
        status = 400,
        detail = "[ from ] query param should not equal [ to ] query param",
        instance = s"/rates"
      )
  }

  private[rates] implicit class GetApiResponseOps(val rate: Rate) extends AnyVal {
    def asGetApiResponse: GetApiResponse =
      GetApiResponse(
        from = rate.pair.from,
        to = rate.pair.to,
        price = rate.price,
        timestamp = rate.timestamp
      )
  }
}
