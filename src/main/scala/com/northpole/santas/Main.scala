package com.northpole.santas

import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    SantasServer.resource.useForever.as(ExitCode.Success)
}
