/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.test;

import java.net.InetAddress;

/**
 *
 * @author jkrueger
 */
public class TestIPV4 {
    
    public static void main (String [] args) throws Exception{
        System.out.printf("IPV4 %s%n",InetAddress.getLocalHost().getHostAddress());
    }
    
}
