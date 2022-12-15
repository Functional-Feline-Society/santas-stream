package com.northpole.santas

import cats.effect.IO
import fs2.kafka.vulcan.AvroSettings

case class KafkaConfig(bootstrapServers: String, avroSettings: AvroSettings[IO])
