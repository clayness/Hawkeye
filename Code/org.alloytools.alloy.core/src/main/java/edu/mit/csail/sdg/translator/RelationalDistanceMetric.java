package edu.mit.csail.sdg.translator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import kodkod.instance.Instance;
import kodkod.instance.Tuple;
import kodkod.instance.TupleSet;
import kodkod.ast.Relation;

/**
 * Provides distance metrics for comparing A4Solution objects.
 * Used for clustering and similarity analysis of Alloy solutions.
 */
public class RelationalDistanceMetric {

    /**
     * Calculates the Jaccard distance between two A4Solution objects.
     * Jaccard distance = 1 - Jaccard similarity
     * Jaccard similarity = |intersection| / |union|
     * 
     * @param solution1 First solution
     * @param solution2 Second solution
     * @return Distance value between 0.0 (identical) and 1.0 (completely different)
     */
    public static double jaccardDistance(A4Solution solution1, A4Solution solution2) {
        if (!solution1.satisfiable() || !solution2.satisfiable()) {
            return 1.0; // Maximum distance for unsatisfiable solutions
        }
        
        try {
            Instance instance1 = solution1.debugExtractKInstance();
            Instance instance2 = solution2.debugExtractKInstance();
            
            // Get all relations from both instances
            Set<Relation> allRelations = new HashSet<>();
            allRelations.addAll(instance1.relations());
            allRelations.addAll(instance2.relations());
            
            int totalIntersection = 0;
            int totalUnion = 0;
            
            for (Relation relation : allRelations) {
                TupleSet tuples1 = instance1.tuples(relation);
                TupleSet tuples2 = instance2.tuples(relation);
                
                // Handle null tuple sets - use the universe from the instance
                if (tuples1 == null) tuples1 = instance1.universe().factory().noneOf(relation.arity());
                if (tuples2 == null) tuples2 = instance2.universe().factory().noneOf(relation.arity());
                
                // Calculate intersection and union sizes
                Set<Tuple> set1 = new HashSet<>();
                Set<Tuple> set2 = new HashSet<>();
                
                for (Tuple t : tuples1) set1.add(t);
                for (Tuple t : tuples2) set2.add(t);
                
                Set<Tuple> intersection = new HashSet<>(set1);
                intersection.retainAll(set2);
                
                Set<Tuple> union = new HashSet<>(set1);
                union.addAll(set2);
                
                totalIntersection += intersection.size();
                totalUnion += union.size();
            }
            
            // Calculate Jaccard similarity and convert to distance
            if (totalUnion == 0) return 0.0; // Both solutions are empty
            double jaccardSimilarity = (double) totalIntersection / totalUnion;
            return 1.0 - jaccardSimilarity;
            
        } catch (Exception e) {
            // If extraction fails, return maximum distance
            return 1.0;
        }
    }

    /**
     * Calculates weighted Jaccard distance giving different weights to different relations.
     * More important relations (e.g., user-defined vs. built-in) can have higher weights.
     * 
     * @param solution1 First solution
     * @param solution2 Second solution
     * @param relationWeights Map of relation names to their weights
     * @return Weighted distance value between 0.0 and 1.0
     */
    public static double weightedJaccardDistance(A4Solution solution1, A4Solution solution2, 
                                               Map<String, Double> relationWeights) {
        if (!solution1.satisfiable() || !solution2.satisfiable()) {
            return 1.0;
        }
        
        try {
            Instance instance1 = solution1.debugExtractKInstance();
            Instance instance2 = solution2.debugExtractKInstance();
            
            Set<Relation> allRelations = new HashSet<>();
            allRelations.addAll(instance1.relations());
            allRelations.addAll(instance2.relations());
            
            double totalWeightedIntersection = 0.0;
            double totalWeightedUnion = 0.0;
            
            for (Relation relation : allRelations) {
                String relationName = relation.name();
                double weight = relationWeights.getOrDefault(relationName, 1.0);
                
                TupleSet tuples1 = instance1.tuples(relation);
                TupleSet tuples2 = instance2.tuples(relation);
                
                if (tuples1 == null) tuples1 = instance1.universe().factory().noneOf(relation.arity());
                if (tuples2 == null) tuples2 = instance2.universe().factory().noneOf(relation.arity());
                
                Set<Tuple> set1 = new HashSet<>();
                Set<Tuple> set2 = new HashSet<>();
                
                for (Tuple t : tuples1) set1.add(t);
                for (Tuple t : tuples2) set2.add(t);
                
                Set<Tuple> intersection = new HashSet<>(set1);
                intersection.retainAll(set2);
                
                Set<Tuple> union = new HashSet<>(set1);
                union.addAll(set2);
                
                totalWeightedIntersection += weight * intersection.size();
                totalWeightedUnion += weight * union.size();
            }
            
            if (totalWeightedUnion == 0.0) return 0.0;
            double weightedSimilarity = totalWeightedIntersection / totalWeightedUnion;
            return 1.0 - weightedSimilarity;
            
        } catch (Exception e) {
            return 1.0;
        }
    }

