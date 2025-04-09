"""
This module contains models used by the REST api
"""

from typing import Dict, List, Optional, Literal, Union

from pydantic import BaseModel, Field, IPvAnyAddress


# pylint: disable=too-few-public-methods

class Role(BaseModel):
    """
    Ansible Role
    """
    name: str
    tags: Optional[List[str]]


class UserRole(BaseModel):
    """
    Allows users to add custom ansible roles
    """
    hosts: List[str]
    roles: List[Role]
    varsFiles: Optional[List[str]] = Field(default=[])


class CloudScheduling(BaseModel):
    """
    Model for cloud scheduling
    """
    sshTimeout: Optional[int] = 5


class BootVolume(BaseModel):
    """
    Holds information about where the server boots from
    """
    name: Optional[str] = None
    terminate: Optional[bool] = True
    size: Optional[int] = 50


class Volume(BaseModel):
    """
    Holds volume/attached storage information
    """
    name: Optional[str] = None
    snapshot: Optional[str] = None
    permanent: Optional[bool] = False
    semiPermanent: Optional[bool] = False
    exists: Optional[bool] = False
    mountPoint: Optional[str] = None
    size: Optional[int] = 50
    fstype: Optional[str] = None
    type: Optional[str] = None
    name: Optional[str]
    snapshot: Optional[str]
    permanent: Optional[bool]
    semiPermanent: Optional[bool]
    exists: Optional[bool]
    mountPoint: Optional[str]
    size: Optional[int]
    fstype: Optional[str]
    type: Optional[str]


class Instance(BaseModel):
    """
    Holds instance/server information
    """
    type: str
    image: str
    count: Optional[int] = 1
    onDemand: Optional[bool] = True
    partitions: Optional[List[str]] = Field(default=[])
    features: Optional[List[str]] = Field(default=[])
    bootVolume: Optional[BootVolume] = None
    volumes: Optional[List[Volume]] = Field(default=[])


class ElasticScheduling(BaseModel):
    """
    Holds info on Slurms scheduling
    """
    SuspendTime: Optional[int] = 1800
    SuspendTimeout: Optional[int] = 90
    ResumeTimeout: Optional[int] = 1800
    TreeWidth: Optional[int] = 128


class SlurmConf(BaseModel):
    """
    Holds info on basic Slurm settings
    """
    db: Optional[str] = "slurm"
    db_user: Optional[str] = "slurm"
    db_password: Optional[str] = "changeme"
    munge_key: Optional[str] = None
    elastic_scheduling: Optional[ElasticScheduling] = None


class Gateway(BaseModel):
    """
    Holds info regarding whether a gateway is used to connect to the master
    """
    ip: str
    portFunction: str


class MasterConfig(BaseModel):
    """
    Holds info regarding the configuration
    """
    infrastructure: Literal["openstack"]  # currently limited to openstack
    cloud: str = "openstack"
    sshUser: str
    subnet: Optional[str] = Field(default=None)
    network: Optional[str] = Field(default=None)
    cloud_identifier: Optional[str] = None
    sshPublicKeyFiles: Optional[List[str]] = Field(default=[])
    sshPublicKeys: Optional[List[str]] = Field(default=None)
    sshTimeout: Optional[int] = 5
    cloudScheduling: Optional[CloudScheduling] = None
    nfsShares: Optional[List[str]] = Field(default=[])
    userRoles: Optional[List[UserRole]] = Field(default=[])
    localFS: Optional[bool] = False
    localDNSlookup: Optional[bool] = False
    slurm: Optional[bool] = True
    slurmConf: Optional[SlurmConf] = None
    zabbix: Optional[bool] = False
    nfs: Optional[bool] = False
    ide: Optional[bool] = False
    useMasterAsCompute: Optional[bool] = True
    useMasterWithPublicIp: Optional[bool] = True
    waitForServices: Optional[List[str]] = Field(default=[])
    gateway: Optional[Gateway] = None
    dontUploadCredentials: Optional[bool] = False
    fallbackOnOtherImage: Optional[bool] = False
    features: Optional[List[str]] = Field(default=[])
    workerInstances: List[Instance]
    masterInstance: Instance
    bootVolume: Optional[BootVolume] = None


class OtherConfig(BaseModel):
    """
    Holds info about other configurations
    """
    infrastructure: Literal["openstack"]  # currently limited to openstack
    cloud: str = "openstack"
    sshUser: str
    subnet: Optional[str] = Field(default=None)
    network: Optional[str] = Field(default=None)
    cloud_identifier: Optional[str] = None
    waitForServices: Optional[List[str]] = Field(default=[])
    features: Optional[List[str]] = Field(default=[])
    workerInstances: List[Instance]
    vpnInstance: Instance
    bootVolume: Optional[BootVolume] = None


class ConfigurationsModel(BaseModel):
    """
    Model for configurations
    """
    configurations: List[Union[MasterConfig, OtherConfig]]


class MinimalConfigurationModel(BaseModel):
    """
    Minimal model for a configuration. Containing only info to load clouds.yaml and to connect to provider.
    """
    infrastructure: Literal["openstack"]
    cloud: str = "openstack"


class MinimalConfigurationsModel(BaseModel):
    """
    Minimal model for configurations.
    """
    configurations: List[MinimalConfigurationModel]


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
