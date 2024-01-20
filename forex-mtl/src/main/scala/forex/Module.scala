package forex

import cats.effect.{Concurrent, ConcurrentEffect, Timer}
import forex.config.ApplicationConfig
import forex.http.HttpErrorHandler
import forex.http.KleisliCustomSyntax._
import forex.http.rates.RatesHttpRoutes
import forex.programs._
import forex.services._
import forex.services.oneframe.RefreshableCache
import forex.util.TimeProvider
import org.http4s._
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.server.middleware.{AutoSlash, Timeout}

import scala.concurrent.ExecutionContext

class Module[F[_]: Concurrent: Timer](config: ApplicationConfig,
                                      ratesService: RatesService[F],
                                      refreshableCache: RefreshableCache[F]) {

  private type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  private type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService)

  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

  private val routesMiddleware: PartialMiddleware = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    }
  }

  private val httpErrorHandler = HttpErrorHandler[F]

  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] =>
    Timeout(config.http.timeout)(http)
  }

  private val http: HttpRoutes[F] = ratesHttpRoutes

  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(httpErrorHandler(http)).orNotFoundProblem)

  val streamApp: fs2.Stream[F, Unit] = fs2.Stream
    .awakeDelay[F](config.oneFrame.cache.ttl)
    .evalMap(_ => refreshableCache.refreshCache())
    .map(_ => ())
}

object Module {

  def apply[F[_]: ConcurrentEffect: Timer](
      config: ApplicationConfig,
  )(implicit ec: ExecutionContext): fs2.Stream[F, Module[F]] = apply(config, TimeProvider.real())

  def apply[F[_]: ConcurrentEffect: Timer](
      config: ApplicationConfig,
      timeProvider: TimeProvider[F]
  )(implicit ec: ExecutionContext): fs2.Stream[F, Module[F]] =
    fs2.Stream
      .resource(BlazeClientBuilder[F](ec).resource)
      .evalMap(it => OneFrameClient.cached(it, config.oneFrame, timeProvider))
      .map(it => new Module(config, RatesServices.live(it, config.rates, timeProvider), it))
}
