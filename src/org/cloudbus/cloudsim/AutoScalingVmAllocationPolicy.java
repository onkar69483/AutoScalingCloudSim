package org.cloudbus.cloudsim;

import org.cloudbus.cloudsim.core.CloudSim;
import java.util.*;

public class AutoScalingVmAllocationPolicy extends VmAllocationPolicy {
    private Map<String, Host> vmTable;
    private List<Host> hostList;
    private Map<Vm, Double> vmUtilization;
    private Map<Integer, Integer> vmScalingHistory;
    private Map<Integer, List<ScalingEvent>> vmScalingEvents;
    private Map<Integer, Integer> originalPes;
    private String name;
    private Map<Integer, Double> lastScalingTime; // Track the last time each VM was scaled
    private static final double SCALING_COOLDOWN = 5.0; // Cooldown period in simulation time units
    
    public static class ScalingEvent {
        public double time;
        public int vmId;
        public int oldPes;
        public int newPes;
        public int oldHostId;
        public int newHostId;
        public double cpuUtilization;
        
        public ScalingEvent(double time, int vmId, int oldPes, int newPes, int oldHostId, int newHostId, double cpuUtilization) {
            this.time = time;
            this.vmId = vmId;
            this.oldPes = oldPes;
            this.newPes = newPes;
            this.oldHostId = oldHostId;
            this.newHostId = newHostId;
            this.cpuUtilization = cpuUtilization;
        }
    }

    public AutoScalingVmAllocationPolicy(List<? extends Host> hostList) {
        super(hostList);
        this.hostList = new ArrayList<>(hostList);
        this.vmTable = new HashMap<>();
        this.vmUtilization = new HashMap<>();
        this.vmScalingHistory = new HashMap<>();
        this.vmScalingEvents = new HashMap<>();
        this.originalPes = new HashMap<>();
        this.lastScalingTime = new HashMap<>();
        this.name = "AutoScalingVmAllocationPolicy";
    }

    public String getName() {
        return this.name;
    }

    @Override
    public boolean allocateHostForVm(Vm vm) {
        boolean result = false;
        Host suitableHost = findHostForVm(vm);
        
        if (suitableHost != null) {
            result = suitableHost.vmCreate(vm);
            if (result) {
                vmTable.put(vm.getUid(), suitableHost);
                vmUtilization.put(vm, 0.0);
                originalPes.putIfAbsent(vm.getId(), vm.getNumberOfPes());
                vmScalingHistory.putIfAbsent(vm.getId(), 0);
                vmScalingEvents.putIfAbsent(vm.getId(), new ArrayList<>());
                lastScalingTime.putIfAbsent(vm.getId(), 0.0);
                
                Log.printLine(CloudSim.clock() + ": " + getName() + ": VM #" + vm.getId() + 
                    " has been allocated to host #" + suitableHost.getId());
            }
        }
        return result;
    }

    @Override
    public boolean allocateHostForVm(Vm vm, Host host) {
        if (host.vmCreate(vm)) {
            vmTable.put(vm.getUid(), host);
            vmUtilization.put(vm, 0.0);
            originalPes.putIfAbsent(vm.getId(), vm.getNumberOfPes());
            vmScalingHistory.putIfAbsent(vm.getId(), 0);
            vmScalingEvents.putIfAbsent(vm.getId(), new ArrayList<>());
            lastScalingTime.putIfAbsent(vm.getId(), 0.0);
            
            Log.printLine(CloudSim.clock() + ": " + getName() + ": VM #" + vm.getId() + 
                " has been allocated to host #" + host.getId());
            return true;
        }
        return false;
    }

    @Override
    public List<Map<String, Object>> optimizeAllocation(List<? extends Vm> vmList) {
        return null;
    }

    @Override
    public void deallocateHostForVm(Vm vm) {
        Host host = vmTable.remove(vm.getUid());
        vmUtilization.remove(vm);
        if (host != null) {
            host.vmDestroy(vm);
        }
    }

    @Override
    public Host getHost(Vm vm) {
        return vmTable.get(vm.getUid());
    }

    @Override
    public Host getHost(int vmId, int userId) {
        return vmTable.get(Vm.getUid(userId, vmId));
    }

    private Host findHostForVm(Vm vm) {
        // First try to find a host with lowest utilization
        Host bestHost = null;
        double minUtilization = Double.MAX_VALUE;
        
        for (Host host : hostList) {
            if (host.isSuitableForVm(vm)) {
                double utilization = calculateHostUtilization(host);
                if (utilization < minUtilization) {
                    minUtilization = utilization;
                    bestHost = host;
                }
            }
        }
        
        return bestHost;
    }
    
    private double calculateHostUtilization(Host host) {
        double totalMips = host.getTotalMips();
        double usedMips = 0;
        
        for (Vm vm : host.getVmList()) {
            usedMips += vm.getCurrentRequestedTotalMips();
        }
        
        return usedMips / totalMips;
    }

