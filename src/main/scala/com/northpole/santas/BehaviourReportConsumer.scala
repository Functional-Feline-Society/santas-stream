package com.northpole.santas

import cats.effect.IO
import fs2.kafka._
import fs2.kafka.RecordDeserializer
import fs2.kafka.vulcan._
import BehaviourReportConsumer.BehaviourReportTopic
import cats.effect.kernel.Resource

import scala.concurrent.duration.DurationInt

case class BehaviourReportConsumer(config: KafkaConfig) {

  implicit val fullNameDeserializer: RecordDeserializer[IO, FullName] =
    avroDeserializer[FullName].using(config.avroSettings)

  implicit val behaviourReportDeserializer: RecordDeserializer[IO, BehaviourReport] =
    avroDeserializer[BehaviourReport].using(config.avroSettings)

  private val behaviourConsumerSettings =
    ConsumerSettings[IO, FullName, BehaviourReport]
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withBootstrapServers(config.bootstrapServers)
      .withGroupId("Rudolph")

  def consumeWith(
      processReport: (FullName, BehaviourReport) => IO[Unit]
  ): Resource[IO, fs2.Stream[IO, Unit]] =
    KafkaConsumer
      .resource(behaviourConsumerSettings)
      .evalTap(
        _.subscribeTo(BehaviourReportTopic)
      )
      .map(
        _.stream
          .evalTap(r => processReport(r.record.key, r.record.value))
          .map(_.offset)
          .through(commitBatchWithin(500, 15.seconds))
      )

}

object BehaviourReportConsumer {
  val BehaviourReportTopic = "behaviour_reports"
}
