package edu.mit.csail.sdg.translator;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorAPI;

/**
 * Demonstration class for hierarchical K-means clustering functionality.
 * 
 * This class shows how to use the new hierarchical clustering features where:
 * 1. Parent clusters are created from initial solutions using K-means
 * 2. Each parent cluster's present tuples are used as lower bounds
 * 3. Child clusters are generated from constrained solution spaces
 * 4. Results are logged hierarchically
 * 
 * Expected output files:
 * - hierarchical_clustering_parent.log (parent clusters A, B, C, D)
 * - hierarchical_clustering_child_A.log (child clusters A1, A2, A3, A4)
 * - hierarchical_clustering_child_B.log (child clusters B1, B2, B3, B4)
 * - hierarchical_clustering_child_C.log (child clusters C1, C2, C3, C4)
 * - hierarchical_clustering_child_D.log (child clusters D1, D2, D3, D4)
 */
public class HierarchicalClusteringDemo {
    
    /**
     * Demonstrates hierarchical clustering with a given A4Solution.
     * 
     * @param solution The A4Solution to perform clustering on (must be incremental)
     * @param baseLogFilename Base name for log files
     */
    public static void demonstrateHierarchicalClustering(A4Solution solution, String baseLogFilename) throws Err {
        // Verify the solution is suitable for hierarchical clustering
        if (!solution.satisfiable()) {
            throw new ErrorAPI("Solution must be satisfiable for hierarchical clustering");
        }
        
        if (!solution.isIncremental()) {
            throw new ErrorAPI("Solution must be generated with incremental SAT solver for hierarchical clustering");
        }
        
        // Perform hierarchical clustering with default parameters
        // - 20 solutions for parent clusters
        // - 4 parent clusters (A, B, C, D)
        // - 20 solutions for each child cluster generation
        // - 4 child clusters per parent (A1-A4, B1-B4, etc.)
        // All output will be logged to files, no console output
        solution.performHierarchicalKMeansClusteringAnalysis(baseLogFilename);
    }
    
    /**
     * Demonstrates hierarchical clustering with custom parameters.
     * 
     * @param solution The A4Solution to perform clustering on
     * @param baseLogFilename Base name for log files
     * @param solutionCount Number of solutions to extract at each level
     * @param clusterCount Number of clusters to create at each level
     */
    public static void demonstrateCustomHierarchicalClustering(A4Solution solution, String baseLogFilename,
                                                              int solutionCount, int clusterCount) throws Err {
        if (!solution.satisfiable()) {
            throw new ErrorAPI("Solution must be satisfiable for hierarchical clustering");
        }
        
        if (!solution.isIncremental()) {
            throw new ErrorAPI("Solution must be generated with incremental SAT solver for hierarchical clustering");
        }
        
        // Perform hierarchical clustering with custom parameters
        // All output will be logged to files, no console output
        solution.performHierarchicalKMeansClusteringAnalysis(baseLogFilename, solutionCount, clusterCount);
    }
    
    /**
     * Example usage showing how hierarchical clustering integrates with the 
     * automatic workflow in A4Solution.solve().
     */
    public static void demonstrateAutomaticIntegration() {
        System.out.println("=== AUTOMATIC INTEGRATION EXAMPLE ===");
        System.out.println();
        System.out.println("When you solve an Alloy model with an incremental SAT solver,");
        System.out.println("hierarchical clustering is automatically triggered in A4Solution.solve().");
        System.out.println();
        System.out.println("Example code:");
        System.out.println("  A4Options options = new A4Options();");
        System.out.println("  options.solver = A4Options.SatSolver.SAT4J; // Incremental solver");
        System.out.println("  ");
        System.out.println("  A4Solution solution = // ... solve your model with options");
        System.out.println("  // Hierarchical clustering runs automatically!");
        System.out.println("  // Check hierarchical_clustering_*.log files for results");
        System.out.println();
        System.out.println("The automatic workflow includes:");
        System.out.println("1. Entropy-guided clustering (if enabled via options.enableClustering = true)");
        System.out.println("2. Hierarchical K-means clustering (always runs with incremental solver)");
        System.out.println("3. Fallback to standard K-means clustering (if hierarchical fails)");
        System.out.println("4. Final fallback to basic cluster demo (if all else fails)");
        System.out.println();
        System.out.println("=== AUTOMATIC INTEGRATION DEMONSTRATION COMPLETED ===");
    }
    
    /**
     * Main method for standalone testing.
     * Note: This requires an actual A4Solution instance to demonstrate.
     */
    public static void main(String[] args) {
        System.out.println("HierarchicalClusteringDemo - Usage Examples");
        System.out.println();
        
        // Show how to use the functionality
        demonstrateAutomaticIntegration();
        
        System.out.println();
        System.out.println("To test with an actual Alloy model:");
        System.out.println("1. Create an A4Solution with an incremental SAT solver");
        System.out.println("2. Call demonstrateHierarchicalClustering(solution, \"test.log\")");
        System.out.println("3. Examine the generated log files");
        System.out.println();
        System.out.println("For automatic clustering, simply solve your model - it happens automatically!");
    }
}