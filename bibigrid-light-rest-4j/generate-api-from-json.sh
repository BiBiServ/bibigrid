rm -rf application
rm -rf .mvn
rm -rf .gitignore
rm -rf .build.sh
rm kubernetes.yml
rm LICENSE
rm -rf src
rm -rf mvnw
rm -rf mvnw.cmd
rm -rf target
rm -f dependency-reduced-pom.xml
rm -f bibigrid_light_rest_4j.iml
rm -rf .mvn
rm -rf docker

java -jar light-codegen/codegen-cli/target/codegen-cli.jar -f openapi -o ./ -m openapi.json -c config.json
