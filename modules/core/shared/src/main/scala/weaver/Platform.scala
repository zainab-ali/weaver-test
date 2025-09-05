package weaver

private[weaver] sealed abstract class Platform

private[weaver] object Platform {
  def isJVM: Boolean    = PlatformCompat.platform == JVM
  def isJS: Boolean     = PlatformCompat.platform == JS
  def isNative: Boolean = PlatformCompat.platform == Native
  def isScala3: Boolean = ScalaCompat.isScala3

  case object JS     extends Platform
  case object JVM    extends Platform
  case object Native extends Platform
}
