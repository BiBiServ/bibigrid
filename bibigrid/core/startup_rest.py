"""
Contains the main method for the BiBiGrid REST API. Interprets command line arguments, sets up cid sorted logging,
and starts the corresponding action based on the provided arguments.
"""
import argparse
import logging
import multiprocessing
import os
import subprocess
import sys
import threading

import uvicorn
import yaml
from fastapi import FastAPI, status, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from werkzeug.utils import secure_filename

from bibigrid.core.actions import create, terminate, list_clusters
from bibigrid.core.utility import validate_configuration, id_generation
from bibigrid.core.utility.handler import provider_handler, configuration_handler
from bibigrid.core.utility.paths.basic_path import CLUSTER_INFO_FOLDER, CLOUD_NODE_REQUIREMENTS_PATH, \
    ENFORCED_CONFIG_PATH, DEFAULT_CONFIG_PATH
from bibigrid.models.rest import ValidationResponseModel, CreateResponseModel, TerminateResponseModel, \
    InfoResponseModel, LogResponseModel, ClusterStateResponseModel, ConfigurationsModel, MinimalConfigurationsModel, \
    RequirementsModel

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


def setup(cluster_id, configurations_json=None):
    """
    Sets up the logger for the given cluster_id. If cluster_id is None, generates a new cluster ID.
    The logger is named after the cluster_id and logs to a file named cluster_id.log.

    @param cluster_id: The cluster ID or None.
    @return: A tuple containing the cluster_id and the configured logger.
    """
    if cluster_id:
        if cluster_id and (
                len(cluster_id) != id_generation.MAX_ID_LENGTH or not set(cluster_id).issubset(
            id_generation.CLUSTER_UUID_ALPHABET)):
            LOG.warning(f"Cluster id doesn't fit length ({id_generation.MAX_ID_LENGTH}) or defined alphabet "
                        f"({id_generation.CLUSTER_UUID_ALPHABET}). Aborting.")
            cluster_id = None  # this will lead to an abort
        else:
            cluster_id = secure_filename(cluster_id)
    else:
        cluster_id = id_generation.generate_cluster_id()
    log = logging.getLogger(cluster_id)
    log.setLevel(logging.DEBUG)
    if not log.handlers:
        log_handler = logging.FileHandler(os.path.join(LOG_FOLDER, f"{cluster_id}.log"))
        log_handler.setFormatter(LOG_FORMATTER)
        log.addHandler(log_handler)
    if cluster_id is None:
        log.error(f"Cluster id doesn't fit length ({id_generation.MAX_ID_LENGTH}) or defined alphabet "
                  f"({id_generation.CLUSTER_UUID_ALPHABET}). Aborting.")
        raise RuntimeError(f"Cluster id doesn't fit length ({id_generation.MAX_ID_LENGTH}) or defined alphabet "
                           f"({id_generation.CLUSTER_UUID_ALPHABET}). Aborting.")

    if configurations_json:
        configurations = configurations_json.model_dump(exclude_none=True)["configurations"]
        configurations = configuration_handler.merge_configurations(user_config=configurations,
                                                                    default_config_path=DEFAULT_CONFIG_PATH,
                                                                    enforced_config_path=ENFORCED_CONFIG_PATH,
                                                                    log=log)
        return cluster_id, log, configurations
    return cluster_id, log, None


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    exc_str = f'{exc}'.replace('\n', ' ').replace('   ', ' ')
    logging.error(f"{request}: {exc_str}")
    content = {'status_code': 10422, 'message': exc_str, 'data': None}
    return JSONResponse(content=content, status_code=status.HTTP_422_UNPROCESSABLE_ENTITY)


@app.get("/bibigrid/requirements", response_model=RequirementsModel)
async def get_requirements():
    with open(CLOUD_NODE_REQUIREMENTS_PATH, "r", encoding="UTF-8") as cloud_node_requirements_file:
        cloud_node_requirements = yaml.safe_load(cloud_node_requirements_file)
    return JSONResponse(content={"cloud_node_requirements": cloud_node_requirements}, status_code=200)


