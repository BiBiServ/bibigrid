package de.unibi.cebitec.bibigrid.util;

import de.unibi.techfak.bibiserv.cms.Tparam;
import de.unibi.techfak.bibiserv.cms.TparamGroup;
import de.unibi.techfak.bibiserv.cms.Tprimitive;

/**
 *
 * @author benedikt
 */
public class RuleBuilder {

    private TparamGroup basicGroup = new TparamGroup();
    private TparamGroup intentGroup = new TparamGroup();

    public RuleBuilder(){


        Tparam.Min minValue = new Tparam.Min(); // min Value for all int options that need to be greater zero
        minValue.setValue(1);

        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("m", "master-instance-type", Tprimitive.STRING, "see INSTANCE-TYPES below", "intermediate"));

        Tparam mme = createBasicRule("mme", "max-master-ephemerals", Tprimitive.INT, "limits the maxium number of used ephemerals for master spool volume (raid 0)", "intermediate");
        mme.setMin(minValue);
        basicGroup.getParamrefOrParamGroupref().add(mme);

        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("M", "master-image", Tprimitive.STRING, "machine image id for master, if not set  images defined at https://bibiserv.cebitec.uni-bielefeld.de/resoruces/bibigrid/<framework>/<region>.ami.properties are used!", "beginner"));
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("s", "slave-instance-type", Tprimitive.STRING, "see INSTANCE-TYPES below", "intermediate"));

        Tparam mse = createBasicRule("mse", "max-slave-ephemerals", Tprimitive.INT, "limits the maxium number of used ephemerals for slave spool volume (raid 0 )", "intermediate");
        mse.setMin(minValue);
        basicGroup.getParamrefOrParamGroupref().add(mse);

        Tparam n = createBasicRule("n", "slave-instance-count", Tprimitive.INT, "min: 0", "intermediate");
        n.setMin(minValue);
        basicGroup.getParamrefOrParamGroupref().add(n);

        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("S", "slave-image", Tprimitive.STRING, "machine image id for slaves, same behaviour like master-image", "beginner"));
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("usir", "use-spot-instance-request", Tprimitive.BOOLEAN, " Yes or No of spot instances should be used  (Type t instance types are unsupported).", "intermediate")); // Wirklich boolean ?
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("bp", "bidprice", Tprimitive.FLOAT, "bid price for spot instances", "expert"));

        Tparam bpm = createBasicRule("bpm", "bidprice-master", Tprimitive.INT, "bid price for the master spot instance, if not set general 'bidprice' is used.", "expert");
        bpm.setMin(minValue);
        basicGroup.getParamrefOrParamGroupref().add(bpm);

        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("k", "keypair", Tprimitive.STRING, "name of the keypair in aws console", "beginner"));
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("i", "identity-file", Tprimitive.STRING, "absolute path to private ssh key file", "beginner"));
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("e", "region", Tprimitive.STRING, "region of instance", "expert"));
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("z", "availability-zone", Tprimitive.STRING, "e.g. availability-zone=eu-west-1a", "expert")); // Eigene short description
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("ex", "early-execute-script", Tprimitive.STRING, "path to shell script to be executed on master instance startup (size limitation of 10K chars)", "intermediate"));
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("esx", "early-slave-execute-script", Tprimitive.STRING, " path to shell script to be executed on slave instance(s) startup (size limitation of 10K chars)", "intermediate"));
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("a", "aws-credentials-file", Tprimitive.STRING, "containing access-key-id & secret-key, default: ~/.bibigrid.properties", "beginner"));

        Tparam port = createBasicRule("p", "ports", Tprimitive.INT, "comma-separated list of additional ports (tcp & udp) to be opened for all nodes (e.g. 80,443,8080). Ignored if 'security-basicGroup' is set!", "intermediate");
        port.setRegexp("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})(?:/(\\d{1,2}))*"); // Regex für Ports. Range Test zb. 0-255 sinnvoll?
        basicGroup.getParamrefOrParamGroupref().add(port);

        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("sg", "security-basicGroup", Tprimitive.STRING, "security basicGroup id used by current setup", "beginner"));
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("d", "master-mounts", Tprimitive.STRING, "comma-separated snapshot=mountpoint list (e.g. snap-12234abcd=/mnt/mydir1,snap-5667889ab=/mnt/mydir2) mounted to master. (Optional: Partition selection with ':', e.g. snap-12234abcd:1=/mnt/mydir1)", "intermediate"));
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("f", "slave-mounts", Tprimitive.STRING, "comma-separated snapshot=mountpoint list (e.g. snap-12234abcd=/mnt/mydir1,snap-5667889ab=/mnt/mydir2) mounted to all slaves individually", "intermediate")); //Regex zb. nach = Zeichen suchen?
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("x", "execute-script", Tprimitive.STRING, "shell script file to be executed on master", "beginner"));
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("g", "nfs-shares", Tprimitive.STRING, "comma-separated list of paths on master to be shared via NFS", "intermediate"));
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("v", "verbose", null, "more console output", "beginner"));
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("o", "config", Tprimitive.STRING, "path to alternative config file", "beginner"));
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("b", "use-master-as-compute", Tprimitive.BOOLEAN, "yes or no if master is supposed to be used as a compute instance", "intermediate"));
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("db", "cassandra", Tprimitive.BOOLEAN, "Enable Cassandra database support", "intermediate"));
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("gpf", "grid-properties-file", Tprimitive.STRING, "store essential grid properties like master & slave dns values and grid id in a Java property file", "beginner"));
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("vpc", "vpc-id", Tprimitive.STRING, "Vpc ID used instead of default vpc", "intermediate"));
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("router", "router", Tprimitive.STRING, "Name of router used (Openstack));, only one of --router --network or --subnet should be used. ", "intermediate"));
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("network", "network", Tprimitive.STRING, "Name of network used (Openstack));, only one of --router --network or --subnet should be used.", "intermediate"));
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("subnet", "subnet", Tprimitive.STRING, "Name of subnet used (Openstack));, only one of --router --network or --subnet should be used.", "intermediate"));
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("psi", "public-slave-ip", Tprimitive.BOOLEAN, "Slave instances also get an public ip address", "intermediate"));
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("me", "mesos", Tprimitive.BOOLEAN, "Yes or no if Mesos framework should be configured/started. Default is No", "intermediate"));
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("mode", "meta-mode", Tprimitive.STRING, "Allows you to use a different cloud provider e.g openstack with meta=openstack. Default AWS is used!", "intermediate"));
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("oge", "oge", Tprimitive.BOOLEAN, "Yes or no if OpenGridEngine should be configured/started. Default is Yes!", "intermediate"));
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("nfs", "nfs", Tprimitive.BOOLEAN, "Yes or no if NFS should be configured/started. Default is Yes!", "intermediate"));
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("lfs", "local-fs", Tprimitive.STRING, "File system used for internal (empheral)); diskspace. One of 'ext2', 'ext3', 'ext4' or 'xfs'. Default is 'xfs'.", "intermediate")); //Durch Regex verschiedene Fälle überprüfen? Enum
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("u", "user", Tprimitive.STRING, "User name (mandatory));", "beginner"));
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("osu", "openstack-username", Tprimitive.STRING, "The given Openstack Username", "beginner"));
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("ost", "openstack-tenantname", Tprimitive.STRING, "The given Openstack Tenantname", "beginner"));
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("osp", "openstack-password", Tprimitive.STRING, "The given Openstack User-Password", "beginner"));
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("ose", "openstack-endpoint", Tprimitive.STRING, "The given Openstack Endpoint e.g. (http://xxx.xxx.xxx.xxx:5000/v2.0/));", "beginner")); //Regex?
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("osd", "openstack-domain", Tprimitive.STRING, "The given Openstack Domain", "beginner"));
        basicGroup.getParamrefOrParamGroupref().add(createBasicRule("dr", "debug-requests", null, "Enable HTTP request and response logging.", "intermediate"));
        Tparam terminate = createBasicRule("t", "terminate", Tprimitive.STRING, "terminate running cluster", "beginner");
        intentGroup.getParamrefOrParamGroupref().add(terminate);
        intentGroup.getParamrefOrParamGroupref().add(createBasicRule("V", "version", null, "version", "beginner"));
        intentGroup.getParamrefOrParamGroupref().add(createBasicRule("h", "help", null, "help", "beginner"));
        intentGroup.getParamrefOrParamGroupref().add(createBasicRule("c", "create", null, "create cluster", "beginner"));
        intentGroup.getParamrefOrParamGroupref().add(createBasicRule("l", "list", null, "list running clusters", "beginner"));
        intentGroup.getParamrefOrParamGroupref().add(createBasicRule("ch", "check", null, "check config file", "beginner"));
    }

    private Tparam createBasicRule(String sflag, String lflag, Tprimitive type, String shortdesc, String guiGroup) {

        Tparam tp = new Tparam();

        tp.setId(sflag); // sflag for short commands
        tp.setOption(lflag); // lflag resp. option represent the long command

        Tparam.ShortDescription sdesc = new Tparam.ShortDescription();
        sdesc.setValue(shortdesc);
        tp.getShortDescription().add(sdesc);

        tp.setType(type);

        if (type == Tprimitive.STRING){
            tp.setMinLength(1); //if a string is needed it must not be empty
        }

        tp.setGuiElement(guiGroup);

        return tp;
    }

    public TparamGroup getBasicRules(){
        return basicGroup;
    }

    public TparamGroup getIntentRules(){
        return intentGroup;
    }

    public TparamGroup getAllRules() {

        for(int j = 0;j < intentGroup.getParamrefOrParamGroupref().size();j++){
            basicGroup.getParamrefOrParamGroupref().add(intentGroup.getParamrefOrParamGroupref().get(j));
        };
        return basicGroup;
    }
}