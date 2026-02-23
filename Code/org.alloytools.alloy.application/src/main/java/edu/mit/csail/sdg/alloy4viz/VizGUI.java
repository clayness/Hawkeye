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

package edu.mit.csail.sdg.alloy4viz;

import static edu.mit.csail.sdg.alloy4.OurUtil.menu;
import static edu.mit.csail.sdg.alloy4.OurUtil.menuItem;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import edu.mit.csail.sdg.alloy4.A4Preferences.IntPref;
import edu.mit.csail.sdg.alloy4.A4Preferences.StringPref;
import edu.mit.csail.sdg.alloy4.Computer;
import edu.mit.csail.sdg.alloy4.ConstList;
import edu.mit.csail.sdg.alloy4.OurBorder;
import edu.mit.csail.sdg.alloy4.OurCheckbox;
import edu.mit.csail.sdg.alloy4.OurConsole;
import edu.mit.csail.sdg.alloy4.OurDialog;
import edu.mit.csail.sdg.alloy4.OurUtil;
import edu.mit.csail.sdg.alloy4.Runner;
import edu.mit.csail.sdg.alloy4.Util;
import edu.mit.csail.sdg.alloy4.Version;
import edu.mit.csail.sdg.alloy4graph.GraphViewer;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.ast.Sig.Field;
import edu.mit.csail.sdg.translator.A4Solution;


/**
 * GUI main window for the visualizer.
 * <p>
 * <b>Thread Safety:</b> Can be called only by the AWT event thread.
 */

public final class VizGUI implements ComponentListener {

    /** The background color for the toolbar. */
    private static final Color  background      = new Color(0.9f, 0.9f, 0.9f);

    /** The icon for a "checked" menu item. */
    private static final Icon   iconYes         = OurUtil.loadIcon("images/menu1.gif");

    /** The icon for an "unchecked" menu item. */
    private static final Icon   iconNo          = OurUtil.loadIcon("images/menu0.gif");

    /**
     * Whether the JVM should shutdown after the last file is closed.
     */
    private final boolean       standalone;

    /** The current display mode. */
    private VisualizerMode      currentMode     = VisualizerMode.get();

    private String              wbMode          = "";

    /**
     * The JFrame for the main GUI window; or null if we intend to display the graph
     * inside a user-given JPanel instead.
     */
    private final JFrame        frame;

    private final JToolBar      toolbar;

    /** The projection popup menu. */
    private final JPopupMenu    projectionPopup;

    /** The buttons on the toolbar. */
    private final JButton       projectionButton, openSettingsButton, closeSettingsButton, magicLayout,
                    loadSettingsButton, saveSettingsButton, saveAsSettingsButton, resetSettingsButton, updateSettingsButton,
                    openEvaluatorButton, closeEvaluatorButton, enumerateButton, vizButton, treeButton,
                    txtButton, tableButton, graphButton, textButton/* , dotButton, xmlButton */;

    /**
     * This list must contain all the display mode buttons (that is, vizButton,
     * xmlButton...)
     */
    private final List<JButton> solutionButtons = new ArrayList<JButton>();

    /** The "theme" menu. */
    private final JMenu         thememenu;

    /** The "window" menu. */
    private final JMenu         windowmenu;

    /** The "show next" menu item. */
    private final JMenuItem     enumerateMenu;

    /** Current font size. */
    private int                 fontSize        = 12;

    /**
     * 0: theme and evaluator are both invisible; 1: theme is visible; 2: evaluator
     * is visible.
     */
    private int                 settingsOpen    = 0;

    /**
     * The current instance and visualization settings; null if none is loaded.
     */
    private VizState            myState         = null;

    /**
     * Returns the current visualization settings (and you can call
     * getOriginalInstance() on it to get the current instance). If you make changes
     * to the state, you should call doApply() on the VizGUI object to refresh the
     * screen.
     */
    public VizState getVizState() {
        return myState;
    }

    /**
     * The customization panel to the left; null if it is not yet loaded.
     */
    private VizCustomizationPanel myCustomPanel    = null;

    /**
     * The evaluator panel to the left; null if it is not yet loaded.
     */
    private OurConsole            myEvaluatorPanel = null;

    /**
     * The graphical panel to the right; null if it is not yet loaded.
     */
    private VizGraphPanel         myGraphPanel     = null;

    /**
     * The splitpane between the customization panel and the graph panel.
     */
    private final JSplitPane      splitpane;

    /**
     * The tree or graph or text being displayed on the right hand side.
     */
    private JComponent            content          = null;

    /**
     * Returns the JSplitPane containing the customization/evaluator panel in the
     * left and the graph on the right.
     */
    public JSplitPane getPanel() {
        return splitpane;
    }

    /** Returns the main frame of the visualizer */
    public JFrame getFrame() {
        return frame;
    }

    /**
     * The last known divider position between the customization panel and the graph
     * panel.
     */
    private int                 lastDividerPosition = 0;

    /**
     * If nonnull, you can pass in an expression to be evaluated. If it throws an
     * exception, that means an error has occurred.
     */
    private final Computer      evaluator;

    /**
     * If nonnull, you can pass in an XML file to find the next solution.
     */
    private final Computer      enumerator;

    private JToolBar            wbToolbar;
    private final List<JButton> solutionButtonsWB   = new ArrayList<JButton>();

    private boolean             redraw_selection    = true;
    private JSplitPane          hawkeye_displays;
    private JScrollPane         hawkeye_selections;
    private String              PROJECT_DIR_PATH    = System.getProperty("user.dir");
    private final String        HIDDEN_DIR_PATH     = "";

    // ==============================================================================================//

    /**
     * The current theme file; "" if there is no theme file loaded.
     */
    private String              thmFileName         = "";

    /**
     * Returns the current THM filename; "" if no theme file is currently loaded.
     */
    public String getThemeFilename() {
        return thmFileName;
    }

    // ==============================================================================================//

    /**
     * The current XML file; "" if there is no XML file loaded.
     */
    private String xmlFileName = "";

    /**
     * Returns the current XML filename; "" if no file is currently loaded.
     */
    public String getXMLfilename() {
        return xmlFileName;
    }

    // ==============================================================================================//

    /** The list of XML files loaded in this session so far. */
    private final List<String> xmlLoaded = new ArrayList<String>();

    /**
     * Return the list of XML files loaded in this session so far.
     */
    public ConstList<String> getInstances() {
        return ConstList.make(xmlLoaded);
    }

    // ==============================================================================================//

    /** This maps each XML filename to a descriptive title. */
    private Map<String,String> xml2title = new LinkedHashMap<String,String>();

    /**
     * Returns a short descriptive title associated with an XML file.
     */
    public String getInstanceTitle(String xmlFileName) {
        String answer = xml2title.get(Util.canon(xmlFileName));
        return (answer == null) ? "(unknown)" : answer;
    }

    // ==============================================================================================//

    /** Add a vertical divider to the toolbar. */
    private void addDivider() {
        JPanel divider = OurUtil.makeH(new Dimension(1, 40), Color.LIGHT_GRAY);
        divider.setAlignmentY(0.5f);
        if (!Util.onMac())
            toolbar.add(OurUtil.makeH(5, background));
        else
            toolbar.add(OurUtil.makeH(5));
        toolbar.add(divider);
        if (!Util.onMac())
            toolbar.add(OurUtil.makeH(5, background));
        else
            toolbar.add(OurUtil.makeH(5));
    }

    // ======== The Preferences
    // ======================================================================================//
    // ======== Note: you must make sure each preference has a unique key
    // ============================================//

    /**
     * This enum defines the set of possible visualizer modes.
     */
    private enum VisualizerMode {

                                 /** Visualize using graphviz's dot. */
                                 Viz("graphviz"),
                                 // /** See the DOT content. */ DOT("dot"),
                                 // /** See the XML content. */ XML("xml"),
                                 /** See the instance as text. */
                                 TEXT("txt"),
                                 TABLE("table"),
                                 /** See the instance as a tree. */
                                 Tree("tree");

        /**
         * This is a unique String for this value; it should be kept consistent in
         * future versions.
         */
        private final String id;

        /**
         * Constructs a new VisualizerMode value with the given id.
         */
        private VisualizerMode(String id) {
            this.id = id;
        }

        /**
         * Given an id, return the enum value corresponding to it (if there's no match,
         * then return Viz).
         */
        private static VisualizerMode parse(String id) {
            for (VisualizerMode vm : values())
                if (vm.id.equals(id))
                    return vm;
            return Viz;
        }

        /** Saves this value into the Java preference object. */
        public void set() {
            Preferences.userNodeForPackage(Util.class).put("VisualizerMode", id);
        }

        /**
         * Reads the current value of the Java preference object (if it's not set, then
         * return Viz).
         */
        public static VisualizerMode get() {
            return parse(Preferences.userNodeForPackage(Util.class).get("VisualizerMode", ""));
        }
    };

    private enum WBMode {

                         Graph("graph"),
                         // /** See the DOT content. */ DOT("dot"),
                         // /** See the XML content. */ XML("xml"),
                         Text("text");

        private final String id;

        private WBMode(String id) {
            this.id = id;
        }
    };


