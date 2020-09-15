# OpenAPI Light-4J Server

## Quick Start

1. Clone the bibigrid repo and change into the rest api directory

    ~~~BASH
    git clone https://github.com/BiBiServ/bibigrid.git
    cd bibigrid/bibigrid-light-rest-4j
    ~~~

2.   To start the API, you must copy the .env.in to .env and set the values.

     ~~~BASH
        cp .env.in .env
     ~~~
        **You could also use environment variables for the keys in the .env.in ! They will have a higher priority.**
  
4. **Makefile**

    There are several make commands available, you will find an overview with documentation running:
     ~~~BASH
    make help
     ~~~
    
    You can start the API with an existing docker image or have it built locally e.g. for developing.
    
    Additionally the API can be run over http or over https
    
    #### Using existing Docker Image
    You can view (if you have access) the tags for existing docker images here: https://hub.docker.com/repository/docker/bibiserv/bibigrid/tags<br>
    
    Just set BIBIGRID_TAG in your environment or .env file to the tag you want to use.<br>
    Then you can simply start the API with one of the following make commands:
    
    ~~~BASH
        make bibigrid-http
        or 
        make bibigrid-https
    ~~~
   
   #### Build image locally
   If you want to build a new Docker image, e.g. to test local changes, just use one of the following make commands
    ~~~BASH
           make bibigrid-dev-http
           or 
           make bibigrid-dev-https
    ~~~
   
   **The container will always be started detached!**
    

## Build and Start Maven only

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





