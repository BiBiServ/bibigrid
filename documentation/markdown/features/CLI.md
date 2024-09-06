# CLI

Available command line parameters:

- `-h, --help`            show help message and exit
- `-v, --verbose`         Increases output verbosity (can be of great use when cluster fails to start). `-v` adds more
  detailed info to the logfile, `-vv` adds debug information to the logfile.
- `-d, --debug`           Keeps cluster active in case of an error. Offers termination after successful create. Prints full stack trace on errors.
- `-i <path>, --config_input <path> (required)` Path to YAML configurations file. Relative paths can be used and start
  at `~/.config/bibigrid`
- `-cid <cluster-id>, --cluster_id <cluster-id>` Cluster id is needed for ide and termination. If no cluster id is set,
  the last started cluster's id will be used (except for `list_clusters`).

## Mutually exclusive actions: choose exactly one

- `-V, --version`         Displays version.
- `-t, --terminate_cluster` Terminates cluster. Needs cluster-id set.
- `-c, --create`          Creates cluster.
- `-l, --list_clusters`   Lists all running clusters. If cluster-id is
  set, will list this cluster in detail only.
- `-ch, --check`          Validates cluster configuration.
- `-ide, --ide`           Establishes a secured connection to ide.
  Needs cluster-id set.
- `-u, --update`        Updates master's playbook. Needs cluster-id set, no job running and no workers powered up.