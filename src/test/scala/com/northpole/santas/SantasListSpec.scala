package com.northpole.santas

import cats.effect.IO
import org.http4s._
import org.http4s.implicits._
import munit.CatsEffectSuite

class SantasListSpec extends CatsEffectSuite {

  private[this] val response: IO[Response[IO]] = {
    val getHW = Request[IO](Method.GET, uri"/list/houseOfCats/bones")
    SantasRoutes.listRoutes.orNotFound(getHW)
  }

  test("GET route for a gift recipient returns OK") {
    assertIO(response.map(_.status), Status.Ok)
  }

  test("GET route for a gift recipient returns 'Nice'") {
    assertIO(response.flatMap(_.as[String]), "Nice")
  }

}
