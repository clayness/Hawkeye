/* Alloy Analyzer 4 -- Copyright (c) 2006-2009, Felix Chang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF
 * OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package edu.mit.csail.sdg.translator;

import static edu.mit.csail.sdg.ast.Sig.NONE;
import static edu.mit.csail.sdg.ast.Sig.SEQIDX;
import static edu.mit.csail.sdg.ast.Sig.SIGINT;
import static edu.mit.csail.sdg.ast.Sig.STRING;
import static edu.mit.csail.sdg.ast.Sig.UNIV;
import static kodkod.engine.Solution.Outcome.UNSATISFIABLE;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.alloytools.alloy.core.AlloyCore;
import org.alloytools.util.table.Table;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.ConstList;
import edu.mit.csail.sdg.alloy4.ConstMap;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorAPI;
import edu.mit.csail.sdg.alloy4.ErrorFatal;
import edu.mit.csail.sdg.alloy4.ErrorSyntax;
import edu.mit.csail.sdg.alloy4.Pair;
import edu.mit.csail.sdg.alloy4.Pos;
import edu.mit.csail.sdg.alloy4.SafeList;
import edu.mit.csail.sdg.alloy4.TableView;
import edu.mit.csail.sdg.alloy4.UniqueNameGenerator;
import edu.mit.csail.sdg.alloy4.Util;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.ExprBinary;
import edu.mit.csail.sdg.ast.ExprConstant;
import edu.mit.csail.sdg.ast.ExprUnary;
import edu.mit.csail.sdg.ast.ExprVar;
import edu.mit.csail.sdg.ast.Func;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.ast.Sig.Field;
import edu.mit.csail.sdg.ast.Sig.PrimSig;
import edu.mit.csail.sdg.ast.Type;
import edu.mit.csail.sdg.translator.A4Options.SatSolver;
import kodkod.ast.BinaryExpression;
import kodkod.ast.BinaryFormula;
import kodkod.ast.Decl;
import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.IntExpression;
import kodkod.ast.Node;
import kodkod.ast.Relation;
import kodkod.ast.Variable;
import kodkod.ast.operator.ExprOperator;
import kodkod.ast.operator.FormulaOperator;
import kodkod.engine.CapacityExceededException;
import kodkod.engine.Evaluator;
import kodkod.engine.Proof;
import kodkod.engine.Solution;
import kodkod.engine.SolutionIterator;
import kodkod.engine.Solver;
import kodkod.engine.config.AbstractReporter;
import kodkod.engine.config.Options;
import kodkod.engine.config.Reporter;
import kodkod.engine.fol2sat.TranslationRecord;
import kodkod.engine.fol2sat.Translator;
import kodkod.engine.satlab.SATFactory;
import kodkod.engine.ucore.HybridStrategy;
import kodkod.engine.ucore.RCEStrategy;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;
import kodkod.instance.Tuple;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;
import kodkod.instance.Universe;
import kodkod.util.ints.IndexedEntry;

/**
 * This class stores a SATISFIABLE or UNSATISFIABLE solution. It is also used as
 * a staging area for the solver before generating the solution. Once solve()
 * has been called, then this object becomes immutable after that.
 */

public final class A4Solution implements Serializable {

    private static final long serialVersionUID = 42L;

    // ====== static immutable fields
    // ====================================================================//

    /**
     * The constant unary relation representing the smallest Int atom.
     */
    static final Relation KK_MIN    = Relation.unary("Int/min");

    /**
     * The constant unary relation representing the Int atom "0".
     */
    static final Relation KK_ZERO   = Relation.unary("Int/zero");

    /**
     * The constant unary relation representing the largest Int atom.
     */
    static final Relation KK_MAX    = Relation.unary("Int/max");

    /**
     * The constant binary relation representing the "next" relation from each Int
     * atom to its successor.
     */
    static final Relation KK_NEXT   = Relation.binary("Int/next");

    /**
     * The constant unary relation representing the set of all seq/Int atoms.
     */
    static final Relation KK_SEQIDX = Relation.unary("seq/Int");

    /**
     * The constant unary relation representing the set of all String atoms.
     */
    static final Relation KK_STRING = Relation.unary("String");

    // ====== immutable fields
    // ===========================================================================//

    /**
     * The original Alloy options that generated this solution.
     */
    private final A4Options        originalOptions;

    /**
     * The original Alloy command that generated this solution; can be "" if
     * unknown.
     */
    private final String           originalCommand;

    /** The bitwidth; always between 1 and 30. */
    private final int              bitwidth;

    /**
     * The maximum allowed sequence length; always between 0 and 2^(bitwidth-1)-1.
     */
    private final int              maxseq;

    /**
     * The maximum allowed number of loop unrolling and recursion level.
     */
    private final int              unrolls;

    /** The list of all atoms. */
    public final ConstList<String> kAtoms;

    /** The Kodkod TupleFactory object. */
    private final TupleFactory     factory;

    /** The set of all Int atoms; immutable. */
    private final TupleSet         sigintBounds;

    /** The set of all seq/Int atoms; immutable. */
    private final TupleSet         seqidxBounds;

    /** The set of all String atoms; immutable. */
    private final TupleSet         stringBounds;

    /** The Kodkod Solver object. */
    private final Solver           solver;

    // ====== mutable fields (immutable after solve() has been called)
    // ===================================//

    /** True iff the problem is solved. */
    private boolean                           solved      = false;

    /** The Kodkod Bounds object. */
    private Bounds                            bounds;

    /**
     * The list of Kodkod formulas; can be empty if unknown; once a solution is
     * solved we must not modify this anymore
     */
    private ArrayList<Formula>                formulas    = new ArrayList<Formula>();

    /** The list of known Alloy4 sigs. */
    private SafeList<Sig>                     sigs;

    /**
     * If solved==true and is satisfiable, then this is the list of known skolems.
     */
    private SafeList<ExprVar>                 skolems     = new SafeList<ExprVar>();

    /**
     * If solved==true and is satisfiable, then this is the list of actually used
     * atoms.
     */
    private SafeList<ExprVar>                 atoms       = new SafeList<ExprVar>();

    /**
     * If solved==true and is satisfiable, then this maps each Kodkod atom to a
     * short name.
     */
    public Map<Object,String>                 atom2name   = new LinkedHashMap<Object,String>();

    /**
     * If solved==true and is satisfiable, then this maps each Kodkod atom to its
     * most specific sig.
     */
    private Map<Object,PrimSig>               atom2sig    = new LinkedHashMap<Object,PrimSig>();

    /**
     * If solved==true and is satisfiable, then this is the Kodkod evaluator.
     */
    private Evaluator                         eval        = null;

    /** If not null, you can ask it to get another solution. */
    public Iterator<Solution>                 kEnumerator = null;

    /**
     * The map from each Sig/Field/Skolem/Atom to its corresponding Kodkod
     * expression.
     */
    private Map<Expr,Expression>              a2k;

    /**
     * The map from each String literal to its corresponding Kodkod expression.
     */
    private final ConstMap<String,Expression> s2k;

    /**
     * The map from each kodkod Formula to Alloy Expr or Alloy Pos (can be empty if
     * unknown)
     */
    private Map<Formula,Object>               k2pos;

    /**
     * The map from each Kodkod Relation to Alloy Type (can be empty or incomplete
     * if unknown)
     */
    private Map<Relation,Type>                rel2type;

    /**
     * The map from each Kodkod Variable to an Alloy Type and Alloy Pos.
     */
    private Map<Variable,Pair<Type,Pos>>      decl2type;

    // ===================================================================================================//

    /**
     * Construct a blank A4Solution containing just UNIV, SIGINT, SEQIDX, STRING,
     * and NONE as its only known sigs.
     *
     * @param originalCommand - the original Alloy command that generated this
     *            solution; can be "" if unknown
     * @param bitwidth - the bitwidth; must be between 1 and 30
     * @param maxseq - the maximum allowed sequence length; must be between 0 and
     *            (2^(bitwidth-1))-1
     * @param atoms - the set of atoms
     * @param rep - the reporter that will receive diagnostic and progress messages
     * @param opt - the Alloy options that will affect the solution and the solver
     * @param expected - whether the user expected an instance or not (1 means yes,
     *            0 means no, -1 means the user did not express an expectation)
     */
    A4Solution(String originalCommand, int bitwidth, int maxseq, Set<String> stringAtoms, Collection<String> atoms, final A4Reporter rep, A4Options opt, int expected) throws Err {
        this(originalCommand, bitwidth, maxseq, stringAtoms, atoms, rep, opt, expected, false);
    }

    /**
     * Construct a blank A4Solution containing just UNIV, SIGINT, SEQIDX, STRING,
     * and NONE as its only known sigs.
     *
     * @param originalCommand - the original Alloy command that generated this
     *            solution; can be "" if unknown
     * @param bitwidth - the bitwidth; must be between 1 and 30
     * @param maxseq - the maximum allowed sequence length; must be between 0 and
     *            (2^(bitwidth-1))-1
     * @param atoms - the set of atoms
     * @param rep - the reporter that will receive diagnostic and progress messages
     * @param opt - the Alloy options that will affect the solution and the solver
     * @param expected - whether the user expected an instance or not (1 means yes,
     *            0 means no, -1 means the user did not express an expectation)
     * @param hasUncertainTuples - whether this solution should support uncertain
     *            tuple storage
     */
    A4Solution(String originalCommand, int bitwidth, int maxseq, Set<String> stringAtoms, Collection<String> atoms, final A4Reporter rep, A4Options opt, int expected, boolean hasUncertainTuples) throws Err {
        opt = opt.dup();
        this.unrolls = opt.unrolls;
        this.sigs = new SafeList<Sig>(Arrays.asList(UNIV, SIGINT, SEQIDX, STRING, NONE));
        this.a2k = Util.asMap(new Expr[] {
                                          UNIV, SIGINT, SEQIDX, STRING, NONE
        }, Expression.INTS.union(KK_STRING), Expression.INTS, KK_SEQIDX, KK_STRING, Expression.NONE);
        this.k2pos = new LinkedHashMap<Formula,Object>();
        this.rel2type = new LinkedHashMap<Relation,Type>();
        this.decl2type = new LinkedHashMap<Variable,Pair<Type,Pos>>();
        this.originalOptions = opt;
        this.originalCommand = (originalCommand == null ? "" : originalCommand);
        this.bitwidth = bitwidth;
        this.maxseq = maxseq;
        this.hasUncertainTuples = hasUncertainTuples;

        // Initialize uncertain tuple containers based on flag
        if (hasUncertainTuples) {
            this.uncertainTupleCache = new LinkedHashMap<Expr,A4TupleSet>();
            this.uncertainTuples = new LinkedHashMap<Relation,Set<Tuple>>();
            this.uncertainTupleFrequency = new LinkedHashMap<Relation,Map<Tuple,Integer>>();
        } else {
            this.uncertainTupleCache = null;
            this.uncertainTuples = null;
            this.uncertainTupleFrequency = null;
        }
        if (bitwidth < 0)
            throw new ErrorSyntax("Cannot specify a bitwidth less than 0");
        if (bitwidth > 30)
            throw new ErrorSyntax("Cannot specify a bitwidth greater than 30");
        if (maxseq < 0)
            throw new ErrorSyntax("The maximum sequence length cannot be negative.");
        if (maxseq > 0 && maxseq > max())
            throw new ErrorSyntax("With integer bitwidth of " + bitwidth + ", you cannot have sequence length longer than " + max());
        if (atoms.isEmpty()) {
            atoms = new ArrayList<String>(1);
            atoms.add("<empty>");
        }
        kAtoms = ConstList.make(atoms);
        bounds = new Bounds(new Universe(kAtoms));
        factory = bounds.universe().factory();
        TupleSet sigintBounds = factory.noneOf(1);
        TupleSet seqidxBounds = factory.noneOf(1);
        TupleSet stringBounds = factory.noneOf(1);
        final TupleSet next = factory.noneOf(2);
        int min = min(), max = max();
        if (max >= min)
            for (int i = min; i <= max; i++) { // Safe since we know 1 <=
                                              // bitwidth <= 30
                Tuple ii = factory.tuple("" + i);
                TupleSet is = factory.range(ii, ii);
                bounds.boundExactly(i, is);
                sigintBounds.add(ii);
                if (i >= 0 && i < maxseq)
                    seqidxBounds.add(ii);
                if (i + 1 <= max)
                    next.add(factory.tuple("" + i, "" + (i + 1)));
                if (i == min)
                    bounds.boundExactly(KK_MIN, is);
                if (i == max)
                    bounds.boundExactly(KK_MAX, is);
                if (i == 0)
                    bounds.boundExactly(KK_ZERO, is);
            }
        this.sigintBounds = sigintBounds.unmodifiableView();
        this.seqidxBounds = seqidxBounds.unmodifiableView();
        bounds.boundExactly(KK_NEXT, next);
        bounds.boundExactly(KK_SEQIDX, this.seqidxBounds);
        Map<String,Expression> s2k = new HashMap<String,Expression>();
        for (String e : stringAtoms) {
            Relation r = Relation.unary("");
            Tuple t = factory.tuple(e);
            s2k.put(e, r);
            bounds.boundExactly(r, factory.range(t, t));
            stringBounds.add(t);
        }
        this.s2k = ConstMap.make(s2k);
        this.stringBounds = stringBounds.unmodifiableView();
        bounds.boundExactly(KK_STRING, this.stringBounds);
        int sym = (expected == 1 ? 0 : opt.symmetry);
        solver = new Solver();
        solver.options().setNoOverflow(opt.noOverflow);
        // solver.options().setFlatten(false); // added for now, since
        // multiplication and division circuit takes forever to flatten
        if (opt.solver.external() != null) {
            String ext = opt.solver.external();
            if (opt.solverDirectory.length() > 0 && ext.indexOf(File.separatorChar) < 0)
                ext = opt.solverDirectory + File.separatorChar + ext;
            try {
                File tmp = File.createTempFile("tmp", ".cnf", new File(opt.tempDirectory));
                tmp.deleteOnExit();
                solver.options().setSolver(SATFactory.externalFactory(ext, tmp.getAbsolutePath(), opt.solver.options()));
                // solver.options().setSolver(SATFactory.externalFactory(ext,
                // tmp.getAbsolutePath(), opt.solver.options()));
            } catch (IOException ex) {
                throw new ErrorFatal("Cannot create temporary directory.", ex);
            }
        } else if (opt.solver.equals(A4Options.SatSolver.LingelingJNI)) {
            solver.options().setSolver(SATFactory.Lingeling);
        } else if (opt.solver.equals(A4Options.SatSolver.PLingelingJNI)) {
            solver.options().setSolver(SATFactory.plingeling(4, null));
        } else if (opt.solver.equals(A4Options.SatSolver.GlucoseJNI)) {
            solver.options().setSolver(SATFactory.Glucose);
        } else if (opt.solver.equals(A4Options.SatSolver.Glucose41JNI)) {
            solver.options().setSolver(SATFactory.Glucose41);
        } else if (opt.solver.equals(A4Options.SatSolver.CryptoMiniSatJNI)) {
            solver.options().setSolver(SATFactory.CryptoMiniSat);
        } else if (opt.solver.equals(A4Options.SatSolver.MiniSatJNI)) {
            solver.options().setSolver(SATFactory.MiniSat);
        } else if (opt.solver.equals(A4Options.SatSolver.MiniSatProverJNI)) {
            sym = 20;
            solver.options().setSolver(SATFactory.MiniSatProver);
            solver.options().setLogTranslation(2);
            solver.options().setCoreGranularity(opt.coreGranularity);
        } else {
            solver.options().setSolver(SATFactory.DefaultSAT4J); // Even for
                                                                // "KK" and
                                                                // "CNF", we
                                                                // choose
                                                                // SAT4J
                                                                // here;
                                                                // later,
                                                                // just
                                                                // before
                                                                // solving,
                                                                // we'll
                                                                // change it
                                                                // to a
                                                                // Write2CNF
                                                                // solver
        }
        solver.options().setSymmetryBreaking(sym);
        solver.options().setSkolemDepth(opt.skolemDepth);
        solver.options().setBitwidth(bitwidth > 0 ? bitwidth : (int) Math.ceil(Math.log(atoms.size())) + 1);
        solver.options().setIntEncoding(Options.IntEncoding.TWOSCOMPLEMENT);
    }

