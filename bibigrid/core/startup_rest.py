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
from fastapi import FastAPI, status, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from bibigrid.core.actions import check, create, terminate, list_clusters
from bibigrid.core.rest.models import ValidationResponseModel, CreateResponseModel, TerminateResponseModel, \
    InfoResponseModel, LogResponseModel, ClusterStateResponseModel, ConfigurationsModel, MinimalConfigurationsModel
from bibigrid.core.utility import id_generation
from bibigrid.core.utility.handler import provider_handler
from bibigrid.core.utility.paths.basic_path import CLUSTER_INFO_FOLDER

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
file_handler = logging.FileHandler(os.path.join(LOG_FOLDER, "bibigrid_rest.log"))
file_handler.setFormatter(LOG_FORMATTER)
LOG.addHandler(file_handler)
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


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    exc_str = f'{exc}'.replace('\n', ' ').replace('   ', ' ')
    logging.error(f"{request}: {exc_str}")
    content = {'status_code': 10422, 'message': exc_str, 'data': None}
    return JSONResponse(content=content, status_code=status.HTTP_422_UNPROCESSABLE_ENTITY)


@app.post("/bibigrid/validate", response_model=ValidationResponseModel)
async def validate_configuration(configurations_json: ConfigurationsModel, cluster_id: str = None):
    """
    Expects a cluster id and a
    [configuration](https://github.com/BiBiServ/bibigrid/blob/master/documentation/markdown/features/configuration.md)
    as json.

    Returns validation result (success, or failure)
    * @param cluster_id: optional id of to be created cluster in order to log into the same file.
    If not given, one is generated.
    * @param configurations_json: [configuration](https://github.com/BiBiServ/bibigrid/blob/master/documentation/markdown
    /features/configuration.md)
    * @return: success or failure of the validation
    """
    cluster_id, log = setup(cluster_id)
    LOG.info(f"Requested validation on {cluster_id}")
    try:
        configurations = configurations_json.model_dump(exclude_none=True)["configurations"]
        providers = provider_handler.get_providers(configurations, log)
        exit_state = check.check(configurations, providers, log)
        if exit_state:
            return JSONResponse(
                content={"message": "Validation failed", "cluster_id": cluster_id, "success": not bool(exit_state)},
                status_code=420)
        return JSONResponse(
            content={"message": "Validation successful", "cluster_id": cluster_id, "success": not bool(exit_state)},
            status_code=200)
    except Exception as exc:  # pylint: disable=broad-except
        return JSONResponse(content={"error": type(exc).__name__, "message": str(exc)}, status_code=400)


@app.post("/bibigrid/create/", response_model=CreateResponseModel)
async def create_cluster(configurations_json: ConfigurationsModel, cluster_id: str = None):
    """
    Expects an optional cluster id and a
    [configuration](https://github.com/BiBiServ/bibigrid/blob/master/documentation/markdown/features/configuration.md)
    as json.

    Returns the cluster id and whether cluster creation (according to the configuration) has started.
    Using '/state' you can see if the cluster is ready.
    * @param cluster_id: optional UUID with 15 letters. if not given, one is generated
    * @param configurations_json: [configuration](https://github.com/BiBiServ/bibigrid/blob/master/documentation/markdown
    /features/configuration.md)
    * @return: message whether the cluster creation has been started and cluster id
    """
    LOG.debug(f"Requested creation on {cluster_id}")
    cluster_id, log = setup(cluster_id)

    async def create_async():
        creator.create()

    try:
        configurations = configurations_json.model_dump(exclude_none=True)["configurations"]
        providers = provider_handler.get_providers(configurations, log)
        creator = create.Create(providers=providers, configurations=configurations, log=log,
                                config_path=None, cluster_id=cluster_id)
        cluster_id = creator.cluster_id
        asyncio.create_task(create_async())
        return JSONResponse(content={"message": "Cluster creation started.", "cluster_id": cluster_id}, status_code=202)
    except ValueError as exc:  # pylint: disable=broad-except
        return JSONResponse(content={"error": str(exc)}, status_code=400)


