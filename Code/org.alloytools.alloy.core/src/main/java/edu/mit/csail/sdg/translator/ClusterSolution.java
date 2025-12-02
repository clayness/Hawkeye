package edu.mit.csail.sdg.translator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a cluster of similar A4Solution objects.
 * Contains the solutions in the cluster and the cluster ID.
 */
public class ClusterSolution {
    private final List<A4Solution> solutions;
    private final int clusterNumber;

    /**
     * Creates a new cluster solution.
     * 
     * @param solutions List of solutions in this cluster
     * @param clusterNumber The cluster number (1-indexed)
     */
    public ClusterSolution(List<A4Solution> solutions, int clusterNumber) {
        this.solutions = new ArrayList<>(solutions);
        this.clusterNumber = clusterNumber;
    }

    /**
     * Gets the solutions in this cluster.
     * 
     * @return Unmodifiable list of solutions
     */
    public List<A4Solution> getSolutions() {
        return Collections.unmodifiableList(solutions);
    }

    /**
     * Gets the cluster number.
     * 
     * @return Cluster number (1-indexed)
     */
    public int getClusterNumber() {
        return clusterNumber;
    }

    /**
     * Gets the number of solutions in this cluster.
     * 
     * @return Number of solutions
     */
    public int size() {
        return solutions.size();
    }

    @Override
    public String toString() {
        return "Cluster " + clusterNumber + " (" + solutions.size() + " solutions)";
    }
}

