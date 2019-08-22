cd bibigrid/bibigrid-light-rest-4j
rm -rf application
java -jar light-codegen/codegen-cli/target/codegen-cli.jar -f openapi -o ./application -m openapi.json -c config.json
