package de.unibi.cebitec.bibigrid.core.model;

import org.junit.Test;

import java.io.OutputStream;
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
            setMasterAnsibleRoles(Arrays.asList("~/ansible/testrole1", "~/ansible/testrole2"));
            setSlaveAnsibleRoles(Arrays.asList("/etc/ansible/testrole3"));
        }
    }

    private class TestInstance extends Instance {
        private final String privateIp;

        TestInstance(Configuration.InstanceConfiguration instanceConfiguration, String privateIp) {
            super(instanceConfiguration);
            this.privateIp = privateIp;
        }

        @Override
        public String getId() {
            return null;
        }

        @Override
        public String getPublicIp() {
            return null;
        }

        @Override
        public String getPrivateIp() {
            return privateIp;
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

        @Override
        public String getKeyName() {
            return null;
        }
    }

    @Test
    public void testToString() {
        TestConfiguration testConfiguration = new TestConfiguration();
        TestInstance masterInstance = new TestInstance(testConfiguration.getMasterInstance(), "192.168.33.5");
        List<Instance> slaveInstances = Arrays.asList(
                new TestInstance(testConfiguration.getSlaveInstances().get(0), "192.168.33.6"),
                new TestInstance(testConfiguration.getSlaveInstances().get(1), "192.168.33.7"),
                new TestInstance(testConfiguration.getSlaveInstances().get(1), "192.168.33.8"));
        AnsibleConfig config = new AnsibleConfig(testConfiguration, "/dev/vd", "192.168.33.0/24", masterInstance,
                slaveInstances);

        System.out.println(":Site:");
        // print site configuration
        config.writeSiteFile(new OutputStream() {
            @Override
            public void write(int b) {
                System.out.write(b);
            }
        });
        System.out.println(":Master:");
        // print common configuration
        config.writeCommonFile(new OutputStream() {
            @Override
            public void write(int b) {
                System.out.write(b);
            }
        });
        // print slave specific configuration
        for (Instance slave : slaveInstances) {
            System.out.println(":Slave:");
            config.writeInstanceFile(slave, new OutputStream() {
                @Override
                public void write(int b) {
                    System.out.write(b);
                }
            });
        }
    }
}
