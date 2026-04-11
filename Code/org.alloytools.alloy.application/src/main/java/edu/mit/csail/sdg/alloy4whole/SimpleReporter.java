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

package edu.mit.csail.sdg.alloy4whole;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingUtilities;

import org.alloytools.alloy.core.AlloyCore;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Computer;
import edu.mit.csail.sdg.alloy4.ConstList;
import edu.mit.csail.sdg.alloy4.ConstMap;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorSyntax;
import edu.mit.csail.sdg.alloy4.ErrorType;
import edu.mit.csail.sdg.alloy4.ErrorWarning;
import edu.mit.csail.sdg.alloy4.MailBug;
import edu.mit.csail.sdg.alloy4.OurDialog;
import edu.mit.csail.sdg.alloy4.Pair;
import edu.mit.csail.sdg.alloy4.Pos;
import edu.mit.csail.sdg.alloy4.Util;
import edu.mit.csail.sdg.alloy4.Version;
import edu.mit.csail.sdg.alloy4.WorkerEngine.WorkerCallback;
import edu.mit.csail.sdg.alloy4.WorkerEngine.WorkerTask;
import edu.mit.csail.sdg.alloy4.XMLNode;
import edu.mit.csail.sdg.alloy4viz.AlloyInstance;
import edu.mit.csail.sdg.alloy4viz.StaticInstanceReader;
import edu.mit.csail.sdg.alloy4viz.VizGUI;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Module;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.A4SolutionReader;
import edu.mit.csail.sdg.translator.A4SolutionWriter;
import edu.mit.csail.sdg.translator.ClusterSolution;
import edu.mit.csail.sdg.translator.ClusteringResult;
import edu.mit.csail.sdg.translator.RelationalKMeansClusterer;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;
import kodkod.ast.Relation;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;
import kodkod.instance.Tuple;
import kodkod.instance.TupleSet;

/** This helper method is used by SimpleGUI. */

@SuppressWarnings("restriction" )
final class SimpleReporter extends A4Reporter {

    public static final class SimpleCallback1 implements WorkerCallback {

        private final SimpleGUI         gui;
        private final VizGUI            viz;
        private final SwingLogPanel     span;
        private final Set<ErrorWarning> warnings = new HashSet<ErrorWarning>();
        private final List<String>      results  = new ArrayList<String>();
        private int                     len2     = 0, len3 = 0, verbosity = 0;
        private final String            latestName;
        private final int               latestVersion;


        public HashMap<Integer,String>  lit_to_tuple;

        public SimpleCallback1(SimpleGUI gui, VizGUI viz, SwingLogPanel span, int verbosity, String latestName, int latestVersion) {
            this.gui = gui;
            this.viz = viz;
            this.span = span;
            this.verbosity = verbosity;
            this.latestName = latestName;
            this.latestVersion = latestVersion;
            len2 = len3 = span.getLength();
        }

        @Override
        public void done() {
            if (viz != null)
                span.setLength(len2);
            else
                span.logDivider();
            span.flush();
            gui.doStop(0);
        }

        @Override
        public void fail() {
            span.logBold("\nAn error has occurred!\n");
            span.logDivider();
            span.flush();
            gui.doStop(1);
        }

