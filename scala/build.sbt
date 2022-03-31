lazy val scala212 = "2.12.10"
lazy val scala213 = "2.13.8"
lazy val supportedScalaVersions = List(scala212, scala213)




lazy val scalatestVersion = "3.2.11"
lazy val akkaHttpVersion = "10.1.11"
lazy val akkaVersion = "2.6.5"

lazy val circeVersion = SettingKey[String]("circeVersion")
circeVersion := (CrossVersion.partialVersion(scalaVersion.value) match {
  case Some((2, 12)) => "0.11.1"
  case _ => "0.14.1"
})

val assemblyJarPath = taskKey[Unit]("Call assembly and get the JAR file path.")

lazy val root = (project in file("."))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    inThisBuild(
      List(
        organization := "com.pennsieve",
        scalaVersion := scala212,
        crossScalaVersions := supportedScalaVersions
      )
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
      "io.circe" %% "circe-core" % circeVersion.value,
      "io.circe" %% "circe-generic" % circeVersion.value,
      "io.circe" %% "circe-parser" % circeVersion.value,
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % scalatestVersion % Test
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
