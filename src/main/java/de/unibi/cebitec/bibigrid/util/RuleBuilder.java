package de.unibi.cebitec.bibigrid.util;

import de.unibi.techfak.bibiserv.cms.Tparam;
import de.unibi.techfak.bibiserv.cms.TparamGroup;
import de.unibi.techfak.bibiserv.cms.Tprimitive;

/**
 *
 * @author benedikt
 */
public class RuleBuilder {

    private TparamGroup group = new TparamGroup();

    public RuleBuilder(){


        Tparam.Min minValue = new Tparam.Min(); // min Value for all int options that need to be greater zero
        minValue.setValue(1);

        group.getParamrefOrParamGroupref().add(createBasicRule("m","master-instance-type",Tprimitive.STRING, "see INSTANCE-TYPES below"));

        Tparam mme = createBasicRule("mme", "max-master-ephemerals", Tprimitive.INT, "limits the maxium number of used ephemerals for master spool volume (raid 0)");
        mme.setMin(minValue);
        group.getParamrefOrParamGroupref().add(mme);

        group.getParamrefOrParamGroupref().add(createBasicRule("M", "master-image", Tprimitive.STRING, "machine image id for master, if not set  images defined at https://bibiserv.cebitec.uni-bielefeld.de/resources/bibigrid/<framework>/<region>.ami.properties are used!"));
        group.getParamrefOrParamGroupref().add(createBasicRule("s", "slave-instance-type", Tprimitive.STRING, "see INSTANCE-TYPES below"));

        Tparam mse = createBasicRule("mse", "max-slave-ephemerals", Tprimitive.INT, "limits the maxium number of used ephemerals for slave spool volume (raid 0 )");
        mse.setMin(minValue);
        group.getParamrefOrParamGroupref().add(mse);

        Tparam n = createBasicRule("n", "slave-instance-count", Tprimitive.INT, "min: 0");
        n.setMin(minValue);
        group.getParamrefOrParamGroupref().add(n);

        group.getParamrefOrParamGroupref().add(createBasicRule("S", "slave-image", Tprimitive.STRING, "machine image id for slaves, same behaviour like master-image"));
        group.getParamrefOrParamGroupref().add(createBasicRule("usir", "use-spot-instance-request", Tprimitive.BOOLEAN, " Yes or No of spot instances should be used  (Type t instance types are unsupported).")); // Wirklich boolean ?
        group.getParamrefOrParamGroupref().add(createBasicRule("bp", "bidprice", Tprimitive.FLOAT, "bid price for spot instances"));

        Tparam bpm = createBasicRule("bpm", "bidprice-master", Tprimitive.INT, "bid price for the master spot instance, if not set general 'bidprice' is used.");
        bpm.setMin(minValue);
        group.getParamrefOrParamGroupref().add(bpm);

        group.getParamrefOrParamGroupref().add(createBasicRule("k", "keypair", Tprimitive.STRING, "name of the keypair in aws console"));
        group.getParamrefOrParamGroupref().add(createBasicRule("i", "identity-file", Tprimitive.STRING, "absolute path to private ssh key file"));
        group.getParamrefOrParamGroupref().add(createBasicRule("e", "region", Tprimitive.STRING, "region of instance"));
        group.getParamrefOrParamGroupref().add(createBasicRule("z", "availability-zone", Tprimitive.STRING, "e.g. availability-zone=eu-west-1a")); // Eigene short description
        group.getParamrefOrParamGroupref().add(createBasicRule("ex", "early-execute-script", Tprimitive.STRING, "path to shell script to be executed on master instance startup (size limitation of 10K chars)"));
        group.getParamrefOrParamGroupref().add(createBasicRule("esx", "early-slave-execute-script", Tprimitive.STRING, " path to shell script to be executed on slave instance(s) startup (size limitation of 10K chars)"));
        group.getParamrefOrParamGroupref().add(createBasicRule("a", "aws-credentials-file", Tprimitive.STRING, "containing access-key-id & secret-key, default: ~/.bibigrid.properties"));

        Tparam port = createBasicRule("p", "ports", Tprimitive.INT, "comma-separated list of additional ports (tcp & udp) to be opened for all nodes (e.g. 80,443,8080). Ignored if 'security-group' is set!");
        port.setRegexp("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})(?:/(\\d{1,2}))*"); // Regex für Ports. Range Test zb. 0-255 sinnvoll?
        group.getParamrefOrParamGroupref().add(port);

        group.getParamrefOrParamGroupref().add(createBasicRule("sg", "security-group", Tprimitive.STRING,"security group id used by current setup"));
        group.getParamrefOrParamGroupref().add(createBasicRule("d", "master-mounts", Tprimitive.STRING, "comma-separated volume/snapshot id=mountpoint list (e.g. snap-12234abcd=/mnt/mydir1,snap-5667889ab=/mnt/mydir2) mounted to master. (Optional: Partition selection with ':', e.g. snap-12234abcd:1=/mnt/mydir1)"));
        group.getParamrefOrParamGroupref().add(createBasicRule("f", "slave-mounts", Tprimitive.STRING, "comma-separated snapshot id=mountpoint list (e.g. snap-12234abcd=/mnt/mydir1,snap-5667889ab=/mnt/mydir2) mounted to all slaves individually")); //Regex zb. nach = Zeichen suchen?
        group.getParamrefOrParamGroupref().add(createBasicRule("x", "execute-script", Tprimitive.STRING, "shell script file to be executed on master"));
        group.getParamrefOrParamGroupref().add(createBasicRule("g", "nfs-shares", Tprimitive.STRING, "comma-separated list of paths on master to be shared via NFS (e.g. 192.168.10.44=/export/data,192.168.10.44=/export/bla)"));
        group.getParamrefOrParamGroupref().add(createBasicRule("ge", "ext-nfs-shares", Tprimitive.STRING,"comma-separated nfsserver=path list (e.g."));
        group.getParamrefOrParamGroupref().add(createBasicRule("v", "verbose", null, "more console output"));
        group.getParamrefOrParamGroupref().add(createBasicRule("o", "config", Tprimitive.STRING, "path to alternative config file"));
        group.getParamrefOrParamGroupref().add(createBasicRule("b", "use-master-as-compute", Tprimitive.BOOLEAN, "yes or no if master is supposed to be used as a compute instance"));
        group.getParamrefOrParamGroupref().add(createBasicRule("hdfs","hdfs",Tprimitive.BOOLEAN, "Enable HDFS support"));
        group.getParamrefOrParamGroupref().add(createBasicRule("db", "cassandra", Tprimitive.BOOLEAN, "Enable Cassandra database support"));
        group.getParamrefOrParamGroupref().add(createBasicRule("spark","spark",Tprimitive.BOOLEAN, "Enable Spark cluster support"));
        group.getParamrefOrParamGroupref().add(createBasicRule("gpf", "grid-properties-file", Tprimitive.STRING, "store essential grid properties like master & slave dns values and grid id in a Java property file"));
        group.getParamrefOrParamGroupref().add(createBasicRule("vpc", "vpc-id", Tprimitive.STRING, "Vpc ID used instead of default vpc"));
        group.getParamrefOrParamGroupref().add(createBasicRule("router", "router", Tprimitive.STRING, "Name of router used (Openstack));, only one of --router --network or --subnet should be used. "));
        group.getParamrefOrParamGroupref().add(createBasicRule("network", "network", Tprimitive.STRING, "Name of network used (Openstack));, only one of --router --network or --subnet should be used."));
        group.getParamrefOrParamGroupref().add(createBasicRule("subnet", "subnet", Tprimitive.STRING, "Naem of subnet used (Openstack));, only one of --router --network or --subnet should be used."));
        group.getParamrefOrParamGroupref().add(createBasicRule("psi", "public-slave-ip", Tprimitive.BOOLEAN, "Slave instances also get an public ip address"));
        group.getParamrefOrParamGroupref().add(createBasicRule("me", "mesos", Tprimitive.BOOLEAN, "Yes or no if Mesos framework should be configured/started. Default is No"));
        group.getParamrefOrParamGroupref().add(createBasicRule("mode", "meta-mode", Tprimitive.STRING, "Allows you to use a different cloud provider e.g openstack with meta=openstack. Default AWS is used!"));
        group.getParamrefOrParamGroupref().add(createBasicRule("oge", "oge", Tprimitive.BOOLEAN, "Yes or no if OpenGridEngine should be configured/started. Default is Yes!"));
        group.getParamrefOrParamGroupref().add(createBasicRule("nfs", "nfs", Tprimitive.BOOLEAN, "Yes or no if NFS should be configured/started. Default is Yes!"));
        group.getParamrefOrParamGroupref().add(createBasicRule("lfs", "local-fs", Tprimitive.STRING, "File system used for internal (empheral)); diskspace. One of 'ext2', 'ext3', 'ext4' or 'xfs'. Default is 'xfs'.")); //Durch Regex verschiedene Fälle überprüfen? Enum
        group.getParamrefOrParamGroupref().add(createBasicRule("u", "user", Tprimitive.STRING, "User name (mandatory));"));
        group.getParamrefOrParamGroupref().add(createBasicRule("osu", "openstack-username", Tprimitive.STRING, "The given Openstack Username"));
        group.getParamrefOrParamGroupref().add(createBasicRule("ost", "openstack-tenantname", Tprimitive.STRING, "The given Openstack Tenantname"));
        group.getParamrefOrParamGroupref().add(createBasicRule("osp", "openstack-password", Tprimitive.STRING, "The given Openstack User-Password"));
        group.getParamrefOrParamGroupref().add(createBasicRule("ose", "openstack-endpoint", Tprimitive.STRING, "The given Openstack Endpoint e.g. (http://xxx.xxx.xxx.xxx:5000/v2.0/));")); //Regex?
        group.getParamrefOrParamGroupref().add(createBasicRule("osd", "openstack-domain", Tprimitive.STRING, "The given Openstack Domain"));
        group.getParamrefOrParamGroupref().add(createBasicRule("osdt", "openstack-tenantdomain", Tprimitive.STRING, "The given Openstack Tenant Domain"));
        group.getParamrefOrParamGroupref().add(createBasicRule("dr", "debug-requests", null, "Enable HTTP request and response logging."));

    }

    private Tparam createBasicRule(String sflag, String lflag, Tprimitive type, String shortdesc){

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

        return tp;
    }

    public TparamGroup getRules(){
        return group;
    }
}