    /**
     * Calculates Hamming distance between solutions by treating each possible tuple
     * as a binary feature and counting differences.
     * 
     * @param solution1 First solution
     * @param solution2 Second solution
     * @return Normalized Hamming distance between 0.0 and 1.0
     */
    public static double hammingDistance(A4Solution solution1, A4Solution solution2) {
        if (!solution1.satisfiable() || !solution2.satisfiable()) {
            return 1.0;
        }
        
        try {
            Instance instance1 = solution1.debugExtractKInstance();
            Instance instance2 = solution2.debugExtractKInstance();
            
            // Get bounds to determine all possible tuples
            // We'll use the bounds from solution1 (assuming both have same scope)
            Set<Relation> allRelations = new HashSet<>();
            allRelations.addAll(instance1.relations());
            allRelations.addAll(instance2.relations());
            
            int differences = 0;
            int totalPossibleTuples = 0;
            
            for (Relation relation : allRelations) {
                TupleSet tuples1 = instance1.tuples(relation);
                TupleSet tuples2 = instance2.tuples(relation);
                
                if (tuples1 == null) tuples1 = instance1.universe().factory().noneOf(relation.arity());
                if (tuples2 == null) tuples2 = instance2.universe().factory().noneOf(relation.arity());
                
                Set<Tuple> set1 = new HashSet<>();
                Set<Tuple> set2 = new HashSet<>();
                
                for (Tuple t : tuples1) set1.add(t);
                for (Tuple t : tuples2) set2.add(t);
                
                // Find all tuples that appear in either solution
                Set<Tuple> allTuples = new HashSet<>(set1);
                allTuples.addAll(set2);
                
                // Count differences
                for (Tuple tuple : allTuples) {
                    boolean inSol1 = set1.contains(tuple);
                    boolean inSol2 = set2.contains(tuple);
                    if (inSol1 != inSol2) {
                        differences++;
                    }
                    totalPossibleTuples++;
                }
            }
            
            if (totalPossibleTuples == 0) return 0.0;
            return (double) differences / totalPossibleTuples;
            
        } catch (Exception e) {
            return 1.0;
        }
    }

    /**
     * Creates default relation weights for weighted Jaccard distance.
     * User-defined relations get weight 1.0, built-in relations get weight 0.5.
     * 
     * @param solution Sample solution to extract relations from
     * @return Map of relation names to weights
     */
    public static Map<String, Double> createDefaultRelationWeights(A4Solution solution) {
        Map<String, Double> weights = new HashMap<>();
        
        try {
            Instance instance = solution.debugExtractKInstance();
            
            for (Relation relation : instance.relations()) {
                String name = relation.name();
                
                // Built-in relations typically have specific prefixes or patterns
                if (name.startsWith("Int/") || name.startsWith("String/") || 
                    name.equals("univ") || name.equals("iden") || 
                    name.startsWith("seq/")) {
                    weights.put(name, 0.5);
                } else {
                    weights.put(name, 1.0);
                }
            }
        } catch (Exception e) {
            // Return empty map on error
        }
        
        return weights;
    }
}

