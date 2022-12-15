package com.northpole.santas

import cats.effect.IO
import cats.effect.kernel.Resource
import com.northpole.santas.AddressUpdateConsumer.AddressUpdateReportTopic
import fs2.kafka.{
  AutoOffsetReset,
  ConsumerSettings,
  KafkaConsumer,
  RecordDeserializer,
  commitBatchWithin
}
import fs2.kafka.vulcan.avroDeserializer

import scala.concurrent.duration.DurationInt

case class AddressUpdateConsumer(config: KafkaConfig) {

  implicit private val fullNameDeserializer: RecordDeserializer[IO, FullName] =
    avroDeserializer[FullName].using(config.avroSettings)

  implicit private val addressUpdateDeserializer
      : RecordDeserializer[IO, Address] =
    avroDeserializer[Address].using(config.avroSettings)

  private val addressUpdateConsumerSettings =
    ConsumerSettings[IO, FullName, Address]
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withBootstrapServers(config.bootstrapServers)
      .withGroupId("Rudolph")

  def consumeWith(
      processAddressUpdate: AddressUpdate => IO[Unit]
  ): Resource[IO, fs2.Stream[IO, Unit]] =
    KafkaConsumer
      .resource(addressUpdateConsumerSettings)
      .evalTap(
        _.subscribeTo(AddressUpdateReportTopic)
      )
      .map(
        _.stream
          .evalTap(r =>
            processAddressUpdate(AddressUpdate(r.record.key, r.record.value))
          )
          .map(_.offset)
          .through(commitBatchWithin(500, 15.seconds))
      )
}

object AddressUpdateConsumer {
  val AddressUpdateReportTopic = "address_update"
}
