package edu.mit.csail.sdg.translator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorAPI;

/**
 * Performs K-means clustering on A4Solution objects using relational distance metrics.
 * Uses medoid-based approach since we cannot compute "average" solutions directly.
 */
public class RelationalKMeansClusterer {

    public enum DistanceMetric {
        JACCARD,
        WEIGHTED_JACCARD,
        HAMMING
    }

    private final int k;
    private final int maxIterations;
    private final double convergenceThreshold;
    private final DistanceMetric distanceMetric;
    private final Map<String, Double> relationWeights;
    private final Random random;

    /**
     * Creates a new K-means clusterer with default parameters.
     * 
     * @param k Number of clusters
     */
    public RelationalKMeansClusterer(int k) {
        this(k, 100, 0.001, DistanceMetric.JACCARD, new HashMap<>(), new Random());
    }

    /**
     * Creates a new K-means clusterer with custom parameters.
     * 
     * @param k Number of clusters
     * @param maxIterations Maximum number of iterations
     * @param convergenceThreshold Threshold for convergence
     * @param distanceMetric Distance metric to use
     * @param relationWeights Weights for relations (used with WEIGHTED_JACCARD)
     * @param random Random number generator for reproducibility
     */
    public RelationalKMeansClusterer(int k, int maxIterations, double convergenceThreshold,
                                    DistanceMetric distanceMetric, Map<String, Double> relationWeights,
                                    Random random) {
        this.k = k;
        this.maxIterations = maxIterations;
        this.convergenceThreshold = convergenceThreshold;
        this.distanceMetric = distanceMetric;
        this.relationWeights = relationWeights;
        this.random = random;
    }

    /**
     * Performs K-means clustering on a list of A4Solution objects.
     * 
     * @param solutions List of solutions to cluster
     * @return ClusteringResult containing the clustered solutions
     */
    public ClusteringResult cluster(List<A4Solution> solutions) throws Err {
        if (solutions.size() < k) {
            throw new ErrorAPI("Cannot create " + k + " clusters from " + solutions.size() + " solutions");
        }
        
        // Filter out unsatisfiable solutions
        List<A4Solution> satisfiableSolutions = solutions.stream()
            .filter(A4Solution::satisfiable)
            .collect(Collectors.toList());
            
        if (satisfiableSolutions.size() < k) {
            throw new ErrorAPI("Not enough satisfiable solutions for clustering");
        }
        
        // If using weighted Jaccard and no weights provided, create default weights
        Map<String, Double> weights = relationWeights;
        if (distanceMetric == DistanceMetric.WEIGHTED_JACCARD && weights.isEmpty()) {
            weights = RelationalDistanceMetric.createDefaultRelationWeights(satisfiableSolutions.get(0));
        }
        
        // Initialize centroids randomly
        List<A4Solution> centroids = initializeCentroids(satisfiableSolutions);
        List<List<A4Solution>> clusters = new ArrayList<>();
        
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            // Assign solutions to nearest centroids
            clusters = assignSolutionsToClusters(satisfiableSolutions, centroids, weights);
            
            // Update centroids
            List<A4Solution> newCentroids = updateCentroids(clusters);
            
            // Check for convergence
            if (hasConverged(centroids, newCentroids, weights)) {
                break;
            }
            
            centroids = newCentroids;
        }
        
        // Create ClusterSolution objects for each cluster
        List<ClusterSolution> clusterSolutions = new ArrayList<>();
        for (int i = 0; i < clusters.size(); i++) {
            if (!clusters.get(i).isEmpty()) {
                clusterSolutions.add(new ClusterSolution(clusters.get(i), i + 1));
            }
        }
        
