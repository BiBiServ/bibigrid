package de.unibi.cebitec.bibigrid.model;

/**
 * Simple definition of an network. Used to build security rules.
 * 
 * 
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public class Port {
    
    public enum TPort {TCP,UDP,ICMP};
  
    public String iprange;   
    public int number;
    public TPort type;
   
    
    public Port(String iprange, int number){
        this.iprange = iprange;
        this.number = number;
        type = TPort.TCP;
    }
    
    public Port(String iprange, int number, TPort type){
        this.iprange = iprange;
        this.number = number;
        this.type = type;
    }
    
}
