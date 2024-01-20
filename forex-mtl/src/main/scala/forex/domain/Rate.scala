package forex.domain

import forex.domain.Currency.Currency

case class Rate(
    pair: Rate.Pair,
    price: Price,
    timestamp: Timestamp
)

object Rate {

  final val AllCurrencyPairs = Currency.values.view
    .flatMap(a => Currency.values.filter(_ != a).map(b => a -> b))
    .map { case (from, to) => Pair(from, to) }
    .toSeq

  final case class Pair(
      from: Currency,
      to: Currency
  )
}
