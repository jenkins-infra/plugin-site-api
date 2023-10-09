FROM jetty:12.0.1-jdk17-eclipse-temurin
COPY target/*.war $JETTY_BASE/webapps/ROOT.war
RUN java -jar $JETTY_HOME/start.jar \
  --create-startd \
  --approve-all-licenses \
  --add-to-start=logging-logback \
  --module=logging-logback
