package weaver

import scala.concurrent.Future

import cats.effect.unsafe.implicits.global
import cats.effect.{ FiberIO, IO }

object CatsUnsafeRun extends CatsUnsafeRun

trait CatsUnsafeRun extends UnsafeRun[IO] with CatsUnsafeRunPlatformCompat {

  type CancelToken = FiberIO[Unit]

  override implicit val parallel = IO.parallelForIO
  override implicit val effect   = IO.asyncForIO

  def cancel(token: CancelToken): Unit = token.cancel.unsafeRunSync()

  def unsafeRunToFuture(task: IO[Unit]): Future[Unit] = task.unsafeToFuture()

}