    /**
     * Construct a new A4Solution that is the continuation of the old one, but with
     * the "next" instance.
     */
    private A4Solution(A4Solution old) throws Err {
        if (!old.solved)
            throw new ErrorAPI("This solution is not yet solved, so next() is not allowed.");
        if (old.kEnumerator == null)
            throw new ErrorAPI("This solution was not generated by an incremental SAT solver.\n" + "Solution enumeration is currently only implemented for MiniSat and SAT4J.");
        if (old.eval == null)
            throw new ErrorAPI("This solution is already unsatisfiable, so you cannot call next() to get the next solution.");
        Instance inst = old.kEnumerator.next().instance();
        unrolls = old.unrolls;
        originalOptions = old.originalOptions;
        originalCommand = old.originalCommand;
        bitwidth = old.bitwidth;
        maxseq = old.maxseq;
        hasUncertainTuples = old.hasUncertainTuples;
        kAtoms = old.kAtoms;
        factory = old.factory;
        sigintBounds = old.sigintBounds;
        seqidxBounds = old.seqidxBounds;
        stringBounds = old.stringBounds;
        solver = old.solver;
        bounds = old.bounds;
        formulas = old.formulas;
        sigs = old.sigs;
        kEnumerator = old.kEnumerator;
        k2pos = old.k2pos;
        rel2type = old.rel2type;
        decl2type = old.decl2type;
        if (inst != null) {
            eval = new Evaluator(inst, old.solver.options());
            a2k = new LinkedHashMap<Expr,Expression>();
            for (Map.Entry<Expr,Expression> e : old.a2k.entrySet())
                if (e.getKey() instanceof Sig || e.getKey() instanceof Field)
                    a2k.put(e.getKey(), e.getValue());
            UniqueNameGenerator un = new UniqueNameGenerator();
            rename(this, null, null, un);
            a2k = ConstMap.make(a2k);
        } else {
            skolems = old.skolems;
            eval = null;
            a2k = old.a2k;
        }
        s2k = old.s2k;

        // Copy uncertain tuple data if available
        if (hasUncertainTuples && old.uncertainTupleCache != null) {
            uncertainTupleCache = new LinkedHashMap<Expr,A4TupleSet>(old.uncertainTupleCache);
            uncertainTuples = new LinkedHashMap<Relation,Set<Tuple>>(old.uncertainTuples);
            uncertainTupleFrequency = new LinkedHashMap<Relation,Map<Tuple,Integer>>(old.uncertainTupleFrequency);
        } else {
            uncertainTupleCache = null;
            uncertainTuples = null;
            uncertainTupleFrequency = null;
        }

        atoms = atoms.dup();
        atom2name = ConstMap.make(atom2name);
        atom2sig = ConstMap.make(atom2sig);
        solved = true;
    }

    /**
     * Turn the solved flag to be true, and make all remaining fields immutable.
     */
    private void solved() {
        if (solved)
            return; // already solved
        bounds = bounds.clone().unmodifiableView();
        sigs = sigs.dup();
        skolems = skolems.dup();
        atoms = atoms.dup();
        atom2name = ConstMap.make(atom2name);
        atom2sig = ConstMap.make(atom2sig);
        a2k = ConstMap.make(a2k);
        k2pos = ConstMap.make(k2pos);
        rel2type = ConstMap.make(rel2type);
        decl2type = ConstMap.make(decl2type);

        // Make uncertain tuple data immutable if present
        if (hasUncertainTuples && uncertainTupleCache != null) {
            uncertainTupleCache = ConstMap.make(uncertainTupleCache);
            uncertainTuples = ConstMap.make(uncertainTuples);
            uncertainTupleFrequency = ConstMap.make(uncertainTupleFrequency);
        }

        solved = true;
    }

    // ===================================================================================================//

    /** Returns the bitwidth; always between 1 and 30. */
    public int getBitwidth() {
        return bitwidth;
    }

    /**
     * Returns the maximum allowed sequence length; always between 0 and
     * 2^(bitwidth-1)-1.
     */
    public int getMaxSeq() {
        return maxseq;
    }

    /**
     * Returns the largest allowed integer, or -1 if no integers are allowed.
     */
    public int max() {
        return Util.max(bitwidth);
    }

    /**
     * Returns the smallest allowed integer, or 0 if no integers are allowed
     */
    public int min() {
        return Util.min(bitwidth);
    }

    /**
     * Returns the maximum number of allowed loop unrolling or recursion level.
     */
    public int unrolls() {
        return unrolls;
    }

    // ===================================================================================================//

    /**
     * Returns the original Alloy file name that generated this solution; can be ""
     * if unknown.
     */
    public String getOriginalFilename() {
        return originalOptions.originalFilename;
    }

    /**
     * Returns the original command that generated this solution; can be "" if
     * unknown.
     */
    public String getOriginalCommand() {
        return originalCommand;
    }

    // ===================================================================================================//

    /**
     * Returns the Kodkod input used to generate this solution; returns "" if
     * unknown.
     */
    public String debugExtractKInput() {
        if (solved)
            return TranslateKodkodToJava.convert(Formula.and(formulas), bitwidth, kAtoms, bounds, atom2name);
        else
            return TranslateKodkodToJava.convert(Formula.and(formulas), bitwidth, kAtoms, bounds.unmodifiableView(), null);
    }

    // ===================================================================================================//

    /** Returns the Kodkod TupleFactory object. */
    TupleFactory getFactory() {
        return factory;
    }

    /**
     * Returns a modifiable copy of the Kodkod Bounds object.
     */
    Bounds getBounds() {
        return bounds.clone();
    }

    /**
     * Add a new relation with the given label and the given lower and upper bound.
     *
     * @param label - the label for the new relation; need not be unique
     * @param lower - the lowerbound; can be null if you want it to be the empty set
     * @param upper - the upperbound; cannot be null; must contain everything in
     *            lowerbound
     */
    Relation addRel(String label, TupleSet lower, TupleSet upper) throws ErrorFatal {
        if (solved)
            throw new ErrorFatal("Cannot add a Kodkod relation since solve() has completed.");
        Relation rel = Relation.nary(label, upper.arity());
        if (lower == upper) {
            bounds.boundExactly(rel, upper);
        } else if (lower == null) {
            bounds.bound(rel, upper);
        } else {
            if (lower.arity() != upper.arity())
                throw new ErrorFatal("Relation " + label + " must have same arity for lowerbound and upperbound.");
            bounds.bound(rel, lower, upper);
        }
        return rel;
    }

    /**
     * Add a new sig to this solution and associate it with the given expression
     * (and if s.isTopLevel then add this expression into Sig.UNIV). <br>
     * The expression must contain only constant Relations or Relations that are
     * already bound in this solution. <br>
     * (If the sig was already added by a previous call to addSig(), then this call
     * will return immediately without altering what it is associated with)
     */
    void addSig(Sig s, Expression expr) throws ErrorFatal {
        if (solved)
            throw new ErrorFatal("Cannot add an additional sig since solve() has completed.");
        if (expr.arity() != 1)
            throw new ErrorFatal("Sig " + s + " must be associated with a unary relational value.");
        if (a2k.containsKey(s))
            return;
        a2k.put(s, expr);
        sigs.add(s);
        if (s.isTopLevel())
            a2k.put(UNIV, a2k.get(UNIV).union(expr));
    }

    /**
     * Add a new field to this solution and associate it with the given expression.
     * <br>
     * The expression must contain only constant Relations or Relations that are
     * already bound in this solution. <br>
     * (If the field was already added by a previous call to addField(), then this
     * call will return immediately without altering what it is associated with)
     */
    void addField(Field f, Expression expr) throws ErrorFatal {
        if (solved)
            throw new ErrorFatal("Cannot add an additional field since solve() has completed.");
        if (expr.arity() != f.type().arity())
            throw new ErrorFatal("Field " + f + " must be associated with an " + f.type().arity() + "-ary relational value.");
        if (a2k.containsKey(f))
            return;
        a2k.put(f, expr);
    }

    /**
     * Add a new skolem to this solution and associate it with the given expression.
     * <br>
     * The expression must contain only constant Relations or Relations that are
     * already bound in this solution.
     */
    private ExprVar addSkolem(String label, Type type, Expression expr) throws Err {
        if (solved)
            throw new ErrorFatal("Cannot add an additional skolem since solve() has completed.");
        int a = type.arity();
        if (a < 1)
            throw new ErrorFatal("Skolem " + label + " must be associated with a relational value.");
        if (a != expr.arity())
            throw new ErrorFatal("Skolem " + label + " must be associated with an " + a + "-ary relational value.");
        ExprVar v = ExprVar.make(Pos.UNKNOWN, label, type);
        a2k.put(v, expr);
        skolems.add(v);
        return v;
    }

    /**
     * Returns an unmodifiable copy of the map from each Sig/Field/Skolem/Atom to
     * its corresponding Kodkod expression.
     */
    public ConstMap<Expr,Expression> a2k() {
        return ConstMap.make(a2k);
    }

    /**
     * Returns an unmodifiable copy of the map from each String literal to its
     * corresponding Kodkod expression.
     */
    ConstMap<String,Expression> s2k() {
        return s2k;
    }

    /**
     * Returns the corresponding Kodkod expression for the given Sig, or null if it
     * is not associated with anything.
     */
    Expression a2k(Sig sig) {
        return a2k.get(sig);
    }

    /**
     * Returns the corresponding Kodkod expression for the given Field, or null if
     * it is not associated with anything.
     */
    Expression a2k(Field field) {
        return a2k.get(field);
    }

    /**
     * Returns the corresponding Kodkod expression for the given Atom/Skolem, or
     * null if it is not associated with anything.
     */
    Expression a2k(ExprVar var) {
        return a2k.get(var);
    }

    /**
     * Returns the corresponding Kodkod expression for the given String constant, or
     * null if it is not associated with anything.
     */
    Expression a2k(String stringConstant) {
        return s2k.get(stringConstant);
    }

    /**
     * Returns the corresponding Kodkod expression for the given expression, or null
     * if it is not associated with anything.
     */
    Expression a2k(Expr expr) throws ErrorFatal {
        while (expr instanceof ExprUnary) {
            if (((ExprUnary) expr).op == ExprUnary.Op.NOOP) {
                expr = ((ExprUnary) expr).sub;
                continue;
            }
            if (((ExprUnary) expr).op == ExprUnary.Op.EXACTLYOF) {
                expr = ((ExprUnary) expr).sub;
                continue;
            }
            break;
        }
        if (expr instanceof ExprConstant && ((ExprConstant) expr).op == ExprConstant.Op.EMPTYNESS)
            return Expression.NONE;
        if (expr instanceof ExprConstant && ((ExprConstant) expr).op == ExprConstant.Op.STRING)
            return s2k.get(((ExprConstant) expr).string);
        if (expr instanceof Sig || expr instanceof Field || expr instanceof ExprVar)
            return a2k.get(expr);
        if (expr instanceof ExprBinary) {
            Expr a = ((ExprBinary) expr).left, b = ((ExprBinary) expr).right;
            switch (((ExprBinary) expr).op) {
                case ARROW :
                    return a2k(a).product(a2k(b));
                case PLUS :
                    return a2k(a).union(a2k(b));
                case MINUS :
                    return a2k(a).difference(a2k(b));
                // TODO: IPLUS, IMINUS???
                default :
                    // TODO log?
                    break;
            }
        }
        return null; // Current only UNION, PRODUCT, and DIFFERENCE of Sigs and
                    // Fields and ExprConstant.EMPTYNESS are allowed in a
                    // defined field's definition.
    }

    /**
     * Return a modifiable TupleSet representing a sound overapproximation of the
     * given expression.
     */
    TupleSet approximate(Expression expression) {
        return factory.setOf(expression.arity(), Translator.approximate(expression, bounds, solver.options()).denseIndices());
    }

    /**
     * Query the Bounds object to find the lower/upper bound; throws ErrorFatal if
     * expr is not Relation, nor a {union, product} of Relations.
     */
    TupleSet query(boolean findUpper, Expression expr, boolean makeMutable) throws ErrorFatal {
        if (expr == Expression.NONE)
            return factory.noneOf(1);
        if (expr == Expression.INTS)
            return makeMutable ? sigintBounds.clone() : sigintBounds;
        if (expr == KK_SEQIDX)
            return makeMutable ? seqidxBounds.clone() : seqidxBounds;
        if (expr == KK_STRING)
            return makeMutable ? stringBounds.clone() : stringBounds;
        if (expr instanceof Relation) {
            TupleSet ans = findUpper ? bounds.upperBound((Relation) expr) : bounds.lowerBound((Relation) expr);
            if (ans != null)
                return makeMutable ? ans.clone() : ans;
        } else if (expr instanceof BinaryExpression) {
            BinaryExpression b = (BinaryExpression) expr;
            if (b.op() == ExprOperator.UNION) {
                TupleSet left = query(findUpper, b.left(), true);
                TupleSet right = query(findUpper, b.right(), false);
                left.addAll(right);
                return left;
            } else if (b.op() == ExprOperator.PRODUCT) {
                TupleSet left = query(findUpper, b.left(), true);
                TupleSet right = query(findUpper, b.right(), false);
                return left.product(right);
            }
        }
        throw new ErrorFatal("Unknown expression encountered during bounds computation: " + expr);
    }

