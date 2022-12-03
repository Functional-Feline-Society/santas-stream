package com.northpole.santas

import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import cats.effect.IO

import scala.annotation.nowarn

object SantasRoutes {

  @nowarn
  def listRoutes: HttpRoutes[IO] = {
    HttpRoutes.of[IO] { case GET -> Root / "list" / household / name =>
      Ok("Nice")
    }
  }
}
