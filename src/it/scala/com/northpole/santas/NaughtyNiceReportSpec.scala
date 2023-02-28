package com.northpole.santas

import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
import cats.syntax.all._
import com.dimafeng.testcontainers.{Container, KafkaContainer, SchemaRegistryContainer}
import com.northpole.santas.AddressUpdateConsumer.AddressUpdateReportTopic
import com.northpole.santas.BehaviourReportConsumer.BehaviourReportTopic
import com.northpole.santas.NaughtyNiceReportSpec._
import fs2.kafka.vulcan.{AvroSettings, SchemaRegistryClientSettings, avroSerializer}
import vulcan.Codec
import fs2.kafka._
import munit.CatsEffectSuite
import org.http4s.Uri
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.client.Client
import org.http4s.dsl.io.Path
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.server.Server
import org.testcontainers.containers.Network
import retry.RetryPolicies
import retry.RetryPolicies.limitRetries
import retry.implicits._

import java.net.URLEncoder
import scala.concurrent.duration.DurationInt

class NaughtyNiceReportSpec extends CatsEffectSuite {

  private val MegachusFirstName = "Megachu"
  private val MegachusLastName  = "McAts"
  private val MegachusFullName  = FullName(MegachusFirstName, MegachusLastName)
  private val MegachusAddress   = Address("671 Lincoln Ave, Winnetka, IL 60093-2345")

  private val fixture: Fixture[TestDependencies] = ResourceSuiteLocalFixture(
    FixtureName,
    for {
      network <- Resource.fromAutoCloseable(IO(Network.newNetwork()))
      kafka <- resourceFromContainer(
        KafkaContainer().configure(_.withNetwork(network).withNetworkAliases(KafkaHostname): Unit)
      )
      registry <- resourceFromContainer(SchemaRegistryContainer(network, KafkaHostname))
      registryExternalPort = registry.mappedPort(SchemaRegistryContainer.defaultSchemaPort)
      avroSettings =
        AvroSettings(
          SchemaRegistryClientSettings[IO](s"http://localhost:$registryExternalPort")
        )
      kafkaConfig = KafkaConfig(kafka.bootstrapServers, avroSettings)
      behaviourReportProducer <- createProducer[FullName, BehaviourReport](kafka.bootstrapServers, avroSettings)
      addressUpdateProducer   <- createProducer[FullName, Address](kafka.bootstrapServers, avroSettings)
      applicationServer       <- SantasServer.resource(kafkaConfig)
      httpClient              <- EmberClientBuilder.default[IO].build
    } yield TestDependencies(
      behaviourReportProducer,
      addressUpdateProducer,
      applicationServer,
      httpClient
    )
  )

  override def munitFixtures: Seq[Fixture[_]] = List(fixture)

  test("behaviour report can be consumed") {
    val ExpectedScore = 7
    for {
      _ <- fixture().behaviourReportProducer
        .produceOne_(
          ProducerRecord(
            BehaviourReportTopic,
            MegachusFullName,
            BehaviourReport(ExpectedScore)
          )
        )
        .flatten
      _ <- fixture().addressUpdateProducer
        .produceOne_(
          ProducerRecord(
            AddressUpdateReportTopic,
            MegachusFullName,
            MegachusAddress
          )
        )
        .flatten
      // Assert
      megachusHouseInhabitants <- eventually(
        getHouseInhabitants(fixture().applicationServer.baseUri, MegachusAddress)
      )
      _ <- eventually(
        assertIO(
          getConsignment(fixture().applicationServer.baseUri, megachusHouseInhabitants.head),
          ChristmasConsignment(
            MegachusFullName,
            MegachusAddress.some,
            ChristmasPresent.Nice(uri"nice_present", ExpectedScore)
          )
        )
      )
    } yield ()

  }

  private def getConsignment(
      baseUri: Uri,
      fullName: FullName
  ): IO[ChristmasConsignment] =
    fixture().httpClient.expect[ChristmasConsignment](
      baseUri.withPath(
        Path.unsafeFromString(
          s"/list/${fullName.lastName}/${fullName.firstName}"
        )
      )
    )

  private def getHouseInhabitants(
      baseUri: Uri,
      address: Address
  ): IO[NonEmptyList[FullName]] =
    fixture().httpClient.expect[NonEmptyList[FullName]](
      baseUri.withPath(
        Path.unsafeFromString(
          s"/house/${URLEncoder.encode(address.address, "UTF-8")}"
        )
      )
    )

  def eventually[A](io: IO[A]): IO[A] =
    io.retryingOnAllErrors(
      RetryPolicies.constantDelay[IO](10.seconds) join limitRetries(maxRetries = 10),
      (_, retryDetails) =>
        if (retryDetails.givingUp)
          IO.println(s"Failed after ${retryDetails.retriesSoFar} attempts.")
        else
          IO.println(s"Attempt #${retryDetails.retriesSoFar} failed. Trying again in ${retryDetails.upcomingDelay}.")
    )
}

object NaughtyNiceReportSpec {
  private val KafkaHostname = "kafkaBroker"
  private val FixtureName   = "kafka-deps"
  private case class TestDependencies(
      behaviourReportProducer: KafkaProducer[IO, FullName, BehaviourReport],
      addressUpdateProducer: KafkaProducer[IO, FullName, Address],
      applicationServer: Server,
      httpClient: Client[IO]
  )

  private def resourceFromContainer[C <: Container](container: => C): Resource[IO, C] =
    Resource.fromAutoCloseable(IO {
      container
    }.flatTap(c => IO(c.start())))

  private def createProducer[K: Codec, V: Codec](
      bootstrapServers: String,
      avroSettings: AvroSettings[IO]
  ): Resource[IO, KafkaProducer[IO, K, V]] = {
    implicit val keySerializer: RecordSerializer[IO, K] =
      avroSerializer[K].using(avroSettings)

    implicit val valueSerializer: RecordSerializer[IO, V] =
      avroSerializer[V].using(avroSettings)

    KafkaProducer.resource(
      ProducerSettings[IO, K, V].withBootstrapServers(
        bootstrapServers
      )
    )
  }
}
