package de.unibi.cebitec.bibigrid.core.model;

import org.junit.Test;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class AnsibleConfigTest {
    private class TestConfiguration extends Configuration {
        TestConfiguration() {
            setUser("ubuntu");
            setMasterInstanceType(new InstanceType() {
                @Override
                public InstanceSpecification getSpec() {
                    return new InstanceSpecification(2, 0, false, false, true, true);
                }
            });
            setSlaveInstanceType(new InstanceType() {
                @Override
                public InstanceSpecification getSpec() {
                    return new InstanceSpecification(2, 0, false, false, true, true);
                }
            });
        }
    }

    @Test
    public void testToString() {
        AnsibleConfig config = new AnsibleConfig(new TestConfiguration());
        config.setMasterIpHostname("192.168.33.10", "master");
        config.addSlaveIpHostname("192.168.33.11", "client1");
        config.setSubnetCidr("192.168.33.0/24");
        System.out.println(config);
    }
}
/*nfs_mounts:
  - src: "/vol/spool"
    dst: "/vol/spool"
  - src: "/opt"
    dst: "/opt"
*/