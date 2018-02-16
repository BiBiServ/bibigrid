package de.unibi.cebitec.bibigrid.core.model;

import de.unibi.cebitec.bibigrid.core.util.AnsibleResources;
import org.junit.Assert;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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
                        public int getCpuCores() {
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
                        public int getCpuCores() {
                            return 2;
                        }

                        @Override
                        public int getEphemerals() {
                            return 1;
                        }
                    };
                }
            });
            slaveInstances.add(new SlaveInstanceConfiguration() {
                @Override
                public int getCount() {
                    return 2;
                }

                @Override
                public InstanceType getProviderType() {
                    return new InstanceType() {
                        @Override
                        public String getValue() {
                            return "de.NBI.Large";
                        }

                        @Override
                        public int getCpuCores() {
                            return 2;
                        }

                        @Override
                        public int getEphemerals() {
                            return 4;
                        }
                    };
                }
            });
            setSlaveInstances(slaveInstances);
        }
    }

    private class TestInstance extends Instance {
        TestInstance(Configuration.InstanceConfiguration instanceConfiguration) {
            super(instanceConfiguration);
        }

        @Override
        public String getPublicIp() {
            return null;
        }

        @Override
        public String getPrivateIp() {
            return null;
        }

        @Override
        public String getHostname() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getTag(String key) {
            return null;
        }

        @Override
        public ZonedDateTime getCreationTimestamp() {
            return null;
        }
    }

    @Test
    public void testToString() {
        TestConfiguration testConfiguration = new TestConfiguration();
        TestInstance masterInstance = new TestInstance(testConfiguration.getMasterInstance());
        List<Instance> slaveInstances = Arrays.asList(
                new TestInstance(testConfiguration.getSlaveInstances().get(0)),
                new TestInstance(testConfiguration.getSlaveInstances().get(1)),
                new TestInstance(testConfiguration.getSlaveInstances().get(1)));
        AnsibleConfig config = new AnsibleConfig(testConfiguration, "/dev/vd", "192.168.33.0/24", masterInstance,
                slaveInstances);
        Assert.assertArrayEquals(new String[]{
                AnsibleResources.CONFIG_ROOT_PATH + "slave-1.yml",
                AnsibleResources.CONFIG_ROOT_PATH + "slave-2.yml",
                AnsibleResources.CONFIG_ROOT_PATH + "slave-3.yml"
        }, config.getSlaveFilenames());
    }
}
