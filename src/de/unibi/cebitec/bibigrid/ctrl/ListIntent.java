package de.unibi.cebitec.bibigrid.ctrl;

import de.unibi.cebitec.bibigrid.exc.IntentNotConfiguredException;
import de.unibi.cebitec.bibigrid.meta.aws.ListIntentAWS;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListIntent extends Intent {

    public static final Logger log = LoggerFactory.getLogger(ListIntent.class);

    @Override
    public String getCmdLineOption() {
        return "l";
    }

    @Override
    public List<String> getRequiredOptions() {
        return Arrays.asList(new String[]{"l", "k", "e", "a"});
    }

    @Override
    public boolean execute() throws IntentNotConfiguredException {
        if (getConfiguration() == null) {
            throw new IntentNotConfiguredException();
        }

        switch (getConfiguration().getMetaMode()) {
            case "aws-ec2":
            case "default":
                return new ListIntentAWS(getConfiguration()).list();
            case "openstack":
                return false;
            default:
                log.error("Malformed meta-mode! [use: 'aws-ec2','openstack' or leave it blanc.");
                return false;
        }
    }
}