    /**
     * The latest X corrdinate of the Alloy Visualizer window.
     */
    private static final IntPref    VizX      = new IntPref("VizX", 0, -1, 65535);

    /**
     * The latest Y corrdinate of the Alloy Visualizer window.
     */
    private static final IntPref    VizY      = new IntPref("VizY", 0, -1, 65535);

    /** The latest width of the Alloy Visualizer window. */
    private static final IntPref    VizWidth  = new IntPref("VizWidth", 0, -1, 65535);

    /** The latest height of the Alloy Visualizer window. */
    private static final IntPref    VizHeight = new IntPref("VizHeight", 0, -1, 65535);

    /**
     * The first file in Alloy Visualizer's "open recent theme" list.
     */
    private static final StringPref Theme0    = new StringPref("Theme0");

    /**
     * The second file in Alloy Visualizer's "open recent theme" list.
     */
    private static final StringPref Theme1    = new StringPref("Theme1");

    /**
     * The third file in Alloy Visualizer's "open recent theme" list.
     */
    private static final StringPref Theme2    = new StringPref("Theme2");

    /**
     * The fourth file in Alloy Visualizer's "open recent theme" list.
     */
    private static final StringPref Theme3    = new StringPref("Theme3");

    // ==============================================================================================//

    /**
     * If true, that means the event handlers should return a Runner encapsulating
     * them, rather than perform the actual work.
     */
    private boolean                 wrap      = false;


    /**
     * Wraps the calling method into a Runnable whose run() will call the calling
     * method with (false) as the only argument.
     */
    private Runner wrapMe() {
        final String name;
        try {
            throw new Exception();
        } catch (Exception ex) {
            name = ex.getStackTrace()[1].getMethodName();
        }
        Method[] methods = getClass().getDeclaredMethods();
        Method m = null;
        for (int i = 0; i < methods.length; i++)
            if (methods[i].getName().equals(name)) {
                m = methods[i];
                break;
            }
        if (m == null) {
            throw new IllegalStateException("Missing method " + name);
        }

        final Method method = m;
        return new Runner() {

            private static final long serialVersionUID = 0;

            @Override
            public void run() {
                try {
                    method.setAccessible(true);
                    method.invoke(VizGUI.this, new Object[] {});
                } catch (Throwable ex) {
                    ex = new IllegalArgumentException("Failed call to " + name + "()", ex);
                    Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), ex);
                }
            }

