#Script to re-generate the api based on the openapi.json and config.json file. In the beginning, all previously
#genererated files are removed and then light-codegen (a part of the light-rest-4j framework) will generate the project
#from the scratch.
# See https://doc.networknt.com/references/light-codegen/openapi-generator/ for more information about the generation process.
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
