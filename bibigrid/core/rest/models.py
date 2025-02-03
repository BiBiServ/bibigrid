from typing import List, Optional, Literal
from pydantic import BaseModel, Field, IPvAnyAddress


# pylint: disable=too-few-public-methods


class BootVolume(BaseModel):
    name: Optional[str]
    terminate: Optional[bool]
    size: Optional[int]


class Volume(BaseModel):
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
    type: str
    image: str
    count: Optional[int]
    onDemand: Optional[bool]
    partitions: Optional[List[str]]
    features: Optional[List[str]]
    bootVolume: Optional[BootVolume]
    volumes: Optional[List[Volume]]


class UserRole(BaseModel):
    hosts: List[str]
    roles: List[dict]  # Replace 'dict' with more specific type if possible
    varsFiles: Optional[List[str]]


class ElasticScheduling(BaseModel):
    SuspendTime: Optional[int]
    SuspendTimeout: Optional[int]
    ResumeTimeout: Optional[int]
    TreeWidth: Optional[int]


class SlurmConf(BaseModel):
    db: Optional[str]
    db_user: Optional[str]
    db_password: Optional[str]
    munge_key: Optional[str]
    elastic_scheduling: Optional[ElasticScheduling]


class Gateway(BaseModel):
    ip: str
    portFunction: str


class ConfigModel(BaseModel):
    infrastructure: str
    cloud: str
    sshUser: str
    subnet: Optional[str] = Field(default=None)
    network: Optional[str] = Field(default=None)
    cloud_identifier: str
    sshPublicKeyFiles: Optional[List[str]]
    sshTimeout: Optional[int]
    cloudScheduling: Optional[dict]  # Modify if you have a more definite structure
    autoMount: Optional[bool]
    nfsShares: Optional[List[str]]
    userRoles: Optional[List[UserRole]]
    localFS: Optional[bool]
    localDNSlookup: Optional[bool]
    slurm: Optional[bool]
    slurmConf: Optional[SlurmConf]
    zabbix: Optional[bool]
    nfs: Optional[bool]
    ide: Optional[bool]
    useMasterAsCompute: Optional[bool]
    useMasterWithPublicIp: Optional[bool]
    waitForServices: Optional[List[str]]
    gateway: Optional[Gateway]
    dontUploadCredentials: Optional[bool]
    fallbackOnOtherImage: Optional[bool]
    localDNSLookup: Optional[bool]
    features: Optional[List[str]]
    workerInstances: List[Instance]
    masterInstance: Instance
    vpngtw: Optional[Instance]
    bootVolume: Optional[BootVolume]


class OtherConfigModel(ConfigModel):
    vpnInstance: Instance


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


class ClusterStateResponseModel(BaseModel):
    cluster_id: str
    floating_ip: IPvAnyAddress
    message: str
    ssh_user: str
    state: Literal[200, 201, 204, 404, 500]
