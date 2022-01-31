// https://github.com/mefellows/sbt-dotenv
resolvers += Resolver.url(
  "sbt-plugin-releases",
  url("https://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/"))(
  Resolver.ivyStylePatterns)

resolvers += Classpaths.sbtPluginReleases

addSbtPlugin("au.com.onegeek" %% "sbt-dotenv" % "2.0.117")

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.13")

// Swagger
addSbtPlugin("com.iheart" % "sbt-play-swagger" % "0.10.6-PLAY2.8")

// Sbt Dependency Tree
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.2")