    /**
     * Shrink the bounds for the given relation; throws an exception if the new
     * bounds is not sameAs/subsetOf the old bounds.
     */
    void shrink(Relation relation, TupleSet lowerBound, TupleSet upperBound) throws Err {
        if (solved)
            throw new ErrorFatal("Cannot shrink a Kodkod relation since solve() has completed.");
        TupleSet oldL = bounds.lowerBound(relation);
        TupleSet oldU = bounds.upperBound(relation);
        if (oldU.containsAll(upperBound) && upperBound.containsAll(lowerBound) && lowerBound.containsAll(oldL)) {
            bounds.bound(relation, lowerBound, upperBound);
        } else {
            throw new ErrorAPI("Inconsistent bounds shrinking on relation: " + relation);
        }
    }

    // ===================================================================================================//

    /**
     * Returns true iff the problem has been solved and the result is satisfiable.
     */
    public boolean satisfiable() {
        return eval != null;
    }

    /**
     * Returns an unmodifiable copy of the list of all sigs in this solution's
     * model; always contains UNIV+SIGINT+SEQIDX+STRING+NONE and has no duplicates.
     */
    public SafeList<Sig> getAllReachableSigs() {
        return sigs.dup();
    }

    /**
     * Returns an unmodifiable copy of the list of all skolems if the problem is
     * solved and is satisfiable; else returns an empty list.
     */
    public Iterable<ExprVar> getAllSkolems() {
        return skolems.dup();
    }

    /**
     * Returns an unmodifiable copy of the list of all atoms if the problem is
     * solved and is satisfiable; else returns an empty list.
     */
    public Iterable<ExprVar> getAllAtoms() {
        return atoms.dup();
    }

    /**
     * Returns the short unique name corresponding to the given atom if the problem
     * is solved and is satisfiable; else returns atom.toString().
     */
    String atom2name(Object atom) {
        String ans = atom2name.get(atom);
        return ans == null ? atom.toString() : ans;
    }

    /**
     * Returns the most specific sig corresponding to the given atom if the problem
     * is solved and is satisfiable; else returns UNIV.
     */
    PrimSig atom2sig(Object atom) {
        PrimSig sig = atom2sig.get(atom);
        return sig == null ? UNIV : sig;
    }

    /** Caches eval(Sig) and eval(Field) results. */
    private Map<Expr,A4TupleSet>             evalCache               = new LinkedHashMap<Expr,A4TupleSet>();

    // ====== uncertain tuple support fields ===================================//

    /** True iff this A4Solution contains uncertain tuple data. */
    private final boolean                    hasUncertainTuples;

    /** Caches uncertain tuple results for expressions. */
    private Map<Expr,A4TupleSet>             uncertainTupleCache     = new LinkedHashMap<Expr,A4TupleSet>();

    /** Storage for uncertain tuple data per relation. */
    private Map<Relation,Set<Tuple>>         uncertainTuples         = new LinkedHashMap<Relation,Set<Tuple>>();

    /** Frequency data for uncertain tuples (for visualization purposes). */
    private Map<Relation,Map<Tuple,Integer>> uncertainTupleFrequency = new LinkedHashMap<Relation,Map<Tuple,Integer>>();

    /**
     * Return the A4TupleSet for the given sig (if solution not yet solved, or
     * unsatisfiable, or sig not found, then return an empty tupleset)
     */
    public A4TupleSet eval(Sig sig) {
        try {
            if (!solved || eval == null)
                return new A4TupleSet(factory.noneOf(1), this);
            A4TupleSet ans = evalCache.get(sig);
            if (ans != null)
                return ans;
            TupleSet ts = eval.evaluate((Expression) TranslateAlloyToKodkod.alloy2kodkod(this, sig));
            ans = new A4TupleSet(ts, this);
            evalCache.put(sig, ans);
            return ans;
        } catch (Err er) {
            return new A4TupleSet(factory.noneOf(1), this);
        }
    }

    /**
     * Return the A4TupleSet for the given field (if solution not yet solved, or
     * unsatisfiable, or field not found, then return an empty tupleset)
     */
    public A4TupleSet eval(Field field) {
        try {
            if (!solved || eval == null)
                return new A4TupleSet(factory.noneOf(field.type().arity()), this);
            A4TupleSet ans = evalCache.get(field);
            if (ans != null)
                return ans;
            TupleSet ts = eval.evaluate((Expression) TranslateAlloyToKodkod.alloy2kodkod(this, field));
            ans = new A4TupleSet(ts, this);
            evalCache.put(field, ans);
            return ans;
        } catch (Err er) {
            return new A4TupleSet(factory.noneOf(field.type().arity()), this);
        }
    }

    /**
     * If this solution is solved and satisfiable, evaluates the given expression
     * and returns an A4TupleSet, a java Integer, or a java Boolean.
     */
    public Object eval(Expr expr) throws Err {
        try {
            if (expr instanceof Sig)
                return eval((Sig) expr);
            if (expr instanceof Field)
                return eval((Field) expr);
            if (!solved)
                throw new ErrorAPI("This solution is not yet solved, so eval() is not allowed.");
            if (eval == null)
                throw new ErrorAPI("This solution is unsatisfiable, so eval() is not allowed.");
            if (expr.ambiguous && !expr.errors.isEmpty())
                expr = expr.resolve(expr.type(), null);
            if (!expr.errors.isEmpty())
                throw expr.errors.pick();
            Object result = TranslateAlloyToKodkod.alloy2kodkod(this, expr);
            if (result instanceof IntExpression)
                return eval.evaluate((IntExpression) result) + (eval.wasOverflow() ? " (OF)" : "");
            if (result instanceof Formula)
                return eval.evaluate((Formula) result);
            if (result instanceof Expression)
                return new A4TupleSet(eval.evaluate((Expression) result), this);
            throw new ErrorFatal("Unknown internal error encountered in the evaluator.");
        } catch (CapacityExceededException ex) {
            throw TranslateAlloyToKodkod.rethrow(ex);
        }
    }

    /**
     * Returns the Kodkod instance represented by this solution; throws an exception
     * if the problem is not yet solved or if it is unsatisfiable.
     */
    public Instance debugExtractKInstance() throws Err {
        if (!solved)
            throw new ErrorAPI("This solution is not yet solved, so instance() is not allowed.");
        if (eval == null)
            throw new ErrorAPI("This solution is unsatisfiable, so instance() is not allowed.");
        return eval.instance().unmodifiableView();
    }

    // ===================================================================================================//
    // UNCERTAIN TUPLE SUPPORT METHODS
    // ===================================================================================================//

    /**
     * Returns true if this A4Solution contains uncertain tuple data.
     */
    public boolean hasUncertainTuples() {
        return hasUncertainTuples;
    }

    /**
     * Returns uncertain tuples for a given relation. Returns empty set if no
     * uncertain data or relation not found.
     */
    public Set<Tuple> getUncertainTuples(Relation relation) {
        if (!hasUncertainTuples || uncertainTuples == null) {
            return new HashSet<Tuple>();
        }
        Set<Tuple> result = uncertainTuples.get(relation);
        return result != null ? new HashSet<Tuple>(result) : new HashSet<Tuple>();
    }

    /**
     * Returns uncertain tuple frequency for a given relation. Returns empty map if
     * no uncertain data.
     */
    public Map<Tuple,Integer> getUncertainTupleFrequency(Relation relation) {
        if (!hasUncertainTuples || uncertainTupleFrequency == null) {
            return new HashMap<Tuple,Integer>();
        }
        Map<Tuple,Integer> freq = uncertainTupleFrequency.get(relation);
        return freq != null ? new HashMap<Tuple,Integer>(freq) : new HashMap<Tuple,Integer>();
    }

    /**
     * Returns A4TupleSet for uncertain tuples of a given expression. Returns empty
     * tupleset if no uncertain data.
     */
    public A4TupleSet evalUncertain(Expr expr) {
        if (!hasUncertainTuples || uncertainTupleCache == null) {
            return new A4TupleSet(factory.noneOf(1), this);
        }
        A4TupleSet cached = uncertainTupleCache.get(expr);
        return cached != null ? cached : new A4TupleSet(factory.noneOf(1), this);
    }

    /**
     * Populates uncertain tuple data from a ClusterSolution. Can only be called if
     * hasUncertainTuples() returns true and solution is not yet solved.
     */
    public void populateUncertainTuples(ClusterSolution cluster) throws Err {
        if (!hasUncertainTuples) {
            throw new ErrorAPI("This A4Solution was not created with uncertain tuple support");
        }
        if (solved) {
            throw new ErrorAPI("Cannot populate uncertain tuples after solution is solved");
        }
        if (uncertainTuples == null || uncertainTupleFrequency == null) {
            throw new ErrorAPI("Uncertain tuple storage not properly initialized");
        }

        // Populate from cluster's uncertain tuple data
        for (Relation relation : bounds.relations()) {
            Set<Tuple> uncertain = cluster.getUncertainTuples(relation);
            Map<Tuple,Integer> frequency = cluster.getTupleFrequency(relation);

            if (!uncertain.isEmpty()) {
                uncertainTuples.put(relation, new HashSet<Tuple>(uncertain));

                // Store only uncertain tuple frequencies
                Map<Tuple,Integer> uncertainFreq = new HashMap<Tuple,Integer>();
                for (Tuple tuple : uncertain) {
                    uncertainFreq.put(tuple, frequency.getOrDefault(tuple, 0));
                }
                uncertainTupleFrequency.put(relation, uncertainFreq);
            }
        }
    }

    /**
     * Internal method to add uncertain tuples for a specific relation.
     */
    private void addUncertainTuples(Relation relation, Set<Tuple> tuples, Map<Tuple,Integer> frequencies) throws Err {
        if (!hasUncertainTuples) {
            throw new ErrorAPI("This A4Solution does not support uncertain tuples");
        }
        if (solved) {
            throw new ErrorAPI("Cannot modify uncertain tuples after solution is solved");
        }
        if (uncertainTuples == null || uncertainTupleFrequency == null) {
            throw new ErrorAPI("Uncertain tuple storage not properly initialized");
        }

        uncertainTuples.put(relation, new HashSet<Tuple>(tuples));
        if (frequencies != null) {
            uncertainTupleFrequency.put(relation, new HashMap<Tuple,Integer>(frequencies));
        }
    }

    // ===================================================================================================//

    /**
     * Maps a Kodkod formula to an Alloy Expr or Alloy Pos (or null if no such
     * mapping)
     */
    Object k2pos(Node formula) {
        return k2pos.get(formula);
    }

    /**
     * Associates the Kodkod formula to a particular Alloy Expr (if the Kodkod
     * formula is not already associated with an Alloy Expr or Alloy Pos)
     */
    Formula k2pos(Formula formula, Expr expr) throws Err {
        if (solved)
            throw new ErrorFatal("Cannot alter the k->pos mapping since solve() has completed.");
        if (formula == null || expr == null || k2pos.containsKey(formula))
            return formula;
        k2pos.put(formula, expr);
        if (formula instanceof BinaryFormula) {
            BinaryFormula b = (BinaryFormula) formula;
            if (b.op() == FormulaOperator.AND) {
                k2pos(b.left(), expr);
                k2pos(b.right(), expr);
            }
        }
        return formula;
    }

    /**
     * Associates the Kodkod formula to a particular Alloy Pos (if the Kodkod
     * formula is not already associated with an Alloy Expr or Alloy Pos)
     */
    Formula k2pos(Formula formula, Pos pos) throws Err {
        if (solved)
            throw new ErrorFatal("Cannot alter the k->pos mapping since solve() has completed.");
        if (formula == null || pos == null || pos == Pos.UNKNOWN || k2pos.containsKey(formula))
            return formula;
        k2pos.put(formula, pos);
        if (formula instanceof BinaryFormula) {
            BinaryFormula b = (BinaryFormula) formula;
            if (b.op() == FormulaOperator.AND) {
                k2pos(b.left(), pos);
                k2pos(b.right(), pos);
            }
        }
        return formula;
    }

    // ===================================================================================================//

    /**
     * Associates the Kodkod relation to a particular Alloy Type (if it is not
     * already associated with something)
     */
    void kr2type(Relation relation, Type newType) throws Err {
        if (solved)
            throw new ErrorFatal("Cannot alter the k->type mapping since solve() has completed.");
        if (!rel2type.containsKey(relation))
            rel2type.put(relation, newType);
    }

    /**
     * Remove all mapping from Kodkod relation to Alloy Type.
     */
    void kr2typeCLEAR() throws Err {
        if (solved)
            throw new ErrorFatal("Cannot clear the k->type mapping since solve() has completed.");
        rel2type.clear();
    }

    // ===================================================================================================//

    /** Caches a constant pair of Type.EMPTY and Pos.UNKNOWN */
    private Pair<Type,Pos> cachedPAIR = null;

    /**
     * Maps a Kodkod variable to an Alloy Type and Alloy Pos (if no association
     * exists, it will return (Type.EMPTY , Pos.UNKNOWN)
     */
    public Pair<Type,Pos> kv2typepos(Variable var) {
        Pair<Type,Pos> ans = decl2type.get(var);
        if (ans != null)
            return ans;
        if (cachedPAIR == null)
            cachedPAIR = new Pair<Type,Pos>(Type.EMPTY, Pos.UNKNOWN);
        return cachedPAIR;
    }

    /**
     * Associates the Kodkod variable to a particular Alloy Type and Alloy Pos (if
     * it is not already associated with something)
     */
    void kv2typepos(Variable var, Type type, Pos pos) throws Err {
        if (solved)
            throw new ErrorFatal("Cannot alter the k->type mapping since solve() has completed.");
        if (type == null)
            type = Type.EMPTY;
        if (pos == null)
            pos = Pos.UNKNOWN;
        if (!decl2type.containsKey(var))
            decl2type.put(var, new Pair<Type,Pos>(type, pos));
    }

    // ===================================================================================================//

    /**
     * Add the given formula to the list of Kodkod formulas, and associate it with
     * the given Pos object (pos can be null if unknown).
     */
    void addFormula(Formula newFormula, Pos pos) throws Err {
        if (solved)
            throw new ErrorFatal("Cannot add an additional formula since solve() has completed.");
        if (formulas.size() > 0 && formulas.get(0) == Formula.FALSE)
            return; // If one formula is false, we don't need the others
        if (newFormula == Formula.FALSE)
            formulas.clear(); // If one formula is false, we don't need the
                             // others
        formulas.add(newFormula);
        if (pos != null && pos != Pos.UNKNOWN)
            k2pos(newFormula, pos);
    }

    /**
     * Add the given formula to the list of Kodkod formulas, and associate it with
     * the given Expr object (expr can be null if unknown)
     */
    void addFormula(Formula newFormula, Expr expr) throws Err {
        if (solved)
            throw new ErrorFatal("Cannot add an additional formula since solve() has completed.");
        if (formulas.size() > 0 && formulas.get(0) == Formula.FALSE)
            return; // If one formula is false, we don't need the others
        if (newFormula == Formula.FALSE)
            formulas.clear(); // If one formula is false, we don't need the
                             // others
        formulas.add(newFormula);
        if (expr != null)
            k2pos(newFormula, expr);
    }

