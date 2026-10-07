"""
This module contains models regarding the configuration yaml
"""

from typing import Dict, List, Optional, Literal, Annotated

from pydantic import BaseModel, Field, StringConstraints, model_validator

MetaKey = Annotated[str, StringConstraints(max_length=255)]
MetaValue = Annotated[str, StringConstraints(max_length=255)]
MetaDict = Dict[MetaKey, MetaValue]


class StrictModel(BaseModel):
    """
    Enforces that every key undefined by the corresponding model raises an error
    """
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
    tags: Optional[List[str]] = None


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


class Package(StrictModel):
    """
    Holds info on packages to be installed on the cluster
    """
    version: Optional[str] = "slurm-bibigrid-24.11"
    experimental_url: Optional[str] = "https://s3.bi.denbi.de/bibigrid/slurm-bibigrid-experimental.deb"
    use_experimental: Optional[bool] = False

class SlurmConf(StrictModel):
    """
    Holds info on basic Slurm settings
    """
    default_partition: Optional[str] = None
    db: Optional[str] = "slurm"
    db_user: Optional[str] = "slurm"
    db_password: Optional[str] = "changeme"
    munge_key: Optional[str] = None
    elastic_scheduling: Optional[ElasticScheduling] = None
    package: Optional[Package] = None

class Gateway(StrictModel):
    """
    Holds info regarding whether a gateway is used to connect to the master
    """
    ip: str
    portFunction: str


class BaseConfig(StrictModel):
    """
    Holds base keys for both master and others
    """
    infrastructure: Literal["openstack"]  # currently limited to openstack
    cloud: str = "openstack"
    cloud_identifier: Optional[str] = None
    sshUser: str
    subnet: Optional[str] = Field(default=None)
    network: Optional[str] = Field(default=None)
    floatingIpId: Optional[str] = Field(default=None)
    securityGroups: Optional[list[str]] = Field(default_factory=list)
    serverGroup: Optional[str] = None
    waitForServices: Optional[List[str]] = Field(default_factory=list)
    features: Optional[List[str]] = Field(default_factory=list)
    workerInstances: List[Instance]
    bootVolume: Optional[BootVolume] = None
    meta: Optional[MetaDict] = None

    @model_validator(mode="after")
    def subnet_xor_network(self):
        if bool(self.subnet) == bool(self.network):
            raise ValueError(
                "Either 'subnet' or 'network' must be defined (XOR); neither both, nor none!"
            )
        return self


class MasterConfig(BaseConfig):
    """
    Holds info regarding the configuration
    """
    masterInstance: Instance
    dns_server_list: Optional[List[str]] = Field(default_factory=list)
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
    gateway: Optional[Gateway] = None
    dontUploadCredentials: Optional[bool] = False
    fallbackOnOtherImage: Optional[bool] = False
    bootVolume: Optional[BootVolume] = None
    noAllPartition: Optional[bool] = False


class OtherConfig(BaseConfig):
    """
    Holds info about other configurations
    """
    vpnInstance: Instance


class ConfigurationsModel(StrictModel):
    """
    Model for configurations
    """
    master: MasterConfig
    others: List[OtherConfig]

    # the following are two "hack" methods until the configuration file is more pydantic
    @model_validator(mode="before")
    @classmethod
    def split_master_and_other(cls, values):
        if isinstance(values, list):
            values = {"configurations": values}
        if values.get("master"):
            return values
        configs = values.get("configurations")
        if not configs:
            raise ValueError("Configurations list cannot be empty")
        values["master"] = configs[0]
        values["others"] = configs[1:]
        values.pop("configurations", None)
        return values

    def model_custom_dump(self, **kwargs):
        return [self.master.model_dump(**kwargs)] + [other.model_dump(**kwargs) for other in self.others]


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
