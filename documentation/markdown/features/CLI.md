# CLI

BiBiGrid is either executed with `--version`, that displays BiBiGrid's version and additional information, 
`--help` to show BiBiGrid's help message or with an action argument.

```markdown
bibigrid.core.startup [OPTIONS] {create|terminate|list|check|ide|update}
```

## Action Argument
- `check` Validates cluster configuration.
- `create` Creates cluster.
- `terminate` Terminates cluster. Needs option cluster-id
- `list` Lists all running clusters. If option cluster-id is set, will list this cluster in detail only.
- `update` Updates master's playbook. Needs option cluster-id, no job running and no workers powered up.
- `ide` Establishes a secured connection to Theia ide.

## Options

- `-v, --verbose`         Increases output verbosity (can be of great use when cluster fails to start). `-v` adds more
  detailed info to the logfile, `-vv` adds debug information to the logfile.
- `-d, --debug`           Keeps cluster active in case of an error. Offers termination after successful create. Prints full stack trace on errors.
- `-i <path>, --config_input <path> (required)` Path to YAML configurations file. Relative paths can be used and start
  at `~/.config/bibigrid`
- `-cid <cluster-id>, --cluster_id <cluster-id>` Cluster id is needed for ide and termination. If no cluster id is set,
  the last started cluster's id will be used (except for `list_clusters`).