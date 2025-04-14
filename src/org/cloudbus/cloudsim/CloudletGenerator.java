package org.cloudbus.cloudsim;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CloudletGenerator {
    public static List<Cloudlet> createCloudlets(int userId, int count) {
        List<Cloudlet> cloudlets = new ArrayList<>();
        Random rand = new Random();
        
        for (int i = 0; i < count; i++) {
            // Create cloudlets with varying computational requirements
            long length = (long) (Constants.CLOUDLET_LENGTH * (0.8 + 0.4 * rand.nextDouble()));
            
            // Use different utilization models to create dynamic workloads
            UtilizationModel cpuModel;
            
            // Assign different types of CPU utilization models to simulate varying workloads
            int modelType = rand.nextInt(3);
            switch (modelType) {
                case 0:
                    // Full utilization model - always uses 100% of allocated resources
                    cpuModel = new UtilizationModelFull();
                    break;
                case 1:
                    // Stochastic model - utilization varies randomly over time
                    cpuModel = new UtilizationModelStochastic();
                    break;
                default:
                    // Custom utilization model that increases over time
                    cpuModel = new DynamicUtilizationModel(0.5, 0.1);
                    break;
            }
            
            // Create the cloudlet with the selected utilization model
            Cloudlet cloudlet = new Cloudlet(
                i, length, Constants.CLOUDLET_PES,
                Constants.CLOUDLET_FILE_SIZE, Constants.CLOUDLET_OUTPUT_SIZE,
                cpuModel, new UtilizationModelFull(), new UtilizationModelFull()
            );
            cloudlet.setUserId(userId);
            cloudlets.add(cloudlet);
        }
        
        return cloudlets;
    }
    
    /**
     * Dynamic utilization model that changes over time
     */
    public static class DynamicUtilizationModel implements UtilizationModel {
        private double initialUtilization;
        private double utilizationIncrementPerSec;
        
        public DynamicUtilizationModel(double initialUtilization, double utilizationIncrementPerSec) {
            this.initialUtilization = initialUtilization;
            this.utilizationIncrementPerSec = utilizationIncrementPerSec;
        }
        
        @Override
        public double getUtilization(double time) {
            // Increase utilization over time, capped at 1.0 (100%)
            return Math.min(1.0, initialUtilization + time * utilizationIncrementPerSec);
        }
    }
}