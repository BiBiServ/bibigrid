from typing import Literal, List, Dict

from pydantic import BaseModel, IPvAnyAddress


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
    Model for get_log
    """
    message: str
    log: str


class ClusterStateResponseModel(BaseModel):
    """
    Response model for state
    """
    cluster_id: str
    floating_ip: IPvAnyAddress
    message: str
    ssh_user: str
    state: Literal["starting", "running", "terminated", "failed"]
    last_changed: str


class OsModel(BaseModel):
    """
    Model for operating system requirements description
    """
    os_versions: List[str]


class CloudNodeRequirementsModel(BaseModel):
    """
    Model for cloud_node_requirements.yaml
    """
    os_distro: Dict[str, OsModel]


class RequirementsModel(BaseModel):
    """
    Response model for requirements
    """
    cloud_node_requirements: CloudNodeRequirementsModel
