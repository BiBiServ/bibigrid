"""
Contains main method. Interprets command line, sets logging and starts corresponding action.
"""
import asyncio
import logging
import contextlib
import yaml
# !/usr/bin/env python3
from fastapi import FastAPI, File, UploadFile, status, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from fastapi.testclient import TestClient

from bibigrid.core.actions import check, create, terminate, list_clusters
from bibigrid.core.utility.handler import provider_handler

app = FastAPI()

LOGGING_HANDLER_LIST = [logging.StreamHandler(), logging.FileHandler("bibigrid.log")]  # stdout and to file
VERBOSITY_LIST = [logging.WARNING, logging.INFO, logging.DEBUG]
LOGGER_FORMAT = "%(asctime)s [%(levelname)s] %(message)s"
logging.basicConfig(format=LOGGER_FORMAT, handlers=LOGGING_HANDLER_LIST)
log = logging.getLogger("bibigrid")
log.setLevel(logging.DEBUG)


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    exc_str = f'{exc}'.replace('\n', ' ').replace('   ', ' ')
    logging.error(f"{request}: {exc_str}")
    content = {'status_code': 10422, 'message': exc_str, 'data': None}
    return JSONResponse(content=content, status_code=status.HTTP_422_UNPROCESSABLE_ENTITY)


@app.post("/bibigrid/validate")
async def validate_configuration(config_file: UploadFile = File(...)):
    try:
        content = await config_file.read()
        configurations = yaml.safe_load(content.decode())
        with open("test", 'w', encoding="utf8") as f:
            with contextlib.redirect_stdout(f):
                providers = provider_handler.get_providers(configurations)
                exit_state = check.check(configurations, providers)
        # Validate the YAML content here
        # Implement your validation logic
        if exit_state:
            return JSONResponse(content={"message": "Validation failed"}, status_code=420)  # Fail
        return JSONResponse(content={"message": "Validation successful"}, status_code=200)  # Success
    except Exception as exc:  # pylint: disable=broad-except
        return JSONResponse(content={"error": str(exc)}, status_code=400)


@app.post("/bibigrid/create")
async def create_cluster(config_file: UploadFile = File(...)):
    async def create_async():
        with open("test3", 'w', encoding="utf8") as f:
            with contextlib.redirect_stdout(f):
                creator.create()
    try:
        content = await config_file.read()
        configurations = yaml.safe_load(content.decode())
        with open("test2", 'w', encoding="utf8") as f:
            with contextlib.redirect_stdout(f):
                providers = provider_handler.get_providers(configurations)
                creator = create.Create(providers=providers, configurations=configurations, config_path=None)
            cluster_id = creator.cluster_id
            asyncio.create_task(create_async())
            # Create the Bibigrid configuration here
            # Implement your creation logic

        return JSONResponse(content={"cluster_id": cluster_id}, status_code=200)
    except Exception as exc:  # pylint: disable=broad-except
        return JSONResponse(content={"error": str(exc)}, status_code=400)


@app.post("/bibigrid/terminate")
async def terminate_cluster(cluster_id: str, config_file: UploadFile = File(...)):
    async def terminate_async():
        with open(cluster_id, 'w', encoding="utf8") as f:
            with contextlib.redirect_stdout(f):
                terminate.terminate(cluster_id, providers)
    try:
        # Rewrite: Maybe load a configuration file stored somewhere locally to just define access
        content = await config_file.read()
        configurations = yaml.safe_load(content.decode())
        with open(cluster_id, 'w', encoding="utf8") as f:
            with contextlib.redirect_stdout(f):
                providers = provider_handler.get_providers(configurations)
        asyncio.create_task(terminate_async())
        # Create the Bibigrid configuration here
        # Implement your creation logic

        return JSONResponse(content={"message": "Termination successfully requested."}, status_code=200)
    except Exception as exc:  # pylint: disable=broad-except
        return JSONResponse(content={"error": str(exc)}, status_code=400)


@app.get("/bibigrid/info/{cluster_id}")
async def info(cluster_id: str):
    try:
        # Rewrite: Maybe load a configuration file stored somewhere locally to just define access
        with open('test.yml', encoding='utf8') as f:
            configurations = yaml.safe_load(f)
        providers = provider_handler.get_providers(configurations)
        with open(cluster_id, 'w', encoding="utf8") as f:
            with contextlib.redirect_stdout(f):
                cluster_dict = list_clusters.dict_clusters(providers).get(cluster_id)  # add information filtering
        # check whether cluster is actually active and append that information
        if cluster_dict:
            return JSONResponse(content=cluster_dict, status_code=200)
        return JSONResponse(content={}, status_code=404)
    except Exception as exc:  # pylint: disable=broad-except
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
    assert "id" in response_data


def test_terminate_cluster():
    with open('test.yml', 'rb') as file:
        response = client.post("/bibigrid/terminate", params={"cluster_id": "2uiy5ka2c5y1k8o"},
                               files={"config_file": file})
    print(response)
    assert response.status_code == 200
    response_data = response.json()
    assert "message" in response_data


def test_info():
    # Assuming you've previously created a configuration with ID 1
    response = client.get("/bibigrid/info/1")
    assert response.status_code == 200
    response_data = response.json()
    assert bool(response_data)


def test_get_nonexistent_configuration_info():
    response = client.get("/bibigrid/info/999")
    assert response.status_code == 404
    assert response.json() == {"error": "Configuration not found"}

# Run the tests
# test_validate()
test_create()
# test_info()
# test_terminate_cluster()
# test_get_nonexistent_configuration_info()
