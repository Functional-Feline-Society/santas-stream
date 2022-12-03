package com.northpole.santas

import cats.effect.{IO, Resource}
import com.comcast.ip4s._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.Server

object SantasServer {

  def resource: Resource[IO, Server] = {
    for {
      _ <- Resource.eval((IO.println("Starting server...")))
//      client <- Stream.resource(EmberClientBuilder.default[IO].build)

      httpApp = (
        SantasRoutes.listRoutes
      ).orNotFound

      server <- EmberServerBuilder
        .default[IO]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"8080")
        .withHttpApp(httpApp)
        .build
    } yield server
  }
}