            @Override
            public void run(Object arg) {
                run();
            }
        };
    }

    /**
     * Wraps the calling method into a Runnable whose run() will call the calling
     * method with (false,argument) as the two arguments.
     */
    private Runner wrapMe(final Object argument) {
        final String name;
        try {
            throw new Exception();
        } catch (Exception ex) {
            name = ex.getStackTrace()[1].getMethodName();
        }
        Method[] methods = getClass().getDeclaredMethods();
        Method m = null;
        for (int i = 0; i < methods.length; i++)
            if (methods[i].getName().equals(name)) {
                m = methods[i];
                break;
            }

        if (m == null) {
            throw new IllegalStateException("Missing method " + name);
        }

        final Method method = m;
        return new Runner() {

            private static final long serialVersionUID = 0;

            @Override
            public void run(Object arg) {
                try {
                    method.setAccessible(true);
                    method.invoke(VizGUI.this, new Object[] {
                                                             arg
                    });
                } catch (Throwable ex) {
                    ex = new IllegalArgumentException("Failed call to " + name + "(" + arg + ")", ex);
                    Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), ex);
                }
            }

            @Override
            public void run() {
                run(argument);
            }
        };
    }

    /**
     * Creates a new visualization GUI window; this method can only be called by the
     * AWT event thread.
     *
     * @param standalone - whether the JVM should shutdown after the last file is
     *            closed
     * @param xmlFileName - the filename of the incoming XML file; "" if there's no
     *            file to open
     * @param windowmenu - if standalone==false and windowmenu!=null, then this will
     *            be added as a menu on the menubar
     *            <p>
     *            Note: if standalone==false and xmlFileName.length()==0, then we
     *            will initially hide the window.
     */
    public VizGUI(boolean standalone, String xmlFileName, JMenu windowmenu) {
        this(standalone, xmlFileName, windowmenu, null, null);
    }

    /**
     * Creates a new visualization GUI window; this method can only be called by the
     * AWT event thread.
     *
     * @param standalone - whether the JVM should shutdown after the last file is
     *            closed
     * @param xmlFileName - the filename of the incoming XML file; "" if there's no
     *            file to open
     * @param windowmenu - if standalone==false and windowmenu!=null, then this will
     *            be added as a menu on the menubar
     * @param enumerator - if it's not null, it provides solution enumeration
     *            ability
     * @param evaluator - if it's not null, it provides solution evaluation ability
     *            <p>
     *            Note: if standalone==false and xmlFileName.length()==0, then we
     *            will initially hide the window.
     */
    public VizGUI(boolean standalone, String xmlFileName, JMenu windowmenu, Computer enumerator, Computer evaluator) {
        this(standalone, xmlFileName, windowmenu, enumerator, evaluator, true);
    }

    /**
     * Creates a new visualization GUI window; this method can only be called by the
     * AWT event thread.
     *
     * @param standalone - whether the JVM should shutdown after the last file is
     *            closed
     * @param xmlFileName - the filename of the incoming XML file; "" if there's no
     *            file to open
     * @param windowmenu - if standalone==false and windowmenu!=null, then this will
     *            be added as a menu on the menubar
     * @param enumerator - if it's not null, it provides solution enumeration
     *            ability
     * @param evaluator - if it's not null, it provides solution evaluation ability
     * @param makeWindow - if false, then we will only construct the JSplitPane,
     *            without making the window
     *            <p>
     *            Note: if standalone==false and xmlFileName.length()==0 and
     *            makeWindow==true, then we will initially hide the window.
     */
    public VizGUI(boolean standalone, String xmlFileName, JMenu windowmenu, Computer enumerator, Computer evaluator, boolean makeWindow) {

        this.enumerator = enumerator;
        this.standalone = standalone;
        this.evaluator = evaluator;
        this.frame = makeWindow ? new JFrame("Alloy Visualizer") : null;

        // Figure out the desired x, y, width, and height
        int screenWidth = OurUtil.getScreenWidth(), screenHeight = OurUtil.getScreenHeight();
        int width = VizWidth.get();
        if (width < 0)
            width = screenWidth - 150;
        else if (width < 100)
            width = 100;
        if (width > screenWidth)
            width = screenWidth;
        int height = VizHeight.get();
        if (height < 0)
            height = screenHeight - 150;
        else if (height < 100)
            height = 100;
        if (height > screenHeight)
            height = screenHeight;
        int x = VizX.get();
        if (x < 0 || x > screenWidth - 10)
            x = 0;
        int y = VizY.get();
        if (y < 0 || y > screenHeight - 10)
            y = 0;

        // Create the menubar
        JMenuBar mb = new JMenuBar();
        try {
            wrap = true;
            JMenu fileMenu = menu(mb, "&File", null);
            menuItem(fileMenu, "Open...", 'O', 'O', doLoad());
            JMenu exportMenu = menu(null, "&Export To", null);
            menuItem(exportMenu, "Dot...", 'D', 'D', doExportDot());
            menuItem(exportMenu, "XML...", 'X', 'X', doExportXml());
            fileMenu.add(exportMenu);
            menuItem(fileMenu, "Close", 'W', 'W', doClose());
            if (standalone)
                menuItem(fileMenu, "Quit", 'Q', 'Q', doCloseAll());
            else
                menuItem(fileMenu, "Close All", 'A', doCloseAll());
            JMenu instanceMenu = menu(mb, "&Instance", null);
            enumerateMenu = menuItem(instanceMenu, "Show Next Solution", 'N', 'N', doNext());
            thememenu = menu(mb, "&Theme", doRefreshTheme());
            if (standalone || windowmenu == null)
                windowmenu = menu(mb, "&Window", doRefreshWindow());
            this.windowmenu = windowmenu;
        } finally {
            wrap = false;
        }
        mb.add(windowmenu);
        thememenu.setEnabled(false);
        windowmenu.setEnabled(false);
        if (frame != null)
            frame.setJMenuBar(mb);

        // Create the toolbar
        projectionPopup = new JPopupMenu();
        projectionButton = new JButton("Projection: none");
        projectionButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                repopulateProjectionPopup();
                if (projectionPopup.getComponentCount() > 0)
                    projectionPopup.show(projectionButton, 10, 10);
            }
        });
        repopulateProjectionPopup();
        toolbar = new JToolBar();
        toolbar.setVisible(false);
        toolbar.setFloatable(false);
        toolbar.setBorder(null);

        wbToolbar = new JToolBar();
        wbToolbar.setFloatable(false);

        if (!Util.onMac())
            toolbar.setBackground(background);
        wbToolbar.setBackground(background);
        try {
            wrap = true;
            vizButton = makeSolutionButton("Viz", "Show Visualization", "images/show.png", doShowViz());
            //         dotButton=makeSolutionButton("Dot", "Show the Dot File for the Graph", "images/24_plaintext.gif", doShowDot());
            //         xmlButton=makeSolutionButton("XML", "Show XML", "images/24_plaintext.gif", doShowXML());
            tableButton = makeSolutionButton("Table", "Show the table output for the Graph", "images/24_plaintext.gif", doShowTable());
            txtButton = makeSolutionButton("Txt", "Show the textual output for the Graph", "images/text.png", doShowTxt());
            treeButton = makeSolutionButton("Tree", "Show Tree", "images/tree.png", doShowTree());
            if (frame != null)
                addDivider();
            toolbar.add(closeSettingsButton = OurUtil.button("Close", "Close the theme customization panel", "images/bigClose.png", doCloseThemePanel(), background));
            toolbar.add(updateSettingsButton = OurUtil.button("Apply", "Apply the changes to the current theme", "images/apply.png", doApply(), background));
            toolbar.add(openSettingsButton = OurUtil.button("Theme", "Open the theme customization panel", "images/theme.png", doOpenThemePanel(), background));
            toolbar.add(magicLayout = OurUtil.button("Magic Layout", "Automatic theme customization (will reset current theme)", "images/magiclayout.png", doMagicLayout(), background));
            toolbar.add(openEvaluatorButton = OurUtil.button("Evaluator", "Open the evaluator", "images/evaluator.png", doOpenEvalPanel(), background));
            toolbar.add(closeEvaluatorButton = OurUtil.button("Close Evaluator", "Close the evaluator", "images/bigClose.png", doCloseEvalPanel(), background));
            toolbar.add(enumerateButton = OurUtil.button("Next", "Show the next solution", "images/next.png", doNext(), background));
            if (frame != null)
                addDivider();
            toolbar.add(projectionButton);
            toolbar.add(loadSettingsButton = OurUtil.button("Load", "Load the theme customization from a theme file", "images/24_open.gif", doLoadTheme()));
            toolbar.add(saveSettingsButton = OurUtil.button("Save", "Save the current theme customization", "images/24_save.gif", doSaveTheme()));
            toolbar.add(saveAsSettingsButton = OurUtil.button("Save As", "Save the current theme customization as a new theme file", "images/24_save.gif", doSaveThemeAs()));
            toolbar.add(resetSettingsButton = OurUtil.button("Reset", "Reset the theme customization", "images/24_settings_close2.gif", doResetTheme()));

            textButton = makeSolutionButtonWB("Text", "Show the textual output for why the command is true.", "images/text.png", doShowText());
            graphButton = makeSolutionButtonWB("Graph", "Show the graph output for why the command is true", "images/show.png", doShowGraph());
            JPanel divider = OurUtil.makeH(new Dimension(1, 40), Color.LIGHT_GRAY);
            divider.setAlignmentY(0.5f);
            wbToolbar.add(divider);
            wbToolbar.add(OurUtil.makeH(5, background));
        } finally {
            wrap = false;
        }
        settingsOpen = 0;


        wbMode = "text";

        // Create the horizontal split pane
        splitpane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitpane.setOneTouchExpandable(false);
        splitpane.setResizeWeight(0.);
        splitpane.setContinuousLayout(true);
        splitpane.setBorder(null);
        ((BasicSplitPaneUI) (splitpane.getUI())).getDivider().setBorder(new OurBorder(false, true, false, false));

        // Display the window, then proceed to load the input file
        if (frame != null) {
            frame.pack();
            if (!Util.onMac() && !Util.onWindows()) {
                // many Window managers do not respect ICCCM2; this should help
                // avoid the Title Bar being shifted "off screen"
                if (x < 30) {
                    if (x < 0)
                        x = 0;
                    width = width - (30 - x);
                    x = 30;
                }
                if (y < 30) {
                    if (y < 0)
                        y = 0;
                    height = height - (30 - y);
                    y = 30;
                }
                if (width < 100)
                    width = 100;
                if (height < 100)
                    height = 100;
            }
            frame.setSize(width, height);
            frame.setLocation(x, y);
            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            try {
                wrap = true;
                frame.addWindowListener(doClose());
            } finally {
                wrap = false;
            }
            frame.addComponentListener(this);
        }
        if (xmlFileName.length() > 0)
            doLoadInstance(xmlFileName);
    }

    /** Invoked when the Visualizationwindow is resized. */
    @Override
    public void componentResized(ComponentEvent e) {
        componentMoved(e);
    }

    /** Invoked when the Visualizationwindow is moved. */
    @Override
    public void componentMoved(ComponentEvent e) {
        if (frame != null) {
            VizWidth.set(frame.getWidth());
            VizHeight.set(frame.getHeight());
            VizX.set(frame.getX());
            VizY.set(frame.getY());
        }
    }

    /** Invoked when the Visualizationwindow is shown. */
    @Override
    public void componentShown(ComponentEvent e) {
    }

    /** Invoked when the Visualizationwindow is hidden. */
    @Override
    public void componentHidden(ComponentEvent e) {
    }

    /**
     * Helper method that repopulates the Porjection popup menu.
     */
    private void repopulateProjectionPopup() {
        int num = 0;
        String label = "Projection: none";
        if (myState == null) {
            projectionButton.setEnabled(false);
            return;
        }
        projectionButton.setEnabled(true);
        projectionPopup.removeAll();
        final Set<AlloyType> projected = myState.getProjectedTypes();
        for (final AlloyType t : myState.getOriginalModel().getTypes())
            if (myState.canProject(t)) {
                final boolean on = projected.contains(t);
                final JMenuItem m = new JMenuItem(t.getName(), on ? OurCheckbox.ON : OurCheckbox.OFF);
                m.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (on)
                            myState.deproject(t);
                        else
                            myState.project(t);
                        updateDisplay();
                    }
                });
                projectionPopup.add(m);
                if (on) {
                    num++;
                    if (num == 1)
                        label = "Projected over " + t.getName();
                }
            }
        projectionButton.setText(num > 1 ? ("Projected over " + num + " sigs") : label);
    }

    /**
     * Helper method that refreshes the right-side visualization panel with the
     * latest settings.
     */


    ArrayList<OptionSelections> user_selections;
    JPanel                      ast_decomp = new JPanel(new BorderLayout());

    @SuppressWarnings("deprecation" )
    private void updateDisplay() {
        if (myState == null)
            return;
        // First, update the toolbar
        currentMode.set();
        for (JButton button : solutionButtons)
            button.setEnabled(settingsOpen != 1);
        switch (currentMode) {
            case Tree :
                treeButton.setEnabled(false);
                break;
            case TEXT :
                txtButton.setEnabled(false);
                break;
            case TABLE :
                tableButton.setEnabled(false);
                break;
            // case XML: xmlButton.setEnabled(false); break;
            // case DOT: dotButton.setEnabled(false); break;
            default :
                vizButton.setEnabled(false);
        }

        for (JButton button : solutionButtonsWB)
            button.setEnabled(settingsOpen != 1);
        switch (wbMode) {
            case "text" :
                textButton.setEnabled(false);
                break;
            case "graph" :
                graphButton.setEnabled(false);
                break;
        }
        textButton.setVisible(frame != null);
        graphButton.setVisible(frame != null);

        final boolean isMeta = myState.getOriginalInstance().isMetamodel;
        vizButton.setVisible(frame != null);
        treeButton.setVisible(frame != null);
        txtButton.setVisible(frame != null);
        // dotButton.setVisible(frame!=null);
        // xmlButton.setVisible(frame!=null);
        magicLayout.setVisible((settingsOpen == 0 || settingsOpen == 1) && currentMode == VisualizerMode.Viz);
        projectionButton.setVisible((settingsOpen == 0 || settingsOpen == 1) && currentMode == VisualizerMode.Viz);
        openSettingsButton.setVisible(settingsOpen == 0 && currentMode == VisualizerMode.Viz);
        loadSettingsButton.setVisible(frame == null && settingsOpen == 1 && currentMode == VisualizerMode.Viz);
        saveSettingsButton.setVisible(frame == null && settingsOpen == 1 && currentMode == VisualizerMode.Viz);
        saveAsSettingsButton.setVisible(frame == null && settingsOpen == 1 && currentMode == VisualizerMode.Viz);
        resetSettingsButton.setVisible(frame == null && settingsOpen == 1 && currentMode == VisualizerMode.Viz);
        closeSettingsButton.setVisible(settingsOpen == 1 && currentMode == VisualizerMode.Viz);
        updateSettingsButton.setVisible(settingsOpen == 1 && currentMode == VisualizerMode.Viz);
        openEvaluatorButton.setVisible(!isMeta && settingsOpen == 0 && evaluator != null);
        closeEvaluatorButton.setVisible(!isMeta && settingsOpen == 2 && evaluator != null);
        enumerateMenu.setEnabled(!isMeta && settingsOpen == 0 && enumerator != null);
        enumerateButton.setVisible(!isMeta && settingsOpen == 0 && enumerator != null);
        toolbar.setVisible(true);
        wbToolbar.setVisible(true);
        // Now, generate the graph or tree or textarea that we want to display
        // on the right
        if (frame != null)
            frame.setTitle(makeVizTitle());
        switch (currentMode) {
            case Tree : {
                final VizTree t = new VizTree(myState.getOriginalInstance().originalA4, makeVizTitle(), fontSize);
                final JScrollPane scroll = OurUtil.scrollpane(t, Color.BLACK, Color.WHITE, new OurBorder(true, false, true, false));
                scroll.addFocusListener(new FocusListener() {

                    @Override
                    public final void focusGained(FocusEvent e) {
                        t.requestFocusInWindow();
                    }

                    @Override
                    public final void focusLost(FocusEvent e) {
                    }
                });
                content = scroll;
                break;
            }
            case TEXT : {
                String textualOutput = myState.getOriginalInstance().originalA4.toString();
                content = getTextComponent(textualOutput);
                break;
            }
            case TABLE : {
                String textualOutput = myState.getOriginalInstance().originalA4.format();
                content = getTextComponent(textualOutput);
                break;
            }
            // case XML: {
            // content=getTextComponent(xmlFileName);
            // break;
            // }
            default : {
                // Check if we should use aggregated rendering with frequency-based styling
                if (isAggregatedMode()) {
                    try {
                        content = StaticGraphMaker.produceAggregatedGraph(currentAggregatedData);
                    } catch (Throwable e) {
                        OurDialog.alert("Error rendering aggregated graph: " + e.getMessage());
                        // Fallback to regular rendering
                        if (myGraphPanel == null) {
                            myGraphPanel = new VizGraphPanel(myState, false);
                        } else {
                            myGraphPanel.seeDot(false);
                            myGraphPanel.remakeAll();
                        }
                        content = myGraphPanel;
                    }
                } else {
                    // Regular single-instance rendering
                    if (myGraphPanel == null) {
                        myGraphPanel = new VizGraphPanel(myState, false);
                    } else {
                        myGraphPanel.seeDot(false);
                        myGraphPanel.remakeAll();
                    }
                    content = myGraphPanel;
                }
            }
        }
        // Now that we've re-constructed "content", let's set its font size
        if (currentMode != VisualizerMode.Tree) {
            content.setFont(OurUtil.getVizFont().deriveFont((float) fontSize));
            content.invalidate();
            content.repaint();
            content.validate();
        }
        // Now, display them!
        final Box instanceTopBox = Box.createHorizontalBox();
        instanceTopBox.add(toolbar);
        final JPanel instanceArea = new JPanel(new BorderLayout());
        instanceArea.add(instanceTopBox, BorderLayout.NORTH);
        instanceArea.add(content, BorderLayout.CENTER);
        instanceArea.setVisible(true);
        if (!Util.onMac()) {
            instanceTopBox.setBackground(background);
            instanceArea.setBackground(background);
        }

        String filename = myState.getOriginalInstance().filename;
        String cmd_name = myState.getOriginalInstance().commandname;
        cmd_name = cmd_name.substring(cmd_name.indexOf(" ") + 1);
        String[] placeholder = cmd_name.split(" ");
        cmd_name = placeholder[0];



        if (redraw_selection) {

            redraw_selection = false;
            final JPanel ast_decomp = new JPanel();

            GridBagConstraints c = new GridBagConstraints();
            GridBagLayout gridbag = new GridBagLayout();
            c.gridx = 0;
            c.gridy = 0;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.fill = GridBagConstraints.HORIZONTAL;

            A4Solution curr_instance = myState.getOriginalInstance().originalA4;
            HashMap<Integer,String> lit_to_tuple = new HashMap<Integer,String>();
            HashMap<Integer,String> tuple_to_text = new HashMap<Integer,String>();
            HashMap<String,String> skolems = new HashMap<String,String>();

            try {
                File myObj = new File(HIDDEN_DIR_PATH + "atom2name.txt");
                Scanner myReader = new Scanner(myObj);
                while (myReader.hasNextLine()) {
                    String data = myReader.nextLine();
                    String[] temp = data.split(":");
                    skolems.put(temp[0], temp[1].replaceAll("\\$", ""));
                }
                myReader.close();
            } catch (FileNotFoundException e) {
                System.out.println("An error occurred.");
                File delete_creations = new File(HIDDEN_DIR_PATH + "atom2name.txt");
                delete_creations.delete();
                e.printStackTrace();
            }

            try {
                File myObj = new File(HIDDEN_DIR_PATH + "lits.txt");
                Scanner myReader = new Scanner(myObj);
                while (myReader.hasNextLine()) {
                    String data = myReader.nextLine();
                    String[] temp = data.split(",");
                    for (String s : skolems.keySet()) {
                        temp[2] = temp[2].replace(s, skolems.get(s));
                    }

                    tuple_to_text.put(Integer.valueOf(temp[1]), temp[0]);
                    lit_to_tuple.put(Integer.valueOf(temp[1]), temp[2]);
                }
                myReader.close();
            } catch (FileNotFoundException e) {
                System.out.println("An error occurred.");
                File delete_creations = new File(HIDDEN_DIR_PATH + "lits.txt");
                delete_creations.delete();
                delete_creations = new File(HIDDEN_DIR_PATH + "atom2name.txt");
                delete_creations.delete();
                e.printStackTrace();
            }



            File delete_creations = new File(Paths.get(HIDDEN_DIR_PATH + "lits.txt").toString());
            delete_creations.delete();
            delete_creations = new File(HIDDEN_DIR_PATH + "atom2name.txt");
            delete_creations.delete();

            ArrayList<String> sigs = new ArrayList<String>();
            ArrayList<String> abstractSig = new ArrayList<String>();
            HashMap<String,ArrayList<String>> sigs_and_rels = new HashMap<String,ArrayList<String>>();
            user_selections = new ArrayList<OptionSelections>();

            for (Sig sig : curr_instance.getAllReachableSigs()) {
                if (!sig.label.equals("seq/Int") && !sig.label.equals("none") && !sig.label.equals("univ") && !sig.label.equals("Int") && !sig.label.equals("String")) {
                    sigs.add(sig.label);

                    if (sig.isAbstract != null) {
                        abstractSig.add(sig.label);
                    }

                    sigs_and_rels.put(sig.label, new ArrayList<String>());
                    for (Field rel : sig.getFields()) {
                        sigs_and_rels.get(sig.label).add(rel.label);
                    }
                }
            }

            c = new GridBagConstraints();
            gridbag = new GridBagLayout();
            c.gridx = 0;
            c.gridy = 0;
            c.anchor = GridBagConstraints.NORTHWEST;
            //c.weightx = 1;
            //c.weighty = 1;

            JPanel select_boxes = new JPanel();
            select_boxes.setLayout(gridbag);
            select_boxes.setBackground(Color.WHITE);

            JPanel wrapper = new JPanel();
            wrapper.setLayout(gridbag);
            wrapper.setBackground(Color.WHITE);

            hawkeye_selections = new JScrollPane(wrapper);
            hawkeye_selections.getVerticalScrollBar().setUnitIncrement(16);
            hawkeye_selections.setBackground(Color.WHITE);

            JLabel selection_title = new JLabel("Select which signature(s)/relation(s) to keep constant or change:");
            Border border = selection_title.getBorder();
            Border margin = new EmptyBorder(10, 10, 5, 10);
            selection_title.setBorder(new CompoundBorder(border, margin));
            wrapper.add(selection_title, c);

            int num_highlevel = 0;
            for (String sig : sigs) {

                if (abstractSig.contains(sig)) {
                    JLabel sig_name = new JLabel("<html><b>Signature: </b>" + sig + " cannot be modified as it is abstract.");
                    border = sig_name.getBorder();
                    margin = new EmptyBorder(0, 20, 0, 5);
                    sig_name.setBorder(new CompoundBorder(border, margin));
                    c.gridx = 0;
                    c.gridy = c.gridy + 1;
                    c.anchor = GridBagConstraints.LINE_START;
                    select_boxes.add(sig_name, c);
                    c.anchor = GridBagConstraints.NORTHWEST;

                    c.gridy = c.gridy + 1;
                    c.anchor = GridBagConstraints.LINE_START;
                    select_boxes.add(new JSeparator(), c);
                } else {
                    user_selections.add(new OptionSelections(sig));

                    //Generate display label for the name
                    JLabel sig_name = new JLabel("<html><b>Signature: </b>" + sig);
                    border = sig_name.getBorder();
                    margin = new EmptyBorder(0, 20, 0, 5);
                    sig_name.setBorder(new CompoundBorder(border, margin));
                    c.gridx = 0;
                    c.gridy = c.gridy + 1;
                    c.anchor = GridBagConstraints.LINE_START;
                    select_boxes.add(sig_name, c);
                    c.anchor = GridBagConstraints.NORTHWEST;

                    //Add all the high level options for the sig/relation at large
                    List<AbstractButton> btns = Collections.list(user_selections.get(num_highlevel).high_level_btns.getElements());
                    for (AbstractButton btn : btns) {
                        c.gridx = c.gridx + 1;
                        select_boxes.add(btn, c);
                    }

                    int num_atoms = 0;
                    for (Integer index : lit_to_tuple.keySet()) {
                        String check_sig = sig;//.replaceAll("this/", "");
                        if (tuple_to_text.get(index).startsWith(check_sig) && !(tuple_to_text.get(index).contains("."))) {
                            user_selections.get(num_highlevel).addAtom(lit_to_tuple.get(index), index);
                            JLabel atom_name = new JLabel(lit_to_tuple.get(index));
                            border = atom_name.getBorder();
                            margin = new EmptyBorder(0, 40, 0, 5);
                            atom_name.setBorder(new CompoundBorder(border, margin));
                            c.gridx = 0;
                            c.gridy = c.gridy + 1;
                            c.anchor = GridBagConstraints.LINE_START;
                            select_boxes.add(atom_name, c);
                            c.anchor = GridBagConstraints.NORTHWEST;

                            //Add all the high level options for the sig/relation at large
                            btns = Collections.list(user_selections.get(num_highlevel).atom_level.get(num_atoms).getElements());
                            for (AbstractButton btn : btns) {
                                c.gridx = c.gridx + 1;
                                select_boxes.add(btn, c);
                            }
                            num_atoms++;
                        }
                    }

                    user_selections.get(num_highlevel).addAction();
                    num_highlevel++;
                }

                for (String rel : sigs_and_rels.get(sig)) {
                    user_selections.add(new OptionSelections(sig + "." + rel));

                    JLabel rel_name = new JLabel("<html><b>Relation: </b>" + sig + "." + rel);
                    border = rel_name.getBorder();
                    margin = new EmptyBorder(0, 20, 0, 5);
                    rel_name.setBorder(new CompoundBorder(border, margin));
                    c.gridx = 0;
                    c.anchor = GridBagConstraints.LINE_START;
                    c.gridy = c.gridy + 1;
                    select_boxes.add(rel_name, c);

                    //Add all the high level options for the sig/relation at large
                    List<AbstractButton> btns = Collections.list(user_selections.get(num_highlevel).high_level_btns.getElements());
                    for (AbstractButton btn : btns) {
                        c.gridx = c.gridx + 1;
                        select_boxes.add(btn, c);
                    }

                    int num_atoms = 0;
                    for (Integer index : lit_to_tuple.keySet()) {
                        if (tuple_to_text.get(index).equals(sig + "." + rel)) {
                            user_selections.get(num_highlevel).addAtom(lit_to_tuple.get(index), index);
                            JLabel atom_name = new JLabel(lit_to_tuple.get(index));
                            border = atom_name.getBorder();
                            margin = new EmptyBorder(0, 40, 0, 5);
                            atom_name.setBorder(new CompoundBorder(border, margin));
                            c.gridx = 0;
                            c.gridy = c.gridy + 1;
                            c.anchor = GridBagConstraints.LINE_START;
                            select_boxes.add(atom_name, c);
                            c.anchor = GridBagConstraints.NORTHWEST;

                            //Add all the high level options for the sig/relation at large
                            btns = Collections.list(user_selections.get(num_highlevel).atom_level.get(num_atoms).getElements());
                            for (AbstractButton btn : btns) {
                                c.gridx = c.gridx + 1;
                                select_boxes.add(btn, c);
                            }
                            num_atoms++;
                        }
                    }
                    user_selections.get(num_highlevel).addAction();
                    num_highlevel++;
                }

                c.gridy = c.gridy + 1;
                c.anchor = GridBagConstraints.LINE_START;
                select_boxes.add(new JSeparator(), c);
            }



            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            c.anchor = GridBagConstraints.NORTHWEST;
            wrapper.add(select_boxes, c);

            GridBagConstraints pushTopLeft = new GridBagConstraints();
            pushTopLeft.gridx = 0;
            pushTopLeft.gridy = 2;
            pushTopLeft.weightx = 1;
            pushTopLeft.weighty = 1;
            wrapper.add(new JLabel(" "), pushTopLeft);

            hawkeye_displays = new JSplitPane();
            hawkeye_displays = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
            hawkeye_displays.setOneTouchExpandable(false);
            hawkeye_displays.setContinuousLayout(true);
            hawkeye_displays.setResizeWeight(0.5D);
            hawkeye_displays.setDividerSize(6);
            hawkeye_displays.setUI(new BasicSplitPaneUI() {

                @Override
                public BasicSplitPaneDivider createDefaultDivider() {
                    return new BasicSplitPaneDivider(this) {

                        @Override
                        public void setBorder(Border b) {

                        }

                        @Override
                        public void paint(Graphics g) {
                            g.setColor(new Color(220, 220, 220));
                            g.fillRect(1, 0, getSize().width - 2, getSize().height);
                            g.setColor(new Color(232, 232, 232));
                            g.fillRect(0, 0, 1, getSize().height);
                            g.setColor(new Color(173, 173, 173));
                            g.fillRect(getSize().width - 1, 0, 1, getSize().height);
                            super.paint(g);
                        }
                    };
                }
            });
        }



        lastDividerPosition = frame.getHeight() / 2;

        JSplitPane instance_displays = new JSplitPane();
        instance_displays = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        instance_displays.setOneTouchExpandable(false);
        instance_displays.setContinuousLayout(true);
        instance_displays.setResizeWeight(0.5D);
        instance_displays.setDividerSize(6);
        instance_displays.setUI(new BasicSplitPaneUI() {

            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {

                    @Override
                    public void setBorder(Border b) {

                    }

                    @Override
                    public void paint(Graphics g) {
                        g.setColor(new Color(220, 220, 220));
                        g.fillRect(1, 0, getSize().width - 2, getSize().height);
                        g.setColor(new Color(232, 232, 232));
                        g.fillRect(0, 0, 1, getSize().height);
                        g.setColor(new Color(173, 173, 173));
                        g.fillRect(getSize().width - 1, 0, 1, getSize().height);
                        super.paint(g);
                    }
                };
            }
        });

        lastDividerPosition = frame.getWidth() / 2;
        instance_displays.setDividerLocation(lastDividerPosition);
        instance_displays.setLeftComponent(instanceArea);

        //instance_displays.setRightComponent(hawkeye_selections); // <-- disabled hawkeye selection ui

        JComponent left = null;
        if (settingsOpen == 1) {
            if (myCustomPanel == null)
                myCustomPanel = new VizCustomizationPanel(splitpane, myState);
            else
                myCustomPanel.remakeAll();
            left = myCustomPanel;
        } else if (settingsOpen > 1) {
            if (myEvaluatorPanel == null)
                myEvaluatorPanel = new OurConsole(evaluator, true, "The ", true, "Alloy Evaluator ", false, "allows you to type\nin Alloy expressions and see their values.\nFor example, ", true, "univ", false, " shows the list of all atoms.\n(You can press UP and DOWN to recall old inputs).\n");
            try {
                evaluator.compute(new File(xmlFileName));
            } catch (Exception ex) {
            } // exception should not happen
            left = myEvaluatorPanel;
            left.setBorder(new OurBorder(false, false, false, false));
        }
        if (frame != null && frame.getContentPane() == splitpane)
            lastDividerPosition = splitpane.getDividerLocation();
        splitpane.setRightComponent(instance_displays);
        splitpane.setLeftComponent(left);
        if (left != null) {
            Dimension dim = left.getPreferredSize();
            if (lastDividerPosition < 50 && frame != null)
                lastDividerPosition = frame.getWidth() / 2;
            if (lastDividerPosition < dim.width)
                lastDividerPosition = dim.width;
            if (settingsOpen == 2 && lastDividerPosition > 400)
                lastDividerPosition = 400;
            splitpane.setDividerLocation(lastDividerPosition);
        }
        if (frame != null)
            frame.setContentPane(splitpane);
        if (settingsOpen != 2)
            content.requestFocusInWindow();
        else
            myEvaluatorPanel.requestFocusInWindow();
        repopulateProjectionPopup();
        if (frame != null)
            frame.validate();
        else
            splitpane.validate();
    }

    /**
     * Helper method that creates a button and add it to both the "SolutionButtons"
     * list, as well as the toolbar.
     */
    private JButton makeSolutionButton(String label, String toolTip, String image, ActionListener mode) {
        JButton button = OurUtil.button(label, toolTip, image, mode, background);
        solutionButtons.add(button);
        toolbar.add(button);
        return button;
    }


    private JButton makeSolutionButtonWB(String label, String toolTip, String image, ActionListener mode) {
        JButton button = OurUtil.button(label, toolTip, image, mode, background);
        solutionButtonsWB.add(button);
        wbToolbar.add(button);
        return button;
    }

    /**
     * Helper method that returns a concise description of the instance currently
     * being displayed.
     */
    private String makeVizTitle() {
        String filename = (myState != null ? myState.getOriginalInstance().filename : "");
        String commandname = (myState != null ? myState.getOriginalInstance().commandname : "");
        int i = filename.lastIndexOf('/');
        if (i >= 0)
            filename = filename.substring(i + 1);
        i = filename.lastIndexOf('\\');
        if (i >= 0)
            filename = filename.substring(i + 1);
        int n = filename.length();
        if (n > 4 && filename.substring(n - 4).equalsIgnoreCase(".als"))
            filename = filename.substring(0, n - 4);
        if (filename.length() > 0)
            return "(" + filename + ") " + commandname;
        else
            return commandname;
    }

    /**
     * Helper method that inserts "filename" into the "recently opened THEME file
     * list".
     */
    private void addThemeHistory(String filename) {
        String name0 = Theme0.get(), name1 = Theme1.get(), name2 = Theme2.get();
        if (name0.equals(filename))
            return;
        else {
            Theme0.set(filename);
            Theme1.set(name0);
        }
        if (name1.equals(filename))
            return;
        else
            Theme2.set(name1);
        if (name2.equals(filename))
            return;
        else
            Theme3.set(name2);
    }

    /**
     * Helper method returns a JTextArea containing the given text.
     */
    private JComponent getTextComponent(String text) {
        final JTextArea ta = OurUtil.textarea(text, 10, 10, false, true);
        final JScrollPane ans = new JScrollPane(ta, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED) {

            private static final long serialVersionUID = 0;

            @Override
            public void setFont(Font font) {
                super.setFont(font);
            }
        };
        ans.setBorder(new OurBorder(true, false, true, false));
        String fontName = OurDialog.getProperFontName("Lucida Console", "Monospaced");
        Font font = new Font(fontName, Font.PLAIN, fontSize);
        ans.setFont(font);
        ta.setFont(font);
        return ans;
    }

    // /** Helper method that reads a file and then return a JTextArea
    // containing it. */
    // private JComponent getTextComponentFromFile(String filename) {
    // String text = "";
    // try { text="<!-- "+filename+" -->\n"+Util.readAll(filename); }
    // catch(IOException ex) { text="# Error reading from "+filename; }
    // return getTextComponent(text);
    // }

    /**
     * Returns the GraphViewer that contains the graph; can be null if the graph
     * hasn't been loaded yet.
     */
    public GraphViewer getViewer() {
        if (null == myGraphPanel)
            return null;
        return myGraphPanel.alloyGetViewer();
    }

    /** Load the XML instance. */
    public void loadXML(final String fileName, boolean forcefully) {
        redraw_selection = true;

        final String xmlFileName = Util.canon(fileName);
        File f = new File(xmlFileName);
        if (forcefully || !xmlFileName.equals(this.xmlFileName)) {
            AlloyInstance myInstance;
            try {
                if (!f.exists())
                    throw new IOException("File " + xmlFileName + " does not exist.");
                myInstance = StaticInstanceReader.parseInstance(f);
            } catch (Throwable e) {
                xmlLoaded.remove(fileName);
                xmlLoaded.remove(xmlFileName);
                OurDialog.alert("Cannot read or parse Alloy instance: " + xmlFileName + "\n\nError: " + e.getMessage());
                if (xmlLoaded.size() > 0) {
                    loadXML(xmlLoaded.get(xmlLoaded.size() - 1), false);
                    return;
                }
                doCloseAll();
                return;
            }
            if (myState == null)
                myState = new VizState(myInstance);
            else
                myState.loadInstance(myInstance);
            repopulateProjectionPopup();
            xml2title.put(xmlFileName, makeVizTitle());
            this.xmlFileName = xmlFileName;
        }
        if (!xmlLoaded.contains(xmlFileName))
            xmlLoaded.add(xmlFileName);
        if (myGraphPanel != null)
            myGraphPanel.resetProjectionAtomCombos();
        toolbar.setEnabled(true);
        settingsOpen = 0;
        thememenu.setEnabled(true);
        windowmenu.setEnabled(true);
        if (frame != null) {
            frame.setVisible(true);
            frame.setTitle("Alloy Visualizer " + Version.version() + " loading... Please wait...");
            OurUtil.show(frame);
        }
        updateDisplay();
    }

    public void launchA4Solution(A4Solution sol) {
        redraw_selection = true;

        AlloyInstance myInstance;
        try {
            myInstance = StaticInstanceReader.a4SolutionToAlloyInstanceMaker(sol);
        } catch (Throwable e) {

            doCloseAll();
            return;
        }
        if (myState == null)
            myState = new VizState(myInstance);
        else
            myState.loadInstance(myInstance);
        repopulateProjectionPopup();
        this.xmlFileName = "Hritik did stuff";

        if (myGraphPanel != null)
            myGraphPanel.resetProjectionAtomCombos();
        toolbar.setEnabled(true);
        settingsOpen = 0;
        thememenu.setEnabled(true);
        windowmenu.setEnabled(true);
        if (frame != null) {
            frame.setVisible(true);
            frame.setTitle("Alloy Visualizer " + Version.version() + " loading... Please wait...");
            OurUtil.show(frame);
        }
        updateDisplay();
    }

    /**
     * Launch visualization with a list of AlloyInstances and cluster information.
     * Creates aggregated graph showing: - SOLID lines for nodes/edges in ALL
     * solutions - DASHED lines for nodes/edges in SOME solutions Window title
     * includes cluster number and size.
     */
    public void launchA4SolutionListWithClusterInfo(List<AlloyInstance> instances, int clusterNumber, int clusterSize) {
        if (instances == null || instances.isEmpty()) {
            doCloseAll();
            return;
        }

        redraw_selection = true;

        // Generate aggregated graph data
        StaticGraphMaker.AggregatedGraphData aggregatedData = null;
        try {
            aggregatedData = StaticGraphMaker.produceAggregatedGraphData(instances, myState != null ? myState : new VizState(instances.get(0)), null);
        } catch (Throwable e) {
            OurDialog.alert("Error aggregating solutions: " + e.getMessage());
            doCloseAll();
            return;
        }

        // Store aggregated data and instance list for Next cycling
        this.currentAggregatedData = aggregatedData;
        this.currentInstanceList = new ArrayList<AlloyInstance>(instances);
        this.showSingleInstanceIndex = -1;

        // Load first instance for VizState initialization
        AlloyInstance firstInstance = instances.get(0);

        if (myState == null)
            myState = new VizState(firstInstance);
        else
            myState.loadInstance(firstInstance);
        repopulateProjectionPopup();
        this.xmlFileName = "Cluster " + clusterNumber + " (" + clusterSize + " solutions)";

        if (myGraphPanel != null)
            myGraphPanel.resetProjectionAtomCombos();
        toolbar.setEnabled(true);
        settingsOpen = 0;
        thememenu.setEnabled(true);
        windowmenu.setEnabled(true);
        if (frame != null) {
            frame.setVisible(true);
            frame.setTitle("Alloy Visualizer " + Version.version() + " - Cluster " + clusterNumber + " (" + clusterSize + " solutions)");
            OurUtil.show(frame);
        }

        // Log aggregation statistics
        System.out.println("=== Cluster " + clusterNumber + " Statistics ===");
        System.out.println("Total solutions: " + aggregatedData.totalSolutions);
        System.out.println("Total unique nodes: " + aggregatedData.nodeFrequencies.size());
        System.out.println("Total unique edges: " + aggregatedData.edgeFrequencies.size());

        // Count common vs partial elements
        int commonNodes = 0, partialNodes = 0;
        for (AlloyAtom atom : aggregatedData.getAllNodes()) {
            if (aggregatedData.isCommonNode(atom))
                commonNodes++;
            else
                partialNodes++;
        }
        int commonEdges = 0, partialEdges = 0;
        for (AlloyTuple tuple : aggregatedData.getAllEdges()) {
            if (aggregatedData.isCommonEdge(tuple))
                commonEdges++;
            else
                partialEdges++;
        }
        System.out.println("Common nodes (solid, in all solutions): " + commonNodes);
        System.out.println("Partial nodes (dashed, in some solutions): " + partialNodes);
        System.out.println("Common edges (solid, in all solutions): " + commonEdges);
        System.out.println("Partial edges (dashed, in some solutions): " + partialEdges);
        System.out.println("==============================");

        updateDisplay();
    }

    /**
     * Launch visualization with a list of AlloyInstances. Creates aggregated graph
     * showing: - SOLID lines for nodes/edges in ALL solutions - DASHED lines for
     * nodes/edges in SOME solutions
     */
    public void launchA4SolutionList(List<AlloyInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            doCloseAll();
            return;
        }

        redraw_selection = true;

        // Generate aggregated graph data
        StaticGraphMaker.AggregatedGraphData aggregatedData = null;
        try {
            aggregatedData = StaticGraphMaker.produceAggregatedGraphData(instances, myState != null ? myState : new VizState(instances.get(0)), null);
        } catch (Throwable e) {
            OurDialog.alert("Error aggregating solutions: " + e.getMessage());
            doCloseAll();
            return;
        }

        // Store aggregated data and instance list for Next cycling
        this.currentAggregatedData = aggregatedData;
        this.currentInstanceList = new ArrayList<AlloyInstance>(instances);
        this.showSingleInstanceIndex = -1;

        // Load first instance for VizState initialization
        AlloyInstance firstInstance = instances.get(0);

        if (myState == null)
            myState = new VizState(firstInstance);
        else
            myState.loadInstance(firstInstance);
        repopulateProjectionPopup();
        this.xmlFileName = "Aggregated: " + instances.size() + " solutions";

        if (myGraphPanel != null)
            myGraphPanel.resetProjectionAtomCombos();
        toolbar.setEnabled(true);
        settingsOpen = 0;
        thememenu.setEnabled(true);
        windowmenu.setEnabled(true);
        if (frame != null) {
            frame.setVisible(true);
            frame.setTitle("Alloy Visualizer " + Version.version() + " - Aggregated: " + instances.size() + " solutions");
            OurUtil.show(frame);
        }

        // Log aggregation statistics
        System.out.println("=== Aggregation Statistics ===");
        System.out.println("Total solutions: " + aggregatedData.totalSolutions);
        System.out.println("Total unique nodes: " + aggregatedData.nodeFrequencies.size());
        System.out.println("Total unique edges: " + aggregatedData.edgeFrequencies.size());

        // Count common vs partial elements
        int commonNodes = 0, partialNodes = 0;
        for (AlloyAtom atom : aggregatedData.getAllNodes()) {
            if (aggregatedData.isCommonNode(atom))
                commonNodes++;
            else
                partialNodes++;
        }

        int commonEdges = 0, partialEdges = 0;
        for (AlloyTuple tuple : aggregatedData.getAllEdges()) {
            if (aggregatedData.isCommonEdge(tuple))
                commonEdges++;
            else
                partialEdges++;
        }

        System.out.println("Common nodes (solid, in all solutions): " + commonNodes);
        System.out.println("Partial nodes (dashed, in some solutions): " + partialNodes);
        System.out.println("Common edges (solid, in all solutions): " + commonEdges);
        System.out.println("Partial edges (dashed, in some solutions): " + partialEdges);
        System.out.println("==============================");

        updateDisplay();
    }

    /** Stored aggregated data for rendering with frequency-based styling */
    private StaticGraphMaker.AggregatedGraphData currentAggregatedData   = null;

    /**
     * When non-null, this window has an in-memory list of instances (e.g. from
     * clustering).
     */
    private List<AlloyInstance>                  currentInstanceList     = null;

    /**
     * -1 = show aggregated view; >= 0 = show single instance at this index from
     * currentInstanceList.
     */
    private int                                  showSingleInstanceIndex = -1;

    /**
     * Check if we're in aggregated mode (aggregated data present and not cycling
     * single instance).
     */
    private boolean isAggregatedMode() {
        return currentAggregatedData != null && showSingleInstanceIndex < 0;
    }

    /**
     * Cycle to the next instance in the current in-memory list (used by cluster
     * windows). If currently showing aggregated, show first single instance; then
     * 2nd, 3rd, ... then wrap back to aggregated.
     */
    public void showNextInList() {
        if (currentInstanceList == null || currentInstanceList.isEmpty())
            return;
        showSingleInstanceIndex++;
        if (showSingleInstanceIndex >= currentInstanceList.size())
            showSingleInstanceIndex = -1;
        if (showSingleInstanceIndex >= 0)
            myState.loadInstance(currentInstanceList.get(showSingleInstanceIndex));
        updateDisplay();
    }

    /** This method loads a specific theme file. */
    public boolean loadThemeFile(String filename) {
        if (myState == null)
            return false; // Can only load if there is a VizState loaded
        filename = Util.canon(filename);
        try {
            myState.loadPaletteXML(filename);
        } catch (IOException ex) {
            OurDialog.alert("Error: " + ex.getMessage());
            return false;
        }
        repopulateProjectionPopup();
        if (myCustomPanel != null)
            myCustomPanel.remakeAll();
        if (myGraphPanel != null)
            myGraphPanel.remakeAll();
        addThemeHistory(filename);
        thmFileName = filename;
        updateDisplay();
        return true;
    }

    /**
     * This method saves a specific current theme (if filename==null, it asks the
     * user); returns true if it succeeded.
     */
    public boolean saveThemeFile(String filename) {
        if (myState == null)
            return false; // Can only save if there is a VizState loaded
        if (filename == null) {
            File file = OurDialog.askFile(false, null, ".thm", ".thm theme files");
            if (file == null)
                return false;
            if (file.exists())
                if (!OurDialog.askOverwrite(Util.canon(file.getPath())))
                    return false;
            Util.setCurrentDirectory(file.getParentFile());
            filename = file.getPath();
        }
        filename = Util.canon(filename);
        try {
            myState.savePaletteXML(filename);
            filename = Util.canon(filename); // Since the canon name may have
                                            // changed
            addThemeHistory(filename);
        } catch (Throwable er) {
            OurDialog.alert("Error saving the theme.\n\nError: " + er.getMessage());
            return false;
        }
        thmFileName = filename;
        return true;
    }

    // ========================================= EVENTS
    // ============================================================================================

    /**
     * This method changes the font size for everything (except the graph)
     */
    public void doSetFontSize(int fontSize) {
        this.fontSize = fontSize;
        if (!(content instanceof VizGraphPanel))
            updateDisplay();
        else
            content.setFont(OurUtil.getVizFont().deriveFont((float) fontSize));
    }

    /**
     * This method asks the user for a new XML instance file to load.
     */
    private Runner doLoad() {
        if (wrap)
            return wrapMe();
        File file = OurDialog.askFile(true, null, ".xml", ".xml instance files");
        if (file == null)
            return null;
        Util.setCurrentDirectory(file.getParentFile());
        loadXML(file.getPath(), true);
        return null;
    }

    /**
     * This method loads a new XML instance file if it's not the current file.
     */
    private Runner doLoadInstance(String fileName) {
        if (!wrap)
            loadXML(fileName, false);
        return wrapMe(fileName);
    }

    /**
     * This method closes the current instance; if there are previously loaded
     * files, we will load one of them; otherwise, this window will set itself as
     * invisible (if not in standalone mode), or it will terminate the entire
     * application (if in standalone mode).
     */
    private Runner doClose() {
        if (wrap)
            return wrapMe();
        xmlLoaded.remove(xmlFileName);
        if (xmlLoaded.size() > 0) {
            doLoadInstance(xmlLoaded.get(xmlLoaded.size() - 1));
            return null;
        }
        if (standalone)
            System.exit(0);
        else if (frame != null)
            frame.setVisible(false);
        return null;
    }

    /**
     * This method closes every XML file. If in standalone mode, the JVM will then
     * shutdown, otherwise it will just set the window invisible.
     */
    private Runner doCloseAll() {
        if (wrap)
            return wrapMe();
        xmlLoaded.clear();
        xmlFileName = "";
        if (standalone)
            System.exit(0);
        else if (frame != null)
            frame.setVisible(false);
        return null;
    }

    /** This method refreshes the "theme" menu. */
    private Runner doRefreshTheme() {
        if (wrap)
            return wrapMe();
        String defaultTheme = System.getProperty("alloy.theme0");
        thememenu.removeAll();
        try {
            wrap = true;
            menuItem(thememenu, "Load Theme...", 'L', doLoadTheme());
            if (defaultTheme != null && defaultTheme.length() > 0 && (new File(defaultTheme)).isDirectory())
                menuItem(thememenu, "Load Sample Theme...", 'B', doLoadSampleTheme());
            menuItem(thememenu, "Save Theme", 'S', doSaveTheme());
            menuItem(thememenu, "Save Theme As...", 'A', doSaveThemeAs());
            menuItem(thememenu, "Reset Theme", 'R', doResetTheme());
        } finally {
            wrap = false;
        }
        return null;
    }

    /**
     * This method asks the user for a new theme file to load.
     */
    private Runner doLoadTheme() {
        if (wrap)
            return wrapMe();
        String defaultTheme = System.getProperty("alloy.theme0");
        if (defaultTheme == null)
            defaultTheme = "";
        if (myState == null)
            return null; // Can only load if there is a VizState loaded
        if (myState.changedSinceLastSave()) {
            char opt = OurDialog.askSaveDiscardCancel("The current theme");
            if (opt == 'c')
                return null;
            if (opt == 's' && !saveThemeFile(thmFileName.length() == 0 ? null : thmFileName))
                return null;
        }
        File file = OurDialog.askFile(true, null, ".thm", ".thm theme files");
        if (file != null) {
            Util.setCurrentDirectory(file.getParentFile());
            loadThemeFile(file.getPath());
        }
        return null;
    }

    /**
     * This method asks the user for a new theme file (from the default Alloy4
     * distribution) to load.
     */
    private Runner doLoadSampleTheme() {
        if (wrap)
            return wrapMe();
        String defaultTheme = System.getProperty("alloy.theme0");
        if (defaultTheme == null)
            defaultTheme = "";
        if (myState == null)
            return null; // Can only load if there is a VizState loaded
        if (myState.changedSinceLastSave()) {
            char opt = OurDialog.askSaveDiscardCancel("The current theme");
            if (opt == 'c')
                return null;
            if (opt == 's' && !saveThemeFile(thmFileName.length() == 0 ? null : thmFileName))
                return null;
        }
        File file = OurDialog.askFile(true, defaultTheme, ".thm", ".thm theme files");
        if (file != null)
            loadThemeFile(file.getPath());
        return null;
    }

    /** This method saves the current theme. */
    private Runner doSaveTheme() {
        if (!wrap)
            saveThemeFile(thmFileName.length() == 0 ? null : thmFileName);
        return wrapMe();
    }

    /**
     * This method saves the current theme to a new ".thm" file.
     */
    private Runner doSaveThemeAs() {
        if (wrap)
            return wrapMe();
        File file = OurDialog.askFile(false, null, ".thm", ".thm theme files");
        if (file == null)
            return null;
        if (file.exists())
            if (!OurDialog.askOverwrite(Util.canon(file.getPath())))
                return null;
        Util.setCurrentDirectory(file.getParentFile());
        saveThemeFile(file.getPath());
        return null;
    }

    private Runner doExportDot() {
        if (wrap)
            return wrapMe();
        File file = OurDialog.askFile(false, null, ".dot", ".dot graph files");
        if (file == null)
            return null;
        if (file.exists())
            if (!OurDialog.askOverwrite(Util.canon(file.getPath())))
                return null;
        Util.setCurrentDirectory(file.getParentFile());
        String filename = Util.canon(file.getPath());
        try {
            Util.writeAll(filename, myGraphPanel.toDot());
        } catch (Throwable er) {
            OurDialog.alert("Error saving the theme.\n\nError: " + er.getMessage());
        }
        return null;
    }

    private Runner doExportXml() {
        if (wrap)
            return wrapMe();
        File file = OurDialog.askFile(false, null, ".xml", ".xml XML files");
        if (file == null)
            return null;
        if (file.exists())
            if (!OurDialog.askOverwrite(Util.canon(file.getPath())))
                return null;
        Util.setCurrentDirectory(file.getParentFile());
        String filename = Util.canon(file.getPath());
        try {
            Util.writeAll(filename, Util.readAll(xmlFileName));
        } catch (Throwable er) {
            OurDialog.alert("Error saving XML instance.\n\nError: " + er.getMessage());
        }
        return null;
    }

    /** This method resets the current theme. */
    private Runner doResetTheme() {
        if (wrap)
            return wrapMe();
        if (myState == null)
            return null;
        if (!OurDialog.yesno("Are you sure you wish to clear all your customizations?", "Yes, clear them", "No, keep them"))
            return null;
        myState.resetTheme();
        repopulateProjectionPopup();
        if (myCustomPanel != null)
            myCustomPanel.remakeAll();
        if (myGraphPanel != null)
            myGraphPanel.remakeAll();
        thmFileName = "";
        updateDisplay();
        return null;
    }

    /**
     * This method modifies the theme using a set of heuristics.
     */
    private Runner doMagicLayout() {
        if (wrap)
            return wrapMe();
        if (myState == null)
            return null;
        if (!OurDialog.yesno("This will clear your original customizations. Are you sure?", "Yes, clear them", "No, keep them"))
            return null;
        myState.resetTheme();
        try {
            MagicLayout.magic(myState);
            MagicColor.magic(myState);
        } catch (Throwable ex) {
        }
        repopulateProjectionPopup();
        if (myCustomPanel != null)
            myCustomPanel.remakeAll();
        if (myGraphPanel != null)
            myGraphPanel.remakeAll();
        updateDisplay();
        return null;
    }

    /** This method refreshes the "window" menu. */
    private Runner doRefreshWindow() {
        if (wrap)
            return wrapMe();
        windowmenu.removeAll();
        try {
            wrap = true;
            for (final String f : getInstances()) {
                JMenuItem it = new JMenuItem("Instance: " + getInstanceTitle(f), null);
                it.setIcon(f.equals(getXMLfilename()) ? iconYes : iconNo);
                it.addActionListener(doLoadInstance(f));
                windowmenu.add(it);
            }
        } finally {
            wrap = false;
        }
        return null;
    }

    /**
     * This method inserts "Minimize" and "Maximize" entries into a JMenu.
     */
    public void addMinMaxActions(JMenu menu) {
        try {
            wrap = true;
            menuItem(menu, "Minimize", 'M', doMinimize(), iconNo);
            menuItem(menu, "Zoom", doZoom(), iconNo);
        } finally {
            wrap = false;
        }
    }

    /** This method minimizes the window. */
    private Runner doMinimize() {
        if (!wrap && frame != null)
            OurUtil.minimize(frame);
        return wrapMe();
    }

    /**
     * This method alternatingly maximizes or restores the window.
     */
    private Runner doZoom() {
        if (!wrap && frame != null)
            OurUtil.zoom(frame);
        return wrapMe();
    }

    /**
     * This method attempts to derive the next satisfying instance.
     */
    private Runner doNext() {
        redraw_selection = true;
        if (wrap)
            return wrapMe();
        if (settingsOpen != 0)
            return null;
        if (xmlFileName.length() == 0) {
            OurDialog.alert("Cannot display the next solution since no instance is currently loaded.");
        } else if (enumerator == null) {
            OurDialog.alert("Cannot display the next solution since the analysis engine is not loaded with the visualizer.");
        } else {
            try {
                ArrayList<Integer> same = new ArrayList<Integer>();
                ArrayList<Integer> diff = new ArrayList<Integer>();
                ArrayList<String> same_hl = new ArrayList<String>();
                ArrayList<String> diff_hl = new ArrayList<String>();

                if (user_selections == null)
                    user_selections = new ArrayList<OptionSelections>();
                for (int i = 0; i < user_selections.size(); i++) {
                    String choice = user_selections.get(i).high_level_btns.getSelection().getActionCommand();
                    if (choice.equals("same")) {
                        same_hl.add(user_selections.get(i).name);
                    } else if (choice.equals("diff")) {
                        diff_hl.add(user_selections.get(i).name);
                    } else if (choice.equals("ind")) {
                        for (int j = 0; j < user_selections.get(i).atom_level.size(); j++) {
                            choice = user_selections.get(i).atom_level.get(j).getSelection().getActionCommand();
                            if (choice.equals("same")) {
                                same.add(user_selections.get(i).atom_indx.get(j));
                            } else if (choice.equals("diff")) {
                                diff.add(user_selections.get(i).atom_indx.get(j));
                            }
                        }
                    }
                }

                enumerator.setSameAtoms(same);
                enumerator.setDiffAtoms(diff);
                enumerator.setSameHighlevel(same_hl);
                enumerator.setDiffHighlevel(diff_hl);
                enumerator.compute(xmlFileName);
            } catch (Throwable ex) {
                OurDialog.alert(ex.getMessage());
            }
        }
        return null;
    }

    /**
     * This method updates the graph with the current theme customization.
     */
    private Runner doApply() {
        if (!wrap)
            updateDisplay();
        return wrapMe();
    }

    /**
     * This method opens the theme customization panel if closed.
     */
    private Runner doOpenThemePanel() {
        if (!wrap) {
            settingsOpen = 1;
            updateDisplay();
        }
        return wrapMe();
    }

    /**
     * This method closes the theme customization panel if open.
     */
    private Runner doCloseThemePanel() {
        if (!wrap) {
            settingsOpen = 0;
            updateDisplay();
        }
        return wrapMe();
    }

    /** This method opens the evaluator panel if closed. */
    private Runner doOpenEvalPanel() {
        if (!wrap) {
            settingsOpen = 2;
            updateDisplay();
        }
        return wrapMe();
    }

    /** This method closes the evaluator panel if open. */
    private Runner doCloseEvalPanel() {
        if (!wrap) {
            settingsOpen = 0;
            updateDisplay();
        }
        return wrapMe();
    }

    /**
     * This method changes the display mode to show the instance as a graph (the
     * return value is always null).
     */
    public Runner doShowViz() {
        if (!wrap) {
            currentMode = VisualizerMode.Viz;
            updateDisplay();
            return null;
        }
        return wrapMe();
    }

    /**
     * This method changes the display mode to show the instance as a tree (the
     * return value is always null).
     */
    public Runner doShowTree() {
        if (!wrap) {
            currentMode = VisualizerMode.Tree;
            updateDisplay();
            return null;
        }
        return wrapMe();
    }

    /**
     * This method changes the display mode to show the equivalent dot text (the
     * return value is always null).
     */
    public Runner doShowTxt() {
        if (!wrap) {
            currentMode = VisualizerMode.TEXT;
            updateDisplay();
            return null;
        }
        return wrapMe();
    }

    /**
     * This method changes the display mode to show the equivalent dot text (the
     * return value is always null).
     */
    public Runner doShowTable() {
        if (!wrap) {
            currentMode = VisualizerMode.TABLE;
            updateDisplay();
            return null;
        }
        return wrapMe();
    }

    public Runner doShowText() {
        if (!wrap) {
            wbMode = "text";
            updateDisplay();
            return null;
        }
        return wrapMe();
    }

    public Runner doShowGraph() {
        if (!wrap) {
            wbMode = "graph";
            updateDisplay();
            return null;
        }
        return wrapMe();
    }


    // /** This method changes the display mode to show the equivalent dot text
    // (the return value is always null). */
    // public Runner doShowDot() {
    // if (!wrap) { currentMode=VisualizerMode.DOT; updateDisplay(); return
    // null; }
    // return wrapMe();
    // }
    //
    // /** This method changes the display mode to show the instance as XML (the
    // return value is always null). */
    // public Runner doShowXML() {
    // if (!wrap) { currentMode=VisualizerMode.XML; updateDisplay(); return
    // null; }
    // return wrapMe();
    // }

}
