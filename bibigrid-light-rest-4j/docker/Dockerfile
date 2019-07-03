
FROM openjdk:11.0.3-slim
ADD /target/bibigrid_light_rest_4j-1.0.0.jar server.jar
CMD ["/bin/sh","-c","java -Dlight-4j-config-dir=/config -Dlogback.configurationFile=/config/logback.xml -jar /server.jar"]
