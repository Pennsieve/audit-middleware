lazy val akkaHttpVersion = "10.1.3"
lazy val akkaVersion = "2.5.22"
lazy val circeVersion = "0.11.1"

val assemblyJarPath = taskKey[Unit]("Call assembly and get the JAR file path.")

lazy val root = (project in file("."))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    inThisBuild(
      List(organization := "com.blackfynn", scalaVersion := "2.12.10")
    ),
    name := "audit-middleware",
    headerLicense := Some(
      HeaderLicense
        .Custom("Copyright (c) 2020 Pennsieve, Inc. All Rights Reserved.")
    ),
    headerMappings := headerMappings.value + (HeaderFileType.scala -> HeaderCommentStyle.cppStyleLineComment),
    resolvers ++= Seq(
      "Pennsieve Releases" at "https://nexus.pennsieve.cc/repository/maven-releases",
      "Pennsieve Snapshots" at "https://nexus.pennsieve.cc/repository/maven-snapshots",
      Resolver.bintrayRepo("commercetools", "maven"),
      Resolver.sonatypeRepo("releases"),
      Resolver.sonatypeRepo("snapshots")
    ),
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.0.5" % Test
    ),
    publishTo := {
      val nexus = "https://nexus.pennsieve.cc/repository"

      if (isSnapshot.value) {
        Some("Nexus Realm" at s"$nexus/maven-snapshots")
      } else {
        Some("Nexus Realm" at s"$nexus/maven-releases")
      }
    },
    publishMavenStyle := true,
    scalafmtOnCompile := true,
    releaseIgnoreUntrackedFiles := true,
    credentials += Credentials(
      "Sonatype Nexus Repository Manager",
      "nexus.pennsieve.cc",
      sys.env.getOrElse("PENNSIEVE_NEXUS_USER", "pennsieve-ci"),
      sys.env.getOrElse("PENNSIEVE_NEXUS_PW", "")
    ),
    test in assembly := {}, // Skip running tests during JAR assembly
    assemblyJarPath := {
      println(assembly.value.getAbsolutePath)
    }
  )
