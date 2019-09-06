package de.unibi.cebitec.bibigrid.core.model;

import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;

import java.util.List;
import java.util.Properties;

public class Tomate extends Configuration{

        @Override
        public int getWorkerInstanceCount() {
        return super.getWorkerInstanceCount();
    }

        @Override
        public void setGridPropertiesFile(String gridPropertiesFile) {
        super.setGridPropertiesFile(gridPropertiesFile);
    }

        @Override
        public String getGridPropertiesFile() {
        return super.getGridPropertiesFile();
    }

        @Override
        public boolean isUseMasterAsCompute() {
        return super.isUseMasterAsCompute();
    }

        @Override
        public void setUseMasterAsCompute(boolean useMasterAsCompute) {
        super.setUseMasterAsCompute(useMasterAsCompute);
    }

        @Override
        public boolean isUseMasterWithPublicIp() {
        return super.isUseMasterWithPublicIp();
    }

        @Override
        public void setUseMasterWithPublicIp(boolean useMasterWithPublicIp) {
        super.setUseMasterWithPublicIp(useMasterWithPublicIp);
    }

        @Override
        public String getKeypair() {
        return super.getKeypair();
    }

        @Override
        public void setKeypair(String keypair) {
        super.setKeypair(keypair);
    }

        @Override
        public String getSshPublicKeyFile() {
        return super.getSshPublicKeyFile();
    }

        @Override
        public void setSshPublicKeyFile(String sshPublicKeyFile) {
        super.setSshPublicKeyFile(sshPublicKeyFile);
    }

        @Override
        public String getSshPrivateKeyFile() {
        return super.getSshPrivateKeyFile();
    }

        @Override
        public void setSshPrivateKeyFile(String sshPrivateKeyFile) {
        super.setSshPrivateKeyFile(sshPrivateKeyFile);
    }

        @Override
        public String getRegion() {
        return super.getRegion();
    }

        @Override
        public void setRegion(String region) {
        super.setRegion(region);
    }

        @Override
        public InstanceConfiguration getMasterInstance() {
        return super.getMasterInstance();
    }

        @Override
        public void setMasterInstance(InstanceConfiguration masterInstance) {
        super.setMasterInstance(masterInstance);
    }

        @Override
        public List<WorkerInstanceConfiguration> getSlaveInstances() {
        return super.getSlaveInstances();
    }

        @Override
        public void setSlaveInstances(List<WorkerInstanceConfiguration> workerInstances) {
        super.setSlaveInstances(workerInstances);
    }

        @Override
        public List<WorkerInstanceConfiguration> getWorkerInstances() {
        return super.getWorkerInstances();
    }

        @Override
        public void setWorkerInstances(List<WorkerInstanceConfiguration> workerInstances) {
        super.setWorkerInstances(workerInstances);
    }

        @Override
        public List<WorkerInstanceConfiguration> getExpandedWorkerInstances() {
        return super.getExpandedWorkerInstances();
    }

        @Override
        public String getAvailabilityZone() {
        return super.getAvailabilityZone();
    }

        @Override
        public void setAvailabilityZone(String availabilityZone) {
        super.setAvailabilityZone(availabilityZone);
    }

        @Override
        public String getServerGroup() {
        return super.getServerGroup();
    }

        @Override
        public void setServerGroup(String serverGroup) {
        super.setServerGroup(serverGroup);
    }

        @Override
        public String[] getClusterIds() {
        return super.getClusterIds();
    }

        @Override
        public void setClusterIds(String clusterIds) {
        super.setClusterIds(clusterIds);
    }

        @Override
        public void setClusterIds(String[] clusterIds) {
        super.setClusterIds(clusterIds);
    }

        @Override
        public List<Port> getPorts() {
        return super.getPorts();
    }

        @Override
        public void setPorts(List<Port> ports) {
        super.setPorts(ports);
    }

        @Override
        public List<MountPoint> getMasterMounts() {
        return super.getMasterMounts();
    }

        @Override
        public void setMasterMounts(List<MountPoint> masterMounts) {
        super.setMasterMounts(masterMounts);
    }

        @Override
        public List<String> getNfsShares() {
        return super.getNfsShares();
    }

        @Override
        public void setNfsShares(List<String> nfsShares) {
        super.setNfsShares(nfsShares);
    }

        @Override
        public List<MountPoint> getExtNfsShares() {
        return super.getExtNfsShares();
    }

        @Override
        public void setExtNfsShares(List<MountPoint> extNfsShares) {
        super.setExtNfsShares(extNfsShares);
    }

        @Override
        public boolean isAlternativeConfigFile() {
        return super.isAlternativeConfigFile();
    }

        @Override
        public String getAlternativeConfigPath() {
        return super.getAlternativeConfigPath();
    }

        @Override
        public void setAlternativeConfigPath(String alternativeConfigPath) {
        super.setAlternativeConfigPath(alternativeConfigPath);
    }

        @Override
        public String getNetwork() {
        return super.getNetwork();
    }

        @Override
        public void setNetwork(String network) {
        super.setNetwork(network);
    }

        @Override
        public boolean isNfs() {
        return super.isNfs();
    }

        @Override
        public void setNfs(boolean nfs) {
        super.setNfs(nfs);
    }

        @Override
        public boolean isOge() {
        return super.isOge();
    }

        @Override
        public void setOge(boolean oge) {
        super.setOge(oge);
    }

        @Override
        public String getMode() {
        return super.getMode();
    }

        @Override
        public void setMode(String mode) {
        super.setMode(mode);
    }


    public Tomate(){};


}
