# Instance Configuration
**This is work in progress, meaning that this part is not yet integrated into the BiBiGrid workflow nor the documentation is 
complete.

jkrue, 12/14/2017
**



## Current (previous) state
After instance startup by used cloud API the further configuration is currently done by generated scripts, which will be executed during(cloud-init) or after (ssh) initialization. Special images for master and slave are needed, which must be prepared beforehand.

On the one hand this kind of implementation is (very) fast to setup a BiBiGrid cluster, but on the other hand it is quite inflexible. Any changes (software and os updates, cloud infrastructure changes) must be implemented directly in the source.

## Future state
For the next version of BiBiGrid we want to make the configuration part more flexible and independent from previous prepared images.
BiBiGrid now uses [Ansible](https://www.ansible.com) to configure the master and all slave instances.

### Image independency
Using Ansible for configuration makes BiBiGrid more flexible. While it's possible to use prepared images to speed up the BiBiGrid initialization it is not necessary anymore. Ansible (tasks) take care that missing software is installed (and configured) during runtime.

### Distribution (version) independency
Using Ansible tasks make BiBiGrid more independend from any distribution as long as the task itself is written in a distribution independed manner. In either case, adjusting a task to be compatible  with a new or other distribution is much easier than rewrite a BiBiGrid core source block.

## Easy testing
The ansible playbook can be easily tested without any cloud access using [Vagrant](https://www.vagrantup.com). The Vagrantfile coming with BiBiGrid configure 2 VMs (master and slave) on top of [VirtualBox](https://www.virtualbox.org).