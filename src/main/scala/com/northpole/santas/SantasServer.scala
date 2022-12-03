package com.northpole.santas

import cats.effect.{IO, Resource}
import com.comcast.ip4s._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.Server
import org.http4s.server.middleware.Logger

object SantasServer {

  def resource: Resource[IO, Server] = {
    for {
      _ <- Resource.eval((IO.println("Starting server...")))
//      client <- Stream.resource(EmberClientBuilder.default[IO].build)

      // Combine Service Routes into an HttpApp.
      // Can also be done via a Router if you
      // want to extract segments not checked
      // in the underlying routes.
      httpApp = (
        SantasRoutes.listRoutes
        ).orNotFound

      // With Middlewares in place
      finalHttpApp = Logger.httpApp(true, true)(httpApp)

      server <- EmberServerBuilder.default[IO]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"8080")
        .withHttpApp(finalHttpApp)
        .build
    } yield server
  }
}
