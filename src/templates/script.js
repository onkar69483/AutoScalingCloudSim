// Global variables for data storage
let simulationData = {};
let chartColors = [
    'rgba(52, 152, 219, 0.8)',  // Blue
    'rgba(46, 204, 113, 0.8)',   // Green
    'rgba(243, 156, 18, 0.8)',   // Orange
    'rgba(231, 76, 60, 0.8)',    // Red
    'rgba(155, 89, 182, 0.8)'    // Purple
];

// Function to fetch and parse CSV data
async function fetchCsvData() {
    try {
        const response = await fetch('simulation_results.csv');
        if (!response.ok) {
            throw new Error(`HTTP error! Status: ${response.status}`);
        }
        const csvText = await response.text();
        return parseCsv(csvText);
    } catch (error) {
        console.error('Error fetching CSV:', error);
        document.body.innerHTML = `<div class="container">
            <div class="panel">
                <h2>Error loading simulation data</h2>
                <p>Please make sure simulation_results.csv exists and is accessible.</p>
                <p>Error details: ${error.message}</p>
            </div>
        </div>`;
    }
}

// CSV parser function
function parseCsv(text) {
    const sections = {};
    let currentSection = null;
    let headers = [];
    
    const lines = text.split('\n');
    for (let i = 0; i < lines.length; i++) {
        const line = lines[i].trim();
        
        // Skip empty lines
        if (!line) continue;
        
        // Check if this is a section header (all caps with no commas)
        if (line.toUpperCase() === line && !line.includes(',')) {
            currentSection = line;
            sections[currentSection] = {
                headers: [],
                data: []
            };
        } else if (line.startsWith('Timestamp,')) {
            // Special handling for timestamp line
            sections.timestamp = line.split(',')[1].trim();
        } else if (currentSection && line.includes(',') && !line.startsWith('Timestamp')) {
            // Check if this is a header line (usually comes after section name)
            if (sections[currentSection].headers.length === 0) {
                headers = line.split(',').map(h => h.trim());
                sections[currentSection].headers = headers;
            } else {
                // This is a data line
                let values = line.split(',');
                
                // Create an object with the headers as keys
                const row = {};
                headers.forEach((header, index) => {
                    row[header] = values[index] || '';
                });
                
                sections[currentSection].data.push(row);
            }
        }
    }
    
    return sections;
}

// Function to create a table from data
function createTable(headers, data) {
    const table = document.createElement('table');
    
    // Create header row
    const thead = document.createElement('thead');
    const headerRow = document.createElement('tr');
    
    headers.forEach(header => {
        const th = document.createElement('th');
        th.textContent = header.replace(/_/g, ' ');
        headerRow.appendChild(th);
    });
    
    thead.appendChild(headerRow);
    table.appendChild(thead);
    
    // Create data rows
    const tbody = document.createElement('tbody');
    
    data.forEach(row => {
        const tr = document.createElement('tr');
        
        headers.forEach(header => {
            const td = document.createElement('td');
            td.textContent = row[header] || '';
            tr.appendChild(td);
        });
        
        tbody.appendChild(tr);
    });
    
    table.appendChild(tbody);
    return table;
}

// Function to initialize dashboard metrics
function initializeDashboard(data) {
    if (data.CLOUDLET_EXECUTION_SUMMARY && data.CLOUDLET_EXECUTION_SUMMARY.data.length > 0) {
        const summary = data.CLOUDLET_EXECUTION_SUMMARY.data[0];
        document.getElementById('total-cloudlets').textContent = summary.Total_Cloudlets || '-';
        document.getElementById('executed-cloudlets').textContent = summary.Executed_Cloudlets || '-';
        document.getElementById('avg-execution-time').textContent = summary.Average_Execution_Time || '-';
    }
    
    if (data.AUTO_SCALING_STATISTICS && data.AUTO_SCALING_STATISTICS.data.length > 0) {
        let totalEvents = 0;
        data.AUTO_SCALING_STATISTICS.data.forEach(row => {
            totalEvents += parseInt(row.Scaling_Events || 0);
        });
        document.getElementById('total-scaling-events').textContent = totalEvents;
    }
}

