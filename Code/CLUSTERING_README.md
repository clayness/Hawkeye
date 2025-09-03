# Alloy Solution Clustering

This document explains how to use the clustering functionality added to Alloy 4.

## Overview

The clustering functionality allows you to analyze multiple solutions from an Alloy model and group them into clusters. The system now supports **three different clustering approaches**:

1. **Sequential Clustering** (Legacy): Simple partitioning of solutions
2. **K-means Relational Clustering** (New): K-means with relational distance metrics  
3. **Entropy-guided Recursive Clustering** (Advanced): Comprehensive solution space exploration

Each cluster categorizes tuples as:

- **Present**: Tuple exists in ALL solutions in the cluster
- **Absent**: Tuple exists in NO solutions in the cluster  
- **Uncertain**: Tuple exists in SOME solutions in the cluster

## Key Innovation: No Binary Serialization

Unlike other approaches that convert relations to binary features, our implementation works **directly with relational structures** (`ClusterSolution` objects), preserving the semantic meaning of the data while still providing sophisticated clustering capabilities.

## Components

### 1. ClusterSolution Class
Represents a cluster of A4Solutions with aggregated tuple analysis.

**Key Methods:**
- `ClusterSolution(List<A4Solution> solutions, int clusterId)` - Constructor
- `logClusterAnalysis(String filename)` - Logs analysis to file
- `getPresentTuples(Relation relation)` - Get present tuples for a relation
- `getAbsentTuples(Relation relation)` - Get absent tuples for a relation
- `getUncertainTuples(Relation relation)` - Get uncertain tuples for a relation

### 2. ClusterMaker Class (Enhanced)
Utility class for creating clusters from A4Solutions. Now supports multiple clustering strategies.

**Key Methods:**
- `createClusters(List<A4Solution> solutions, int clusterCount, int solutionsPerCluster, ClusteringStrategy strategy, Map<String, Double> relationWeights)` - Create clusters with specified strategy
- `createDefaultClusters(List<A4Solution> solutions)` - Create 4 clusters of 5 solutions each (legacy)
- `createKMeansDefaultClusters(List<A4Solution> solutions, int clusterCount)` - K-means clustering
- `performEntropyGuidedClustering(A4Solution initialSolution, String logFilename)` - Full entropy-guided analysis

### 3. RelationalDistanceMetric Class (New)
Defines distance metrics for comparing A4Solution objects based on relational structures.

**Key Methods:**
- `jaccardDistance(A4Solution solution1, A4Solution solution2)` - Jaccard distance between solutions
- `weightedJaccardDistance(A4Solution solution1, A4Solution solution2, Map<String, Double> relationWeights)` - Weighted Jaccard distance
- `hammingDistance(A4Solution solution1, A4Solution solution2)` - Hamming distance between solutions
- `createDefaultRelationWeights(A4Solution solution)` - Create default weights for relations

### 4. RelationalKMeansClusterer Class (New)
K-means clustering implementation that operates directly on A4Solution objects.

**Key Methods:**
- `cluster(List<A4Solution> solutions)` - Perform K-means clustering
- Distance metrics: JACCARD, WEIGHTED_JACCARD, HAMMING

### 5. RelationalEntropyCalculator Class (New)
Calculates entropy for relational clustering without binary serialization.

**Key Methods:**
- `calculateClusterEntropy(ClusterSolution cluster)` - Calculate entropy for a cluster
- `calculateUncertaintyScore(ClusterSolution cluster)` - Calculate uncertainty based on tuple variability
- `sortClustersByEntropy(List<ClusterSolution> clusters)` - Sort clusters by entropy (highest first)
- `createEntropyPriorityQueue(List<ClusterSolution> clusters)` - Create priority queue for entropy-guided processing

### 6. EntropyGuidedClusteringOrchestrator Class (New)
Orchestrates entropy-guided recursive clustering for comprehensive solution space exploration.

**Key Methods:**
- `performEntropyGuidedClustering(A4Solution initialSolution, String logFilename)` - Main clustering workflow
- `performCompleteAnalysis(A4Solution initialSolution, String baseLogFilename)` - Complete analysis with statistics

### 7. A4Solution Extensions (Enhanced)
Added multiple clustering methods to A4Solution class.

**Key Methods:**
- `extractSolutions(int count)` - Extract solutions from kEnumerator
- `performClusteringAnalysis(String logFilename)` - Sequential clustering (legacy)
- `performKMeansClusteringAnalysis(String logFilename)` - K-means clustering with relational metrics
- `performEntropyGuidedClusteringAnalysis(String logFilename)` - Advanced entropy-guided clustering
- `performEntropyGuidedClusteringAnalysis(String logFilename, int maxSolutionsPerCluster, int numClusters, int maxRecursionDepth)` - Custom entropy-guided clustering
- `demoClusterSolution(String logFilename)` - Demo single cluster creation

## Usage

### Method 1: Automatic Sequential Clustering (Legacy)

Enable clustering in A4Options and it will run automatically:

