package forex.services.oneframe

import cats.effect.{ Concurrent, Timer }
import forex.config.OneFrameConfig
import forex.services.oneframe.client.impl.Http4sOneFrameClient
import forex.services.oneframe.interpreters._
import org.http4s.client.Client

object Interpreters {

  def cached[F[_]: Concurrent: Timer](http4s: Client[F], config: OneFrameConfig): F[OneFrameCachingMiddleware[F]] =
    OneFrameCachingMiddleware(live(http4s, config))

  private def live[F[_]: Concurrent: Timer](http4s: Client[F], config: OneFrameConfig): Algebra[F] =
    new OneFrameLive(Http4sOneFrameClient[F](http4s, config))
}
