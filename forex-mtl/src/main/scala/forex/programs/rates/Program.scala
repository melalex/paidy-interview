package forex.programs.rates

import cats.Monad
import cats.data.EitherT
import forex.domain._
import forex.programs.rates.errors.Error.{ RequiredParamIsMissing, UnknownCurrency }
import forex.programs.rates.errors.{ toProgramError, _ }
import forex.services.RatesService

class Program[F[_]: Monad](
    ratesService: RatesService[F]
) extends Algebra[F] {

  override def get(request: Protocol.GetRatesRequest): F[Error Either Rate] = {
    val result = for {
      fromStr <- EitherT.fromEither[F](request.from.toRight(RequiredParamIsMissing("from")))
      toStr <- EitherT.fromEither[F](request.to.toRight(RequiredParamIsMissing("to")))
      from <- EitherT.fromEither[F](Currency.fromString(fromStr)(UnknownCurrency))
      to <- EitherT.fromEither[F](Currency.fromString(toStr)(UnknownCurrency))
      rate <- EitherT(ratesService.get(Rate.Pair(from, to))).leftMap(toProgramError)
    } yield rate

    result.value
  }
}

object Program {

  def apply[F[_]: Monad](
      ratesService: RatesService[F]
  ): Algebra[F] = new Program[F](ratesService)
}
