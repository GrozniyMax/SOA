FROM tomcat:10.1-jdk21

COPY deploy/tomcat/server.xml /usr/local/tomcat/conf/server.xml
COPY oscar-service/build/libs/oscar.war /usr/local/tomcat/webapps/oscar.war
COPY frontend/dist /usr/local/tomcat/webapps/ROOT
