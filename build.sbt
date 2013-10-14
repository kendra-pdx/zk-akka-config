organization := "me.enkode"

name := "akka-zk"

version := "0.1-SNAPSHOT"

scalaVersion := "2.10.2"

libraryDependencies += "org.apache.zookeeper" % "zookeeper" % "3.4.5" excludeAll(
	ExclusionRule(name = "jms"),
	ExclusionRule(name = "jmxtools"),
	ExclusionRule(name = "jmxri"),
	ExclusionRule(name = "mail"),
	ExclusionRule(name = "junit"),
	ExclusionRule(organization = "log4j"),
	ExclusionRule(name = "slf4j-log4j12"))

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.2.1"

libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.2.1"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.2.1" % "test"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.0.RC2" % "test"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.7"

libraryDependencies += "org.slf4j" % "log4j-over-slf4j" % "1.7.5"

scalacOptions += "-deprecation"