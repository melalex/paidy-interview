# Forex-mtl

This project is an implementation of the test task from
Paidy - https://github.com/melalex/paidy-interview/blob/master/Forex.md

## Implementation

This application acts as a proxy for `One Frame API` to reduce the number of calls to it by using a cache. To achieve
this, the following components were added:

- [Http4sOneFrameClient.scala](src/main/scala/forex/services/oneframe/client/impl/Http4sOneFrameClient.scala)
  acts as client class to communicate with `One Frame API`. Current implementation performs retries with exponential
  policy in case of failed requests. Also, it logs error responses. It uses `Http4s` under the hood.
- [OneFrameLive.scala](src/main/scala/forex/services/oneframe/interpreters/OneFrameLive.scala)
  acts as adapter between `One Frame API` and rest of `forex-mtl`. Maps `One Frame API` DTOs to Models.
- [OneFrameCachingMiddleware.scala](src/main/scala/forex/services/oneframe/interpreters/OneFrameCachingMiddleware.scala)
  caches exchange rates provided by `One Frame API`. See [Caching](#caching).
- [OneFrameRatesLive.scala](src/main/scala/forex/services/rates/interpreters/OneFrameRatesLive.scala)
  provides exchange rates. Also, it validates whether rates are outdated.

### Caching

Since the application supports only 9 currencies, it is possible to cache exchange rates for all currency pairs
in a memory (72 records). `forex-mtl` calls `One Frame API` every 270 seconds and caches all supported exchange rates.
In case of an error response, `forex-mtl` retries the request several times. If `One Frame API` still responds with an
error `forex-mtl` will return `Service Unavailable` to clients till the next cache refresh. As result, `forex-mtl` will
do only ~320 requests every day.

In a real environment, it is likely that `forex-mtl` would have more than one instance running. If the instance
count is less or equal to 3 we are still able to use a single `One Frame API key`. But in case of 4 or more instances
we need to have more API keys or distributed cache. The optimal solution with a distributed cache
depends on the environment.

### Error propagation

`forex-mtl` defines following errors:

- `internal-server-problem` - unhandled exception occurred
- `not-found` - requested route is not defined
- `one-frame-is-unavailable` - `One Frame API` responded with an error
- `exchange-rate-is-outdated` - cached exchange rate is outdated
- `unknown-currency` - client supplied not supported currency
- `required-param-is-missing` - client haven't supplied required parameter
- `could-not-check-rate-of-same-currencies` - client requesting exchange rate for same currencies (e. g. USD -> USD)

`forex-mtl` returns errors in [Problem JSON](https://datatracker.ietf.org/doc/html/rfc7807) format:

```json
{
  "type": "https://example.com/probs/not-found",
  "tittle": "Not Found",
  "status": 404,
  "detail": "Not Found",
  "instance": "/abc"
}
```

### API key

API key is supplied for environment variables (see [application.conf](src/main/resources/application.conf)). For real
application it would be better to use something similar to [Vault](https://www.vaultproject.io/) or store
secretes in the encrypted file.

### Test

Test are implemented using `ScalaTest`, `Mockito` and `WireMock`. [RatesApiSuite.scala](src/test/java/forex/RatesApiSuite.scala) 
is an integration test that checks happy path.