        @Override
        public void callback(Object msg) {
            if (msg == null) {
                span.logBold("Done\n");
                span.flush();
                return;
            }

            if (msg instanceof String) {
                span.logBold(((String) msg).trim() + "\n");
                span.flush();
                return;
            }
            if (msg instanceof Throwable) {
                for (Throwable ex = (Throwable) msg; ex != null; ex = ex.getCause()) {
                    if (ex instanceof OutOfMemoryError) {
                        span.logBold("\nFatal Error: the solver ran out of memory!\n" + "Try simplifying your model or reducing the scope,\n" + "or increase memory under the Options menu.\n");
                        return;
                    }
                    if (ex instanceof StackOverflowError) {
                        span.logBold("\nFatal Error: the solver ran out of stack space!\n" + "Try simplifying your model or reducing the scope,\n" + "or increase stack under the Options menu.\n");
                        return;
                    }
                }
            }
            if (msg instanceof Err) {
                Err ex = (Err) msg;
                String text = "fatal";
                boolean fatal = false;
                if (ex instanceof ErrorSyntax)
                    text = "syntax";
                else if (ex instanceof ErrorType)
                    text = "type";
                else
                    fatal = true;
                if (ex.pos == Pos.UNKNOWN)
                    span.logBold("A " + text + " error has occurred:  ");
                else
                    span.logLink("A " + text + " error has occurred:  ", "POS: " + ex.pos.x + " " + ex.pos.y + " " + ex.pos.x2 + " " + ex.pos.y2 + " " + ex.pos.filename);
                if (verbosity > 2) {
                    span.log("(see the ");
                    span.logLink("stacktrace", "MSG: " + ex.dump());
                    span.log(")\n");
                } else {
                    span.log("\n");
                }
                span.logIndented(ex.msg.trim());
                span.log("\n");
                if (fatal && latestVersion > Version.buildNumber())
                    span.logBold("\nNote: You are running Alloy build#" + Version.buildNumber() + ",\nbut the most recent is Alloy build#" + latestVersion + ":\n( version " + latestName + " )\nPlease try to upgrade to the newest version," + "\nas the problem may have been fixed already.\n");
                span.flush();
                if (!fatal)
                    gui.doVisualize("POS: " + ex.pos.x + " " + ex.pos.y + " " + ex.pos.x2 + " " + ex.pos.y2 + " " + ex.pos.filename);
                return;
            }
            if (msg instanceof Throwable) {
                Throwable ex = (Throwable) msg;
                span.logBold(ex.toString().trim() + "\n");
                span.flush();
                return;
            }
            if (!(msg instanceof Object[]))
                return;
            Object[] array = (Object[]) msg;
            if (array[0].equals("pop")) {
                span.setLength(len2);
                String x = (String) (array[1]);
                if (viz != null && x.length() > 0)
                    OurDialog.alert(x);
            }
            if (array[0].equals("declare")) {
                gui.doSetLatest((String) (array[1]));
            }
            if (array[0].equals("S2")) {
                len3 = len2 = span.getLength();
                span.logBold("" + array[1]);
            }
            if (array[0].equals("R3")) {
                span.setLength(len3);
                span.log("" + array[1]);
            }
            if (array[0].equals("link")) {
                span.logLink((String) (array[1]), (String) (array[2]));
            }
            if (array[0].equals("bold")) {
                span.logBold("" + array[1]);
            }
            if (array[0].equals("")) {
                span.log("" + array[1]);
            }
            if (array[0].equals("scope") && verbosity > 0) {
                span.log("   " + array[1]);
            }
            if (array[0].equals("bound") && verbosity > 1) {
                span.log("   " + array[1]);
            }
            if (array[0].equals("resultCNF")) {
                results.add(null);
                span.setLength(len3);
                span.log("   File written to " + array[1] + "\n\n");
            }
            if (array[0].equals("setworld")) {

            }
            if (array[0].equals("setcmd")) {
            }
            if (array[0].equals("setinstance")) {
            }
            if (array[0].equals("debug") && verbosity > 2) {
                span.log("   " + array[1] + "\n");
                len2 = len3 = span.getLength();
            }
            if (array[0].equals("translate")) {
                span.log("   " + array[1]);
                len3 = span.getLength();
                span.logBold("   Generating CNF...\n");
            }
            if (array[0].equals("solve")) {
                span.setLength(len3);
                span.log("   " + array[1]);
                len3 = span.getLength();
                span.logBold("   Solving...\n");
            }
            if (array[0].equals("warnings")) {
                if (warnings.size() == 0)
                    span.setLength(len2);
                else if (warnings.size() > 1)
                    span.logBold("Note: There were " + warnings.size() + " compilation warnings. Please scroll up to see them.\n\n");
                else
                    span.logBold("Note: There was 1 compilation warning. Please scroll up to see them.\n\n");
                if (warnings.size() > 0 && Boolean.FALSE.equals(array[1])) {
                    Pos e = warnings.iterator().next().pos;
                    gui.doVisualize("POS: " + e.x + " " + e.y + " " + e.x2 + " " + e.y2 + " " + e.filename);
                    span.logBold("Warnings often indicate errors in the model.\n" + "Some warnings can affect the soundness of the analysis.\n" + "To proceed despite the warnings, go to the Options menu.\n");
                }
            }
            if (array[0].equals("warning")) {
                ErrorWarning e = (ErrorWarning) (array[1]);
                if (!warnings.add(e))
                    return;
                Pos p = e.pos;
                span.logLink("Warning #" + warnings.size(), "POS: " + p.x + " " + p.y + " " + p.x2 + " " + p.y2 + " " + p.filename);
                span.log("\n");
                span.logIndented(e.msg.trim());
                span.log("\n\n");
            }
            if (array[0].equals("sat")) {
                boolean chk = Boolean.TRUE.equals(array[1]);
                int expects = (Integer) (array[2]);
                String filename = (String) (array[3]), formula = (String) (array[4]);
                results.add(filename);
                (new File(filename)).deleteOnExit();
                gui.doSetLatest(filename);
                span.setLength(len3);
                span.log("   ");
                span.logLink(chk ? "Counterexample" : "Instance", "XML: " + filename);
                span.log(" found. ");
                span.logLink(chk ? "Assertion" : "Predicate", formula);
                span.log(chk ? " is invalid" : " is consistent");
                if (expects == 0)
                    span.log(", contrary to expectation");
                else if (expects == 1)
                    span.log(", as expected");
                span.log(". " + array[5] + "ms.\n\n");
                gui.doDisplayRun();
            }
            if (array[0].equals("metamodel")) {
                String outf = (String) (array[1]);
                span.setLength(len2);
                (new File(outf)).deleteOnExit();
                gui.doSetLatest(outf);
                span.logLink("Metamodel", "XML: " + outf);
                span.log(" successfully generated.\n\n");
            }
            if (array[0].equals("minimizing")) {
                boolean chk = Boolean.TRUE.equals(array[1]);
                int expects = (Integer) (array[2]);
                span.setLength(len3);
                span.log(chk ? "   No counterexample found." : "   No instance found.");
                if (chk)
                    span.log(" Assertion may be valid");
                else
                    span.log(" Predicate may be inconsistent");
                if (expects == 1)
                    span.log(", contrary to expectation");
                else if (expects == 0)
                    span.log(", as expected");
                span.log(". " + array[4] + "ms.\n");
                span.logBold("   Minimizing the unsat core of " + array[3] + " entries...\n");
            }
            if (array[0].equals("unsat")) {
                boolean chk = Boolean.TRUE.equals(array[1]);
                int expects = (Integer) (array[2]);
                String formula = (String) (array[4]);
                span.setLength(len3);
                span.log(chk ? "   No counterexample found. " : "   No instance found. ");
                span.logLink(chk ? "Assertion" : "Predicate", formula);
                span.log(chk ? " may be valid" : " may be inconsistent");
                gui.doDisplayRun();
                if (expects == 1)
                    span.log(", contrary to expectation");
                else if (expects == 0)
                    span.log(", as expected");
                if (array.length == 5) {
                    span.log(". " + array[3] + "ms.\n\n");
                    span.flush();
                    return;
                }
                String core = (String) (array[5]);
                int mbefore = (Integer) (array[6]), mafter = (Integer) (array[7]);
                span.log(". " + array[3] + "ms.\n");
                if (core.length() == 0) {
                    results.add("");
                    span.log("   No unsat core is available in this case. " + array[8] + "ms.\n\n");
                    span.flush();
                    return;
                }
                results.add(core);
                (new File(core)).deleteOnExit();
                span.log("   ");
                span.logLink("Core", core);
                if (mbefore <= mafter)
                    span.log(" contains " + mafter + " top-level formulas. " + array[8] + "ms.\n\n");
                else
                    span.log(" reduced from " + mbefore + " to " + mafter + " top-level formulas. " + array[8] + "ms.\n\n");
            }
            span.flush();
        }
    }



