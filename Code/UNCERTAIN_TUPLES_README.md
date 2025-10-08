# Uncertain Tuple Support in A4Solution

This document describes the uncertain tuple storage functionality added to the A4Solution class.

## Overview

The A4Solution class has been enhanced to optionally store uncertain tuple data alongside the standard solution data. This allows for visualization of tuples that exist in some but not all solutions within a cluster.

## Key Features

### ✅ **Backward Compatibility**
- Existing A4Solution usage remains completely unchanged
- No performance overhead for standard A4Solutions
- All existing methods work exactly as before

### ✅ **Clear Distinguishability** 
- `hasUncertainTuples()` method indicates whether solution contains uncertain data
- Boolean flag set once during construction and immutable thereafter

### ✅ **Immutability Support**
- Uncertain tuple data becomes immutable when `solve()` is called
- Follows same lifecycle pattern as other A4Solution data

### ✅ **Type Safety**
- Compile-time checking for uncertain tuple operations
- Clear error messages for invalid operations

## Usage

### Creating A4Solutions

```java
// Standard A4Solution (existing usage unchanged)
A4Solution standardSolution = new A4Solution(cmd, bitwidth, maxseq, 
                                            stringAtoms, atoms, reporter, options, 1);

// A4Solution with uncertain tuple support
A4Solution uncertainSolution = new A4Solution(cmd, bitwidth, maxseq, 
                                             stringAtoms, atoms, reporter, options, 1, true);
```

### Checking for Uncertain Tuple Support

```java
if (solution.hasUncertainTuples()) {
    // This solution can store and provide uncertain tuple data
    Set<Tuple> uncertainTuples = solution.getUncertainTuples(someRelation);
    Map<Tuple, Integer> frequencies = solution.getUncertainTupleFrequency(someRelation);
} else {
    // This is a standard solution - uncertain tuple methods return empty collections
}
```

### Populating Uncertain Tuple Data

```java
// From a ClusterSolution
A4Solution uncertainSolution = new A4Solution(/*params*/, true);
uncertainSolution.populateUncertainTuples(clusterSolution);

// Or use the factory method
A4Solution solution = A4Solution.createWithUncertainTuples(cluster, options, "command");
```

### Accessing Uncertain Tuple Data

```java
// Get uncertain tuples for a specific relation
Set<Tuple> uncertainTuples = solution.getUncertainTuples(relation);

// Get frequency data (how many solutions contained each tuple)
Map<Tuple, Integer> frequencies = solution.getUncertainTupleFrequency(relation);

// Get A4TupleSet for expressions (for future visualization support)
A4TupleSet uncertainTupleSet = solution.evalUncertain(expression);
```

## API Reference

### Constructor
```java
A4Solution(String originalCommand, int bitwidth, int maxseq, 
           Set<String> stringAtoms, Collection<String> atoms, 
           A4Reporter rep, A4Options opt, int expected, 
           boolean hasUncertainTuples)
```

### Query Methods
```java
boolean hasUncertainTuples()                              // Check if solution supports uncertain tuples
Set<Tuple> getUncertainTuples(Relation relation)          // Get uncertain tuples for relation
Map<Tuple, Integer> getUncertainTupleFrequency(Relation relation)  // Get frequency data
A4TupleSet evalUncertain(Expr expr)                       // Get uncertain data for expression
```

### Data Population
```java
void populateUncertainTuples(ClusterSolution cluster)     // Populate from ClusterSolution
static A4Solution createWithUncertainTuples(ClusterSolution cluster, A4Options options, String command)  // Factory method
```

## Implementation Details

### Internal Structure
- `boolean hasUncertainTuples` - Immutable flag indicating uncertain tuple support
- `Map<Relation, Set<Tuple>> uncertainTuples` - Storage for uncertain tuples per relation
- `Map<Relation, Map<Tuple, Integer>> uncertainTupleFrequency` - Frequency data for visualization
- `Map<Expr, A4TupleSet> uncertainTupleCache` - Cache for expression evaluation

### Memory Management
- Fields are `null` for standard A4Solutions (no memory overhead)
- Fields are initialized only when `hasUncertainTuples = true`
- Data becomes immutable when `solved()` is called

### Error Handling
- Clear error messages for invalid operations
- Type-safe access patterns
- Graceful degradation (empty collections) for standard solutions

## Integration with Clustering

This feature is designed to work seamlessly with the existing clustering functionality:

```java
// Extract solutions and create clusters
List<A4Solution> solutions = originalSolution.extractSolutions(20);
List<ClusterSolution> clusters = ClusterMaker.createKMeansDefaultClusters(solutions, 4);

// Create A4Solution with uncertain data from cluster
for (ClusterSolution cluster : clusters) {
    A4Solution solutionWithUncertainData = A4Solution.createWithUncertainTuples(
        cluster, options, "cluster_" + cluster.getClusterId());
    
    // Now you can visualize both present and uncertain tuples
    if (solutionWithUncertainData.hasUncertainTuples()) {
        // Visualize present tuples (existing functionality)
        A4TupleSet presentTuples = solutionWithUncertainData.eval(someRelation);
        
        // Visualize uncertain tuples (new functionality) 
        Set<Tuple> uncertainTuples = solutionWithUncertainData.getUncertainTuples(someRelation);
        Map<Tuple, Integer> frequencies = solutionWithUncertainData.getUncertainTupleFrequency(someRelation);
    }
}
```

## Future Extensions

The design allows for easy extension:
- Additional uncertain data types (expressions, formulas)
- Visualization-specific caching
- Statistical analysis methods
- Export/import of uncertain data

## Testing

Test files are provided:
- `UncertainTupleTest.java` - Basic functionality tests
- `UncertainTupleExample.java` - Usage examples and demonstrations

Run tests to verify the implementation works correctly in your environment.
