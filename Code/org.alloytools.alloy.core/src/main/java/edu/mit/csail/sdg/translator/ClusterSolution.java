package edu.mit.csail.sdg.translator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorAPI;
import edu.mit.csail.sdg.alloy4.Util;
import kodkod.ast.Relation;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;
import kodkod.instance.Tuple;
import kodkod.instance.TupleSet;

/**
 * Represents a cluster of A4Solutions with aggregated tuple analysis.
 * A cluster contains multiple solutions and categorizes tuples as:
 * - Present: Tuple exists in ALL solutions
 * - Absent: Tuple exists in NO solutions  
 * - Uncertain: Tuple exists in SOME solutions
 */
public class ClusterSolution {
    
    private final List<A4Solution> solutions;
    private final Map<Relation, Set<Tuple>> presentTuples;
    private final Map<Relation, Set<Tuple>> absentTuples;
    private final Map<Relation, Set<Tuple>> uncertainTuples;
    private final Map<Relation, Map<Tuple, Integer>> tupleFrequency;
    private final int clusterId;
    
    /**
     * Creates a cluster from a list of solutions.
     * @param solutions List of A4Solutions to cluster
     * @param clusterId Unique identifier for this cluster
     */
    public ClusterSolution(List<A4Solution> solutions, int clusterId) throws Err {
        this.solutions = new ArrayList<>(solutions);
        this.clusterId = clusterId;
        this.presentTuples = new HashMap<>();
        this.absentTuples = new HashMap<>();
        this.uncertainTuples = new HashMap<>();
        this.tupleFrequency = new HashMap<>();
        
        if (solutions.isEmpty()) {
            throw new ErrorAPI("Cannot create cluster with empty solution list");
        }
        
        analyzeTuples();
    }
    
    /**
     * Analyzes tuples across all solutions in the cluster.
     */
    private void analyzeTuples() throws Err {
        // Get bounds from the first solution (all solutions should have same bounds)
        Bounds bounds = solutions.get(0).getBounds();
        int solutionCount = solutions.size();
        
        // Iterate through all relations in the bounds
        for (Relation relation : bounds.relations()) {
            Set<Tuple> present = new HashSet<>();
            Set<Tuple> absent = new HashSet<>();
            Set<Tuple> uncertain = new HashSet<>();
            Map<Tuple, Integer> frequency = new HashMap<>();
            
            // Get all possible tuples for this relation
            TupleSet upperBound = bounds.upperBound(relation);
            TupleSet lowerBound = bounds.lowerBound(relation);
            
            // Analyze each tuple in the upper bound
            for (Tuple tuple : upperBound) {
                int count = 0;
                
                // Check if tuple exists in each solution
                for (A4Solution solution : solutions) {
                    if (solution.satisfiable()) {
                        Instance instance = solution.debugExtractKInstance();
                        TupleSet relationTuples = instance.tuples(relation.name());
                        if (relationTuples != null && relationTuples.contains(tuple)) {
                            count++;
                        }
                    }
                }
                
                // Record frequency
                frequency.put(tuple, count);
                
                // Categorize based on frequency
                if (count == 0) {
                    absent.add(tuple);
                } else if (count == solutionCount) {
                    present.add(tuple);
                } else {
                    uncertain.add(tuple);
                }
            }
            
            // Store results for this relation
            presentTuples.put(relation, present);
            absentTuples.put(relation, absent);
            uncertainTuples.put(relation, uncertain);
            tupleFrequency.put(relation, frequency);
        }
    }
    
    /**
     * Logs the cluster analysis to a file.
     * @param filename Output file name
     */
    public void logClusterAnalysis(String filename) throws Err {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename, true))) {
            writer.println("=== CLUSTER " + clusterId + " ANALYSIS ===");
            writer.println("Number of solutions in cluster: " + solutions.size());
            writer.println("Number of relations analyzed: " + presentTuples.size());
            writer.println();
            
            // List all relations
            writer.println("Relations analyzed:");
            for (Relation relation : presentTuples.keySet()) {
                writer.println("  - " + relation.name());
            }
            writer.println();
            
            // Analyze each relation
            for (Relation relation : presentTuples.keySet()) {
                writer.println("Relation: " + relation.name());
                writer.println("  Present tuples: " + presentTuples.get(relation).size());
                writer.println("  Absent tuples: " + absentTuples.get(relation).size());
                writer.println("  Uncertain tuples: " + uncertainTuples.get(relation).size());
                
                // Show tuple frequencies for uncertain tuples
                Map<Tuple, Integer> frequency = tupleFrequency.get(relation);
                if (!uncertainTuples.get(relation).isEmpty()) {
                    writer.println("  Uncertain tuple frequencies:");
                    for (Tuple tuple : uncertainTuples.get(relation)) {
                        int freq = frequency.get(tuple);
                        writer.println("    " + tuple + ": " + freq + "/" + solutions.size());
                    }
                }
                writer.println();
            }
            
            writer.println("=== END CLUSTER " + clusterId + " ===\n");
            
        } catch (IOException e) {
            throw new ErrorAPI("Error writing cluster analysis to file: " + e.getMessage());
        }
    }
    
    /**
     * Returns the number of solutions in this cluster.
     */
    public int getSolutionCount() {
        return solutions.size();
    }
    
    /**
     * Returns the cluster ID.
     */
    public int getClusterId() {
        return clusterId;
    }
    
    /**
     * Returns the list of solutions in this cluster.
     */
    public List<A4Solution> getSolutions() {
        return new ArrayList<>(solutions);
    }
    
    /**
     * Returns present tuples for a given relation.
     */
    public Set<Tuple> getPresentTuples(Relation relation) {
        return new HashSet<>(presentTuples.getOrDefault(relation, new HashSet<>()));
    }
    
    /**
     * Returns absent tuples for a given relation.
     */
    public Set<Tuple> getAbsentTuples(Relation relation) {
        return new HashSet<>(absentTuples.getOrDefault(relation, new HashSet<>()));
    }
    
    /**
     * Returns uncertain tuples for a given relation.
     */
    public Set<Tuple> getUncertainTuples(Relation relation) {
        return new HashSet<>(uncertainTuples.getOrDefault(relation, new HashSet<>()));
    }
    
    /**
     * Returns tuple frequency map for a given relation.
     */
    public Map<Tuple, Integer> getTupleFrequency(Relation relation) {
        Map<Tuple, Integer> freq = tupleFrequency.get(relation);
        return freq != null ? new HashMap<>(freq) : new HashMap<>();
    }
} 