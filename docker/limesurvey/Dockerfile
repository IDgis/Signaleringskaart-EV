FROM crramirez/limesurvey:latest

ARG WMSURL
ARG TOETSSERVLETURL

RUN echo "{\"wmsurl\":\"$WMSURL\", \"toetsservleturl\":\"$TOETSSERVLETURL\"}" > /var/www/html/ov-ev-config.json

COPY hide_version_header.sh /hide_version_header.sh
RUN chmod a+x /hide_version_header.sh
RUN /hide_version_header.sh
