package forex.config

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
    http: HttpConfig,
    oneFrame: OneFrameConfig,
    rates: RatesConfig
)

case class HttpConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration,
)

case class OneFrameConfig(
    baseUrl: String,
    apiKey: String,
    cache: OneFrameCacheConfig,
    retry: OneFrameRetryConfig
)

case class OneFrameCacheConfig(
    ttl: FiniteDuration
)

case class OneFrameRetryConfig(
    maxDuration: FiniteDuration,
    maxRetries: Int
)

case class RatesConfig(
    ttl: FiniteDuration
)
