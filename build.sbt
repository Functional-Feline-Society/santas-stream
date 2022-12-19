val CatsVersion            = "2.9.0"
val CatsEffectVersion      = "3.4.2"
val Http4sVersion          = "0.23.16"
val CirceVersion           = "0.14.3"
val MunitVersion           = "0.7.29"
val LogbackVersion         = "1.2.11"
val MunitCatsEffectVersion = "1.0.7"
val Fs2KafkaVersion        = "3.0.0-M9"
val VulcanVersion          = "1.8.4"
val TestContainersVersion  = "0.40.11"
val Fs2Version             = "3.3.0"

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    organization := "com.northpole",
    name         := "santas",
    version      := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.10",
    // Needed to resolve newer versions of kakfa-avro-serializer
    resolvers += "confluent".at("https://packages.confluent.io/maven/"),
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core"   % CatsVersion,
      "org.typelevel" %% "cats-effect" % CatsEffectVersion,
      "co.fs2"        %% "fs2-core"    % Fs2Version,
      // json codec
      "io.circe" %% "circe-generic" % CirceVersion,
      // web server
      "org.http4s" %% "http4s-core"         % Http4sVersion,
      "org.http4s" %% "http4s-server"       % Http4sVersion,
      "org.http4s" %% "http4s-ember-server" % Http4sVersion,
      "org.http4s" %% "http4s-ember-client" % Http4sVersion,
      "org.http4s" %% "http4s-dsl"          % Http4sVersion,
      "io.circe"   %% "circe-core"          % CirceVersion,
      "org.http4s" %% "http4s-circe"        % Http4sVersion,

      // kafka
      "com.github.fd4s" %% "vulcan"           % VulcanVersion,
      "com.github.fd4s" %% "vulcan-generic"   % VulcanVersion,
      "com.github.fd4s" %% "fs2-kafka-vulcan" % Fs2KafkaVersion,
      "com.github.fd4s" %% "fs2-kafka"        % Fs2KafkaVersion,
      // test related
      "org.scalameta" %% "munit"                          % MunitVersion           % "it,test",
      "org.typelevel" %% "munit-cats-effect-3"            % MunitCatsEffectVersion % "it,test",
      "com.dimafeng"  %% "testcontainers-scala-scalatest" % TestContainersVersion  % IntegrationTest,
      "com.dimafeng"  %% "testcontainers-scala-kafka"     % TestContainersVersion  % IntegrationTest,
      "com.dimafeng"  %% "testcontainers-scala-munit"     % TestContainersVersion  % IntegrationTest,
      // Misc
      "ch.qos.logback" % "logback-classic" % LogbackVersion % Runtime
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    Defaults.itSettings
  )
