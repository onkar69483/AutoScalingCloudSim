package org.cloudbus.cloudsim;

public class Constants {
    // Host parameters
    public static final int HOSTS = 10;
    public static final int HOST_PES = 12;         // Number of CPU cores per host
    public static final int HOST_MIPS = 1000;      // MIPS per PE
    public static final int HOST_RAM = 8192;       // Host RAM in MB
    public static final int HOST_BW = 10000;       // Host bandwidth in Mbps
    public static final int HOST_STORAGE = 1000000; // Host storage in MB
    public static final double HOST_COST = 0.1;     // $ per hour per host
    public static final double COST_PER_MEM = 0.05; // $ per MB of memory
    public static final double COST_PER_STORAGE = 0.001; // $ per MB of storage
    public static final double COST_PER_BW = 0.01;  // $ per Mbps of bandwidth
    
    // VM parameters
    public static final int VMS = 5;
    public static final int VM_MIPS = 1000;        // MIPS per PE
    public static final int VM_PES = 2;            // Initial number of PEs per VM
    public static final int MAX_VM_PES = 8;        // Maximum PEs a VM can be scaled to
    public static final int VM_RAM = 1024;         // VM RAM in MB
    public static final int VM_BW = 1000;          // VM bandwidth in Mbps
    public static final int VM_SIZE = 10000;       // VM image size in MB
    
    // Cloudlet parameters
    public static final int CLOUDLETS = 20;
    public static final int CLOUDLET_PES = 1;      // Number of PEs required by each cloudlet
    public static final int CLOUDLET_LENGTH = 20000; // Length of cloudlet in MI
    public static final int CLOUDLET_FILE_SIZE = 300; // Input file size in MB
    public static final int CLOUDLET_OUTPUT_SIZE = 300; // Output file size in MB
    
    // Simulation parameters
    public static final double SCHEDULING_INTERVAL = 1.0; // Time interval for scheduling in seconds
    public static final double AUTO_SCALING_THRESHOLD = 0.7; // 70% CPU utilization threshold for scaling
    public static final double AUTO_SCALING_FACTOR = 1.5;  // Scale up by 50%
    public static final double SIMULATION_LIMIT = 50.0;   // Maximum simulation time in seconds
}