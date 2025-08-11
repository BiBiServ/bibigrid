# SOCKS5

In order to use BiBiGrid via a jump host one has to set up a SOCKS5 proxy; if you haven't already.

Many cloud locations have their own tutorials for the SOCKS5 proxy setup:
- [Berlin](https://cloud.denbi.de/wiki/Compute_Center/Berlin/#setting-up-a-socks-proxy)
- [Heidelberg DKFZ](https://cloud.denbi.de/wiki/Compute_Center/Heidelberg-DKFZ/#setting-up-a-socks-proxy)
- [Heidelberg Uni](https://cloud.denbi.de/wiki/Compute_Center/Heidelberg/#setting-up-a-socks-proxy)

## Setting Up SOCKS5

This tutorial differs because it saves the SOCKS5 proxy config in a separate file that is then included.

### Include denbi_ssh_conifg
Add the following line to your local `~/.ssh/config`:

```
Include ~/.ssh/denbi_ssh_config
```


### Write denbi_ssh_config
Create the file `~/.ssh/denbi_ssh_config` and fill the template below

```
# Access to the de.NBI jumphost
Host {{ jumphost_name }} # 
  User {{ ls_username }} # see https://profile.aai.lifescience-ri.eu/profile
  IdentityFile {{ ssh_key }}
  DynamicForward localhost:7777
  ForwardAgent yes
  ServerAliveInterval 120

# Access to de.NBI cloud floating IP networks via SOCKS Proxy
Host {{ openstack_floating_ip_ranges }} # 172.17.5.* 172.17.12.* for example
  ProxyCommand /usr/bin/socat --experimental "SOCKS5:localhost:7777:%h:%p" -
  IdentityFile {{ ssh_key }}
  ForwardAgent yes
  ServerAliveInterval 120
```

#### Jump Hosts

- Berlin `denbi-jumphost-01.bihealth.org`
- Heidelberg DKFZ `denbi-jumphost-01.denbi.dkfz-heidelberg.de`
- Heidelberg Uni `denbi-jumphost-01.bioquant.uni-heidelberg.de`

If you encounter errors, double check with the site specific documentation linked [above](#socks5).

### Let OpenStack Know
Then you need to export these values to allow the OpenStack API to reach your OpenStack provider.

```shell
export http_proxy=socks5h://localhost:7777
export https_proxy=socks5h://localhost:7777
export no_proxy=localhost,127.0.0.1,::1
```

### Establish the Connection

```shell
ssh -D 7777 -f -C -q -N ls_username@jumphost_name
```

### Configure BiBiGrid

Make sure to set `sock5_proxy` correctly in the bibigrid.yaml.
See [socks-proxy](../features/configuration.md#socks-proxy) for more information.