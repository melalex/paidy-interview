package forex.util

import cats.Applicative
import cats.effect.{Concurrent, Sync}
import org.http4s.Status.Successful
import org.http4s.client.Client
import org.http4s.client.middleware.ResponseLogger
import org.http4s.{Charset, MediaType, Response}
import org.slf4j.Logger

object LogErrorResponses {

  def apply[F[_]: Concurrent](client: Client[F])(implicit logger: Logger): Client[F] =
    ResponseLogger.customized(client, logBody = true, Some(logErrorAction))(responseToText(_))

  private def responseToText[F[_]: Concurrent](resp: Response[F])(implicit F: Applicative[F]): F[String] = resp match {
    case Successful(_) => F.pure("")
    case it            => responseBodyToString(it)
  }

  private def responseBodyToString[F[_]: Concurrent](resp: Response[F]) = {
    val isBinary = resp.contentType.exists(_.mediaType.binary)
    val isJson = resp.contentType.exists(
      mT => mT.mediaType == MediaType.application.json || mT.mediaType.subType.endsWith("+json")
    )
    val bodyStream = if (!isBinary || isJson) {
      resp.bodyText(implicitly, resp.charset.getOrElse(Charset.`UTF-8`))
    } else {
      resp.body.map(b => java.lang.Integer.toHexString(b & 0xff))
    }

    bodyStream.compile.string
  }

  private def logErrorAction[F[_]](implicit F: Sync[F], logger: Logger): String => F[Unit] = str =>
    if (str.isEmpty) F.unit
    else F.delay(logger.error(s"Received an error response: $str"))
}
