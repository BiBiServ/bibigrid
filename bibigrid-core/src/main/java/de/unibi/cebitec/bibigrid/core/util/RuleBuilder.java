package de.unibi.cebitec.bibigrid.core.util;

import de.unibi.techfak.bibiserv.cms.Tparam;
import de.unibi.techfak.bibiserv.cms.TparamGroup;
import de.unibi.techfak.bibiserv.cms.Tprimitive;

import java.util.List;

public class RuleBuilder {
    private final TparamGroup group = new TparamGroup();

    public RuleBuilder() {
        List<Object> groupReference = group.getParamrefOrParamGroupref();
        // Global program rules
        groupReference.add(createBasicRule(RuleNames.VERBOSE_S, RuleNames.VERBOSE_L, null, "more console output"));
        groupReference.add(createBasicRule(RuleNames.DEBUG_REQUESTS_S, RuleNames.DEBUG_REQUESTS_L, null,
                "Enable HTTP request and response logging"));

        groupReference.add(createBasicRule(RuleNames.HELP_LIST_INSTANCE_TYPES_S, RuleNames.HELP_LIST_INSTANCE_TYPES_L,
                null, "Help: list instance types"));

        // Master instance rules
        groupReference.add(createStringRule(RuleNames.MASTER_INSTANCE_TYPE_S, RuleNames.MASTER_INSTANCE_TYPE_L,
                "Master instance type to be used. Execute \"-h -lit\" for a complete list"));
        groupReference.add(createIntRule(RuleNames.MAX_MASTER_EPHEMERALS_S, RuleNames.MAX_MASTER_EPHEMERALS_L,
                "Limits the maximum number of used ephemerals for master spool volume (raid 0)", 1));
        groupReference.add(createStringRule(RuleNames.MASTER_IMAGE_S, RuleNames.MASTER_IMAGE_L,
                "Machine image id for master, if not set images defined at https://bibiserv.cebitec.uni-bielefeld.de" +
                        "/resources/bibigrid/<framework>/<region>.ami.properties are used!"));
        // TODO: Regex for ex. search for =?
        groupReference.add(createStringRule(RuleNames.MASTER_MOUNTS_S, RuleNames.MASTER_MOUNTS_L,
                "Comma-separated volume/snapshot id=mountpoint list (e.g. snap-12234abcd=/mnt/mydir1,snap-5667889ab=/mnt/mydir2) " +
                        "mounted to master. (Optional: Partition selection with ':', e.g. snap-12234abcd:1=/mnt/mydir1)"));
        groupReference.add(createBooleanRule(RuleNames.USE_MASTER_AS_COMPUTE_S, RuleNames.USE_MASTER_AS_COMPUTE_L,
                "yes or no if master is supposed to be used as a compute instance"));
        groupReference.add(createBooleanRule(RuleNames.USE_MASTER_WITH_PUBLIC_IP_S, RuleNames.USE_MASTER_WITH_PUBLIC_IP_L,
                "yes or no if master is supposed to be used with a public ip address"));

        // Slave instance rules
        groupReference.add(createStringRule(RuleNames.SLAVE_INSTANCE_TYPE_S, RuleNames.SLAVE_INSTANCE_TYPE_L,
                "Slave instance type to be used. Execute \"-h -lit\" for a complete list"));
        groupReference.add(createIntRule(RuleNames.MAX_SLAVE_EPHEMERALS_S, RuleNames.MAX_SLAVE_EPHEMERALS_L,
                "Limits the maximum number of used ephemerals for slave spool volume (raid 0)", 1));
        // TODO: "min: 0" but min.setValue(1) ?
        groupReference.add(createIntRule(RuleNames.SLAVE_INSTANCE_COUNT_S, RuleNames.SLAVE_INSTANCE_COUNT_L,
                "min: 0", 1));
        groupReference.add(createStringRule(RuleNames.SLAVE_IMAGE_S, RuleNames.SLAVE_IMAGE_L,
                "Machine image id for slaves, same behaviour like master-image"));

        // Other rules
        groupReference.add(createBooleanRule(RuleNames.USE_SPOT_INSTANCE_REQUEST_S, RuleNames.USE_SPOT_INSTANCE_REQUEST_L,
                "[yes, no] if spot instances should be used"));
        groupReference.add(createBasicRule(RuleNames.BID_PRICE_S, RuleNames.BID_PRICE_L,
                Tprimitive.FLOAT, "bid price for spot instances"));

        groupReference.add(createIntRule(RuleNames.BID_PRICE_MASTER_S, RuleNames.BID_PRICE_MASTER_L,
                "Bid price for the master spot instance, if not set general 'bidprice' is used.", 1));

        groupReference.add(createStringRule(RuleNames.KEYPAIR_S, RuleNames.KEYPAIR_L,
                "Name of the keypair stored in the cloud provider console"));
        groupReference.add(createStringRule(RuleNames.SSH_PUBLIC_KEY_FILE_S, RuleNames.SSH_PUBLIC_KEY_FILE_L,
                "Absolute path to public ssh key file"));
        groupReference.add(createStringRule(RuleNames.SSH_PRIVATE_KEY_FILE_S, RuleNames.SSH_PRIVATE_KEY_FILE_L,
                "Absolute path to private ssh key file"));
        groupReference.add(createStringRule(RuleNames.REGION_S, RuleNames.REGION_L,
                "Region in which the cluster is created"));
        groupReference.add(createStringRule(RuleNames.AVAILABILITY_ZONE_S, RuleNames.AVAILABILITY_ZONE_L,
                "Specific zone in the provided region (e.g. AWS: eu-west-1a, Google: europe-west1-b)"));
        groupReference.add(createStringRule(RuleNames.AWS_CREDENTIALS_FILE_S, RuleNames.AWS_CREDENTIALS_FILE_L,
                "Containing access-key-id & secret-key, default: ~/.bibigrid.properties"));

        Tparam port = createIntRule(RuleNames.PORTS_S, RuleNames.PORTS_L,
                "Comma-separated list of additional ports (tcp & udp) to be opened for all nodes (e.g. 80,443,8080). " +
                        "(Ignored if 'security-group' is set for Openstack!)");
        // Regex für Ports. TODO: Range Test ex. 0-255 useful?
        port.setRegexp("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})(?:/(\\d{1,2}))*");
        groupReference.add(port);

        groupReference.add(createStringRule(RuleNames.SECURITY_GROUP_S, RuleNames.SECURITY_GROUP_L,
                "Security group id used by current setup"));
        groupReference.add(createStringRule(RuleNames.NFS_SHARES_S, RuleNames.NFS_SHARES_L,
                "Comma-separated list of paths on master to be shared via NFS (e.g. 192.168.10.44=/export/data,192.168.10.44=/export/bla)"));
        groupReference.add(createStringRule(RuleNames.EXT_NFS_SHARES_S, RuleNames.EXT_NFS_SHARES_L,
                "Comma-separated nfsserver=path list (e.g.")); // TODO: e.g.?
        groupReference.add(createStringRule(RuleNames.CONFIG_S, RuleNames.CONFIG_L, "Path to alternative config file"));
        groupReference.add(createStringRule(RuleNames.GRID_PROPERTIES_FILE_S, RuleNames.GRID_PROPERTIES_FILE_L,
                "Store essential grid properties like master & slave dns values and grid id in a Java property file"));
        groupReference.add(createStringRule(RuleNames.ROUTER_S, RuleNames.ROUTER_L, "Name of router used (Openstack)"));
        groupReference.add(createStringRule(RuleNames.NETWORK_S, RuleNames.NETWORK_L, "Name of network used"));
        groupReference.add(createStringRule(RuleNames.SUBNET_S, RuleNames.SUBNET_L, "Name of subnet used"));
        groupReference.add(createBooleanRule(RuleNames.PUBLIC_SLAVE_IP_S, RuleNames.PUBLIC_SLAVE_IP_L,
                "Slave instances also get an public ip address"));
        groupReference.add(createStringRule(RuleNames.META_MODE_S, RuleNames.META_MODE_L,
                "Allows you to use a different cloud provider. Available providers are listed above"));
        // TODO: Durch Regex verschiedene Fälle überprüfen? Enum
        groupReference.add(createStringRule(RuleNames.LOCAL_FS_S, RuleNames.LOCAL_FS_L,
                "File system used for internal (empheral) diskspace. One of 'ext2', 'ext3', 'ext4' or 'xfs'. Default is 'xfs'."));
        groupReference.add(createStringRule(RuleNames.USER_S, RuleNames.USER_L,
                "User name (for VM tagging)"));
        groupReference.add(createStringRule(RuleNames.SSH_USER_S, RuleNames.SSH_USER_L,
                "SSH user name for master instance configuration"));
        groupReference.add(createStringRule(RuleNames.OPENSTACK_USERNAME_S, RuleNames.OPENSTACK_USERNAME_L,
                "The given Openstack Username"));
        groupReference.add(createStringRule(RuleNames.OPENSTACK_TENANT_NAME_S, RuleNames.OPENSTACK_TENANT_NAME_L,
                "The given Openstack Tenantname"));
        groupReference.add(createStringRule(RuleNames.OPENSTACK_PASSWORD_S, RuleNames.OPENSTACK_PASSWORD_L,
                "The given Openstack User-Password"));
        groupReference.add(createStringRule(RuleNames.OPENSTACK_ENDPOINT_S, RuleNames.OPENSTACK_ENDPOINT_L,
                "The given Openstack Endpoint (e.g. https://xxx.xxx.xxx.xxx:5000/v3/)")); //Regex?
        groupReference.add(createStringRule(RuleNames.OPENSTACK_DOMAIN_S, RuleNames.OPENSTACK_DOMAIN_L,
                "The given Openstack Domain"));
        groupReference.add(createStringRule(RuleNames.OPENSTACK_TENANT_DOMAIN_S, RuleNames.OPENSTACK_TENANT_DOMAIN_L,
                "The given Openstack Tenant Domain"));

        groupReference.add(createStringRule(RuleNames.GOOGLE_PROJECT_ID_S, RuleNames.GOOGLE_PROJECT_ID_L,
                "Google compute engine project id"));
        groupReference.add(createStringRule(RuleNames.GOOGLE_IMAGE_PROJECT_ID_S, RuleNames.GOOGLE_IMAGE_PROJECT_ID_L,
                "Google compute engine image project id"));
        groupReference.add(createStringRule(RuleNames.GOOGLE_CREDENTIALS_FILE_S, RuleNames.GOOGLE_CREDENTIALS_FILE_L,
                "Path to google compute engine service account credentials file"));
        groupReference.add(createStringRule(RuleNames.AZURE_CREDENTIALS_FILE_S, RuleNames.AZURE_CREDENTIALS_FILE_L,
                "Path to microsoft azure credentials file"));

        // Software rules
        groupReference.add(createBooleanRule(RuleNames.MESOS_S, RuleNames.MESOS_L,
                "[yes, no] if Mesos framework should be configured/started. Default is no"));
        groupReference.add(createBooleanRule(RuleNames.OPEN_GRID_ENGINE_S, RuleNames.OPEN_GRID_ENGINE_L,
                "[yes, no] if OpenGridEngine should be configured/started. Default is yes"));
        groupReference.add(createBooleanRule(RuleNames.NFS_S, RuleNames.NFS_L,
                "[yes, no] if NFS should be configured/started. Default is yes"));
        groupReference.add(createBooleanRule(RuleNames.HDFS_S, RuleNames.HDFS_L,
                "[yes, no] if HDFS should be configured/started. Default is no"));
        groupReference.add(createBooleanRule(RuleNames.CASSANDRA_S, RuleNames.CASSANDRA_L,
                "[yes, no] if Cassandra database should be configured/started. Default is no"));
        groupReference.add(createBooleanRule(RuleNames.SPARK_S, RuleNames.SPARK_L,
                "[yes, no] if Spark cluster support should be configured/started. Default is no"));
        groupReference.add(createBooleanRule(RuleNames.CLOUD9_S, RuleNames.CLOUD9_L,
                "[yes, no] if Cloud9 IDE should be configured/started. Default is no"));
    }

