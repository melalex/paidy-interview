package forex

package object services {

  type RatesService[F[_]]   = rates.Algebra[F]
  type OneFrameService[F[_]] = oneframe.Algebra[F]
  type RatesError           = rates.errors.Error
  type OneFrameError        = oneframe.errors.Error

  final val RatesServices  = rates.Interpreters
  final val OneFrameClient = oneframe.Interpreters
}
