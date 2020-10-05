bibigrid-http: ## Start detached container with Bibigrid API and http port 8080
	touch .tmp.env
	cat .env >> .tmp.env
	echo "server.enableHttps=false" >> .tmp.env
	echo "server.enableHttp=true" >> .tmp.env
	BIBIGRID_PORT=8080 docker-compose -f docker-compose.yml  up -d
	rm .tmp.env

bibigrid-https: ## Start detached container with Bibigrid API and https port 8443
	touch .tmp.env
	cat .env >> .tmp.env
	echo "server.enableHttps=true" >> .tmp.env
	echo "server.enableHttp=false" >> .tmp.env
	BIBIGRID_PORT=8443 docker-compose -f docker-compose.yml  up -d
	rm .tmp.env

	
bibigrid-dev-http: ##Build local and start detached container with Bibigrid API and http port 8080
	./prepareDockerBuild.sh
	touch .tmp.env
	cat .env >> .tmp.env
	echo "server.enableHttps=false" >> .tmp.env
	echo "server.enableHttp=true" >> .tmp.env
	BIBIGRID_PORT=8080 docker-compose -f docker-compose.dev.yml  up -d
	rm .tmp.env


bibigrid-dev-https: ## Build local and start detached container with Bibigrid API and https port 8443
	./prepareDockerBuild.sh
	cat .env >> .tmp.env
	echo "server.enableHttps=true" >> .tmp.env
	echo "server.enableHttp=false" >> .tmp.env
	BIBIGRID_PORT=8443 docker-compose -f docker-compose.dev.yml  up -d
	rm .tmp.env


bibigrid-logs-container: ## Show logs from bibigrid container
	docker logs bibigrid-light-rest-4j_bibigrid_1
	
bibigrid-stop-containter: ## Stop bibigrid container
	docker stop bibigrid-light-rest-4j_bibigrid_1

bibigrid-remove-containter: ## Remove bibigrid container
	docker stop bibigrid-light-rest-4j_bibigrid_1 && docker rm bibigrid-light-rest-4j_bibigrid_1
	
help:
	@egrep '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-16s\033[0m %s\n", $$1, $$2}'
