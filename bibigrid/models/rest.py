"""
This module contains models used by the REST api
"""

from typing import Dict, List, Optional, Literal, Union, Annotated

from pydantic import BaseModel, Field, IPvAnyAddress, StringConstraints, model_validator

MetaKey = Annotated[str, StringConstraints(max_length=255)]
MetaValue = Annotated[str, StringConstraints(max_length=255)]
MetaDict = Dict[MetaKey, MetaValue]


class StrictModel(BaseModel):
    model_config = {
        "extra": "forbid",
        "strict": True,
    }


# pylint: disable=too-few-public-methods

class Role(StrictModel):
    """
    Ansible Role
    """
    name: str
    tags: Optional[List[str]]


class UserRole(StrictModel):
    """
    Allows users to add custom ansible roles
    """
    hosts: List[str]
    roles: List[Role]
    varsFiles: Optional[List[str]] = Field(default_factory=list)


class CloudScheduling(StrictModel):
    """
    Model for cloud scheduling
    """
    sshTimeout: Optional[int] = 5


class BootVolume(StrictModel):
    """
    Holds information about where the server boots from
    """
    name: Optional[str] = None
    terminate: Optional[bool] = True
    size: Optional[int] = 50


class Volume(StrictModel):
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
    id: Optional[str] = None

    @model_validator(mode="after")
    def only_one_flag(self):
        flags = [self.permanent, self.semiPermanent, self.exists]
        if sum(flags) > 1:
            raise ValueError(
                "Only one of permanent, semiPermanent, or exists may be true"
            )
        return self


class Instance(StrictModel):
    """
    Holds instance/server information
    """
    type: str
    image: str
    count: Optional[int] = 1
    onDemand: Optional[bool] = True
    partitions: Optional[List[str]] = Field(default_factory=list)
    features: Optional[List[str]] = Field(default_factory=list)
    bootVolume: Optional[BootVolume] = None
    volumes: Optional[List[Volume]] = Field(default_factory=list)
    meta: Optional[MetaDict] = None
    securityGroups: Optional[list[str]] = Field(default_factory=list)
    serverGroup: Optional[str] = None


class ElasticScheduling(StrictModel):
    """
    Holds info on Slurms scheduling
    """
    SuspendTime: Optional[int] = 1800
    SuspendTimeout: Optional[int] = 90
    ResumeTimeout: Optional[int] = 1800
    TreeWidth: Optional[int] = 128


class SlurmConf(StrictModel):
    """
    Holds info on basic Slurm settings
    """
    db: Optional[str] = "slurm"
    db_user: Optional[str] = "slurm"
    db_password: Optional[str] = "changeme"
    munge_key: Optional[str] = None
    elastic_scheduling: Optional[ElasticScheduling] = None


class Gateway(StrictModel):
    """
    Holds info regarding whether a gateway is used to connect to the master
    """
    ip: str
    portFunction: str


class MasterConfig(StrictModel):
    """
    Holds info regarding the configuration
    """
    infrastructure: Literal["openstack"]  # currently limited to openstack
    cloud: str = "openstack"
    sshUser: str
    subnet: Optional[str] = Field(default=None)
    network: Optional[str] = Field(default=None)
    cloud_identifier: str = "openstack"
    securityGroups: Optional[list[str]] = Field(default_factory=list)
    serverGroup: Optional[str] = None
    sshPublicKeyFiles: Optional[List[str]] = Field(default_factory=list)
    sshPublicKeys: Optional[List[str]] = Field(default_factory=list)
    sshTimeout: Optional[int] = 5
    cloudScheduling: Optional[CloudScheduling] = None
    nfsShares: Optional[List[str]] = Field(default_factory=list)
    userRoles: Optional[List[UserRole]] = Field(default_factory=list)
    localFS: Optional[bool] = False
    localDNSlookup: Optional[bool] = False
    slurm: Optional[bool] = True
    slurmConf: Optional[SlurmConf] = None
    zabbix: Optional[bool] = False
    nfs: Optional[bool] = False
    ide: Optional[bool] = False
    useMasterAsCompute: Optional[bool] = True
    useMasterWithPublicIp: Optional[bool] = True
    waitForServices: Optional[List[str]] = Field(default_factory=list)
    gateway: Optional[Gateway] = None
    dontUploadCredentials: Optional[bool] = False
    fallbackOnOtherImage: Optional[bool] = False
    features: Optional[List[str]] = Field(default_factory=list)
    workerInstances: List[Instance]
    masterInstance: Instance
    bootVolume: Optional[BootVolume] = None
    noAllPartition: Optional[bool] = False
    meta: Optional[MetaDict] = None

    @model_validator(mode="after")
    def subnet_xor_network(self):
        if not bool(self.subnet) != bool(self.network):
            raise ValueError(
                "Either 'subnet' or 'network' must be defined (XOR); neither both, nor none!"
            )
        return self


class OtherConfig(StrictModel):
    """
    Holds info about other configurations
    """
    infrastructure: Literal["openstack"]  # currently limited to openstack
    cloud: str = "openstack"
    sshUser: str
    subnet: Optional[str] = Field(default=None)
    network: Optional[str] = Field(default=None)
    cloud_identifier: str = "openstack"
    securityGroups: Optional[list[str]] = None
    serverGroup: Optional[str] = None
    waitForServices: Optional[List[str]] = Field(default_factory=list)
    features: Optional[List[str]] = Field(default_factory=list)
    workerInstances: List[Instance]
    vpnInstance: Instance
    bootVolume: Optional[BootVolume] = None
    meta: Optional[MetaDict] = None

    @model_validator(mode="after")
    def subnet_xor_network(self):
        if not bool(self.subnet) != bool(self.network):
            raise ValueError(
                "Either 'subnet' or 'network' must be defined (XOR); neither both, nor none!"
            )
        return self


class ConfigurationsModel(StrictModel):
    """
    Model for configurations
    """
    configurations: List[Union[MasterConfig, OtherConfig]]


class MinimalConfigurationModel(StrictModel):
    """
    Minimal model for a configuration. Containing only info to load clouds.yaml and to connect to provider.
    """
    infrastructure: Literal["openstack"]
    cloud: str = "openstack"


class MinimalConfigurationsModel(StrictModel):
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
