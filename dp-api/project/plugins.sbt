// https://github.com/mefellows/sbt-dotenv
resolvers += Resolver.url(
  "bintray-sbt-plugin-releases",
  url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(
    Resolver.ivyStylePatterns)

resolvers += Classpaths.sbtPluginReleases

addSbtPlugin("au.com.onegeek" %% "sbt-dotenv" % "2.0.117")


// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.7.2")

// Swagger
addSbtPlugin("com.iheart" % "sbt-play-swagger" % "0.10.2")

// Sbt Dependency Tree
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.2")
