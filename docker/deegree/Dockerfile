FROM java:8

MAINTAINER deegree TMC <tmc@deegree.org>

EXPOSE 8080

ENV DEEGREE_VERSION 3.3.18


# download deegree
RUN wget http://repo.deegree.org/content/repositories/public/org/deegree/deegree-webservices/${DEEGREE_VERSION}/deegree-webservices-${DEEGREE_VERSION}.zip -O /tmp/deegree.zip

# unpack 
RUN unzip /tmp/deegree.zip -d /opt/ && \
    ln -s /opt/deegree-webservices-${DEEGREE_VERSION}/ /opt/deegree && \
    rm /tmp/deegree.zip

#kopieer de default workspace setting
COPY workspace/ev /root/.deegree/ev
COPY webapps.properties /root/.deegree

#kopieer war de config en de templates voor de veiligheidstoetsserlvlet
COPY veiligheidstoets/veiligheidstoetsservlet.war /opt/deegree-webservices-${DEEGREE_VERSION}/webapps
COPY veiligheidstoets/veiligheidstoets.xml /etc/veiligheidstoets/veiligheidstoets.xml
COPY veiligheidstoets/templates /etc/veiligheidstoets/templates

# run tomcat
CMD /opt/deegree/bin/catalina.sh run


