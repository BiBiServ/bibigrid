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

##### Changing the api specification
* Minor changes, like altering the accepted request bodies, can be made without having to re-generate the whole project
by editing the openapi.json config found under:
    ```
    /bibigrid-light-rest-4j/src/main/resources/config/openapi.json 
    ``` 
    Apply changes with:
    ```
    cd bibigrid/bibigrid-light-rest-4j/
    mvn clean install exec:exec
    ```
  It is highly recommended to also update the   ```/bibigrid/bibigrid-light-rest-4j/openapi.json  ``` to match the minor changes
  in    ```/bibigrid-light-rest-4j/src/main/resources/config/openapi.json  ``` config because otherwise making major changes gets difficult (read below).

* Major changes, like adding new routes, changing http request methods requires you to re-generate the whole project.  
  Do those changes to openapi.json located under 
  ```    
  /bibigrid-light-rest-4j/openapi.json 
  ```
  and then it gets tricky since generating the project creates 2 problems
  * it resets minor change in ```/bibigrid-light-rest-4j/src/main/resources/config/openapi.json  ``` config  
  * it overwrites the controllers and models 
  #####Quick and dirty solution: 
  * manually merge content of minor changes config ```/bibigrid-light-rest-4j/src/main/resources/config/openapi.json``` into the
    major changes ```/bibigrid/bibigrid-light-rest-4j/openapi.json  ```
  * copy the content of all the controllers to a text editor and then paste the logic back after
    the project has been generated with
  ```    
  sh generate-api-from-json.sh
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

