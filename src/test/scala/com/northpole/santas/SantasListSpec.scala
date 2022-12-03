package com.northpole.santas

import cats.effect.IO
import org.http4s._
import org.http4s.implicits._
import munit.CatsEffectSuite

// www.santas.com/list/:household/:name
class SantasListSpec extends CatsEffectSuite {

  test("should return naughty or nice") {
    assertIO(retStatus.map(_.status), Status.Ok)
  }

  test("HelloWorld returns hello world message") {
    assertIO(retStatus.flatMap(_.as[String]), "Noce")
  }

  private[this] val retStatus: IO[Response[IO]] = {
    val getHW = Request[IO](Method.GET, uri"/list/court/elias")
    SantasRoutes.listRoutes.orNotFound(getHW)
  }
}
