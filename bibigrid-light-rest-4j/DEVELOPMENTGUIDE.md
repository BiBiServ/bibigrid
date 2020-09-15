# Development Guide
Simple guide for developing the bibigrid rest api.

## Before getting started
Take a good look at the api documentation located at
```
/bibigrid/bibigrid-light-rest-4j/documentation.html
```
A guide for viewing the documentation is provide inside the above mentioned html file.
## Starting the server
```
cd bibigrid/bibigrid-light-rest-4j/
mvn clean install exec:exec
```
Your server should now be running on port 8443 of your local machine.

When the server is used for the first time, you will need to build the other bibigrid projects because
of the dependencies. 
This is done with: 
```
cd bibigrid/
mvn clean install
``` 


## Developing
##### Making changes to handlers
Handlers are found under:
```
/bibigrid/bibigrid-light-rest-4j/src/main/java/de/unibi/cebitec/bibigrid/light_rest_4j/handler/  
```

After doing changes compile with:
```
cd bibigrid/bibigrid-light-rest-4j/
mvn clean install exec:exec
```
If you want to skip tests (saves a few seconds) add -DskipTests to the maven command above.

##### Making changes to the api specification
Important: There are 2 openapi.json files which are used by light-rest-4j. 
* The openapi.json config found under ``` /bibigrid-light-rest-4j/src/main/resources/config/openapi.json ``` 
    is used for request validation at runtime. Modify this file when making changes to API.
* The openapi.json located at   ```/bibigrid/bibigrid-light-rest-4j/openapi.json  ```   is the initial Openapi specs 
    file which is used to generate the project.
Changes should always be made to both openapi.json files just to be safe.   


##### Making changes to classes outside bibigrid-light-rest-4j Module.
Make changes to desired classes and then build module to which changes were made. This can be done by
building all modules with:
```
cd bibigrid/
mvn clean install
```
And then run:
```
cd bibigrid/bibigrid-light-rest-4j/
mvn clean install exec:exec
```

## Legacy
##### This documents how the whole project was created (see [light-rest-4j docs](https://doc.networknt.com/references/light-codegen/openapi-generator/) for additional help)
#### Setup needed repositories:
```
cd bibigrid
mkdir bibigrid-light-rest-4j
cd bibigrid-light-rest-4j/
git clone https://github.com/networknt/model-config.git
git clone https://github.com/networknt/light-codegen.git
cd light-codegen
mvn clean install
```

#### Generate api from config.json and openapi.json:
```
cd bibigrid/bibigrid-light-rest-4j/
sh generate-api-from-json.sh
```
#### Starting the server:
```
cd bibigrid/bibigrid-light-rest-4j/
mvn clean install exec:exec
```
