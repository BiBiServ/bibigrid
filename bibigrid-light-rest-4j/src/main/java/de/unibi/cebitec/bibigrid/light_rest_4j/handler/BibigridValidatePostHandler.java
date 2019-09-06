package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.unibi.cebitec.bibigrid.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.body.BodyHandler;
import com.networknt.handler.LightHttpHandler;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.Port;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;

import java.io.DataInput;
import java.util.*;

public class BibigridValidatePostHandler implements LightHttpHandler{


    public Configuration c = new Configuration() {
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

        @Override
        public String getUser() {
            return super.getUser();
        }

        @Override
        public void setUser(String user) {
            super.setUser(user);
        }

        @Override
        public String getSshUser() {
            return super.getSshUser();
        }

        @Override
        public void setSshUser(String sshUser) {
            super.setSshUser(sshUser);
        }

        @Override
        public FS getLocalFS() {
            return super.getLocalFS();
        }

        @Override
        public void setLocalFS(FS localFS) {
            super.setLocalFS(localFS);
        }

        @Override
        public String getSubnet() {
            return super.getSubnet();
        }

        @Override
        public void setSubnet(String subnet) {
            super.setSubnet(subnet);
        }

        @Override
        public boolean isDebugRequests() {
            return super.isDebugRequests();
        }

        @Override
        public void setDebugRequests(boolean debugRequests) {
            super.setDebugRequests(debugRequests);
        }

        @Override
        public boolean isIDE() {
            return super.isIDE();
        }

        @Override
        public boolean isCloud9() {
            return super.isCloud9();
        }

        @Override
        public void setCloud9(boolean cloud9) throws ConfigurationException {
            super.setCloud9(cloud9);
        }

        @Override
        public boolean isTheia() {
            return super.isTheia();
        }

        @Override
        public void setTheia(boolean theia) throws ConfigurationException {
            super.setTheia(theia);
        }

        @Override
        public String getCredentialsFile() {
            return super.getCredentialsFile();
        }

        @Override
        public void setCredentialsFile(String credentialsFile) {
            super.setCredentialsFile(credentialsFile);
        }

        @Override
        public String getCloud9Workspace() {
            return super.getCloud9Workspace();
        }

        @Override
        public void setCloud9Workspace(String cloud9Workspace) {
            super.setCloud9Workspace(cloud9Workspace);
        }

        @Override
        public String getWorkspace() {
            return super.getWorkspace();
        }

        @Override
        public void setWorkspace(String workspace) {
            super.setWorkspace(workspace);
        }

        @Override
        public boolean isLocalDNSLookup() {
            return super.isLocalDNSLookup();
        }

        @Override
        public void setLocalDNSLookup(boolean localDNSLookup) {
            super.setLocalDNSLookup(localDNSLookup);
        }

        @Override
        public boolean isSlurm() {
            return super.isSlurm();
        }

        @Override
        public void setSlurm(boolean slurm) {
            super.setSlurm(slurm);
        }

        @Override
        public String getMungeKey() {
            return super.getMungeKey();
        }

        @Override
        public void setMungeKey(String mungeKey) {
            super.setMungeKey(mungeKey);
        }

        @Override
        public boolean isGanglia() {
            return super.isGanglia();
        }

        @Override
        public void setGanglia(boolean ganglia) {
            super.setGanglia(ganglia);
        }

        @Override
        public boolean isZabbix() {
            return super.isZabbix();
        }

        @Override
        public void setZabbix(boolean zabbix) {
            super.setZabbix(zabbix);
        }

        @Override
        public ZabbixConf getZabbixConf() {
            return super.getZabbixConf();
        }

        @Override
        public void setZabbixConf(ZabbixConf zabbixConf) {
            super.setZabbixConf(zabbixConf);
        }

        @Override
        public Properties getOgeConf() {
            return super.getOgeConf();
        }

        @Override
        public void setOgeConf(Properties ogeConf) {
            super.setOgeConf(ogeConf);
        }

        @Override
        public boolean hasCustomAnsibleRoles() {
            return super.hasCustomAnsibleRoles();
        }

        @Override
        public boolean hasCustomAnsibleGalaxyRoles() {
            return super.hasCustomAnsibleGalaxyRoles();
        }

        @Override
        public List<AnsibleRoles> getAnsibleRoles() {
            return super.getAnsibleRoles();
        }

        @Override
        public void setAnsibleRoles(List<AnsibleRoles> ansibleRoles) {
            super.setAnsibleRoles(ansibleRoles);
        }

        @Override
        public List<AnsibleGalaxyRoles> getAnsibleGalaxyRoles() {
            return super.getAnsibleGalaxyRoles();
        }

        @Override
        public void setAnsibleGalaxyRoles(List<AnsibleGalaxyRoles> ansibleGalaxyRoles) {
            super.setAnsibleGalaxyRoles(ansibleGalaxyRoles);
        }
    };

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {




        ObjectMapper mapper = new ObjectMapper();
        Map s = (Map)exchange.getAttachment(BodyHandler.REQUEST_BODY);
        String j = mapper.writeValueAsString(s);
        JSONObject json = (JSONObject)new JSONParser().parse(j);
        byte[] decodedBytes = Base64.getDecoder().decode( (String) json.get("credentialsFile"));
        String decodedString = new String(decodedBytes);
        System.out.println(decodedString);

//        try {
//            ObjectMapper objectMapper = new ObjectMapper();
//            c = objectMapper.readValue(j, Configuration.class);         }
//        catch(Exception e){
//            System.out.println(e);
//        }



        c.setMode((String) json.get("mode"));
        c.setCredentialsFile((String) json.get("credentialsFile"));


        System.out.println(c.getMode());


        exchange.endExchange();
    }
}
