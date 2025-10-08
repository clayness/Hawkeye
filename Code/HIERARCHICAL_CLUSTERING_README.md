# Hierarchical K-means Clustering Implementation

## Overview

This implementation adds **hierarchical K-means clustering** functionality to the Alloy 4 analyzer. The system creates a two-level cluster hierarchy where parent clusters guide the generation of more refined child clusters.

## How It Works

### Phase 1: Parent Cluster Generation
1. Extract 20 solutions from the initial Alloy model
2. Apply K-means clustering to create 4 parent clusters (A, B, C, D)
3. Each parent cluster categorizes tuples as:
   - **Present**: Tuples that exist in ALL solutions in the cluster
   - **Absent**: Tuples that exist in NO solutions in the cluster  
   - **Uncertain**: Tuples that exist in SOME solutions in the cluster

### Phase 2: Child Cluster Generation
For each parent cluster:
1. Extract the **present tuples** (must be included) and **absent tuples** (must be excluded) from the parent cluster
2. **Select a seed solution** from the parent cluster's solutions
3. **Create a constrained A4Solution** with modified bounds:
   - Add present tuples as **lower bounds** (must be included)
   - Remove absent tuples from **upper bounds** (must be excluded)
4. Generate 20 new solutions from the constrained solution space
5. Apply K-means clustering to create 4 child clusters (A1-A4, B1-B4, etc.)

**Key Innovation**: True constraint enforcement through Kodkod bounds modification. Present tuples become mandatory (lower bounds) and absent tuples are forbidden (removed from upper bounds), creating genuinely constrained solution spaces for each parent cluster.

### Hierarchical Structure
```
Initial 20 solutions
├── Parent Cluster A → A's present/absent tuples → Constrained A4Solution → 20 constrained solutions → A1, A2, A3, A4
├── Parent Cluster B → B's present/absent tuples → Constrained A4Solution → 20 constrained solutions → B1, B2, B3, B4  
├── Parent Cluster C → C's present/absent tuples → Constrained A4Solution → 20 constrained solutions → C1, C2, C3, C4
└── Parent Cluster D → D's present/absent tuples → Constrained A4Solution → 20 constrained solutions → D1, D2, D3, D4
```

## Integration Points

### Automatic Integration
The hierarchical clustering is **automatically triggered** when:
- An A4Solution is solved with an incremental SAT solver
- The `solve()` method completes successfully
- Multiple solutions are available

### Manual Usage
```java
// After solving with an incremental solver
A4Solution solution = // ... solve your model

// Perform hierarchical clustering
solution.performHierarchicalKMeansClusteringAnalysis("clustering_results.log");

// Or with custom parameters
solution.performHierarchicalKMeansClusteringAnalysis("custom.log", 30, 5); // 30 solutions, 5 clusters
```

## Output Files

The system generates multiple log files with hierarchical naming (no console output):

- `clustering_results_hierarchical_main.log` - Main process log with progress information
- `clustering_results_parent.log` - Parent clusters A, B, C, D analysis
- `clustering_results_child_A.log` - Child clusters A1, A2, A3, A4 analysis  
- `clustering_results_child_B.log` - Child clusters B1, B2, B3, B4 analysis
- `clustering_results_child_C.log` - Child clusters C1, C2, C3, C4 analysis
- `clustering_results_child_D.log` - Child clusters D1, D2, D3, D4 analysis

### Sample Log Output
```
=== CLUSTER A1 ANALYSIS ===
Number of solutions in cluster: 5
Number of relations analyzed: 3

Relations analyzed:
  - Book
  - Name  
  - Addr

Relation: Book
  Present tuples: 3
  Absent tuples: 7
  Uncertain tuples: 2
  Uncertain tuple frequencies:
    (Book0, Name0, Addr0): 3/5
    (Book1, Name1, Addr1): 4/5

=== END CLUSTER A1 ===
```

## Implementation Details

### Key Classes Modified

#### A4Solution.java
- `performHierarchicalKMeansClusteringAnalysis()` - Main entry point
- `createChildClustersFromParent()` - Creates child clusters from parent
- `createConstrainedSolution()` - Creates A4Solution with additional constraints
- `cloneForHierarchicalClustering()` - Clones A4Solution for modification
- `addPresentTuplesAsLowerBounds()` - Adds present tuples as Kodkod lower bounds

#### ClusterSolution.java  
- `getPresentTuplesMap()` - Returns map of present tuples for constraint generation
- Enhanced with methods for hierarchical clustering integration

### Technical Approach

