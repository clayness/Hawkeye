package edu.mit.csail.sdg.translator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorAPI;
import edu.mit.csail.sdg.translator.RelationalKMeansClusterer.ClusteringResult;

/**
 * Utility class for creating clusters from A4Solutions.
 * Now supports both legacy sequential clustering and new K-means relational clustering.
 */
public class ClusterMaker {
    
    public enum ClusteringStrategy {
        SEQUENTIAL,     // Legacy approach - simple partitioning
        KMEANS,         // K-means with relational distance metrics
        ENTROPY_GUIDED  // Full entropy-guided recursive clustering
    }
    
    /**
     * Creates clusters from a list of solutions using sequential clustering (legacy method).
     * @param solutions List of A4Solutions to cluster
     * @param clusterCount Number of clusters to create
     * @param solutionsPerCluster Number of solutions per cluster
     * @return List of ClusterSolution objects
     */
    public static List<ClusterSolution> createClusters(List<A4Solution> solutions, int clusterCount, int solutionsPerCluster) throws Err {
        return createClusters(solutions, clusterCount, solutionsPerCluster, ClusteringStrategy.SEQUENTIAL, null);
    }
    
    /**
     * Creates clusters using the specified clustering strategy.
     * @param solutions List of A4Solutions to cluster
     * @param clusterCount Number of clusters to create
     * @param solutionsPerCluster Number of solutions per cluster (for sequential) or target size (for others)
     * @param strategy Clustering strategy to use
     * @param relationWeights Optional weights for relations (used with weighted distance metrics)
     * @return List of ClusterSolution objects
     */
    public static List<ClusterSolution> createClusters(List<A4Solution> solutions, int clusterCount, 
                                                      int solutionsPerCluster, ClusteringStrategy strategy,
                                                      Map<String, Double> relationWeights) throws Err {
        switch (strategy) {
            case SEQUENTIAL:
                return createSequentialClusters(solutions, clusterCount, solutionsPerCluster);
            
            case KMEANS:
                return createKMeansClusters(solutions, clusterCount, relationWeights);
            
            case ENTROPY_GUIDED:
                throw new ErrorAPI("Use EntropyGuidedClusteringOrchestrator for entropy-guided clustering");
            
            default:
                return createSequentialClusters(solutions, clusterCount, solutionsPerCluster);
        }
    }
    
    /**
     * Creates clusters using sequential partitioning (original implementation).
     */
    private static List<ClusterSolution> createSequentialClusters(List<A4Solution> solutions, int clusterCount, 
                                                                 int solutionsPerCluster) throws Err {
        List<ClusterSolution> clusters = new ArrayList<>();
        
        if (solutions.size() < clusterCount * solutionsPerCluster) {
            throw new ErrorAPI("Not enough solutions for clustering. Need " + (clusterCount * solutionsPerCluster) + 
                         " but only have " + solutions.size());
        }
        
        for (int i = 0; i < clusterCount; i++) {
            int startIndex = i * solutionsPerCluster;
            int endIndex = startIndex + solutionsPerCluster;
            
            List<A4Solution> clusterSolutions = solutions.subList(startIndex, endIndex);
            ClusterSolution cluster = new ClusterSolution(clusterSolutions, i + 1);
            clusters.add(cluster);
        }
        
        return clusters;
    }
    
    /**
     * Creates clusters using K-means with relational distance metrics.
     */
    private static List<ClusterSolution> createKMeansClusters(List<A4Solution> solutions, int clusterCount,
                                                            Map<String, Double> relationWeights) throws Err {
        if (solutions.size() < clusterCount) {
            throw new ErrorAPI("Not enough solutions for K-means clustering. Need at least " + clusterCount + 
                         " but only have " + solutions.size());
        }
        
        // Use weighted Jaccard if weights provided, otherwise regular Jaccard
        RelationalKMeansClusterer.DistanceMetric metric = relationWeights != null && !relationWeights.isEmpty() 
            ? RelationalKMeansClusterer.DistanceMetric.WEIGHTED_JACCARD 
            : RelationalKMeansClusterer.DistanceMetric.JACCARD;
        
        RelationalKMeansClusterer clusterer = new RelationalKMeansClusterer(
            clusterCount, 100, 0.001, metric, relationWeights, new java.util.Random(42)
        );
        
        ClusteringResult result = clusterer.cluster(solutions);
        return result.getClusters();
    }
    