    // ===================================================================================================//

    /**
     * Helper class that wraps an iterator up where it will pre-fetch the first
     * element (note: it will not prefetch subsequent elements).
     */
    public static final class Peeker<T> implements Iterator<T> {

        /** The encapsulated iterator. */
        private Iterator<T> iterator;
        /** True iff we have captured the first element. */
        private boolean     hasFirst;
        /**
         * If hasFirst is true, then this is the captured first element.
         */
        private T           first;

        /** Constructrs a Peeker object. */
        private Peeker(Iterator<T> it) {
            iterator = it;
            if (it.hasNext()) {
                hasFirst = true;
                first = it.next();
            }
        }

        public void setDiffAtoms(ArrayList<Integer> diff) {
            ((SolutionIterator) iterator).setDiffAtoms(diff);
        }

        public void setSameAtoms(ArrayList<Integer> same) {
            ((SolutionIterator) iterator).setSameAtoms(same);
        }

        public void setDiffHighlevel(ArrayList<String> diff) {
            ((SolutionIterator) iterator).setDiffHighLevel(diff);
        }

        public void setSameHighlevel(ArrayList<String> same) {
            ((SolutionIterator) iterator).setSameHighLevel(same);
        }


        /** {@inheritDoc} */
        @Override
        public boolean hasNext() {
            return hasFirst || iterator.hasNext();
        }

        /** {@inheritDoc} */
        @Override
        public T next() {
            if (hasFirst) {
                hasFirst = false;
                T ans = first;
                first = null;
                return ans;
            } else
                return iterator.next();
        }

        /** {@inheritDoc} */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    // ===================================================================================================//

    /**
     * Helper method to determine if a given binary relation is a total order over a
     * given unary relation.
     */
    private static List<Tuple> isOrder(TupleSet b, TupleSet u) {
        // Size check
        final int n = u.size();
        final List<Tuple> list = new ArrayList<Tuple>(n);
        if (b.size() == 0 && n <= 1)
            return list;
        if (b.size() != n - 1)
            return null;
        // Find the starting element
        Tuple head = null;
        TupleSet right = b.project(1);
        for (Tuple x : u)
            if (!right.contains(x)) {
                head = x;
                break;
            }
        if (head == null)
            return null;
        final TupleFactory f = head.universe().factory();
        // Form the list
        list.add(head);
        while (true) {
            // Find head.next
            Tuple headnext = null;
            for (Tuple x : b)
                if (x.atom(0) == head.atom(0)) {
                    headnext = f.tuple(x.atom(1));
                    break;
                }
            // If we've reached the end of the chain, and indeed we've formed
            // exactly n elements (and all are in u), we're done
            if (headnext == null)
                return list.size() == n ? list : null;
            // If we've accumulated more than n elements, or if we reached an
            // element not in u, then we declare failure
            if (list.size() == n || !u.contains(headnext))
                return null;
            // Move on to the next step
            head = headnext;
            list.add(head);
        }
    }

    /**
     * Helper method that chooses a name for each atom based on its most specific
     * sig; (external caller should call this method with s==null and nexts==null)
     */
    private static void rename(A4Solution frame, PrimSig s, Map<Sig,List<Tuple>> nexts, UniqueNameGenerator un) throws Err {
        if (s == null) {
            for (ExprVar sk : frame.skolems)
                un.seen(sk.label);
            // Store up the skolems
            List<Object> skolems = new ArrayList<Object>();
            for (Map.Entry<Relation,Type> e : frame.rel2type.entrySet()) {
                Relation r = e.getKey();
                if (!frame.eval.instance().contains(r))
                    continue;
                Type t = e.getValue();
                if (t.arity() > r.arity())
                    continue; // Something is wrong; let's skip it
                while (t.arity() < r.arity())
                    t = UNIV.type().product(t);
                String n = Util.tail(r.name());
                while (n.length() > 0 && n.charAt(0) == '$')
                    n = n.substring(1);
                skolems.add(n);
                skolems.add(t);
                skolems.add(r);
            }
            // Find all suitable "next" or "prev" relations
            nexts = new LinkedHashMap<Sig,List<Tuple>>();
            for (Sig sig : frame.sigs)
                for (Field f : sig.getFields())
                    if (f.label.compareToIgnoreCase("next") == 0) {
                        List<List<PrimSig>> fold = f.type().fold();
                        if (fold.size() == 1) {
                            List<PrimSig> t = fold.get(0);
                            if (t.size() == 3 && t.get(0).isOne != null && t.get(1) == t.get(2) && !nexts.containsKey(t.get(1))) {
                                TupleSet set = frame.eval.evaluate(frame.a2k(t.get(1)));
                                if (set.size() <= 1)
                                    continue;
                                TupleSet next = frame.eval.evaluate(frame.a2k(t.get(0)).join(frame.a2k(f)));
                                List<Tuple> test = isOrder(next, set);
                                if (test != null)
                                    nexts.put(t.get(1), test);
                            } else if (t.size() == 2 && t.get(0) == t.get(1) && !nexts.containsKey(t.get(0))) {
                                TupleSet set = frame.eval.evaluate(frame.a2k(t.get(0)));
                                if (set.size() <= 1)
                                    continue;
                                TupleSet next = frame.eval.evaluate(frame.a2k(f));
                                List<Tuple> test = isOrder(next, set);
                                if (test != null)
                                    nexts.put(t.get(1), test);
                            }
                        }
                    }
            for (Sig sig : frame.sigs)
                for (Field f : sig.getFields())
                    if (f.label.compareToIgnoreCase("prev") == 0) {
                        List<List<PrimSig>> fold = f.type().fold();
                        if (fold.size() == 1) {
                            List<PrimSig> t = fold.get(0);
                            if (t.size() == 3 && t.get(0).isOne != null && t.get(1) == t.get(2) && !nexts.containsKey(t.get(1))) {
                                TupleSet set = frame.eval.evaluate(frame.a2k(t.get(1)));
                                if (set.size() <= 1)
                                    continue;
                                TupleSet next = frame.eval.evaluate(frame.a2k(t.get(0)).join(frame.a2k(f)).transpose());
                                List<Tuple> test = isOrder(next, set);
                                if (test != null)
                                    nexts.put(t.get(1), test);
                            } else if (t.size() == 2 && t.get(0) == t.get(1) && !nexts.containsKey(t.get(0))) {
                                TupleSet set = frame.eval.evaluate(frame.a2k(t.get(0)));
                                if (set.size() <= 1)
                                    continue;
                                TupleSet next = frame.eval.evaluate(frame.a2k(f).transpose());
                                List<Tuple> test = isOrder(next, set);
                                if (test != null)
                                    nexts.put(t.get(1), test);
                            }
                        }
                    }
            // Assign atom->name and atom->MostSignificantSig
            for (Tuple t : frame.eval.evaluate(Expression.INTS)) {
                frame.atom2sig.put(t.atom(0), SIGINT);
            }
            for (Tuple t : frame.eval.evaluate(KK_SEQIDX)) {
                frame.atom2sig.put(t.atom(0), SEQIDX);
            }
            for (Tuple t : frame.eval.evaluate(KK_STRING)) {
                frame.atom2sig.put(t.atom(0), STRING);
            }
            for (Sig sig : frame.sigs)
                if (sig instanceof PrimSig && !sig.builtin && ((PrimSig) sig).isTopLevel())
                    rename(frame, (PrimSig) sig, nexts, un);
            // These are redundant atoms that were not chosen to be in the final
            // instance
            int unused = 0;
            for (Tuple tuple : frame.eval.evaluate(Expression.UNIV)) {
                Object atom = tuple.atom(0);
                if (!frame.atom2sig.containsKey(atom)) {
                    frame.atom2name.put(atom, "unused" + unused);
                    unused++;
                }
            }
            // Add the skolems
            for (int num = skolems.size(), i = 0; i < num - 2; i = i + 3) {
                String n = (String) skolems.get(i);
                while (n.length() > 0 && n.charAt(0) == '$')
                    n = n.substring(1);
                Type t = (Type) skolems.get(i + 1);
                Relation r = (Relation) skolems.get(i + 2);
                frame.addSkolem(un.make("$" + n), t, r);
            }
            return;
        }
        for (PrimSig c : s.children())
            rename(frame, c, nexts, un);
        String signame = un.make(s.label.startsWith("this/") ? s.label.substring(5) : s.label);
        List<Tuple> list = new ArrayList<Tuple>();
        for (Tuple t : frame.eval.evaluate(frame.a2k(s)))
            list.add(t);
        List<Tuple> order = nexts.get(s);
        if (order != null && order.size() == list.size() && order.containsAll(list)) {
            list = order;
        }
        int i = 0;
        for (Tuple t : list) {
            if (frame.atom2sig.containsKey(t.atom(0)))
                continue; // This means one of the subsig has already claimed
                         // this atom.
            String x = signame + "$" + i;
            i++;
            frame.atom2sig.put(t.atom(0), s);
            frame.atom2name.put(t.atom(0), x);
            ExprVar v = ExprVar.make(null, x, s.type());
            TupleSet ts = t.universe().factory().range(t, t);
            Relation r = Relation.unary(x);
            frame.eval.instance().add(r, ts);
            frame.a2k.put(v, r);
            frame.atoms.add(v);
        }
    }

    // ===================================================================================================//

    /**
     * Solve for the solution if not solved already; if cmd==null, we will simply
     * use the lowerbound of each relation as its value.
     */
    public A4Solution solve(final A4Reporter rep, Command cmd, Simplifier simp, boolean tryBookExamples) throws Err, IOException {
        // If already solved, then return this object as is
        if (solved)
            return this;
        // If cmd==null, then all four arguments are ignored, and we simply use
        // the lower bound of each relation
        if (cmd == null) {
            Instance inst = new Instance(bounds.universe());
            for (int max = max(), i = min(); i <= max; i++) {
                Tuple it = factory.tuple("" + i);
                inst.add(i, factory.range(it, it));
            }
            for (Relation r : bounds.relations())
                inst.add(r, bounds.lowerBound(r));
            eval = new Evaluator(inst, solver.options());
            rename(this, null, null, new UniqueNameGenerator());
            solved();
            return this;
        }
        // Otherwise, prepare to do the solve...
        final A4Options opt = originalOptions;
        long time = System.currentTimeMillis();
        rep.debug("Simplifying the bounds...\n");
        if (opt.inferPartialInstance && simp != null && formulas.size() > 0 && !simp.simplify(rep, this, formulas))
            addFormula(Formula.FALSE, Pos.UNKNOWN);
        rep.translate(opt.solver.id(), bitwidth, maxseq, solver.options().skolemDepth(), solver.options().symmetryBreaking());
        Formula fgoal = Formula.and(formulas);
        rep.debug("Generating the solution...\n");
        kEnumerator = null;
        Solution sol = null;
        final Reporter oldReporter = solver.options().reporter();
        final boolean solved[] = new boolean[] {
                                                true
        };
        solver.options().setReporter(new AbstractReporter() { // Set up a
                                                             // reporter to
                                                             // catch the
                                                             // type+pos of
                                                             // skolems

            @Override
            public void skolemizing(Decl decl, Relation skolem, List<Decl> predecl) {
                try {
                    Type t = kv2typepos(decl.variable()).a;
                    if (t == Type.EMPTY)
                        return;
                    for (int i = (predecl == null ? -1 : predecl.size() - 1); i >= 0; i--) {
                        Type pp = kv2typepos(predecl.get(i).variable()).a;
                        if (pp == Type.EMPTY)
                            return;
                        t = pp.product(t);
                    }
                    kr2type(skolem, t);
                } catch (Throwable ex) {
                } // Exception here is not fatal
            }

            @Override
            public void solvingCNF(int primaryVars, int vars, int clauses) {
                if (solved[0])
                    return;
                else
                    solved[0] = true; // initially solved[0] is true, so we
                                     // won't report the # of vars/clauses
                if (rep != null)
                    rep.solve(primaryVars, vars, clauses);
            }
        });
        if (!opt.solver.equals(SatSolver.CNF) && !opt.solver.equals(SatSolver.KK) && tryBookExamples) { // try
                                                                                                       // book
                                                                                                       // examples
            A4Reporter r = AlloyCore.isDebug() ? rep : null;
            try {
                sol = BookExamples.trial(r, this, fgoal, solver, cmd.check);
            } catch (Throwable ex) {
                sol = null;
            }
        }
        solved[0] = false; // this allows the reporter to report the # of
                          // vars/clauses
        for (Relation r : bounds.relations()) {
            formulas.add(r.eq(r));
        } // Without this, kodkod refuses to grow unmentioned relations
        fgoal = Formula.and(formulas);
        // Now pick the solver and solve it!
        if (opt.solver.equals(SatSolver.KK)) {
            File tmpCNF = File.createTempFile("tmp", ".java", new File(opt.tempDirectory));
            String out = tmpCNF.getAbsolutePath();
            Util.writeAll(out, debugExtractKInput());
            rep.resultCNF(out);
            return null;
        }
        if (opt.solver.equals(SatSolver.CNF)) {
            File tmpCNF = File.createTempFile("tmp", ".cnf", new File(opt.tempDirectory));
            String out = tmpCNF.getAbsolutePath();
            solver.options().setSolver(WriteCNF.factory(out));
            try {
                sol = solver.solve(fgoal, bounds);
            } catch (WriteCNF.WriteCNFCompleted ex) {
                rep.resultCNF(out);
                return null;
            }
            // The formula is trivial (otherwise, it would have thrown an
            // exception)
            // Since the user wants it in CNF format, we manually generate a
            // trivially satisfiable (or unsatisfiable) CNF file.
            Util.writeAll(out, sol.instance() != null ? "p cnf 1 1\n1 0\n" : "p cnf 1 2\n1 0\n-1 0\n");
            rep.resultCNF(out);
            return null;
        }
        if (!solver.options().solver().incremental() /*
                                                      * || solver.options().solver()==SATFactory. ZChaffMincost
                                                      */) {
            if (sol == null)
                sol = solver.solve(fgoal, bounds);
        } else {
            kEnumerator = new Peeker<Solution>(solver.solveAll(fgoal, bounds));
            if (sol == null) {
                sol = kEnumerator.next();
            }
        }
        if (!solved[0])
            rep.solve(0, 0, 0);
        final Instance inst = sol.instance();
        // To ensure no more output during SolutionEnumeration
        solver.options().setReporter(oldReporter);
        // If unsatisfiable, then retreive the unsat core if desired
        if (inst == null && solver.options().solver() == SATFactory.MiniSatProver) {
            try {
                lCore = new LinkedHashSet<Node>();
                Proof p = sol.proof();
                if (sol.outcome() == UNSATISFIABLE) {
                    // only perform the minimization if it was UNSATISFIABLE,
                    // rather than TRIVIALLY_UNSATISFIABLE
                    int i = p.highLevelCore().size();
                    rep.minimizing(cmd, i);
                    if (opt.coreMinimization == 0)
                        try {
                            p.minimize(new RCEStrategy(p.log()));
                        } catch (Throwable ex) {
                        }
                    if (opt.coreMinimization == 1)
                        try {
                            p.minimize(new HybridStrategy(p.log()));
                        } catch (Throwable ex) {
                        }
                    rep.minimized(cmd, i, p.highLevelCore().size());
                }
                for (Iterator<TranslationRecord> it = p.core(); it.hasNext();) {
                    Object n = it.next().node();
                    if (n instanceof Formula)
                        lCore.add((Formula) n);
                }
                Map<Formula,Node> map = p.highLevelCore();
                hCore = new LinkedHashSet<Node>(map.keySet());
                hCore.addAll(map.values());
            } catch (Throwable ex) {
                lCore = hCore = null;
            }
        }
        // If satisfiable, then add/rename the atoms and skolems
        if (inst != null) {
            eval = new Evaluator(inst, solver.options());
            rename(this, null, null, new UniqueNameGenerator());
        }
        // report the result
        solved();
        time = System.currentTimeMillis() - time;
        //skipping xml generation here - Hritik
        /*
         * if (inst != null) rep.resultSAT(cmd, time, this); else rep.resultUNSAT(cmd,
         * time, this);
         */

        return this;
    }

