package com.northpole.santas

import cats.effect.IO
import cats.syntax.all._
import com.dimafeng.testcontainers.{GenericContainer, KafkaContainer}
import com.dimafeng.testcontainers.lifecycle.and
import com.dimafeng.testcontainers.munit.TestContainersForEach
import com.northpole.santas.AddressUpdateConsumer.AddressUpdateReportTopic
import com.northpole.santas.BehaviourReportConsumer.BehaviourReportTopic
import fs2.kafka._
import fs2.kafka.vulcan.{AvroSettings, SchemaRegistryClientSettings, avroSerializer}
import munit.CatsEffectSuite
import org.http4s.Uri
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.dsl.io.Path
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.implicits.http4sLiteralsSyntax
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait

import scala.concurrent.duration.{DurationInt, FiniteDuration}

class NaughtyNiceReportSpec extends CatsEffectSuite with TestContainersForEach {
  override type Containers =
    KafkaContainer and GenericContainer

  private val KafkaBrokerPort    = 9092
  private val SchemaRegistryPort = 8081

  private val MegachusFirstName = "Megachu"
  private val MegachusLastName  = "McAts"
  private val MegachusFullName  = FullName(MegachusFirstName, MegachusLastName)
  private val MegachusAddress   = Address("671 Lincoln Ave, Winnetka, IL 60093-2345")

  override def startContainers(): Containers = {
    val network = Network.newNetwork()
    val kafka = KafkaContainer().configure { c =>
      c.withNetwork(network)
      c.addExposedPort(KafkaBrokerPort)
    }

    kafka.start()

    val registry = GenericContainer(
      s"confluentinc/cp-schema-registry:${KafkaContainer.defaultTag}",
      exposedPorts = Seq(SchemaRegistryPort),
      waitStrategy = Wait.forHttp("/"),
      env = Map(
        "SCHEMA_REGISTRY_HOST_NAME"                    -> "schema-registry",
        "SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS" -> s"${kafka.networkAliases.head}:$KafkaBrokerPort",
        "SCHEMA_REGISTRY_LISTENERS"                    -> s"http://0.0.0.0:$SchemaRegistryPort"
      )
    ).configure(_.withNetwork(network): Unit)

    registry.start()

    kafka and registry
  }

  test("behaviour report can be consumed") {
// Setup
    withContainers { case kafkaContainer and registryContainer =>
      val registryPort = registryContainer.mappedPort(SchemaRegistryPort)

      val avroSettings =
        AvroSettings(
          SchemaRegistryClientSettings[IO](s"http://localhost:$registryPort")
        )
      // kafka serialisers
      implicit val fullNameSerializer: RecordSerializer[IO, FullName] =
        avroSerializer[FullName].using(avroSettings)

      implicit val behaviourReportSerializer: RecordSerializer[IO, BehaviourReport] =
        avroSerializer[BehaviourReport].using(avroSettings)

      implicit val addressSerializer: RecordSerializer[IO, Address] =
        avroSerializer[Address].using(avroSettings)

      // kafka producers (needed for test)
      val config = KafkaConfig(kafkaContainer.bootstrapServers, avroSettings)
      val behaviourReportProducer =
        ProducerSettings[IO, FullName, BehaviourReport].withBootstrapServers(
          kafkaContainer.bootstrapServers
        )
      val addressUpdateProducer =
        ProducerSettings[IO, FullName, Address].withBootstrapServers(
          kafkaContainer.bootstrapServers
        )
// Action
      (
        KafkaProducer.resource(behaviourReportProducer),
        KafkaProducer.resource(addressUpdateProducer),
        SantasServer.resource(config)
      ).tupled
        .use { case (behaviourReportProducer, addressUpdateProducer, server) =>
          val ExpectedScore = 7
          for {
            _ <- behaviourReportProducer
              .produceOne_(
                ProducerRecord(
                  BehaviourReportTopic,
                  MegachusFullName,
                  BehaviourReport(ExpectedScore)
                )
              )
              .flatten
            _ <- addressUpdateProducer
              .produceOne_(
                ProducerRecord(
                  AddressUpdateReportTopic,
                  MegachusFullName,
                  MegachusAddress
                )
              )
              .flatten
// Assert
            _ <- eventually(
              assertIO(
                getConsignment(server.baseUri, MegachusFullName),
                ChristmasConsignment(
                  MegachusFullName,
                  MegachusAddress.some,
                  ChristmasPresent.Nice(uri"nice_present", ExpectedScore)
                )
              )
            )
          } yield ()
        }
    }
  }

  private def getConsignment(
      baseUri: Uri,
      fullName: FullName
  ): IO[ChristmasConsignment] =
    EmberClientBuilder.default[IO].build.use { client =>
      client.expect[ChristmasConsignment](
        baseUri.withPath(
          Path.unsafeFromString(
            s"/list/${fullName.lastName}/${fullName.firstName}"
          )
        )
      )
    }

  def eventually[A](io: IO[A]): IO[A] = eventually()(io)

  def eventually[A](n: Int = 10, delay: FiniteDuration = 10.seconds)(retryThis: IO[A]): IO[A] = {
    def aux(m: Int): IO[A] =
      IO.sleep(delay) >> retryThis.handleErrorWith(t =>
        if (m === 1) IO.raiseError(new Exception(s"Failed after $n attempts", t))
        else
          IO.println(s"Trying another ${m - 1}/$n times.\n") >> aux(m - 1)
      )
    retryThis.handleErrorWith(_ => aux(n - 1))
  }

}
