package forex.services.oneframe.interpreters

import cats.data.EitherT
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.implicits._
import forex.domain.Rate.Pair
import forex.domain.{ Currency, Rate }
import forex.services.oneframe.RefreshableCache
import forex.services.oneframe.errors.Error
import forex.services.oneframe.errors.Error.OneFrameLookupFailed
import forex.services.oneframe.interpreters.OneFrameCachingMiddleware.AllCurrencyPairs
import forex.services.{ oneframe, OneFrameService }
import forex.util.{ Logging, TimeProvider }

import java.time.{ Duration => JDuration }
import scala.concurrent.duration.Duration
import scala.jdk.DurationConverters.JavaDurationOps

class OneFrameCachingMiddleware[F[_]] private (delegate: oneframe.Algebra[F], timeProvider: TimeProvider[F])(
    cache: Ref[F, Error Either Map[Pair, Rate]]
)(implicit F: Sync[F])
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
      start <- timeProvider.now
      update <- delegate.getExchangeRates(AllCurrencyPairs)
      finish <- timeProvider.now
      _ <- cache.set(update)
      _ <- logCacheRefreshFinished(update, JDuration.between(start, finish).toScala)
    } yield update.map(_ => ())

  private def logCacheRefreshFinished(result: Either[Error, Map[Pair, Rate]], duration: Duration) = F.delay {
    result match {
      case Left(OneFrameLookupFailed(msg)) =>
        logger.error(s"Failed to refresh rates cache after [ ${duration.toMillis} ] ms. Reason: $msg")
      case Right(value) =>
        logger.info(
          s"Rates cache refresh has been finished successfully. [ ${value.size} ] values has been cached in [ ${duration.toMillis} ] ms."
        )
    }
  }
}

object OneFrameCachingMiddleware {

  final val AllCurrencyPairs = Currency.values.view
    .flatMap(a => Currency.values.filter(_ != a).map(b => a -> b))
    .map { case (from, to) => Pair(from, to) }
    .toSeq

  def apply[F[_]: Sync](delegate: OneFrameService[F], timeProvider: TimeProvider[F]): F[OneFrameCachingMiddleware[F]] =
    Ref
      .of(Map.empty[Pair, Rate].asRight[Error])
      .map(new OneFrameCachingMiddleware(delegate, timeProvider)(_))
      .flatMap(it => it.refreshCache().as(it))
}