@app.post("/bibigrid/terminate/{cluster_id}", response_model=TerminateResponseModel)
async def terminate_cluster(cluster_id: str, configurations_json: MinimalConfigurationsModel):
    """
    Expects a cluster id and a
    [configuration](https://github.com/BiBiServ/bibigrid/blob/master/documentation/markdown/features/configuration.md)
    as json.

    Returns whether cluster termination (according to the configuration) has started.
    Using '/state' you can see if the cluster has been terminated.
    * @param cluster_id: id of cluster to terminate
    * @param configurations_json: [configuration](https://github.com/BiBiServ/bibigrid/blob/master/documentation/markdown
    /features/configuration.md)
    * @return: message whether the cluster termination has been started.
    """
    cluster_id, log = setup(cluster_id)
    LOG.debug(f"Requested termination on {cluster_id}")

    async def terminate_async():
        terminate.terminate(cluster_id, providers, log)

    try:
        configurations = configurations_json.model_dump()["configurations"]
        providers = provider_handler.get_providers(configurations, log)
        asyncio.create_task(terminate_async())

        return JSONResponse(content={"message": "Termination successfully requested."}, status_code=202)
    except Exception as exc:  # pylint: disable=broad-except
        return JSONResponse(content={"error": type(exc).__name__, "message":str(exc)}, status_code=400)


@app.post("/bibigrid/info/{cluster_id}", response_model=InfoResponseModel)
async def info(cluster_id: str, configurations_json: MinimalConfigurationsModel):
    """
    Expects a cluster id and a
    [configuration](https://github.com/BiBiServ/bibigrid/blob/master/documentation/markdown/features/configuration.md)
    as json.

    Returns detailed cluster information.
    * @param cluster_id: id of cluster to get info on
    * @param config_file: [configuration](https://github.com/BiBiServ/bibigrid/blob/master/documentation/markdown
    /features/configuration.md) yaml file
    * @return: detailed cluster information
    """
    LOG.debug(f"Requested info on {cluster_id}.")
    cluster_id, log = setup(cluster_id)
    try:
        configurations = configurations_json.model_dump()["configurations"]
        providers = provider_handler.get_providers(configurations, log)
        cluster_dict = list_clusters.dict_clusters(providers, log).get(cluster_id, {})  # add information filtering
        if cluster_dict:
            cluster_dict["message"] = "Cluster found."
            return JSONResponse(content=cluster_dict, status_code=200)
        return JSONResponse(content={"message": "Cluster not found.", "ready": False}, status_code=404)
    except Exception as exc:  # pylint: disable=broad-except
        return JSONResponse(content={"error": type(exc).__name__, "message": str(exc)}, status_code=400)


@app.get("/bibigrid/log/{cluster_id}", response_model=LogResponseModel)
async def get_log(cluster_id: str, lines: int = None):
    """
    Expects a cluster id and, optional, lines.

    Returns last lines of the .log for the given cluster id. If no lines are specified, all lines are returned.
    * @param cluster_id: id of cluster to get .log from
    * @param lines: lines to read from the end
    * @return: Message whether the log has been found and if found, the last lines of the logged text
    (or everything if lines has been omitted).
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
        return JSONResponse(content={"error": type(exc).__name__, "message": str(exc)}, status_code=400)


@app.get("/bibigrid/state/{cluster_id}", response_model=ClusterStateResponseModel)
async def state(cluster_id: str):
    """
    Expects a cluster id.

    Returns the cluster's state according to the ~/.config/bibigrid/cluster_info/{cluster_id}.yaml file.
    If the running state of the cluster has been changed outside BiBiGrid, this method cannot detect that.
    * @param cluster_id: id of cluster to get state from
    * @return: cluster state
    """
    LOG.debug(f"Requested log on {cluster_id}.")
    try:
        cluster_id, log = setup(cluster_id)
        cluster_info_path = os.path.normpath(os.path.join(CLUSTER_INFO_FOLDER, f"{cluster_id}.yaml"))
        if not cluster_info_path.startswith(os.path.normpath(CLUSTER_INFO_FOLDER)):
            raise ValueError("Invalid cluster_id resulting in path traversal")
        if os.path.isfile(cluster_info_path):
            with open(cluster_info_path, mode="r", encoding="UTF-8") as cluster_info_file:
                cluster_state = yaml.safe_load(cluster_info_file)
            return JSONResponse(content=cluster_state,
                                status_code=200)
        else:
            return JSONResponse(content={"message": "Cluster not found.", "cluster_id": None, "floating_ip": None,
                                         "ssh_user":None}, status_code=404)
    except Exception as exc:  # pylint: disable=broad-except
        return JSONResponse(content={"error": type(exc).__name__, "message": str(exc)}, status_code=400)


if __name__ == "__main__":
    uvicorn.run("bibigrid.core.startup_rest:app", host="0.0.0.0", port=8000,
                workers=multiprocessing.cpu_count() * 2 + 1)  # Use the on_starting event