        return new ClusteringResult(clusterSolutions, centroids);
    }

    /**
     * Initializes k centroids randomly from the given solutions.
     */
    private List<A4Solution> initializeCentroids(List<A4Solution> solutions) {
        List<A4Solution> centroids = new ArrayList<>();
        List<A4Solution> availableSolutions = new ArrayList<>(solutions);
        
        for (int i = 0; i < k && !availableSolutions.isEmpty(); i++) {
            int randomIndex = random.nextInt(availableSolutions.size());
            centroids.add(availableSolutions.remove(randomIndex));
        }
        
        return centroids;
    }

    /**
     * Assigns each solution to the cluster with the nearest centroid.
     */
    private List<List<A4Solution>> assignSolutionsToClusters(List<A4Solution> solutions, 
                                                           List<A4Solution> centroids,
                                                           Map<String, Double> weights) {
        List<List<A4Solution>> clusters = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            clusters.add(new ArrayList<>());
        }
        
        for (A4Solution solution : solutions) {
            int nearestCentroidIndex = findNearestCentroid(solution, centroids, weights);
            clusters.get(nearestCentroidIndex).add(solution);
        }
        
        return clusters;
    }

    /**
     * Finds the index of the centroid nearest to the given solution.
     */
    private int findNearestCentroid(A4Solution solution, List<A4Solution> centroids, 
                                  Map<String, Double> weights) {
        double minDistance = Double.MAX_VALUE;
        int nearestIndex = 0;
        
        for (int i = 0; i < centroids.size(); i++) {
            double distance = calculateDistance(solution, centroids.get(i), weights);
            if (distance < minDistance) {
                minDistance = distance;
                nearestIndex = i;
            }
        }
        
        return nearestIndex;
    }

    /**
     * Updates centroids by finding the medoid (most central solution) in each cluster.
     * Since we can't compute "average" solutions directly, we use medoid approach.
     */
    private List<A4Solution> updateCentroids(List<List<A4Solution>> clusters) {
        List<A4Solution> newCentroids = new ArrayList<>();
        
        for (List<A4Solution> cluster : clusters) {
            if (cluster.isEmpty()) {
                // Keep the old centroid if cluster is empty
                newCentroids.add(null);
                continue;
            }
            
            A4Solution medoid = findMedoid(cluster);
            newCentroids.add(medoid);
        }
        
        return newCentroids;
    }

    /**
     * Finds the medoid (solution with minimum total distance to all other solutions) in a cluster.
     */
    private A4Solution findMedoid(List<A4Solution> cluster) {
        if (cluster.size() == 1) {
            return cluster.get(0);
        }
        
        A4Solution medoid = cluster.get(0);
        double minTotalDistance = Double.MAX_VALUE;
        
        for (A4Solution candidate : cluster) {
            double totalDistance = 0.0;
            
            for (A4Solution other : cluster) {
                if (candidate != other) {
                    totalDistance += calculateDistance(candidate, other, relationWeights);
                }
            }
            
            if (totalDistance < minTotalDistance) {
                minTotalDistance = totalDistance;
                medoid = candidate;
            }
        }
        
        return medoid;
    }

    /**
     * Checks if the centroids have converged.
     */
    private boolean hasConverged(List<A4Solution> oldCentroids, List<A4Solution> newCentroids, 
                               Map<String, Double> weights) {
        if (oldCentroids.size() != newCentroids.size()) {
            return false;
        }
        
        double totalMovement = 0.0;
        int validComparisons = 0;
        
        for (int i = 0; i < oldCentroids.size(); i++) {
            A4Solution oldCentroid = oldCentroids.get(i);
            A4Solution newCentroid = newCentroids.get(i);
            
            if (oldCentroid != null && newCentroid != null) {
                totalMovement += calculateDistance(oldCentroid, newCentroid, weights);
                validComparisons++;
            }
        }
        
        if (validComparisons == 0) return true;
        
        double avgMovement = totalMovement / validComparisons;
        return avgMovement < convergenceThreshold;
    }

    /**
     * Calculates distance between two solutions using the configured metric.
     */
    private double calculateDistance(A4Solution solution1, A4Solution solution2, 
                                   Map<String, Double> weights) {
        switch (distanceMetric) {
            case JACCARD:
                return RelationalDistanceMetric.jaccardDistance(solution1, solution2);
            case WEIGHTED_JACCARD:
                return RelationalDistanceMetric.weightedJaccardDistance(solution1, solution2, weights);
            case HAMMING:
                return RelationalDistanceMetric.hammingDistance(solution1, solution2);
            default:
                return RelationalDistanceMetric.jaccardDistance(solution1, solution2);
        }
    }
}

