package bibigrid_gui;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;

import de.unibi.cebitec.bibigrid.StartUp;
import de.unibi.cebitec.bibigrid.util.RuleBuilder;
import de.unibi.techfak.bibiserv.cms.TparamGroup;
import de.unibi.techfak.bibiserv.cms.Tprimitive;
import org.springframework.web.bind.annotation.*;

import de.unibi.techfak.bibiserv.cms.Tparam;

@RestController
public class Controller {

    String retMessage = "Start";

    @CrossOrigin(origins = "http://localhost:3000")
    @RequestMapping("/resource")
    public Map<String,Object> home(@RequestParam(value="param", defaultValue="placeholder") String param) {

        Map<String,Object> model = new HashMap<String,Object>();
        model.put("data",dispRules());
        return model;
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @RequestMapping("/answer")
    public Map<String,Object> answer() {

        Map<String,Object> model = new HashMap<String,Object>();
        String[] tmp = new String[1];
        tmp[0] = retMessage;
        model.put("data",tmp);

        return model;
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @RequestMapping(
            value = "/process",
            method = RequestMethod.POST)
    public void process(@RequestBody Map<String,Object> payload)
            throws Exception {

        List<LinkedHashMap> resJson = (List<LinkedHashMap>) payload.get("data");
        List<String> tmpArgList = new ArrayList<String>();

        for (LinkedHashMap entry : resJson ) {
            String tmp = "-"+entry.get("sFlag");
            tmpArgList.add(tmp.trim());
            tmp = ""+entry.get("sDescription");
            if(!tmp.equals("")){
                tmpArgList.add(tmp.trim());
            }
        }

        String[] arguments = new String[tmpArgList.size()];
        arguments = tmpArgList.toArray(arguments);


        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        System.setOut(ps);
        System.setErr(ps);

        StartUp testUp = new StartUp();
        testUp.main(arguments);

        retMessage = new String(baos.toByteArray());
        answer();
    }

    public MenuEntry[] dispRules(){

        RuleBuilder builder = new RuleBuilder();
        TparamGroup group = builder.getAllRules();
        List<MenuEntry> menuList = new ArrayList<MenuEntry>();

        for (Object ob : group.getParamrefOrParamGroupref()){

            Tparam tp = (Tparam) ob;

            String sFlag = tp.getId();
            String lFlag = tp.getOption();
            String sDescription = tp.getShortDescription().get(0).getValue();
            Tprimitive type = tp.getType();
            String guiGroup = tp.getGuiElement();

            menuList.add(new MenuEntry(sFlag, lFlag, sDescription, type, guiGroup));
        }

        MenuEntry[] tmp = new MenuEntry[menuList.size()];
        tmp = menuList.toArray(tmp);
        return tmp;
    }


}
