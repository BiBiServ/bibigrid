# GPU Nodes (GRES)

Currently, BiBiGrid can only handle GPU nodes with NVIDIA graphic cards. 
BiBiGrid will [install NVIDIA drivers](bibigrid/resources/playbook/roles/bibigrid/tasks/001-apt.yaml), 
set up the [gres.conf](https://slurm.schedmd.com/gres.conf.html) files and adjust 
the [slurm.conf](https://slurm.schedmd.com/slurm.conf.html#OPT_Gres_1) accordingly.

Additionally, `nvidia-cuda-toolkit` and `nvidia-container-runtime` are installed and configured nvidia-container-runtime 
to use docker: `nvidia-ctk runtime configure --runtime=docker`.

For this the gres information is stored in the group vars.

You can check the installation by executing nvidia-smi or `sinfo -N -o "%N %G"` (which lists the gres).