package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.CommandLineValidator;
import de.unibi.cebitec.bibigrid.model.IntentMode;
import de.unibi.cebitec.bibigrid.model.OpenStackCredentials;
import de.unibi.cebitec.bibigrid.model.ProviderModule;
import de.unibi.cebitec.bibigrid.util.DefaultPropertiesFile;
import de.unibi.cebitec.bibigrid.util.RuleBuilder;
import org.apache.commons.cli.CommandLine;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public final class CommandLineValidatorOpenstack extends CommandLineValidator {
    CommandLineValidatorOpenstack(final CommandLine cl, final DefaultPropertiesFile defaultPropertiesFile,
                                  final IntentMode intentMode, final ProviderModule providerModule) {
        super(cl, defaultPropertiesFile, intentMode, providerModule);
    }

    @Override
    protected List<String> getRequiredOptions() {
        switch (intentMode) {
            case LIST:
                return Arrays.asList(
                        RuleBuilder.RuleNames.KEYPAIR_S.toString(),
                        RuleBuilder.RuleNames.REGION_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_USERNAME_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_TENANT_NAME_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_PASSWORD_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_ENDPOINT_S.toString());
            case TERMINATE:
                return Arrays.asList(
                        "t",
                        RuleBuilder.RuleNames.REGION_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_TENANT_NAME_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_USERNAME_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_PASSWORD_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_ENDPOINT_S.toString());
            case CREATE:
                return Arrays.asList(
                        RuleBuilder.RuleNames.MASTER_INSTANCE_TYPE_S.toString(),
                        RuleBuilder.RuleNames.MASTER_IMAGE_S.toString(),
                        RuleBuilder.RuleNames.SLAVE_INSTANCE_TYPE_S.toString(),
                        RuleBuilder.RuleNames.SLAVE_IMAGE_S.toString(),
                        RuleBuilder.RuleNames.SLAVE_INSTANCE_COUNT_S.toString(),
                        RuleBuilder.RuleNames.KEYPAIR_S.toString(),
                        RuleBuilder.RuleNames.IDENTITY_FILE_S.toString(),
                        RuleBuilder.RuleNames.REGION_S.toString(),
                        RuleBuilder.RuleNames.AVAILABILITY_ZONE_S.toString(),
                        RuleBuilder.RuleNames.NFS_SHARES_S.toString(),
                        RuleBuilder.RuleNames.USE_MASTER_AS_COMPUTE_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_TENANT_NAME_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_USERNAME_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_PASSWORD_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_ENDPOINT_S.toString());
            case VALIDATE:
                return null;
        }
        return null;
    }

    @Override
    protected boolean validateProviderParameters(List<String> req, Properties defaults) {
        OpenStackCredentials osc = new OpenStackCredentials();
        // OpenStack username
        ParseResult result = parseParameter(defaults, RuleBuilder.RuleNames.OPENSTACK_USERNAME_S,
                RuleBuilder.RuleNames.OPENSTACK_USERNAME_L, RuleBuilder.RuleNames.OPENSTACK_USERNAME_ENV);
        if (!result.success) {
            LOG.error("No suitable entry for OpenStack-Username (osu) found nor environment OS_USERNAME set! Exit");
            return false;
        }
        osc.setUsername(result.value);
        // OpenStack tenant name
        result = parseParameter(defaults, RuleBuilder.RuleNames.OPENSTACK_TENANT_NAME_S,
                RuleBuilder.RuleNames.OPENSTACK_TENANT_NAME_L, RuleBuilder.RuleNames.OPENSTACK_TENANT_NAME_ENV);
        if (!result.success) {
            LOG.error("No suitable entry for OpenStack-Tenantname (ost) found nor environment OS_PROJECT_NAME set! Exit");
            return false;
        }
        osc.setTenantDomain(result.value);
        // OpenStack tenant domain
        result = parseParameter(defaults, RuleBuilder.RuleNames.OPENSTACK_TENANT_DOMAIN_S,
                RuleBuilder.RuleNames.OPENSTACK_TENANT_DOMAIN_L, null);
        if (!result.success) {
            LOG.info("No suitable entry for OpenStack-TenantDomain (ostd) found! Use OpenStack-Domain(osd) instead!");
            return false;
        }
        osc.setTenantDomain(result.value);
        // OpenStack password
        result = parseParameter(defaults, RuleBuilder.RuleNames.OPENSTACK_PASSWORD_S,
                RuleBuilder.RuleNames.OPENSTACK_PASSWORD_L, RuleBuilder.RuleNames.OPENSTACK_PASSWORD_ENV);
        if (!result.success) {
            LOG.error("No suitable entry for OpenStack-Password (osp) found nor environment OS_PASSWORD set! Exit");
            return false;
        }
        osc.setPassword(result.value);
        // OpenStack endpoint
        result = parseParameter(defaults, RuleBuilder.RuleNames.OPENSTACK_ENDPOINT_S,
                RuleBuilder.RuleNames.OPENSTACK_ENDPOINT_L, RuleBuilder.RuleNames.OPENSTACK_ENDPOINT_ENV);
        if (!result.success) {
            LOG.error("No suitable entry for OpenStack-Endpoint (ose) found nor environment OS_AUTH_URL set! Exit");
            return false;
        }
        osc.setEndpoint(result.value);
        // OpenStack domain
        result = parseParameter(defaults, RuleBuilder.RuleNames.OPENSTACK_DOMAIN_S,
                RuleBuilder.RuleNames.OPENSTACK_DOMAIN_L, RuleBuilder.RuleNames.OPENSTACK_DOMAIN_ENV);
        if (result.success) {
            osc.setDomain(result.value);
        } else {
            LOG.info("Keystone V2 API.");
        }
        this.cfg.setOpenstackCredentials(osc);
        return true;
    }
}
