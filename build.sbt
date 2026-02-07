name := "descent-clone"
version := "0.1"
scalaVersion := "3.3.1"
fork := true

// Reduce SBT noise
ThisBuild / useSuperShell := false
Global / excludeLintKeys += logLevel
run / outputStrategy := Some(StdoutOutput)
