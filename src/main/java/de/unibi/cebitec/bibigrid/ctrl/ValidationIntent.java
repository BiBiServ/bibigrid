package de.unibi.cebitec.bibigrid.ctrl;

import com.amazonaws.services.ec2.AmazonEC2;
import de.unibi.cebitec.bibigrid.exception.IntentNotConfiguredException;
import de.unibi.cebitec.bibigrid.meta.aws.ValidateIntentAWS;
import de.unibi.cebitec.bibigrid.meta.googlecloud.ValidateIntentGoogleCloud;
import de.unibi.cebitec.bibigrid.model.Configuration.MODE;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author alueckne
 */
public class ValidationIntent extends Intent {

    private AmazonEC2 ec2;
    public static final Logger log = LoggerFactory.getLogger(ValidationIntent.class);

    @Override
    public String getCmdLineOption() {
        return "check";
    }

    @Override
    public List<String> getRequiredOptions(MODE mode) {
        switch (mode) {
            case AWS:
                return Arrays.asList(new String[]{"ch", "m", "M", "s", "S", "n", "u", "k", "i", "e", "a", "z", "g", "r", "b"});
            case OPENSTACK:
                return Arrays.asList(new String[]{"ch", "m", "M", "s", "S", "n", "u", "k", "i", "e", "z", "g", "r", "b", "ost", "osu", "osp", "ose"});
            case GOOGLECLOUD:
                return Arrays.asList("ch", "M", "S", "gcf", "gpid");
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
                return new ValidateIntentAWS(getConfiguration()).validate();
            case OPENSTACK:
                return false;
            case GOOGLECLOUD:
                return new ValidateIntentGoogleCloud(getConfiguration()).validate();
            default:
                log.error("Malformed meta-mode! [use: 'aws-ec2','openstack','googlecloud' or leave it blanc.");
                return false;
        }

    }

}
