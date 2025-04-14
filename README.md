# ⚡ Auto-Scaling and Load-Aware VM Allocation in CloudSim

A simulation project built on **CloudSim 3.0.3**, focused on dynamic resource allocation using auto-scaling strategies and efficient VM placement through load-aware allocation policies.

---

## 📘 Overview

This project simulates **auto-scaling and load-balancing** in a cloud datacenter environment. It demonstrates the effectiveness of dynamic VM scaling strategies based on workload demands and intelligent VM placement.

Key aspects include:

- Dynamic VM allocation based on current host utilization
- Auto-scaling based on resource utilization metrics
- Real-time workload generation and monitoring
- HTML and CSV result reporting
- Self-adjusting infrastructure

---

## ✨ Features

- ✅ **Dynamic Load-Aware VM Allocation**
- 🔄 **Auto-Scaling up/down based on utilization thresholds**
- 📊 **Host-level utilization metrics monitoring**
- ⚙️ **Load balancing across heterogeneous hosts**
- 📈 **Automatic results generation in CSV/HTML**

---

## 🖥 Requirements

- **Java JDK** 11+ (compatible with JDK 21)
- **CloudSim 3.0.3** (included in `/lib`)
- **Maven 3.x+** (optional, for building with pom.xml)

---

## 🚀 Quick Start

### ✅ Build and Run Manually

```bash
# Clone the repository
git clone https://github.com/your-username/autoscaling-cloudsim.git
cd autoscaling-cloudsim

# Create directories
mkdir -p lib
mkdir -p out
mkdir -p results

# Place cloudsim-3.0.3.jar in the lib directory
# (Download or copy from your CloudSim installation)

# Compile the project
javac -cp "lib/cloudsim-3.0.3.jar" -d out src/org/cloudbus/cloudsim/*.java src/org/cloudbus/cloudsim/utils/*.java

# Run the simulation
java -cp "out;lib/cloudsim-3.0.3.jar" org.cloudbus.cloudsim.AutoScalingSimulation
```

### ✅ Build and Run with Maven

```bash
# Compile and package the project
mvn clean package

# Run the simulation
java -jar target/autoscaling-cloudsim-1.0.0.jar
```

---

## 🔧 Understanding the Simulation

1. The simulation creates a datacenter with multiple hosts and VMs
2. A load-aware VM allocation policy places VMs on hosts based on CPU utilization
3. Auto-scaling is performed periodically based on utilization thresholds
4. New workloads (cloudlets) are generated randomly during simulation
5. Results are written to CSV files and a HTML report

### 🔍 Auto-Scaling Strategy

- When average CPU utilization > 80% → Scale UP (add VMs)
- When average CPU utilization < 30% → Scale DOWN (remove idle VMs)
- Monitoring occurs every 5 simulation seconds

### 💡 Load-Aware VM Allocation

VMs are allocated to hosts with the lowest current CPU utilization that have sufficient resources, which:
- Prevents hotspots
- Balances workload across hosts
- Maximizes resource efficiency

---

## 📈 Results Visualization

After running the simulation, check the `results` directory for:

1. **cloudlet_results.csv** - Performance metrics for all cloudlets
2. **vm_allocation.csv** - VM distribution and host utilization
3. **index.html** - Visual HTML report of simulation results

The HTML report shows:
- Cloudlet execution details
- Summary statistics
- Simulation metrics

---

## 🔄 Customizing the Simulation

To modify simulation parameters, edit the `Constants.java` file:

- `SCALE_UP_THRESHOLD` - CPU utilization threshold for scaling up (default: 80%)
- `SCALE_DOWN_THRESHOLD` - CPU utilization threshold for scaling down (default: 30%)
- `MONITORING_INTERVAL` - Time between auto-scaling checks (default: 5s)
- `INITIAL_HOST_COUNT` - Number of hosts to create initially
- `INITIAL_VM_COUNT` - Number of VMs to create initially
- `MAX_VM_COUNT` - Maximum number of VMs allowed

---

## 📍 Project Structure

```
AutoScalingCloudSim/
├── lib/
│   └── cloudsim-3.0.3.jar
├── src/
│   └── org/
│       └── cloudbus/
│           └── cloudsim/
│               ├── AutoScalingSimulation.java
│               ├── LoadAwareVmAllocationPolicy.java
│               └── utils/
│                   ├── Constants.java
│                   └── CsvWriter.java
├── results/
│   ├── cloudlet_results.csv
│   ├── vm_allocation.csv
│   └── index.html
├── pom.xml
└── README.md
```

---

## 🤝 Future Enhancements

Potential improvements:
- Implement predictive scaling based on workload patterns
- Add more sophisticated VM consolidation algorithms
- Include power models for energy consumption analysis
- Develop a graphical UI for simulation monitoring

---

## 📬 Troubleshooting

Common issues:

**1. ClassNotFoundException**
- Ensure cloudsim-3.0.3.jar is in the lib directory
- Check your classpath includes both the compiled classes and the jar

**2. Compilation Errors**
- Make sure you're using Java 11+ 
- Verify all source files are included in compilation

**3. No Results Generated**
- Check if the `results` directory exists and is writable