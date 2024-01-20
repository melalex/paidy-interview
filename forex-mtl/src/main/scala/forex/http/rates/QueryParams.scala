package forex.http.rates

import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher

object QueryParams {

  object FromQueryParam extends OptionalQueryParamDecoderMatcher[String]("from")
  object ToQueryParam extends OptionalQueryParamDecoderMatcher[String]("to")
}