    private void cb(Serializable... objs) {
        cb.callback(objs);
    }

    /** {@inheritDoc} */
    @Override
    public void resultCNF(final String filename) {
        cb("resultCNF", filename);
    }

    /** {@inheritDoc} */
    @Override
    public void warning(final ErrorWarning ex) {
        warn++;
        cb("warning", ex);
    }

    /** {@inheritDoc} */
    @Override
    public void scope(final String msg) {
        cb("scope", msg);
    }

    /** {@inheritDoc} */
    @Override
    public void bound(final String msg) {
        cb("bound", msg);
    }

    /** {@inheritDoc} */
    @Override
    public void debug(final String msg) {
        cb("debug", msg.trim());
    }

    /** {@inheritDoc} */
    @Override
    public void translate(String solver, int bitwidth, int maxseq, int skolemDepth, int symmetry) {
        if (aunit_extension_executions) {
            //Currently no details to track
        } else {
            cb("translate", "Solver=" + solver + " Bitwidth=" + bitwidth + " MaxSeq=" + maxseq + (skolemDepth == 0 ? "" : " SkolemDepth=" + skolemDepth) + " Symmetry=" + (symmetry > 0 ? ("" + symmetry) : "OFF") + '\n');
        }
    }

    /** {@inheritDoc} */
    @Override
    public void solve(final int primaryVars, final int totalVars, final int clauses) {
        if (aunit_extension_executions) {
            //No details saved currently
        } else {
            cb("solve", "" + totalVars + " vars. " + primaryVars + " primary vars. " + clauses + " clauses. " + (System.currentTimeMillis() - lastTime) + "ms.\n");
        }
        lastTime = System.currentTimeMillis();
    }

