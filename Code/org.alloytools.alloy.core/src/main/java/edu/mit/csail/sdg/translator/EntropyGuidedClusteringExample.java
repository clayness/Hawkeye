package edu.mit.csail.sdg.translator;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Module;
import edu.mit.csail.sdg.parser.CompUtil;

/**
 * Example demonstrating the new entropy-guided clustering functionality.
 * This shows how to use the advanced clustering features that replicate
 * the binary clustering approach but work directly with ClusterSolution objects.
 */
public class EntropyGuidedClusteringExample {
    
    /**
     * Main method demonstrating different clustering approaches.
     */
    public static void main(String[] args) {
        try {
            // Example usage - you would replace this with your actual Alloy model path
            String modelPath = "path/to/your/model.als";
            demonstrateClusteringApproaches(modelPath);
            
        } catch (Exception e) {
            System.err.println("Error running clustering example: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Demonstrates all three clustering approaches available.
     */
    public static void demonstrateClusteringApproaches(String modelPath) throws Err {
        A4Reporter rep = new A4Reporter();
        
        // Parse the Alloy model
        Module world = CompUtil.parseEverything_fromFile(rep, null, modelPath);
        
        // Setup options for incremental solving (required for clustering)
        A4Options options = new A4Options();
        options.solver = A4Options.SatSolver.SAT4J;  // Use incremental solver
        options.enableClustering = true;             // Enable automatic clustering
        
        // Get the first command from the model
        Command command = world.getAllCommands().get(0);
        
        // Solve the model
        A4Solution solution = TranslateAlloyToKodkod.execute_command(rep, world.getAllReachableSigs(), command, options);
        
        if (solution.satisfiable()) {
            System.out.println("=== DEMONSTRATING CLUSTERING APPROACHES ===\n");
            
            // 1. Legacy Sequential Clustering (original approach)
            demonstrateSequentialClustering(solution);
            
            // 2. K-means Relational Clustering (new approach)
            demonstrateKMeansClustering(solution);
            
            // 3. Entropy-guided Recursive Clustering (advanced approach)
            demonstrateEntropyGuidedClustering(solution);
            
        } else {
            System.out.println("Model is unsatisfiable - cannot demonstrate clustering");
        }
    }
    
    /**
     * Demonstrates the original sequential clustering approach.
     */
    private static void demonstrateSequentialClustering(A4Solution solution) throws Err {
        System.out.println("1. SEQUENTIAL CLUSTERING (Legacy Approach)");
        System.out.println("==========================================");
        
        try {
            // This uses the original simple partitioning approach
            solution.performClusteringAnalysis("sequential_clustering.log");
            System.out.println("✓ Sequential clustering completed. Results in: sequential_clustering.log");
            
        } catch (Err e) {
            System.out.println("✗ Sequential clustering failed: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrates K-means clustering with relational distance metrics.
     */
    private static void demonstrateKMeansClustering(A4Solution solution) throws Err {
        System.out.println("2. K-MEANS RELATIONAL CLUSTERING (New Approach)");
        System.out.println("===============================================");
        
        try {
            // K-means clustering with Jaccard distance on relational structures
            solution.performKMeansClusteringAnalysis("kmeans_clustering.log", 20, 4);
            System.out.println("✓ K-means clustering completed. Results in: kmeans_clustering.log");
            
            // Alternative: Weighted K-means clustering
            System.out.println("Running weighted K-means variant...");
            solution.performKMeansClusteringAnalysis("weighted_kmeans_clustering.log", 15, 3);
            System.out.println("✓ Weighted K-means clustering completed. Results in: weighted_kmeans_clustering.log");
            
        } catch (Err e) {
            System.out.println("✗ K-means clustering failed: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrates entropy-guided recursive clustering (the main contribution).
     */
    private static void demonstrateEntropyGuidedClustering(A4Solution solution) throws Err {
        System.out.println("3. ENTROPY-GUIDED RECURSIVE CLUSTERING (Advanced Approach)");
        System.out.println("==========================================================");
        
        try {
            // This replicates the sophisticated clustering from the other repository
            // but without binary serialization - works directly with ClusterSolution objects
            
            System.out.println("Starting entropy-guided clustering analysis...");
            System.out.println("This may take a while as it explores the solution space comprehensively.");
            
            solution.performEntropyGuidedClusteringAnalysis("entropy_guided_clustering.log");
            
            System.out.println("✓ Entropy-guided clustering completed!");
            System.out.println("  Main log: entropy_guided_clustering_main.log");
            System.out.println("  Summary: entropy_guided_clustering_summary.log");
            System.out.println("  Individual cluster logs: entropy_guided_clustering_cluster_*.log");
            
            // Alternative: Custom parameters for more control
            System.out.println("\nRunning entropy-guided clustering with custom parameters...");
            solution.performEntropyGuidedClusteringAnalysis(
                "custom_entropy_clustering.log", 
                15,  // max solutions per cluster
                3,   // number of clusters (k)
                3    // max recursion depth
            );
            System.out.println("✓ Custom entropy-guided clustering completed!");
            
        } catch (Err e) {
            System.out.println("✗ Entropy-guided clustering failed: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrates programmatic access to clustering results.
     */
    public static void demonstrateProgrammaticAccess(A4Solution solution) throws Err {
        System.out.println("4. PROGRAMMATIC ACCESS TO CLUSTERING RESULTS");
        System.out.println("============================================");
        
        try {
            // Direct access to clustering orchestrator for custom analysis
            EntropyGuidedClusteringOrchestrator orchestrator = new EntropyGuidedClusteringOrchestrator(
                20, 4, 5, 
                RelationalKMeansClusterer.DistanceMetric.JACCARD, 
                null, 
                new java.util.Random(42)
            );
            
            EntropyGuidedClusteringOrchestrator.ClusteringAnalysisResult result = 
                orchestrator.performCompleteAnalysis(solution, "programmatic_clustering.log");
            
            // Access results programmatically
            System.out.println("Clustering completed with " + result.getAllClusters().size() + " total clusters");
            System.out.println("Total solutions generated: " + result.getTotalSolutionsGenerated());
            System.out.println("Maximum recursion depth reached: " + result.getMaxDepthReached());
            System.out.println("Overall entropy: " + String.format("%.4f", result.getOverallEntropy()));
            
            // Find the most uncertain cluster
            ClusterSolution mostUncertain = result.getMostUncertainCluster();
            if (mostUncertain != null) {
                System.out.println("Most uncertain cluster: " + mostUncertain.getClusterId() + 
                                 " with " + mostUncertain.getSolutionCount() + " solutions");
            }
            
            // Access individual cluster details
            for (ClusterSolution cluster : result.getAllClusters()) {
                double entropy = result.getClusterEntropies().getOrDefault(cluster.getClusterId(), 0.0);
                System.out.println("  Cluster " + cluster.getClusterId() + 
                                 ": " + cluster.getSolutionCount() + " solutions, " +
                                 "entropy = " + String.format("%.4f", entropy));
            }
            
        } catch (Err e) {
            System.out.println("✗ Programmatic access failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates comparison between different clustering approaches.
     */
    public static void compareClusteringApproaches(A4Solution solution) throws Err {
        System.out.println("5. COMPARING CLUSTERING APPROACHES");
        System.out.println("==================================");
        
        try {
            // Extract solutions once for fair comparison
            java.util.List<A4Solution> solutions = solution.extractSolutions(20);
            System.out.println("Extracted " + solutions.size() + " solutions for comparison");
            
            // Sequential clustering
            long start = System.currentTimeMillis();
            java.util.List<ClusterSolution> sequentialClusters = 
                ClusterMaker.createClusters(solutions, 4, 5);
            long sequentialTime = System.currentTimeMillis() - start;
            
            // K-means clustering  
            start = System.currentTimeMillis();
            java.util.List<ClusterSolution> kmeansClusters = 
                ClusterMaker.createKMeansDefaultClusters(solutions, 4);
            long kmeansTime = System.currentTimeMillis() - start;
            
            // Calculate entropies for comparison
            double sequentialEntropy = RelationalEntropyCalculator.calculateAverageEntropy(sequentialClusters);
            double kmeansEntropy = RelationalEntropyCalculator.calculateAverageEntropy(kmeansClusters);
            
            System.out.println("Results:");
            System.out.println("  Sequential: " + sequentialClusters.size() + " clusters, " +
                             "avg entropy = " + String.format("%.4f", sequentialEntropy) + 
                             ", time = " + sequentialTime + "ms");
            System.out.println("  K-means:    " + kmeansClusters.size() + " clusters, " +
                             "avg entropy = " + String.format("%.4f", kmeansEntropy) + 
                             ", time = " + kmeansTime + "ms");
            
            if (kmeansEntropy > sequentialEntropy) {
                System.out.println("✓ K-means produced higher entropy (more diverse) clusters");
            } else {
                System.out.println("! Sequential clustering had higher entropy");
            }
            
        } catch (Err e) {
            System.out.println("✗ Comparison failed: " + e.getMessage());
        }
    }
}