```java
A4Options options = new A4Options();
options.enableClustering = true;
options.solver = A4Options.SatSolver.SAT4J; // Must be incremental solver

// When you solve, sequential clustering will run automatically
A4Solution solution = // ... solve your model
```

### Method 2: K-means Relational Clustering (Recommended)

Use K-means clustering with relational distance metrics:

```java
// After solving with an incremental solver
A4Solution solution = // ... solve your model

if (solution.isIncremental()) {
    // K-means clustering with default parameters (20 solutions, 4 clusters)
    solution.performKMeansClusteringAnalysis("kmeans_clustering.log");
    
    // Or with custom parameters
    solution.performKMeansClusteringAnalysis("custom_kmeans.log", 30, 5);
}
```

### Method 3: Entropy-guided Recursive Clustering (Advanced)

Use the sophisticated entropy-guided approach for comprehensive exploration:

```java
// After solving with an incremental solver
A4Solution solution = // ... solve your model

if (solution.isIncremental()) {
    // Entropy-guided clustering with default parameters
    solution.performEntropyGuidedClusteringAnalysis("entropy_clustering.log");
    
    // Or with custom parameters
    solution.performEntropyGuidedClusteringAnalysis(
        "custom_entropy.log", 
        15,  // max solutions per cluster before subdivision
        3,   // number of clusters (k)
        4    // max recursion depth
    );
}
```

### Method 4: Using ClusterMaker Directly

```java
// Extract solutions
List<A4Solution> solutions = solution.extractSolutions(20);

// Sequential clustering (legacy)
List<ClusterSolution> sequentialClusters = ClusterMaker.createDefaultClusters(solutions);

// K-means clustering
List<ClusterSolution> kmeansClusters = ClusterMaker.createKMeansDefaultClusters(solutions, 4);

// Weighted K-means clustering
Map<String, Double> relationWeights = ClusterMaker.createDefaultRelationWeights(solutions);
List<ClusterSolution> weightedClusters = ClusterMaker.createWeightedKMeansClusters(solutions, 4, relationWeights);

// Log analysis for each cluster
for (ClusterSolution cluster : kmeansClusters) {
    cluster.logClusterAnalysis("cluster_analysis.log");
}
```

### Method 5: Programmatic Access to Advanced Results

```java
// For advanced analysis and programmatic access to results
EntropyGuidedClusteringOrchestrator orchestrator = new EntropyGuidedClusteringOrchestrator(
    20, 4, 5, 
    RelationalKMeansClusterer.DistanceMetric.WEIGHTED_JACCARD, 
    relationWeights, 
    new Random(42)
);

EntropyGuidedClusteringOrchestrator.ClusteringAnalysisResult result = 
    orchestrator.performCompleteAnalysis(solution, "advanced_clustering.log");

// Access results programmatically
System.out.println("Total clusters: " + result.getAllClusters().size());
System.out.println("Total solutions generated: " + result.getTotalSolutionsGenerated());
System.out.println("Overall entropy: " + result.getOverallEntropy());

// Find most uncertain cluster
ClusterSolution mostUncertain = result.getMostUncertainCluster();
System.out.println("Most uncertain cluster: " + mostUncertain.getClusterId());

// Access entropy values
Map<Integer, Double> entropies = result.getClusterEntropies();
for (ClusterSolution cluster : result.getAllClusters()) {
    double entropy = entropies.get(cluster.getClusterId());
    System.out.println("Cluster " + cluster.getClusterId() + " entropy: " + entropy);
}
```

### Method 4: ClusterSolution Demo (Single Cluster)

```java
// After solving with an incremental solver
A4Solution solution = // ... solve your model

if (solution.isIncremental()) {
    // Extract 10 solutions and create a single cluster for demonstration
    solution.demoClusterSolution("cluster_demo.log");
    
    // Or with custom number of solutions
    solution.demoClusterSolution("cluster_demo.log", 15);
}
```

## Requirements

1. **Incremental SAT Solver**: Clustering requires an incremental SAT solver (SAT4J, MiniSat, etc.)
2. **Multiple Solutions**: The model must have multiple solutions to cluster
3. **Sufficient Solutions**: Need enough solutions for clustering (default: 20 solutions for 4 clusters of 5 each)

## Output

The clustering analysis creates a log file with:

```
=== CLUSTER 1 ANALYSIS ===
Number of solutions in cluster: 5
Number of relations analyzed: 3

Relations analyzed:
  - Book
  - Name
  - Addr

Relation: Book
  Present tuples: 2
  Absent tuples: 8
  Uncertain tuples: 3
  Uncertain tuple frequencies:
    (Book0, Name0, Addr0): 3/5
    (Book0, Name1, Addr1): 2/5
    (Book0, Name2, Addr2): 4/5

=== END CLUSTER 1 ===
```

## Configuration

### A4Options Settings

```java
A4Options options = new A4Options();
options.enableClustering = true;        // Enable clustering
options.solver = A4Options.SatSolver.SAT4J;  // Must be incremental
```

