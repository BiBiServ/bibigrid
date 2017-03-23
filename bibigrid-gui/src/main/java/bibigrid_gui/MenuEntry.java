package bibigrid_gui;

import de.unibi.techfak.bibiserv.cms.Tprimitive;

/**
 * Created by benedikt on 30.11.16.
 */
public class MenuEntry {
    private final String sFlag;
    private final String lFlag;
    private final String sDescription;
    private final String type;
    private final String guiGroup;

    public MenuEntry(String sFlag, String lFlag, String sDescription, Tprimitive type, String guiGroup) {

        this.sFlag = sFlag;
        this.lFlag = lFlag;
        this.sDescription = sDescription;
        this.guiGroup = guiGroup;

        if (type == null) {
            this.type = "NONE";
        } else {
            this.type = type.value();
        }

    }


    public String getType() {
        return type;
    }

    public String getsDescription() {
        return sDescription;
    }

    public String getlFlag() {
        return lFlag;
    }

    public String getsFlag() {
        return sFlag;
    }

    public String getGuiGroup() {
        return guiGroup;
    }
}
