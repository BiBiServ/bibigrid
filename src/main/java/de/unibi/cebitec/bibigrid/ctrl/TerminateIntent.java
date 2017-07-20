package de.unibi.cebitec.bibigrid.ctrl;

import de.unibi.cebitec.bibigrid.exception.IntentNotConfiguredException;
import de.unibi.cebitec.bibigrid.meta.aws.TerminateIntentAWS;
import de.unibi.cebitec.bibigrid.meta.googlecloud.TerminateIntentGoogleCloud;
import de.unibi.cebitec.bibigrid.meta.openstack.TerminateIntentOpenstack;
import de.unibi.cebitec.bibigrid.model.Configuration.MODE;
import java.util.Arrays;
import java.util.List;
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
                return Arrays.asList("t", "e", "a");
            case OPENSTACK:
                return Arrays.asList("t", "e", "osu", "ost", "osp", "ose");
            case GOOGLECLOUD:
                return Arrays.asList("t", "e", "gcf", "gpid");
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
                log.error("Malformed meta-mode! [use: 'aws-ec2','openstack','googlecloud' or leave it blanc.");
                return false;
        }
    }

}
