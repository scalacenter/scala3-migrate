package zio

import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.random.Random
import zio.system.System

private[zio] trait PlatformSpecific {
  type ZEnv = Clock with Console with System with Random with Blocking

  object ZEnv {

    private[zio] object Services {
      val live: ZEnv =
        Has.allOf[Clock.Service, Console.Service, System.Service, Random.Service, Blocking.Service](
          Clock.Service.live,
          Console.Service.live,
          System.Service.live,
          Random.Service.live,
          Blocking.Service.live
        )(izumi.reflect.Tag.tagFromTagMacro, izumi.reflect.Tag.tagFromTagMacro, izumi.reflect.Tag.tagFromTagMacro, izumi.reflect.Tag.tagFromTagMacro, izumi.reflect.Tag.tagFromTagMacro)
    }

    val any: ZLayer[ZEnv, Nothing, ZEnv] =
      ZLayer.requires[ZEnv]

    val live: Layer[Nothing, ZEnv] = {
      (Clock.live ++ Console.live ++ System.live ++ Random.live ++ Blocking.live)
    }
  }
}