    /** {@inheritDoc} */
    @Override
    public void resultSAT(Object command, long solvingTime, Object solution) {
        if (!(solution instanceof A4Solution) || !(command instanceof Command))
            return;
        A4Solution sol = (A4Solution) solution;
        Command cmd = (Command) command;
        String formula = recordKodkod ? sol.debugExtractKInput() : "";
        String filename = tempfile + ".xml";
        synchronized (SimpleReporter.class) {
            try {
                if (!aunit_extension_executions)
                    cb("R3", "   Writing the XML file...");
                if (latestModule != null)
                    writeXML(this, latestModule, filename, sol, latestKodkodSRC);
            } catch (Throwable ex) {
                cb("bold", "\n" + (ex.toString().trim()) + "\nStackTrace:\n" + (MailBug.dump(ex).trim()) + "\n");
                return;
            }
            latestKodkods.clear();
            latestKodkods.add(sol.toString());
            latestKodkod = sol;
            latestKodkodXML = filename;
        }
        String formulafilename = "";
        if (formula.length() > 0 && tempfile != null) {
            formulafilename = tempfile + ".java";
            try {
                Util.writeAll(formulafilename, formula);
                formulafilename = "CNF: " + formulafilename;
            } catch (Throwable ex) {
                formulafilename = "";
            }
        }
        if (aunit_extension_executions) {
            String details = System.currentTimeMillis() - lastTime + "ms";
            aunitTestDetails.add(details);
        } else {
            cb("sat", cmd.check, cmd.expects, filename, formulafilename, System.currentTimeMillis() - lastTime);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void minimizing(Object command, int before) {
        if (!(command instanceof Command))
            return;
        Command cmd = (Command) command;
        minimized = System.currentTimeMillis();
        cb("minimizing", cmd.check, cmd.expects, before, minimized - lastTime);
    }

    /** {@inheritDoc} */
    @Override
    public void minimized(Object command, int before, int after) {
        minimizedBefore = before;
        minimizedAfter = after;
    }

    /** {@inheritDoc} */
    @Override
    public void resultUNSAT(Object command, long solvingTime, Object solution) {
        if (!(solution instanceof A4Solution) || !(command instanceof Command))
            return;
        A4Solution sol = (A4Solution) solution;
        Command cmd = (Command) command;
        String originalFormula = recordKodkod ? sol.debugExtractKInput() : "";
        String corefilename = "", formulafilename = "";
        if (originalFormula.length() > 0 && tempfile != null) {
            formulafilename = tempfile + ".java";
            try {
                Util.writeAll(formulafilename, originalFormula);
                formulafilename = "CNF: " + formulafilename;
            } catch (Throwable ex) {
                formulafilename = "";
            }
        }
        Pair<Set<Pos>,Set<Pos>> core = sol.highLevelCore();
        if ((core.a.size() > 0 || core.b.size() > 0) && tempfile != null) {
            corefilename = tempfile + ".core";
            OutputStream fs = null;
            ObjectOutputStream os = null;
            try {
                fs = new FileOutputStream(corefilename);
                os = new ObjectOutputStream(fs);
                os.writeObject(core);
                os.writeObject(sol.lowLevelCore());
                corefilename = "CORE: " + corefilename;
            } catch (Throwable ex) {
                corefilename = "";
            } finally {
                Util.close(os);
                Util.close(fs);
            }
        }
        if (aunit_extension_executions) {
            String details = System.currentTimeMillis() - lastTime + "ms";
            aunitTestDetails.add(details);
        } else {
            if (minimized == 0)
                cb("unsat", cmd.check, cmd.expects, (System.currentTimeMillis() - lastTime), formulafilename);
            else
                cb("unsat", cmd.check, cmd.expects, minimized - lastTime, formulafilename, corefilename, minimizedBefore, minimizedAfter, (System.currentTimeMillis() - minimized));
        }
    }

    private final WorkerCallback cb;

    // ========== These fields should be set each time we execute a set of
    // commands

    /** Whether we should record Kodkod input/output. */
    private final boolean recordKodkod;

    /**
     * The time that the last action began; we subtract it from
     * System.currentTimeMillis() to determine the elapsed time.
     */
    private long          lastTime  = 0;

    /**
     * If we performed unsat core minimization, then this is the start of the
     * minimization, else this is 0.
     */
    private long          minimized = 0;

    /** The unsat core size before minimization. */
    private int           minimizedBefore;

    /** The unsat core size after minimization. */
    private int           minimizedAfter;

    /**
     * The filename where we can write a temporary Java file or Core file.
     */
    private String        tempfile  = null;

    // ========== These fields may be altered as each successful command
    // generates a Kodkod or Metamodel instance

    /**
     * The set of Strings already enumerated for this current solution.
     */
    private static final Set<String>       latestKodkods              = new LinkedHashSet<String>();

    /**
     * The A4Solution corresponding to the latest solution generated by Kodkod; this
     * field must be synchronized.
     */
    private static A4Solution              latestKodkod               = null;

    /**
     * Continuation point for Hawkeye multi-cluster enumeration: the last
     * {@link A4Solution} produced after the most recent batch (initial run or
     * cluster "Next"). Incremental {@code next()} advances the shared solver stream
     * from here; it is not stored per cluster window. Cluster windows are refreshed
     * by index after each re-clustering step.
     *
     * <p>Access must be synchronized on {@code SimpleReporter.class}.</p>
     */
    static A4Solution                      latestClusterSolution      = null;

    /**
     * The root Module corresponding to this.latestKodkod; this field must be
     * synchronized.
     */
    private static Module                  latestModule               = null;

    /**
     * The source code corresponding to the latest solution generated by Kodkod;
     * this field must be synchronized.
     */
    private static ConstMap<String,String> latestKodkodSRC            = null;

    /**
     * The XML filename corresponding to the latest solution generated by Kodkod;
     * this field must be synchronized.
     */
    private static String                  latestKodkodXML            = null;

    /**
     * The XML filename corresponding to the latest metamodel generated by
     * TranslateAlloyToMetamodel; this field must be synchronized.
     */
    private static String                  latestMetamodelXML         = null;

    //Track details for aunit tests
    static ArrayList<String>               aunitTestDetails           = new ArrayList<String>();
    //boolean flag to indicate aunit or one of its extensions is executing which changes logging information
    static boolean                         aunit_extension_executions = false;

    /** Constructor is private. */
    private SimpleReporter(WorkerCallback cb, boolean recordKodkod) {
        this.cb = cb;
        this.recordKodkod = recordKodkod;
    }

    /** Helper method to write out a full XML file. */
    private static void writeXML(A4Reporter rep, Module mod, String filename, A4Solution sol, Map<String,String> sources) throws Exception {
        sol.writeXML(rep, filename, mod.getAllFunc(), sources);
        if (AlloyCore.isDebug())
            validate(filename);
    }

    private int warn = 0;

    /** Task that performs solution enumeration. */
    public static final class SimpleTask2 implements WorkerTask {

        public static CompModule        latestWorld;
        public static Command           latestCmd;
        public static A4Solution        latestSolution;

        private static final long       serialVersionUID = 0;
        public String                   filename         = "";
        public transient WorkerCallback out              = null;
        ArrayList<Integer>              same;
        ArrayList<Integer>              diff;
        ArrayList<String>               same_hl;
        ArrayList<String>               diff_hl;

        private String                  PROJECT_DIR_PATH = System.getProperty("user.dir");
        private final String            HIDDEN_DIR_PATH  = "";


        private void cb(Object... objs) throws Exception {
            out.callback(objs);
        }

        public void setSameAtoms(ArrayList<Integer> same) {
            this.same = same;
        }

        public void setDiffAtoms(ArrayList<Integer> diff) {
            this.diff = diff;
        }

        public void setSameHighlevel(ArrayList<String> same_hl) {
            this.same_hl = same_hl;
        }

        public void setDiffHighlevel(ArrayList<String> diff_hl) {
            this.diff_hl = diff_hl;
        }

        @Override
        public void run(WorkerCallback out) throws Exception {
            this.out = out;
            cb("S2", "Enumerating...\n");
            A4Solution sol;
            Module mod;

            synchronized (SimpleReporter.class) {
                if (latestMetamodelXML != null && latestMetamodelXML.equals(filename)) {
                    cb("pop", "You cannot enumerate a metamodel.\n");
                    return;
                }
                if (latestKodkodXML == null || !latestKodkodXML.equals(filename)) {
                    cb("pop", "You can only enumerate the solutions of the most-recently-solved command.");
                    return;
                }
                if (latestKodkod == null || latestModule == null || latestKodkodSRC == null) {
                    cb("pop", "Error: the SAT solver that generated the instance has exited,\nso we cannot enumerate unless you re-solve that command.\n");
                    return;
                }
                sol = latestKodkod;
                mod = latestModule;
            }
            if (!sol.satisfiable()) {
                cb("pop", "Error: This command is unsatisfiable,\nso there are no solutions to enumerate.");
                return;
            }
            if (!sol.isIncremental()) {
                cb("pop", "Error: This solution was not generated by an incremental SAT solver.\n" + "Currently only MiniSat and SAT4J are supported.");
                return;
            }
            int tries = 0;
            while (true) {
                sol = sol.next(same, diff, same_hl, diff_hl);

                PrintWriter writer;
                try {
                    writer = new PrintWriter(HIDDEN_DIR_PATH + "atom2name.txt", "UTF-8");
                    for (Object s : sol.atom2name.keySet()) {
                        writer.println(s.toString() + ":" + sol.atom2name.get(s));
                    }

                    writer.close();
                } catch (FileNotFoundException | UnsupportedEncodingException e) { // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                if (!sol.satisfiable()) {
                    cb("pop", "There are no more satisfying instances.\n\n" + "Note: due to symmetry breaking and other optimizations,\n" + "some equivalent solutions may have been omitted.");
                    return;
                }
                String toString = sol.toString();
                synchronized (SimpleReporter.class) {
                    if (!latestKodkods.add(toString))
                        if (tries < 100) {
                            tries++;
                            continue;
                        }
                    // The counter is needed to avoid a Kodkod bug where
                    // sometimes we might repeat the same solution infinitely
                    // number of times; this at least allows the user to keep
                    // going
                    writeXML(null, mod, filename, sol, latestKodkodSRC);
                    latestKodkod = sol;
                }
                cb("declare", filename);
                return;
            }
        }
    }

    /**
     * Validate the given filename to see if it is a valid Alloy XML instance file.
     */
    private static void validate(String filename) throws Exception {
        A4SolutionReader.read(new ArrayList<Sig>(), new XMLNode(new File(filename))).toString();
        StaticInstanceReader.parseInstance(new File(filename));
    }

    /** Task that perform one command. */
    public static final class SimpleTask1 implements WorkerTask {

        private static final long   serialVersionUID = 0;
        public A4Options            options;
        public String               tempdir;
        public boolean              bundleWarningNonFatal;
        public int                  bundleIndex;
        public int                  resolutionMode;
        public Map<String,String>   map;

        private String              PROJECT_DIR_PATH = System.getProperty("user.dir");
        private final String        HIDDEN_DIR_PATH  = "";
        private static final VizGUI viz              = new VizGUI(false, "", null);


        public SimpleTask1() {
        }

        public void cb(WorkerCallback out, Object... objs) throws IOException {
            out.callback(objs);
        }

        @Override
        public void run(WorkerCallback out) throws Exception {
            cb(out, "S2", "Starting the solver...\n\n");
            final SimpleReporter rep = new SimpleReporter(out, options.recordKodkod);
            final CompModule world = CompUtil.parseEverything_fromFile(rep, map, options.originalFilename, resolutionMode);
            final List<Sig> sigs = world.getAllReachableSigs();
            final ConstList<Command> cmds = world.getAllCommands();

            cb(out, "warnings", bundleWarningNonFatal);
            if (rep.warn > 0 && !bundleWarningNonFatal)
                return;
            List<String> result = new ArrayList<String>(cmds.size());
            if (bundleIndex == -2) {
                final String outf = tempdir + File.separatorChar + "m.xml";
                cb(out, "S2", "Generating the metamodel...\n");
                PrintWriter of = new PrintWriter(outf, "UTF-8");
                Util.encodeXMLs(of, "\n<alloy builddate=\"", Version.buildDate(), "\">\n\n");
                A4SolutionWriter.writeMetamodel(ConstList.make(sigs), options.originalFilename, of);
                Util.encodeXMLs(of, "\n</alloy>");
                Util.close(of);
                if (AlloyCore.isDebug())
                    validate(outf);
                cb(out, "metamodel", outf);
                synchronized (SimpleReporter.class) {
                    latestMetamodelXML = outf;
                }
            } else
                for (int i = 0; i < cmds.size(); i++)
                    if (bundleIndex < 0 || i == bundleIndex) {
                        synchronized (SimpleReporter.class) {
                            latestModule = world;
                            latestKodkodSRC = ConstMap.make(map);
                        }
                        final String tempXML = tempdir + File.separatorChar + i + ".cnf.xml";
                        final String tempCNF = tempdir + File.separatorChar + i + ".cnf";
                        final Command cmd = cmds.get(i);
                        rep.tempfile = tempCNF;
                        cb(out, "bold", "Executing \"" + cmd + "\"\n");
                        A4Solution ai = TranslateAlloyToKodkod.execute_commandFromBook(rep, world.getAllReachableSigs(), cmd, options);

                        /*
                         * Hawkeye clustered visualization (multigraph):
                         *
                         * 1) Enumerate up to maxSolutions satisfiable instances (incremental SAT).
                         * 2) Cluster them with RelationalKMeansClusterer into numClusters groups.
                         * 3) Open one VizGUI per cluster with aggregated graph + stored A4Solutions.
                         * 4) "Show Next Solution" on a cluster window enumerates a new batch from
                         *    latestClusterSolution, optionally fixing tuple literals derived from that
                         *    window's current cluster, re-clusters, and refreshes all windows.
                         */
                        int maxSolutions = 20;
                        int numClusters = 4;
                        List<A4Solution> solutionsList = new ArrayList<A4Solution>();
                        if (ai != null && ai.satisfiable()) {
                            solutionsList.add(ai);
                            cb(out, "bold", "Generated solution 1\n");

                            // Unconstrained enumeration: each next() excludes the previous model only.
                            if (ai.isIncremental()) {
                                A4Solution currentSol = ai;
                                for (int solutionNum = 2; solutionNum <= maxSolutions; solutionNum++) {
                                    try {
                                        A4Solution nextSol = currentSol.next(new ArrayList<Integer>(), new ArrayList<Integer>(), new ArrayList<String>(), new ArrayList<String>());
                                        if (nextSol != null && nextSol.satisfiable()) {
                                            solutionsList.add(nextSol);
                                            currentSol = nextSol;
                                            cb(out, "bold", "Generated solution " + solutionNum + "\n");
                                        } else {
                                            cb(out, "bold", "No more solutions available (found " + (solutionNum - 1) + " total)\n");
                                            break;
                                        }
                                    } catch (Err e) {
                                        cb(out, "bold", "Error generating solution " + solutionNum + ": " + e.getMessage() + "\n");
                                        break;
                                    }
                                }
                                if (solutionsList.size() == maxSolutions) {
                                    cb(out, "bold", "Reached maximum of " + maxSolutions + " solutions\n");
                                }
                            } else {
                                cb(out, "bold", "Solver is not incremental, cannot enumerate solutions\n");
                            }
                        }

                        if (solutionsList.size() >= numClusters) {
                            // Seed global enumeration cursor for subsequent cluster "Next" batches.
                            synchronized (SimpleReporter.class) {
                                latestClusterSolution = solutionsList.get(solutionsList.size() - 1);
                            }

                            try {
                                cb(out, "bold", "\nClustering " + solutionsList.size() + " solutions into " + numClusters + " clusters...\n");

                                RelationalKMeansClusterer clusterer = new RelationalKMeansClusterer(numClusters);
                                ClusteringResult clusteringResult = clusterer.cluster(solutionsList);

                                cb(out, "bold", "Clustering complete!\n");
                                cb(out, "bold", clusteringResult.toString() + "\n");

                                // All cluster frames for this command; refreshed in place after each
                                // "Next" (same window objects, new aggregated content).
                                final List<VizGUI> clusterVizWindows = new ArrayList<VizGUI>();
                                final int finalNumClusters = numClusters;

                                for (ClusterSolution cluster : clusteringResult.getClusters()) {
                                    cb(out, "bold", "Creating visualization for " + cluster.toString() + "\n");

                                    List<AlloyInstance> clusterInstances = new ArrayList<AlloyInstance>();
                                    final List<A4Solution> clusterSolutions = cluster.getSolutions();
                                    for (A4Solution sol : cluster.getSolutions()) {
                                        try {
                                            AlloyInstance instance = StaticInstanceReader.a4SolutionToAlloyInstanceMaker(sol);
                                            clusterInstances.add(instance);
                                        } catch (Throwable e) {
                                            cb(out, "bold", "Error converting solution to instance: " + e.getMessage() + "\n");
                                        }
                                    }

                                    if (!clusterInstances.isEmpty()) {
                                        final List<AlloyInstance> instancesForCluster = new ArrayList<AlloyInstance>(clusterInstances);
                                        final int clusterNum = cluster.getClusterNumber();
                                        final int clusterSz = cluster.size();

                                        // Stable index of this window in clusterVizWindows; used to resolve
                                        // which VizGUI's cluster constraints apply when this enumerator runs.
                                        final int thisClusterIdx = clusterVizWindows.size();

                                        /**
                                         * Per-cluster {@link Computer} wired as {@code VizGUI}'s enumerator.
                                         * Invoked when the user chooses "Show Next Solution"; runs off the EDT,
                                         * applies tuple constraints from the window at {@code thisClusterIdx},
                                         * then re-clusters and refreshes all windows in {@code clusterVizWindows}.
                                         */
                                        final Computer clusterNextBatchEnumerator = new Computer() {

                                            /** Hawkeye panel: same/diff atom and relation selections from the UI. */
                                            private ArrayList<Integer> userSameAtoms     = new ArrayList<Integer>();
                                            private ArrayList<Integer> userDiffAtoms     = new ArrayList<Integer>();
                                            private ArrayList<String>  userSameHighlevel = new ArrayList<String>();
                                            private ArrayList<String>  userDiffHighlevel = new ArrayList<String>();

                                            @Override
                                            public Object compute(Object input) {
                                                new Thread(new Runnable() {

                                                    /** Shows or hides every cluster frame (EDT); used during batch work. */
                                                    private void setAllClusterWindowsVisible(final boolean visible) {
                                                        SwingUtilities.invokeLater(new Runnable() {

                                                            @Override
                                                            public void run() {
                                                                for (VizGUI v : clusterVizWindows) {
                                                                    if (v == null || v.getFrame() == null)
                                                                        continue;
                                                                    v.getFrame().setVisible(visible);
                                                                }
                                                            }
                                                        });
                                                    }

                                                    @Override
                                                    public void run() {
                                                        // Avoid stale graphs while the worker enumerates and re-clusters.
                                                        setAllClusterWindowsVisible(false);

                                                        // Snapshot UI choices; doNext may mutate the Computer before the worker reads them.
                                                        final ArrayList<Integer> sameAtomsUserSnapshot = userSameAtoms == null ? new ArrayList<Integer>() : new ArrayList<Integer>(userSameAtoms);
                                                        final ArrayList<Integer> diffAtomsUserSnapshot = userDiffAtoms == null ? new ArrayList<Integer>() : new ArrayList<Integer>(userDiffAtoms);
                                                        final ArrayList<String> sameHighUserSnapshot = userSameHighlevel == null ? new ArrayList<String>() : new ArrayList<String>(userSameHighlevel);
                                                        final ArrayList<String> diffHighUserSnapshot = userDiffHighlevel == null ? new ArrayList<String>() : new ArrayList<String>(userDiffHighlevel);

                                                        final A4Solution startSol;
                                                        synchronized (SimpleReporter.class) {
                                                            startSol = latestClusterSolution;
                                                        }
                                                        if (startSol == null || !startSol.isIncremental()) {
                                                            SwingUtilities.invokeLater(new Runnable() {

                                                                public void run() {
                                                                    OurDialog.alert("No more solutions can be enumerated.");
                                                                }
                                                            });
                                                            setAllClusterWindowsVisible(true);
                                                            return;
                                                        }

                                                        // Tuple-level constraints: universal present/absent across the
                                                        // triggering window's currentClusterA4Solutions (see VizGUI).
                                                        final VizGUI triggerWindow = (thisClusterIdx >= 0 && thisClusterIdx < clusterVizWindows.size()) ? clusterVizWindows.get(thisClusterIdx) : null;
                                                        final ArrayList<VizGUI.ClusterFixedVar> fixedVars = (triggerWindow == null) ? new ArrayList<VizGUI.ClusterFixedVar>() : triggerWindow.computeClusterFixedVars();

                                                        // Bounds and tuple objects for interpreting fixedVars against each candidate solution.
                                                        final Bounds bounds = startSol.debugExtractKodkodBounds();
                                                        final HashMap<String,Relation> relByName = new HashMap<String,Relation>();
                                                        for (Relation r : bounds.relations()) {
                                                            relByName.put(r.name(), r);
                                                        }

                                                        final HashMap<String,HashMap<Integer,Tuple>> tupleByRelAndIdx = new HashMap<String,HashMap<Integer,Tuple>>();
                                                        final HashSet<Integer> fixedVarIds = new HashSet<Integer>();
                                                        final HashSet<String> neededRelNames = new HashSet<String>();
                                                        for (VizGUI.ClusterFixedVar fv : fixedVars) {
                                                            fixedVarIds.add(fv.varId);
                                                            neededRelNames.add(fv.relName);
                                                        }

                                                        for (String relName : neededRelNames) {
                                                            final Relation rObj = relByName.get(relName);
                                                            if (rObj == null)
                                                                continue;
                                                            final TupleSet upper = bounds.upperBound(rObj);
                                                            final HashMap<Integer,Tuple> idxToTuple = new HashMap<Integer,Tuple>();
                                                            if (upper != null) {
                                                                for (Tuple t : upper)
                                                                    idxToTuple.put(t.index(), t);
                                                            }
                                                            tupleByRelAndIdx.put(relName, idxToTuple);
                                                        }

                                                        // Each step: map fixed tuple intent to same_atoms/diff_atoms primary
                                                        // variable ids, then call A4Solution.next (SolutionIterator).
                                                        final List<A4Solution> nextBatch = new ArrayList<A4Solution>();
                                                        A4Solution cur = startSol;
                                                        for (int k = 0; k < 20; k++) {
                                                            try {
                                                                final Instance curInst = cur.debugExtractKInstance();

                                                                final ArrayList<Integer> sameAtoms = new ArrayList<Integer>();
                                                                final ArrayList<Integer> diffAtoms = new ArrayList<Integer>();

                                                                for (VizGUI.ClusterFixedVar fv : fixedVars) {
                                                                    final Relation rObj = relByName.get(fv.relName);
                                                                    if (rObj == null)
                                                                        continue;
                                                                    final HashMap<Integer,Tuple> idxToTuple = tupleByRelAndIdx.get(fv.relName);
                                                                    if (idxToTuple == null)
                                                                        continue;
                                                                    final Tuple tObj = idxToTuple.get(fv.tupleIndex);
                                                                    if (tObj == null)
                                                                        continue;
                                                                    final TupleSet tuplesPresent = curInst.tuples(rObj);
                                                                    final boolean currentContains = tuplesPresent != null && tuplesPresent.contains(tObj);

                                                                    // Keep literal if already correct; flip via diff_atoms otherwise.
                                                                    if (currentContains == fv.desiredPresent)
                                                                        sameAtoms.add(fv.varId);
                                                                    else
                                                                        diffAtoms.add(fv.varId);
                                                                }

                                                                // Merge Hawkeye UI constraints; cluster-fixed ids take precedence.
                                                                for (Integer atom : sameAtomsUserSnapshot) {
                                                                    if (!fixedVarIds.contains(atom))
                                                                        sameAtoms.add(atom);
                                                                }
                                                                for (Integer atom : diffAtomsUserSnapshot) {
                                                                    if (!fixedVarIds.contains(atom))
                                                                        diffAtoms.add(atom);
                                                                }

                                                                final A4Solution nxt = cur.next(sameAtoms, diffAtoms, sameHighUserSnapshot, diffHighUserSnapshot);
                                                                if (nxt != null && nxt.satisfiable()) {
                                                                    nextBatch.add(nxt);
                                                                    cur = nxt;
                                                                } else {
                                                                    break;
                                                                }
                                                            } catch (Err ex) {
                                                                break;
                                                            }
                                                        }

                                                        synchronized (SimpleReporter.class) {
                                                            latestClusterSolution = cur;
                                                        }

                                                        if (nextBatch.isEmpty()) {
                                                            SwingUtilities.invokeLater(new Runnable() {

                                                                public void run() {
                                                                    OurDialog.alert("No more satisfying instances are available.");
                                                                }
                                                            });
                                                            setAllClusterWindowsVisible(true);
                                                            return;
                                                        }

                                                        try {
                                                            RelationalKMeansClusterer clusterer2 = new RelationalKMeansClusterer(finalNumClusters);
                                                            final ClusteringResult newResult = clusterer2.cluster(nextBatch);
                                                            final List<ClusterSolution> newClusters = newResult.getClusters();

                                                            final List<List<AlloyInstance>> instancesPerCluster = new ArrayList<List<AlloyInstance>>();
                                                            for (ClusterSolution cs : newClusters) {
                                                                List<AlloyInstance> inst = new ArrayList<AlloyInstance>();
                                                                for (A4Solution sol : cs.getSolutions()) {
                                                                    try {
                                                                        inst.add(StaticInstanceReader.a4SolutionToAlloyInstanceMaker(sol));
                                                                    } catch (Throwable ignored) {
                                                                    }
                                                                }
                                                                instancesPerCluster.add(inst);
                                                            }

                                                            SwingUtilities.invokeLater(new Runnable() {

                                                                @Override
                                                                public void run() {
                                                                    // Refresh by position: newClusters[i] -> clusterVizWindows[i].
                                                                    for (int idx = 0; idx < clusterVizWindows.size() && idx < newClusters.size(); idx++) {
                                                                        ClusterSolution cs = newClusters.get(idx);
                                                                        final List<A4Solution> solsForViz = cs.getSolutions();
                                                                        final List<AlloyInstance> inst = instancesPerCluster.get(idx);
                                                                        if (!inst.isEmpty())
                                                                            clusterVizWindows.get(idx).launchA4SolutionListWithClusterInfo(inst, solsForViz, cs.getClusterNumber(), cs.size());
                                                                    }
                                                                }
                                                            });
                                                        } catch (final Exception ex) {
                                                            SwingUtilities.invokeLater(new Runnable() {

                                                                public void run() {
                                                                    OurDialog.alert("Error during clustering: " + ex.getMessage());
                                                                }
                                                            });
                                                            setAllClusterWindowsVisible(true);
                                                        }
                                                    }
                                                }, "ClusterNextBatch").start();
                                                return input;
                                            }

                                            @Override
                                            public void setSameAtoms(ArrayList<Integer> same) {
                                                this.userSameAtoms = same;
                                            }

                                            @Override
                                            public void setDiffAtoms(ArrayList<Integer> diff) {
                                                this.userDiffAtoms = diff;
                                            }

                                            @Override
                                            public void setSameHighlevel(ArrayList<String> same) {
                                                this.userSameHighlevel = same;
                                            }

                                            @Override
                                            public void setDiffHighlevel(ArrayList<String> diff) {
                                                this.userDiffHighlevel = diff;
                                            }
                                        };

                                        // Create window before invokeLater so the list is populated
                                        // before the user can click Next
                                        final VizGUI clusterViz = new VizGUI(false, "", null, clusterNextBatchEnumerator, null);
                                        clusterVizWindows.add(clusterViz);

                                        Runnable launch = new Runnable() {

                                            @Override
                                            public void run() {
                                                clusterViz.launchA4SolutionListWithClusterInfo(instancesForCluster, clusterSolutions, clusterNum, clusterSz);
                                            }
                                        };
                                        if (SwingUtilities.isEventDispatchThread())
                                            launch.run();
                                        else
                                            SwingUtilities.invokeLater(launch);
                                    }
                                }

                            } catch (Err e) {
                                cb(out, "bold", "Error during clustering: " + e.getMessage() + "\n");
                                cb(out, "bold", "Falling back to single visualization window\n");

                                // Fallback: show all solutions in one aggregated window
                                List<AlloyInstance> instancesList = new ArrayList<AlloyInstance>();
                                for (A4Solution sol : solutionsList) {
                                    try {
                                        AlloyInstance instance = StaticInstanceReader.a4SolutionToAlloyInstanceMaker(sol);
                                        instancesList.add(instance);
                                    } catch (Throwable e2) {
                                        cb(out, "bold", "Error converting solution to instance: " + e2.getMessage() + "\n");
                                    }
                                }
                                if (!instancesList.isEmpty()) {
                                    viz.launchA4SolutionList(instancesList);
                                }
                            }
                        } else {
                            // Not enough solutions for clustering, visualize all in one window
                            cb(out, "bold", "Not enough solutions for clustering (need at least " + numClusters + "), visualizing all in one window\n");
                            List<AlloyInstance> instancesList = new ArrayList<AlloyInstance>();
                            for (A4Solution sol : solutionsList) {
                                try {
                                    AlloyInstance instance = StaticInstanceReader.a4SolutionToAlloyInstanceMaker(sol);
                                    instancesList.add(instance);
                                } catch (Throwable e) {
                                    cb(out, "bold", "Error converting solution to instance: " + e.getMessage() + "\n");
                                }
                            }
                            if (!instancesList.isEmpty()) {
                                viz.launchA4SolutionList(instancesList);
                            }
                        }
                        /*
                         * if (ai == null) result.add(null); else if (ai.satisfiable()) {
                         * result.add(tempXML); PrintWriter writer; try { writer = new
                         * PrintWriter(HIDDEN_DIR_PATH + "atom2name.txt", "UTF-8"); for (Object s :
                         * ai.atom2name.keySet()) { writer.println(s.toString() + ":" +
                         * ai.atom2name.get(s)); } writer.close(); } catch (FileNotFoundException |
                         * UnsupportedEncodingException e) { // TODO Auto-generated catch block
                         * e.printStackTrace(); } } else if (ai.highLevelCore().a.size() > 0)
                         * result.add(tempCNF + ".core"); else result.add("");
                         */
                    }
            (new File(tempdir)).delete(); // In case it was UNSAT, or
                                         // canceled...
            if (result.size() > 1) {
                rep.cb("bold", "" + result.size() + " commands were executed. The results are:\n");
                for (int i = 0; i < result.size(); i++) {
                    Command r = world.getAllCommands().get(i);
                    if (result.get(i) == null) {
                        rep.cb("", "   #" + (i + 1) + ": Unknown.\n");
                        continue;
                    }
                    if (result.get(i).endsWith(".xml")) {
                        rep.cb("", "   #" + (i + 1) + ": ");
                        rep.cb("link", r.check ? "Counterexample found. " : "Instance found. ", "XML: " + result.get(i));
                        rep.cb("", r.label + (r.check ? " is invalid" : " is consistent"));
                        if (r.expects == 0)
                            rep.cb("", ", contrary to expectation");
                        else if (r.expects == 1)
                            rep.cb("", ", as expected");
                    } else if (result.get(i).endsWith(".core")) {
                        rep.cb("", "   #" + (i + 1) + ": ");
                        rep.cb("link", r.check ? "No counterexample found. " : "No instance found. ", "CORE: " + result.get(i));
                        rep.cb("", r.label + (r.check ? " may be valid" : " may be inconsistent"));
                        if (r.expects == 1)
                            rep.cb("", ", contrary to expectation");
                        else if (r.expects == 0)
                            rep.cb("", ", as expected");
                    } else {
                        if (r.check)
                            rep.cb("", "   #" + (i + 1) + ": No counterexample found. " + r.label + " may be valid");
                        else
                            rep.cb("", "   #" + (i + 1) + ": No instance found. " + r.label + " may be inconsistent");
                        if (r.expects == 1)
                            rep.cb("", ", contrary to expectation");
                        else if (r.expects == 0)
                            rep.cb("", ", as expected");
                    }
                    rep.cb("", ".\n");
                }
                rep.cb("", "\n");
            }
            if (rep.warn > 1)
                rep.cb("bold", "Note: There were " + rep.warn + " compilation warnings. Please scroll up to see them.\n");
            if (rep.warn == 1)
                rep.cb("bold", "Note: There was 1 compilation warning. Please scroll up to see it.\n");
        }
    }
}
