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
from pydantic import BaseModel

from bibigrid.core.actions import check, create, terminate, list_clusters
from bibigrid.core.utility import id_generation
from bibigrid.core.utility.handler import provider_handler

VERSION = "0.0.1"
DESCRIPTION = """
BiBiGrid REST API allows you to use the most important features of [BiBiGrid](https://github.com/BiBiServ/bibigrid)
via REST. This includes:
validation, creation, termination and getting cluster information.
"""

app = FastAPI(title="BiBiGrid REST API", description=DESCRIPTION,
              summary="REST API for the cluster creation and management tool BiBiGrid.", version=VERSION)

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


class ValidationResponseModel(BaseModel):
    """
    ResponseModel for validate
    """
    message: str
    cluster_id: str
    success: bool


class CreateResponseModel(BaseModel):
    """
    ResponseModel for create
    """
    message: str
    cluster_id: str


class TerminateResponseModel(BaseModel):
    """
    ResponseModel for terminate
    """
    message: str


class InfoResponseModel(BaseModel):
    """
    ResponseModel for info
    """
    workers: list
    vpngtws: list
    master: dict
    message: str
    ready: bool


class LogResponseModel(BaseModel):
    """
    ResponseModel for get_log
    """
    message: str
    log: str


class ReadyResponseModel(BaseModel):
    """
    ResponseModel for ready
    """
    message: str
    ready: bool


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
    if os.path.isfile(file_name):
        log.debug(f"Log for {cluster_id} found.")
        with open(file_name, "r", encoding="utf8") as log_file:
            for line in reversed(log_file.readlines()):
                if "up and running" in line:
                    log.debug(f"Found running cluster for {cluster_id}.")
                    return True
                if "Successfully terminated cluster" in line:
                    log.debug(f"Found cluster termination for {cluster_id}.")
                    return False
    else:
        log.debug(f"Log for {cluster_id} not found.")
    log.debug(f"Found neither a running nor a terminated cluster for {cluster_id}.")
    return False


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    exc_str = f'{exc}'.replace('\n', ' ').replace('   ', ' ')
    logging.error(f"{request}: {exc_str}")
    content = {'status_code': 10422, 'message': exc_str, 'data': None}
    return JSONResponse(content=content, status_code=status.HTTP_422_UNPROCESSABLE_ENTITY)


@app.post("/bibigrid/validate", response_model=ValidationResponseModel)
async def validate_configuration(cluster_id: str = None, config_file: UploadFile = File(...)):
    """
    Expects a cluster id and a
    [configuration](https://github.com/BiBiServ/bibigrid/blob/master/documentation/markdown/features/configuration.md)
    file.

    Returns validation result (success, or failure)
    * @param cluster_id: optional id of to be created cluster in order to log into the same file.
    If not given, one is generated.
    * @param config_file: [configuration](https://github.com/BiBiServ/bibigrid/blob/master/documentation/markdown
    /features/configuration.md) yaml file
    * @return: success or failure of the validation
    """
    cluster_id, log = setup(cluster_id)
    LOG.info(f"Requested validation on {cluster_id}")
    try:
        content = await config_file.read()
        configurations = yaml.safe_load(content.decode())
        providers = provider_handler.get_providers(configurations, log)
        exit_state = check.check(configurations, providers, log)
        if exit_state:
            return JSONResponse(
                content={"message": "Validation failed", "cluster_id": cluster_id, "success": exit_state},
                status_code=420)
        return JSONResponse(
            content={"message": "Validation successful", "cluster_id": cluster_id, "success": exit_state},
            status_code=200)
    except Exception as exc:  # pylint: disable=broad-except
        return JSONResponse(content={"error": str(exc)}, status_code=400)


@app.post("/bibigrid/create", response_model=CreateResponseModel)
async def create_cluster(cluster_id: str = None, config_file: UploadFile = File(...)):
    """
    Expects an optional cluster id and a
    [configuration](https://github.com/BiBiServ/bibigrid/blob/master/documentation/markdown/features/configuration.md)
    file.

    Returns the cluster id and whether cluster creation (according to the configuration) has started.
    The user then can check via [ready](#ready) if the cluster is ready.
    * @param cluster_id: UUID with 15 letters. if not given, one is generated
    * @param config_file: [configuration](https://github.com/BiBiServ/bibigrid/blob/master/documentation/markdown
    /features/configuration.md) yaml file
    * @return: message whether the cluster creation has been started and cluster id
    """
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
        return JSONResponse(content={"message": "Cluster creation started.", "cluster_id": cluster_id}, status_code=202)
    except Exception as exc:  # pylint: disable=broad-except
        return JSONResponse(content={"error": str(exc)}, status_code=400)


