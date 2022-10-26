name := """dataprofiler-api"""
organization := "com.dataprofiler"

version := "1.0.0"

lazy val root = (project in file(".")).enablePlugins(PlayJava, SwaggerPlugin)

Compile / unmanagedSourceDirectories += baseDirectory.value / "../dp-core/util/src/main/java"

scalaVersion := "2.12.15"

resolvers += Resolver.mavenLocal
resolvers += "Local Maven Repository" at Path.userHome.asFile.toURI.toURL + ".m2/repository"
resolvers += "Secured Central Repository" at "https://repo1.maven.org/maven2"

PlayKeys.devSettings += "play.server.http.idleTimeout" -> "infinite"

swaggerV3 := true
swaggerDomainNameSpaces := Seq("helpers", "controllers", "objects", "com.dataprofiler")

libraryDependencies += filters

libraryDependencies += caffeine

libraryDependencies += guice

libraryDependencies += "javax.mail" % "javax.mail-api" % "1.6.2"

libraryDependencies += "org.postgresql" % "postgresql" % "42.2.18"

libraryDependencies += "com.univocity" % "univocity-parsers" % "2.9.0"

libraryDependencies += "org.apache.accumulo" % "accumulo-core" % "2.0.1"

libraryDependencies += "org.apache.hadoop" % "hadoop-client" % "3.3.4"

libraryDependencies += "com.google.code.gson" % "gson" % "2.8.0"

libraryDependencies += "com.clearspring.analytics" % "stream" % "2.9.5"

libraryDependencies += "com.amazonaws" % "aws-java-sdk" % "1.11.762"

libraryDependencies += "org.xerial.snappy" % "snappy-java" % "1.1.4-M3"

libraryDependencies += "com.nimbusds" % "nimbus-jose-jwt" % "5.3"

libraryDependencies += "io.sentry" % "sentry" % "1.6.4"

libraryDependencies += "org.apache.httpcomponents" % "httpcore" % "4.4.10" // somehow is a depedency to unirest that isn't listed

libraryDependencies += "com.konghq" % "unirest-java" % "3.13.6"

libraryDependencies += "com.konghq" % "unirest-objectmapper-jackson" % "3.13.6"

libraryDependencies += "io.sentry" % "sentry-logback" % "1.7.16" intransitive()

libraryDependencies += "org.webjars" % "swagger-ui" % "2.2.0"

// https://mvnrepository.com/artifact/com.fasterxml.jackson.module/jackson-module-scala
libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.12.2"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.9.2"

libraryDependencies += "io.github.cdimascio" % "java-dotenv" % "5.1.4"

libraryDependencies += "org.mockito" % "mockito-core" % "3.2.4" % "test"

libraryDependencies += "org.mockito" % "mockito-inline" % "2.13.0" % "test"

libraryDependencies += "com.graphql-java" % "graphql-java" % "16.2"

libraryDependencies += "com.graphql-java" % "graphql-java-extended-scalars" % "16.0.1"

// If you add "static" dependencies (above this line), Please add them to build.sbt.cached
// If you change dependencies above this line, also change them in build.sbt.cached
// Below this line, are dependencies that "change frequently" and therefore we don't want to cache them into our docker build

// CACHE LINE
// Place dependencies below this line that change a lot. We can cache the dependencies above this line for faster docker builds

libraryDependencies += "com.dataprofiler" % "dataprofiler-iterators" % "0.1.0" exclude("org.slf4j", "slf4j") exclude("org.apache.spark", "spark-core_2.12") exclude("org.apache.spark", "spark-core_2.11") exclude("org.scala-lang.modules", "scala-parser-combinators_2.11") exclude("com.fasterxml.jackson.module", "jackson-module-scala_2.11")

libraryDependencies += "com.dataprofiler" % "dataprofiler-tools" % "0.1.0" exclude("org.slf4j", "slf4j") exclude("org.apache.spark", "spark-core_2.12") exclude("org.apache.spark", "spark-core_2.11") exclude("org.scala-lang.modules", "scala-parser-combinators_2.11") exclude("com.fasterxml.jackson.module", "jackson-module-scala_2.11")

libraryDependencies += "com.dataprofiler" % "dataprofiler-util" % "0.1.0" exclude("org.slf4j", "slf4j") exclude("org.apache.spark", "spark-core_2.12") exclude("org.apache.spark", "spark-core_2.11") exclude("org.scala-lang.modules", "scala-parser-combinators_2.11") exclude("com.fasterxml.jackson.module", "jackson-module-scala_2.11")

libraryDependencies += "com.dataprofiler" % "dataprofiler-lastmile" % "0.1.0" exclude("org.slf4j", "slf4j") exclude("org.scala-lang.modules", "scala-xml_2.11")  exclude("org.scala-lang.modules", "scala-parser-combinators_2.11") exclude("com.fasterxml.jackson.module", "jackson-module-scala_2.11")

Test / javaOptions += "-Dlogger.resource=logback-test.xml"

Compile / doc / sources := List()
