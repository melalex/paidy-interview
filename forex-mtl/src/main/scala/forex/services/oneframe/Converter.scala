package forex.services.oneframe

import forex.domain.Currency.Currency
import forex.domain.Rate.Pair
import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.services.oneframe.client.OneFrameProtocol.CurrencyDto.CurrencyDto
import forex.services.oneframe.client.OneFrameProtocol.{ CurrencyDto, CurrencyExchangeRateDto, CurrencyPairDto }

trait Converter {

  def convertToOneFramePair(source: Pair): CurrencyPairDto = CurrencyPairDto(
    convertToOneFrameCurrency(source.from),
    convertToOneFrameCurrency(source.to)
  )

  def convertToRate(source: CurrencyExchangeRateDto): Rate = Rate(
    pair = Pair(convertToCurrency(source.from), convertToCurrency(source.to)),
    price = Price(source.price),
    timestamp = Timestamp(source.timeStamp)
  )

  private def convertToOneFrameCurrency(source: Currency): CurrencyDto = source match {
    case Currency.Aud => CurrencyDto.Aud
    case Currency.Cad => CurrencyDto.Cad
    case Currency.Chf => CurrencyDto.Chf
    case Currency.Eur => CurrencyDto.Eur
    case Currency.Gbp => CurrencyDto.Gbp
    case Currency.Nzd => CurrencyDto.Nzd
    case Currency.Jpy => CurrencyDto.Jpy
    case Currency.Sgd => CurrencyDto.Sgd
    case Currency.Usd => CurrencyDto.Usd
  }

  private def convertToCurrency(source: CurrencyDto): Currency = source match {
    case CurrencyDto.Aud => Currency.Aud
    case CurrencyDto.Cad => Currency.Cad
    case CurrencyDto.Chf => Currency.Chf
    case CurrencyDto.Eur => Currency.Eur
    case CurrencyDto.Gbp => Currency.Gbp
    case CurrencyDto.Nzd => Currency.Nzd
    case CurrencyDto.Jpy => Currency.Jpy
    case CurrencyDto.Sgd => Currency.Sgd
    case CurrencyDto.Usd => Currency.Usd
  }
}

object Converter extends Converter
