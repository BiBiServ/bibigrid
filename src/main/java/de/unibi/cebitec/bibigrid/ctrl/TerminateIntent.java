package de.unibi.cebitec.bibigrid.ctrl;

import de.unibi.cebitec.bibigrid.exception.IntentNotConfiguredException;
import de.unibi.cebitec.bibigrid.meta.aws.TerminateIntentAWS;
import de.unibi.cebitec.bibigrid.meta.googlecloud.TerminateIntentGoogleCloud;
import de.unibi.cebitec.bibigrid.meta.openstack.TerminateIntentOpenstack;
import de.unibi.cebitec.bibigrid.model.Configuration.MODE;

import java.util.Arrays;
import java.util.List;

import de.unibi.cebitec.bibigrid.util.RuleBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TerminateIntent extends Intent {
    public static final Logger log = LoggerFactory.getLogger(TerminateIntent.class);

    @Override
    public String getCmdLineOption() {
        return "t";
    }

    @Override
    public List<String> getRequiredOptions(MODE mode) {
        switch (mode) {
            case AWS:
                return Arrays.asList(
                        getCmdLineOption(),
                        RuleBuilder.RuleNames.REGION_S.toString(),
                        RuleBuilder.RuleNames.AWS_CREDENTIALS_FILE_S.toString());
            case OPENSTACK:
                return Arrays.asList(
                        getCmdLineOption(),
                        RuleBuilder.RuleNames.REGION_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_TENANT_NAME_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_USERNAME_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_PASSWORD_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_ENDPOINT_S.toString());
            case GOOGLECLOUD:
                return Arrays.asList(
                        getCmdLineOption(),
                        RuleBuilder.RuleNames.REGION_S.toString(),
                        RuleBuilder.RuleNames.AVAILABILITY_ZONE_S.toString(),
                        RuleBuilder.RuleNames.GOOGLE_CREDENTIALS_FILE_S.toString(),
                        RuleBuilder.RuleNames.GOOGLE_PROJECT_ID_S.toString());
        }
        return null;
    }

    @Override
    public boolean execute() throws IntentNotConfiguredException {
        if (getConfiguration() == null) {
            throw new IntentNotConfiguredException();
        }
        switch (getConfiguration().getMode()) {
            case AWS:
                return new TerminateIntentAWS(getConfiguration()).terminate();
            case OPENSTACK:
                return new TerminateIntentOpenstack(getConfiguration()).terminate();
            case GOOGLECLOUD:
                return new TerminateIntentGoogleCloud(getConfiguration()).terminate();
            default:
                log.error("Malformed meta-mode! [use: 'aws-ec2','openstack','googlecloud' or leave it blank.");
                return false;
        }
    }
}