    public void updateVmUtilization(Vm vm, double utilization) {
        if (vmUtilization.containsKey(vm)) {
            vmUtilization.put(vm, utilization);
            
            double currentTime = CloudSim.clock();
            double lastScaled = lastScalingTime.getOrDefault(vm.getId(), 0.0);
            
            // Check if we can scale (enough time passed since last scaling)
            if (utilization > Constants.AUTO_SCALING_THRESHOLD && 
                    (currentTime - lastScaled >= SCALING_COOLDOWN) &&
                    vm.getNumberOfPes() < Constants.MAX_VM_PES) {
                    
                scaleVm(vm, utilization);
            }
        }
    }

    private void scaleVm(Vm vm, double utilization) {
        Host currentHost = getHost(vm);
        if (currentHost == null) return;
        
        int currentPes = vm.getNumberOfPes();
        int newPes = Math.min(
            (int) Math.ceil(currentPes * Constants.AUTO_SCALING_FACTOR),
            Constants.MAX_VM_PES
        );
        
        // If already at max PEs or no increase in PEs, no need to scale
        if (newPes <= currentPes) return;
        
        int newRam = (int) (vm.getRam() * Constants.AUTO_SCALING_FACTOR);
        int newBw = (int) (vm.getBw() * Constants.AUTO_SCALING_FACTOR);
        
        // Create a new VM with increased resources
        Vm newVm = new Vm(
            vm.getId(), 
            vm.getUserId(), 
            vm.getMips(), 
            newPes, 
            newRam, 
            newBw, 
            vm.getSize(), 
            vm.getVmm(), 
            vm.getCloudletScheduler()
        );
        
        // Update the vmTable to reflect the new VM
        String vmUid = vm.getUid();
        
        // Try to scale on the current host first
        if (currentHost.isSuitableForVm(newVm)) {
            // Destroy old VM and create new VM on the same host
            currentHost.vmDestroy(vm);  // This returns void in CloudSim 3.0.3
            
            boolean success = currentHost.vmCreate(newVm);
            if (!success) {
                Log.printLine(CloudSim.clock() + ": " + getName() + ": Failed to create new scaled VM #" + vm.getId());
                return;
            }
            
            vmTable.put(vmUid, currentHost);
            vmUtilization.put(newVm, utilization);
            lastScalingTime.put(vm.getId(), CloudSim.clock());
            
            // Record the scaling event
            incrementScalingCount(vm.getId());
            recordScalingEvent(
                CloudSim.clock(), 
                vm.getId(), 
                currentPes, 
                newPes, 
                currentHost.getId(), 
                currentHost.getId(),
                utilization
            );
            
            Log.printLine(CloudSim.clock() + ": " + getName() + ": VM #" + vm.getId() + 
                " has been scaled up from " + currentPes + " PEs to " + newPes + " PEs");
        } else {
            // Try to find a new host if scaling on current host is not possible
            Host newHost = findHostForVm(newVm);
            if (newHost != null) {
                currentHost.vmDestroy(vm);
                newHost.vmCreate(newVm);
                vmTable.put(vmUid, newHost);
                vmUtilization.put(newVm, utilization);
                lastScalingTime.put(vm.getId(), CloudSim.clock());
                
                // Record the scaling event
                incrementScalingCount(vm.getId());
                recordScalingEvent(
                    CloudSim.clock(), 
                    vm.getId(), 
                    currentPes, 
                    newPes, 
                    currentHost.getId(), 
                    newHost.getId(),
                    utilization
                );
                
                Log.printLine(CloudSim.clock() + ": " + getName() + ": VM #" + vm.getId() + 
                    " has been migrated to host #" + newHost.getId() + " and scaled up to " + newPes + " PEs");
            }
        }
    }
    
    private void incrementScalingCount(int vmId) {
        vmScalingHistory.put(vmId, vmScalingHistory.getOrDefault(vmId, 0) + 1);
    }
    
    private void recordScalingEvent(double time, int vmId, int oldPes, int newPes, 
                                   int oldHostId, int newHostId, double cpuUtilization) {
        ScalingEvent event = new ScalingEvent(time, vmId, oldPes, newPes, oldHostId, newHostId, cpuUtilization);
        vmScalingEvents.get(vmId).add(event);
    }
    
    public Map<Integer, Integer> getVmScalingHistory() {
        return vmScalingHistory;
    }
    
    public Map<Integer, List<ScalingEvent>> getVmScalingEvents() {
        return vmScalingEvents;
    }
    
    public Map<Integer, Integer> getOriginalPes() {
        return originalPes;
    }
    
    public Map<Vm, Double> getVmUtilization() {
        return vmUtilization;
    }
}