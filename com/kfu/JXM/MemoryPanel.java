/*
 
 JXM - XMPCR control program for Java
 Copyright (C) 2003-2004 Nicholas W. Sayer
 
 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.
 
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 $Id: MemoryPanel.java,v 1.6 2004/03/30 17:18:21 nsayer Exp $
 
 */

package com.kfu.JXM;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.prefs.*;

import com.kfu.xm.*;

public class MemoryPanel extends JDialog {

    private MainWindow parent;
    private JList memoryList;
    private DefaultListModel memoryListModel;
    private JButton deleteButton;
    private JTextArea notesField;
    private boolean ignoreNotesChange = false;

    private static final int memoryCellHeight = 80;
    private static final int memoryCellWidth = 300;

    private static DateFormat myFormatter = DateFormat.getDateTimeInstance();

    public MemoryPanel(MainWindow parent) {
	super(parent.getFrame(), "JXM - Memorized Info", false);
	this.parent = parent;

        this.getContentPane().setLayout(new GridBagLayout());
	GridBagConstraints gbc = new GridBagConstraints();

        this.memoryList = new JList();
	this.memoryListModel = new DefaultListModel();
	this.memoryList.setModel(this.memoryListModel);
        this.memoryList.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { this.maybePopup(e); }
            public void mouseReleased(MouseEvent e) { this.maybePopup(e); }
            private void maybePopup(MouseEvent e) {
                if (!RadioCommander.theRadio().isOn())
                    return;
                if (!e.isPopupTrigger())
                    return;
                int row = MemoryPanel.this.memoryList.locationToIndex(e.getPoint());
                if (row < 0)
                    return;
                MemoryListItem item = (MemoryListItem)MemoryPanel.this.memoryListModel.getElementAt(row);
                JPopupMenu jpm = MemoryPanel.this.parent.new ChannelPopupMenu(item.getChannelInfo(), MainWindow.CHAN_POPUP_NO_MEM);
                jpm.show(e.getComponent(), e.getX(), e.getY());
            }
        });
	class MemoryCellRenderer extends JPanel implements ListCellRenderer {
	    JLabel dateField;
	    MainWindow.ChannelInfoPanel channelInfoPanel;

	    public MemoryCellRenderer() {
		this.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		this.channelInfoPanel = new MainWindow.ChannelInfoPanel();
		this.channelInfoPanel.setOpaque(false);
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(10, 10, 0, 10);
		gbc.fill = GridBagConstraints.BOTH;
		this.add(this.channelInfoPanel, gbc);

		this.dateField = new JLabel();
		this.dateField.setFont(new Font(null, Font.PLAIN, 9));
		this.dateField.setOpaque(false);
		this.dateField.setHorizontalAlignment(SwingConstants.CENTER);
		this.dateField.setVerticalAlignment(SwingConstants.CENTER);
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.insets = new Insets(0, 10, 10, 10);
		this.add(this.dateField, gbc);
	    }
	    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		MemoryListItem item = (MemoryListItem)value;
		Date when = item.getDate();
		ChannelInfo info = item.getChannelInfo();
		if (isSelected) {
		    this.setForeground(list.getSelectionForeground());
		    this.setBackground(list.getSelectionBackground());
		} else {
		    this.setForeground(null);
		    this.setBackground(null);
		}
		this.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, MainWindow.gridColor));
		this.dateField.setText(MemoryPanel.myFormatter.format(when));
		this.channelInfoPanel.setChannelInfo(info);
		return this;
	    }
	}
	this.memoryList.setCellRenderer(new MemoryCellRenderer());
	this.memoryList.addListSelectionListener(new ListSelectionListener() {
	    public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting())
		    return;
		int[] selections = MemoryPanel.this.memoryList.getSelectedIndices();
		MemoryPanel.this.deleteButton.setEnabled(selections.length >= 0);
		MemoryPanel.this.ignoreNotesChange = true;
		try {
		if (selections.length == 1) {
		    MemoryPanel.this.notesField.setEnabled(true);
		    MemoryListItem mli = (MemoryListItem)MemoryPanel.this.memoryList.getSelectedValue();
		    MemoryPanel.this.notesField.setText(mli.getNotes());
		} else {
		    MemoryPanel.this.notesField.setEnabled(false);
		    MemoryPanel.this.notesField.setText("");
		}
		}
		finally {
		    MemoryPanel.this.ignoreNotesChange = false;
		}
	    }
	});
	this.memoryList.setFixedCellHeight(memoryCellHeight);
	this.memoryList.setFixedCellWidth(memoryCellWidth);
	this.memoryList.setVisibleRowCount(3);
	//this.memoryList.setMinimumSize(this.memoryList.getPreferredScrollableViewportSize());
	//this.memoryList.setPreferredSize(this.memoryList.getPreferredScrollableViewportSize());
	JScrollPane jsp = new JScrollPane(this.memoryList);
	//jsp.setMinimumSize(this.memoryList.getPreferredScrollableViewportSize());
	//jsp.setPreferredSize(this.memoryList.getPreferredScrollableViewportSize());
	gbc.weightx = 1;
	gbc.weighty = 1;
	gbc.fill = GridBagConstraints.BOTH;
	gbc.insets = new Insets(10, 10, 5, 10);
	this.getContentPane().add(jsp, gbc);
	this.notesField = new JTextArea();
	this.notesField.setRows(5);
	this.notesField.getDocument().addDocumentListener(new DocumentListener() {
	    public void changedUpdate(DocumentEvent e) { this.doIt(); }
            public void insertUpdate(DocumentEvent e) { this.doIt(); }
            public void removeUpdate(DocumentEvent e) { this.doIt(); }
            private void doIt() {
		if (MemoryPanel.this.ignoreNotesChange)
		    return;
		MemoryListItem mli = (MemoryListItem)MemoryPanel.this.memoryList.getSelectedValue();
		mli.setNotes(MemoryPanel.this.notesField.getText());
	    }
	});
	jsp = new JScrollPane(this.notesField);
	jsp.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Notes"));
	this.notesField.setEnabled(false);
	gbc.gridy = 1;
	this.getContentPane().add(jsp, gbc);
	this.deleteButton = new JButton("Delete");
	this.deleteButton.setEnabled(false);
	this.deleteButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		Object[] selectedItems = MemoryPanel.this.memoryList.getSelectedValues();
		for(int i = 0; i < selectedItems.length; i++)
		    MemoryPanel.this.memoryListModel.removeElement(selectedItems[i]);
		MemoryPanel.this.deleteButton.setEnabled(false);
	    }
	});
	gbc.weightx = 0;
	gbc.weighty = 0;
	gbc.gridy = 2;
	gbc.insets = new Insets(0, 10, 10, 10);
	gbc.fill = GridBagConstraints.NONE;
	gbc.anchor = GridBagConstraints.CENTER;
	this.getContentPane().add(this.deleteButton, gbc);

	this.loadMemory();
	this.pack();
    }

    public void quit() {
	this.saveMemory();
    }

    private final static String MEMORY_NODE = "MemoryList";
    private void loadMemory() {
	Preferences node = JXM.myUserNode().node(MEMORY_NODE);
	String[] keys;
	try {
	    keys = node.keys();
	}
	catch(BackingStoreException e) {
	    // ignore
	    return;
	}
	this.memoryListModel.removeAllElements();
	for(int i = 0; i < keys.length; i++) {
	    String record = node.get(keys[i], "");
	    MemoryListItem mli;
	    try {
		mli = new MemoryListItem(record);
	    }
	    catch(IllegalArgumentException e) {
		continue;
	    }
	    int j;
	    for(j = 0; j < this.memoryListModel.getSize(); j++)
		if (((MemoryListItem)this.memoryListModel.getElementAt(j)).getDate().getTime() > mli.getDate().getTime())
		    break;
	    this.memoryListModel.add(j, mli);
	}
    }
    private void saveMemory() {
	Preferences node = JXM.myUserNode().node(MEMORY_NODE);
	try {
	    node.clear();
	}
	catch(BackingStoreException e) {
	    return;
	}
	for(int i = 0; i < this.memoryListModel.getSize(); i++) {
	    MemoryListItem mli = ((MemoryListItem)this.memoryListModel.getElementAt(i));
	    String s = mli.serialize();
	    node.put(Integer.toString(i), s);
	}
    }

    public void memorize(ChannelInfo i) {
	MemoryListItem mli = new MemoryListItem(i);
	this.memoryListModel.add(this.memoryListModel.getSize(), mli);
	this.memoryList.setSelectedValue(mli, true);
	this.show();
    }
}
