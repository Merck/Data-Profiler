FROM container-registry.dataprofiler.com/data_profiler_core:latest
WORKDIR /opt
EXPOSE 2181

# Cache dependencies
ADD pom.xml .
ADD iterators/pom.xml iterators/pom.xml
ADD tools/pom.xml tools/pom.xml
ADD util/pom.xml util/pom.xml
ADD query-lang/pom.xml query-lang/pom.xml
RUN mvn package -B -DskipTests -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn

ADD . .
