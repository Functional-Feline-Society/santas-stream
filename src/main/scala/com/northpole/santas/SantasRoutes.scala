package com.northpole.santas

import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.io._

import java.net.URLDecoder

object SantasRoutes {

  def ledger(addressBookService: SantasAddressBookService, consignmentService: ConsignmentService): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case GET -> Root / "list" / lastName / firstName =>
        Ok(
          consignmentService.getConsignment(FullName(firstName, lastName))
        )
      // TODO: needs encoding and we need to add this to the integration test
      case GET -> Root / "house" / address =>
        // NOTE: IRL we use a different way to identify addresses, but this should do for this toy example
        val decodedAddress = URLDecoder.decode(address, "UTF-8")
        Ok(addressBookService.getHouseholdList(Address(decodedAddress)))

    }
}
