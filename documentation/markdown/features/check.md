# Check

## Exactly one master or vpn instance per configuration

There can only be a single master or a single vpn-gateway per configuration.

## Given Server group exist

If a server group is defined in the [Configuration](configuration.md), it must exist in the cloud.

## All instances are defined correctly

### All instances' images and flavors are compatible

Images have a minimal requirement for ram and disk space that a flavor needs to fulfil. If the given flavor fulfils the
image's requirement, the check is successful.

## All MasterMounts exist as snapshots or volumes

If any `MasterMounts` are defined in the [Configuration](configuration.md#mastermounts--optional-), they must exist in
the cloud as volumes or
snapshots (if they exist as snapshots, volumes will be created of them during creation).

## Network or Subnet is given and exists

A network or subnet must be defined in the [Configuration](configuration.md#subnet--required-), and it must exist in the
cloud as well.

## Quotas are not exceeded

Total cores, floating-ips (not working as OpenStack doesn't return the correct value), instances number, total ram,
volumes, volume gigabytes and snapshots are compared to the expected usage of the cluster. If the required resources
fit, the check is successful. Total cores and ram is used as OpenStack doesn't provide a feasible extraction
option for current usage.

## All public key files exist

If any additional public key files are defined in the [Configuration](configuration.md#sshpublickeyfiles--optional-),
the public key file must actually exist on the local machine.

### All public key files are secure (not failing)

BiBiGrid will also check whether your key is considered secure. Currently `RSA: >=4096`, `ECDSA: >=521`
and `ED25519: >=256`
are whitelisted. This check will only print a warning, but will not fail the check.

## All clouds yaml entries are secure and valid

If this check doesn't succeed, downloading the `clouds.yaml` again might be the fastest solution.
You can read more about these files here [Cloud-specification](cloud_specification_data.md)

### Valid

A `clouds.yaml` entry is considered valid if - combined with any `clouds-public.yaml` entries it refers to using the
profile
key - the following keys exist. Additional keys may be set, but are not required for the check to be successful.

#### Password

```yaml
  openstack:
    auth:
      username:
      password:
      auth_url:
    region_name:
```

#### Application Credential

```yaml
  openstack_giessen:
    auth:
      auth_url:
      application_credential_id:
      application_credential_secret:
    region_name:
    auth_type: "v3applicationcredential"
```

### Secure

A cloud-specification setup is considered secure if the `clouds-public.yaml` doesn't
contain `password`, `username`, `application_credential_id`,
`profile` or `application_credential_secret`.

## If NFS shares are given, nfs must be set to True