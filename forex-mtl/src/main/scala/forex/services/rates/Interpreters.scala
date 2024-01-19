package forex.services.rates

import cats.effect.Clock
import cats.{ Applicative, Monad }
import forex.config.RatesConfig
import forex.services.OneFrameService
import forex.services.rates.interpreters._

object Interpreters {

  def dummy[F[_]: Applicative]: Algebra[F] = new OneFrameDummy()

  def live[F[_]: Monad: Clock](client: OneFrameService[F], config: RatesConfig) = new OneFrameRatesLive(client, config)
}
