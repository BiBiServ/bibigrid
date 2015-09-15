package de.unibi.cebitec.bibigrid.ctrl;

import com.amazonaws.services.ec2.AmazonEC2;
import de.unibi.cebitec.bibigrid.exc.IntentNotConfiguredException;
import de.unibi.cebitec.bibigrid.meta.aws.ValidateIntentAWS;
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
    public List<String> getRequiredOptions() {
        return Arrays.asList(new String[]{"ch", "m", "M", "s", "S", "n", "u", "k", "i", "e", "a", "z", "g", "r", "b"});
    }

    @Override
    public boolean execute() throws IntentNotConfiguredException {
        if (getConfiguration() == null) {
            throw new IntentNotConfiguredException();
        }

        switch (getConfiguration().getMetaMode()) {
            case "aws-ec2":
            case "default":
                return new ValidateIntentAWS(getConfiguration()).validate();
            case "openstack":
                return false;
            default:
                log.error("Malformed meta-mode! [use: 'aws-ec2','openstack' or leave it blanc.");
                return false;
        }

    }

}
