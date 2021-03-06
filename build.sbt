githubProject := "sbt-git-changelog"

libraryDependencies ++=
    "org.eclipse.jgit" % "org.eclipse.jgit" % "4.4.1.201607150455-r" ::
    "org.typelevel" %% "cats-core" % "0.7.2" ::
    "org.typelevel" %% "cats-kernel" % "0.7.2" ::
    "org.typelevel" %% "cats-macros" % "0.7.2" ::
    Nil

name := "sbt-git-changelog"

organization := "io.taig"

sbtPlugin := true

scalaVersion := "2.10.6"