// Function to create VM utilization chart
function createUtilizationChart(data) {
    if (!data.CURRENT_VM_UTILIZATION || !data.CURRENT_VM_UTILIZATION.data.length) return;
    
    const ctx = document.getElementById('utilization-chart').getContext('2d');
    
    // Group data by VM_ID
    const vmData = {};
    data.CURRENT_VM_UTILIZATION.data.forEach(row => {
        const vmId = row.VM_ID;
        if (!vmData[vmId]) {
            vmData[vmId] = [];
        }
        vmData[vmId].push(parseFloat(row.CPU_Utilization));
    });
    
    const datasets = [];
    Object.keys(vmData).forEach((vmId, index) => {
        datasets.push({
            label: `VM ${vmId}`,
            data: vmData[vmId],
            backgroundColor: chartColors[index % chartColors.length],
            borderColor: chartColors[index % chartColors.length].replace('0.8', '1'),
            borderWidth: 1
        });
    });
    
    new Chart(ctx, {
        type: 'bar',
        data: {
            labels: Array(Math.max(...Object.values(vmData).map(arr => arr.length))).fill().map((_, i) => `Sample ${i+1}`),
            datasets: datasets
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                title: {
                    display: true,
                    text: 'VM CPU Utilization Samples (%)'
                },
                legend: {
                    position: 'top',
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    max: 100,
                    title: {
                        display: true,
                        text: 'CPU Utilization (%)'
                    }
                }
            }
        }
    });
}

// Function to create scaling events timeline chart
function createScalingTimelineChart(data) {
    if (!data.DETAILED_SCALING_EVENTS || !data.DETAILED_SCALING_EVENTS.data.length) return;
    
    const ctx = document.getElementById('scaling-timeline-chart').getContext('2d');
    
    // Group data by VM_ID
    const vmEvents = {};
    data.DETAILED_SCALING_EVENTS.data.forEach(row => {
        const vmId = row.VM_ID;
        if (!vmEvents[vmId]) {
            vmEvents[vmId] = [];
        }
        vmEvents[vmId].push({
            x: parseInt(row.Time),
            y: parseInt(row.New_PEs) - parseInt(row.Old_PEs),
            utilization: parseFloat(row.CPU_Utilization)
        });
    });
    
    const datasets = [];
    Object.keys(vmEvents).forEach((vmId, index) => {
        datasets.push({
            label: `VM ${vmId}`,
            data: vmEvents[vmId],
            backgroundColor: chartColors[index % chartColors.length],
            borderColor: chartColors[index % chartColors.length].replace('0.8', '1'),
            borderWidth: 1,
            pointRadius: 6,
            pointHoverRadius: 8
        });
    });
    
    new Chart(ctx, {
        type: 'scatter',
        data: {
            datasets: datasets
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                title: {
                    display: true,
                    text: 'VM Scaling Events Timeline'
                },
                legend: {
                    position: 'top',
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            const point = context.raw;
                            return `VM ${context.dataset.label.split(' ')[1]}: ${point.y > 0 ? '+' : ''}${point.y} PEs at time ${point.x} (Util: ${point.utilization.toFixed(2)}%)`;
                        }
                    }
                }
            },
            scales: {
                x: {
                    title: {
                        display: true,
                        text: 'Simulation Time'
                    }
                },
                y: {
                    title: {
                        display: true,
                        text: 'PE Change'
                    },
                    ticks: {
                        stepSize: 1
                    }
                }
            }
        }
    });
}

// Main function to load and display data
async function loadData() {
    simulationData = await fetchCsvData();
    
    if (simulationData) {
        // Set timestamp
        if (simulationData.timestamp) {
            document.getElementById('timestamp').textContent = `Generated on: ${simulationData.timestamp}`;
        }
        
        // Initialize dashboard metrics
        initializeDashboard(simulationData);
        
        // Create charts
        createUtilizationChart(simulationData);
        createScalingTimelineChart(simulationData);
        
        // VM execution details
        if (simulationData.VM_EXECUTION_DETAILS) {
            const vmDiv = document.getElementById('vm-execution');
            vmDiv.appendChild(createTable(
                simulationData.VM_EXECUTION_DETAILS.headers,
                simulationData.VM_EXECUTION_DETAILS.data
            ));
        }
        
        // Auto-scaling statistics
        if (simulationData.AUTO_SCALING_STATISTICS) {
            const autoScalingDiv = document.getElementById('auto-scaling');
            autoScalingDiv.appendChild(createTable(
                simulationData.AUTO_SCALING_STATISTICS.headers,
                simulationData.AUTO_SCALING_STATISTICS.data
            ));
        }
        
        // Detailed scaling events
        if (simulationData.DETAILED_SCALING_EVENTS) {
            const eventsDiv = document.getElementById('scaling-events');
            eventsDiv.appendChild(createTable(
                simulationData.DETAILED_SCALING_EVENTS.headers,
                simulationData.DETAILED_SCALING_EVENTS.data
            ));
        }
        
        // Current VM utilization
        if (simulationData.CURRENT_VM_UTILIZATION) {
            const utilizationDiv = document.getElementById('vm-utilization');
            utilizationDiv.appendChild(createTable(
                simulationData.CURRENT_VM_UTILIZATION.headers,
                simulationData.CURRENT_VM_UTILIZATION.data
            ));
        }
    }
}

// Load data when page loads
window.addEventListener('DOMContentLoaded', loadData);