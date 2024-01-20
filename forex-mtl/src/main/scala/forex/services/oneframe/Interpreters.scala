package forex.services.oneframe

import cats.effect.{ Concurrent, Timer }
import forex.config.OneFrameConfig
import forex.services.oneframe.client.impl.Http4sOneFrameClient
import forex.services.oneframe.interpreters._
import forex.util.TimeProvider
import org.http4s.client.Client

object Interpreters {

  def cached[F[_]: Concurrent: Timer](http4s: Client[F],
                                      config: OneFrameConfig,
                                      timeProvider: TimeProvider[F]): F[OneFrameCachingMiddleware[F]] =
    OneFrameCachingMiddleware(live(http4s, config), timeProvider)

  private def live[F[_]: Concurrent: Timer](http4s: Client[F], config: OneFrameConfig): Algebra[F] =
    new OneFrameLive(Http4sOneFrameClient[F](http4s, config))
}
