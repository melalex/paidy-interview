package forex.services.oneframe

import forex.services.oneframe.errors.Error

trait RefreshableCache[F[_]] {

  def refreshCache(): F[Error Either Unit]
}
