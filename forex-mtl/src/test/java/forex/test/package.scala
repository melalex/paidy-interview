package forex

import cats.ApplicativeError

import java.net.ServerSocket
import scala.util.Using

package object test {

  def freePort[F[_]](implicit F: ApplicativeError[F, Throwable]): F[Int] = F.fromTry {
    Using(new ServerSocket(0)) { socket =>
      socket.setReuseAddress(true)
      socket.getLocalPort
    }
  }
}
