"""
Module that contains methods to update an already running cluster.
"""
import traceback

import paramiko

from bibigrid.core.actions.list_clusters import dict_clusters
from bibigrid.core.actions.terminate import terminate_server, write_cluster_state
from bibigrid.core.utility.handler import cluster_ssh_handler, ssh_handler
from bibigrid.core.utility.statics.create_statics import MASTER_IDENTIFIER, WORKER_IDENTIFIER
from bibigrid.models import exceptions
from bibigrid.models.exceptions import ConfigurationException, ExecutionException


def compute_worker_diff(creator, cluster):
    """
    Compares configured worker slots against actually running workers.
    Only a tail-safe append (growing an existing group's count, or adding a new group) is allowed - anything
    else raises ConfigurationException, since worker indices/names are recomputed from scratch every run and
    a reorder/shrink/onDemand-flip would silently point at the wrong already-running instance.
    @param creator: Create instance holding this cluster's configurations/providers/cluster_id
    @param cluster: this cluster_id's entry from list_clusters.dict_clusters (has a "workers" list)
    @return: list of (worker, index, configuration, provider) tuples to start, in index order
    """
    slots = {}  # index -> (worker, onDemand, configuration, provider)
    index = 0
    for configuration, provider in zip(creator.configurations, creator.providers):
        for worker in configuration.get("workerInstances", []):
            for _ in range(int(worker.get("count", 1))):
                slots[index] = (worker, worker.get("onDemand", True), configuration, provider)
                index += 1

    desired_names_by_index = {i: WORKER_IDENTIFIER(cluster_id=creator.cluster_id, additional=i) for i in slots}
    desired_names = set(desired_names_by_index.values())
    actual_names = {worker["name"] for worker in cluster.get("workers", [])}

    unmatched = actual_names - desired_names
    if unmatched:
        raise ConfigurationException(
            f"Existing worker(s) {sorted(unmatched)} no longer match this configuration's expected worker "
            "names. Reordering, inserting, or shrinking workerInstances groups on a running cluster isn't "
            "supported - only appending to an existing group's count is.")

    to_create = []
    for i, name in desired_names_by_index.items():
        worker, on_demand, configuration, provider = slots[i]
        if name in actual_names:
            if on_demand:
                raise ConfigurationException(
                    f"Worker {name} is already running but its group is now marked onDemand in the "
                    "configuration. Changing onDemand for an already-running worker isn't supported.")
            continue
        if not on_demand:
            to_create.append((worker, i, configuration, provider))
    return to_create


