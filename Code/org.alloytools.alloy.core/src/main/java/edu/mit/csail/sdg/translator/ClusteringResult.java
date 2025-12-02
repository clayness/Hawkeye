package edu.mit.csail.sdg.translator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the result of clustering A4Solution objects.
 * Contains the clusters and their centroids.
 */
public class ClusteringResult {
    private final List<ClusterSolution> clusters;
    private final List<A4Solution> centroids;

    /**
     * Creates a new clustering result.
     * 
     * @param clusters List of cluster solutions
     * @param centroids List of centroid solutions (one per cluster)
     */
    public ClusteringResult(List<ClusterSolution> clusters, List<A4Solution> centroids) {
        this.clusters = new ArrayList<>(clusters);
        this.centroids = new ArrayList<>(centroids);
    }

    /**
     * Gets the clusters.
     * 
     * @return Unmodifiable list of clusters
     */
    public List<ClusterSolution> getClusters() {
        return Collections.unmodifiableList(clusters);
    }

    /**
     * Gets the centroids.
     * 
     * @return Unmodifiable list of centroids
     */
    public List<A4Solution> getCentroids() {
        return Collections.unmodifiableList(centroids);
    }

    /**
     * Gets the number of clusters.
     * 
     * @return Number of clusters
     */
    public int getNumClusters() {
        return clusters.size();
    }

    /**
     * Gets the total number of solutions across all clusters.
     * 
     * @return Total number of solutions
     */
    public int getTotalSolutions() {
        return clusters.stream().mapToInt(ClusterSolution::size).sum();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Clustering Result:\n");
        sb.append("Total clusters: ").append(getNumClusters()).append("\n");
        sb.append("Total solutions: ").append(getTotalSolutions()).append("\n");
        for (ClusterSolution cluster : clusters) {
            sb.append("  ").append(cluster).append("\n");
        }
        return sb.toString();
    }
}

