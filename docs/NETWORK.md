# Cloud Network
Documentation about the cluster network built for each provider implementation.

## Ports
Aside from the user defined ports the following standard ports are configured for all providers.

| Port (range) | Protocol | IP range                |
|--------------|----------|-------------------------|
| 22           | TCP      | 0.0.0.0/0               |
|              | ICMP     | subnet / security group |
| 1 - 65535    | TCP      | subnet / security group |
| 1 - 65535    | UDP      | subnet / security group |