@app.post("/bibigrid/terminate", response_model=TerminateResponseModel)
async def terminate_cluster(cluster_id: str, config_file: UploadFile = File(...)):
    """
    Expects a cluster id and a
    [configuration](https://github.com/BiBiServ/bibigrid/blob/master/documentation/markdown/features/configuration.md)
    file.

    Returns whether cluster termination (according to the configuration) has started.
    * @param cluster_id: id of cluster to terminate
    * @param config_file: [configuration](https://github.com/BiBiServ/bibigrid/blob/master/documentation/markdown
    /features/configuration.md) yaml file
    * @return: message whether the cluster termination has been started.
    """
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

        return JSONResponse(content={"message": "Termination successfully requested."}, status_code=202)
    except Exception as exc:  # pylint: disable=broad-except
        return JSONResponse(content={"error": str(exc)}, status_code=400)


@app.post("/bibigrid/info/", response_model=InfoResponseModel)
async def info(cluster_id: str, config_file: UploadFile):
    """
    Expects a cluster id and a
    [configuration](https://github.com/BiBiServ/bibigrid/blob/master/documentation/markdown/features/configuration.md)
    file.

    Returns detailed cluster information, including whether the cluster is "ready".
    * @param cluster_id: id of cluster to get info on
    * @param config_file: [configuration](https://github.com/BiBiServ/bibigrid/blob/master/documentation/markdown
    /features/configuration.md) yaml file
    * @return: detailed cluster information
    """
    LOG.debug(f"Requested info on {cluster_id}.")
    cluster_id, log = setup(cluster_id)
    content = await config_file.read()
    configurations = yaml.safe_load(content.decode())
    try:
        providers = provider_handler.get_providers(configurations, log)
        cluster_dict = list_clusters.dict_clusters(providers, log).get(cluster_id, {})  # add information filtering
        if cluster_dict:
            cluster_dict["message"] = "Cluster found."
            cluster_dict["ready"] = is_up(cluster_id, log)
            return JSONResponse(content=cluster_dict, status_code=200)
        return JSONResponse(content={"message": "Cluster not found.", "ready": False}, status_code=404)
    except Exception as exc:  # pylint: disable=broad-except
        return JSONResponse(content={"error": str(exc)}, status_code=400)


@app.get("/bibigrid/log/", response_model=LogResponseModel)
async def get_log(cluster_id: str, lines: int = None):
    """
    Expects a cluster id and optional lines.

    Returns last lines of the .log for the given cluster id. If no lines are specified, all lines are returned.
    * @param cluster_id: id of cluster to get .log from
    * @param lines: lines to read from the end
    * @return: Message whether the log has been found and if found, the las lines lines of the logged text
    (or everything if lines were omitted).
    """
    LOG.debug(f"Requested log on {cluster_id}.")
    try:
        file_name = os.path.join(LOG_FOLDER, f"{cluster_id}.log")
        if os.path.isfile(file_name):
            if not lines:
                with open(file_name, "r", encoding="utf8") as log_file:
                    response = log_file.read()
            else:
                response = tail(file_name, lines)
            return JSONResponse(content={"message": "Log found", "log": response}, status_code=200)
        return JSONResponse(content={"message": "Log not found.", "log": None}, status_code=404)
    except Exception as exc:  # pylint: disable=broad-except
        return JSONResponse(content={"error": str(exc)}, status_code=400)


@app.get("/bibigrid/ready/", response_model=ReadyResponseModel)
async def ready(cluster_id: str):
    """
    Expects a cluster id.

    Returns whether the cluster with cluster id is ready according to the log file.
    If the running state of the cluster has been changed outside BiBiGrid REST, this method cannot detect this.

    In such cases checking [info](#info)'s ready value is more reliable as it includes a check whether the cluster
    actually exists on the provider. Ready omits checking the provider and is therefore less reliable, but faster.
    * @param cluster_id: id of cluster to get ready state from
    * @return: Message whether cluster is down or up and a bool "ready" whether the cluster is ready.
    """
    LOG.debug(f"Requested log on {cluster_id}.")
    try:
        cluster_id, log = setup(cluster_id)
        result = is_up(cluster_id, log)
        return JSONResponse(content={"message": "Cluster is up" if result else "Cluster is down.", "ready": result},
                            status_code=200)
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
