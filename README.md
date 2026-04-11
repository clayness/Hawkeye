# Hawkeye: An Interactive Enumerator for Alloy
`Hawkeye` is an extension to the [Alloy Analyzer](https://github.com/AlloyTools/org.alloytools.alloy) that enables users to guide the Analyzer's enumeration by communicating which elements of the current scenario a user would like to see stay the same or change. Hawkeye allows users to give these preferences at a high-level (sets of the scenario) and low-level (individual atoms of the scenario).

# Requirements:

* Operating Systems
  - Windows (64 bit)
  - Linux (64 bit)
  - Mac OS (64 bit)

# Folders in this repository
* "Jar" - Hawkeye's standalone executable jar file
* "Code" - Source code for Hawkeye
* "Model" - Models used in our evaluation and case study

# Installation:
Hawkeye is available as a pre-compiled executable jar file under the folder "Jar"
Additionally, Hawkeye can be built and compiled. If compiling Hawkeye, it is recommended you follow the installation instructions outlined [here](https://github.com/AlloyTools/org.alloytools.alloy) and reproduced below for convenience. Since Hawkeye builds on the Analyzer, it requires the user to set up and install the Analyzer's code base.

## Building Alloy

The Alloy build is using a _bnd workspace_ setup using a maven layout. This means it can be build  with Gradle and  the Eclipse IDE for interactive development. Projects are setup to continuously deliver the executable.

### Projects

The workspace is divided into a number of projects:

* [cnf](cnf) – Setup directory. Dependencies are specified in [cnf/central.xml] using the maven POM layout
* [org.alloytools.alloy.application](org.alloytools.alloy.application) – Main application code includes the parser, ast, visualiser, and application code
* [org.alloytools.alloy.dist](org.alloytools.alloy.dist) – Project to create the distribution executable JAR
* [org.alloytools.alloy.extra](org.alloytools.alloy.extra) – Models and examples
* [org.alloytools.pardinus](org.alloytools.pardinus) – A Kodkod extension without native code
* [org.alloytools.kodkod.nativesat](org.alloytools.kodkod.nativesat) – The native code libraries for Kodkod

### Relevant Project files

This workspace uses bnd. This means that the following have special meaning:

* [cnf/build.xml](cnf/build.xml) – Settings shared between projects
* ./bnd.bnd – Settings for a project. This file will _drag_ in code in a JAR.
* [cnf/central.xml](cnf/central.xml) – Dependencies from maven central

### Eclipse

The workspace is setup for interactive development in Eclipse with the Bndtools plugin. Download [Eclipse](https://www.eclipse.org/downloads/) and install it. You can then `Import` existing projects from the Git workspace. You should be asked to install Bndtools from the market place. You can also install Bndtools directly from the [Eclipse Market](https://marketplace.eclipse.org/content/bndtools) place (see `Help/Marketplace` and search for `Bndtools`). 

Bndtools will continuously create the final executable. The projects are setup to automatically update when a downstream project changes.

### IntelliJ IDEA (Ultimate Edition only)

Ensure you have the [Osmorc] plugin is enabled, as this plugin is needed for
Bndtools support. It should be enabled by default.

1. Choose "Import Project"
2. Select the `org.alloytools.alloy` directory.
3. Choose "Import project from external model: Bnd/Bndtools" and click "Next"
4. For "Select Bnd/Bndtools project to import", all projects should be checked
   by default, click "Next"
5. For project SDK, Choose "1.8", Click Finish

Note: do *not* link the Gradle project, as this will prevent you from running
Alloy within IDEA.

To run the Alloy GUI within IDEA, navigate to
org.alloytools.alloy.application/src/main/java/edu/mit/csail/sdg/alloy4whole/SimpleGUI and run the SimpleGUI class.

---

## Hawkeye clustered visualization (multigraph) — code flow

This section summarizes how **clustered instance views** and **Show Next Solution** work in the Hawkeye fork. Main entry points live under `Code/org.alloytools.alloy.application/`.

### 1. Initial run: enumerate a batch, cluster, open windows

- **`SimpleReporter.SimpleTask1`** runs the Alloy command and obtains the first satisfiable `A4Solution`.
- If the solver is **incremental**, it enumerates up to **20** further solutions by calling `A4Solution.next(...)` with empty constraint lists (each step excludes the previous model only).
- **`latestClusterSolution`** is set to the **last** solution in that batch (shared continuation cursor for later batches).
- If there are enough solutions, **`RelationalKMeansClusterer`** partitions them into *k* clusters (default *k* = 4).
- For each cluster, solutions are converted to **`AlloyInstance`** for the visualizer, and a **`VizGUI`** is created with a **per-window `Computer`** enumerator.
- **`VizGUI.launchA4SolutionListWithClusterInfo(..., clusterSolutions, ...)`** loads the aggregated graph (solid = in all solutions, dashed = in some) and stores the parallel **`A4Solution`** list in **`currentClusterA4Solutions`** for tuple-level constraints on the next step.

### 2. Show Next Solution (cluster window)

- **`VizGUI.doNext()`** collects Hawkeye same/diff choices from the UI and calls the window’s **`Computer`**.
- The cluster enumerator (defined inside **`SimpleTask1`**) runs **off the EDT**:
  - **Hides** all open cluster frames while working.
  - Reads **`latestClusterSolution`** as the start of the next enumeration segment.
  - From the **window that was clicked**, **`computeClusterFixedVars()`** builds tuple constraints: tuples **in every** cluster solution must stay present; tuples **in none** (within Kodkod upper bounds) must stay absent; tuples in **some but not all** are left free.
  - For each of up to **20** new solutions, those constraints are mapped to Kodkod **primary variable** ids and passed into **`A4Solution.next(same_atoms, diff_atoms, ...)`** together with any non-conflicting user selections.
  - **`latestClusterSolution`** is updated to the last solution produced.
  - The new batch is **re-clustered**; each **`VizGUI`** is **refreshed in place** by list index (`newClusters[i]` → `clusterVizWindows[i]`), passing fresh **`A4Solution`** lists so the next click uses the **current** cluster content.
- On error or “no more instances,” cluster frames are **shown again** and an alert is displayed.

### 3. Supporting APIs (for maintainers)

- Tuple variable ids come from **`kodkod.engine.SolutionIterator.getIndexToLit()`**, exposed through **`A4Solution.debugExtractIndexToLit()`** (incremental solvers only).
- Bounds for “never appeared” tuples use **`A4Solution.debugExtractKodkodBounds()`**.

For the standard (non-cluster) **Next** path from XML instances, see **`SimpleReporter.SimpleTask2`**.
