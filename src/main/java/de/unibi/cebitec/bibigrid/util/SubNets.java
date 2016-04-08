package de.unibi.cebitec.bibigrid.util;

import com.amazonaws.services.ec2.model.Subnet;
import java.util.Collection;

/**
 * Class Subnet, provides  a function that returns next subnet
 * 
 * 
 * 
 * @author jkrueger
 */
public class SubNets {
    
    private final long ip;  
    private final long networksize;  
    private final long subnetsize;           
    private final long networkmask;   
    private final long subnetbase;
    private long current = 0;
    
    private final long number_of_subnets;
            
    
    
    public SubNets(String vpccidr, long subnetsize){
        long [] tmp = parseCidr(vpccidr);
        ip = tmp[0];
        networksize = tmp[1];
        networkmask = tmp[2];
        this.subnetsize = subnetsize;
        subnetbase = ip & networkmask;
        number_of_subnets = Math.round((Math.pow(2, subnetsize-networksize)-1));
        if (networksize >= subnetsize) {
            throw new RuntimeException("Network size is smaller than Subnet size");
        }
    }
    
    
    /**
     * Return next subnet address or -1 if there isn't one left.
     * 
     * @return 
     */
    public long next(){
        if (current > number_of_subnets) {
            return -1;
        } 
        return subnetbase + ((current++)<<(subnetsize-networksize));
    }
    
    /**
     * Return next free subnet in 
     * 
     * @return 
     */
    public String nextCidr(){
        long lip = next();
        if (lip == -1) {
            return null;
        } else {
            return ((lip & 0xFF000000l) >> 24)+"."+((lip&0xFF0000l) >> 16) +"."+((lip&0xFF00l) >> 8) +"."+(lip&0xFFl)+"/"+subnetsize;
        }
    }
    
    /**
     * Return next free Cidr that is not part of the given Collection of Cidr
     * 
     * @param coc
     * @return 
     */
    public String nextCidr(Collection<String> coc){       
        String nextcidr;  
        while((nextcidr = nextCidr()) != null) {
            if (!coc.contains(nextcidr)) {
                return nextcidr;
            } 
        }
        
        return null;
    }
    
    
    /**
     * Parse the given CIDR and return an array of long values
     * {ip,network size,network mask}
     * 
     * @param cidr
     * @return 
     */
    public static long [] parseCidr(String cidr){
        String [] vpccidr = cidr.split("/");
        if (vpccidr.length != 2) {
            throw new RuntimeException("IP has invalid format - d.d.d.d/m");
        }
        String [] ips = vpccidr[0].split("\\.");
        if (ips.length != 4) {
            throw new RuntimeException("IP has invalid format - d.d.d.d/m");
        }
        long ip = (Long.parseLong(ips[0]) << 24) + (Long.parseLong(ips[1]) << 16) + (Long.parseLong(ips[2]) << 8) + Long.parseLong(ips[3]);      
        long size = Integer.parseInt(vpccidr[1]);
        long mask = createMask(size);
        
        return new long [] {ip,size,mask};
    }
    
    
    
    /**
     * Return first usable IP address from CIDR block
     * 
     * @param cidr
     * @return 
     */
     public static String getFirstIP(String cidr){
        long [] tmp = parseCidr(cidr);
        long number_of_ips = Math.round((Math.pow(2, 32-tmp[2])-1));
        return longAsIPV4String(tmp[0]+5);
    }
    
    
    
    /**
     * Return a networkmask on base of given network size.
     * 
     * @param m - networksize 
     * @return 
     */
    public static long createMask(long m){
        return  Math.round((Math.pow(2, m)-1)) << (32-m) ;
    }
    
    /**
     * Return a long value as IPV4 String in form XX.XX.XX.XX
     * 
     * @param l
     * @return 
     */
    public static String longAsIPV4String(long l) {
        return ((l & 0xFF000000l) >> 24)+"."+((l&0xFF0000l) >> 16)+"."+((l&0xFF00l) >> 8)+"."+(l&0xFFl);
    }
    
    public static void printLongAsIPV4(long l){
        System.out.printf(" %d --> %s --> %s --> %s.%s.%s.%s %n",l,Long.toBinaryString(l),Long.toHexString(l),((l & 0xFF000000l) >> 24),((l&0xFF0000l) >> 16) ,((l&0xFF00l) >> 8) ,(l&0xFFl));
    }
    
    
    
}
