# ⚡ Auto-Scaling and Load-Aware VM Allocation in CloudSim

A simulation project built on **CloudSim 3.0.3** that demonstrates dynamic resource allocation using auto-scaling strategies and efficient VM placement through utilization-aware allocation policies.

---

## 📘 Overview

This project simulates **auto-scaling and load-balancing** in a cloud datacenter environment. It dynamically adjusts VM resources based on real-time CPU utilization and tracks performance metrics throughout the simulation lifecycle.

Key aspects include:
- Dynamic VM scaling based on CPU utilization thresholds
- VM migration across hosts when necessary
- Detailed tracking of scaling events and utilization patterns
- Comprehensive HTML and CSV reporting of simulation results

---

## ✨ Features

- ✅ **Auto-Scaling VM Allocation Policy** - Dynamically adjusts VM resources based on utilization
- 📊 **Resource Utilization Monitoring** - Tracks CPU usage in real-time during simulation
- 🔄 **VM Migration** - Relocates VMs between hosts when scaling requires more resources
- 📈 **Comprehensive Results Reporting** - Generates detailed HTML and CSV output
- ⚙️ **Customizable Parameters** - Configurable simulation settings via Constants class

---

## 🖥 Requirements

- **Java JDK** 11+
- **CloudSim 3.0.3** (included in `/lib`)
- **Maven** (optional, for building with pom.xml)

---

## 🚀 Quick Start

### ✅ Build and Run Manually

```bash
# Clone the repository
git clone https://github.com/yourusername/AutoScalingCloudSim.git
cd AutoScalingCloudSim

# Create directories
mkdir -p bin
mkdir -p results

# Compile the project
javac -Xlint:unchecked -cp "lib/cloudsim-3.0.3.jar" -d bin src/org/cloudbus/cloudsim/*.java

# Run the simulation
java -cp "bin:lib/cloudsim-3.0.3.jar:lib/cloudsim-examples-3.0.3.jar" org.cloudbus.cloudsim.Main
```

### ✅ Build and Run with Maven

```bash
# Compile and package the project
mvn clean package

# Run the simulation
java -jar target/autoscaling-cloudsim-1.0-SNAPSHOT-jar-with-dependencies.jar
```

---

## 🔧 Understanding the Simulation

The simulation implements an auto-scaling strategy that adjusts VM resources based on CPU utilization:

1. **DatacenterBroker** periodically updates VM utilization (simulating real-world load patterns)
2. **AutoScalingVmAllocationPolicy** monitors VM resource usage and performs scaling actions
3. When utilization exceeds thresholds, VMs are scaled up/down by adjusting PE (Processing Element) allocations
4. If necessary, VMs are migrated to hosts with sufficient resources
5. Detailed events and metrics are recorded throughout the process

### 🔍 Auto-Scaling Strategy

- **Scale UP**: When CPU utilization exceeds the upper threshold
- **Scale DOWN**: When CPU utilization falls below the lower threshold
- **Monitoring Interval**: Defined by `SCHEDULING_INTERVAL` in Constants class

### 💡 VM Allocation Policy

The `AutoScalingVmAllocationPolicy` class implements:
- Dynamic resource allocation based on utilization
- VM migration to balance load across hosts
- Event tracking for scaling operations
- Utilization history recording

---

## 📈 Results Visualization

After running the simulation, check the `results` directory for:

1. **index.html** - Detailed HTML report with simulation metrics
2. **simulation_results.csv** - CSV data for further analysis

The HTML report includes:
- Cloudlet execution summary
- VM execution details
- Auto-scaling statistics
- Detailed scaling events
- Current VM utilization

### ✅ Cloudlet Execution Summary

| Metric                       | Value        |
|-----------------------------|--------------|
| Cloudlets Executed          | 20 of 20     |
| Average Execution Time      | 25.321 sec   |

---

### 🖥 VM Execution Details

| VM ID | Execution Time (s) | Avg. Cloudlets |
|-------|---------------------|----------------|
| 0     | 0                   | 4              |
| 1     | 0                   | 4              |
| 2     | 0                   | 4              |
| 3     | 0                   | 4              |
| 4     | 0                   | 4              |

---

## 📈 Auto-Scaling Statistics

