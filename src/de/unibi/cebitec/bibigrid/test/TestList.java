/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.test;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribePlacementGroupsRequest;
import com.amazonaws.services.ec2.model.DescribePlacementGroupsResult;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.PlacementGroup;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Tag;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author jkrueger
 */
public class TestList {

    public static AWSCredentials getAWSCredentials() {
        File f = new File(System.getProperty("user.home") + System.getProperty("file.separator") + ".bibigrid.properties");
        AWSCredentials cred = null;
        try {
            if (f.isFile() && f.canRead()) {
                cred = new PropertiesCredentials(f);
            }

        } catch (IOException e) {

            throw new RuntimeException(System.getProperty("user.home") + System.getProperty("file.separator") + ".bibigrid.properties  not found!");
        }

        return cred;
    }

    
    private static final String REGION = "eu-west-1";
    
    public static void main(String [] args){
        AmazonEC2Client ec2 = new AmazonEC2Client(getAWSCredentials());
        ec2.setEndpoint("ec2." + REGION + ".amazonaws.com");
        
        
        // Instances
        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
        DescribeInstancesResult describeInstancesResult = ec2.describeInstances(describeInstancesRequest);
        List<Reservation> reservations = describeInstancesResult.getReservations();
        System.out.printf("Instance(s)%n%10s | %30s | %20s | %15s | %s%n","Id","Name","KeyName","Type","State");
        for (Reservation r : reservations){
            for (Instance i : r.getInstances()) {
               
               // if (getValueforName(i.getTags(), "bibigrid-id") != null) {
                    System.out.printf("%10s | %30s | %20s | %15s | %s%n",i.getInstanceId(), getValueforName(i.getTags(), "name"), i.getKeyName(), i.getInstanceType(),i.getState().getName());
                            
               // }
            }
        }
        
        // Security Groups
        DescribeSecurityGroupsRequest describeSecurityGroupsRequest = new DescribeSecurityGroupsRequest();
        DescribeSecurityGroupsResult describeSecurityGroupsResult = ec2.describeSecurityGroups(describeSecurityGroupsRequest);
        
        List<SecurityGroup>  groups= describeSecurityGroupsResult.getSecurityGroups();
        System.out.println("SecurityGroup(s)");
        for (SecurityGroup sg : groups) {
            System.out.printf("%10s | %30s | %30s %n",sg.getGroupId(),sg.getGroupName(),getValueforName(sg.getTags(), "bibigrid-id"));

        }
        
        
        // SubNets
        
        DescribeSubnetsRequest describeSubnetsRequest = new DescribeSubnetsRequest();
        DescribeSubnetsResult describeSubnetResult = ec2.describeSubnets(describeSubnetsRequest);
        
        
        List<Subnet> subnets = describeSubnetResult.getSubnets();
        
        System.out.printf("Subnet(s)%n%20s | %10s | %s%n","SubnetId","Cluster-Id","Name");
        for (Subnet s : subnets) {
            System.out.printf("%20s | %10s | %s%n",s.getSubnetId(),getValueforName(s.getTags(),"bibigrid-id"),getValueforName(s.getTags(), "name"));
            
            
        }
        
        
        //Placementgroups
        DescribePlacementGroupsRequest describePlacementGroupsRequest = new DescribePlacementGroupsRequest();
        
        DescribePlacementGroupsResult describePlacementGroupsResult = ec2.describePlacementGroups(describePlacementGroupsRequest);
        
        List <PlacementGroup> placementgroups = describePlacementGroupsResult.getPlacementGroups();
        System.out.println("PlacementGroup(s):");
        for (PlacementGroup pg : placementgroups) {
                System.out.printf("%10s%n",pg.getGroupName());
        }
        
        
        
    }
    
    
    
    private static String getValueforName(List<Tag> tags, String name){
        for (Tag t : tags){
            if (t.getKey().equalsIgnoreCase(name)) {
                return t.getValue();
            }
        }
        return null;
    }
}
