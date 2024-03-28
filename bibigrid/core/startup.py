"""
Contains main method. Interprets command line, sets logging and starts corresponding action.
"""
import logging
import math
import os
import sys
import time
import traceback

import yaml

from bibigrid.core.actions import check, create, ide, list_clusters, terminate, update, version
from bibigrid.core.utility import command_line_interpreter
from bibigrid.core.utility.handler import configuration_handler, provider_handler

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
    if os.path.isfile(create.CLUSTER_MEMORY_PATH):
        try:
            with open(create.CLUSTER_MEMORY_PATH, mode="r", encoding="UTF-8") as cluster_memory_file:
                mem_dict = yaml.safe_load(stream=cluster_memory_file)
                return mem_dict.get("cluster_id")
        except yaml.YAMLError as exc:
            LOG.warning("Couldn't read configuration %s: %s", create.CLUSTER_MEMORY_PATH, exc)
    return None


def set_logger_verbosity(verbosity):
    """
    Sets verbosity, format and handler.
    :param verbosity: level of verbosity
    :return:
    """

    capped_verbosity = min(verbosity, len(VERBOSITY_LIST) - 1)
    # LOG.basicConfig(format=LOGGER_FORMAT, level=VERBOSITY_LIST[capped_verbosity],
    #                    handlers=LOGGING_HANDLER_LIST)
    LOG.setLevel(VERBOSITY_LIST[capped_verbosity])

    LOG.debug(f"Logging verbosity set to {capped_verbosity}")


# pylint: disable=too-many-nested-blocks,too-many-branches, too-many-statements
def run_action(args, configurations, config_path):
    """
    Uses args to decide which action will be executed and executes said action.
    :param args: command line arguments
    :param configurations: list of configurations (dicts)
    :param config_path: path to configurations-file
    :return:
    """
    if args.version:
        LOG.info("Action version selected")
        version.version(LOG)
        return 0

    start_time = time.time()
    exit_state = 0
    try:
        providers = provider_handler.get_providers(configurations, LOG)
        if providers:
            if args.list:
                LOG.info("Action list selected")
                exit_state = list_clusters.log_list(args.cluster_id, providers, LOG)
            elif args.check:
                LOG.info("Action check selected")
                exit_state = check.check(configurations, providers, LOG)
            elif args.create:
                LOG.info("Action create selected")
                creator = create.Create(providers=providers,
                                        configurations=configurations,
                                        log=LOG,
                                        debug=args.debug,
                                        config_path=config_path)
                LOG.log(42, "Creating a new cluster takes about 10 or more minutes depending on your cloud provider "
                           "and your configuration. Please be patient.")
                exit_state = creator.create()
            else:
                if not args.cluster_id:
                    args.cluster_id = get_cluster_id_from_mem()
                    LOG.info("No cid (cluster_id) specified. Defaulting to last created cluster: %s",
                             args.cluster_id or 'None found')
                if args.cluster_id:
                    if args.terminate:
                        LOG.info("Action terminate selected")
                        exit_state = terminate.terminate(cluster_id=args.cluster_id,
                                                         providers=providers,
                                                         log=LOG,
                                                         debug=args.debug)
                    elif args.ide:
                        LOG.info("Action ide selected")
                        exit_state = ide.ide(args.cluster_id, providers[0], configurations[0], LOG)
                    elif args.update:
                        LOG.info("Action update selected")
                        exit_state = update.update(args.cluster_id, providers[0], configurations[0], LOG)
            for provider in providers:
                provider.close()
        else:
            exit_state = 1
    except Exception as err:  # pylint: disable=broad-except
        if args.debug:
            exc_type, exc_value, exc_traceback = sys.exc_info()
            LOG.error("".join(traceback.format_exception(exc_type, exc_value, exc_traceback)))
        else:
            LOG.error(err)
        exit_state = 2
    time_in_s = time.time() - start_time
    LOG.log(42, f"--- {math.floor(time_in_s / 60)} minutes and {round(time_in_s % 60, 2)} seconds ---")
    return exit_state


def main():
    """
    Interprets command line, sets logger, reads configuration and runs selected action. Then exits.
    :return:
    """
    logging.basicConfig(format=LOGGER_FORMAT)
    # LOG.addHandler(logging.StreamHandler())  # stdout
    LOG.addHandler(logging.FileHandler("bibigrid.log"))  # file
    args = command_line_interpreter.interpret_command_line()
    set_logger_verbosity(args.verbose)
    configurations = configuration_handler.read_configuration(LOG, args.config_input)
    if configurations:
        sys.exit(run_action(args, configurations, args.config_input))
    sys.exit(1)


if __name__ == "__main__":
    main()
