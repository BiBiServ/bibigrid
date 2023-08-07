"""
Contains main method. Interprets command line, sets logging and starts corresponding action.
"""
import asyncio
import logging
import yaml
# !/usr/bin/env python3
from fastapi import FastAPI, File, UploadFile
from fastapi.responses import JSONResponse

from fastapi.testclient import TestClient
from bibigrid.core.actions import check, create
from bibigrid.core.utility.handler import provider_handler

app = FastAPI()

LOGGING_HANDLER_LIST = [logging.StreamHandler(), logging.FileHandler("bibigrid.log")]  # stdout and to file
VERBOSITY_LIST = [logging.WARNING, logging.INFO, logging.DEBUG]
LOGGER_FORMAT = "%(asctime)s [%(levelname)s] %(message)s"
logging.basicConfig(format=LOGGER_FORMAT, handlers=LOGGING_HANDLER_LIST)
log = logging.getLogger("bibigrid")
log.setLevel(logging.DEBUG)

@app.post("/bibigrid/validate")
async def validate_configuration(config_file: UploadFile = File(...)):
    try:
        content = await config_file.read()
        configurations = yaml.safe_load(content.decode())
        providers = provider_handler.get_providers(configurations)
        exit_state = check.check(configurations, providers)
        # Validate the YAML content here
        # Implement your validation logic
        if exit_state:
            return JSONResponse(content={"message": "Validation failed"}, status_code=420)  # Fail
        return JSONResponse(content={"message": "Validation successful"}, status_code=200)  # Success
    except Exception as exc: # pylint: disable=broad-except
        return JSONResponse(content={"error": str(exc)}, status_code=400)


@app.post("/bibigrid/create")
async def create_cluster(config_file: UploadFile = File(...)):
    try:
        content = await config_file.read()
        configurations = yaml.safe_load(content.decode())
        providers = provider_handler.get_providers(configurations)
        creator = create.Create(providers=providers, configurations=configurations, config_path=None)
        cluster_id = creator.cluster_id

        async def create_async():
            await creator.create()

        asyncio.create_task(create_async())
        # Create the Bibigrid configuration here
        # Implement your creation logic

        return JSONResponse(content={"cluster_id": cluster_id}, status_code=200)
    except Exception as exc: # pylint: disable=broad-except
        return JSONResponse(content={"error": str(exc)}, status_code=400)


@app.get("/bibigrid/info/{id}")
async def info(cluster_id: str):
    try:
        config_content = cluster_id
        if config_content is None:
            return JSONResponse(content={"error": "Configuration not found"}, status_code=404)

        # Fetch information about the configuration based on the ID
        # Implement your info retrieval logic

        return JSONResponse(content={"id": cluster_id, "info": "Configuration information"})
    except Exception as exc: # pylint: disable=broad-except
        return JSONResponse(content={"error": str(exc)}, status_code=400)


client = TestClient(app)


def test_validate():
    with open('test.yml', 'rb') as file:
        response = client.post("/bibigrid/validate", files={"config_file": file})
    assert response.status_code == 200
    assert response.json() == {"message": "Validation successful"}


def test_create():
    with open('test.yml', 'rb') as file:
        response = client.post("/bibigrid/create", files={"config_file": file})
    assert response.status_code == 200
    response_data = response.json()
    assert "message" in response_data
    assert "id" in response_data
#
#
# def test_info():
#     # Assuming you've previously created a configuration with ID 1
#     response = client.get("/bibigrid/info/1")
#     assert response.status_code == 200
#     response_data = response.json()
#     assert "id" in response_data
#     assert "info" in response_data
#
#
# def test_get_nonexistent_configuration_info():
#     response = client.get("/bibigrid/info/999")
#     assert response.status_code == 404
#     assert response.json() == {"error": "Configuration not found"}
#
#
# Run the tests
test_validate()
test_create()
# test_info()
# # test_get_nonexistent_configuration_info()