@app.post("/bibigrid/validate", response_model=ValidationResponseModel)
async def validate_configuration_json(configurations_json: ConfigurationsModel, cluster_id: str = None):
    """
    Validates the given configuration JSON against the specified cluster ID.

    @param cluster_id: Optional cluster ID. If not provided, a new ID is generated.
    @param configurations_json: The configuration JSON to be validated.
    @return: A JSON response indicating the success or failure of the validation.
    """
    LOG.info(f"Requested validation on {cluster_id}")

    try:
        cluster_id, log, configurations = setup(cluster_id, configurations_json)
    except RuntimeError:
        return JSONResponse(content={"message": "Cluster id malformed."}, status_code=422)

    try:
        providers = provider_handler.get_providers(configurations, log)
        success = validate_configuration.ValidateConfiguration(configurations, providers, log).validate()
        if not success:
            return JSONResponse(
                content={"message": "Validation failed", "cluster_id": cluster_id, "success": success},
                status_code=420)
        return JSONResponse(
            content={"message": "Validation successful", "cluster_id": cluster_id, "success": success},
            status_code=200)
    except Exception as exc:  # pylint: disable=broad-except
        log.error(f"{type(exc).__name__}: {str(exc)}")
        return JSONResponse(content={"message": "Internal server error."}, status_code=400)


@app.post("/bibigrid/create/", response_model=CreateResponseModel)
async def create_cluster(configurations_json: ConfigurationsModel, cluster_id: str = None):
    """
    Initiates the creation of a cluster based on the provided configuration JSON.

    @param cluster_id: Optional cluster ID. If not provided, a new ID is generated.
    @param configurations_json: The configuration JSON for the cluster.
    @return: A JSON response indicating whether the cluster creation has started and the cluster ID.
    """
    LOG.debug(f"Requested creation on {cluster_id}")

    try:
        cluster_id, log, configurations = setup(cluster_id, configurations_json)
    except RuntimeError:
        return JSONResponse(content={"message": "Cluster id malformed."}, status_code=422)

    try:
        providers = provider_handler.get_providers(configurations, log)
        if not id_generation.is_unique_cluster_id(cluster_id=cluster_id, providers=providers):
            return JSONResponse(content={"message": f"Cluster with cluster id {cluster_id} is already running."},
                                status_code=400)
        creator = create.Create(providers=providers, configurations=configurations, log=log,
                                config_path=None, cluster_id=cluster_id)
        cluster_id = creator.cluster_id
        thread = threading.Thread(target=creator.create, args=())
        thread.start()
        return JSONResponse(content={"message": "Cluster creation started.", "cluster_id": cluster_id}, status_code=202)
    except ValueError as exc:  # pylint: disable=broad-except
        log.error(f"{type(exc).__name__}: {str(exc)}")
        return JSONResponse(content={"message": "Internal server error."}, status_code=400)


@app.post("/bibigrid/terminate/{cluster_id}", response_model=TerminateResponseModel)
async def terminate_cluster(cluster_id: str, configurations_json: MinimalConfigurationsModel):
    """
    Initiates the termination of a cluster based on the provided configuration JSON.

    @param cluster_id: The cluster ID to terminate.
    @param configurations_json: The configuration JSON for the cluster.
    @return: A JSON response indicating whether the cluster termination has started.
    """
    LOG.debug(f"Requested termination on {cluster_id}")

    try:
        cluster_id, log, configurations = setup(cluster_id, configurations_json)
    except RuntimeError:
        return JSONResponse(content={"message": "Cluster id malformed."}, status_code=422)

    try:
        providers = provider_handler.get_providers(configurations, log)
        thread = threading.Thread(target=terminate.terminate, args=(cluster_id, providers, log))
        thread.start()
        return JSONResponse(content={"message": "Termination successfully requested."}, status_code=202)
    except Exception as exc:  # pylint: disable=broad-except
        log.error(f"{type(exc).__name__}: {str(exc)}")
        return JSONResponse(content={"message": "Internal server error."}, status_code=400)


@app.post("/bibigrid/info/{cluster_id}", response_model=InfoResponseModel)
async def info(cluster_id: str, configurations_json: MinimalConfigurationsModel):
    """
    Retrieves detailed information about the specified cluster.

    @param cluster_id: The cluster ID to get information on.
    @param configurations_json: The configuration JSON for the cluster.
    @return: A JSON response containing detailed cluster information.
    """
    LOG.debug(f"Requested info on {cluster_id}.")
    cluster_id, log, configurations = setup(cluster_id, configurations_json)
    try:
        providers = provider_handler.get_providers(configurations, log)
        cluster_dict = list_clusters.dict_clusters(providers, log).get(cluster_id, {})  # add information filtering
        if cluster_dict:
            cluster_dict["message"] = "Cluster found."
            return JSONResponse(content=cluster_dict, status_code=200)
        return JSONResponse(content={"message": "Cluster not found.", "ready": False}, status_code=404)
    except Exception as exc:  # pylint: disable=broad-except
        log.error(f"{type(exc).__name__}: {str(exc)}")
        return JSONResponse(content={"message": "Internal server error."}, status_code=400)


