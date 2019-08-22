# openapi-light-rest-4j-example
Bachelor Thesis

# Development Guide
Easy step by step guide to create a simple rest api using openapi 3.0 and the light-rest-4j framework.


##### Set up light java and codegen for a new project. Only needed if the bibigrid-light-rest-4j directory is empty.
```
cd bibigrid/bibigrid-light-rest-4j
git clone https://github.com/networknt/model-config.git
git clone https://github.com/networknt/light-codegen.git
cd light-codegen
mvn clean install
```

#### Generate api from config.json and openapi.json
```
cd bibigrid/bibigrid-light-rest-4j
rm -rf application # Needs to be done when there exists an application folder from older api generations
java -jar light-codegen/codegen-cli/target/codegen-cli.jar -f openapi -o ./application -m openapi.json -c config.json
```
##### After doing code changes you need to compile again with the following commands
```
cd bibigrid/bibigrid-light-rest-4j/application
mvn clean install exec:exec
```
Your server should now be running on port 8443 of your local machine

# Example requests
Use [Postman](https://www.getpostman.com/) or   
```
curl  https://localhost:8443/
```




