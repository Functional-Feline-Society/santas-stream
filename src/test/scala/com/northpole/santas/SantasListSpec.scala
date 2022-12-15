package com.northpole.santas

import cats.effect.IO
import cats.implicits.catsSyntaxOptionId
import com.northpole.santas.ChristmasPresent.Nice
import org.http4s._
import org.http4s.implicits._
import munit.CatsEffectSuite
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder

class SantasListSpec extends CatsEffectSuite {
  private val BonesFirstName = "Bones"
  private val BonesLastName = "Court"

  private val TestFullName = FullName(BonesFirstName, BonesLastName)
  private val TestAddress = Address(
    "671 Lincoln Ave, Winnetka, IL 60093-2345"
  ).some
  private val Gift = Nice(uri"Bones_favourite.gif", 10)
  private val expectedConsignment = ChristmasConsignment(
    TestFullName,
    TestAddress,
    Gift
  )

  private val stubbedConsignmentService = new ConsignmentService {
    override def getConsignment(fullName: FullName): IO[ChristmasConsignment] =
      IO.pure(expectedConsignment)
  }

  private[this] val response: IO[Response[IO]] = {
    val uri = Uri.unsafeFromString(s"/list/$BonesLastName/$BonesFirstName")
    val getHW = Request[IO](Method.GET, uri)
    SantasRoutes.ledger(stubbedConsignmentService).orNotFound(getHW)
  }

  test("GET route for a gift recipient returns OK") {
    assertIO(response.map(_.status), Status.Ok)
  }

  test("GET route for a gift recipient returns a christmas consignment") {
    assertIO(
      response.flatMap(_.as[ChristmasConsignment]),
      expectedConsignment
    )
  }

}
