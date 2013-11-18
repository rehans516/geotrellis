import sbt._
import sbt.Keys._

object GeotrellisBuild extends Build {

  val key = AttributeKey[Boolean]("javaOptionsPatched")

  val defaultScalacOptions = 
    Seq("-deprecation",
        "-unchecked",
        "-Yclosure-elim",
        "-optimize",
        "-language:implicitConversions",
        "-language:postfixOps",
        "-language:existentials",
        "-feature")

  lazy val macros = 
    Project("macros", file("macros"))
      .settings(
        name := "geotrellis-macros",
        version := "0.9.0-SNAPSHOT",
        scalaVersion := "2.10.2",
        scalacOptions ++= defaultScalacOptions,
        addCompilerPlugin("org.scala-lang.plugins" % "macro-paradise_2.10.2" % "2.0.0-SNAPSHOT"),
        libraryDependencies ++= Seq(
          "org.scala-lang" % "scala-reflect" % "2.10.2"
        ),
        resolvers += Resolver.sonatypeRepo("snapshots")
    )

  lazy val root = 
    Project("root", file("."))
      .settings(
        organization := "com.azavea.geotrellis",
        name := "geotrellis",
        version := "0.9.0-SNAPSHOT",
        scalaVersion := "2.10.2",

      // disable annoying warnings about 2.10.x
        conflictWarning in ThisBuild := ConflictWarning.disable,
        scalacOptions ++= defaultScalacOptions,
        scalacOptions in Compile in doc ++= Seq("-diagrams", "-implicits"),
        parallelExecution := false,
        testListeners <+= target.map(tgt => new eu.henkelmann.sbt.JUnitXmlTestsListener(tgt.toString)),

        fork in test := false,

        mainClass := Some("geotrellis.rest.WebRunner"),

        javaOptions in run += "-Xmx2G",

        libraryDependencies ++= Seq(
          "org.scalatest" % "scalatest_2.10" % "2.0.M5b" % "test",
          "org.scala-lang" % "scala-reflect" % "2.10.2",
          "junit" % "junit" % "4.5" % "test",
          "com.vividsolutions" % "jts" % "1.12",
          "com.typesafe.akka" %% "akka-kernel" % "2.2.1",
          "com.typesafe.akka" %% "akka-remote" % "2.2.1",
          "com.typesafe.akka" %% "akka-actor" % "2.2.1",
          "com.typesafe.akka" %% "akka-cluster" % "2.2.1",
          "org.codehaus.jackson" % "jackson-core-asl" % "1.6.1",
          "org.codehaus.jackson" % "jackson-mapper-asl" % "1.6.1",
          "org.spire-math" %% "spire" % "0.4.0",
          "com.nativelibs4java" %% "scalaxy-loops" % "0.3-SNAPSHOT" % "provided",
          "net.databinder" %% "dispatch-http" % "0.8.10"
        ),

        resolvers ++= Seq(
          "NL4J Repository" at "http://nativelibs4java.sourceforge.net/maven/",
          "maven2 dev repository" at "http://download.java.net/maven/2",
          "Scala Test" at "http://www.scala-tools.org/repo-reloases/",
          "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
          "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository",
          "sonatypeSnapshots" at "http://oss.sonatype.org/content/repositories/snapshots"
        ),

        // Settings to publish to Sonatype
        publishMavenStyle := true,
        
        publishTo <<= version { (v: String) =>
          val nexus = "https://oss.sonatype.org/"
          if (v.trim.endsWith("SNAPSHOT"))
            Some("snapshots" at nexus + "content/repositories/snapshots")
          else
            Some("releases"  at nexus + "service/local/staging/deploy/maven2")
        },

        publishArtifact in Test := false,

        pomIncludeRepository := { _ => false },
        licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
        homepage := Some(url("http://geotrellis.github.io/")),

        pomExtra := defaultPomExtra
       )
      .dependsOn(macros)

  lazy val server:Project = 
    Project("server", file("server"))
      .settings(
        scalaVersion := "2.10.2",
        scalacOptions ++= defaultScalacOptions,
        libraryDependencies ++= Seq(
          "org.eclipse.jetty" % "jetty-webapp" % "8.1.0.RC4",
          "com.sun.jersey" % "jersey-bundle" % "1.11",
          "org.slf4j" % "slf4j-api" % "1.6.0",
          "org.slf4j" % "slf4j-nop" % "1.6.0"
        )
       )
      .dependsOn(root)

  lazy val spark:Project = 
    Project("spark", file("geotrellis-spark"))
      .settings(
        scalaVersion := "2.10.2",
        scalacOptions ++= defaultScalacOptions,
        libraryDependencies ++= Seq(
          "org.apache.spark" %% "spark-core" % "0.9.0-incubating-SNAPSHOT"
        )
       )
      .dependsOn(root)

  lazy val dev:Project = 
    Project("dev", file("dev"))
      .settings(
        scalaVersion := "2.10.2",
        scalacOptions ++= defaultScalacOptions,
        libraryDependencies ++= Seq(
          "org.scala-lang" % "scala-reflect" % "2.10.2",
          "org.hyperic" % "sigar"  % "1.6.4"
        ),
        resolvers ++= Seq(
          "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
        ),
        Keys.fork in run := true,
        fork := true,
        javaOptions in run ++= Seq(
          "-Djava.library.path=./sigar"
        )
       )
      .dependsOn(root)

