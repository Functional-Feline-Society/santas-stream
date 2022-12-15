package com.northpole.santas

import cats.effect.{IO, Resource}
import com.comcast.ip4s._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server

object SantasServer {

  def resource(kafkaConfig: KafkaConfig): Resource[IO, Server] = {
    for {
      _ <- Resource.eval((IO.println("Starting server...")))
      addressBook <- Resource.eval(SantasInMemoryAddressBook.default())
      ledger <- Resource.eval(SantasInMemoryLedger.default())
      reportConsumer <- BehaviourReportConsumer(kafkaConfig)
        .consumeWith(ledger.addBehaviourReport)
      addressUpdatesConsumer <- AddressUpdateConsumer(kafkaConfig)
        .consumeWith(addressBook.updateAddress)
      _ <- reportConsumer
        .concurrently(addressUpdatesConsumer)
        .compile
        .drain
        .background
      consignmentService = ConsignmentService(addressBook, ledger)
      httpApp = (
        SantasRoutes.ledger(consignmentService)
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