    // ===================================================================================================//

    /**
     * Performs comprehensive clustering analysis on this A4Solution if it has an
     * enumerator. This method extracts the clustering logic that was previously
     * embedded in the solve() method.
     *
     * @param rep The A4Reporter for logging debug messages
     * @return List of ClusterSolution objects representing the clusters, or empty
     *         list if clustering fails
     * @throws Err if clustering encounters an error
     */
    public List<ClusterSolution> performComprehensiveClusteringAnalysis(final A4Reporter rep) throws Err {
        List<ClusterSolution> allClusters = new ArrayList<>();

        if (kEnumerator == null) {
            rep.debug("No solution enumerator available for clustering analysis.\n");
            return allClusters;
        }

        // Perform clustering analysis if enabled and kEnumerator is available
        if (originalOptions.enableClustering) {
            try {
                rep.debug("Running entropy-guided clustering analysis...\n");
                performEntropyGuidedClusteringAnalysis("entropy_guided_clustering.log");
                rep.debug("Entropy-guided clustering completed. Check entropy_guided_clustering_*.log files\n");

                // Extract solutions and create clusters for return
                List<A4Solution> solutions = extractSolutions(20);
                if (!solutions.isEmpty()) {
                    List<ClusterSolution> entropyClusters = ClusterMaker.createKMeansDefaultClusters(solutions, 4);
                    allClusters.addAll(entropyClusters);
                }
            } catch (Err e) {
                rep.debug("Entropy-guided clustering failed: " + e.getMessage() + "\n");
                // Fallback to legacy clustering
                try {
                    rep.debug("Falling back to legacy clustering...\n");
                    performClusteringAnalysis("legacy_clustering_analysis.log");

                    // Extract solutions and create clusters for return
                    List<A4Solution> solutions = extractSolutions(20);
                    if (!solutions.isEmpty()) {
                        List<ClusterSolution> legacyClusters = ClusterMaker.createClusters(solutions, 4, 5);
                        allClusters.addAll(legacyClusters);
                    }
                } catch (Err e2) {
                    rep.debug("Legacy clustering also failed: " + e2.getMessage() + "\n");
                }
            }
        }

        // Run hierarchical K-means clustering if kEnumerator is available (regardless of enableClustering flag)
        if (eval != null) { // Check if we have a satisfiable solution
            try {
                rep.debug("Running hierarchical K-means clustering...\n");
                performHierarchicalKMeansClusteringAnalysis("hierarchical_clustering.log");
                rep.debug("Hierarchical K-means clustering completed. Check hierarchical_clustering_*.log files\n");

                // Extract solutions and create clusters for return if not already done
                if (allClusters.isEmpty()) {
                    List<A4Solution> solutions = extractSolutions(20);
                    if (!solutions.isEmpty()) {
                        List<ClusterSolution> hierarchicalClusters = ClusterMaker.createKMeansDefaultClusters(solutions, 4);
                        allClusters.addAll(hierarchicalClusters);
                    }
                }
            } catch (Err e) {
                rep.debug("Hierarchical K-means clustering failed: " + e.getMessage() + "\n");
                // Fallback to standard K-means clustering
                try {
                    rep.debug("Falling back to standard K-means clustering...\n");
                    performKMeansClusteringAnalysis("kmeans_cluster_demo.log");
                    rep.debug("K-means clustering completed. Results written to kmeans_cluster_demo.log\n");

                    // Extract solutions and create clusters for return if not already done
                    if (allClusters.isEmpty()) {
                        List<A4Solution> solutions = extractSolutions(20);
                        if (!solutions.isEmpty()) {
                            List<ClusterSolution> kmeansClusters = ClusterMaker.createKMeansDefaultClusters(solutions, 4);
                            allClusters.addAll(kmeansClusters);
                        }
                    }
                } catch (Err e2) {
                    rep.debug("K-means clustering also failed: " + e2.getMessage() + "\n");
                    // Final fallback to basic demo
                    try {
                        rep.debug("Falling back to basic cluster demo...\n");
                        demoClusterSolution("basic_cluster_demo.log");

                        // Create a basic cluster for return
                        if (allClusters.isEmpty()) {
                            List<A4Solution> solutions = extractSolutions(10);
                            if (!solutions.isEmpty()) {
                                ClusterSolution basicCluster = new ClusterSolution(solutions, 1);
                                allClusters.add(basicCluster);
                            }
                        }
                    } catch (Err e3) {
                        rep.debug("Basic cluster demo also failed: " + e3.getMessage() + "\n");
                    }
                }
            }
        }

        rep.debug("Clustering analysis completed. Generated " + allClusters.size() + " clusters.\n");
        return allClusters;
    }

    /**
     * Creates a new A4Solution with uncertain tuple data from the provided
     * clusters. This replaces the A4Solution's instance data with data that
     * includes uncertain tuples.
     *
     * @param clusters The list of clusters containing uncertain tuple information
     * @param rep The A4Reporter for logging
     * @return A new A4Solution with uncertain tuple support, or the original
     *         solution if creation fails
     * @throws Err if uncertain tuple creation fails
     */
    public A4Solution createSolutionWithUncertainTuples(List<ClusterSolution> clusters, final A4Reporter rep) throws Err {
        if (clusters == null || clusters.isEmpty()) {
            rep.debug("No clusters provided for uncertain tuple creation. Returning original solution.\n");
            return this;
        }

        try {
            rep.debug("Creating A4Solution with uncertain tuples from " + clusters.size() + " clusters...\n");

            // Use the first cluster as the primary source, or merge data from all clusters
            ClusterSolution primaryCluster = clusters.get(2);

            // Create a new A4Solution with uncertain tuple support enabled
            A4Solution uncertainSolution = createWithUncertainTuples(primaryCluster, originalOptions, originalCommand);

            // If we have multiple clusters, we could potentially merge their uncertain data
            // For now, we'll use the primary cluster
            rep.debug("Successfully created A4Solution with uncertain tuples.\n");
            return uncertainSolution;

        } catch (Exception e) {
            rep.debug("Failed to create A4Solution with uncertain tuples: " + e.getMessage() + "\n");
            rep.debug("Returning original solution instead.\n");
            return this;
        }
    }

    // ===================================================================================================//

    /** This caches the toString() output. */
    private String toStringCache = null;

    /** Dumps the Kodkod solution into String. */
    @Override
    public String toString() {
        if (!solved)
            return "---OUTCOME---\nUnknown.\n";
        if (eval == null)
            return "---OUTCOME---\nUnsatisfiable.\n";
        String answer = toStringCache;
        if (answer != null)
            return answer;
        Instance sol = eval.instance();
        StringBuilder sb = new StringBuilder();
        sb.append("---INSTANCE---\n" + "integers={");
        boolean firstTuple = true;
        for (IndexedEntry<TupleSet> e : sol.intTuples()) {
            if (firstTuple)
                firstTuple = false;
            else
                sb.append(", ");
            // No need to print e.index() since we've ensured the Int atom's
            // String representation is always equal to ""+e.index()
            Object atom = e.value().iterator().next().atom(0);
            sb.append(atom2name(atom));
        }
        sb.append("}\n");
        try {
            for (Sig s : sigs) {
                sb.append(s.label).append("=").append(eval(s)).append("\n");
                for (Field f : s.getFields())
                    sb.append(s.label).append("<:").append(f.label).append("=").append(eval(f)).append("\n");
            }
            for (ExprVar v : skolems) {
                sb.append("skolem ").append(v.label).append("=").append(eval(v)).append("\n");
            }
            return toStringCache = sb.toString();
        } catch (Err er) {
            return toStringCache = ("<Evaluator error occurred: " + er + ">");
        }
    }

    // ===================================================================================================//

    /** If nonnull, it caches the result of calling "next()". */
    private A4Solution nextCache = null;

    /**
     * If this solution is UNSAT, return itself; else return the next solution
     * (which could be SAT or UNSAT).
     *
     * @throws ErrorAPI if the solver was not an incremental solver
     */
    public A4Solution next(ArrayList<Integer> same_atoms, ArrayList<Integer> diff_atoms, ArrayList<String> same_hl, ArrayList<String> diff_hl) throws Err {
        if (!solved)
            throw new ErrorAPI("This solution is not yet solved, so next() is not allowed.");
        if (eval == null)
            return this;
        if (nextCache == null) {
            ((Peeker) kEnumerator).setDiffAtoms(diff_atoms);
            ((Peeker) kEnumerator).setSameAtoms(same_atoms);
            ((Peeker) kEnumerator).setDiffHighlevel(diff_hl);
            ((Peeker) kEnumerator).setSameHighlevel(same_hl);
            nextCache = new A4Solution(this);
        }
        return nextCache;
    }

    /**
     * Returns true if this solution was generated by an incremental SAT solver.
     */
    public boolean isIncremental() {
        return kEnumerator != null;
    }

    // ===================================================================================================//

    /**
     * The low-level unsat core; null if it is not available.
     */
    private LinkedHashSet<Node> lCore      = null;

    /** This caches the result of lowLevelCore(). */
    private Set<Pos>            lCoreCache = null;

    /**
     * If this solution is unsatisfiable and its unsat core is available, then
     * return the core; else return an empty set.
     */
    public Set<Pos> lowLevelCore() {
        if (lCoreCache != null)
            return lCoreCache;
        Set<Pos> ans1 = new LinkedHashSet<Pos>();
        if (lCore != null)
            for (Node f : lCore) {
                Object y = k2pos(f);
                if (y instanceof Pos)
                    ans1.add((Pos) y);
                else if (y instanceof Expr)
                    ans1.add(((Expr) y).span());
            }
        return lCoreCache = Collections.unmodifiableSet(ans1);
    }

    // ===================================================================================================//

    /**
     * The high-level unsat core; null if it is not available.
     */
    private LinkedHashSet<Node>     hCore      = null;

    /** This caches the result of highLevelCore(). */
    private Pair<Set<Pos>,Set<Pos>> hCoreCache = null;

    /**
     * If this solution is unsatisfiable and its unsat core is available, then
     * return the core; else return an empty set.
     */
    public Pair<Set<Pos>,Set<Pos>> highLevelCore() {
        if (hCoreCache != null)
            return hCoreCache;
        Set<Pos> ans1 = new LinkedHashSet<Pos>(), ans2 = new LinkedHashSet<Pos>();
        if (hCore != null)
            for (Node f : hCore) {
                Object x = k2pos(f);
                if (x instanceof Pos) {
                    // System.out.println("F: "+f+" at "+x+"\n");
                    // System.out.flush();
                    ans1.add((Pos) x);
                } else if (x instanceof Expr) {
                    Expr expr = (Expr) x;
                    Pos p = ((Expr) x).span();
                    ans1.add(p);
                    // System.out.println("F: "+f+" by
                    // "+p.x+","+p.y+"->"+p.x2+","+p.y2+" for "+x+"\n\n");
                    // System.out.flush();
                    for (Func func : expr.findAllFunctions())
                        ans2.add(func.getBody().span());
                }
            }
        return hCoreCache = new Pair<Set<Pos>,Set<Pos>>(Collections.unmodifiableSet(ans1), Collections.unmodifiableSet(ans2));
    }

    // ===================================================================================================//

    /** Helper method to write out a full XML file. */
    public void writeXML(String filename) throws Err {
        writeXML(filename, null, null);
    }

    /** Helper method to write out a full XML file. */
    public void writeXML(String filename, Iterable<Func> macros) throws Err {
        writeXML(filename, macros, null);
    }

    /** Helper method to write out a full XML file. */
    public void writeXML(String filename, Iterable<Func> macros, Map<String,String> sourceFiles) throws Err {
        try (PrintWriter out = new PrintWriter(filename, "UTF-8")) {
            writeXML(out, macros, sourceFiles);
            if (!Util.close(out))
                throw new ErrorFatal("Error writing the solution XML file.");
        } catch (IOException ex) {
            throw new ErrorFatal("Error writing the solution XML file.", ex);
        }
    }

    /** Helper method to write out a full XML file. */
    public void writeXML(A4Reporter rep, String filename, Iterable<Func> macros, Map<String,String> sourceFiles) throws Err {
        try (PrintWriter out = new PrintWriter(filename, "UTF-8")) {
            writeXML(rep, out, macros, sourceFiles);
            if (!Util.close(out))
                throw new ErrorFatal("Error writing the solution XML file.");
        } catch (IOException ex) {
            throw new ErrorFatal("Error writing the solution XML file.", ex);
        }
    }

    /** Helper method to write out a full XML file. */
    public void writeXML(PrintWriter writer, Iterable<Func> macros, Map<String,String> sourceFiles) throws Err {
        A4SolutionWriter.writeInstance(null, this, writer, macros, sourceFiles);
        if (writer.checkError())
            throw new ErrorFatal("Error writing the solution XML file.");
    }

    /** Helper method to write out a full XML file. */
    public void writeXML(A4Reporter rep, PrintWriter writer, Iterable<Func> macros, Map<String,String> sourceFiles) throws Err {
        A4SolutionWriter.writeInstance(rep, this, writer, macros, sourceFiles);
        if (writer.checkError())
            throw new ErrorFatal("Error writing the solution XML file.");
    }

