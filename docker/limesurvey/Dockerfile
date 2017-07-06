FROM crramirez/limesurvey:latest

ARG WMSURL
ARG TOETSSERVLETURL

RUN echo "{\"wmsurl\":\"$WMSURL\", \"toetsservleturl\":\"$TOETSSERVLETURL\"}" > /var/www/html/ov-ev-config.json

