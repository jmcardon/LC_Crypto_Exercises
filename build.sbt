val Http4sVersion = "0.18.12"
val Specs2Version = "4.2.0"
val LogbackVersion = "1.2.3"
val tsecV = "0.0.1-M11"
val circeV = "0.9.3"

lazy val root = (project in file("."))
  .settings(
    organization := "com.jmcardon",
    name := "lambdaconf-exercises",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.6",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "org.specs2" %% "specs2-core" % Specs2Version % "test",
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "io.github.jmcardon" %% "tsec-common" % tsecV,
      "io.github.jmcardon" %% "tsec-cipher-jca" % tsecV,
      "io.github.jmcardon" %% "tsec-hash-jca" % tsecV,
      "io.github.jmcardon" %% "tsec-cipher-bouncy" % tsecV,
      "io.github.jmcardon" %% "tsec-mac" % tsecV,
      "io.github.jmcardon" %% "tsec-signatures" % tsecV,
      "io.github.jmcardon" %% "tsec-password" % tsecV,
      "io.github.jmcardon" %% "tsec-jwt-mac" % tsecV,
      "io.circe" %% "circe-core" % circeV,
      "io.circe" %% "circe-generic" % circeV,
      "io.circe" %% "circe-parser" % circeV
    )
  )

lazy val `cipher-exercises` = (project in file("exercises"))
  .dependsOn(root)

lazy val `hash-exercises` = (project in file("hash-exercises"))
  .dependsOn(root)

lazy val `mac-exercises` = (project in file("mac-exercises"))
  .dependsOn(root)

lazy val `sig-exercises` = (project in file("sig-exercises"))
  .dependsOn(root)
  .dependsOn(`hash-exercises`)

lazy val `final-exercises` = (project in file("final-exercises"))
  .dependsOn(root)
  .settings(libraryDependencies += "io.github.jmcardon" %% "tsec-http4s" % tsecV)
