package forex.http

import cats.Monad
import cats.data.{ Kleisli, OptionT }
import org.http4s.dsl.Http4sDsl
import org.http4s.{ Request, Response }

object KleisliCustomSyntax {

  implicit def http4sKleisliResponseCustomSyntaxOptionT[F[_]: Monad](
      kleisli: Kleisli[OptionT[F, *], Request[F], Response[F]]
  ): KleisliCustomResponseOps[F] =
    new KleisliCustomResponseOps[F](kleisli)

  final class KleisliCustomResponseOps[F[_]: Monad](self: Kleisli[OptionT[F, *], Request[F], Response[F]])
      extends Http4sDsl[F] {

    def orNotFoundProblem: Kleisli[F, Request[F], Response[F]] =
      Kleisli(a => self.run(a).getOrElseF(NotFound(Problems.notFound(a.uri.path.renderString))))
  }
}
