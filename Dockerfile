FROM tomcat:8.0.20-jre8

RUN apt-get update && apt-get install -y \
    openjdk-8-jdk \
    && rm -rf /var/lib/apt/lists/*

COPY build/libs/web-jayhorn.war /usr/local/tomcat/webapps/