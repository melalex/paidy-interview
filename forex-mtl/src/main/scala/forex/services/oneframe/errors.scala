package forex.services.oneframe

object errors {

  sealed trait Error

  object Error {
    final case class OneFrameLookupFailed(message: String) extends Error
  }
}
