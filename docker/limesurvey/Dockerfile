FROM crramirez/limesurvey:latest

ARG WMSURL
ARG BASESERVLETURL

RUN echo "{\"wmsurl\":\"$WMSURL\", \"baseservleturl\":\"$BASESERVLETURL\"}" > /var/www/html/ov-ev-config.json

