# SOCKS5

In order to use BiBiGrid via a jump host one has to setup a SOCKS5 proxy; if you haven't already.

Many cloud locations have their own tutorials for that:
- [Berlin](https://cloud.denbi.de/wiki/Compute_Center/Heidelberg-DKFZ/#setting-up-a-socks-proxy)
- [Heidelberg DKFZ](https://cloud.denbi.de/wiki/Compute_Center/Heidelberg-DKFZ/#setting-up-a-socks-proxy)
- [Heidelberg Uni](https://cloud.denbi.de/wiki/Compute_Center/Heidelberg-DKFZ/#setting-up-a-socks-proxy)
- 
## Setting Up SOCKS5

Add the following line to your local `~/.ssh/config`:

```
Include ~/.ssh/denbi_ssh_config
```

Create the file `~/.ssh/denbi_ssh_config` and fill in:
```
# Access to the de.NBI jumphost
Host {{ jumphost_name }}
  User {{ ls_username }} # see https://profile.aai.lifescience-ri.eu/profile
  IdentityFile {{ ssh_key }}
  DynamicForward localhost:7777
  ForwardAgent yes
  ServerAliveInterval 120

# Access to de.NBI cloud floating IP networks via SOCKS Proxy
Host {{ openstack_"public"_ip_ranges }} # 172.17.5.* for example
  ProxyCommand /usr/bin/socat --experimental "SOCKS5:localhost:7777:%h:%p" -
  IdentityFile {{ ssh_key }}
  ForwardAgent yes
  ServerAliveInterval 120
```

Then you need to export these values to allow the OpenStack API to reach your OpenStack provider.

```shell
export http_proxy=socks5h://localhost:7777
export https_proxy=socks5h://localhost:7777
export no_proxy=localhost,127.0.0.1,::1
```

and establish the connection:

```shell
ssh -D 7777 -f -C -q -N ls_username@jumphost_name
```





Make sure to set `sock5_proxy` correctly in the bibigrid.yaml.
See [socks-proxy](../features/configuration.md#socks-proxy) for more information.