package forex.util

import org.slf4j.{Logger, LoggerFactory}

trait Logging {

  implicit protected final val logger: Logger = LoggerFactory.getLogger(getClass)
}