def update(creator, log):  # pylint: disable=too-many-branches
    """
    Updates an already running cluster with this cluster_id and starts any newly configured static
    (onDemand: False) workers (see compute_worker_diff). On failure, only rolls back the workers this run
    itself created, never the whole cluster.
    @param creator: Create instance holding this cluster's configurations/providers/cluster_id
    @param log:
    @return: exit_state
    """
    log.log(42, f"Starting update for cluster {creator.cluster_id}...")
    cluster_dict = dict_clusters(creator.providers, log)
    cluster = cluster_dict.get(creator.cluster_id)
    if not cluster or not cluster.get("master"):
        log.warning(f"Cluster {creator.cluster_id} not found. Aborting.")
        return 1

    new_worker_names = []
    try:
        if len(creator.providers) > 1:
            raise ConfigurationException(
                "Updating a multi-provider (multi-cloud/vpngtw) cluster isn't supported yet.")
        master_ip, ssh_user, used_private_key = cluster_ssh_handler.get_ssh_connection_info(
            creator.cluster_id, creator.providers[0], creator.configurations[0], log)
        if not (master_ip and ssh_user and used_private_key):
            raise ConfigurationException(
                "Unable to determine master_ip, ssh_user or private key of the running cluster. Aborting.")
        server = creator.providers[0].get_server(MASTER_IDENTIFIER(cluster_id=creator.cluster_id))
        if not server:
            raise ConfigurationException(f"Master of cluster {creator.cluster_id} not found. Aborting.")
        creator.master_ip = master_ip
        creator.configurations[0]["private_v4"] = server["private_v4"]
        creator.configurations[0]["floating_ip"] = master_ip
        creator.configurations[0]["volumes"] = server["volumes"]

        # reuse the master's existing munge key - otherwise ansible_configurator's default would generate a
        # fresh one and rotate munge out from under already-running nodes on every update
        ssh_data = {"floating_ip": master_ip, "private_key": used_private_key, "username": ssh_user,
                   "gateway": creator.configurations[0].get("gateway", {}), "timeout": creator.ssh_timeout,
                   "sock5_proxy": creator.configurations[0].get("sock5_proxy")}
        existing_munge_key = ssh_handler.read_remote_file(ssh_data, "/etc/munge/munge.key", log)
        if existing_munge_key:
            creator.configurations[0].setdefault("slurmConf", {})["munge_key"] = existing_munge_key
        else:
            raise ConfigurationException(
                "Unable to read the running cluster's existing munge key from the master. Aborting rather "
                "than risk deploying a new, mismatched one to the cluster.")

        creator.prepare_configurations()
        creator.create_defaults()
        # don't call creator.generate_security_groups(): the group already exists and that would duplicate it
        configuration = creator.configurations[0]
        if not configuration.get("securityGroups"):
            configuration["securityGroups"] = [creator.default_security_group_name]
        else:
            configuration["securityGroups"] = [creator.default_security_group_name] + configuration["securityGroups"]

        to_create = compute_worker_diff(creator, cluster)
        for worker, index, configuration, provider in to_create:
            creator.start_worker(worker, index, configuration, provider)
            new_worker_names.append(WORKER_IDENTIFIER(cluster_id=creator.cluster_id, additional=index))

        creator.upload_data(used_private_key, clean_playbook=True)
        if new_worker_names:
            message = f"Successfully updated cluster {creator.cluster_id}. Added workers: {new_worker_names}"
        else:
            message = f"Successfully updated cluster {creator.cluster_id}."
        log.log(42, message)
        write_cluster_state({"cluster_id": creator.cluster_id, "ssh_user": creator.ssh_user,
                             "floating_ip": creator.master_ip, "state": "running", "message": message})
    except exceptions.ConnectionException:
        log.error(traceback.format_exc())
        log.error("Connection couldn't be established. Check Provider connection.")
    except paramiko.ssh_exception.NoValidConnectionsError:
        log.error(traceback.format_exc())
        log.error("SSH connection couldn't be established. Check keypair.")
    except KeyError as exc:
        log.error(traceback.format_exc())
        log.error(f"Tried to access dictionary key {str(exc)}, but couldn't. Please check your configurations.")
    except FileNotFoundError as exc:
        log.error(traceback.format_exc())
        log.error(f"Tried to access resource files but couldn't. No such file or directory: {str(exc)}")
    except TimeoutError as exc:
        log.error(traceback.format_exc())
        log.error(f"Timeout while connecting to master: {str(exc)}")
    except ExecutionException as exc:
        log.error(traceback.format_exc())
        log.error(f"Execution of cmd on remote host fails: {str(exc)}")
    except ConfigurationException as exc:
        log.error(traceback.format_exc())
        log.error(f"Configuration invalid: {str(exc)}")
    except Exception as exc:  # pylint: disable=broad-except
        log.error(traceback.format_exc())
        log.error(f"Unexpected error: '{str(exc)}' ({type(exc)}) Contact a developer!)")
    else:
        return 0
    for name in new_worker_names:
        server = creator.providers[0].get_server(name)
        if server:
            terminate_server(creator.providers[0], server, log)
    write_cluster_state({"cluster_id": creator.cluster_id, "ssh_user": creator.ssh_user,
                         "floating_ip": creator.master_ip, "state": "failed",
                         "message": "Cluster update failed. Existing infrastructure left untouched."})
    return 1