    public String format() {
        if (!solved)
            return "---OUTCOME---\nUnknown.\n";
        if (eval == null)
            return "---OUTCOME---\nUnsatisfiable.\n";

        Map<String,Table> table = TableView.toTable(this, eval.instance(), sigs);
        return String.join("\n", table.values().stream().map(x -> x.toString()).collect(Collectors.toSet()));
    }

    // ===================================================================================================//
    // CLUSTERING METHODS
    // ===================================================================================================//

    /**
     * Extracts a specified number of solutions from the kEnumerator.
     *
     * @param count Number of solutions to extract
     * @return List of A4Solution objects
     */
    public List<A4Solution> extractSolutions(int count) throws Err {
        if (kEnumerator == null) {
            throw new ErrorAPI("No solution enumerator available for clustering.");
        }

        List<A4Solution> solutions = new ArrayList<>();
        int extracted = 0;

        // Add the current solution if it's satisfiable
        if (satisfiable()) {
            solutions.add(this);
            extracted++;
        }

        // Extract additional solutions from the enumerator
        A4Solution currentSolution = this;
        while (extracted < count && kEnumerator.hasNext()) {
            try {
                // Use the public next() method with empty parameters for standard enumeration
                currentSolution = currentSolution.next(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
                if (currentSolution.satisfiable()) {
                    solutions.add(currentSolution);
                    extracted++;
                }
            } catch (Exception e) {
                // Stop if we can't get more solutions
                break;
            }
        }

        return solutions;
    }

    /**
     * Performs clustering analysis on solutions and logs the results.
     *
     * @param logFilename Name of the log file
     * @param solutionCount Number of solutions to extract (default 20)
     * @param clusterCount Number of clusters to create (default 4)
     * @param solutionsPerCluster Number of solutions per cluster (default 5)
     */
    public void performClusteringAnalysis(String logFilename, int solutionCount, int clusterCount, int solutionsPerCluster) throws Err {
        try {
            // Extract solutions
            List<A4Solution> solutions = extractSolutions(solutionCount);

            if (solutions.size() < clusterCount * solutionsPerCluster) {
                throw new ErrorAPI("Not enough solutions for clustering. Need " + (clusterCount * solutionsPerCluster) + " but only have " + solutions.size());
            }

            // Create clusters
            List<ClusterSolution> clusters = ClusterMaker.createClusters(solutions, clusterCount, solutionsPerCluster);

            // Log analysis for each cluster
            for (ClusterSolution cluster : clusters) {
                cluster.logClusterAnalysis(logFilename);
            }

        } catch (Exception e) {
            throw new ErrorAPI("Error during clustering analysis: " + e.getMessage());
        }
    }

    /**
     * Performs clustering analysis with default parameters (20 solutions, 4
     * clusters of 5 each).
     *
     * @param logFilename Name of the log file
     */
    public void performClusteringAnalysis(String logFilename) throws Err {
        performClusteringAnalysis(logFilename, 20, 4, 5);
    }

    /**
     * Performs K-means clustering analysis with relational distance metrics.
     *
     * @param logFilename Name of the log file
     * @param solutionCount Number of solutions to extract
     * @param clusterCount Number of clusters to create
     */
    public void performKMeansClusteringAnalysis(String logFilename, int solutionCount, int clusterCount) throws Err {
        try {
            // Extract solutions
            List<A4Solution> solutions = extractSolutions(solutionCount);

            if (solutions.size() < clusterCount) {
                throw new ErrorAPI("Not enough solutions for K-means clustering. Need at least " + clusterCount + " but only have " + solutions.size());
            }

            // Create clusters using K-means
            List<ClusterSolution> clusters = ClusterMaker.createKMeansDefaultClusters(solutions, clusterCount);

            // Log analysis for each cluster
            for (ClusterSolution cluster : clusters) {
                cluster.logClusterAnalysis(logFilename);
            }

        } catch (Exception e) {
            throw new ErrorAPI("Error during K-means clustering analysis: " + e.getMessage());
        }
    }

    /**
     * Performs K-means clustering analysis with default parameters.
     *
     * @param logFilename Name of the log file
     */
    public void performKMeansClusteringAnalysis(String logFilename) throws Err {
        performKMeansClusteringAnalysis(logFilename, 20, 4);
    }

    /**
     * Performs hierarchical K-means clustering analysis where parent clusters are
     * used to generate child clusters with present tuples as lower bounds.
     *
     * @param logFilename Base name for log files
     * @param solutionCount Number of solutions to extract for parent clusters
     * @param clusterCount Number of clusters to create at each level
     */
    public void performHierarchicalKMeansClusteringAnalysis(String logFilename, int solutionCount, int clusterCount) throws Err {
        if (!isIncremental()) {
            throw new ErrorAPI("Hierarchical clustering requires incremental SAT solver");
        }

        try {
            // Create main log file for hierarchical clustering progress
            String mainLogFile = logFilename.replace(".log", "_hierarchical_main.log");

            try (PrintWriter mainWriter = new PrintWriter(new FileWriter(mainLogFile))) {
                mainWriter.println("=== HIERARCHICAL K-MEANS CLUSTERING STARTED ===");
                mainWriter.println("Parent level: " + solutionCount + " solutions, " + clusterCount + " clusters");

                // Phase 1: Create parent clusters using standard K-means
                mainWriter.println("Phase 1: Creating parent clusters...");
                List<A4Solution> parentSolutions = extractSolutions(solutionCount);

                if (parentSolutions.size() < clusterCount) {
                    throw new ErrorAPI("Not enough solutions for hierarchical K-means clustering. Need at least " + clusterCount + " but only have " + parentSolutions.size());
                }

                // Create parent clusters using K-means
                List<ClusterSolution> parentClusters = ClusterMaker.createKMeansDefaultClusters(parentSolutions, clusterCount);

                // Log parent cluster analysis
                String parentLogFile = logFilename.replace(".log", "_parent.log");
                mainWriter.println("Logging parent clusters to: " + parentLogFile);

                for (ClusterSolution parentCluster : parentClusters) {
                    parentCluster.logClusterAnalysis(parentLogFile);
                }

                mainWriter.println("Phase 1 completed. Parent clusters logged to: " + parentLogFile);

                // Phase 2: Create child clusters for each parent cluster
                mainWriter.println("Phase 2: Creating child clusters...");
                int totalChildClusters = 0;

                for (int parentIndex = 0; parentIndex < parentClusters.size(); parentIndex++) {
                    ClusterSolution parentCluster = parentClusters.get(parentIndex);
                    String parentClusterName = getClusterName(parentIndex); // A, B, C, D

                    mainWriter.println("Processing parent cluster " + parentClusterName + " (ID: " + parentCluster.getClusterId() + ")...");

                    try {
                        // Create child clusters for this parent
                        List<ClusterSolution> childClusters = createChildClustersFromParent(parentCluster, solutionCount, clusterCount, parentClusterName, mainWriter);

                        // Log child cluster analysis
                        String childLogFile = logFilename.replace(".log", "_child_" + parentClusterName + ".log");
                        mainWriter.println("Logging child clusters for parent " + parentClusterName + " to: " + childLogFile);

                        for (ClusterSolution childCluster : childClusters) {
                            childCluster.logClusterAnalysis(childLogFile);
                            totalChildClusters++;
                        }

                        mainWriter.println("Parent cluster " + parentClusterName + " processed. " + childClusters.size() + " child clusters created.");

                    } catch (Exception e) {
                        mainWriter.println("ERROR: Failed to create child clusters for parent " + parentClusterName + ": " + e.getMessage());
                        // Continue with next parent cluster
                    }
                }

                mainWriter.println("=== HIERARCHICAL K-MEANS CLUSTERING COMPLETED ===");
                mainWriter.println("Total parent clusters: " + parentClusters.size());
                mainWriter.println("Total child clusters: " + totalChildClusters);
                mainWriter.println("Generated log files:");
                mainWriter.println("- " + parentLogFile + " (parent clusters)");
                for (int i = 0; i < parentClusters.size(); i++) {
                    String parentClusterName = getClusterName(i);
                    mainWriter.println("- " + logFilename.replace(".log", "_child_" + parentClusterName + ".log") + " (child clusters for parent " + parentClusterName + ")");
                }
            }

        } catch (Exception e) {
            throw new ErrorAPI("Error during hierarchical K-means clustering analysis: " + e.getMessage());
        }
    }

    /**
     * Performs hierarchical K-means clustering analysis with default parameters.
     *
     * @param logFilename Base name for log files
     */
    public void performHierarchicalKMeansClusteringAnalysis(String logFilename) throws Err {
        performHierarchicalKMeansClusteringAnalysis(logFilename, 20, 4);
    }

    /**
     * Performs entropy-guided recursive clustering analysis. This is the advanced
     * clustering approach that replicates the functionality from the other
     * repository but without binary serialization.
     *
     * @param logFilename Base name for log files (multiple files will be created)
     */
    public void performEntropyGuidedClusteringAnalysis(String logFilename) throws Err {
        if (!isIncremental()) {
            throw new ErrorAPI("Entropy-guided clustering requires incremental SAT solver");
        }

        try {
            EntropyGuidedClusteringOrchestrator.ClusteringAnalysisResult result = ClusterMaker.performEntropyGuidedClustering(this, logFilename);

            // Log summary
            try (PrintWriter summaryWriter = new PrintWriter(new FileWriter(logFilename.replace(".log", "_summary.log")))) {
                summaryWriter.println("=== ENTROPY-GUIDED CLUSTERING SUMMARY ===");
                summaryWriter.println(result.toString());
                summaryWriter.println();
                summaryWriter.println("Cluster Details:");

                for (ClusterSolution cluster : result.getAllClusters()) {
                    summaryWriter.println("Cluster " + cluster.getClusterId() + ": " + cluster.getSolutionCount() + " solutions, " + "entropy: " + String.format("%.4f", result.getClusterEntropies().getOrDefault(cluster.getClusterId(), 0.0)));
                }

                summaryWriter.println("=== END SUMMARY ===");
            }

        } catch (Exception e) {
            throw new ErrorAPI("Error during entropy-guided clustering analysis: " + e.getMessage());
        }
    }

    /**
     * Performs entropy-guided clustering analysis with custom parameters.
     *
     * @param logFilename Base name for log files
     * @param maxSolutionsPerCluster Maximum solutions per cluster before
     *            subdivision
     * @param numClusters Number of clusters for K-means (k value)
     * @param maxRecursionDepth Maximum depth for recursive clustering
     */
    public void performEntropyGuidedClusteringAnalysis(String logFilename, int maxSolutionsPerCluster, int numClusters, int maxRecursionDepth) throws Err {
        if (!isIncremental()) {
            throw new ErrorAPI("Entropy-guided clustering requires incremental SAT solver");
        }

        try {
            EntropyGuidedClusteringOrchestrator.ClusteringAnalysisResult result = ClusterMaker.performEntropyGuidedClustering(this, logFilename, maxSolutionsPerCluster, numClusters, maxRecursionDepth);

            // Log summary
            try (PrintWriter summaryWriter = new PrintWriter(new FileWriter(logFilename.replace(".log", "_summary.log")))) {
                summaryWriter.println("=== ENTROPY-GUIDED CLUSTERING SUMMARY ===");
                summaryWriter.println("Parameters: maxSolutions=" + maxSolutionsPerCluster + ", k=" + numClusters + ", maxDepth=" + maxRecursionDepth);
                summaryWriter.println(result.toString());
                summaryWriter.println();
                summaryWriter.println("Cluster Details:");

                for (ClusterSolution cluster : result.getAllClusters()) {
                    summaryWriter.println("Cluster " + cluster.getClusterId() + ": " + cluster.getSolutionCount() + " solutions, " + "entropy: " + String.format("%.4f", result.getClusterEntropies().getOrDefault(cluster.getClusterId(), 0.0)));
                }

                summaryWriter.println("=== END SUMMARY ===");
            }

        } catch (Exception e) {
            throw new ErrorAPI("Error during entropy-guided clustering analysis: " + e.getMessage());
        }
    }

    /**
     * Demo method that demonstrates ClusterSolution class working. This method
     * extracts solutions from the solution iterator, creates a single cluster, and
     * logs the cluster contents to a file.
     *
     * @param logFilename Name of the log file to write cluster analysis
     * @param solutionCount Number of solutions to extract for the cluster (default
     *            10)
     * @throws Err if clustering fails or insufficient solutions.
     */
    public void demoClusterSolution(String logFilename, int solutionCount) throws Err {
        System.out.println("=== ClusterSolution Demo Started ===");

        if (kEnumerator == null) {
            throw new ErrorAPI("No solution enumerator available for clustering demo. Solution must be incremental.");
        }

        if (!satisfiable()) {
            throw new ErrorAPI("Current solution is not satisfiable. Cannot demo clustering.");
        }

        try {
            System.out.println("Extracting " + solutionCount + " solutions from solution iterator...");

            // Extract solutions from the iterator
            List<A4Solution> solutions = extractSolutions(solutionCount);

            if (solutions.isEmpty()) {
                throw new ErrorAPI("No solutions extracted. Cannot create cluster.");
            }

            System.out.println("Successfully extracted " + solutions.size() + " solutions.");
            System.out.println("Creating a single cluster from all extracted solutions...");

            // Create a single cluster with all extracted solutions
            ClusterSolution cluster = new ClusterSolution(solutions, 1);

            System.out.println("Cluster created successfully with " + solutions.size() + " solutions.");
            System.out.println("Logging cluster analysis to file: " + logFilename);

            // Log the cluster analysis to file
            cluster.logClusterAnalysis(logFilename);

            // Also print some summary information to console
            System.out.println("\n=== Cluster Analysis Summary ===");
            System.out.println("Cluster ID: 1");
            System.out.println("Number of solutions in cluster: " + solutions.size());

            // Get first solution to examine relations
            if (!solutions.isEmpty()) {
                A4Solution firstSolution = solutions.get(0);
                if (firstSolution.eval != null && firstSolution.eval.instance() != null) {
                    int relationCount = firstSolution.eval.instance().relations().size();
                    System.out.println("Number of relations analyzed: " + relationCount);

                    // Print relation names for reference
                    System.out.println("Relations in analysis:");
                    for (kodkod.ast.Relation relation : firstSolution.eval.instance().relations()) {
                        System.out.println("  - " + relation.name());
                    }
                }
            }

            System.out.println("\nDetailed cluster analysis has been written to: " + logFilename);
            System.out.println("=== ClusterSolution Demo Completed Successfully ===");

        } catch (Exception e) {
            String errorMsg = "ClusterSolution demo failed: " + e.getMessage();
            System.err.println(errorMsg);
            throw new ErrorAPI(errorMsg);
        }
    }

    /**
     * Demo method with default parameters (extracts 10 solutions).
     *
     * @param logFilename Name of the log file to write cluster analysis
     * @throws Err if clustering fails or insufficient solutions
     */
    public void demoClusterSolution(String logFilename) throws Err {
        demoClusterSolution(logFilename, 20);
    }

    // ===================================================================================================//
    // Helper methods for hierarchical clustering
    // ===================================================================================================//

    /**
     * Creates child clusters from a parent cluster by using the parent's present
     * tuples as lower bounds for generating new solutions.
     *
     * @param parentCluster The parent cluster whose present tuples will be used as
     *            constraints
     * @param solutionCount Number of solutions to generate for child clustering
     * @param clusterCount Number of child clusters to create
     * @param parentClusterName Name of the parent cluster (for logging)
     * @return List of child clusters
     */
    private List<ClusterSolution> createChildClustersFromParent(ClusterSolution parentCluster, int solutionCount, int clusterCount, String parentClusterName, PrintWriter logWriter) throws Err {
        try {
            logWriter.println("  Creating constrained A4Solution for parent cluster " + parentClusterName + "...");

            // Create a new A4Solution with present tuples as lower bounds
            A4Solution constrainedSolution = createConstrainedSolution(parentCluster, logWriter);

            if (!constrainedSolution.satisfiable()) {
                throw new ErrorAPI("Constrained solution for parent cluster " + parentClusterName + " is unsatisfiable");
            }

            logWriter.println("  Extracting " + solutionCount + " solutions from constrained problem...");

            // Generate new solutions with the constraints
            List<A4Solution> childSolutions = constrainedSolution.extractSolutions(solutionCount);

            if (childSolutions.size() < clusterCount) {
                logWriter.println("  Warning: Only " + childSolutions.size() + " solutions available for parent " + parentClusterName + ", creating " + Math.min(childSolutions.size(), clusterCount) + " clusters");
                clusterCount = Math.min(childSolutions.size(), clusterCount);
            }

            logWriter.println("  Creating " + clusterCount + " child clusters from " + childSolutions.size() + " solutions...");

            // Create child clusters using K-means
            List<ClusterSolution> childClusters = ClusterMaker.createKMeansDefaultClusters(childSolutions, clusterCount);

            // Update cluster IDs to reflect hierarchical structure
            for (int i = 0; i < childClusters.size(); i++) {
                ClusterSolution childCluster = childClusters.get(i);
                // Update the cluster ID to reflect parent-child relationship
                String childClusterName = parentClusterName + (i + 1); // A1, A2, A3, A4
                updateClusterName(childCluster, childClusterName);
            }

            return childClusters;

        } catch (Exception e) {
            throw new ErrorAPI("Failed to create child clusters for parent " + parentClusterName + ": " + e.getMessage());
        }
    }

    /**
     * Creates a new A4Solution with additional constraints based on present tuples
     * from the parent cluster.
     *
     * @param parentCluster The parent cluster whose present tuples will be used as
     *            constraints
     * @return A new A4Solution with additional lower bound constraints
     */
    private A4Solution createConstrainedSolution(ClusterSolution parentCluster, PrintWriter logWriter) throws Err {
        try {
            logWriter.println("    Creating constrained A4Solution with present tuples as lower bounds...");

            // Create a new A4Solution instance with present tuples enforced as lower bounds
            A4Solution constrainedSolution = createConstrainedA4SolutionWithLowerBounds(parentCluster, logWriter);

            return constrainedSolution;

        } catch (Exception e) {
            throw new ErrorAPI("Failed to create constrained solution: " + e.getMessage());
        }
    }

    /**
     * Creates a clone of this A4Solution for hierarchical clustering purposes. This
     * creates a new A4Solution that can be modified with additional constraints
     *
     * @return A cloned A4Solution that can be modified.
     */
    private A4Solution cloneForHierarchicalClustering() throws Err {
        try {
            // Create a new A4Solution with the same parameters
            A4Solution clone = new A4Solution(originalCommand, bitwidth, maxseq, getStringAtoms(), kAtoms, new A4Reporter(), originalOptions, 1);

            // Copy the signatures
            for (Sig sig : sigs) {
                if (sig != UNIV && sig != SIGINT && sig != SEQIDX && sig != STRING && sig != NONE) {
                    clone.addSig(sig, a2k.get(sig));
                }
            }

            // Copy existing formulas
            for (Formula formula : formulas) {
                clone.addFormula(formula, (Pos) null);
            }

            return clone;

        } catch (Exception e) {
            throw new ErrorAPI("Failed to clone A4Solution for hierarchical clustering: " + e.getMessage());
        }
    }

    /**
     * Gets the string atoms from this solution.
     *
     * @return Set of string atoms
     */
    private Set<String> getStringAtoms() {
        Set<String> stringAtoms = new HashSet<>();
        for (Map.Entry<String,Expression> entry : s2k.entrySet()) {
            stringAtoms.add(entry.getKey());
        }
        return stringAtoms;
    }

    /**
     * Adds present tuples from the parent cluster as lower bounds to the
     * constrained solution.
     *
     * @param constrainedSolution The solution to add constraints to
     * @param parentCluster The parent cluster containing present tuples
     */
    private void addPresentTuplesAsLowerBounds(A4Solution constrainedSolution, ClusterSolution parentCluster, PrintWriter logWriter) throws Err {
        try {
            // Get present tuples from parent cluster
            Map<kodkod.ast.Relation,Set<kodkod.instance.Tuple>> presentTuples = parentCluster.getPresentTuplesMap();

            if (presentTuples.isEmpty()) {
                logWriter.println("    No present tuples found in parent cluster - no additional constraints added");
                return;
            }

            logWriter.println("    Adding " + presentTuples.size() + " relation constraints as lower bounds...");

            // For each relation with present tuples, add them as lower bounds
            for (Map.Entry<kodkod.ast.Relation,Set<kodkod.instance.Tuple>> entry : presentTuples.entrySet()) {
                kodkod.ast.Relation relation = entry.getKey();
                Set<kodkod.instance.Tuple> tuples = entry.getValue();

                if (!tuples.isEmpty()) {
                    // Create a TupleSet for the present tuples
                    TupleSet lowerBoundTuples = constrainedSolution.factory.noneOf(relation.arity());
                    for (kodkod.instance.Tuple tuple : tuples) {
                        lowerBoundTuples.add(tuple);
                    }

                    // Get current bounds for this relation
                    TupleSet currentUpper = constrainedSolution.bounds.upperBound(relation);
                    TupleSet currentLower = constrainedSolution.bounds.lowerBound(relation);

                    // Merge with existing lower bounds
                    TupleSet newLowerBounds;
                    if (currentLower != null) {
                        newLowerBounds = currentLower.clone();
                        newLowerBounds.addAll(lowerBoundTuples);
                    } else {
                        newLowerBounds = lowerBoundTuples;
                    }

                    // Update bounds with new lower bounds
                    if (currentUpper != null) {
                        constrainedSolution.bounds.bound(relation, newLowerBounds, currentUpper);
                    } else {
                        constrainedSolution.bounds.bound(relation, newLowerBounds, newLowerBounds);
                    }

                    logWriter.println("      Added " + tuples.size() + " tuples as lower bounds for relation: " + relation.name());
                }
            }

        } catch (Exception e) {
            throw new ErrorAPI("Failed to add present tuples as lower bounds: " + e.getMessage());
        }
    }

    /**
     * Gets the cluster name for the given index (A, B, C, D, etc.).
     *
     * @param index The cluster index (0-based)
     * @return The cluster name (A, B, C, D, etc.)
     */
    private String getClusterName(int index) {
        return String.valueOf((char) ('A' + index));
    }

    /**
     * Updates the cluster name for hierarchical display purposes. Note: This is for
     * logging purposes only.
     *
     * @param cluster The cluster to update
     * @param newName The new hierarchical name
     */
    private void updateClusterName(ClusterSolution cluster, String newName) {
        // This method is a placeholder for future enhancement
        // Currently ClusterSolution doesn't support name changes
        // The hierarchical naming is handled in the logging file names
    }

    /**
     * Creates a constrained A4Solution by adding present tuples from parent cluster
     * as formula constraints. This approach avoids universe compatibility issues.
     *
     * @param parentCluster The parent cluster containing present tuples
     * @param logWriter Writer for logging progress
     * @return A4Solution with additional constraints based on parent cluster
     */
    private A4Solution createConstrainedA4SolutionWithLowerBounds(ClusterSolution parentCluster, PrintWriter logWriter) throws Err {
        try {
            Map<kodkod.ast.Relation,Set<kodkod.instance.Tuple>> presentTuples = parentCluster.getPresentTuplesMap();
            Map<kodkod.ast.Relation,Set<kodkod.instance.Tuple>> absentTuples = parentCluster.getAbsentTuplesMap();

            logWriter.println("    Creating constrained A4Solution with present/absent tuple constraints");

            // Use a specific solution from the parent cluster as seed
            List<A4Solution> parentSolutions = parentCluster.getSolutions();

            if (parentSolutions.isEmpty()) {
                logWriter.println("    Warning: No parent solutions available, using original solution");
                return this;
            }

            // Use cluster ID to deterministically select different solutions for different parents
            int seedIndex = (parentCluster.getClusterId() - 1) % parentSolutions.size();
            A4Solution seedSolution = parentSolutions.get(seedIndex);

            logWriter.println("    Selected parent solution " + seedIndex + " from cluster " + parentCluster.getClusterId() + " as seed");
            logWriter.println("    Applying present tuples as lower bounds and removing absent tuples from upper bounds");

            // Log tuple constraint information
            int totalPresentTuples = 0;
            int totalAbsentTuples = 0;
            for (Map.Entry<kodkod.ast.Relation,Set<kodkod.instance.Tuple>> entry : presentTuples.entrySet()) {
                totalPresentTuples += entry.getValue().size();
            }
            for (Map.Entry<kodkod.ast.Relation,Set<kodkod.instance.Tuple>> entry : absentTuples.entrySet()) {
                totalAbsentTuples += entry.getValue().size();
            }
            logWriter.println("    Parent cluster constraints: " + totalPresentTuples + " present tuples, " + totalAbsentTuples + " absent tuples");

            // Create a constrained solution by modifying bounds
            if (seedSolution.isIncremental() && seedSolution.satisfiable()) {
                logWriter.println("    Creating new A4Solution with modified bounds based on parent cluster constraints");
                A4Solution constrainedSolution = createBoundsConstrainedSolution(seedSolution, presentTuples, absentTuples, logWriter);

                if (constrainedSolution != null && constrainedSolution.satisfiable()) {
                    logWriter.println("    Successfully created constrained solution - child clusters will be truly different");
                    return constrainedSolution;
                } else {
                    logWriter.println("    Constrained solution failed, using seed solution as fallback");
                    return seedSolution;
                }
            } else {
                logWriter.println("    Seed solution not incremental, using original solution");
                return this;
            }

        } catch (Exception e) {
            logWriter.println("    Error creating constrained solution: " + e.getMessage());
            logWriter.println("    Falling back to original solution");
            return this;
        }
    }

    /**
     * Creates a new A4Solution with bounds modified based on parent cluster
     * constraints. Present tuples are added as lower bounds, absent tuples are
     * removed from upper bounds.
     *
     * @param seedSolution The seed solution to base the new solution on
     * @param presentTuples Tuples that must be present (lower bounds)
     * @param absentTuples Tuples that must be absent (excluded from upper bounds)
     * @param logWriter Writer for logging progress
     * @return A new A4Solution with modified bounds, or null if creation fails.
     */
    private A4Solution createBoundsConstrainedSolution(A4Solution seedSolution, Map<kodkod.ast.Relation,Set<kodkod.instance.Tuple>> presentTuples, Map<kodkod.ast.Relation,Set<kodkod.instance.Tuple>> absentTuples, PrintWriter logWriter) throws Err {
        try {
            logWriter.println("      Creating new A4Solution with the same parameters but modified bounds");

            // Create a new A4Solution with the same parameters
            A4Solution constrainedSolution = new A4Solution(originalCommand, bitwidth, maxseq, extractStringAtoms(), kAtoms, new A4Reporter(), originalOptions, 1);

            // Copy signatures from seed solution
            for (Sig sig : seedSolution.sigs) {
                if (sig != UNIV && sig != SIGINT && sig != SEQIDX && sig != STRING && sig != NONE) {
                    constrainedSolution.addSig(sig, seedSolution.a2k.get(sig));
                }
            }

            // Copy original formulas
            for (Formula formula : seedSolution.formulas) {
                constrainedSolution.addFormula(formula, (Pos) null);
            }

            logWriter.println("      Applying bounds constraints from parent cluster");
            int boundsModified = 0;
            int presentConstraints = 0;
            int absentConstraints = 0;

            // Get all relations that need bounds modification
            Set<kodkod.ast.Relation> allRelevantRelations = new HashSet<>();
            allRelevantRelations.addAll(presentTuples.keySet());
            allRelevantRelations.addAll(absentTuples.keySet());

            for (kodkod.ast.Relation relation : allRelevantRelations) {
                // Find corresponding relation in the new solution
                kodkod.ast.Relation constrainedRelation = findCorrespondingRelation(constrainedSolution, relation);

                if (constrainedRelation != null) {
                    // Get current bounds from the new solution
                    TupleSet originalUpper = constrainedSolution.bounds.upperBound(constrainedRelation);
                    TupleSet originalLower = constrainedSolution.bounds.lowerBound(constrainedRelation);

                    if (originalUpper != null) {
                        // Start with the original upper bound
                        TupleSet newUpper = originalUpper.clone();
                        TupleSet newLower = originalLower != null ? originalLower.clone() : constrainedSolution.factory.noneOf(constrainedRelation.arity());

                        // Add present tuples as lower bounds
                        Set<kodkod.instance.Tuple> relationPresentTuples = presentTuples.get(relation);
                        if (relationPresentTuples != null && !relationPresentTuples.isEmpty()) {
                            TupleSet presentSet = createTupleSetFromParentTuples(constrainedSolution, constrainedRelation, relationPresentTuples, logWriter);
                            if (presentSet != null && !presentSet.isEmpty()) {
                                newLower.addAll(presentSet);
                                presentConstraints++;
                                logWriter.println("        Added " + presentSet.size() + " present tuples as lower bounds for " + constrainedRelation.name());
                            }
                        }

                        // Remove absent tuples from upper bounds
                        Set<kodkod.instance.Tuple> relationAbsentTuples = absentTuples.get(relation);
                        if (relationAbsentTuples != null && !relationAbsentTuples.isEmpty()) {
                            TupleSet absentSet = createTupleSetFromParentTuples(constrainedSolution, constrainedRelation, relationAbsentTuples, logWriter);
                            if (absentSet != null && !absentSet.isEmpty()) {
                                newUpper.removeAll(absentSet);
                                absentConstraints++;
                                logWriter.println("        Removed " + absentSet.size() + " absent tuples from upper bounds for " + constrainedRelation.name());
                            }
                        }

                        // Apply the modified bounds
                        if (newUpper.containsAll(newLower)) {
                            constrainedSolution.bounds.bound(constrainedRelation, newLower, newUpper);
                            boundsModified++;
                        } else {
                            logWriter.println("        Warning: Lower bounds not subset of upper bounds for " + constrainedRelation.name() + ", skipping");
                        }
                    }
                }
            }

            logWriter.println("      Applied constraints: " + presentConstraints + " present, " + absentConstraints + " absent, " + boundsModified + " bounds modified");

            // Solve the constrained problem
            logWriter.println("      Solving constrained problem...");
            constrainedSolution = constrainedSolution.solve(new A4Reporter(), null, null, false);

            if (constrainedSolution.satisfiable()) {
                logWriter.println("      Constrained solution is satisfiable with modified bounds");
                return constrainedSolution;
            } else {
                logWriter.println("      Warning: Constrained solution is unsatisfiable");
                return null;
            }

        } catch (Exception e) {
            logWriter.println("      Error creating bounds-constrained solution: " + e.getMessage());
            return null;
        }
    }

    /**
     * Creates a TupleSet from parent tuples by translating them to the constrained
     * solution's universe.
     *
     * @param constrainedSolution The solution with the target universe
     * @param constrainedRelation The relation in the constrained solution
     * @param parentTuples The tuples from the parent cluster
     * @param logWriter Writer for logging progress
     * @return TupleSet containing translated tuples, or null if translation fails
     */
    private TupleSet createTupleSetFromParentTuples(A4Solution constrainedSolution, kodkod.ast.Relation constrainedRelation, Set<kodkod.instance.Tuple> parentTuples, PrintWriter logWriter) {
        try {
            TupleSet result = constrainedSolution.factory.noneOf(constrainedRelation.arity());
            int translatedCount = 0;

            for (kodkod.instance.Tuple parentTuple : parentTuples) {
                kodkod.instance.Tuple translatedTuple = translateTupleToNewUniverse(parentTuple, constrainedSolution);
                if (translatedTuple != null) {
                    if (result.add(translatedTuple)) {
                        translatedCount++;
                    }
                }
            }

            if (translatedCount > 0) {
                return result;
            } else {
                return null;
            }

        } catch (Exception e) {
            logWriter.println("        Error creating tuple set: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extracts string atoms from this solution for creating a new A4Solution.
     *
     * @return Set of string atoms
     */
    private Set<String> extractStringAtoms() {
        Set<String> stringAtoms = new HashSet<>();
        for (Map.Entry<String,Expression> entry : s2k.entrySet()) {
            stringAtoms.add(entry.getKey());
        }
        return stringAtoms;
    }

    /**
     * Finds the corresponding relation in the constrained solution that matches the
     * parent relation by name and arity.
     *
     * @param constrainedSolution The new solution to search in
     * @param parentRelation The relation from the parent cluster
     * @return Corresponding relation in constrained solution, or null if not found
     */
    private kodkod.ast.Relation findCorrespondingRelation(A4Solution constrainedSolution, kodkod.ast.Relation parentRelation) {
        try {
            // Look for a relation with the same name and arity in the constrained solution
            for (kodkod.ast.Relation relation : constrainedSolution.bounds.relations()) {
                if (relation.name().equals(parentRelation.name()) && relation.arity() == parentRelation.arity()) {
                    return relation;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Creates a TupleSet for lower bounds based on parent tuples, translating them
     * to the constrained solution's universe.
     *
     * @param constrainedSolution The solution with the target universe
     * @param constrainedRelation The relation in the constrained solution
     * @param parentTuples The tuples from the parent cluster
     * @param logWriter Writer for logging progress
     * @return TupleSet containing translated tuples, or null if translation fails
     */
    private TupleSet createLowerBoundsFromParentTuples(A4Solution constrainedSolution, kodkod.ast.Relation constrainedRelation, Set<kodkod.instance.Tuple> parentTuples, PrintWriter logWriter) {
        try {
            TupleSet lowerBounds = constrainedSolution.factory.noneOf(constrainedRelation.arity());
            int translatedCount = 0;

            for (kodkod.instance.Tuple parentTuple : parentTuples) {
                // Translate the parent tuple to the constrained solution's universe
                kodkod.instance.Tuple translatedTuple = translateTupleToNewUniverse(parentTuple, constrainedSolution);
                if (translatedTuple != null) {
                    lowerBounds.add(translatedTuple);
                    translatedCount++;
                }
            }

            if (translatedCount > 0) {
                logWriter.println("        Translated " + translatedCount + "/" + parentTuples.size() + " tuples for relation " + constrainedRelation.name());
                return lowerBounds;
            } else {
                logWriter.println("        Warning: No tuples could be translated for relation " + constrainedRelation.name());
                return null;
            }

        } catch (Exception e) {
            logWriter.println("        Error creating lower bounds for " + constrainedRelation.name() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Translates a tuple from the parent universe to the constrained solution's
     * universe. This maps atom names to corresponding atoms in the new universe.
     *
     * @param parentTuple The tuple from the parent cluster
     * @param constrainedSolution The solution with the target universe
     * @return Translated tuple, or null if translation fails
     */
    private kodkod.instance.Tuple translateTupleToNewUniverse(kodkod.instance.Tuple parentTuple, A4Solution constrainedSolution) {
        try {
            // Extract atom names from the parent tuple
            Object[] atoms = new Object[parentTuple.arity()];
            for (int i = 0; i < parentTuple.arity(); i++) {
                Object atom = parentTuple.atom(i);
                // The atom should be a string that exists in both universes
                atoms[i] = atom.toString();
            }

            // Create a new tuple in the constrained solution's universe
            return constrainedSolution.factory.tuple(atoms);

        } catch (Exception e) {
            // If translation fails, return null
            return null;
        }
    }

    // ===================================================================================================//
    // UTILITY METHODS FOR UNCERTAIN TUPLE SUPPORT
    // ===================================================================================================//

    /**
     * Creates an A4Solution from a ClusterSolution with uncertain tuple data. This
     * creates a solution that represents the entire cluster with: - Current
     * instance contains only present tuples - Lower bounds = present tuples (must
     * be included) - Upper bounds = present tuples + uncertain tuples (can be
     * included) - Absent tuples are completely excluded from bounds - Structure
     * derived from ALL solutions in the cluster, not just one seed
     *
     * @param cluster The ClusterSolution containing uncertain tuple data
     * @param options The A4Options to use for the new solution
     * @param command The command string for the new solution
     * @return A new A4Solution with uncertain tuple data populated from the cluster
     */
    public static A4Solution createWithUncertainTuples(ClusterSolution cluster, A4Options options, String command) throws Err {
        if (cluster == null || cluster.getSolutions().isEmpty()) {
            throw new ErrorAPI("Cannot create A4Solution from empty or null cluster");
        }

        List<A4Solution> solutions = cluster.getSolutions();

        // Analyze ALL solutions to extract common parameters
        ClusterAnalysis analysis = analyzeClusterSolutions(solutions);

        // Create new solution with uncertain tuple support using cluster-wide analysis
        A4Solution newSolution = new A4Solution(command != null ? command : "cluster_" + cluster.getClusterId(), analysis.bitwidth, analysis.maxseq, analysis.stringAtoms, analysis.atoms, new A4Reporter(), options != null ? options : analysis.options, 1, true  // Enable uncertain tuple support
        );

        // Add signatures that are common across ALL solutions in the cluster
        addClusterSignatures(newSolution, solutions, analysis);

        // Add formulas that are common across ALL solutions in the cluster
        addClusterFormulas(newSolution, solutions, analysis);

        // Set up custom bounds based on cluster analysis
        setupClusterBasedBounds(newSolution, cluster);

        // Create instance with present tuples only
        createPresentTuplesInstance(newSolution, cluster);

        // Populate uncertain tuple data
        newSolution.populateUncertainTuples(cluster);

        return newSolution;
    }

    /**
     * Analyzes ALL solutions in a cluster to extract common characteristics
     */
    private static class ClusterAnalysis {

        int                 bitwidth;
        int                 maxseq;
        Set<String>         stringAtoms;
        Collection<String>  atoms;
        A4Options           options;
        Set<Sig>            commonSignatures;
        Set<Formula>        commonFormulas;
        Map<Sig,Expression> commonSigExpressions;
    }

    /**
     * Analyzes all solutions in the cluster to find common characteristics
     */
    private static ClusterAnalysis analyzeClusterSolutions(List<A4Solution> solutions) throws Err {
        if (solutions.isEmpty()) {
            throw new ErrorAPI("Cannot analyze empty solution list");
        }

        ClusterAnalysis analysis = new ClusterAnalysis();
        A4Solution first = solutions.get(0);

        // These should be the same across all solutions (they come from the same problem)
        analysis.bitwidth = first.getBitwidth();
        analysis.maxseq = first.getMaxSeq();
        analysis.stringAtoms = first.getStringAtoms();
        analysis.atoms = first.kAtoms;
        analysis.options = first.originalOptions;

        // Find signatures that exist in ALL solutions
        analysis.commonSignatures = new HashSet<Sig>();
        for (Sig sig : first.sigs) {
            analysis.commonSignatures.add(sig);
        }
        analysis.commonSigExpressions = new HashMap<Sig,Expression>();

        for (A4Solution solution : solutions) {
            // Keep only signatures that exist in this solution too.
            // Convert SafeList to HashSet for compatibility
            Set<Sig> solutionSigs = new HashSet<Sig>();
            for (Sig sig : solution.sigs) {
                solutionSigs.add(sig);
            }
            analysis.commonSignatures.retainAll(solutionSigs);
        }

        // For common signatures, find expressions that are consistent
        for (Sig sig : analysis.commonSignatures) {
            if (sig != UNIV && sig != SIGINT && sig != SEQIDX && sig != STRING && sig != NONE) {
                Expression expr = first.a2k.get(sig);
                boolean consistent = true;

                // Check if this signature has the same expression across all solutions
                for (A4Solution solution : solutions) {
                    Expression otherExpr = solution.a2k.get(sig);
                    if (expr == null || otherExpr == null || !expr.equals(otherExpr)) {
                        consistent = false;
                        break;
                    }
                }

                if (consistent && expr != null) {
                    analysis.commonSigExpressions.put(sig, expr);
                }
            }
        }

        // Find formulas that exist in ALL solutions
        analysis.commonFormulas = new HashSet<Formula>();
        for (Formula formula : first.formulas) {
            analysis.commonFormulas.add(formula);
        }
        for (A4Solution solution : solutions) {
            // Convert to HashSet for compatibility if needed
            Set<Formula> solutionFormulas = new HashSet<Formula>();
            for (Formula formula : solution.formulas) {
                solutionFormulas.add(formula);
            }
            analysis.commonFormulas.retainAll(solutionFormulas);
        }

        return analysis;
    }

    /**
     * Adds signatures that are common across ALL solutions in the cluster
     */
    private static void addClusterSignatures(A4Solution newSolution, List<A4Solution> solutions, ClusterAnalysis analysis) throws Err {
        for (Map.Entry<Sig,Expression> entry : analysis.commonSigExpressions.entrySet()) {
            newSolution.addSig(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Adds formulas that are common across ALL solutions in the cluster
     */
    private static void addClusterFormulas(A4Solution newSolution, List<A4Solution> solutions, ClusterAnalysis analysis) throws Err {
        for (Formula formula : analysis.commonFormulas) {
            newSolution.addFormula(formula, (Pos) null);
        }
    }

    /**
     * Sets up bounds for the solution based on cluster analysis: - Lower bounds =
     * present tuples (must be included) - Upper bounds = present tuples + uncertain
     * tuples (can be included) - Absent tuples are excluded from upper bounds
     */
    private static void setupClusterBasedBounds(A4Solution solution, ClusterSolution cluster) throws Err {
        Bounds bounds = solution.getBounds();
        TupleFactory factory = solution.factory;

        // Process each relation in the bounds
        for (Relation relation : bounds.relations()) {
            // Skip built-in relations
            if (isBuiltInRelation(relation)) {
                continue;
            }

            // Get cluster analysis for this relation
            Set<Tuple> presentTuples = cluster.getPresentTuples(relation);
            Set<Tuple> uncertainTuples = cluster.getUncertainTuples(relation);
            Set<Tuple> absentTuples = cluster.getAbsentTuples(relation);

            // Get current bounds
            TupleSet originalUpper = bounds.upperBound(relation);
            TupleSet originalLower = bounds.lowerBound(relation);

            if (originalUpper != null) {
                // Create new lower bound = present tuples
                TupleSet newLower = factory.noneOf(relation.arity());
                for (Tuple tuple : presentTuples) {
                    if (originalUpper.contains(tuple)) {
                        newLower.add(tuple);
                    }
                }

                // Create new upper bound = present tuples + uncertain tuples
                TupleSet newUpper = newLower.clone();
                for (Tuple tuple : uncertainTuples) {
                    if (originalUpper.contains(tuple)) {
                        newUpper.add(tuple);
                    }
                }

                // Ensure absent tuples are not in upper bound
                for (Tuple tuple : absentTuples) {
                    newUpper.remove(tuple);
                }

                // Apply the new bounds
                if (newUpper.containsAll(newLower)) {
                    bounds.bound(relation, newLower, newUpper);
                }
            }
        }
    }

    /**
     * Creates an instance for the solution containing only present tuples. This
     * represents the "definite" state of the cluster.
     */
    private static void createPresentTuplesInstance(A4Solution solution, ClusterSolution cluster) throws Err {
        // Create a minimal instance with present tuples
        Instance instance = new Instance(solution.bounds.universe());

        // Add integer bounds
        TupleFactory factory = solution.factory;
        for (int i = solution.min(); i <= solution.max(); i++) {
            Tuple tuple = factory.tuple("" + i);
            instance.add(i, factory.range(tuple, tuple));
        }

        // Add present tuples for each relation
        for (Relation relation : solution.bounds.relations()) {
            if (isBuiltInRelation(relation)) {
                // Use lower bound for built-in relations
                instance.add(relation, solution.bounds.lowerBound(relation));
            } else {
                // Use only present tuples for user-defined relations
                Set<Tuple> presentTuples = cluster.getPresentTuples(relation);
                TupleSet relationTuples = factory.noneOf(relation.arity());

                for (Tuple tuple : presentTuples) {
                    if (solution.bounds.upperBound(relation).contains(tuple)) {
                        relationTuples.add(tuple);
                    }
                }

                instance.add(relation, relationTuples);
            }
        }

        // Set the evaluator with the present-tuples-only instance
        solution.eval = new Evaluator(instance, solution.solver.options());
        solution.solved();
    }

    /**
     * Helper method to check if a relation is a built-in system relation
     */
    private static boolean isBuiltInRelation(Relation relation) {
        String name = relation.name();
        return name.startsWith("Int/") || name.equals("seq/Int") || name.equals("String") || name.startsWith("this/") || name.contains("$") || relation.toString().contains("KK_");
    }

}
