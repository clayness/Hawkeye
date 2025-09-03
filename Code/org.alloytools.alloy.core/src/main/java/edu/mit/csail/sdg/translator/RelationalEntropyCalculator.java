package edu.mit.csail.sdg.translator;

import java.util.*;

import edu.mit.csail.sdg.alloy4.Err;
import kodkod.ast.Relation;
import kodkod.instance.Bounds;
import kodkod.instance.Tuple;
import kodkod.instance.TupleSet;

/**
 * Calculates entropy for relational clustering without binary serialization.
 * Measures uncertainty in tuple presence across solutions and clusters.
 */
public class RelationalEntropyCalculator {
    
    /**
     * Calculates the entropy of a cluster based on tuple variability.
     * Higher entropy means more uncertainty/diversity in the cluster.
     * 
     * @param cluster The cluster to analyze
     * @return Entropy value (higher = more uncertain/diverse)
     */
    public static double calculateClusterEntropy(ClusterSolution cluster) throws Err {
        List<A4Solution> solutions = cluster.getSolutions();
        if (solutions.isEmpty()) {
            return 0.0;
        }
        
        // Get bounds from first solution (assume all have same scope)
        Bounds bounds = solutions.get(0).getBounds();
        double totalEntropy = 0.0;
        int relationCount = 0;
        
        // Calculate entropy for each relation
        for (Relation relation : bounds.relations()) {
            double relationEntropy = calculateRelationEntropy(solutions, relation, bounds);
            totalEntropy += relationEntropy;
            relationCount++;
        }
        
        return relationCount > 0 ? totalEntropy / relationCount : 0.0;
    }
    
    /**
     * Calculates entropy for a specific relation across solutions.
     */
    private static double calculateRelationEntropy(List<A4Solution> solutions, Relation relation, Bounds bounds) {
        TupleSet upperBound = bounds.upperBound(relation);
        if (upperBound.isEmpty()) {
            return 0.0; // No variability possible
        }
        
        double totalTupleEntropy = 0.0;
        int tupleCount = 0;
        
        // Calculate entropy for each possible tuple in this relation
        for (Tuple tuple : upperBound) {
            double tupleEntropy = calculateTupleEntropy(solutions, relation, tuple);
            totalTupleEntropy += tupleEntropy;
            tupleCount++;
        }
        
        return tupleCount > 0 ? totalTupleEntropy / tupleCount : 0.0;
    }
    
    /**
     * Calculates Shannon entropy for a specific tuple across solutions.
     * Entropy = -sum(p * log2(p)) where p is probability of each outcome.
     */
    private static double calculateTupleEntropy(List<A4Solution> solutions, Relation relation, Tuple tuple) {
        int presentCount = 0;
        int totalSolutions = 0;
        
        for (A4Solution solution : solutions) {
            if (solution.satisfiable()) {
                try {
                    TupleSet relationTuples = solution.debugExtractKInstance().tuples(relation.name());
                    if (relationTuples != null && relationTuples.contains(tuple)) {
                        presentCount++;
                    }
                    totalSolutions++;
                } catch (Exception e) {
                    // Skip this solution if extraction fails
                }
            }
        }
        
        if (totalSolutions == 0) {
            return 0.0;
        }
        
        double pPresent = (double) presentCount / totalSolutions;
        double pAbsent = 1.0 - pPresent;
        
        // Calculate Shannon entropy
        double entropy = 0.0;
        if (pPresent > 0) {
            entropy -= pPresent * Math.log(pPresent) / Math.log(2);
        }
        if (pAbsent > 0) {
            entropy -= pAbsent * Math.log(pAbsent) / Math.log(2);
        }
        
        return entropy;
    }
    
