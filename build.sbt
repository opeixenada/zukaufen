import com.typesafe.sbt.packager.docker._

name := "zukaufen"

version := "1.0"

scalaVersion := "2.11.8"

enablePlugins(DockerPlugin)
enablePlugins(JavaAppPackaging)

dockerCommands := Seq(
  Cmd("FROM", "java:8-jre-alpine"),
  Cmd("WORKDIR", "/opt/docker"),
  ExecCmd("ADD", "opt", "/opt"),
  ExecCmd("RUN", "chown", "-R", "daemon:daemon", "/opt"),
  Cmd("USER", "daemon"),
  Cmd("EXPOSE", "8080"),
  ExecCmd("ENTRYPOINT", "java", "-cp", "/opt/docker/lib/*"),
  ExecCmd("CMD", "zukaufen.Main")
)

packageName in Docker := "zukaufen"
dockerUpdateLatest := true

libraryDependencies ++= Seq(
  "com.twitter" %% "finagle-http" % "6.44.0",
  "com.novus" %% "salat" % "1.9.9",
  "org.mongodb" % "casbah-core_2.11" % "2.7.1",
  "joda-time" % "joda-time" % "2.9.9",
  "com.typesafe" % "config" % "1.3.1",
  "org.json4s" %% "json4s-native" % "3.2.9",
  "org.json4s" %% "json4s-ext" % "3.2.9",
  "org.logback-extensions" % "logback-ext-loggly" % "0.1.4",
  "org.scalactic" %% "scalactic" % "3.0.2",
  "org.scalatest" %% "scalatest" % "3.0.3" % "test",
  "org.mockito" % "mockito-core" % "2.8.47"
)

(stage in Docker) <<= (stage in Docker) dependsOn (test in Test)
