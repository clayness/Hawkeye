
# Hritik's Notes

## Building Hawkeye

**Requirements**

- **JDK:** 1.8  
- **Eclipse:** Version 2020-12  
  - Install the **bndtools** plugin from Eclipse Marketplace.
  - If prompted to *downgrade bndtools* or *upgrade Eclipse*, **choose to downgrade bndtools**.

---

## Key Visualization Files

These files are central to understanding and debugging Hawkeye’s visualization features:

- **OpenSelections.java**  
  *Entry point for Hawkeye buttons. Ideal for starting call stack debugging to trace code flow.*
- **DotStyle.java**  
  *Handles graph drawing logic.*
- **VizGUI.java**  
  *Main GUI logic for visualization.*
- **GraphEdge.java**  
  *Manages graph edge representation.*

---

## Attaching and Debugging a JAR in Eclipse

To debug a JAR file in Eclipse with its source code, follow these steps:

### Quick Reference

| Step                    | Action                                                                                 |
|-------------------------|----------------------------------------------------------------------------------------|
| Add JAR to Build Path   | Right-click project → Build Path → Configure Build Path → Libraries → Add External JARs|
| Attach Source Code      | Expand JAR in Libraries tab → Source attachment → Browse to source ZIP/folder          |
| Debug with Source       | Set breakpoints → Start debugging → Step into JAR code to view source                  |

---

### Step-by-Step Guide

**1. Add the JAR to Your Project’s Build Path**
- Right-click your project in Eclipse.
- Select **Build Path** → **Configure Build Path**.
- Go to the **Libraries** tab.
- Click **Add External JARs...** and select your JAR file. Click **OK** to add it.

**2. Attach the Source Code to the JAR**
- In the **Java Build Path** dialog (under the **Libraries** tab), expand the entry for your JAR file.
- Select the JAR, then click **Source attachment** (or **Attach Source...**).
- Browse to the location of the source code:
  - If you have a source ZIP (e.g., `src.zip`), select it.
  - If you have a source folder, select the folder containing `.java` files.
- Click **OK** to confirm.

**3. Debugging with Source Code**
- Set breakpoints in your code or the attached source files.
- Start debugging (Run → Debug).
- When you step into code from the JAR, Eclipse will display the actual source code, not just decompiled code.

---

You are now set up to debug into JAR code with full source visibility, making it easier to trace and fix issues during development.

---

#Original readme:

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
