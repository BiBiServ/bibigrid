from typing import List, Optional, Union
from pydantic import BaseModel, Field


# pylint: disable=too-few-public-methods

class Role(BaseModel):
    name: str
    tags: Optional[List[str]]

class UserRole(BaseModel):
    hosts: List[str]
    roles: List[Role]
    varsFiles: Optional[List[str]] = Field(default=[])

class CloudScheduling(BaseModel):
    sshTimeout: Optional[int] = 5

class BootVolume(BaseModel):
    name: Optional[str] = None
    terminate: Optional[bool] = True
    size: Optional[int] = 50

class Volume(BaseModel):
    name: Optional[str] = None
    snapshot: Optional[str] = None
    permanent: Optional[bool] = False
    semiPermanent: Optional[bool] = False
    exists: Optional[bool] = False
    mountPoint: Optional[str] = None
    size: Optional[int] = 50
    fstype: Optional[str] = None
    type: Optional[str] = None

class Instance(BaseModel):
    type: str
    image: str
    onDemand: Optional[bool] = True
    partitions: Optional[List[str]] = Field(default=[])
    features: Optional[List[str]] = Field(default=[])
    bootVolume: Optional[BootVolume] = None
    volumes: Optional[List[Volume]] = Field(default=[])

class ElasticScheduling(BaseModel):
    SuspendTime: Optional[int] = 1800
    SuspendTimeout: Optional[int] = 90
    ResumeTimeout: Optional[int] = 1800
    TreeWidth: Optional[int] = 128

class SlurmConf(BaseModel):
    db: Optional[str] = "slurm"
    db_user: Optional[str] = "slurm"
    db_password: Optional[str] = "changeme"
    munge_key: Optional[str] = None
    elastic_scheduling: Optional[ElasticScheduling] = None

class Gateway(BaseModel):
    ip: str
    portFunction: str

class MasterConfig(BaseModel):
    infrastructure: str
    cloud: str
    sshUser: str
    subnet: Optional[str] = Field(default=None)
    network: Optional[str] = Field(default=None)
    cloud_identifier: Optional[str] = None
    sshPublicKeyFiles: Optional[List[str]] = Field(default=[])
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
    infrastructure: str
    cloud: str
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
    configurations: List[Union[MasterConfig, OtherConfig]]



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
