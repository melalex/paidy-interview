package forex.http

import cats.MonadError
import cats.data.{ Kleisli, OptionT }
import cats.implicits._
import forex.http.Problems.internalServerProblem
import forex.util.Logging
import org.http4s.dsl.Http4sDsl
import org.http4s.{ HttpRoutes, Request, Response }

class HttpErrorHandler[F[_]](implicit F: MonadError[F, Throwable])
    extends (HttpRoutes[F] => HttpRoutes[F])
    with Http4sDsl[F]
    with Logging {

  private def handler(request: Request[F]): Throwable => F[Response[F]] = { ex =>
    F.pure(logger.error("Unexpected exception", ex))
      .flatMap(_ => InternalServerError(internalServerProblem(request.uri.path.renderString)))
  }

  override def apply(routes: HttpRoutes[F]): HttpRoutes[F] = Kleisli { req: Request[F] =>
    OptionT {
      routes.run(req).value.handleErrorWith { e =>
        handler(req)(e).map(Option(_))
      }
    }
  }
}

object HttpErrorHandler {

  def apply[F[_]](implicit F: MonadError[F, Throwable]): HttpErrorHandler[F] = new HttpErrorHandler
}
