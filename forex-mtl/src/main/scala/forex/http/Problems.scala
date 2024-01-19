package forex.http

import cats.arrow.FunctionK
import cats.{Applicative, ~>}
import forex.http.Problems.{Problem, Problematic, SimpleEntityResponseGenerator}
import io.circe.Encoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder
import org.http4s.{Response, Status}
import org.http4s.Status.InternalServerError
import org.http4s.dsl.impl.EntityResponseGenerator

trait Problems {

  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames

  implicit val problemEncoder: Encoder[Problem] = deriveConfiguredEncoder[Problem]

  def internalServerProblem(instance: String): Problem = Problem(
    `type` = "https://example.com/probs/internal-server-problem",
    tittle = "Internal Server Error",
    status = 500,
    detail = "Internal Server Error",
    instance = instance
  )

  def problemResponse[F[_]: Applicative, A: Problematic](a: A): F[Response[F]] = {
    val problem = Problematic[A].asProblem(a)
    val status  = Status.fromInt(problem.status).getOrElse(InternalServerError)

    new SimpleEntityResponseGenerator[F, F](status, FunctionK.id)(problem)
  }
}

object Problems extends Problems {

  case class Problem(
      `type`: String,
      tittle: String,
      status: Int,
      detail: String,
      instance: String
  )

  trait Problematic[A] {

    def asProblem(a: A): Problem
  }

  object Problematic {

    def apply[A](implicit problematic: Problematic[A]): Problematic[A] = problematic
  }

  private class SimpleEntityResponseGenerator[F[_], G[_]](val status: Status, val liftG: G ~> F)
      extends EntityResponseGenerator[F, G] {}
}
