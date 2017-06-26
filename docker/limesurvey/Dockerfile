FROM crramirez/limesurvey:latest

ARG wmsurl
ARG baseservleturl

RUN echo "{\"wmsurl\":\"$wmsurl\", \"baseservleturl\":\"$baseservleturl\"}" > /var/www/html/ov-ev-config.json

