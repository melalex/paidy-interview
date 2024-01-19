package forex.services.oneframe.interpreters

import cats.Functor
import cats.data.EitherT
import forex.domain.Rate
import forex.services.oneframe
import forex.services.oneframe.Converter.{convertToOneFramePair, convertToRate}
import forex.services.oneframe.client.OneFrameClient
import forex.services.oneframe.errors

class OneFrameLive[F[_]: Functor](client: OneFrameClient[F]) extends oneframe.Algebra[F] {

  override def getExchangeRates(pairs: Iterable[Rate.Pair]): F[Either[errors.Error, Map[Rate.Pair, Rate]]] = {
    val oneFramePairs = pairs.map(convertToOneFramePair)

    EitherT(client.getExchangeRates(oneFramePairs))
      .map(_.map(convertToRate).map(it => it.pair -> it).toMap)
      .value
  }
}
