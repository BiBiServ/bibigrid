"""
Handles the schema validation for BiBiGrid's configuration yaml.
"""

from schema import Schema, Optional, Or, SchemaError

WORKER = {'type': str, 'image': str, Optional('count'): int, Optional('onDemand'): bool, Optional('partitions'): [str],
          Optional('features'): [str],
          Optional('bootVolume'): {
              Optional('name'): str,
              Optional('terminate'): bool,
              Optional('size'): int
          },
          Optional('volumes'): [{
              Optional('name'): str,
              Optional('snapshot'): str,  # optional; to create volume from
              # one or none of these
              Optional('permanent'): bool,
              Optional('semiPermanent'): bool,
              Optional('exists'): bool,
              Optional('mountPoint'): str,
              Optional('size'): int,
              Optional('fstype'): str,
              Optional('type'): str}]
          }

MASTER = VPN = {'type': str, 'image': str, Optional('count'): 1, Optional('onDemand'): bool, Optional('partitions'): [str],
                Optional('features'): [str],
                Optional('bootVolume'): {
                    Optional('name'): str,
                    Optional('terminate'): bool,
                    Optional('size'): int
                },
                Optional('volumes'): [{
                    Optional('name'): str,
                    Optional('snapshot'): str,  # optional; to create volume from
                    # one or none of these
                    Optional('permanent'): bool,
                    Optional('semiPermanent'): bool,
                    Optional('exists'): bool,
                    Optional('mountPoint'): str,
                    Optional('size'): int,
                    Optional('fstype'): str,
                    Optional('type'): str}]
                }

# Define the schema for the configuration file
master_schema = Schema(
    {'infrastructure': str, 'cloud': str, 'sshUser': str, Or('subnet', 'network'): str, 'cloud_identifier': str,
     Optional('sshPublicKeyFiles'): [str], Optional('sshTimeout'): int,
     Optional('cloudScheduling'): {Optional('sshTimeout'): int},
     Optional('nfsShares'): [str],
     Optional('userRoles'): [
         {'hosts': [str], 'roles': [{'name': str, Optional('tags'): [str]}], Optional('varsFiles'): [str]}],
     Optional('localFS'): bool, Optional('localDNSlookup'): bool, Optional('slurm'): bool,
     Optional('slurmConf'): {Optional('db'): str, Optional('db_user'): str, Optional('db_password'): str,
                             Optional('munge_key'): str, Optional('elastic_scheduling'): {Optional('SuspendTime'): int,
                                                                                          Optional(
                                                                                              'SuspendTimeout'): int,
                                                                                          Optional(
                                                                                              'ResumeTimeout'): int,
                                                                                          Optional('TreeWidth'): int}},
     Optional('zabbix'): bool, Optional('nfs'): bool, Optional('ide'): bool, Optional('useMasterAsCompute'): bool,
     Optional('useMasterWithPublicIp'): bool, Optional('waitForServices'): [str],
     Optional('gateway'): {'ip': str, 'portFunction': str}, Optional('dontUploadCredentials'): bool,
     Optional('fallbackOnOtherImage'): bool,
     Optional('localDNSLookup'): bool, Optional('features'): [str], 'workerInstances': [
        WORKER],
     'masterInstance': MASTER,
     Optional('bootVolume'): {
         Optional('name'): str,
         Optional('terminate'): bool,
         Optional('size'): int
     },
     })

other_schema = Schema(
    {'infrastructure': str, 'cloud': str, 'sshUser': str, Or('subnet', 'network'): str, 'cloud_identifier': str,
     Optional('waitForServices'): [str], Optional('features'): [str], 'workerInstances': [
        WORKER], 'vpnInstance': VPN,
     Optional('bootVolume'): {
         Optional('name'): str,
         Optional('terminate'): bool,
         Optional('size'): int
     },
     })


def validate_configurations(configurations, log):
    log.info("Validating config file schema...")
    configuration = None
    try:
        configuration = configurations[0]
        if configuration.get("region") or configuration.get("availabilityZone"):
            log.warning(
                "Keys 'region' and 'availabilityZone' are deprecated! Check will return False if you use one of them."
                "Just remove them. They are no longer required.")
        master_schema.validate(configuration)
        log.debug(f"Master configuration '{configuration['cloud_identifier']}' valid.")
        for configuration in configurations[1:]:
            if configuration.get("region") or configuration.get("availabilityZone"):
                log.warning(
                    "Keys region and availabilityZone are deprecated! Check will return False if you use one of them."
                    "Just remove them. They are no longer required.")
            other_schema.validate(configuration)
            log.debug(f"Configuration '{configuration['cloud_identifier']}' schema valid.")
        log.debug("Entire configuration schema valid.")
        return True
    except SchemaError as err:
        log.warning(
            f"Configuration '{configuration.get('cloud_identifier', 'No identifier found')}' invalid. See error: "
            f"{err}.")
        return False