    /**
     * Calculates uncertainty score for a cluster based on the ratio of uncertain tuples.
     * This is analogous to the "?" count in the binary approach.
     * 
     * @param cluster The cluster to analyze
     * @return Uncertainty score between 0.0 (all certain) and 1.0 (all uncertain)
     */
    public static double calculateUncertaintyScore(ClusterSolution cluster) throws Err {
        List<A4Solution> solutions = cluster.getSolutions();
        if (solutions.isEmpty()) {
            return 0.0;
        }
        
        Bounds bounds = solutions.get(0).getBounds();
        int totalTuples = 0;
        int uncertainTuples = 0;
        
        for (Relation relation : bounds.relations()) {
            TupleSet upperBound = bounds.upperBound(relation);
            
            for (Tuple tuple : upperBound) {
                totalTuples++;
                
                // Check if this tuple has uncertain presence (some but not all solutions have it)
                int presentCount = 0;
                int validSolutions = 0;
                
                for (A4Solution solution : solutions) {
                    if (solution.satisfiable()) {
                        try {
                            TupleSet relationTuples = solution.debugExtractKInstance().tuples(relation.name());
                            if (relationTuples != null && relationTuples.contains(tuple)) {
                                presentCount++;
                            }
                            validSolutions++;
                        } catch (Exception e) {
                            // Skip this solution
                        }
                    }
                }
                
                // Tuple is uncertain if it's present in some but not all solutions
                if (validSolutions > 0 && presentCount > 0 && presentCount < validSolutions) {
                    uncertainTuples++;
                }
            }
        }
        
        return totalTuples > 0 ? (double) uncertainTuples / totalTuples : 0.0;
    }
    
    /**
     * Calculates entropy for a list of clusters for prioritization.
     * Returns a map of cluster ID to entropy value.
     */
    public static Map<Integer, Double> calculateClusterEntropies(List<ClusterSolution> clusters) throws Err {
        Map<Integer, Double> entropies = new HashMap<>();
        
        for (ClusterSolution cluster : clusters) {
            double entropy = calculateClusterEntropy(cluster);
            entropies.put(cluster.getClusterId(), entropy);
        }
        
        return entropies;
    }
    
    /**
     * Calculates the average entropy across all clusters.
     */
    public static double calculateAverageEntropy(List<ClusterSolution> clusters) throws Err {
        if (clusters.isEmpty()) {
            return 0.0;
        }
        
        double totalEntropy = 0.0;
        for (ClusterSolution cluster : clusters) {
            totalEntropy += calculateClusterEntropy(cluster);
        }
        
        return totalEntropy / clusters.size();
    }
    
    /**
     * Sorts clusters by entropy in descending order (highest entropy first).
     * This enables processing the most uncertain clusters first.
     */
    public static List<ClusterSolution> sortClustersByEntropy(List<ClusterSolution> clusters) throws Err {
        List<ClusterSolution> sortedClusters = new ArrayList<>(clusters);
        
        sortedClusters.sort((cluster1, cluster2) -> {
            try {
                double entropy1 = calculateClusterEntropy(cluster1);
                double entropy2 = calculateClusterEntropy(cluster2);
                return Double.compare(entropy2, entropy1); // Descending order
            } catch (Err e) {
                return 0; // Keep original order if calculation fails
            }
        });
        
        return sortedClusters;
    }
    
    /**
     * Creates a priority queue of clusters ordered by entropy (highest first).
     */
    public static PriorityQueue<ClusterEntropyPair> createEntropyPriorityQueue(List<ClusterSolution> clusters) throws Err {
        PriorityQueue<ClusterEntropyPair> queue = new PriorityQueue<>(
            (pair1, pair2) -> Double.compare(pair2.entropy, pair1.entropy) // Descending order
        );
        
        for (ClusterSolution cluster : clusters) {
            double entropy = calculateClusterEntropy(cluster);
            queue.offer(new ClusterEntropyPair(cluster, entropy));
        }
        
        return queue;
    }
    
    /**
     * Wrapper class for cluster-entropy pairs used in priority queue.
     */
    public static class ClusterEntropyPair {
        public final ClusterSolution cluster;
        public final double entropy;
        
        public ClusterEntropyPair(ClusterSolution cluster, double entropy) {
            this.cluster = cluster;
            this.entropy = entropy;
        }
        
        @Override
        public String toString() {
            return "Cluster " + cluster.getClusterId() + " (entropy: " + String.format("%.4f", entropy) + ")";
        }
    }
    
    /**
     * Calculates variable-level entropy for each relation across a set of solutions.
     * This helps identify which relations are most variable/uncertain.
     */
    public static Map<String, Double> calculateRelationEntropies(List<A4Solution> solutions) throws Err {
        Map<String, Double> relationEntropies = new HashMap<>();
        
        if (solutions.isEmpty()) {
            return relationEntropies;
        }
        
        Bounds bounds = solutions.get(0).getBounds();
        
        for (Relation relation : bounds.relations()) {
            double entropy = calculateRelationEntropy(solutions, relation, bounds);
            relationEntropies.put(relation.name(), entropy);
        }
        
        return relationEntropies;
    }
}
