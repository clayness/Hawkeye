package kodkod.engine;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.engine.config.Options;
import kodkod.engine.fol2sat.Translation;
import kodkod.engine.fol2sat.Translator;
import kodkod.engine.satlab.SATAbortedException;
import kodkod.engine.satlab.SATSolver;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;
import kodkod.instance.Tuple;
import kodkod.instance.TupleSet;
import kodkod.util.ints.IntTreeSet;

/**
 * An iterator over all solutions of a model.
 *
 * @author Emina Torlak
 */
public final class SolutionIterator implements Iterator<Solution> {

    private Translation.Whole translation;
    private long              translTime;
    private int               trivial;
    private Formula           formula;
    private Bounds            bounds;
    private Options           options;
    boolean                   alternate;
    ArrayList<int[]>          prev_solutions;
    Translation.Whole         transl;
    boolean                   first;
    ArrayList<Integer>        same_atoms;
    ArrayList<Integer>        diff_atoms;
    ArrayList<String>         same_highlevel;
    ArrayList<String>         diff_highlevel;
    private String            PROJECT_DIR_PATH = System.getProperty("user.dir");
    private final String      HIDDEN_DIR_PATH  = "";

    /**
     * Constructs a solution iterator for the given formula, bounds, and options.
     */
    SolutionIterator(Formula formula, Bounds bounds, Options options) {
        this.translTime = System.currentTimeMillis();
        this.translation = Translator.translate(formula, bounds, options);
        this.translTime = System.currentTimeMillis() - translTime;
        this.formula = formula;
        this.bounds = bounds;
        this.options = options;
        this.trivial = 0;
        transl = translation;
        prev_solutions = new ArrayList<int[]>();
        alternate = true;
        first = true;
        same_atoms = new ArrayList<Integer>();
        diff_atoms = new ArrayList<Integer>();
        same_highlevel = new ArrayList<String>();
        diff_highlevel = new ArrayList<String>();
    }

    /**
     * Returns true if there is another solution.
     *
     * @see java.util.Iterator#hasNext()
     */
    @Override
    public boolean hasNext() {
        return translation != null;
    }

    /**
     * Returns the next solution if any.
     *
     * @see java.util.Iterator#next()
     */
    @Override
    public Solution next() {
        if (!hasNext())
            throw new NoSuchElementException();
        try {
            return translation.trivial() ? nextTrivialSolution() : nextNonTrivialSolution();
        } catch (SATAbortedException sae) {
            translation.cnf().free();
            throw new AbortedException(sae);
        }
    }

    /** @throws UnsupportedOperationException */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * Solves {@code translation.cnf} and adds the negation of the found model to
     * the set of clauses. The latter has the effect of forcing the solver to come
     * up with the next solution or return UNSAT. If
     * {@code this.translation.cnf.solve()} is false, sets {@code this.translation}
     * to null.
     *
     * @requires this.translation != null
     * @ensures this.translation.cnf is modified to eliminate the current solution
     *          from the set of possible solutions
     * @return current solution
     */


    private Set<Relation> nameToRelation(ArrayList<String> rels) {
        Map<String,Relation> name2rel = new HashMap<>();
        for (Relation rel : bounds.relations()) {
            if (name2rel.put(rel.name(), rel) != null) {
                throw new IllegalArgumentException("Name conflict");
            }
        }
        Set<Relation> result = new HashSet<>();
        for (String r : rels) {
            result.add(name2rel.get(r));
        }
        return result;
    }



