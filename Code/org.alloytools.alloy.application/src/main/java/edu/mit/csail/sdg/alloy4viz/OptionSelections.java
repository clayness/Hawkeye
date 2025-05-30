package edu.mit.csail.sdg.alloy4viz;

import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JRadioButton;

import edu.mit.csail.sdg.alloy4.OurUtil;

public class OptionSelections {

    String                 name;
    ButtonGroup            high_level_btns;
    JRadioButton           atom_or_high_level;
    ArrayList<ButtonGroup> atom_level;
    ArrayList<String>      atoms;
    ArrayList<Integer>     atom_indx;

    public OptionSelections(String name) {
        this.name = name;
        atom_level = new ArrayList<ButtonGroup>();
        atoms = new ArrayList<String>();
        atom_indx = new ArrayList<Integer>();

        //Make options for sig or relation at large
        JRadioButton option1 = new JRadioButton("Keep");
        option1.setBackground(Color.white);
        option1.setActionCommand("same");
        option1.setIcon(new ImageIcon(OurUtil.class.getClassLoader().getResource("images/radio2.png")));
        option1.setSelectedIcon(new ImageIcon(OurUtil.class.getClassLoader().getResource("images/radioselected2.png")));

        JRadioButton option2 = new JRadioButton("Change");
        option2.setBackground(Color.white);
        option2.setActionCommand("diff");
        option2.setIcon(new ImageIcon(OurUtil.class.getClassLoader().getResource("images/radio2.png")));
        option2.setSelectedIcon(new ImageIcon(OurUtil.class.getClassLoader().getResource("images/radioselected2.png")));

        JRadioButton option3 = new JRadioButton("Default");
        option3.setSelected(true);
        option3.setBackground(Color.white);
        option3.setActionCommand("default");
        option3.setIcon(new ImageIcon(OurUtil.class.getClassLoader().getResource("images/radio2.png")));
        option3.setSelectedIcon(new ImageIcon(OurUtil.class.getClassLoader().getResource("images/radioselected2.png")));

        JRadioButton option4 = new JRadioButton("Select Individual Atoms");
        option4.setSelected(true);
        option4.setBackground(Color.white);
        option4.setActionCommand("ind");
        option4.setIcon(new ImageIcon(OurUtil.class.getClassLoader().getResource("images/radio2.png")));
        option4.setSelectedIcon(new ImageIcon(OurUtil.class.getClassLoader().getResource("images/radioselected2.png")));

        atom_or_high_level = option4;

        JRadioButton option5 = new JRadioButton("Establish Connection");
        option5.setBackground(Color.white);
        option5.setActionCommand("make1");
        option5.setIcon(new ImageIcon(OurUtil.class.getClassLoader().getResource("images/radio2.png")));
        option5.setSelectedIcon(new ImageIcon(OurUtil.class.getClassLoader().getResource("images/radioselected2.png")));

        JRadioButton option6 = new JRadioButton("Remove Connection");
        option6.setBackground(Color.white);
        option6.setActionCommand("make0");
        option6.setIcon(new ImageIcon(OurUtil.class.getClassLoader().getResource("images/radio2.png")));
        option6.setSelectedIcon(new ImageIcon(OurUtil.class.getClassLoader().getResource("images/radioselected2.png")));

        ButtonGroup group = new ButtonGroup();
        group.add(option1);
        group.add(option2);
        group.add(option3);
        group.add(atom_or_high_level);
        group.add(option5);
        group.add(option6);

        this.high_level_btns = group;

    }

    public void addAction() {
        atom_or_high_level.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                for (ButtonGroup atoms : atom_level) {
                    List<AbstractButton> btns = Collections.list(atoms.getElements());

                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        for (AbstractButton b : btns) {
                            b.setEnabled(true);
                        }
                    } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                        for (AbstractButton b : btns) {
                            b.setEnabled(false);
                        }
                    }
                }
            }
        });
    }

    public void addAtom(String name, Integer index) {
        atoms.add(name);
        atom_indx.add(index);

        JRadioButton option1 = new JRadioButton("Keep");
        option1.setBackground(Color.white);
        option1.setActionCommand("same");
        //  option1.setIcon(new ImageIcon(OurUtil.class.getClassLoader().getResource("images/radio2.png")));
        //  option1.setSelectedIcon(new ImageIcon(OurUtil.class.getClassLoader().getResource("images/radioselected2.png")));
        option1.setEnabled(false);

        JRadioButton option2 = new JRadioButton("Change");
        option2.setBackground(Color.white);
        option2.setActionCommand("diff");
        //option2.setIcon(new ImageIcon(OurUtil.class.getClassLoader().getResource("images/radio2.png")));
        //option2.setSelectedIcon(new ImageIcon(OurUtil.class.getClassLoader().getResource("images/radioselected2.png")));
        option2.setEnabled(false);

        JRadioButton option3 = new JRadioButton("Default");
        option3.setSelected(true);
        option3.setBackground(Color.white);
        option3.setActionCommand("default");
        // option3.setIcon(new ImageIcon(OurUtil.class.getClassLoader().getResource("images/radio2.png")));
        // option3.setSelectedIcon(new ImageIcon(OurUtil.class.getClassLoader().getResource("images/radioselected2.png")));
        option3.setEnabled(false);

        ButtonGroup group = new ButtonGroup();
        group.add(option1);
        group.add(option2);
        group.add(option3);
        atom_level.add(group);
    }
}
