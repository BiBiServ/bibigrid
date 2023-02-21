"""
Concrete implementation of provider.py for openstack
"""

import logging

import keystoneclient
import openstack
from cinderclient import client
from keystoneauth1 import session
from keystoneauth1.exceptions.http import NotFound
from keystoneauth1.identity import v3

from bibigrid.core import provider
from bibigrid.core.actions import create
from bibigrid.core.actions import version
from bibigrid.models.exceptions import ExecutionException

LOG = logging.getLogger("bibigrid")


class OpenstackProvider(provider.Provider):  # pylint: disable=too-many-public-methods
    """
    Specific implementation of the Provider class for openstack
    """
    NAME = "OpenstackProvider"

    # to be read from clouds.yaml file.

    def __init__(self, cloud_specification):
        super().__init__(cloud_specification)
        self.conn = self.create_connection()
        sess = self.create_session()
        self.keystone_client = keystoneclient.client.Client(session=sess, interface='public')
        self.cinder = client.Client(3, session=sess)

    def create_session(self, app_name="openstack_scripts", app_version="1.0"):
        """
        Creates and returns a session that can be used to create a connection to different openstack services
        @param app_name:
        @param app_version:
        @return: session
        """
        # print(v3)
        auth = self.cloud_specification["auth"]
        if all(key in auth for key in ["auth_url", "application_credential_id", "application_credential_secret"]):
            auth_session = v3.ApplicationCredential(
                auth_url=auth["auth_url"],
                application_credential_id=auth["application_credential_id"],
                application_credential_secret=auth["application_credential_secret"]
            )
        elif all(key in auth for key in ["auth_url", "username", "password", "project_id", "user_domain_name"]):
            auth_session = v3.Password(auth_url=auth["auth_url"],
                                       username=auth["username"],
                                       password=auth["password"],
                                       project_id=auth["project_id"],
                                       user_domain_name=auth["user_domain_name"])
        else:
            raise KeyError("Not enough authentication information in clouds.yaml/clouds-public.yaml "
                           "to create a session. Use one:\n"
                           "Application Credentials: auth_url, application_credential_id and "
                           "application_credential_secret\n"
                           "Password: auth_url, username, password, project_id and user_domain_name")
        return session.Session(auth=auth_session,
                               app_name=app_name, app_version=app_version)

    def create_connection(self, app_name="openstack_bibigrid", app_version=version.__version__):
        auth = self.cloud_specification["auth"]
        return openstack.connect(
            load_yaml_config=False,
            load_envvars=False,
            auth_url=auth["auth_url"],
            project_name=auth.get("project_name"),
            username=auth.get("username"),
            password=auth.get("password"),
            region_name=self.cloud_specification["region_name"],
            user_domain_name=auth.get("user_domain_name"),
            project_domain_name=auth.get("user_domain_name"),
            app_name=app_name,
            app_version=app_version,
            application_credential_id=auth.get("application_credential_id"),
            application_credential_secret=auth.get("application_credential_secret"),
            interface=self.cloud_specification.get("interface"),
            identity_api_version=self.cloud_specification.get("identity_api_version"),
            auth_type=self.cloud_specification.get("auth_type")
        )

    def create_application_credential(self, name=None):
        return self.keystone_client.application_credentials.create(name=name).to_dict()

    def delete_application_credential_by_id_or_name(self, ac_id_or_name):
        """
        Deletes existing application credential by id or name and returns true.
        If application credential not found it returns false.
        :param ac_id_or_name: application credential id or name
        :return: True if deleted else false
        """
        try:
            self.keystone_client.application_credentials.delete(ac_id_or_name)  # id
            return True
        except NotFound:
            try:
                self.keystone_client.application_credentials.delete(
                    self.keystone_client.application_credentials.find(name=ac_id_or_name))  # name
                return True
            except NotFound:
                return False

    def get_image_by_id_or_name(self, image_id_or_name):
        return self.conn.get_image(name_or_id=image_id_or_name)

    def get_flavor(self, instance_type):
        return self.conn.get_flavor(instance_type)

    def get_volume_snapshot_by_id_or_name(self, snapshot_id_or_name):
        return self.conn.get_volume_snapshot(name_or_id=snapshot_id_or_name)

    def get_network_by_id_or_name(self, network_id_or_name):
        return self.conn.get_network(name_or_id=network_id_or_name)

    def get_subnet_by_id_or_name(self, subnet_id_or_name):
        return self.conn.get_subnet(name_or_id=subnet_id_or_name)

    def list_servers(self):
        return [elem.toDict() for elem in self.conn.list_servers()]

    def create_server(self, name, flavor, image,
                      network, key_name=None, wait=True, volumes=None):
        try:
            server = self.conn.create_server(name=name, flavor=flavor, image=image,
                                             network=network, key_name=key_name, volumes=volumes)
        except openstack.exceptions.BadRequestException as exc:
            raise ConnectionError() from exc
        except openstack.exceptions.SDKException as exc:
            raise ExecutionException() from exc
        except AttributeError as exc:
            raise ExecutionException("Unable to create server due to faulty configuration.") from exc
        if wait:
            self.conn.wait_for_server(server=server, auto_ip=False, timeout=600)
            server = self.conn.get_server(server["id"])
        return server

    def delete_server(self, name_or_id, delete_ips=True):
        """
        Deletes server. floating_ip as well if delete_ips is true. The resources are then free again
        :param name_or_id:
        :param delete_ips:
        :return:
        """
        return self.conn.delete_server(name_or_id=name_or_id, wait=False,
                                       timeout=180, delete_ips=delete_ips,
                                       delete_ip_retry=1)

    def delete_keypair(self, key_name):
        return self.conn.delete_keypair(key_name)

    def get_server_group_by_id_or_name(self, server_group_id_or_name):
        return self.conn.get_server_group(name_or_id=server_group_id_or_name)

    def close(self):
        return self.conn.close()

    def create_keypair(self, name, public_key):
        return self.conn.create_keypair(name=name, public_key=public_key)

    def get_network_id_by_subnet(self, subnet):
        subnet = self.conn.get_subnet(subnet)
        return subnet["network_id"] if subnet else subnet

    def get_subnet_ids_by_network(self, network):
        network = self.conn.get_network(network)
        return network["subnets"] if network else network

    def get_free_resources(self):
        """
        Uses the cinder API to get all relevant volume resources.
        https://github.com/openstack/python-cinderclient/blob/master/cinderclient/v3/limits.py
        Uses the nova API to get all relevant compute resources. Floating-IP is not returned correctly by openstack.
        :return: Dictionary containing the free resources
        """
        compute_limits = dict(self.conn.compute.get_limits()["absolute"])
        # maybe needs limits.get(os.environ["OS_PROJECT_NAME"]) in the future
        volume_limits_generator = self.cinder.limits.get().absolute
        volume_limits = {absolut_limit.name: absolut_limit.value for absolut_limit in
                         volume_limits_generator}
        # ToDo TotalVolumeGigabytes needs totalVolumeGigabytesUsed, but is not given
        volume_limits["totalVolumeGigabytesUsed"] = 0
        free_resources = {}
        for key in ["total_cores", "floating_ips", "instances", "total_ram"]:
            free_resources[key] = compute_limits[key] - compute_limits[key + "_used"]
        for key in ["Volumes", "VolumeGigabytes", "Snapshots", "Backups", "BackupGigabytes"]:
            free_resources[key] = volume_limits["maxTotal" + key] - volume_limits[
                "total" + key + "Used"]
        return free_resources

    def get_volume_by_id_or_name(self, name_or_id):
        return self.conn.get_volume(name_or_id)

    def create_volume_from_snapshot(self, snapshot_name_or_id):
        """
        Uses the cinder API to create a volume from snapshot:
        https://github.com/openstack/python-cinderclient/blob/master/cinderclient/v3/volumes.py
        :param snapshot_name_or_id: name or id of snapshot
        :return: id of created volume
        """
        LOG.debug("Trying to create volume from snapshot")
        snapshot = self.conn.get_volume_snapshot(snapshot_name_or_id)
        if snapshot:
            LOG.debug(f"Snapshot {snapshot_name_or_id} found.")
            if snapshot["status"] == "available":
                LOG.debug("Snapshot %s is available.", {snapshot_name_or_id})
                size = snapshot["size"]
                name = create.PREFIX_WITH_SEP + snapshot["name"]
                description = f"Created from snapshot {snapshot_name_or_id} by BiBiGrid"
                volume = self.cinder.volumes.create(size=size, snapshot_id=snapshot["id"], name=name,
                                                    description=description)
                return volume.to_dict()["id"]
            LOG.warning("Snapshot %s is %s; must be available.", snapshot_name_or_id, snapshot['status'])
        else:
            LOG.warning("Snapshot %s not found.", snapshot_name_or_id)
        return None

    def get_external_network(self, network_name_or_id):
        """
        Finds router interface with network id equal to given network and by that the external network.
        :param network_name_or_id:Name or id of network
        :return:Corresponding external network
        """
        network_id = self.conn.get_network(network_name_or_id)["id"]
        for router in self.conn.list_routers():
            for interface in self.conn.list_router_interfaces(router):
                if interface.network_id == network_id:
                    return router.external_gateway_info["network_id"]
        return None

    def attach_available_floating_ip(self, network=None, server=None):
        """
        Get a floating IP from a network or a pool and attach it to the server
        :param network:
        :param server:
        :return:
        """
        floating_ip = self.conn.available_floating_ip(network=network)
        if server:
            self.conn.compute.add_floating_ip_to_server(server, floating_ip["floating_ip_address"])
        return floating_ip

    def get_images(self):
        """
        Get a generator able ot generate all images
        @return: A generator able ot generate all images
        """
        return self.conn.compute.images()

    def get_flavors(self):
        """
        Get a generator able ot generate all flavors
        @return: A generator able ot generate all flavors
        """
        return self.conn.compute.flavors()
