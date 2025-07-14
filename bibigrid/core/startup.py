"""
Contains main method. Interprets command line, sets logging and starts corresponding action.
"""
import logging
import math
import os
import sys
import time
import traceback

import click
import yaml

from bibigrid.core.actions import check, create, ide, list_clusters, terminate, update, version
from bibigrid.core.utility.handler import configuration_handler, provider_handler
from bibigrid.core.utility.paths.basic_path import CONFIG_FOLDER, CLUSTER_MEMORY_PATH, ENFORCED_CONFIG_PATH, \
    DEFAULT_CONFIG_PATH

FOLDER_START = ("~/", "/", "./")

VERBOSITY_LIST = [logging.WARNING, logging.INFO, logging.DEBUG]
LOGGER_FORMAT = "%(asctime)s [%(levelname)s] %(message)s"
LOG = logging.getLogger("bibigrid")
logging.addLevelName(42, "PRINT")


def get_cluster_id_from_mem():
    """
        Reads the cluster_id of the last created cluster and returns it. Used if no cluster_id is given.

    @return: cluster_id. If no mem file can be found, the file is not a valid yaml file or doesn't contain a cluster_id,
    it returns none.
    """
    if os.path.isfile(CLUSTER_MEMORY_PATH):
        try:
            with open(CLUSTER_MEMORY_PATH, mode="r", encoding="UTF-8") as cluster_memory_file:
                mem_dict = yaml.safe_load(stream=cluster_memory_file)
                return mem_dict.get("cluster_id")
        except yaml.YAMLError as exc:
            LOG.warning("Couldn't read configuration %s: %s", CLUSTER_MEMORY_PATH, exc)
    LOG.warning(f"Couldn't find cluster memory path {CLUSTER_MEMORY_PATH}")
    return None


def set_logger_verbosity(verbosity):
    """
    Sets verbosity, format and handler.
    @param verbosity: level of verbosity
    @return:
    """

    capped_verbosity = min(verbosity, len(VERBOSITY_LIST) - 1)
    # LOG.basicConfig(format=LOGGER_FORMAT, level=VERBOSITY_LIST[capped_verbosity],
    #                    handlers=LOGGING_HANDLER_LIST)
    LOG.setLevel(VERBOSITY_LIST[capped_verbosity])

    LOG.debug(f"Logging verbosity set to {capped_verbosity}")


def check_cid(cid):
    if "-" in cid:
        new_cid = cid.split("-")[-1]
        LOG.info("-cid %s is not a cid, but probably the entire master name. Using '%s' as "
                 "cid instead.", cid, new_cid)
        return new_cid
    if "." in cid:
        LOG.info("-cid %s is not a cid, but probably the master's ip. "
                 "Using the master ip instead of cid only works if a cluster key is in your systems default ssh key "
                 "location (~/.ssh/). Otherwise bibigrid can't identify the cluster key.")
    return cid


def expand_path(path):
    return os.path.expanduser(path) if path.startswith(FOLDER_START) else os.path.join(CONFIG_FOLDER, path)


def run_action(action, configurations, config_input, cluster_id, debug):
    """
    Executes passed action.
    :param action: action to execute
    :param configurations: list of configurations (dicts)
    :param config_input: path to configurations-file
    :param cluster_id: some actions need a cluster_id to be executed
    :param debug: mostly whether the cluster should be kept alive on failure
    :return:
    """
    start_time = time.time()
    exit_state = 0

    try:
        providers = provider_handler.get_providers(configurations, LOG)
        if not providers:
            exit_state = 1

        if providers:
            match action:
                case 'list':
                    LOG.info("Action list selected")
                    exit_state = list_clusters.log_list(cluster_id, providers, LOG)
                case 'check':
                    LOG.info("Action check selected")
                    exit_state = check.check(configurations, providers, LOG)
                case 'create':
                    LOG.info("Action create selected")
                    creator = create.Create(providers=providers, configurations=configurations, log=LOG, debug=debug,
                                            config_path=config_input)
                    LOG.log(42,
                            "Creating a new cluster takes about 10 or more minutes depending on your cloud provider and your configuration. Please be patient.")
                    exit_state = creator.create()
                case _:
                    if not cluster_id:
                        cluster_id = get_cluster_id_from_mem()
                        LOG.info("No cid (cluster_id) specified. Defaulting to last created cluster: %s",
                                 cluster_id or 'None found')

                    if cluster_id:
                        match action:
                            case 'terminate':
                                LOG.info("Action terminate selected")
                                exit_state = terminate.terminate(cluster_id=cluster_id, providers=providers, log=LOG,
                                                                 debug=debug)
                            case 'ide':
                                LOG.info("Action ide selected")
                                exit_state = ide.ide(cluster_id, providers[0], configurations[0], LOG)
                            case 'update':
                                LOG.info("Action update selected")
                                creator = create.Create(providers=providers, configurations=configurations, log=LOG,
                                                        debug=debug, config_path=config_input, cluster_id=cluster_id)
                                exit_state = update.update(creator, LOG)

            for provider in providers:
                provider.close()

    except Exception as err:
        exc_type, exc_value, exc_traceback = sys.exc_info()
        LOG.error("".join(traceback.format_exception(exc_type, exc_value, exc_traceback)))
        exit_state = 2

    time_in_s = time.time() - start_time
    LOG.log(42, f"--- {math.floor(time_in_s / 60)} minutes and {round(time_in_s % 60, 2)} seconds ---")
    return exit_state


@click.command()
@click.version_option(version=version.__version__, prog_name=version.PROG_NAME, message=version.MESSAGE)
@click.option("-v", "--verbose", count=True, help="Increases logging verbosity.")
@click.option("-d", "--debug", is_flag=True,
              help="Keeps cluster active even when crashing. Asks before shutdown. Offers termination after successful create.")
@click.option("-i", "--config_input", type=click.Path(), required=True, help="Path to YAML configurations file.")
@click.option("-di", "--default_config_input", type=click.Path(), default=DEFAULT_CONFIG_PATH,
              help="Path to default YAML configurations file.")
@click.option("-ei", "--enforced_config_input", type=click.Path(), default=ENFORCED_CONFIG_PATH,
              help="Path to enforced YAML configurations file.")
@click.option("-cid", "--cluster_id", help="Cluster id is needed for certain commands.")
@click.argument('action',
                type=click.Choice(['create', 'terminate', 'list', 'check', 'ide', 'update'], case_sensitive=False))
def cli(verbose, debug, config_input, default_config_input, enforced_config_input, cluster_id, action):
    """Interprets command line for BiBiGrid."""
    logging.basicConfig(format=LOGGER_FORMAT)
    LOG.addHandler(logging.FileHandler("bibigrid.log"))

    set_logger_verbosity(verbose)
    config_input = expand_path(config_input)
    default_config_input = expand_path(default_config_input)
    enforced_config_input = expand_path(enforced_config_input)

    if cluster_id:
        cluster_id = check_cid(cluster_id)

    configurations = configuration_handler.read_configuration(LOG, config_input)

    if not configurations:
        sys.exit(1)

    configurations = configuration_handler.merge_configurations(
        user_config=configurations,
        default_config_path=default_config_input,
        enforced_config_path=enforced_config_input,
        log=LOG
    )

    sys.exit(run_action(action, configurations, config_input, cluster_id, debug))


if __name__ == '__main__':
    cli()