### ⚙️ VM Scaling Summary

| VM ID | Original PEs | Final PEs | Scaling Events |
|-------|---------------|-----------|----------------|
| 0     | 2             | 3         | 8              |
| 1     | 2             | 3         | 7              |
| 2     | 2             | 3         | 7              |
| 3     | 2             | 3         | 7              |
| 4     | 2             | 3         | 7              |

---

### 📅 Detailed Scaling Events (Sample)

| Time | VM ID | CPU Util | Old PEs | New PEs | Old Host | New Host |
|------|-------|----------|---------|---------|----------|----------|
| 8    | 0     | 72.77%   | 2       | 3       | 0        | 1        |
| 8    | 1     | 72.62%   | 2       | 3       | 0        | 0        |
| 9    | 2     | 71.35%   | 2       | 3       | 0        | 2        |
| 13   | 0     | 81.26%   | 2       | 3       | 1        | 1        |
| 23   | 3     | 91.81%   | 2       | 3       | 0        | 0        |
| 34   | 2     | 83.48%   | 2       | 3       | 2        | 2        |

> _Only a subset of events shown here. Full details in `results/index.html`._

---

### 📊 Current VM Utilization (Sample)

| VM ID | CPU Utilization |
|-------|-----------------|
| 0     | 72.77%          |
| 1     | 89.45%          |
| 2     | 87.61%          |
| 3     | 91.21%          |
| 4     | 86.64%          |

> _VMs are monitored periodically, and these values reflect a snapshot from the simulation._


## 🔄 Customizing the Simulation

To modify simulation parameters, edit the `Constants.java` file:

- `HOSTS` - Number of hosts in the datacenter
- `VMS` - Initial number of VMs
- `CLOUDLETS` - Number of cloudlets (tasks) to simulate
- `SIMULATION_LIMIT` - Maximum simulation time
- `SCHEDULING_INTERVAL` - Frequency of utilization checks and scaling events
- Resource specifications (MIPS, RAM, BW, etc.)

---

## 📍 Project Structure

```
AutoScalingCloudSim/
├── .github/
│   └── workflows/
│       └── cloudsim-pipeline.yml      # CI/CD pipeline configuration
├── bin/                               # Compiled classes
├── lib/                               # CloudSim libraries
│   ├── cloudsim-3.0.3.jar
│   ├── cloudsim-examples-3.0.3.jar
│   └── cloudsim-3.0.3-sources.jar
├── results/                           # Simulation output
│   ├── index.html                     # HTML report
│   └── simulation_results.csv         # CSV data
├── src/
│   └── org/
│       └── cloudbus/
│           └── cloudsim/
│               ├── AutoScalingVmAllocationPolicy.java
│               ├── CloudletGenerator.java
│               ├── Constants.java
│               └── Main.java
├── pom.xml                            # Maven build configuration
└── README.md
```

---

## 🔑 Key Components

- **Main.java** - Entry point and simulation controller
- **AutoScalingVmAllocationPolicy.java** - Implements the dynamic scaling logic
- **CloudletGenerator.java** - Creates and configures cloudlet workloads
- **Constants.java** - Defines simulation parameters and thresholds

---

## 📊 GitHub Pages Integration

The project includes a CI/CD pipeline that:
1. Builds and runs the simulation on each push
2. Generates simulation results
3. Deploys the results to GitHub Pages
4. Makes simulation reports available online

---

## 🤝 Future Enhancements

Potential improvements:
- Implement predictive scaling based on workload patterns
- Add more sophisticated VM consolidation algorithms
- Include power consumption analysis
- Support for heterogeneous host configurations
- Enhanced visualization of simulation results

---

## 📬 Troubleshooting

Common issues:

**1. ClassNotFoundException**
- Ensure cloudsim-3.0.3.jar is in the lib directory
- Check your classpath includes both the compiled classes and the jar files

**2. Blank Results Page**
- Make sure the simulation completes successfully before results are generated
- Check if the `results` directory exists and is writable
- Verify that the CI/CD pipeline is properly copying simulation results to the docs directory

**3. GitHub Pages Not Updating**
- Confirm the GitHub Pages source is set to the gh-pages branch
- Check workflow run logs for deployment errors