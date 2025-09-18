package weaver

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

import cats.Parallel
import cats.effect.{ Async }
import cats.syntax.all._

/**
 * Abstraction allowing for running IO constructs unsafely.
 *
 * This is meant to delegate to library-specific constructs for running effect
 * types.
 */
trait UnsafeRun[F[_]] {

  implicit def parallel: Parallel[F]
  implicit def effect: Async[F]
  def realTimeMillis: F[Long]                  = effect.realTime.map(_.toMillis)
  final def sleep(duration: FiniteDuration): F[Unit] = effect.sleep(duration)
  final def fromFuture[A](thunk: => scala.concurrent.Future[A]): F[A] =
    effect.fromFuture(effect.delay(thunk))

  type CancelToken

  def background(task: F[Unit]): CancelToken
  def cancel(token: CancelToken): Unit

  def unsafeRunSync(task: F[Unit]): Unit
  def unsafeRunToFuture(task: F[Unit]): Future[Unit]

}
