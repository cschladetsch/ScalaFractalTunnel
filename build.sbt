name := "descent-clone"
version := "1.0"
scalaVersion := "3.3.1"

fork := true

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

assembly / assemblyJarName := "FractalTunnel.jar"
