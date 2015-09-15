package de.unibi.cebitec.bibigrid.ctrl;

import de.unibi.cebitec.bibigrid.exc.IntentNotConfiguredException;
import de.unibi.cebitec.bibigrid.meta.aws.TerminateIntentAWS;
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
    public List<String> getRequiredOptions() {
        return Arrays.asList(new String[]{"t", "e", "a"});
    }

    @Override
    public boolean execute() throws IntentNotConfiguredException {
        if (getConfiguration() == null) {
            throw new IntentNotConfiguredException();
        }

        switch (getConfiguration().getMetaMode()) {
            case "aws-ec2":
            case "default":
                return new TerminateIntentAWS(getConfiguration()).terminate();
            case "openstack":
                return false;
            default:
                log.error("Malformed meta-mode! [use: 'aws-ec2','openstack' or leave it blanc.");
                return false;
        }
    }

//    private void sleep(int seconds) {
//        try {
//            Thread.sleep(seconds * 1000);
//        } catch (InterruptedException ie) {
//            log.error("Thread.sleep interrupted!");
//        }
//    }
//
//    private String join(String del, List<String> cs) {
//        if (cs.isEmpty()) {
//            return "";
//        }
//        if (cs.size() == 1) {
//            return cs.get(0);
//        }
//        StringBuilder sb = new StringBuilder(cs.get(0));
//        for (int i = 1; i < cs.size(); i++) {
//            sb.append(del);
//            sb.append(cs.get(i));
//        }
//        return sb.toString();
//    }

}
