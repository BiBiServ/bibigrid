"""
Contains main method. Interprets command line, sets logging and starts corresponding action.
"""
import asyncio
import logging
import multiprocessing

import uvicorn
import yaml
from fastapi import FastAPI, File, UploadFile, status, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from fastapi.testclient import TestClient

from bibigrid.core.actions import check, create, terminate, list_clusters
from bibigrid.core.utility import id_generation
from bibigrid.core.utility.handler import provider_handler

app = FastAPI()

LOG_FORMAT = "%(asctime)s [%(levelname)s] %(message)s"
LOG_FORMATTER = logging.Formatter(LOG_FORMAT)
LOG = logging.getLogger("bibigrid")


def prepare_logging():
    stream_handler = logging.StreamHandler()
    file_handler = logging.FileHandler("bibigrid_rest.log")
    for handler in [stream_handler, file_handler]:
        handler.setFormatter(LOG_FORMATTER)
        LOG.addHandler(handler)
    logging.addLevelName(42, "PRINT")
    LOG.setLevel(logging.DEBUG)
    LOG.log(42, "Test0")
    LOG.error("Test1")
    LOG.warning("Test2")
    LOG.info("Test3")
    LOG.debug("Test4")


def setup(cluster_id):
    """
    If cluster_id is none, generates a cluster id and sets up the logger. Logger has name cluster_id and
     logs to file named cluster_id .log. Returns both.
    @param cluster_id: cluster_id or None
    @return: tuple of cluster_id and logger
    """
    cluster_id = cluster_id or id_generation.generate_cluster_id()
    log = logging.getLogger(cluster_id)
    log.setLevel(logging.DEBUG)
    if not log.handlers:
        handler = logging.FileHandler(f"{cluster_id}.log")
        handler.setFormatter(LOG_FORMATTER)
        log.addHandler(handler)
    return cluster_id, log


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    exc_str = f'{exc}'.replace('\n', ' ').replace('   ', ' ')
    logging.error(f"{request}: {exc_str}")
    content = {'status_code': 10422, 'message': exc_str, 'data': None}
    return JSONResponse(content=content, status_code=status.HTTP_422_UNPROCESSABLE_ENTITY)


@app.post("/bibigrid/validate")
async def validate_configuration(cluster_id: str = None, config_file: UploadFile = File(...)):
    cluster_id, log = setup(cluster_id)
    LOG.warning("What now!")
    LOG.info(f"Requested validation on {cluster_id}")
    try:
        content = await config_file.read()
        configurations = yaml.safe_load(content.decode())
        providers = provider_handler.get_providers(configurations, log)
        exit_state = check.check(configurations, providers, log)
        if exit_state:
            return JSONResponse(content={"message": "Validation failed", "cluster_id": cluster_id}, status_code=420)
        return JSONResponse(content={"message": "Validation successful", "cluster_id": cluster_id}, status_code=200)
    except Exception as exc:  # pylint: disable=broad-except
        return JSONResponse(content={"error": str(exc)}, status_code=400)


@app.post("/bibigrid/create")
async def create_cluster(cluster_id: str = None, config_file: UploadFile = File(...)):
    LOG.debug(f"Requested termination on {cluster_id}")
    cluster_id, log = setup(cluster_id)

    async def create_async():
        creator.create()

    try:
        content = await config_file.read()
        configurations = yaml.safe_load(content.decode())
        providers = provider_handler.get_providers(configurations, log)
        creator = create.Create(providers=providers, configurations=configurations, log=log, config_path=None,
                                cluster_id=cluster_id)
        cluster_id = creator.cluster_id
        asyncio.create_task(create_async())
        return JSONResponse(content={"message": "Cluster creation started.", "cluster_id": cluster_id}, status_code=200)
    except Exception as exc:  # pylint: disable=broad-except
        return JSONResponse(content={"error": str(exc)}, status_code=400)


@app.post("/bibigrid/terminate")
async def terminate_cluster(cluster_id: str, config_file: UploadFile = File(...)):
    cluster_id, log = setup(cluster_id)
    LOG.debug(f"Requested termination on {cluster_id}")

    async def terminate_async():
        terminate.terminate(cluster_id, providers, log)

    try:
        # Rewrite: Maybe load a configuration file stored somewhere locally to just define access
        content = await config_file.read()
        configurations = yaml.safe_load(content.decode())
        providers = provider_handler.get_providers(configurations, log)
        asyncio.create_task(terminate_async())

        return JSONResponse(content={"message": "Termination successfully requested."}, status_code=200)
    except Exception as exc:  # pylint: disable=broad-except
        return JSONResponse(content={"error": str(exc)}, status_code=400)


@app.post("/bibigrid/info/")
async def info(cluster_id: str, configurations: list = None, config_file: UploadFile = None):
    if not configurations and not config_file:
        return JSONResponse(content={"message": "Missing parameters: configurations or config_file required."},
                            status_code=400)
    LOG.debug(f"Requested info on {cluster_id}.")
    cluster_id, log = setup(cluster_id)
    try:
        providers = provider_handler.get_providers(configurations, log)
        cluster_dict = list_clusters.dict_clusters(providers, log).get(cluster_id)  # add information filtering
        if cluster_dict:
            cluster_dict["message"] = "Cluster found."
            return JSONResponse(content=cluster_dict, status_code=200)
        return JSONResponse(content={"message": "Cluster not found."}, status_code=404)
    except Exception as exc:  # pylint: disable=broad-except
        return JSONResponse(content={"error": str(exc)}, status_code=400)


# outdated tests
client = TestClient(app)


def test_validate():
    with open('test.yml', 'rb') as file:
        response = client.post("/bibigrid/validate", files={"config_file": file})
    assert response.status_code == 200
    response_data = response.json()
    assert response_data["message"] == "Validation successful"
    assert "cluster_id" in response_data


def test_create():
    with open('test.yml', 'rb') as file:
        response = client.post("/bibigrid/create", files={"config_file": file})
    assert response.status_code == 200
    response_data = response.json()
    assert "cluster_id" in response_data


def test_terminate_cluster():
    with open('test.yml', 'rb') as file:
        response = client.post("/bibigrid/terminate", params={"cluster_id": "2uiy5ka2c5y1k8o"},
                               files={"config_file": file})
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


# test_validate()
# test_create()  # test_info()
# test_terminate_cluster()
# test_get_nonexistent_configuration_info()


if __name__ == "__main__":
    prepare_logging()
    uvicorn.run("bibigrid.core.startup_rest:app", host="0.0.0.0", port=8000,
                workers=multiprocessing.cpu_count() * 2 + 1)  # Use the on_starting event)
