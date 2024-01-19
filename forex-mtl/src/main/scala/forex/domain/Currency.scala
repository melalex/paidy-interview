package forex.domain

import cats.Show

object Currency extends Enumeration {

  type Currency = Value

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

  implicit val show: Show[Currency] = Show.show(_.code)

  def fromString[E](str: String)(err: String => E): Either[E, Currency] =
    codeToCurrency.get(str).toRight(err(str))
}
