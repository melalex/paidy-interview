package forex.test

import cats.Applicative
import cats.effect.{ ConcurrentEffect, ContextShift, IO, Resource, Sync, Timer }
import cats.implicits._
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import forex.Module
import forex.config._
import forex.domain.Currency.Currency
import forex.http.Problems.Problem
import forex.http.rates.Protocol.GetApiResponse
import forex.services.oneframe.client.OneFrameProtocol.{ CurrencyExchangeRateDto, CurrencyPairDto }
import forex.util.TimeProvider
import io.circe.syntax.EncoderOps
import org.http4s.Status.Ok
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.client.middleware.Logger
import org.http4s.dsl.io.GET
import org.http4s.{ EntityDecoder, Request, Response, Uri }

import java.time.Instant
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

trait TiFixture {

  private val now = Instant.ofEpochSecond(1705771675)

  protected def withAppUpAndRunning(prepareMock: WireMockEndpoint => IO[Unit])(test: AppEndpoint[IO] => Any): Unit = {
    implicit val ec: ExecutionContext    = ExecutionContext.global
    implicit val shift: ContextShift[IO] = IO.contextShift(ec)
    implicit val timer: Timer[IO]        = IO.timer(ec)

    prepareEnvAndRunTest[IO](prepareMock)(test).unsafeRunSync()
  }

  private def prepareEnvAndRunTest[F[_]: ConcurrentEffect: Timer](prepareMock: WireMockEndpoint => F[Unit])(
      test: AppEndpoint[F] => Any
  )(implicit ec: ExecutionContext) =
    prepareEnv[F](prepareMock).use { endpoint =>
      test(endpoint)
      Applicative[F].unit
    }

  private def prepareEnv[F[_]: ConcurrentEffect: Timer](
      prepareMock: WireMockEndpoint => F[Unit]
  )(implicit ec: ExecutionContext) =
    for {
      oneFrame <- createWireMockServer[F]
      forexPort <- Resource.eval(freePort[F])
      config <- Resource.pure[F, ApplicationConfig](createConfig(forexPort, oneFrame.port()))
      _ <- Resource.eval(prepareMock(new WireMockEndpoint(oneFrame, config)))
      client <- BlazeClientBuilder[F](ec).resource
      module <- Module.resource[F](config, TimeProvider.fixed(now), client)
      _ <- BlazeServerBuilder[F](ec)
            .bindHttp(config.http.port, config.http.host)
            .withHttpApp(module.httpApp)
            .resource
    } yield new AppEndpoint[F](Logger(logHeaders = true, logBody = true)(client), config)

  private def createWireMockServer[F[_]](implicit F: Sync[F]) = {
    def startWireMock(port: Int) = F.delay {
      val server = new WireMockServer(WireMockConfiguration.options.port(port))

      server.start()

      server
    }

    def stopWireMock(it: WireMockServer) = F.delay {
      it.stop()
    }

    for {
      port <- Resource.eval(freePort[F])
      wiremock <- Resource.make(startWireMock(port))(stopWireMock)
    } yield wiremock
  }

  private def createConfig(forexPort: Int, oneFramePort: Int) = ApplicationConfig(
    http = HttpConfig(
      host = "0.0.0.0",
      port = forexPort,
      timeout = 40 seconds
    ),
    oneFrame = OneFrameConfig(
      baseUrl = s"http://localhost:$oneFramePort",
      apiKey = "10dc303535874aeccc86a8251e6992f5",
      cache = OneFrameCacheConfig(
        ttl = 270 seconds
      ),
      retry = OneFrameRetryConfig(
        maxDuration = 5 seconds,
        maxRetries = 5
      )
    ),
    rates = RatesConfig(
      ttl = 5 minutes
    )
  )

  class WireMockEndpoint(oneFrame: WireMockServer, config: ApplicationConfig) {

    def stubOneFrameGetRateResponse(queryParams: Iterable[CurrencyPairDto],
                                    responseBody: Seq[CurrencyExchangeRateDto]): Unit = {
      val queryParamsStr = queryParams.map(_.show).mkString("pair=", "&pair=", "")

      val request = WireMock
        .get(s"/rates?$queryParamsStr")
        .withHeader("token", equalTo(config.oneFrame.apiKey))

      val response = WireMock
        .aResponse()
        .withBody(responseBody.asJson.noSpaces)
        .withHeader("Content-Type", "application/json")
        .withStatus(Ok.code)

      oneFrame.stubFor(request.willReturn(response))
      ()
    }
  }

  class AppEndpoint[F[_]: Sync](client: Client[F], config: ApplicationConfig) extends Http4sClientDsl[F] {

    private val baseUri = Uri.unsafeFromString(s"http://${config.http.host}:${config.http.port}")

    def getExchangeRate(from: Currency, to: Currency): F[Either[Problem, GetApiResponse]] =
      executeRequest(GET(baseUri / "rates" +? ("from", from.code) +? ("to", to.code)))

    private def executeRequest[A](req: Request[F])(implicit decoder: EntityDecoder[F, A],
                                                   problemDecoder: EntityDecoder[F, Problem]) =
      client
        .expectOr[A](req)(onProblem)
        .map(_.asRight[Problem])
        .recover {
          case ProblemException(problem) => problem.asLeft[A]
        }

    private def onProblem(resp: Response[F])(implicit problemDecoder: EntityDecoder[F, Problem]) =
      problemDecoder
        .decode(resp, strict = false)
        .map(ProblemException)
        .leftWiden[Throwable]
        .value
        .flatMap(Sync[F].fromEither)
        .widen[Throwable]

    private case class ProblemException(problem: Problem) extends Exception
  }
}
