package com.northpole.santas

import cats.implicits._
import cats.effect.{IO, IOApp}
import cats.effect.std.Env
import fs2.kafka.vulcan.{AvroSettings, SchemaRegistryClientSettings}

object Main extends IOApp.Simple {
  override def run: IO[Unit] = for {
    _         <- IO.println("Loading config from ENV...")
    kafkaConf <- loadKafkaConfigFromEnv
    _         <- IO.println("Starting service...")
    _         <- SantasServer.resource(kafkaConf).useForever.void
  } yield ()

  private val loadKafkaConfigFromEnv = for {
    maybeBootstrapServers  <- Env[IO].get("BOOTSTRAP_SERVERS")
    maybeSchemaRegistryUrl <- Env[IO].get("SCHEMA_REGISTRY_URL")
    kafkaConf <- (maybeBootstrapServers, maybeSchemaRegistryUrl)
      .mapN((bootstrapServers, schemaRegistryUrl) =>
        KafkaConfig(bootstrapServers, AvroSettings(SchemaRegistryClientSettings[IO](schemaRegistryUrl)))
      )
      .liftTo[IO](MissingConfig)
  } yield kafkaConf

  case object MissingConfig extends Throwable

}
