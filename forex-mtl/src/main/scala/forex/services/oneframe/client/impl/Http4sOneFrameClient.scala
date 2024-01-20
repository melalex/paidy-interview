package forex.services.oneframe.client.impl

import cats.effect.{ Concurrent, Sync, Timer }
import cats.implicits._
import forex.config.OneFrameConfig
import forex.services.oneframe.client.OneFrameProtocol.{ CurrencyExchangeRateDto, CurrencyPairDto }
import forex.services.oneframe.client.{ OneFrameClient, OneFrameProtocol }
import forex.services.oneframe.client.impl.Http4sOneFrameClient.{ PairParam, TokenHeader }
import forex.services.oneframe.errors.Error
import forex.services.oneframe.errors.Error.OneFrameLookupFailed
import forex.util.{ LogErrorResponses, Logging }
import org.http4s._
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.client.middleware.{ Retry, RetryPolicy }
import org.http4s.implicits.http4sLiteralsSyntax
import org.typelevel.ci.CIStringSyntax

class Http4sOneFrameClient[F[_]: Sync] private (http4s: Client[F], config: OneFrameConfig)
    extends OneFrameClient[F]
    with OneFrameProtocol
    with Logging {

  implicit private val decoder: EntityDecoder[F, Seq[CurrencyExchangeRateDto]] = jsonOf[F, Seq[CurrencyExchangeRateDto]]

  private val ratesPath = Uri
    .unsafeFromString(config.baseUrl)
    .withPath(path"rates")

  override def getExchangeRates(
      pairs: Iterable[CurrencyPairDto]
  ): F[Error Either Seq[CurrencyExchangeRateDto]] =
    http4s
      .expect[Seq[CurrencyExchangeRateDto]](exchangeRatesRequest(pairs))
      .redeem(
        ex => OneFrameLookupFailed(ex.getMessage).asLeft[Seq[CurrencyExchangeRateDto]],
        it => it.asRight[Error]
      )

  private def exchangeRatesRequest(pairs: Iterable[CurrencyPairDto]) = {
    val query = pairs.map(it => it.from.code + it.to.code)

    Request[F](
      uri = ratesPath.withMultiValueQueryParams(Map(PairParam -> query.toList)),
      headers = Headers(Header.Raw(TokenHeader, config.apiKey))
    )
  }
}

object Http4sOneFrameClient extends Logging {

  private val PairParam        = "pair"
  private val TokenHeader      = ci"token"
  private val SensitiveHeaders = Headers.SensitiveHeaders + TokenHeader

  def apply[F[_]: Concurrent: Timer](http4s: Client[F], config: OneFrameConfig): Http4sOneFrameClient[F] = {
    val retryPolicy = RetryPolicy[F](RetryPolicy.exponentialBackoff(config.retry.maxDuration, config.retry.maxRetries))

    new Http4sOneFrameClient(LogErrorResponses(Retry(retryPolicy, SensitiveHeaders)(http4s)), config)
  }

  def createSimple[F[_]: Sync](http4s: Client[F], config: OneFrameConfig) = new Http4sOneFrameClient(http4s, config)
}
