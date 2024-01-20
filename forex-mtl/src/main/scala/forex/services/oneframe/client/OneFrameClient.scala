package forex.services.oneframe.client

import forex.services.oneframe.client.OneFrameProtocol.{ CurrencyExchangeRateDto, CurrencyPairDto }
import forex.services.oneframe.errors.Error

trait OneFrameClient[F[_]] {

  def getExchangeRates(pairs: Iterable[CurrencyPairDto]): F[Error Either Seq[CurrencyExchangeRateDto]]
}
