# Update
This feature is experimental

Update re-uploads the playbook, updates the configuration data and executes the playbook again.

Updating the configuration data does not allow for all kinds of updates, because some changes - 
like attaching volumes, would need an undo process which is not implemented. That might come in a future version.
Therefore, some keys mentioned below in [updatable](#updatable) have "(activate)" behind them.
Those keys should not be deactivated, but only activated in updates. 

**Configuration keys not listed below are considered not updatable.**

## Updatable
- Ansible playbook


- workerInstances
- useMasterAsCompute
- userRoles
- cloudScheduling
- waitForServices
- features
- ide (activate)
- nfsShares (activate)
- zabbix (activate)