    private Tparam createBasicRule(RuleNames shortFlag, RuleNames longFlag, Tprimitive type, String shortDescription) {
        Tparam tp = new Tparam();
        tp.setId(shortFlag.toString()); // shortFlag for short commands
        tp.setOption(longFlag.toString()); // longFlag resp. option represent the long command
        Tparam.ShortDescription desc = new Tparam.ShortDescription();
        desc.setValue(shortDescription);
        tp.getShortDescription().add(desc);
        tp.setType(type);
        return tp;
    }

    private Tparam createStringRule(RuleNames shortFlag, RuleNames longFlag, String shortDescription) {
        Tparam tp = createBasicRule(shortFlag, longFlag, Tprimitive.STRING, shortDescription);
        // if a string is needed it must not be empty
        tp.setMinLength(1);
        return tp;
    }

    private Tparam createBooleanRule(RuleNames shortFlag, RuleNames longFlag, String shortDescription) {
        return createBasicRule(shortFlag, longFlag, Tprimitive.BOOLEAN, shortDescription);
    }

    private Tparam createIntRule(RuleNames shortFlag, RuleNames longFlag, String shortDescription) {
        return createBasicRule(shortFlag, longFlag, Tprimitive.INT, shortDescription);
    }

    private Tparam createIntRule(RuleNames shortFlag, RuleNames longFlag, String shortDescription, int min) {
        Tparam tp = createBasicRule(shortFlag, longFlag, Tprimitive.INT, shortDescription);
        Tparam.Min minValue = new Tparam.Min();
        minValue.setValue(min);
        tp.setMin(minValue);
        return tp;
    }