    /**
     * Creates clusters with default parameters using sequential clustering (legacy compatibility).
     * @param solutions List of A4Solutions to cluster
     * @return List of ClusterSolution objects
     */
    public static List<ClusterSolution> createDefaultClusters(List<A4Solution> solutions) throws Err {
        return createClusters(solutions, 4, 5);
    }
    
    /**
     * Creates clusters using K-means with default parameters.
     * @param solutions List of A4Solutions to cluster
     * @param clusterCount Number of clusters to create
     * @return List of ClusterSolution objects
     */
    public static List<ClusterSolution> createKMeansDefaultClusters(List<A4Solution> solutions, int clusterCount) throws Err {
        return createClusters(solutions, clusterCount, 0, ClusteringStrategy.KMEANS, null);
    }
    
    /**
     * Creates clusters using K-means with weighted relations.
     * @param solutions List of A4Solutions to cluster
     * @param clusterCount Number of clusters to create
     * @param relationWeights Weights for different relations
     * @return List of ClusterSolution objects
     */
    public static List<ClusterSolution> createWeightedKMeansClusters(List<A4Solution> solutions, int clusterCount,
                                                                   Map<String, Double> relationWeights) throws Err {
        return createClusters(solutions, clusterCount, 0, ClusteringStrategy.KMEANS, relationWeights);
    }
    
    /**
     * Creates default relation weights that emphasize user-defined relations.
     * @param solutions List of solutions to analyze for relation types
     * @return Map of relation names to weights
     */
    public static Map<String, Double> createDefaultRelationWeights(List<A4Solution> solutions) {
        if (solutions.isEmpty()) {
            return new HashMap<>();
        }
        
        return RelationalDistanceMetric.createDefaultRelationWeights(solutions.get(0));
    }
    
    /**
     * Performs entropy-guided clustering with default parameters.
     * @param initialSolution The initial solution to start clustering from
     * @param logFilename File to write analysis results
     * @return Complete clustering analysis result
     */
    public static EntropyGuidedClusteringOrchestrator.ClusteringAnalysisResult performEntropyGuidedClustering(
            A4Solution initialSolution, String logFilename) throws Err {
        
        EntropyGuidedClusteringOrchestrator orchestrator = new EntropyGuidedClusteringOrchestrator();
        return orchestrator.performCompleteAnalysis(initialSolution, logFilename);
    }
    
    /**
     * Performs entropy-guided clustering with custom parameters.
     * @param initialSolution The initial solution to start clustering from
     * @param logFilename File to write analysis results
     * @param maxSolutionsPerCluster Maximum solutions per cluster before subdivision
     * @param numClusters Number of clusters for K-means (k value)
     * @param maxRecursionDepth Maximum depth for recursive clustering
     * @return Complete clustering analysis result
     */
    public static EntropyGuidedClusteringOrchestrator.ClusteringAnalysisResult performEntropyGuidedClustering(
            A4Solution initialSolution, String logFilename, int maxSolutionsPerCluster, 
            int numClusters, int maxRecursionDepth) throws Err {
        
        EntropyGuidedClusteringOrchestrator orchestrator = new EntropyGuidedClusteringOrchestrator(
            maxSolutionsPerCluster, numClusters, maxRecursionDepth, 
            RelationalKMeansClusterer.DistanceMetric.JACCARD, null, new java.util.Random(42)
        );
        return orchestrator.performCompleteAnalysis(initialSolution, logFilename);
    }
} 