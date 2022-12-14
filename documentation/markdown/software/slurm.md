# Slurm
Be aware that due to BiBiGrid's slurm configuration the default behavior of commands might differ slightly from slurm's defaults.
Everything described below explains how slurm will behave in BiBiGrid's context.

## Slurm Client
### Useful commands
For more options see [slurm client's manpage](https://manpages.debian.org/testing/slurm-client/slurm-wlm.1).

|        Summary         |                                   Command                                    |                                                                            Explanation & Comment                                                                            |
|:----------------------:|:----------------------------------------------------------------------------:|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|
| List all present nodes |                                   `sinfo`                                    | Cloud nodes that are powered down are marked`~`. Knowing [Node State Codes](https://manpages.debian.org/testing/slurm-client/sinfo.1.en.html#NODE_STATE_CODES) helps a lot. |
|  Shutdown an instance  | `sudo scontrol update NodeName=[node-name] state=POWER_DOWN reason=[reason]` |                                                             Powers down the node. The instance will be deleted.                                                             |
|  Powerup an instance   |  `sudo scontrol update NodeName=[node-name] state=POWER_UP reason=[reason]`  |                                                              Powers up the node. An instance will be created.                                                               |
| Lists all running jobs |                                   `squeue`                                   |                                                           Allows you to see whether everything runs as expected.                                                            |

### Read more

|                                      Summary                                      |                 Explanation                  |
|:---------------------------------------------------------------------------------:|:--------------------------------------------:|
| [NODE STATE CODES](https://slurm.schedmd.com/sinfo.html#SECTION_NODE-STATE-CODES) | Very helpful to interpret `sinfo` correctly. |