  lazy val demo:Project = 
    Project("demo", file("demo"))
      .settings(
        scalaVersion := "2.10.2",
        scalacOptions ++= defaultScalacOptions
       )
      .dependsOn(server)

  val geotoolsVersion = "8.0-M4"
  lazy val geotools:Project = 
    Project("geotools", file("geotools"))
      .settings(
        scalaVersion := "2.10.2",
        name := "geotrellis-geotools",
        organization := "com.azavea.geotrellis",
        version := "0.9.0-SNAPSHOT",
        scalacOptions ++= defaultScalacOptions,
        libraryDependencies ++= Seq(
          "org.scalatest" % "scalatest_2.10" % "2.0.M5b" % "test",
          "java3d" % "j3d-core" % "1.3.1",
          "org.geotools" % "gt-main" % geotoolsVersion,
          "org.geotools" % "gt-jdbc" % geotoolsVersion,
          "org.geotools.jdbc" % "gt-jdbc-postgis" % geotoolsVersion,
          "org.geotools" % "gt-coverage" % geotoolsVersion,
          "org.geotools" % "gt-coveragetools" % geotoolsVersion,
          "org.postgis" % "postgis-jdbc" % "1.3.3",
          "javax.media" % "jai_core" % "1.1.3" from "http://download.osgeo.org/webdav/geotools/javax/media/jai_core/1.1.3/jai_core-1.1.3.jar"),
        resolvers ++= Seq(
          "Geotools" at "http://download.osgeo.org/webdav/geotools/"
        )
       )
      .dependsOn(root)

  lazy val tasks:Project = 
    Project("tasks", file("tasks"))
      .settings(
        scalaVersion := "2.10.2",
        scalacOptions ++= defaultScalacOptions,
        libraryDependencies ++= Seq(
          "com.beust" % "jcommander" % "1.23",
          "org.reflections" % "reflections" % "0.9.5"),
        mainClass in Compile := Some("geotrellis.run.Tasks")
       )
      .dependsOn(root,geotools)

   
  lazy val benchmark:Project = 
    Project("benchmark", file("benchmark"))
      .settings(benchmarkSettings: _*)
      .dependsOn(root)

  def benchmarkSettings = 
    Seq(
      scalaVersion := "2.10.2",
      scalacOptions ++= defaultScalacOptions,
      // raise memory limits here if necessary
      javaOptions += "-Xmx8G",

      libraryDependencies ++= Seq(
        "com.google.guava" % "guava" % "r09",
        "com.google.code.java-allocation-instrumenter" % "java-allocation-instrumenter" % "2.0",
        "com.google.code.caliper" % "caliper" % "1.0-SNAPSHOT"
          from "http://plastic-idolatry.com/jars/caliper-1.0-SNAPSHOT.jar",
        "com.google.code.gson" % "gson" % "1.7.1",
        "org.spire-math" %% "spire" % "0.4.0"
      ),

      // enable forking in both run and test
      fork := true,

      // set the correct working directory for acces to resources in test

      runner in Compile in run <<= (thisProject,
        taskTemporaryDirectory,
        scalaInstance,
        baseDirectory,
        javaOptions,
        outputStrategy,
        javaHome,
        connectInput) map {
        (tp, tmp, si, base, options, strategy, javaHomeDir, connectIn) =>
        new BenchmarkRunner(tp.id, ForkOptions(scalaJars = si.jars,
          javaHome = javaHomeDir,
          connectInput = connectIn,
          outputStrategy = strategy,
          runJVMOptions = options,
          workingDirectory = Some(base)))
      }
    )

  val defaultPomExtra = (

    <scm>
      <url>git@github.com:geotrellis/geotrellis.git</url>
      <connection>scm:git:git@github.com:geotrellis/geotrellis.git</connection>
    </scm>
    <developers>
      <developer>
        <id>joshmarcus</id>
        <name>Josh Marcus</name>
        <url>http://github.com/joshmarcus/</url>
      </developer>
      <developer>
        <id>lossyrob</id>
        <name>Rob Emanuele</name>
        <url>http://github.com/lossyrob/</url>
      </developer>
    </developers>

  )
}

class BenchmarkRunner(subproject: String, config: ForkScalaRun) extends sbt.ScalaRun {
  def run(mainClass: String, classpath: Seq[File], options: Seq[String], log: Logger): Option[String] = {
    log.info("Running " + subproject + " " + mainClass + " " + options.mkString(" "))

    val javaOptions = classpathOption(classpath) ::: mainClass :: options.toList
    val strategy = config.outputStrategy getOrElse LoggedOutput(log)
    val jvmopts = config.runJVMOptions ++ javaOptions
    val process =  Fork.java.fork(config.javaHome,
                                  jvmopts,
                                  config.workingDirectory,
                                  Map.empty,
                                  config.connectInput,
                                  strategy)
    def cancel() = {
      log.warn("Run canceled.")
      process.destroy()
      1
    }
    val exitCode = try process.exitValue() catch { case e: InterruptedException => cancel() }
    processExitCode(exitCode, "runner")
  }
  private def classpathOption(classpath: Seq[File]) = "-classpath" :: Path.makeString(classpath) :: Nil
  private def processExitCode(exitCode: Int, label: String) = {
    if(exitCode == 0) None
    else Some("Nonzero exit code returned from " + label + ": " + exitCode)
  }
}
