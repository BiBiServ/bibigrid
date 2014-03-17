/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.monitor;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.*;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import static de.unibi.cebitec.bibigrid.monitor.MonitoringState.*;
import de.unibi.cebitec.bibigrid.monitor.util.GridCommands;
import de.unibi.cebitec.bibigrid.util.InstanceInformation;
import static de.unibi.cebitec.bibigrid.monitor.MonitoringSettings.*;

/**
 *
 * @author alueckne
 */
public class MainMonitor {

    private AWSCredentials key;
    private final String HOME = System.getProperty("user.home");
    private final String DEFAULT_PROPERTIES_DIRNAME = HOME + "/.monitor";
    public final Logger log = LoggerFactory.getLogger(MainMonitor.class);
    private boolean running = true;
    /**
     * Maximum amount of slots in the current grid.
     */
    private int MAX_SLOTS = 0;
    private int cores = 0;
    /**
     * Maximum amount of instances in group.
     */
    private int MAX_INSTANCE = 0;
    /**
     * Minimum amount of instances in group.
     */
    private int MIN_INSTANCE = 0;
    /**
     * # of current Instances.
     */
    private int CURRENT_INSTANCE = 0;
    private int scalingViolation = 0;
    /**
     * # Evaluation.
     */
    private int currentEvaluation = 0;
    /**
     * Needed to aquire the autoscaling group name and available instances to
     * delete.
     */
    private AmazonAutoScaling as;
    /**
     * Needed to aquire the private DNS for the grid engine.
     */
    private AmazonEC2 ec2;
    /**
     * The following will be configured the first time an instance is added.
     */
    private String CLUSTER_ID = "";
    private DocumentBuilderFactory dbFactory;
    private DocumentBuilder builder;
    /**
     * ActiveNodes in the grid.
     */
    private List<QueueEntry> activeNodes;
    /**
     * Current state of the monitor.
     */
    private MonitoringState state = START;
    /**
     * Name of ScaleUp Policy.
     */
    private String scaleUpName = "";
    private int[] evaluations = new int[EVALUATION_COUNT];

    public static void main(String[] args) {
        if (args.length == 2) {
            MainMonitor mainClient = new MainMonitor(args[0], args[1]);
        } else {
            System.out.println("Please provide region and cluster ID. e.g. java -jar monitor.jar 'region' 'id'");
        }

    }

    public MainMonitor(String region, String clusterId) {
        Path propertiesFilePath = FileSystems.getDefault().getPath(DEFAULT_PROPERTIES_DIRNAME, "credentials");
        String awsCredentialsFilePath = propertiesFilePath.toString();
        File credentialsFile = new File(awsCredentialsFilePath);
        try {

            CLUSTER_ID = clusterId;

            key = new PropertiesCredentials(credentialsFile);
            as = new AmazonAutoScalingClient(key);

            as.setEndpoint("autoscaling." + region + ".amazonaws.com");
            ec2 = new AmazonEC2Client(key);
            ec2.setEndpoint("ec2." + region + ".amazonaws.com");

            dbFactory = DocumentBuilderFactory.newInstance();
            builder = dbFactory.newDocumentBuilder();
            activeNodes = new ArrayList<>();
            startEvaluation();
        } catch (IOException | ParserConfigurationException | IllegalArgumentException e) {
            e.printStackTrace();

        }

    }

