package weaver

import cats.effect.{ IO, Resource }

private[weaver] trait BaseCatsSuite extends EffectSuite.Provider[IO]

abstract class MutableIOSuite
    extends MutableFSuite[IO]
    with BaseCatsSuite
    with Expectations.Helpers {
  implicit protected def effectCompat: UnsafeRun[IO] = CatsUnsafeRun
  def getSuite: RunnableSuite[IO]                      = this
}

abstract class SimpleMutableIOSuite extends MutableIOSuite {
  type Res = Unit
  def sharedResource: Resource[IO, Unit] = Resource.pure[IO, Unit](())
}

trait FunSuiteIO extends FunSuiteF[IO] with BaseCatsSuite with Expectations.Helpers {
  implicit protected def effectCompat: UnsafeRun[IO] = CatsUnsafeRun
  def getSuite: RunnableSuite[IO]                              = this
}
