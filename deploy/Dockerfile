FROM jetty:9-alpine

USER root

# Adds yarn. Worked before but was in testing. Now in community.Add main to ensure
# it keeps working
RUN echo -e 'http://dl-cdn.alpinelinux.org/alpine/edge/main' >> /etc/apk/repositories
RUN echo -e 'http://dl-cdn.alpinelinux.org/alpine/edge/community' >> /etc/apk/repositories
RUN echo -e 'http://dl-cdn.alpinelinux.org/alpine/edge/testing' >> /etc/apk/repositories


# Add our system dependencies for running jetty and node services inside the
# container
RUN apk update && apk add supervisor nodejs yarn

# Bring our Java backend app over for Jetty
COPY *.war /var/lib/jetty/webapps/ROOT.war

# Bring our supervisor configuration for supervisord
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf

# Prepare the front-end portion of the site
RUN mkdir /plugins
COPY plugin-site /plugins
WORKDIR /plugins
# Make sure all our node dependencies are present and accounted for
RUN yarn


WORKDIR $JETTY_BASE

# Expose the npm server's port while we're at it
EXPOSE 5000
CMD /usr/bin/supervisord -c /etc/supervisor/conf.d/supervisord.conf
