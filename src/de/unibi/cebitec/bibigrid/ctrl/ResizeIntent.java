/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.ctrl;

import de.unibi.cebitec.bibigrid.exc.IntentNotConfiguredException;
import static de.unibi.cebitec.bibigrid.util.VerboseOutputFilter.V;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author chenke
 */
public class ResizeIntent extends Intent {

    public static final Logger log = LoggerFactory.getLogger(ResizeIntent.class);

    @Override
    public String getCmdLineOption() {
        return "r";
    }

    @Override
    public List<String> getRequiredOptions() {
        return Arrays.asList(new String[]{"s", "S", "n", "k", "i", "e", "a", "z"});
        //TODO: add new options for volumes etc.
    }

    @Override
    public boolean execute() throws IntentNotConfiguredException {
        if (getConfiguration() == null) {
            throw new IntentNotConfiguredException();
        }
        log.error("Sorry, resizing has not been implemented yet.");
        return true;
    }
}
