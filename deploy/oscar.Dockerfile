FROM tomcat:10.1-jdk21

COPY oscar-service/build/libs/oscar.war /usr/local/tomcat/webapps/oscar.war