    private void startEvaluation() {
        while (running) {
            switch (state) {
                case START: {
                    /**
                     * On startup the Group will be checked if it exists and its properties will be retrieved like minimum and maximum of instances.
                     * 
                     */
                    log.info("Start");
                    DescribeAutoScalingGroupsResult result = as.describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames("as_group-" + CLUSTER_ID));
                    MIN_INSTANCE = result.getAutoScalingGroups().get(0).getMinSize();
                    MAX_INSTANCE = result.getAutoScalingGroups().get(0).getMaxSize();
                    CURRENT_INSTANCE = result.getAutoScalingGroups().get(0).getInstances().size();
                    DescribeLaunchConfigurationsResult descLaunchConfigResult = as.describeLaunchConfigurations(new DescribeLaunchConfigurationsRequest().withLaunchConfigurationNames((CLUSTER_ID + "-config")));
                    String instanceType = descLaunchConfigResult.getLaunchConfigurations().get(0).getInstanceType();

                    cores = InstanceInformation.getSpecs(InstanceType.fromValue(instanceType)).instanceCores;

                    List<String> instanceIdList = new ArrayList<>();
                    log.info("Adding instances");
                    for (Instance e : result.getAutoScalingGroups().get(0).getInstances()) {
                        instanceIdList.add(e.getInstanceId());
                    }
                    /*
                     * Wait for the instances to fully start up.
                     */
                    waitForInstances(instanceIdList);
                    /*
                     * Then start adding all the instances to the queue.
                     */
                    DescribeInstancesRequest descrInstanceReq = new DescribeInstancesRequest().withInstanceIds(instanceIdList);
                    DescribeInstancesResult descrInstanceRes = ec2.describeInstances(descrInstanceReq);
                    for (Reservation reservation : descrInstanceRes.getReservations()) {
                        for (com.amazonaws.services.ec2.model.Instance f : reservation.getInstances()) {
                            startCommand("sh " + HOME + "/add_exec " + f.getPrivateDnsName() + " " + cores);
                            startCommand("sudo service gmetad restart");
                            QueueEntry entry = new QueueEntry(f.getPrivateDnsName(), "0", "0", f.getInstanceId());
                            entry.setAdded(f.getLaunchTime());
                            activeNodes.add(entry);
                            sleep(1);
                        }
                    }

                    DescribePoliciesResult policyResult = as.describePolicies(new DescribePoliciesRequest().withAutoScalingGroupName("as_group-" + CLUSTER_ID));

                    for (ScalingPolicy sp : policyResult.getScalingPolicies()) {
                        if (sp.getPolicyName().equals("as_group-" + CLUSTER_ID + "-add")) {
                            scaleUpName = "as_group-" + CLUSTER_ID + "-add";
                        }
                        sleep(1);
                    }
                    if (MIN_INSTANCE == MAX_INSTANCE) {
                        log.info("No scaling necessary. Exiting..."); {
                        System.exit(0);
                    }
                    }
                    state = EVALUATING;
                    break;
                }
                case EVALUATING: {
                    log.info("Evaluating");
                    if (currentEvaluation >= EVALUATION_COUNT) {
                        currentEvaluation = currentEvaluation % EVALUATION_COUNT;
                        log.info("Job-Slot-Use Statistic:");
                        for (int i = 0; i < EVALUATION_COUNT; ++i) {
                            log.info(i + ": " + evaluations[i]);
                        }
                        float regr = regressionAfterTime();
                        log.info("Evaluation prediction of job use: " + regr);
                        if (regr > MAX_SLOTS * (AGGRESSIVENESS / 100f)) {
                            /*
                             * Too many slots are used. This will trigger the scale up alarm.
                             */
                            scalingViolation++;
                            log.info("Alarm: Grid capacity reached. Alarm count now: " + scalingViolation);
                            if (scalingViolation > 2) {
                                /*
                                 * After too many alarms (3) an attempt to scale up the cluster will be made.
                                 */
                                if (CURRENT_INSTANCE < MAX_INSTANCE) {
                                    scalingViolation = 0;
                                    log.info("ScaleUp Policy executed.");
                                    as.executePolicy(new ExecutePolicyRequest().withAutoScalingGroupName("as_group-" + CLUSTER_ID).withPolicyName(scaleUpName));
                                    state = SCALING_IN_PROGRESS;
                                    sleep(45);
                                } else {
                                    log.info("Current instance count is already at maximum.");
                                    scalingViolation = 0;
                                }
                            }
                        } else if ((regr * -1) > MAX_SLOTS * (AGGRESSIVENESS / 100f)) {
                            /*
                             * If the Slot use is under a certain amount this alarm will trigger.
                             */
                            scalingViolation--;
                            log.info("Alarm: Grid underutilized. Alarm count now: " + scalingViolation);
                            if (scalingViolation < -2) {
                                /*
                                 * After enough violations an attempt to delete an instance will be made.
                                 */
                                scalingViolation = 0;
                                if (CURRENT_INSTANCE > MIN_INSTANCE) {
                                    String id = findIdleExecNodeId();

                                    if (!id.isEmpty()) {
                                        as.terminateInstanceInAutoScalingGroup(new TerminateInstanceInAutoScalingGroupRequest().withInstanceId(id).withShouldDecrementDesiredCapacity(true));
                                        log.info(id+" has been deleted.");
                                        CURRENT_INSTANCE--;
                                        state = COOLDOWN;
                                    }
                                } else {
                                    log.info("Grid underutilized but already at minimum.");
                                    scalingViolation = 0;
                                }
                            }
                        } else {
                            if (scalingViolation > 0) {
                                scalingViolation--;
                            } else if (scalingViolation <0) {
                                scalingViolation++;
                            }
                        }
                    }
                    makeEvaluation(currentEvaluation++);
                    break;
                }
                case SCALING_IN_PROGRESS: {
                    log.info("SCALING");
                    DescribeAutoScalingGroupsResult result = as.describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames("as_group-" + CLUSTER_ID));
                    List<String> instanceIdList = new ArrayList<>();
                    /*
                     * Search for new instance recently added.
                     */
                    log.info("Adding instances");
                    for (Instance e : result.getAutoScalingGroups().get(0).getInstances()) {
                        if (!isInstanceAdded(e.getInstanceId())) {
                            instanceIdList.add(e.getInstanceId());
                        }
                    }
                    /*
                     * Wait for it to start
                     */
                    waitForInstances(instanceIdList);
                    DescribeInstancesRequest descrInstanceReq = new DescribeInstancesRequest().withInstanceIds(instanceIdList);
                    DescribeInstancesResult descrInstanceRes = ec2.describeInstances(descrInstanceReq);
                    /*
                     * Get the IP
                     */
                    for (Reservation reservation : descrInstanceRes.getReservations()) {
                        for (com.amazonaws.services.ec2.model.Instance f : reservation.getInstances()) {
                            if (instanceIdList.contains(f.getInstanceId())) {
                                startCommand("sh " + HOME + "/add_exec " + f.getPrivateDnsName() + " " + cores);
                                startCommand("sudo service gmetad restart");
                                QueueEntry entry = new QueueEntry(f.getPrivateDnsName(), "0", "0", f.getInstanceId());
                                entry.setAdded(f.getLaunchTime());
                                activeNodes.add(entry);
                                sleep(1);
                            }
                        }
                    }
                    CURRENT_INSTANCE = result.getAutoScalingGroups().get(0).getInstances().size();
                    state = COOLDOWN;
                }
                case COOLDOWN: {
                    /*
                     * Just cooldown but keep evaluating
                     */
                    log.info("COOLDOWN");
                    if (currentEvaluation >= EVALUATION_COUNT) {
                        currentEvaluation = currentEvaluation % EVALUATION_COUNT;
                    }
                    makeEvaluation(currentEvaluation++);
                    CURRENT_COOLDOWN += EVALUATION_PERIOD;
                    if (CURRENT_COOLDOWN > SCALING_COOLDOWN) {
                        state = EVALUATING;
                        CURRENT_COOLDOWN = 0;
                    }
                    break;
                }
            }
            sleep(EVALUATION_PERIOD);
        }


    }
    /**
     *  Starts a command on the commandline.
     */
    private boolean startCommand(String command) {
        try {
            log.info("Starting command: " + command);
            Process prd = Runtime.getRuntime().exec(command + "\n");
            prd.waitFor();
            prd.destroy();
            log.info("Command executed");

            return true;
        } catch (IOException | InterruptedException ex) {

            log.info("Command could not be executed.");
            return false;
        }

    }

    private String findIdleExecNodeId() {
        // find idle exec node. Pause queue, savely delete, return DNS

        try {

            Process prd = Runtime.getRuntime().exec(GridCommands.qStatXML());
            StringWriter writer = new StringWriter();
            IOUtils.copy(prd.getInputStream(), writer, "UTF-8");
            String xml = writer.toString();
            Document doc = builder.parse(new InputSource(new ByteArrayInputStream(xml.getBytes("utf-8"))));
            NodeList queueNodes = doc.getElementsByTagName("Queue-List");
            List<QueueEntry> entryList = new ArrayList<>();
            for (int i = 0; i < queueNodes.getLength(); ++i) {
                QueueEntry entry = new QueueEntry();
                for (int j = 0; j < queueNodes.item(i).getChildNodes().getLength(); ++j) {
                    String name = queueNodes.item(i).getChildNodes().item(j).getNodeName();
                    String content = queueNodes.item(i).getChildNodes().item(j).getTextContent();

                    switch (name) {
                        case ("name"): {
                            entry.setNodeIp(content.split("@")[1]);
                            break;
                        }
                        case ("slots_used"): {
                            entry.setSlotsUsed(Integer.parseInt(content));
                            break;
                        }
                        case ("slots_total"): {
                            entry.setSlotsTotal(Integer.parseInt(content));
                            break;
                        }
                        default: {
                            break;
                        }
                    }

                }
                if (entry.getSlotsUsed() == 0) {
                    entryList.add(entry);
                }
            }
            QueueEntry dns = getClosestToHour(entryList);
            if (dns != null) {

                String nodeIp = dns.getNodeIp();

                startCommand(GridCommands.disableNode(nodeIp));
                startCommand(GridCommands.deleteNodeFromQueue(nodeIp));
                startCommand(GridCommands.deleteNodeFromHostGroup(nodeIp));
                startCommand(GridCommands.deleteNodeFromGrid(nodeIp));

                activeNodes.remove(dns);
                return dns.getInstanceId();
            }
            //            return true;
        } catch (IOException | SAXException ex) {

            log.error(ex.getMessage());
//            return false;
        }
        return "";
    }
    /**
     * Will make an evaluation of the slot usage. This will also consider array jobs.
     * @param nr 
     * Number of evaluation.
     */
    private void makeEvaluation(int nr) {

        try {
            Process prd = Runtime.getRuntime().exec(GridCommands.qStatXML());
            StringWriter writer = new StringWriter();
            IOUtils.copy(prd.getInputStream(), writer, "UTF-8");
            String xml = writer.toString();
            

            dbFactory = DocumentBuilderFactory.newInstance();
            builder = dbFactory.newDocumentBuilder();
            int neededSlots = 0;
            int totalSlots = 0;
            int usedSlots = 0;

            Document doc = builder.parse(new InputSource(new ByteArrayInputStream(xml.getBytes("utf-8"))));
            NodeList queueNodes = doc.getElementsByTagName("Queue-List");
            for (int i = 0; i < queueNodes.getLength(); ++i) {
                for (int j = 0; j < queueNodes.item(i).getChildNodes().getLength(); ++j) {
                    String name = queueNodes.item(i).getChildNodes().item(j).getNodeName();
                    String content = queueNodes.item(i).getChildNodes().item(j).getTextContent();

                    switch (name) {
                        case ("name"): {
                            break;
                        }
                        case ("slots_used"): {
                            usedSlots += Integer.parseInt(content);
                            break;
                        }
                        case ("slots_total"): {
                            totalSlots += Integer.parseInt(content);
                            break;
                        }
                        default: {
                            break;
                        }
                    }

                }

            }

            NodeList pendingJobs = doc.getElementsByTagName("job_list");

//            List<QueueEntry> entryList = new ArrayList<>();
            for (int i = 0; i < pendingJobs.getLength(); ++i) {
                int tempSlot = 0;
                boolean addSlot = false;
                for (int j = 0; j < pendingJobs.item(i).getChildNodes().getLength(); ++j) {
                    String name = pendingJobs.item(i).getChildNodes().item(j).getNodeName();
                    String content = pendingJobs.item(i).getChildNodes().item(j).getTextContent();

                    if (name.equals("slots")) {
                        tempSlot = Integer.parseInt(content);
                    } else if (name.equals("state")) {
                        if (!content.equals("r")) {
                            addSlot = true;
                        }
                    } else if (name.equals("tasks")) {
                        // needed to recognize array jobs
                        String tasks[] = content.split("-");
                        if (tasks.length == 2) {
                            int taskStart = Integer.parseInt(tasks[0]);
                            int taskEnd = Integer.parseInt(tasks[1].split(":")[0]);
                            int tasksLeft = taskEnd - taskStart;
                            tempSlot = tempSlot * tasksLeft;
                        } else {
                            tasks = content.split(",");
                            int tasksLeft = tasks.length;
                            tempSlot = tempSlot * tasksLeft;
                        }
                    }


                }
                if (addSlot) {
                    neededSlots += tempSlot;
                    addSlot = false;
                }

            }
            MAX_SLOTS = totalSlots;
            evaluations[nr] = neededSlots - (totalSlots - usedSlots);
        } catch (IOException | SAXException | ParserConfigurationException ex) {

            log.error("Evaluation not successful.");
//            return false;
        }
    }
    /**
     * Returns the instance that is closest to another compute hour.
     * @param entries
     * Candidates.
     * @return 
     * Returns a candidate.
     */
    private QueueEntry getClosestToHour(List<QueueEntry> entries) {
        
        long current = Calendar.getInstance().getTime().getTime() / 1000;
        for (QueueEntry e : activeNodes) {
            long dateAdded = e.getAdded().getTime() / 1000;
            long difference = (current - dateAdded) % 3600;
            if (difference > 2700) {
                for (QueueEntry f : entries) {
                    if (e.getNodeIp().equals(f.getNodeIp())) {
                        return e;
                    }
                }
            }
        }
        log.info("No available candidates that are close to 1 hour.");
        return null;
    }

    private void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException ie) {
            log.error("Thread.sleep interrupted!");
        }
    }

    private String getRegionZone() {
        String region = "";
        try {
            Process prd = Runtime.getRuntime().exec("curl -s http://169.254.169.254/latest/dynamic/instance-identity/document/ | grep region \n");
            StringWriter writer = new StringWriter();
            IOUtils.copy(prd.getInputStream(), writer, "UTF-8");

            region = writer.toString().substring(writer.toString().indexOf("region")).split(":")[1].replace("\"", "").replace("}", "").trim();

        } catch (IOException ex) {
            log.error(ex.getMessage());
        }

        return region;
    }

    private List<com.amazonaws.services.ec2.model.Instance> waitForInstances(List<String> listOfInstances) {
        do {
            DescribeInstancesRequest instanceDescrReq = new DescribeInstancesRequest();
            instanceDescrReq.setInstanceIds(listOfInstances);
            boolean allrunning = true;
            try {
                DescribeInstancesResult instanceDescrReqResult = ec2.describeInstances(instanceDescrReq);

                String state;
                for (com.amazonaws.services.ec2.model.Instance e : instanceDescrReqResult.getReservations().get(0).getInstances()) {
                    state = e.getState().getName();
                    if (!state.equals(InstanceStateName.Running.toString())) {
                        allrunning = false;
                        break;
                    }
                }

                if (allrunning) {
                    return instanceDescrReqResult.getReservations().get(0).getInstances();
                } else {
                    log.info("...");
                    sleep(10);
                }

            } catch (AmazonServiceException e) {
                log.debug("{}", e);
                sleep(3);
            }
        } while (true);
    }
    
    /**
     * Calculates a prediction for the future slot usage.
     * If the slot usage has been going up or down for some while this will be noted and the grid can
     * react accordingly and maybe add or delete an instance a bit earlier than usual.
     * 
     * @return 
     */
    private float regressionAfterTime() {

        float ex = 0;
        float ey = 0;

        for (int i = 0; i < EVALUATION_COUNT; ++i) {
            ex += i;
            ey += evaluations[i];
        }
        ex = ex / (float) EVALUATION_COUNT;
        ey = ey / (float) EVALUATION_COUNT;
        float bot = 0f;
        float top = 0f;
        for (int i = 0; i < EVALUATION_COUNT; ++i) {
            top += ((float) i - ex) * ((float) evaluations[i] - ey);
            bot += Math.pow(((float) i - ex), 2);
        }
        float b = top / bot;
        float a = ey - b * ex;
        return (b * (EVALUATION_COUNT + 1) + a);
    }

    private boolean isInstanceAdded(String instanceid) {
        boolean found = false;
        for (QueueEntry entry : activeNodes) {
            if (entry.getInstanceId().equals(instanceid)) {
                return true;
            }
        }
        return found;
    }
}
