# Development Guide
Simple guide for developing the bibigrid rest api.

## Before getting started
Take a good look at the api documentation located at
```
/bibigrid/bibigrid-light-rest-4j/src/main/resources/config/openapi.json
```
 with [swagger](https://editor.swagger.io/)

## Starting the server
```
cd bibigrid/bibigrid-light-rest-4j/
mvn clean install exec:exec
```
Your server should now be running on port 8443 of your local machine

## Developing
##### Making changes to controllers
Controllers are found under 
```
/bibigrid/bibigrid-light-rest-4j/src/main/java/de/unibi/cebitec/bibigrid/light_rest_4j/handler/  
```

After doing changes compile with:
```
cd bibigrid/bibigrid-light-rest-4j/
mvn clean install exec:exec
```

##### Changing the api specification without re-generating the whole project:
Make changes to api config found in 
```
/bibigrid-light-rest-4j/src/main/resources/config/openapi.json 
``` 
Apply changes with:
```
cd bibigrid/bibigrid-light-rest-4j/
mvn clean install exec:exec
```

##### Making changes to classes outside bibigrid-light-rest-4j project
Make changes to desired classes and then run
```
cd bibigrid/
mvn clean install
```
And then run
```
cd bibigrid/bibigrid-light-rest-4j/
mvn clean install exec:exec
```




## History
##### This documents how the whole project was created (see [light-rest-4j docs](https://doc.networknt.com/references/light-codegen/openapi-generator/) for additional help)
#### Setup needed repositories
```
cd bibigrid
mkdir bibigrid-light-rest-4j
cd bibigrid-light-rest-4j/
git clone https://github.com/networknt/model-config.git
git clone https://github.com/networknt/light-codegen.git
cd light-codegen
mvn clean install
```

#### Generate api from config.json and openapi.json
```
cd bibigrid/bibigrid-light-rest-4j/
sh generate-api-from-json.sh
```
#### Starting the server
```
cd bibigrid/bibigrid-light-rest-4j/
mvn clean install exec:exec
```


##### Author
Tim-Niklas Rose


Your server should now be running on port 8443 of your local machine

# Example requests
Use [Postman](https://www.getpostman.com/) or   
```
curl  https://localhost:8443/
```