#### Present Tuple Analysis
The system analyzes present tuples from parent clusters for hierarchical guidance:
```java
// Analyze present tuples from parent cluster
Map<Relation, Set<Tuple>> presentTuples = parentCluster.getPresentTuplesMap();
logWriter.println("Found " + presentTuples.size() + " relations with present tuples");

// Log constraint information for analysis
for (Map.Entry<Relation, Set<Tuple>> entry : presentTuples.entrySet()) {
    logWriter.println("Relation " + entry.getKey().name() + ": " + 
                     entry.getValue().size() + " present tuples");
}
```

#### Bounds-Based Constraint Enforcement
Uses Kodkod bounds modification to enforce parent cluster constraints:
```java
// Get present and absent tuples from parent cluster
Map<Relation,Set<Tuple>> presentTuples = parentCluster.getPresentTuplesMap();
Map<Relation,Set<Tuple>> absentTuples = parentCluster.getAbsentTuplesMap();

// Create new A4Solution with modified bounds
A4Solution constrainedSolution = new A4Solution(originalCommand, bitwidth, maxseq, 
                                               extractStringAtoms(), kAtoms, 
                                               new A4Reporter(), originalOptions, 1);

// For each relation, modify bounds based on parent cluster analysis
for (Relation relation : allRelevantRelations) {
    TupleSet newLower = originalLower.union(presentTuples.get(relation)); // Add present tuples
    TupleSet newUpper = originalUpper.difference(absentTuples.get(relation)); // Remove absent tuples
    constrainedSolution.bounds.bound(relation, newLower, newUpper);
}

// Solve constrained problem
constrainedSolution = constrainedSolution.solve(new A4Reporter(), null, null, false);
```

#### Hierarchical Constraint Differentiation
Ensures different parent clusters generate different child clusters through constraint enforcement:
- Each parent cluster has unique sets of present and absent tuples
- Present tuples become **mandatory** in child solutions (lower bounds)
- Absent tuples become **forbidden** in child solutions (excluded from upper bounds)
- Creates truly constrained solution spaces specific to each parent cluster
- Child clusters inherit and respect their parent's tuple categorization
- Guarantees structural differentiation between A, B, C, D child cluster families

#### Fault Tolerance
The implementation includes multiple fallback levels:
1. Primary: Hierarchical K-means clustering
2. Fallback: Standard K-means clustering
3. Final fallback: Basic cluster demonstration

## Requirements

- **Incremental SAT solver** (SAT4J, MiniSat, etc.)
- **Multiple solutions** available from the model
- **Satisfiable initial solution**

## Usage Examples

### Basic Usage
```java
A4Options options = new A4Options();
options.solver = A4Options.SatSolver.SAT4J; // Incremental solver required

A4Solution solution = // ... solve your model with options
// Hierarchical clustering runs automatically!
```

### Manual Invocation
```java
// For more control over the process
if (solution.isIncremental() && solution.satisfiable()) {
    solution.performHierarchicalKMeansClusteringAnalysis("my_analysis.log");
}
```

### Demo Class Usage
```java
// Using the demonstration class
HierarchicalClusteringDemo.demonstrateHierarchicalClustering(solution, "demo.log");
```

## Key Advantages

1. **Semantic Preservation**: Works directly with relational structures, no binary conversion
2. **True Constraint Enforcement**: Present tuples enforced as lower bounds, absent tuples excluded from upper bounds
3. **Automatic Integration**: Seamlessly integrates with existing Alloy workflow
4. **Hierarchical Organization**: Clear parent-child relationship with genuine constraint-based differentiation
5. **Bounds Modification**: Direct manipulation of Kodkod bounds for precise constraint control
6. **Guaranteed Differentiation**: Each parent cluster's constraints ensure unique child cluster characteristics
7. **Structural Inheritance**: Child clusters inherit and respect their parent's tuple categorization patterns

## Future Enhancements

1. **Deeper Hierarchy**: Support for more than 2 levels
2. **Custom Strategies**: Different clustering strategies per level
3. **Interactive Exploration**: GUI integration for cluster exploration
4. **Performance Optimization**: Parallel child cluster generation
5. **Constraint Generation**: Generate Alloy constraints from cluster patterns

## Files Added/Modified

### New Files
- `HierarchicalClusteringDemo.java` - Demonstration and usage examples
- `HIERARCHICAL_CLUSTERING_README.md` - This documentation

### Modified Files
- `A4Solution.java` - Added hierarchical clustering methods and automatic integration
- `ClusterSolution.java` - Added method to get present tuples map

### Integration
The hierarchical clustering automatically replaces the standard K-means demo in the `A4Solution.solve()` workflow, providing enhanced clustering analysis without requiring changes to user code.
