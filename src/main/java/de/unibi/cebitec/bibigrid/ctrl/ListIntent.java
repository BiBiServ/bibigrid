package de.unibi.cebitec.bibigrid.ctrl;

import de.unibi.cebitec.bibigrid.exception.IntentNotConfiguredException;
import de.unibi.cebitec.bibigrid.meta.aws.ListIntentAWS;
import de.unibi.cebitec.bibigrid.meta.googlecloud.ListIntentGoogleCloud;
import de.unibi.cebitec.bibigrid.meta.openstack.ListIntentOpenstack;
import de.unibi.cebitec.bibigrid.model.Configuration.MODE;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * List all cluster for the used cluster environment.
 * 
 * All resources created by BiBiGrid follows a naming schema and are tagged. Bundles
 * all resources having the same cluster-id  and list them. 
 * 
 * @author Jan Krueger - jkrueger
 */
public class ListIntent extends Intent {

    public static final Logger log = LoggerFactory.getLogger(ListIntent.class);

    @Override
    public String getCmdLineOption() {
        return "l";
    }

    @Override
    public List<String> getRequiredOptions(MODE mode) {
        switch (mode) {
            case AWS:
                return Arrays.asList(new String[]{"l", "k", "e", "a"});
            case OPENSTACK:
                return Arrays.asList(new String[]{"l", "k", "e", "osu", "ost", "osp", "ose"});
            case GOOGLECLOUD:
                return Arrays.asList("l", "gcf", "gpid");
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
                log.info(new ListIntentAWS(getConfiguration()).toString());
                return true;
            case OPENSTACK:
                log.info(new ListIntentOpenstack(getConfiguration()).toString());
                return true;
            case GOOGLECLOUD:
                log.info(new ListIntentGoogleCloud(getConfiguration()).toString());
                return true;
            default:
                log.error("Malformed meta-mode! [use: 'aws-ec2','openstack','googlecloud' or leave it blanc.");
                return false;
        }
    }
}
