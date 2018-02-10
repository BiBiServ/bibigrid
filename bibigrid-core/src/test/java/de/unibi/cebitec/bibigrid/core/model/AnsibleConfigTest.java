package de.unibi.cebitec.bibigrid.core.model;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class AnsibleConfigTest {
    private class TestConfiguration extends Configuration {
        TestConfiguration() {
            setUser("ubuntu");
            setMasterInstance(new InstanceConfiguration() {
                @Override
                public InstanceType getProviderType() {
                    return new InstanceType() {
                        @Override
                        public String getValue() {
                            return "de.NBI.Small";
                        }

                        @Override
                        public int getInstanceCores() {
                            return 2;
                        }
                    };
                }
            });
            List<SlaveInstanceConfiguration> slaveInstances = new ArrayList<>();
            slaveInstances.add(new SlaveInstanceConfiguration() {
                @Override
                public int getCount() {
                    return 1;
                }

                @Override
                public InstanceType getProviderType() {
                    return new InstanceType() {
                        @Override
                        public String getValue() {
                            return "de.NBI.Large";
                        }

                        @Override
                        public int getInstanceCores() {
                            return 2;
                        }

                        @Override
                        public int getEphemerals() {return 1; }
                    };
                }
            });
            setSlaveInstances(slaveInstances);
        }
    }

    @Test
    public void testToString() {
        AnsibleConfig config = new AnsibleConfig(new TestConfiguration(),"/dev/vd");
        config.setMasterIpHostname("192.168.33.10", "master");
        config.addSlaveIpHostname("192.168.33.11", "client1");
        config.setSubnetCidr("192.168.33.0/24");
        System.out.println(config);
    }
}
