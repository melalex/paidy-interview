package forex.util

import cats.Applicative
import cats.effect.Sync

import java.time.{ Clock, Instant }

trait TimeProvider[F[_]] {

  def now: F[Instant]
}

object TimeProvider {

  def real[F[_]](clock: Clock = Clock.systemUTC())(implicit F: Sync[F]): TimeProvider[F] = new TimeProvider[F] {
    override def now: F[Instant] = F.delay(clock.instant())
  }

  def fixed[F[_]](instant: Instant)(implicit F: Applicative[F]): TimeProvider[F] = new TimeProvider[F] {
    override def now: F[Instant] = F.pure(instant)
  }
}
