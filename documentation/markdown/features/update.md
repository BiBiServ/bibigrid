# Update
This feature is experimental

Update re-uploads the playbook, updates the configuration data and executes the playbook again.

For `workerInstances`, update also compares the newly configured worker groups against what's actually
running and starts any newly added static (`onDemand: False`) workers. Only appending to the end of a
worker group's `count` (or adding a new group) is supported - reordering, inserting, shrinking a group,
or flipping `onDemand` for an already-running worker is rejected before anything is touched, since
BiBiGrid recomputes worker names/indices from scratch on every run and would otherwise lose track of
already-running instances. Growing a multi-provider (multi-cloud/vpngtw) cluster isn't supported yet.

Updating the configuration data does not allow for all kinds of updates, because some changes - 
like attaching volumes, would need an undo process which is not implemented. That might come in a future version.
Therefore, some keys mentioned below in [updatable](#updatable) have "(activate)" behind them.
Those keys should not be deactivated, but only activated in updates. 

**Configuration keys not listed below are considered not updatable.**

## Updatable
- Ansible playbook


- workerInstances (append-only, see above)
- useMasterAsCompute
- userRoles
- cloudScheduling
- waitForServices
- features
- ide (activate)
- nfsShares (activate)
- zabbix (activate)