# Multi-Cloud

Multi-Cloud BiBiGrid allows for an easy cluster creation and management across multiple clouds.
With this configuration slurm will span over all given clouds and NFS share will be accessible by every node independent of its cloud.
Due to the high level of abstraction (VPN), using BiBiGrid's multi-cloud clusters is no more difficult than BiBiGrid's single cloud cluster.
However, the [configuration](configuration.md) of course needs to contain two cloud definitions, and you need access to both clouds.
Due to BiBiGrid's cloud separation by partition, users can specifically address individual clouds.

Slides briefly covering the development: [ELIXIR Compute 2023 -- Multi-Cloud - BiBiGrid.pdf](../../pdfs/ELIXIR%20Compute%202023%20--%20Multi-Cloud%20-%20BiBiGrid.pdf).


## VPN - Wireguard

## Port Security

## MTU Probing

## Deactivating Netplan