@app.get("/bibigrid/log/{cluster_id}", response_model=LogResponseModel)
async def get_log(cluster_id: str, lines: int = None):
    """
    Retrieves the log for the specified cluster ID.

    @param cluster_id: The cluster ID to get the log from.
    @param lines: Optional. The number of lines to read from the end of the log.
    @return: A JSON response containing the log text.
    """
    LOG.debug(f"Requested log on {cluster_id}.")

    try:
        cluster_id, log, _ = setup(cluster_id)
    except RuntimeError:
        return JSONResponse(content={"message": "Cluster id malformed."}, status_code=422)

    try:
        base_path = os.path.normpath(LOG_FOLDER)
        file_name = os.path.normpath(os.path.join(base_path, f"{cluster_id}.log"))
        if not file_name.startswith(base_path):
            raise ValueError(f"Invalid file path. Must start with {base_path}.")
        if os.path.isfile(file_name):
            if not lines:
                with open(file_name, "r", encoding="UTF-8") as log_file:
                    response = log_file.read()
            else:
                response = tail(file_name, lines)
            return JSONResponse(content={"message": "Log found", "log": response}, status_code=200)
        return JSONResponse(content={"message": "Log not found.", "log": None}, status_code=404)
    except Exception as exc:  # pylint: disable=broad-except
        log.error(f"{type(exc).__name__}: {str(exc)}")
        return JSONResponse(content={"message": "Internal server error."}, status_code=400)


@app.get("/bibigrid/state/{cluster_id}", response_model=ClusterStateResponseModel)
async def state(cluster_id: str):
    """
    Retrieves the state of the specified cluster.
    The state can be used to determine whether create has finished already.

    @param cluster_id: The cluster ID to get the state from.
    @return: A JSON response containing the cluster state.
    """
    LOG.debug(f"Requested log on {cluster_id}.")

    try:
        cluster_id, log, _ = setup(cluster_id)
    except RuntimeError:
        return JSONResponse(content={"message": "Cluster id malformed."}, status_code=422)

    try:
        cluster_info_path = os.path.normpath(os.path.join(CLUSTER_INFO_FOLDER, f"{cluster_id}.yaml"))
        if not cluster_info_path.startswith(os.path.normpath(CLUSTER_INFO_FOLDER)):
            log.warning("Invalid cluster_id resulting in path traversal")
            raise ValueError("Invalid cluster_id resulting in path traversal")
        if os.path.isfile(cluster_info_path):
            log.debug(f"Found cluster state for cluster_id {cluster_id}")
            with open(cluster_info_path, mode="r", encoding="UTF-8") as cluster_info_file:
                cluster_state = yaml.safe_load(cluster_info_file)
            return JSONResponse(content=cluster_state,
                                status_code=200)
        log.info(f"Couldn't find cluster state for cluster_id {cluster_id}")
        return JSONResponse(content={"message": "Cluster not found.", "cluster_id": None, "floating_ip": None,
                                     "ssh_user": None}, status_code=404)
    except Exception as exc:  # pylint: disable=broad-except
        log.error(f"{type(exc).__name__}: {str(exc)}")
        return JSONResponse(content={"message": "Internal server error."}, status_code=400)


def check_clouds_yaml(clouds):
    clouds_yaml_check = validate_configuration.ValidateConfiguration([{"cloud": cloud} for cloud in clouds], None,
                                                                     LOG).check_clouds_yamls()
    clouds_yaml_security_check = validate_configuration.check_clouds_yaml_security(LOG)
    if clouds_yaml_check and clouds_yaml_security_check:
        LOG.info(
            "Clouds yaml check successful.")
    else:
        message = (f"Clouds yaml check failed! Aborting. Clouds yaml check successful: {clouds_yaml_check}. "
                   f"Security check successful: {clouds_yaml_security_check}.")
        LOG.warning(message)
        print(message)
        sys.exit(0)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='BiBiGrid REST easily sets up clusters within a cloud environment')
    parser.add_argument("-c", "--clouds", default=["openstack"], nargs="+",
                        help="Names of clouds.yaml entries to check on startup.")
    args = parser.parse_args()

    check_clouds_yaml(args.clouds)

    uvicorn.run("bibigrid.core.startup_rest:app", host="0.0.0.0", port=8000,
                workers=multiprocessing.cpu_count() * 2 + 1)
