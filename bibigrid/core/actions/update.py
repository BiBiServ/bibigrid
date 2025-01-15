"""
Module that contains methods to update the master playbook
"""

from bibigrid.core.actions.list_clusters import dict_clusters
from bibigrid.core.utility.handler import cluster_ssh_handler


def update(creator, log):
    log.info(f"Starting update for cluster {creator.cluster_id}...")
    master_ip, ssh_user, used_private_key = cluster_ssh_handler.get_ssh_connection_info(creator.cluster_id,
                                                                                        creator.providers[0],
                                                                                        creator.configurations[0], log)
    log.info(f"Trying to update {master_ip}@{ssh_user} with key {used_private_key}")
    cluster_dict = dict_clusters(creator.providers, log)
    if cluster_dict[creator.cluster_id]["workers"]:
        workers = [worker['name'] for worker in cluster_dict[creator.cluster_id]["workers"]]
        log.warning(f"There are still workers up! {workers}")
        return 1
    if master_ip and ssh_user and used_private_key:
        master = creator.MASTER_IDENTIFIER(cluster_id=creator.cluster_id)
        server = creator.providers[0].get_server(master)
        creator.master_ip = master_ip
        creator.configurations[0]["private_v4"] = server["private_v4"]
        creator.configurations[0]["floating_ip"] = master_ip
        # TODO Test Volumes
        creator.configurations[0]["volumes"] = server["volumes"]
        creator.prepare_configurations()
        log.log(42, f"Uploading data and executing BiBiGrid's Ansible playbook to {creator.cluster_id}")
        creator.upload_data(used_private_key, clean_playbook=True)
        log.log(42, f"Successfully updated cluster {creator.cluster_id}")
        return 0
    log.warning("One or more among master_ip, ssh_user and used_private_key are none. Aborting...")
    return 1
