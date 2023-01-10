package com.northpole.santas

import cats.effect.IO
import cats.implicits.catsSyntaxOptionId
import com.northpole.santas.ChristmasPresent.Nice
import org.http4s._
import org.http4s.implicits._
import munit.CatsEffectSuite
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder

import java.net.URLEncoder

class SantasListSpec extends CatsEffectSuite {
  private val BonesFirstName = "Bones"
  private val BonesLastName  = "Court"

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

  private val stubbedAddressBook = new SantasAddressBookService {
    override def updateAddress(addressUpdate: AddressUpdate): IO[Unit] = ???
    override def getAddress(fullName: FullName): IO[Option[Address]]   = ???

    override def getHouseholdList(address: Address): IO[List[FullName]] = IO.pure(List(TestFullName))
  }

  private[this] val listResponse: IO[Response[IO]] = {
    val uri   = Uri.unsafeFromString(s"/list/$BonesLastName/$BonesFirstName")
    val getHW = Request[IO](Method.GET, uri)
    SantasRoutes.ledger(stubbedAddressBook, stubbedConsignmentService).orNotFound(getHW)
  }

  test("GET route for a gift recipient returns OK") {
    assertIO(listResponse.map(_.status), Status.Ok)
  }

  test("GET route for a gift recipient returns a christmas consignment") {
    assertIO(
      listResponse.flatMap(_.as[ChristmasConsignment]),
      expectedConsignment
    )
  }

  private[this] val householdResponse: IO[Response[IO]] = {
    val address          = URLEncoder.encode(TestAddress.get.address, "UTF-8")
    val uri              = Uri.unsafeFromString(s"/house/${address}")
    val householdRequest = Request[IO](Method.GET, uri)
    SantasRoutes.ledger(stubbedAddressBook, stubbedConsignmentService).orNotFound(householdRequest)
  }

  test("GET route for a household returns OK") {
    assertIO(householdResponse.map(_.status), Status.Ok)
  }

  test("GET route for a gift recipient returns a christmas consignment") {
    assertIO(
      householdResponse.flatMap(_.as[List[FullName]]),
      List(TestFullName)
    )
  }

}
