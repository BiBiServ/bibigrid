package de.unibi.cebitec.bibigrid.ctrl;

import de.unibi.cebitec.bibigrid.exception.IntentNotConfiguredException;
import de.unibi.cebitec.bibigrid.meta.aws.ListIntentAWS;
import de.unibi.cebitec.bibigrid.meta.openstack.ListIntentOpenstack;
import de.unibi.cebitec.bibigrid.model.Configuration.MODE;
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
    public List<String> getRequiredOptions(MODE mode) {
        switch (mode) {
            case AWS:
                return Arrays.asList(new String[]{"l", "k", "e", "a"});
            case OPENSTACK:
                return Arrays.asList(new String[]{"l", "k", "e", "osu", "ost", "osp", "ose"});
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
                return new ListIntentAWS(getConfiguration()).list();
            case OPENSTACK:
                return new ListIntentOpenstack(getConfiguration()).list();
            default:
                log.error("Malformed meta-mode! [use: 'aws-ec2','openstack' or leave it blanc.");
                return false;
        }
    }
}
