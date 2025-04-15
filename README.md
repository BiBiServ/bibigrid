# BiBiGrid

BiBiGrid is a framework for creating and managing cloud clusters, currently supporting OpenStack. 
Future versions will support additional cloud providers.

BiBiGrid uses Ansible to configure standard Ubuntu 22.04/24.04 LTS cloud images. Depending on your configuration BiBiGrid
can set up an HCP cluster for grid computing (Slurm Workload Manager,
a shared filesystem (on local discs and attached volumes),
a cloud IDE for writing, running and debugging (Theia Web IDE) and many more.

> **Note**
> The latest version is currently work in progress. Future changes are likely.
> Not all features of the previous version are available, but they will come soon.
> The [previous version](https://github.com/BiBiServ/bibigrid/tree/bibigrid-2.3.1) is still available, 
> but not maintained anymore.

## Getting Started

For most users the [Hands-On BiBiGrid Tutorial](https://github.com/deNBI/bibigrid_clum2022) 
is the best entry point.

However, if you are already quite experienced with *OpenStack* and the previous *BiBiGrid* the following brief explanation
might be just what you need.

<details>
<summary> Brief, technical BiBiGrid overview </summary>

### How to configure a cluster?

#### Configuration File: bibigrid.yaml

A [template](bibigrid.yaml) file is included in the repository ([bibigrid.yaml](bibigrid.yaml)). 


The cluster configuration file, `bibigrid.yaml`, consists of a list of configurations. 
Each configuration describes provider-specific settings. 
The first configuration in the list also contains keys that apply to the entire cluster (e.g., roles).

The configuration template [bibigrid.yaml](bibigrid.yaml) contains many helpful comments, making completing it easier for you.

[You need more details?](documentation/markdown/features/configuration.md)

#### Cloud Specification Data: clouds.yaml

To access the cloud, authentication information is required.
You can download your `clouds.yaml` from OpenStack.

Place the `clouds.yaml` file in the `~/.config/bibigrid/` directory. BiBiGrid will load this file during execution.

[You need more details?](documentation/markdown/features/cloud_specification_data.md)

### Quick First Time Usage

If you haven't used BiBiGrid1 in the past or are unfamiliar with OpenStack, we heavily recommend following the 
[tutorial](https://github.com/deNBI/bibigrid_clum2022) instead.

#### Preparation

1. Download (or create) your `clouds.yaml` file (and optionally `clouds-public.yaml`) as described [above](#cloud-specification-data-cloudsyaml). 
2. Place the `clouds.yaml` into `~/.config/bibigrid`
3. Fill in the `bibigrid.yaml` configuration file with your specifics. At a minimum you need to specify: a master instance with valid type and image, 
an sshUser (most likely ubuntu) and a subnet. 
You will likely also want to specify at least one worker instance with a valid type, image, and count.
4. If your cloud provider runs post-launch services, you need to set the `waitForServices` 
key appropriately which expects a list of services to wait for.
5. Create a virtual environment from `bibigrid/requirements.txt`. 
See [here](https://www.akamai.com/blog/developers/how-building-virtual-python-environment) for more detailed info. 
6. Take a look at [First execution](#first-execution)

#### First execution

Before proceeding, ensure you have completed the steps described in the [Preparation section](#preparation).

After cloning the repository, navigate to the bibigrid directory. 
Source the virtual environment created during [preparation](#preparation) to execute BiBiGrid.
Refer to BiBiGrid's [Command Line Interface documentation](documentation/markdown/features/CLI.md) if you want to explore additional options.

A first execution run through could be:

1. `./bibigrid.sh -i [path-to-bibigrid.yaml] -ch`: checks the configuration
2. `./bibigrid.sh -i 'bibigrid.yaml -i [path-to-bibigrid.yaml] -c'`: creates the cluster (execute only if check was successful)
3. Use **BiBiGrid's create output** to investigate the created cluster further. Especially connecting to the ide might be helpful. 
Otherwise, connect using ssh.
4. While in ssh try `sinfo` to printing node info
5. Run `srun -x $(hostname) hostname` to power up a worker and get its hostname.
6. Run `sinfo` again to see the node powering up. After a while it will be terminated again.
7. Use the terminate command from **BiBiGrid's create output** to shut down the cluster again. 
All floating-ips used will be released.

Great! You've just started and terminated your first cluster using BiBiGrid!

</details>

### Troubleshooting

If your cluster doesn't start up, first ensure your configuration file is valid using the `-ch` option.
If the configuration is invalid, modify the file as needed.
Use the `-v` or `-vv` options for more verbose output to help identify the issue faster.
Also, double-check that you have sufficient permissions to access the project.
If you cannot make your configuration file valid, please contact a developer.
Additionally, manually check if your quotas are exceeded, as some quotas cannot currently be checked by BiBiGrid.

**Whenever you contact a developer, please send your logfile along.**

# Documentation

For more information about BiBiGrid, please visit the following links:

- [BiBiGrid Configuration](documentation/markdown/features/configuration.md)
- [BiBiGrid Features](documentation/markdown/bibigrid_feature_list.md)
- [Software used by BiBiGrid](documentation/markdown/bibigrid_software_list.md)

<details>
<summary> Differences to old Java BiBiGrid</summary>

- BiBiGrid no longer uses RC- but cloud.yaml-files for cloud-specification data. Environment variables are no longer used (or supported).
See [Cloud Specification Data](documentation/markdown/features/cloud_specification_data.md).
- BiBiGrid has a largely reworked configurations file, because BiBiGrid core supports multiple providers this step was necessary.
See [Configuration](documentation/markdown/features/configuration.md)
- BiBiGrid currently only implements the provider OpenStack.
- BiBiGrid only starts the master and will dynamically start workers using slurm when they are needed. 
Workers are powered down once they are not used for a longer period.
- BiBiGrid lays the foundation for clusters that are spread over multiple providers, but Hybrid Clouds aren't fully implemented yet.
  
</details>

# Development

## Development-Guidelines

[https://github.com/BiBiServ/Development-Guidelines](https://github.com/BiBiServ/Development-Guidelines)

## On implementing concrete providers

Implementing new cloud providers is straightforward. 
Copy the `provider.py` file and implement all necessary methods for your cloud provider.
Inherit from the `provider` class.
Add your provider to the `providerHandler` lists and assign it an associated name for the configuration files.
This will automatically include your provider in BiBiGrid's tests and regular execution.
Test your provider to ensure all methods are implemented correctly.
