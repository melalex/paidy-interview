package forex.services.oneframe.client

import cats.Show
import forex.services.oneframe.client.OneFrameProtocol.CurrencyDto.CurrencyDto
import forex.services.oneframe.client.OneFrameProtocol.{ CurrencyDto, CurrencyExchangeRateDto }
import io.circe.{ Decoder, Encoder }
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{ deriveConfiguredDecoder, deriveConfiguredEncoder }

import java.time.OffsetDateTime
import scala.util.Try

trait OneFrameProtocol {

  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames

  implicit lazy val currencyDtoDecoder: Decoder[CurrencyDto] = Decoder.decodeString.emapTry(CurrencyDto.fromString)
  implicit lazy val offsetDateTimeDecoder: Decoder[OffsetDateTime] =
    Decoder.decodeString.emapTry((value: String) => Try(OffsetDateTime.parse(value)))

  implicit lazy val currencyExchangeRateDtoDecoder: Decoder[CurrencyExchangeRateDto] =
    deriveConfiguredDecoder[CurrencyExchangeRateDto]

  implicit lazy val currencyDtoEncoder: Encoder[CurrencyDto] = Encoder.encodeString.contramap[CurrencyDto](_.code)
  implicit lazy val offsetDateTimeEncoder: Encoder[OffsetDateTime] =
    Encoder.encodeString.contramap[OffsetDateTime](_.toString)

  implicit lazy val currencyExchangeRateDtoEncoder: Encoder[CurrencyExchangeRateDto] =
    deriveConfiguredEncoder[CurrencyExchangeRateDto]
}

object OneFrameProtocol extends OneFrameProtocol {
  object CurrencyDto extends Enumeration {

    type CurrencyDto = Value

    protected case class CurrencyVal(code: String) extends super.Val

    implicit def valueToPlanetVal(x: Value): CurrencyVal = x.asInstanceOf[CurrencyVal]

    final val Aud = CurrencyVal("AUD")
    final val Cad = CurrencyVal("CAD")
    final val Chf = CurrencyVal("CHF")
    final val Eur = CurrencyVal("EUR")
    final val Gbp = CurrencyVal("GBP")
    final val Nzd = CurrencyVal("NZD")
    final val Jpy = CurrencyVal("JPY")
    final val Sgd = CurrencyVal("SGD")
    final val Usd = CurrencyVal("USD")

    private val codeToCurrency = values.map(it => it.code -> it).toMap

    implicit val show: Show[CurrencyDto] = Show.show(_.code)

    def fromString(str: String): Try[CurrencyDto] =
      codeToCurrency
        .get(str)
        .toRight(new IllegalArgumentException(s"[ $str ] is not valid currency"))
        .toTry
  }

  case class CurrencyPairDto(from: CurrencyDto, to: CurrencyDto)

  case class CurrencyExchangeRateDto(
      from: CurrencyDto,
      to: CurrencyDto,
      bid: BigDecimal,
      ask: BigDecimal,
      price: BigDecimal,
      timeStamp: OffsetDateTime
  )
}
