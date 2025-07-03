"""
Contains main method. Interprets command line, sets logging and starts corresponding action.
"""
import click
import logging
import math
import os
import sys
import time
import traceback
from types import SimpleNamespace
import yaml

from bibigrid.core.actions import check, create, ide, list_clusters, terminate, update, version
from bibigrid.core.utility import command_line_interpreter
from bibigrid.core.utility.handler import configuration_handler, provider_handler
from bibigrid.core.utility.paths.basic_path import CLUSTER_MEMORY_PATH

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


# pylint: disable=too-many-nested-blocks,too-many-branches, too-many-statements
def run_action(args, configurations, config_path):
    """
    Uses args to decide which action will be executed and executes said action.
    @param args: command line arguments
    @param configurations: list of configurations (dicts)
    @param config_path: path to configurations-file
    @return:
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
                creator = create.Create(providers=providers, configurations=configurations, log=LOG, debug=args.debug,
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
                        exit_state = terminate.terminate(cluster_id=args.cluster_id, providers=providers, log=LOG,
                                                         debug=args.debug)
                    elif args.ide:
                        LOG.info("Action ide selected")
                        exit_state = ide.ide(args.cluster_id, providers[0], configurations[0], LOG)
                    elif args.update:
                        LOG.info("Action update selected")
                        creator = create.Create(providers=providers, configurations=configurations, log=LOG,
                                                debug=args.debug,
                                                config_path=config_path, cluster_id=args.cluster_id)
                        exit_state = update.update(creator, LOG)
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


# def main():
#     """
#     Interprets command line, sets logger, reads configuration and runs selected action. Then exits.
#     @return:
#     """
#     logging.basicConfig(format=LOGGER_FORMAT)
#     # LOG.addHandler(logging.StreamHandler())  # stdout
#     LOG.addHandler(logging.FileHandler("bibigrid.log"))  # file
#     args = command_line_interpreter.interpret_command_line()
#     set_logger_verbosity(args.verbose)

#     configurations = configuration_handler.read_configuration(LOG, args.config_input)
#     if not configurations:
#         sys.exit(1)
#     configurations = configuration_handler.merge_configurations(
#         user_config=configurations,
#         default_config_path=args.default_config_input,
#         enforced_config_path=args.enforced_config_input,
#         log=LOG
#     )
#     if configurations:
#         sys.exit(run_action(args, configurations, args.config_input))


# if __name__ == "__main__":
#     main()

@click.command()
@click.option('-h', '--help', is_flag=True, help='Show this message and exit.')
@click.option('-v', '--verbose', is_flag=True, help='Enable verbose output.')
@click.option('-d', '--debug', is_flag=True, help='Enable debug mode.')
@click.option('-i', '--config-input', type=click.Path(), help='Path to user configuration.')
@click.option('-di', '--default-config-input', type=click.Path(), help='Path to default configuration.')
@click.option('-ei', '--enforced-config-input', type=click.Path(), help='Path to enforced configuration.')
@click.option('-cid', '--cluster-id', help='Cluster ID.')
@click.option('-V', '--version', is_flag=True, help='Show version and exit.')
@click.option('-t', '--terminate', is_flag=True, help='Terminate cluster.')
@click.option('-c', '--create', is_flag=True, help='Create cluster.')
@click.option('-l', '--list', is_flag=True, help='List clusters.')
@click.option('-ch', '--check', is_flag=True, help='Check cluster.')
@click.option('-ide', '--ide', is_flag=True, help='IDE mode.')
@click.option('-u', '--update', is_flag=True, help='Update cluster.')
def main(help, verbose, debug, config_input, default_config_input, enforced_config_input, cluster_id,
         version, terminate, create, list, check, ide, update):
    """
    Interprets command line, sets logger, reads configuration and runs selected action. Then exits.
    """
    logging.basicConfig(format=LOGGER_FORMAT)
    LOG.addHandler(logging.FileHandler("bibigrid.log"))

    # Enforce that exactly one action is selected
    actions = [version, terminate, create, list, check, ide, update]
    if sum(actions) != 1:
        # Click's fail() prints usage and exits with error
        ctx = click.get_current_context()
        ctx.fail("One (and only one) of the arguments -V/--version -t/--terminate -c/--create -l/--list -ch/--check -ide/--ide -u/--update is required")

    set_logger_verbosity(verbose)

    # Replace with your actual configuration handler
    configurations = configuration_handler.read_configuration(LOG, config_input)
    if not configurations:
        sys.exit(1)

    if default_config_input is None:
        default_config_input = ""
    if enforced_config_input is None:
        enforced_config_input = ""
    configurations = configuration_handler.merge_configurations(
        user_config=configurations,
        default_config_path=default_config_input,
        enforced_config_path=enforced_config_input,
        log=LOG
    )

    args = SimpleNamespace(
        config_input=config_input,
        default_config_input=default_config_input,
        enforced_config_input=enforced_config_input,
        cluster_id=cluster_id,
        version=version,
        terminate=terminate,
        create=create,
        list=list,
        check=check,
        ide=ide,
        update=update,
        verbose=verbose,
        debug=debug
    )

    if configurations:
        # You may want to pass the action as a string instead of the entire args object
        sys.exit(run_action(args, configurations, config_input))

if __name__ == "__main__":
    main()
