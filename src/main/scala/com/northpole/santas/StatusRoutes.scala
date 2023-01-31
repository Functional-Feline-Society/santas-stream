package com.northpole.santas

import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.io._

object StatusRoutes {
  val route: HttpRoutes[IO] = HttpRoutes.of[IO] { case GET -> Root / "status" => Ok() }
}
