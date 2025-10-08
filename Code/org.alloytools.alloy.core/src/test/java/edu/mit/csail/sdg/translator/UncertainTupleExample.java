package edu.mit.csail.sdg.translator;

import java.util.*;
import edu.mit.csail.sdg.alloy4.*;
import kodkod.ast.Relation;
import kodkod.instance.Tuple;

/**
 * Example demonstrating how to use uncertain tuple functionality with A4Solution.
 * This example shows the complete workflow from creating solutions to working with uncertain data.
 */
public class UncertainTupleExample {
    
    public static void demonstrateUncertainTuples() throws Err {
        System.out.println("=== Uncertain Tuple Functionality Demo ===\n");
        
        // Step 1: Create standard A4Solution (existing functionality unchanged)
        System.out.println("1. Creating standard A4Solution...");
        A4Solution standardSolution = createSampleA4Solution(false);
        System.out.println("   hasUncertainTuples(): " + standardSolution.hasUncertainTuples());
        System.out.println("   ✓ Standard solution created successfully\n");
        
        // Step 2: Create A4Solution with uncertain tuple support
        System.out.println("2. Creating A4Solution with uncertain tuple support...");
        A4Solution uncertainSolution = createSampleA4Solution(true);
        System.out.println("   hasUncertainTuples(): " + uncertainSolution.hasUncertainTuples());
        System.out.println("   ✓ Uncertain tuple solution created successfully\n");
        
        // Step 3: Demonstrate distinguishability
        System.out.println("3. Demonstrating distinguishability...");
        demonstrateDistinguishability(standardSolution, uncertainSolution);
        
        // Step 4: Show how uncertain data would be populated from ClusterSolution
        System.out.println("4. Uncertain tuple data population process...");
        demonstrateUncertainDataPopulation(uncertainSolution);
        
        // Step 5: Show factory method for creating from ClusterSolution
        System.out.println("5. Factory method demonstration...");
        demonstrateFactoryMethod();
        
        System.out.println("=== Demo completed successfully! ===");
    }
    
    private static void demonstrateDistinguishability(A4Solution standard, A4Solution uncertain) {
        // Test that we can distinguish between the two types
        if (standard.hasUncertainTuples()) {
            System.out.println("   ERROR: Standard solution reports having uncertain tuples!");
        } else {
            System.out.println("   ✓ Standard solution correctly reports no uncertain tuples");
        }
        
        if (!uncertain.hasUncertainTuples()) {
            System.out.println("   ERROR: Uncertain solution reports not having uncertain tuples!");
        } else {
            System.out.println("   ✓ Uncertain solution correctly reports having uncertain tuples");
        }
        
        // Test access methods return appropriate empty results for standard solution
        Set<Tuple> standardUncertain = standard.getUncertainTuples(null);
        Map<Tuple, Integer> standardFreq = standard.getUncertainTupleFrequency(null);
        
        if (standardUncertain.isEmpty() && standardFreq.isEmpty()) {
            System.out.println("   ✓ Standard solution returns empty collections for uncertain data");
        } else {
            System.out.println("   ERROR: Standard solution should return empty collections!");
        }
        
        System.out.println();
    }
    
    private static void demonstrateUncertainDataPopulation(A4Solution uncertainSolution) {
        System.out.println("   Note: In real usage, you would:");
        System.out.println("   1. Create a ClusterSolution from multiple A4Solutions");
        System.out.println("   2. Call uncertainSolution.populateUncertainTuples(cluster)");
        System.out.println("   3. Access uncertain data via getUncertainTuples(relation)");
        System.out.println("   4. Get frequency data via getUncertainTupleFrequency(relation)");
        System.out.println("   ✓ Population process explained\n");
    }
    
    private static void demonstrateFactoryMethod() {
        System.out.println("   Example factory method usage:");
        System.out.println("   ```java");
        System.out.println("   // Given a ClusterSolution with uncertain tuple data");
        System.out.println("   ClusterSolution cluster = // ... your cluster");
        System.out.println("   A4Options options = new A4Options();");
        System.out.println("   ");
        System.out.println("   // Create A4Solution with uncertain data in one call");
        System.out.println("   A4Solution solution = A4Solution.createWithUncertainTuples(");
        System.out.println("       cluster, options, \"my_command\");");
        System.out.println("   ");
        System.out.println("   // Now you can access uncertain tuple data");
        System.out.println("   if (solution.hasUncertainTuples()) {");
        System.out.println("       Set<Tuple> uncertain = solution.getUncertainTuples(someRelation);");
        System.out.println("       Map<Tuple, Integer> freq = solution.getUncertainTupleFrequency(someRelation);");
        System.out.println("   }");
        System.out.println("   ```");
        System.out.println("   ✓ Factory method usage demonstrated\n");
    }
    
    private static A4Solution createSampleA4Solution(boolean hasUncertainTuples) throws Err {
        // Create test parameters
        String command = hasUncertainTuples ? "uncertain_test" : "standard_test";
        int bitwidth = 4;
        int maxseq = 4;
        
        Set<String> stringAtoms = new HashSet<String>();
        stringAtoms.add("test_string");
        
        Collection<String> atoms = new ArrayList<String>();
        atoms.add("Node0");
        atoms.add("Node1");
        atoms.add("Node2");
        atoms.add("Edge0");
        atoms.add("Edge1");
        
        A4Reporter reporter = new A4Reporter();
        A4Options options = new A4Options();
        int expected = 1;
        
        return new A4Solution(command, bitwidth, maxseq, stringAtoms, atoms, 
                             reporter, options, expected, hasUncertainTuples);
    }
    
    public static void main(String[] args) {
        try {
            demonstrateUncertainTuples();
        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