    private Solution nextNonTrivialSolution() {

        final int primaryVars = transl.numPrimaryVariables();
        ArrayList<int[]> new_clauses = new ArrayList<int[]>();
        SATSolver cnf = transl.cnf();

        Set<Relation> rels_to_differ = nameToRelation(diff_highlevel);
        Set<Relation> rels_to_keep = nameToRelation(same_highlevel);

        if (!first) {
            /* Get the CNF variables tied to all the relations that must differ. */
            int num_vars_change = 0;
            ArrayList<IntTreeSet> cnf_vars_differ = new ArrayList<IntTreeSet>();
            for (Relation rel : rels_to_differ) {
                IntTreeSet vars = new IntTreeSet();
                vars.addAll(transl.primaryVariables(rel));
                num_vars_change += vars.size();
                cnf_vars_differ.add(vars);
            }

            /* Get the CNF variables tied to all the relations that must stay the same. */
            IntTreeSet cnf_vars_same = new IntTreeSet();
            for (Relation rel : rels_to_keep) {
                cnf_vars_same.addAll(transl.primaryVariables(rel));
            }

            /* Add cnf clauses that assert what stays the same value. */
            for (int i = 1; i <= primaryVars; i++) {
                /* Add cnf clauses that assert each atom must stay the same value. */
                if (same_atoms.contains(i)) {
                    int[] c = new int[1];
                    c[0] = cnf.valueOf(i) ? i : -i;
                    /* Add each as own clause to AND them together. */
                    new_clauses.add(c);
                }

                /* Add cnf clauses that assert each sig/relation must stay the same value. */
                if (cnf_vars_same.contains(i)) {
                    int[] c = new int[1];
                    c[0] = cnf.valueOf(i) ? i : -i;
                    /* Add each as own clause to AND them together. */
                    new_clauses.add(c);
                }
            }

            /*
             * Add cnf clauses that assert each relation must differ in value from previous.
             */
            /* Ensure broader a relation differs */
            for (IntTreeSet vars : cnf_vars_differ) {
                int[] original = new int[vars.size()];
                int j = 0;
                for (int i = 1; i <= primaryVars; i++) {
                    if (vars.contains(i)) {
                        original[j] = cnf.valueOf(i) ? -i : i;
                        j++;
                    }
                    //Each set of cnf variables related to a relation make their own clause
                    //Ensures all the relations differ, not just one
                    new_clauses.add(original);
                }
            }

            /* Ensure specific atoms differ */
            for (int i = 1; i <= primaryVars; i++) {
                if (diff_atoms.contains(i)) {
                    int[] c = new int[1];
                    c[0] = cnf.valueOf(i) ? -i : i;
                    /* Add each as own clause to AND them together. */
                    new_clauses.add(c);
                }
            }
        }

        transl = Translator.translate(formula, bounds, options);
        cnf = transl.cnf();

        for (int[] prev_negation : prev_solutions) {
            cnf.addClause(prev_negation);
        }

        for (int[] new_c : new_clauses) {
            cnf.addClause(new_c);
        }

        first = false;
        transl.options().reporter().solvingCNF(primaryVars, cnf.numberOfVariables(), cnf.numberOfClauses());

        final long startSolve = System.currentTimeMillis();
        final boolean isSat = cnf.solve();
        final long endSolve = System.currentTimeMillis();

        final Statistics stats = new Statistics(transl, translTime, endSolve - startSolve);
        final Solution sol;

        if (isSat) {
            // extract the current solution; can't use the sat(..) method
            // because it frees the sat solver
            sol = Solution.satisfiable(stats, transl.interpret());
            // add the negation of the current model to the solver
            //To start, keep all nodes the same

            /*
             * Stores the information to ensure the negation of the current solution is
             * accounted for.
             */
            int[] traditional = new int[primaryVars];
            for (int i = 1; i <= primaryVars; i++) {
                traditional[i - 1] = cnf.valueOf(i) ? -i : i;
            }
            prev_solutions.add(traditional);
            Instance inst = transl.interpret();

            HashMap<String,HashMap<Integer,String>> lit_to_tuple = new HashMap<String,HashMap<Integer,String>>();
            HashMap<String,HashMap<Integer,Integer>> index_to_lit = transl.index_to_lit;
            HashMap<Integer,String> rel_name = new HashMap<Integer,String>();

            for (Relation rel : inst.relations()) {
                for (Tuple tuple : inst.relationTuples().get(rel)) {
                    String pretty = tuple.toString().substring(1, tuple.toString().length() - 1);
                }
            }

            for (Relation rel : inst.relationTuples().keySet()) {
                for (Tuple tuple : inst.relationTuples().get(rel)) {
                    if (index_to_lit.containsKey(rel.name())) {
                        if (index_to_lit.get(rel.name()).containsKey(tuple.index())) {
                            String pretty = tuple.toString().substring(1, tuple.toString().length() - 1);
                            pretty = pretty.replaceAll("", "");
                            pretty = pretty.replaceAll(",", " ->");

                            rel_name.put(index_to_lit.get(rel.name()).get(tuple.index()), rel.name().replaceAll("\\$", ""));
                            if (!lit_to_tuple.containsKey(rel.name())) {
                                lit_to_tuple.put(rel.name(), new HashMap<Integer,String>());
                            }
                            lit_to_tuple.get(rel.name()).put(index_to_lit.get(rel.name()).get(tuple.index()), "The  atom \"" + pretty + "\"");
                        }
                    }

                }
            }

            PrintWriter writer;
            try {
                writer = new PrintWriter(HIDDEN_DIR_PATH + "lits.txt", "UTF-8");
                for (String rel : lit_to_tuple.keySet()) {
                    for (Integer index : lit_to_tuple.get(rel).keySet())
                        writer.println(rel_name.get(index) + "," + index + "," + lit_to_tuple.get(rel).get(index));
                }
                writer.close();
            } catch (FileNotFoundException | UnsupportedEncodingException e) { // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            sol = Solver.unsat(transl, stats); // this also frees up solver
                                              // resources, if any
            translation = null; // unsat, no more solutions
        }
        return sol;
    }

    /**
     * Returns the trivial solution corresponding to the trivial translation stored
     * in {@code this.translation}, and if {@code this.translation.cnf.solve()} is
     * true, sets {@code this.translation} to a new translation that eliminates the
     * current trivial solution from the set of possible solutions. The latter has
     * the effect of forcing either the translator or the solver to come up with the
     * next solution or return UNSAT. If {@code this.translation.cnf.solve()} is
     * false, sets {@code this.translation} to null.
     *
     * @requires this.translation != null
     * @ensures this.translation is modified to eliminate the current trivial
     *          solution from the set of possible solutions
     * @return current solution
     */
    private Solution nextTrivialSolution() {
        final Translation.Whole transl = this.translation;

        final Solution sol = Solver.trivial(transl, translTime); // this also
                                                                // frees up
                                                                // solver
                                                                // resources,
                                                                // if unsat

        if (sol.instance() == null) {
            translation = null; // unsat, no more solutions
        } else {
            trivial++;

            final Bounds bounds = transl.bounds();
            final Bounds newBounds = bounds.clone();
            final List<Formula> changes = new ArrayList<Formula>();

            for (Relation r : bounds.relations()) {
                final TupleSet lower = bounds.lowerBound(r);

                if (lower != bounds.upperBound(r)) { // r may change
                    if (lower.isEmpty()) {
                        changes.add(r.some());
                    } else {
                        final Relation rmodel = Relation.nary(r.name() + "_" + trivial, r.arity());
                        newBounds.boundExactly(rmodel, lower);
                        changes.add(r.eq(rmodel).not());
                    }
                }
            }

            // nothing can change => there can be no more solutions (besides the
            // current trivial one).
            // note that transl.formula simplifies to the constant true with
            // respect to
            // transl.bounds, and that newBounds is a superset of transl.bounds.
            // as a result, finding the next instance, if any, for
            // transl.formula.and(Formula.or(changes))
            // with respect to newBounds is equivalent to finding the next
            // instance of Formula.or(changes) alone.
            final Formula formula = changes.isEmpty() ? Formula.FALSE : Formula.or(changes);

            final long startTransl = System.currentTimeMillis();
            translation = Translator.translate(formula, newBounds, transl.options());
            translTime += System.currentTimeMillis() - startTransl;
        }
        return sol;
    }

    public ArrayList<Integer> getSameAtoms() {
        return same_atoms;
    }

    public void setSameAtoms(ArrayList<Integer> same_atoms) {
        this.same_atoms = same_atoms;
    }

    public ArrayList<Integer> getDiffAtoms() {
        return diff_atoms;
    }

    public void setDiffAtoms(ArrayList<Integer> diff_atoms) {
        this.diff_atoms = diff_atoms;
    }

    public ArrayList<String> getSameHighLevel() {
        return same_highlevel;
    }

    public void setSameHighLevel(ArrayList<String> same_highlevel) {
        this.same_highlevel = same_highlevel;
    }

    public ArrayList<String> getDiffHighLevel() {
        return diff_highlevel;
    }

    public void setDiffHighLevel(ArrayList<String> diff_highlevel) {
        this.diff_highlevel = diff_highlevel;
    }

    /**
     * Tuple-to-variable map for incremental enumeration: each optional tuple in a
     * relation's feasible region is assigned a primary SAT variable. Alloy passes
     * those ids as {@code same_atoms} / {@code diff_atoms} when requesting the
     * next model.
     *
     * @return deep copy of {@link kodkod.engine.fol2sat.Translation#index_to_lit};
     *         empty if translation is not initialized
     */
    public HashMap<String,HashMap<Integer,Integer>> getIndexToLit() {
        if (translation == null || translation.index_to_lit == null)
            return new HashMap<String,HashMap<Integer,Integer>>();
        HashMap<String,HashMap<Integer,Integer>> copy = new HashMap<String,HashMap<Integer,Integer>>();
        for (Map.Entry<String,HashMap<Integer,Integer>> e : translation.index_to_lit.entrySet()) {
            copy.put(e.getKey(), new HashMap<Integer,Integer>(e.getValue()));
        }
        return copy;
    }

}