### Default Parameters

- **Solutions to extract**: 20
- **Number of clusters**: 4  
- **Solutions per cluster**: 5
- **Log file**: "clustering_analysis.log"

## Example Integration

```java
// In your Alloy analysis code
A4Options options = new A4Options();
options.enableClustering = true;
options.solver = A4Options.SatSolver.SAT4J;

// Solve your model
A4Solution solution = // ... solve with options

// Clustering will run automatically and create clustering_analysis.log
```

## ClusterSolution Demo Method

The `demoClusterSolution()` method provides a simple way to test and understand how the `ClusterSolution` class works:

### What it does:
1. **Extracts solutions** from the solution iterator (default 10, or custom count)
2. **Creates a single cluster** containing all extracted solutions
3. **Logs detailed analysis** to a specified file
4. **Prints summary** to console including:
   - Number of solutions extracted
   - Number of relations analyzed
   - Names of all relations in the analysis

### Console Output Example:
```
=== ClusterSolution Demo Started ===
Extracting 10 solutions from solution iterator...
Successfully extracted 10 solutions.
Creating a single cluster from all extracted solutions...
Cluster created successfully with 10 solutions.
Logging cluster analysis to file: cluster_demo.log

=== Cluster Analysis Summary ===
Cluster ID: 1
Number of solutions in cluster: 10
Number of relations analyzed: 3
Relations in analysis:
  - Book
  - Name
  - Addr

Detailed cluster analysis has been written to: cluster_demo.log
=== ClusterSolution Demo Completed Successfully ===
```

## Troubleshooting

1. **"No solution enumerator available"**: Make sure you're using an incremental SAT solver
2. **"Not enough solutions"**: Your model doesn't have enough solutions for clustering
3. **"Solution must be generated by incremental SAT solver"**: Change your solver to an incremental one

## Files Created

### Core Clustering Infrastructure
- `ClusterSolution.java` - Main clustering class (original)
- `ClusterMaker.java` - Enhanced utility for creating clusters with multiple strategies
- `ClusteringDemo.java` - Demo usage examples (original)

### New Relational Clustering Components  
- `RelationalDistanceMetric.java` - Distance metrics for relational structures
- `RelationalKMeansClusterer.java` - K-means clustering without binary serialization
- `RelationalEntropyCalculator.java` - Entropy calculation for relational data
- `EntropyGuidedClusteringOrchestrator.java` - Advanced recursive clustering orchestrator
- `EntropyGuidedClusteringExample.java` - Comprehensive usage examples

### Modified Files
- Modified `A4Solution.java` - Added K-means and entropy-guided clustering methods
- Modified `A4Options.java` - Added enableClustering option (original)

## Key Advantages of the New Approach

### 1. **No Binary Serialization**
- Works directly with `ClusterSolution` objects and relational structures
- Preserves semantic meaning of relations and tuples
- Avoids information loss from binary conversion

### 2. **Multiple Distance Metrics**
- **Jaccard Distance**: Based on tuple set similarity
- **Weighted Jaccard**: Emphasizes important relations  
- **Hamming Distance**: Counts tuple-level differences

### 3. **Entropy-guided Exploration**
- Prioritizes high-uncertainty clusters for subdivision
- Comprehensive solution space coverage
- Recursive clustering with depth control

### 4. **Flexible Integration**
- Backward compatible with existing sequential clustering
- Easy to switch between clustering strategies
- Programmatic access to all results and statistics

### 5. **Performance Benefits**
- K-means clustering produces more meaningful clusters than sequential partitioning
- Entropy calculation guides efficient exploration
- Relational distance metrics avoid expensive binary conversion

## Comparison with Binary Clustering Approach

| Aspect | Binary Approach | Our Relational Approach |
|--------|----------------|-------------------------|
| **Data Representation** | Convert to 1s/0s | Direct relational structures |
| **Semantic Preservation** | Lost in conversion | Fully preserved |
| **Distance Metrics** | Binary feature vectors | Relational Jaccard/Hamming |
| **Clustering Algorithm** | K-means on binary data | K-means on relations |
| **Entropy Calculation** | Count of "?" features | Tuple presence variability |
| **Constraint Generation** | CNF from binary patterns | Future: Relational constraints |
| **Memory Efficiency** | High (binary vectors) | Lower (direct objects) |
| **Interpretability** | Low (binary features) | High (actual relations) |

## Future Enhancements

1. **Relational Constraint Generation**: Develop constraint generation that works directly with relational patterns (equivalent to the CNF generation in binary approach)

2. **Advanced Distance Metrics**: Add domain-specific distance metrics for different types of Alloy models

3. **Interactive Clustering**: GUI integration for interactive cluster exploration

4. **Performance Optimization**: Optimize for very large solution spaces

5. **Cluster Validation**: Add metrics to evaluate cluster quality and stability

This implementation successfully replicates the sophisticated clustering capabilities of the binary approach while maintaining the semantic richness of the original relational data structures. 