    public TparamGroup getRules() {
        return group;
    }

    public enum RuleNames {
        MASTER_INSTANCE_TYPE_S("m"),
        MASTER_INSTANCE_TYPE_L("master-instance-type"),
        MAX_MASTER_EPHEMERALS_S("mme"),
        MAX_MASTER_EPHEMERALS_L("max-master-ephemerals"),
        MASTER_IMAGE_S("M"),
        MASTER_IMAGE_L("master-image"),
        SLAVE_INSTANCE_TYPE_S("s"),
        SLAVE_INSTANCE_TYPE_L("slave-instance-type"),
        MAX_SLAVE_EPHEMERALS_S("mse"),
        MAX_SLAVE_EPHEMERALS_L("max-slave-ephemerals"),
        SLAVE_INSTANCE_COUNT_S("n"),
        SLAVE_INSTANCE_COUNT_L("slave-instance-count"),
        SLAVE_IMAGE_S("S"),
        SLAVE_IMAGE_L("slave-image"),
        USE_SPOT_INSTANCE_REQUEST_S("usir"),
        USE_SPOT_INSTANCE_REQUEST_L("use-spot-instance-request"),
        BID_PRICE_S("bp"),
        BID_PRICE_L("bidprice"),
        BID_PRICE_MASTER_S("bpm"),
        BID_PRICE_MASTER_L("bidprice-master"),
        KEYPAIR_S("k"),
        KEYPAIR_L("keypair"),
        SSH_PUBLIC_KEY_FILE_S("spu"),
        SSH_PUBLIC_KEY_FILE_L("ssh-public-key-file"),
        SSH_PRIVATE_KEY_FILE_S("spr"),
        SSH_PRIVATE_KEY_FILE_L("ssh-private-key-file"),
        REGION_S("e"),
        REGION_L("region"),
        REGION_ENV("OS_REGION_NAME"),
        AVAILABILITY_ZONE_S("z"),
        AVAILABILITY_ZONE_L("availability-zone"),
        PORTS_S("p"),
        PORTS_L("ports"),
        SECURITY_GROUP_S("sg"),
        SECURITY_GROUP_L("security-group"),
        MASTER_MOUNTS_S("d"),
        MASTER_MOUNTS_L("master-mounts"),
        NFS_SHARES_S("g"),
        NFS_SHARES_L("nfs-shares"),
        EXT_NFS_SHARES_S("ge"),
        EXT_NFS_SHARES_L("ext-nfs-shares"),
        VERBOSE_S("v"),
        VERBOSE_L("verbose"),
        HELP_LIST_INSTANCE_TYPES_S("lit"),
        HELP_LIST_INSTANCE_TYPES_L("list-instance-types"),
        CONFIG_S("o"),
        CONFIG_L("config"),
        USE_MASTER_AS_COMPUTE_S("b"),
        USE_MASTER_AS_COMPUTE_L("use-master-as-compute"),
        USE_MASTER_WITH_PUBLIC_IP_S("pub"),
        USE_MASTER_WITH_PUBLIC_IP_L("use-master-with-public-ip"),
        HDFS_S("hdfs"),
        HDFS_L("hdfs"),
        CASSANDRA_S("db"),
        CASSANDRA_L("cassandra"),
        SPARK_S("spark"),
        SPARK_L("spark"),
        GRID_PROPERTIES_FILE_S("gpf"),
        GRID_PROPERTIES_FILE_L("grid-properties-file"),
        ROUTER_S("router"),
        ROUTER_L("router"),
        NETWORK_S("network"),
        NETWORK_L("network"),
        SUBNET_S("subnet"),
        SUBNET_L("subnet"),
        PUBLIC_SLAVE_IP_S("psi"),
        PUBLIC_SLAVE_IP_L("public-slave-ip"),
        MESOS_S("me"),
        MESOS_L("mesos"),
        META_MODE_S("mode"),
        META_MODE_L("meta-mode"),
        OPEN_GRID_ENGINE_S("oge"),
        OPEN_GRID_ENGINE_L("oge"),
        NFS_S("nfs"),
        NFS_L("nfs"),
        LOCAL_FS_S("lfs"),
        LOCAL_FS_L("local-fs"),
        USER_S("u"),
        USER_L("user"),
        SSH_USER_S("su"),
        SSH_USER_L("ssh-user"),
        DEBUG_REQUESTS_S("dr"),
        DEBUG_REQUESTS_L("debug-requests"),
        CLOUD9_S("c9"),
        CLOUD9_L("cloud9"),
        // Amazon AWS
        AWS_CREDENTIALS_FILE_S("a"),
        AWS_CREDENTIALS_FILE_L("aws-credentials-file"),
        // Microsoft Azure
        AZURE_CREDENTIALS_FILE_S("acf"),
        AZURE_CREDENTIALS_FILE_L("azure-credentials-file"),
        // Google Cloud
        GOOGLE_PROJECT_ID_S("gpid"),
        GOOGLE_PROJECT_ID_L("google-projectid"),
        GOOGLE_IMAGE_PROJECT_ID_S("gipid"),
        GOOGLE_IMAGE_PROJECT_ID_L("google-image-projectid"),
        GOOGLE_CREDENTIALS_FILE_S("gcf"),
        GOOGLE_CREDENTIALS_FILE_L("google-credentials-file"),
        // OpenStack
        OPENSTACK_USERNAME_S("osu"),
        OPENSTACK_USERNAME_L("openstack-username"),
        OPENSTACK_USERNAME_ENV("OS_USERNAME"),
        OPENSTACK_TENANT_NAME_S("ost"),
        OPENSTACK_TENANT_NAME_L("openstack-tenantname"),
        OPENSTACK_TENANT_NAME_ENV("OS_PROJECT_NAME"),
        OPENSTACK_PASSWORD_S("osp"),
        OPENSTACK_PASSWORD_L("openstack-password"),
        OPENSTACK_PASSWORD_ENV("OS_PASSWORD"),
        OPENSTACK_ENDPOINT_S("ose"),
        OPENSTACK_ENDPOINT_L("openstack-endpoint"),
        OPENSTACK_ENDPOINT_ENV("OS_AUTH_URL"),
        OPENSTACK_DOMAIN_S("osd"),
        OPENSTACK_DOMAIN_L("openstack-domain"),
        OPENSTACK_DOMAIN_ENV("OS_USER_DOMAIN_NAME"),
        OPENSTACK_TENANT_DOMAIN_S("ostd"),
        OPENSTACK_TENANT_DOMAIN_L("openstack-tenantdomain");

        private final String value;

        RuleNames(String value) {
            this.value = value;
        }

        public boolean equals(String other) {
            return value.equals(other);
        }

        @Override
        public String toString() {
            return this.value;
        }
    }
}