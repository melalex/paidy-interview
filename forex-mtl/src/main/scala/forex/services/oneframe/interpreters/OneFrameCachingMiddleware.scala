package forex.services.oneframe.interpreters

import cats.data.EitherT
import cats.effect.concurrent.Ref
import cats.effect.{Clock, Sync}
import cats.implicits._
import forex.domain.Rate.Pair
import forex.domain.{Currency, Rate}
import forex.services.oneframe.RefreshableCache
import forex.services.oneframe.errors.Error
import forex.services.oneframe.errors.Error.OneFrameLookupFailed
import forex.services.oneframe.interpreters.OneFrameCachingMiddleware.AllCurrencyPairs
import forex.services.{OneFrameService, oneframe}
import forex.util.Logging

import java.util.concurrent.TimeUnit

class OneFrameCachingMiddleware[F[_]] private (delegate: oneframe.Algebra[F])(
    cache: Ref[F, Error Either Map[Pair, Rate]]
)(implicit F: Sync[F], clock: Clock[F])
    extends oneframe.Algebra[F]
    with RefreshableCache[F]
    with Logging {

  def getExchangeRates(pairs: Iterable[Pair]): F[Error Either Map[Pair, Rate]] =
    EitherT(cache.get)
      .map(_.view.filterKeys(pairs.toSet).toMap)
      .value

  override def refreshCache(): F[Error Either Unit] =
    for {
      _ <- F.delay(logger.debug("Starting rates cache refresh"))
      start <- clock.realTime(TimeUnit.MILLISECONDS)
      update <- delegate.getExchangeRates(AllCurrencyPairs)
      finish <- clock.realTime(TimeUnit.MILLISECONDS)
      _ <- cache.set(update)
      _ <- logCacheRefreshFinished(update, finish - start)
    } yield update.map(_ => ())

  private def logCacheRefreshFinished(result: Either[Error, Map[Pair, Rate]], msSpent: Long) = F.delay {
    result match {
      case Left(OneFrameLookupFailed(msg)) =>
        logger.error(s"Failed to refresh rates cache after [ $msSpent ] ms. Reason: $msg")
      case Right(value) =>
        logger.debug(
          s"Rates cache refresh has been finished successfully. [ ${value.size} ] values has been cached in [ $msSpent ]."
        )
    }
  }
}

object OneFrameCachingMiddleware {

  private val AllCurrencyPairs = Currency.values.view
    .flatMap(a => Currency.values.filter(_ != a).map(b => a -> b))
    .map { case (from, to) => Pair(from, to) }
    .toSeq

  def apply[F[_]: Sync: Clock](delegate: OneFrameService[F]): F[OneFrameCachingMiddleware[F]] =
    Ref
      .of(Map.empty[Pair, Rate].asRight[Error])
      .map(new OneFrameCachingMiddleware(delegate)(_))
      .flatMap(it => it.refreshCache().as(it))
}
