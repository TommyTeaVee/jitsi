/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.keybindingchooser;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import chooser.*;

import net.java.sip.communicator.service.keybindings.*;
import net.java.sip.communicator.swing.*;

/**
 * The <tt>ConfigurationForm</tt> that would be added to the settings
 * configuration to configure the application keybindings.
 * 
 * @author Damian Johnson
 * @author Lubomir Marinov
 */
public class KeybindingsConfigPanel
    extends TransparentPanel
{
    private static final long serialVersionUID = 0;

    private final HashMap<KeybindingSet, SIPChooser> choosers =
        new HashMap<KeybindingSet, SIPChooser>();

    public KeybindingsConfigPanel(KeybindingsService service)
    {
        super(new BorderLayout());

        setFocusable(true);
        JTabbedPane chooserPanes = new JTabbedPane(JTabbedPane.LEFT);

        // deselects entries awaiting input when focus is lost
        this.addFocusListener(new FocusAdapter()
        {
            public void focusLost(FocusEvent event)
            {
                for (SIPChooser chooser : choosers.values())
                {
                    chooser.setSelected(null);
                }
            }
        });

        for (KeybindingSet.Category category : KeybindingSet.Category.values())
        {
            KeybindingSet bindingSet = service.getBindings(category);
            if (bindingSet == null)
                continue; // defaults failed to load

            SIPChooser newChooser = new SIPChooser();
            newChooser.putAllBindings(bindingSet.getBindings());

            JPanel chooserWrapper = new TransparentPanel(new BorderLayout());
            chooserWrapper.add(newChooser, BorderLayout.NORTH);
            JScrollPane scroller = new JScrollPane(chooserWrapper);

            // adds listener that receives events to set bindings
            this.addKeyListener(newChooser.makeAdaptor());

            chooserPanes.addTab(getReadableConstant(category.toString()),
                scroller);
            this.choosers.put(bindingSet, newChooser);
        }

        add(chooserPanes);

        JButton apply = new JButton("Apply");
        apply.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                for (KeybindingSet set : choosers.keySet())
                    set.setBindings(choosers.get(set).getBindingMap());
            }
        });

        JPanel bottomWrapper =
            new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomWrapper.add(apply);
        add(bottomWrapper, BorderLayout.SOUTH);
    }

    /**
     * Provides a more readable version of constant names. Spaces replace
     * underscores and this changes the input to lowercase except the first
     * letter of each word. For instance, "RARE_CARDS" would become "Rare
     * Cards".
     * 
     * @param input string to be converted
     * @return reader friendly variant of constant name
     */
    private static String getReadableConstant(String input)
    {
        char[] name = input.toCharArray();

        boolean isStartOfWord = true;
        for (int i = 0; i < name.length; ++i)
        {
            char chr = name[i];
            if (chr == '_')
                name[i] = ' ';
            else if (isStartOfWord)
                name[i] = Character.toUpperCase(chr);
            else
                name[i] = Character.toLowerCase(chr);
            isStartOfWord = chr == '_';
        }

        return new String(name);
    }

    /**
     * Keybinding chooser with customized appearance and functionality for the
     * SIP Communicator.
     */
    private class SIPChooser
        extends BindingChooser
    {
        private static final long serialVersionUID = 0;

        // Provides mapping of UI labels to internal action names
        private HashMap<String, String> actionLabels =
            new HashMap<String, String>();

        // Calls focus to the form so keyboard events are received
        protected void onClick(MouseEvent event, BindingEntry entry,
            BindingEntry.Field field)
        {
            super.onClick(event, entry, field);
            KeybindingsConfigPanel.this.requestFocus();
        }

        public boolean putBinding(BindingEntry newEntry, int index)
        {
            // Converts to I18N strings for UI
            String actionInternal = newEntry.getAction();
            String actionLabel = KeybindingChooserActivator.getResources()
                .getI18NString(actionInternal);
            this.actionLabels.put(actionLabel, actionInternal);
            newEntry.setAction(actionLabel);

            // Overwrites the default entry layout to stretch shortcut field
            newEntry.removeAll();
            newEntry.setLayout(new BorderLayout());

            JPanel left =
                new TransparentPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            left.add(newEntry.getField(BindingEntry.Field.INDENT));
            left.add(newEntry.getField(BindingEntry.Field.ACTION));
            newEntry.add(left, BorderLayout.WEST);
            newEntry.add(newEntry.getField(BindingEntry.Field.SHORTCUT));

            return super.putBinding(newEntry, index);
        }

        public LinkedHashMap<KeyStroke, String> getBindingMap()
        {
            // Translates I18N strings back to internal action labels
            LinkedHashMap<KeyStroke, String> bindings =
                new LinkedHashMap<KeyStroke, String>();
            for (BindingEntry entry : super.getBindings())
            {
                bindings.put(entry.getShortcut(), this.actionLabels.get(entry
                    .getAction()));
            }

            return bindings;
        }
    }
}
