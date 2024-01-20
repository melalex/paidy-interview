package forex.services.rates

import cats.Monad
import forex.config.RatesConfig
import forex.services.OneFrameService
import forex.services.rates.interpreters._
import forex.util.TimeProvider

object Interpreters {

  def live[F[_]: Monad](client: OneFrameService[F], config: RatesConfig, timeProvider: TimeProvider[F]) =
    new OneFrameRatesLive(client, config, timeProvider)
}
