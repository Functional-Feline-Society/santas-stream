package com.northpole.santas

import cats.effect.IO
import org.http4s._
import org.http4s.implicits._
import munit.CatsEffectSuite

class SantasListSpec extends CatsEffectSuite {

  test("GET route for a gift recipient returns OK") {
    assertIO(retStatus.map(_.status), Status.Ok)
  }

  test("GET route for a gift recipient returns 'Nice'") {
    assertIO(retStatus.flatMap(_.as[String]), "Nice")
  }

  private[this] val retStatus: IO[Response[IO]] = {
    val getHW = Request[IO](Method.GET, uri"/list/court/elias")
    SantasRoutes.listRoutes.orNotFound(getHW)
  }
}
