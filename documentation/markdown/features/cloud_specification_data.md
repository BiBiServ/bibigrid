# Cloud Specification Data

To access the cloud, authentication information is required. The BiBiGrid no longer uses environment variables, but a
two file system instead.
`clouds.yaml` and `clouds-public.yaml` can be placed in `~/.config/bibigrid/` or `/etc/bibigrid/` and will be loaded by
BiBiGrid on execution.
While you store your password and username in `clouds.yaml` (private), you can store all other information ready to
share in `clouds-public.yaml` (shareable).
However, all information can just be stored in `clouds.yaml`.

Keys set in `clouds.yaml` will overwrite keys from `clouds-public.yaml`.

## Openstack

Be aware that the downloaded `clouds.yaml` file contains all information.
OpenStack does not split information into `clouds.yaml` and `clouds-public.yaml` on its own.
The example files show an example split.

### Password Example

Using the password `clouds.yaml` is easy. However, since passwords -
unlike [Application Credentials](#application-credentials-example)
don't have an expiration date, caution is advised.

![Download](../../images/features/cloud_specification_data/pw_screen1.png)

Move the downloaded file to `~/.config/bibigrid/` or `/etc/bibigrid/`.

##### Password clouds.yaml

```yaml
clouds:
  openstack:
    profile: nameOfCloudsPublicYamlEntry
    auth:
      username: SamSampleman
      password: SecurePassword
```

##### Password clouds-public.yaml

```yaml
public-clouds:
  nameOfCloudsPublicYamlEntry:
    auth:
      auth_url: https://somelink:someport
      project_id: someProjectId
      project_name: someProjectName
      user_domain_name: someDomainName
    region_name: someRegionName
    interface: "public"
    identity_api_version: 3
```

### Application Credentials Example

The following show, how an Application Credential can be created and the related `clouds.yaml` downloaded.
Application Credentials are the preferred way of authentication since they do have an expiration date and
their access can be limited.

![Navigation](../../images/features/cloud_specification_data/ac_screen1.png)
![Creation](../../images/features/cloud_specification_data/ac_screen2.png)
![Download](../../images/features/cloud_specification_data/ac_screen3.png)

Move the downloaded file to `~/.config/bibigrid/` or `/etc/bibigrid/`.

#### Application Credential clouds.yaml

```yaml
clouds:
  openstack:
    profile: nameOfCloudsPublicYamlEntry
    auth:
      application_credential_id: SomeID
      application_credential_secret: SecureSecret
```

#### Application Credential clouds-public.yaml

```yaml
public-clouds:
  nameOfCloudsPublicYamlEntry:
    auth:
      auth_url: https://somelink:someport
    region_name: SomeRegion
    interface: "public"
    identity_api_version: 3
    auth_type: "v3applicationcredential"
```