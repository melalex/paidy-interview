package forex.services.oneframe

import forex.domain.Rate
import forex.domain.Rate.Pair
import forex.services.oneframe.errors.Error

trait Algebra[F[_]] {

  def getExchangeRates(pairs: Iterable[Pair]): F[Error Either Map[Pair, Rate]]
}
