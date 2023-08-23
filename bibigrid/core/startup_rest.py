"""
Contains main method. Interprets command line, sets logging and starts corresponding action.
"""
import asyncio
import logging
import multiprocessing
import os
import subprocess

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

LOG_FOLDER = "log"
if not os.path.isdir(LOG_FOLDER):
    os.mkdir(LOG_FOLDER)

LOG_FORMAT = "%(asctime)s [%(levelname)s] %(message)s"
LOG_FORMATTER = logging.Formatter(LOG_FORMAT)
LOG = logging.getLogger("bibigrid")
stream_handler = logging.StreamHandler()
file_handler = logging.FileHandler(os.path.join(LOG_FOLDER, "bibigrid_rest.log"))
for handler in [stream_handler, file_handler]:
    handler.setFormatter(LOG_FORMATTER)
    LOG.addHandler(handler)
logging.addLevelName(42, "PRINT")
LOG.setLevel(logging.DEBUG)


def tail(file_path, lines):
    return subprocess.check_output(['tail', '-n', str(lines), file_path], universal_newlines=True)


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
        log_handler = logging.FileHandler(os.path.join(LOG_FOLDER, f"{cluster_id}.log"))
        log_handler.setFormatter(LOG_FORMATTER)
        log.addHandler(log_handler)
    return cluster_id, log


def is_up(cluster_id, log):
    """
    Checks if cluster with cluster_id is up and running
    @param cluster_id:
    @param log:
    @return:
    """
    file_name = os.path.join(LOG_FOLDER, f"{cluster_id}.log")
    print(file_name)
    if os.path.isfile(file_name):
        with open(file_name, "r", encoding="utf8") as log_file:
            for line in reversed(log_file.readlines()):
                if "up and running" in line:
                    log.debug("Found running cluster.")
                    return True
                if "Successfully terminated cluster" in line:
                    log.debug("Found cluster termination.")
                    return False
    log.debug("Found neither a running nor a terminated cluster.")
    return False


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    exc_str = f'{exc}'.replace('\n', ' ').replace('   ', ' ')
    logging.error(f"{request}: {exc_str}")
    content = {'status_code': 10422, 'message': exc_str, 'data': None}
    return JSONResponse(content=content, status_code=status.HTTP_422_UNPROCESSABLE_ENTITY)


@app.post("/bibigrid/validate")
async def validate_configuration(cluster_id: str = None, config_file: UploadFile = File(...)):
    cluster_id, log = setup(cluster_id)
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
    LOG.debug(f"Requested creation on {cluster_id}")
    cluster_id, log = setup(cluster_id)

    async def create_async():
        creator.create()

    try:
        content = await config_file.read()
        configurations = yaml.safe_load(content.decode())
        providers = provider_handler.get_providers(configurations, log)
        creator = create.Create(providers=providers, configurations=configurations, log=log,
                                config_path=config_file.filename, cluster_id=cluster_id)
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


@app.get("/bibigrid/info/")
async def info(cluster_id: str, config_file: UploadFile):
    LOG.debug(f"Requested info on {cluster_id}.")
    cluster_id, log = setup(cluster_id)
    content = await config_file.read()
    configurations = yaml.safe_load(content.decode())
    try:
        providers = provider_handler.get_providers(configurations, log)
        cluster_dict = list_clusters.dict_clusters(providers, log).get(cluster_id)  # add information filtering
        cluster_dict["ready"] = is_up(cluster_id, log)
        if cluster_dict:
            cluster_dict["message"] = "Cluster found."
            return JSONResponse(content=cluster_dict, status_code=200)
        return JSONResponse(content={"message": "Cluster not found."}, status_code=404)
    except Exception as exc:  # pylint: disable=broad-except
        return JSONResponse(content={"error": str(exc)}, status_code=400)


@app.get("/bibigrid/log/")
async def get_log(cluster_id: str, lines: int = None):
    LOG.debug(f"Requested log on {cluster_id}.")
    # cluster_id, log = setup(cluster_id)
    try:
        file_name = os.path.join(LOG_FOLDER, f"{cluster_id}.log")
        print(file_name)
        if os.path.isfile(file_name):
            if not lines:
                with open(file_name, "r", encoding="utf8") as log_file:
                    response = log_file.read()
            else:
                response = tail(file_name, lines)
            return JSONResponse(content={"message": "Log found", "log": response}, status_code=200)
        return JSONResponse(content={"message": "Log not found."}, status_code=404)
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
    uvicorn.run("bibigrid.core.startup_rest:app", host="0.0.0.0", port=8000,
                workers=multiprocessing.cpu_count() * 2 + 1)  # Use the on_starting event
