FROM jetty:9-jdk8-eclipse-temurin
COPY target/*.war $JETTY_BASE/webapps/ROOT.war
RUN java -jar "${JETTY_HOME}/start.jar" \
  --create-startd \
  --approve-all-licenses \
  --add-to-start=logging-logback \
  --module=logging-logback
