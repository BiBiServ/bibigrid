cd bibigrid/bibigrid-light-rest-4j
rm -rf application
rm -rf .gitignore
rm -rf .build.sh
rm kubernetes.yml
rm LICENSE
rm -rf src
rm -rf mvnw
rm -rf mvnw.cmd
#rm -r pom.xml

java -jar light-codegen/codegen-cli/target/codegen-cli.jar -f openapi -o ./ -m openapi.json -c config.json
