package forex.programs.rates

object Protocol {

  final case class GetRatesRequest(
      from: Option[String],
      to: Option[String]
  )
}
