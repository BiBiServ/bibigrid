<<<<<<< f9d5c2d2a47ed258b7efd65f5ec4c9bd886397a5
# OpenAPI Light-4J Server

## Build and Start

For testing locally, you don't need to create the artifact for the document, source code, and the fatjar. You can build and start the server with the following command.

```
mvn clean install exec:exec
```

or

```
mvn clean package exec:exec
```

If you want to build the fatjar and other artifacts, please use the following command.

```
mvn clean install -Prelease
```

With the fatjar in the target directory, you can start the server with the following command.

```
java -jar target/fat.jar
```

## Test

By default, the OAuth2 JWT security verification is disabled. You can use Curl or Postman to test your service right after the code generation. For example,


```
curl -k https://localhost:8443/v1/pets
```


## Security

The OAuth JWT token verifier protects all endpoints, but it is disabled in the generated openapi-security.yml config file. If you want to turn on the security,  change the src/main/resources/config/openapi-security.yml   enableVerifyJwt to true and restart the server.


To access the server, there is a long-lived token below issued by my
oauth2 server [light-oauth2](https://github.com/networknt/light-oauth2)

```
Bearer eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTc5MDAzNTcwOSwianRpIjoiSTJnSmdBSHN6NzJEV2JWdUFMdUU2QSIsImlhdCI6MTQ3NDY3NTcwOSwibmJmIjoxNDc0Njc1NTg5LCJ2ZXJzaW9uIjoiMS4wIiwidXNlcl9pZCI6InN0ZXZlIiwidXNlcl90eXBlIjoiRU1QTE9ZRUUiLCJjbGllbnRfaWQiOiJmN2Q0MjM0OC1jNjQ3LTRlZmItYTUyZC00YzU3ODc0MjFlNzIiLCJzY29wZSI6WyJ3cml0ZTpwZXRzIiwicmVhZDpwZXRzIl19.mue6eh70kGS3Nt2BCYz7ViqwO7lh_4JSFwcHYdJMY6VfgKTHhsIGKq2uEDt3zwT56JFAePwAxENMGUTGvgceVneQzyfQsJeVGbqw55E9IfM_uSM-YcHwTfR7eSLExN4pbqzVDI353sSOvXxA98ZtJlUZKgXNE1Ngun3XFORCRIB_eH8B0FY_nT_D1Dq2WJrR-re-fbR6_va95vwoUdCofLRa4IpDfXXx19ZlAtfiVO44nw6CS8O87eGfAm7rCMZIzkWlCOFWjNHnCeRsh7CVdEH34LF-B48beiG5lM7h4N12-EME8_VDefgMjZ8eqs1ICvJMxdIut58oYbdnkwTjkA
```

Add "Authorization" header with value as above token and a dummy message will return from the generated stub.
=======
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
java -jar light-codegen/codegen-cli/target/codegen-cli.jar -f openapi -o ./application -m openapi.json -c config.json```
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

>>>>>>> added local config files and development README for light-rest-4j and re generated project from those files



