# Check

## Exactly one master or vpn instance per configuration

## Given Server group exist

## All instances are defined correctly

### Flavor

### Image

### All instances' images and flavors are compatible

## All MasterMounts exist as snapshots or volumes

## Given Networks And Subnets Exist

## Quotas are not exceeded

## All public key files exist

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