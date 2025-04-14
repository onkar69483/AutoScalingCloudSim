package org.cloudbus.cloudsim;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {
    private static List<Cloudlet> cloudletList;
    private static List<Vm> vmList;
    private static AutoScalingVmAllocationPolicy allocationPolicy;
    private static List<Host> hostList;
    private static Map<Integer, Double> vmStartTime;
    private static Map<Integer, Double> vmEndTime;

    private static final int CLOUDLET_UPDATE = 987654321;
    private static final int SIMULATION_END = 987654322;

    public static void main(String[] args) {
        Log.printLine("Starting Auto-Scaling and Load-Aware VM Allocation Simulation...");
        Log.printLine("Initialising...");
        Log.printLine("Simulation will run for: " + Constants.SIMULATION_LIMIT + " seconds");

        try {
            CloudSim.init(1, Calendar.getInstance(), false);
            
            vmStartTime = new HashMap<>();
            vmEndTime = new HashMap<>();

            Datacenter datacenter = createDatacenter("Datacenter_1");
            DatacenterBroker broker = createBroker();
            int brokerId = broker.getId();

            vmList = createVms(brokerId);
            cloudletList = CloudletGenerator.createCloudlets(brokerId, Constants.CLOUDLETS);

            broker.submitVmList(vmList);
            broker.submitCloudletList(cloudletList);

            // Schedule end of simulation after a specific time
            broker.schedule(broker.getId(), Constants.SIMULATION_LIMIT, SIMULATION_END);
            
            CloudSim.startSimulation();

            List<Cloudlet> finishedCloudlets = broker.getCloudletReceivedList();
            CloudSim.stopSimulation();

            // Generate and save results
            StringBuilder consoleOutput = new StringBuilder();
            StringBuilder csvOutput = new StringBuilder();
            
            generateResults(finishedCloudlets, consoleOutput, csvOutput);
            
            // Only write CSV file
            writeResultsToCsv(csvOutput.toString());
            
            Log.printLine("Simulation completed successfully at time: " + CloudSim.clock());
            Log.printLine("Results saved to results/simulation_results.csv");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Simulation terminated due to an error");
        }
    }

    private static Datacenter createDatacenter(String name) {
        hostList = new ArrayList<>();

        for (int i = 0; i < Constants.HOSTS; i++) {
            List<Pe> peList = new ArrayList<>();
            for (int j = 0; j < Constants.HOST_PES; j++) {
                peList.add(new Pe(j, new PeProvisionerSimple(Constants.HOST_MIPS)));
            }

            hostList.add(new Host(
                i,
                new RamProvisionerSimple(Constants.HOST_RAM),
                new BwProvisionerSimple(Constants.HOST_BW),
                Constants.HOST_STORAGE,
                peList,
                new VmSchedulerTimeShared(peList)
            ));
        }

        allocationPolicy = new AutoScalingVmAllocationPolicy(hostList);

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
            "x86", "Linux", "Xen",
            hostList, Constants.HOST_COST, Constants.COST_PER_MEM, 
            Constants.COST_PER_STORAGE, Constants.COST_PER_BW, 10.0
        );

        try {
            return new Datacenter(name, characteristics, allocationPolicy, new LinkedList<>(), Constants.SCHEDULING_INTERVAL);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static DatacenterBroker createBroker() {
        try {
            DatacenterBroker broker = new DatacenterBroker("Broker") {
                @Override
                public void processOtherEvent(SimEvent ev) {
                    if (ev.getTag() == CLOUDLET_UPDATE) {
                        updateVmUtilization();
                        // Only schedule next update if simulation hasn't ended
                        if (CloudSim.clock() < Constants.SIMULATION_LIMIT) {
                            schedule(getId(), Constants.SCHEDULING_INTERVAL, CLOUDLET_UPDATE);
                        }
                    }
                }

                @Override
                public void startEntity() {
                    super.startEntity();
                    schedule(getId(), Constants.SCHEDULING_INTERVAL, CLOUDLET_UPDATE);
                }
                
                private void updateVmUtilization() {
                    for (Vm vm : vmList) {
                        double simulationProgress = CloudSim.clock() / Constants.SIMULATION_LIMIT;
                        double baseUtilization = 0.5 + Math.sin(simulationProgress * Math.PI) * 0.4;
                        double randomFactor = 0.1 * (new Random().nextDouble() - 0.5);
                        double utilization = Math.max(0.1, Math.min(0.95, baseUtilization + randomFactor));
                        
                        allocationPolicy.updateVmUtilization(vm, utilization);
                    }
                }
                
                @Override
                public void processEvent(SimEvent ev) {
                    switch (ev.getTag()) {
                        case SIMULATION_END:
                            Log.printLine("DEBUG: Received SIMULATION_END event at time: " + CloudSim.clock());
                            Log.printLine("DEBUG: Terminating simulation...");
                            CloudSim.terminateSimulation();
                            break;
                            
                        case CloudSimTags.CLOUDLET_RETURN:
                            Cloudlet cloudlet = (Cloudlet) ev.getData();
                            getCloudletReceivedList().add(cloudlet);
                            Log.formatLine("%.3f: %s: Cloudlet %d received", 
                                CloudSim.clock(), getName(), cloudlet.getCloudletId());
                            vmEndTime.put(cloudlet.getVmId(), CloudSim.clock());
                            break;
                            
                        case CloudSimTags.CLOUDLET_SUBMIT:
                            super.processEvent(ev);
                            if (ev.getData() instanceof Cloudlet) {
                                Cloudlet cl = (Cloudlet) ev.getData();
                                vmStartTime.putIfAbsent(cl.getVmId(), CloudSim.clock());
                            } else if (ev.getData() instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<Cloudlet> list = (List<Cloudlet>) ev.getData();
                                for (Cloudlet cl : list) {
                                    vmStartTime.putIfAbsent(cl.getVmId(), CloudSim.clock());
                                }
                            }
                            break;
                            
                        default:
                            super.processEvent(ev);
                            break;
                    }
                }
            };
            return broker;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static List<Vm> createVms(int brokerId) {
        List<Vm> vms = new ArrayList<>();

        for (int i = 0; i < Constants.VMS; i++) {
            vms.add(new Vm(
                i, brokerId, Constants.VM_MIPS, Constants.VM_PES,
                Constants.VM_RAM, Constants.VM_BW, Constants.VM_SIZE,
                "Xen", new CloudletSchedulerTimeShared()
            ));
        }

        return vms;
    }

    private static void generateResults(List<Cloudlet> finishedCloudlets, StringBuilder consoleOutput, StringBuilder csvOutput) {
        int totalCloudlets = finishedCloudlets.size();
        DecimalFormat dft = new DecimalFormat("###.###");
        
        // Add CSV headers
        csvOutput.append("SIMULATION_RESULTS\n");
        csvOutput.append("Timestamp,").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n\n");
        csvOutput.append("CLOUDLET_EXECUTION_SUMMARY\n");
        csvOutput.append("Total_Cloudlets,Executed_Cloudlets,Average_Execution_Time\n");
        csvOutput.append(Constants.CLOUDLETS).append(",").append(totalCloudlets).append(",");
        
        // Console output for simulation results
        consoleOutput.append("\n========== SIMULATION RESULTS ==========\n");
        consoleOutput.append("Cloudlets Executed: ").append(totalCloudlets).append(" of ").append(Constants.CLOUDLETS).append("\n");
        
        double totalExecutionTime = 0.0;
        for (Cloudlet cloudlet : finishedCloudlets) {
            totalExecutionTime += cloudlet.getActualCPUTime();
        }
        double avgExecutionTime = totalCloudlets > 0 ? totalExecutionTime / totalCloudlets : 0;
        
        consoleOutput.append("Average Cloudlet Execution Time: ").append(dft.format(avgExecutionTime)).append(" seconds\n");
        csvOutput.append(dft.format(avgExecutionTime)).append("\n\n");
        
        Map<Integer, Double> vmExecutionTime = new HashMap<>();
        for (int i = 0; i < Constants.VMS; i++) {
            if (vmStartTime.containsKey(i) && vmEndTime.containsKey(i)) {
                vmExecutionTime.put(i, vmEndTime.get(i) - vmStartTime.get(i));
            }
        }
        
        consoleOutput.append("\nVM Execution Details:\n");
        consoleOutput.append("-------------------------------------------------\n");
        consoleOutput.append("VM ID | Execution Time (s) | Average Cloudlets\n");
        consoleOutput.append("-------------------------------------------------\n");
        
        // CSV VM execution details
        csvOutput.append("VM_EXECUTION_DETAILS\n");
        csvOutput.append("VM_ID,Execution_Time,Cloudlet_Count\n");
        
        Map<Integer, Integer> cloudletsPerVm = new HashMap<>();
        for (Cloudlet cloudlet : finishedCloudlets) {
            int vmId = cloudlet.getVmId();
            cloudletsPerVm.put(vmId, cloudletsPerVm.getOrDefault(vmId, 0) + 1);
        }
        
        for (int i = 0; i < Constants.VMS; i++) {
            int cloudletCount = cloudletsPerVm.getOrDefault(i, 0);
            double execTime = vmExecutionTime.getOrDefault(i, 0.0);
            consoleOutput.append(String.format(" %3d  | %17s | %16d\n", 
                i, dft.format(execTime), cloudletCount));
            
            csvOutput.append(i).append(",").append(dft.format(execTime)).append(",").append(cloudletCount).append("\n");
        }
        
        consoleOutput.append("-------------------------------------------------\n");
        csvOutput.append("\n");
        
        // Auto-scaling statistics
        consoleOutput.append("\n========== AUTO-SCALING STATISTICS ==========\n");
        csvOutput.append("AUTO_SCALING_STATISTICS\n");
        csvOutput.append("VM_ID,Original_PEs,Final_PEs,Scaling_Events\n");
        
        Map<Integer, Integer> scalingHistory = allocationPolicy.getVmScalingHistory();
        Map<Integer, List<AutoScalingVmAllocationPolicy.ScalingEvent>> scalingEvents = 
            allocationPolicy.getVmScalingEvents();
        Map<Integer, Integer> originalPes = allocationPolicy.getOriginalPes();
        
        consoleOutput.append("VM Scaling Summary:\n");
        consoleOutput.append("--------------------------------------------------\n");
        consoleOutput.append("VM ID | Original PEs | Final PEs | Scaling Events\n");
        consoleOutput.append("--------------------------------------------------\n");
        
        for (int i = 0; i < Constants.VMS; i++) {
            int initialPes = originalPes.getOrDefault(i, Constants.VM_PES);
            int finalPes = initialPes;
            
            List<AutoScalingVmAllocationPolicy.ScalingEvent> events = scalingEvents.getOrDefault(i, new ArrayList<>());
            if (!events.isEmpty()) {
                finalPes = events.get(events.size() - 1).newPes;
            }
            
            int scalingCount = scalingHistory.getOrDefault(i, 0);
            
            consoleOutput.append(String.format(" %3d  | %11d | %9d | %14d\n", 
                i, initialPes, finalPes, scalingCount));
            
            csvOutput.append(i).append(",").append(initialPes).append(",").append(finalPes).append(",")
                .append(scalingCount).append("\n");
        }
        
        consoleOutput.append("--------------------------------------------------\n");
        csvOutput.append("\n");
        
        consoleOutput.append("\nDetailed Scaling Events:\n");
        consoleOutput.append("-----------------------------------------------------------------------\n");
        consoleOutput.append("Time | VM ID | CPU Util | Old PEs | New PEs | Old Host | New Host\n");
        consoleOutput.append("-----------------------------------------------------------------------\n");
        
        // CSV detailed scaling events
        csvOutput.append("DETAILED_SCALING_EVENTS\n");
        csvOutput.append("Time,VM_ID,CPU_Utilization,Old_PEs,New_PEs,Old_Host,New_Host\n");
        
        List<AutoScalingVmAllocationPolicy.ScalingEvent> allEvents = new ArrayList<>();
        for (List<AutoScalingVmAllocationPolicy.ScalingEvent> events : scalingEvents.values()) {
            allEvents.addAll(events);
        }
        
        Collections.sort(allEvents, (e1, e2) -> Double.compare(e1.time, e2.time));
        
        for (AutoScalingVmAllocationPolicy.ScalingEvent event : allEvents) {
            consoleOutput.append(String.format("%4s | %5d | %8s | %7d | %7d | %8d | %8d\n",
                dft.format(event.time), event.vmId, dft.format(event.cpuUtilization * 100) + "%",
                event.oldPes, event.newPes, event.oldHostId, event.newHostId));
            
            csvOutput.append(dft.format(event.time)).append(",").append(event.vmId).append(",")
                .append(dft.format(event.cpuUtilization * 100)).append(",")
                .append(event.oldPes).append(",").append(event.newPes).append(",")
                .append(event.oldHostId).append(",").append(event.newHostId).append("\n");
        }
        
        consoleOutput.append("-----------------------------------------------------------------------\n");
        csvOutput.append("\n");
        
        Map<Vm, Double> vmUtilization = allocationPolicy.getVmUtilization();
        if (!vmUtilization.isEmpty()) {
            consoleOutput.append("\nCurrent VM Utilization:\n");
            consoleOutput.append("------------------------\n");
            consoleOutput.append("VM ID | CPU Utilization\n");
            consoleOutput.append("------------------------\n");
            
            csvOutput.append("CURRENT_VM_UTILIZATION\n");
            csvOutput.append("VM_ID,CPU_Utilization\n");
            
            for (Map.Entry<Vm, Double> entry : vmUtilization.entrySet()) {
                Vm vm = entry.getKey();
                double util = entry.getValue() * 100;
                consoleOutput.append(String.format(" %3d  | %14s\n", vm.getId(), dft.format(util) + "%"));
                
                csvOutput.append(vm.getId()).append(",").append(dft.format(util)).append("\n");
            }
            consoleOutput.append("------------------------\n");
        }
        
        // Print to console
        Log.print(consoleOutput.toString());
    }
    
    // Removed writeResultsToHtml method as we want index.html to be static
    
    private static void writeResultsToCsv(String content) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("results/simulation_results.csv"));
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.printLine("Error writing CSV results: " + e.getMessage());
        }
    }
}