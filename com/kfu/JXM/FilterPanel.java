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

 $Id: FilterPanel.java,v 1.1 2004/03/18 07:41:17 nsayer Exp $
 
 */

package com.kfu.JXM;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.prefs.*;

import com.kfu.xm.*;

public class FilterPanel extends JDialog {

    ArrayList filterSets = new ArrayList();
    int currentFilterIndex = -1;

    private class Filter {
	private HashSet filterList = new HashSet();
	public boolean isFiltered(int sid) { return this.filterList.contains(new Integer(sid)); }
	public void filterSid(int sid, boolean state) {
	    Integer value = new Integer(sid);
	    if (state) {
		this.filterList.add(value);
	    } else {
		this.filterList.remove(value);
	    }
	}
	private String name = "";
	public String getName() { return this.name; }
	public void setName(String name) { this.name = name; }
	public byte[] getFilterArray() {
	    byte[] out = new byte[this.filterList.size()];
	    Iterator i = this.filterList.iterator();
	    int j = 0;
	    while(i.hasNext())
		out[j++] = ((Integer)i.next()).byteValue();
	    return out;
	}
    }

    private MainWindow parent;

    private JTabbedPane theTabbedPane;
    private JPanel tabContents;
    private JTree theTree;
    private JTextField filterNameField;
    private JButton deleteButton;

/*
    class MyTreeCellEditor extends DefaultTreeCellEditor {
	public MyTreeCellEditor(JTree tree, DefaultTreeCellRenderer renderer) {
	    super(tree, renderer);
	}
	public MyTreeCellEditor(JTree tree, DefaultTreeCellRenderer renderer, TreeCellEditor editor) {
	    super(tree, renderer, editor);
	}
	public Component getTreeCellEditorComponent(JTree tree,  Object value,  boolean isSelected, boolean expanded,  boolean leaf,  int row) {
	    if (leaf) {
		ChannelInfo info = (ChannelInfo)value;
		JCheckBox out = new JCheckBox();
		out.setOpaque(false);
		out.setSelected(! FilterPanel.currentFilter().isFiltered(info.getServiceID()));
		out.setText(info.getChannelName());
		return out;
	    } else {
		return null;
	    }
	}
    }
*/

    private Filter currentFilter() {
	return (Filter)this.filterSets.get(this.currentFilterIndex);
    }

    class MyTreeCellRenderer extends DefaultTreeCellRenderer {
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
	    DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
	    if (node.isLeaf()) {
		ChannelInfo info = (ChannelInfo)node.getUserObject();
		// There is one moment where the root can be the only thing in the tree.
		// When that happens, it is a leaf, but has no User Object.
		if (info == null)
		    return super.getTreeCellRendererComponent(tree, value, false, expanded, leaf, row, false);
		JCheckBox jcb = new JCheckBox(info.getChannelName());
		jcb.setSelected(!FilterPanel.this.currentFilter().isFiltered(info.getServiceID()));
		jcb.setOpaque(false);
		return jcb;
	    } else {
		return super.getTreeCellRendererComponent(tree, value, false, expanded, leaf, row, false);
		//JLabel jl = new JLabel((String)node.getUserObject());
		//return jl;
	    }
	}
    }

    public FilterPanel(MainWindow parent) {
	super(parent.getFrame(), "JXM - Filter configuration", false);
	this.parent = parent;
	this.getContentPane().setLayout(new BorderLayout());

	this.theTabbedPane = new JTabbedPane();
	this.theTabbedPane.addChangeListener(new ChangeListener() {
	    public void stateChanged(ChangeEvent e) {
		int index = FilterPanel.this.theTabbedPane.getSelectedIndex();
		if (index < 0)
		    return;
		// This sucks. The tabbed pane can only have one copy of a given component.
		// So we have to fool it by having the deselected tabs hold a blank JPanel
		// And the selected one gets the real sheeet.
		int old = FilterPanel.this.theTabbedPane.indexOfComponent(FilterPanel.this.tabContents);
		if (old >= 0)
		    FilterPanel.this.theTabbedPane.setComponentAt(old, new JPanel());
		FilterPanel.this.theTabbedPane.setComponentAt(index, FilterPanel.this.tabContents);
		FilterPanel.this.currentFilterIndex = index;
		FilterPanel.this.filterNameField.setText(FilterPanel.this.theTabbedPane.getTitleAt(index));
		FilterPanel.this.refreshChannelList();
	    }
	});

	this.tabContents = new JPanel();
	this.tabContents.setLayout(new BorderLayout());
	this.theTree = new JTree();
	this.theTree.setRootVisible(false);
	//MyTreeCellRenderer r = new MyTreeCellRenderer();
	//this.theTree.setCellRenderer(r);
	this.theTree.setCellRenderer(new MyTreeCellRenderer());
	this.theTree.addMouseListener(new MouseAdapter() {
	    public void mouseClicked(MouseEvent e) {
		int selRow = FilterPanel.this.theTree.getRowForLocation(e.getX(), e.getY());
		if (selRow < 0)
		     return;
		TreePath tp = FilterPanel.this.theTree.getPathForRow(selRow);
		if (tp == null)
		     return;
		DefaultMutableTreeNode tn = ((DefaultMutableTreeNode)tp.getLastPathComponent()); // XXX is this cheating? We're only promised a 'treenode'
		Object o = tn.getUserObject();
		if (!(o instanceof ChannelInfo))
		    return;
		ChannelInfo info = (ChannelInfo)o;
		boolean currentState = FilterPanel.this.currentFilter().isFiltered(info.getServiceID());
		FilterPanel.this.currentFilter().filterSid(info.getServiceID(), !currentState);
		FilterPanel.this.theTree.getModel().valueForPathChanged(tp, info); // Use this to notify the renderer, indirectly.
	    }
	});
	//this.theTree.setCellEditor(new MyTreeCellEditor(this.theTree, r));
	this.tabContents.add(new JScrollPane(this.theTree), BorderLayout.CENTER);
	JPanel tabbot = new JPanel();
	JLabel jl = new JLabel("Filter name:");
	tabbot.add(jl);
	this.filterNameField = new JTextField();
	this.filterNameField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { this.doIt(); }
            public void insertUpdate(DocumentEvent e) { this.doIt(); }
            public void removeUpdate(DocumentEvent e) { this.doIt(); }
            private void doIt() {
		FilterPanel.this.currentFilter().setName(FilterPanel.this.filterNameField.getText());
                FilterPanel.this.theTabbedPane.setTitleAt(FilterPanel.this.theTabbedPane.getSelectedIndex(), FilterPanel.this.filterNameField.getText());
            }
        });
	this.filterNameField.setPreferredSize(new Dimension(150, (int)this.filterNameField.getPreferredSize().getHeight()));
	tabbot.add(this.filterNameField);
	JButton jb = new JButton("+");
	jb.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		FilterPanel.this.filterSets.add(new Filter());
		FilterPanel.this.theTabbedPane.addTab("", new JPanel());
		FilterPanel.this.currentFilterIndex = FilterPanel.this.filterSets.size() - 1;
		// After creating a brand new one, select it
		FilterPanel.this.theTabbedPane.setSelectedIndex(FilterPanel.this.currentFilterIndex);
		// We don't get to delete the last one.
		FilterPanel.this.deleteButton.setEnabled(FilterPanel.this.filterSets.size() > 1);
	    }
	});
	tabbot.add(jb);
	this.deleteButton = new JButton("-");
	this.deleteButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		// Remove the old stuff.
		FilterPanel.this.filterSets.remove(FilterPanel.this.currentFilterIndex);
		// This little dance shuffles the tab contents panel over to the new selected tab
		FilterPanel.this.theTabbedPane.remove(FilterPanel.this.currentFilterIndex);
		FilterPanel.this.currentFilterIndex = FilterPanel.this.theTabbedPane.getSelectedIndex();
		FilterPanel.this.theTabbedPane.setComponentAt(FilterPanel.this.currentFilterIndex, FilterPanel.this.tabContents);
		// We don't get to delete the last one.
		FilterPanel.this.deleteButton.setEnabled(FilterPanel.this.filterSets.size() > 1);
	    }
	});
	tabbot.add(this.deleteButton);
	this.tabContents.add(tabbot, BorderLayout.SOUTH);

	this.getContentPane().add(this.theTabbedPane, BorderLayout.CENTER);
	JPanel bot = new JPanel();
	jb = new JButton("Cancel");
	jb.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		FilterPanel.this.reloadFilterSettings();
		FilterPanel.this.hide();
	    }
	});
	bot.add(jb);
	jb = new JButton("OK");
	jb.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		FilterPanel.this.saveFilterSettings();
		FilterPanel.this.hide();
	    }
	});
	bot.add(jb);
	this.getContentPane().add(bot, BorderLayout.SOUTH);

        this.reloadFilterSettings();
	this.setSize(new Dimension(450, 550));
    }

    public void show() {
        this.reloadFilterSettings();
	//this.theTabbedPane.setSelectedIndex(-1);
	//this.theTabbedPane.setSelectedIndex(this.currentFilterIndex); // This will refresh everything via the change listener
	super.show();
    }

    private void refreshChannelList() {
	// Step 0 - make a new root and ditch the old tree
	DefaultMutableTreeNode root = new DefaultMutableTreeNode();
	((DefaultTreeModel)this.theTree.getModel()).setRoot(root);
	// Step 1 - make a HashMap of HashSets - The HashMap is genres, the HashSets are ChannelInfos.
	HashMap genres = new HashMap();
	ChannelInfo[] list = this.parent.getChannelList();
	for(int i = 0; i < list.length; i++) {
	    HashSet hs = (HashSet)genres.get(list[i].getChannelGenre());
	    if (hs == null) {
		hs = new HashSet();
		genres.put(list[i].getChannelGenre(), hs);
	    }
	    hs.add(list[i]);
	}
	// Step 2 - sort the genres
	HashSet[] genreList = new HashSet[genres.size()];
	int i = 0;
	Iterator it = genres.values().iterator();
	while(it.hasNext())
	    genreList[i++] = (HashSet)it.next();
	// Sort the genres by their lowest channel number.
	// Yes, this is terribly inefficient. Don't worry too much.
	// There aren't many genres and we only do this once in a while.
	Arrays.sort(genreList, new Comparator() {
	    public int compare(Object o1, Object o2) {
		HashSet h1 = (HashSet)o1;
		HashSet h2 = (HashSet)o2;
		int l1 = FilterPanel.lowestChannelInHashSet(h1);
		int l2 = FilterPanel.lowestChannelInHashSet(h2);
		return new Integer(l1).compareTo(new Integer(l2));
	    }
	});
	// Step 3, start building the tree - genres
	for(i = 0; i < genreList.length; i++) {
	    DefaultMutableTreeNode genreNode = new DefaultMutableTreeNode();
	    root.add(genreNode);

	    // Step 4 - for each genre, sort the channel list and build the genre node
	    HashSet hs = genreList[i];
	    ChannelInfo[] channels = new ChannelInfo[hs.size()];
	    it = hs.iterator();
	    int j = 0;
	    while(it.hasNext())
		channels[j++] = (ChannelInfo)it.next();

	    Arrays.sort(channels, new Comparator() {
		public int compare(Object o1, Object o2) {
		    ChannelInfo i1 = (ChannelInfo)o1;
		    ChannelInfo i2 = (ChannelInfo)o2;
		    return new Integer(i1.getChannelNumber()).compareTo(new Integer(i2.getChannelNumber()));
		}
	    });

	    genreNode.setUserObject(channels[0].getChannelGenre());
	    for(j = 0; j < channels.length; j++) {
		DefaultMutableTreeNode channelNode = new DefaultMutableTreeNode(channels[j], false);
		genreNode.add(channelNode);
		this.theTree.expandPath(new TreePath(new Object[] { root, genreNode, channelNode }));
	    }
	}
	((DefaultTreeModel)this.theTree.getModel()).nodeStructureChanged(root); // throw down the new tree
	expandAll(this.theTree);
    }

    private static void expandAll(JTree tree) {
	TreeNode root = (TreeNode)tree.getModel().getRoot();
	expandNode(tree, new TreePath(root));
    }
    private static void expandNode(JTree tree, TreePath path) {
	TreeNode node = (TreeNode)path.getLastPathComponent();
	for(Enumeration e = node.children(); e.hasMoreElements(); ) {
	    TreeNode n = (TreeNode)e.nextElement();
            TreePath newPath = path.pathByAddingChild(n);
	    expandNode(tree, newPath);
	}
	tree.expandPath(path);
    }

    private static int lowestChannelInHashSet(HashSet hs) {
	int low = Integer.MAX_VALUE;
	Iterator i = hs.iterator();
	while(i.hasNext()) {
	    ChannelInfo info = (ChannelInfo)i.next();
	    if (low > info.getChannelNumber())
		low = info.getChannelNumber();
	}
	return low;
    }

    private static final String FILTER_SET_KEY = "FilterSets"; // Contains named ByteArrays - each is a filter.
    private static final String FILTER_SET_ORDER = "FilterSetOrder"; // Contains the list of Filtersets, keyed to an integer order
    private static final String DEFAULT_FILTER_KEY = "DefaultFilter"; // A string, which must match the name of one of the filters
    private void reloadFilterSettings() {
	Preferences orderNode = JXM.myUserNode().node(FILTER_SET_ORDER);
	try {
	    String[] keys = orderNode.keys();
	    Arrays.sort(keys, new Comparator() {
		public int compare(Object o1, Object o2) {
		    int i1 = Integer.parseInt((String)o1);
		    int i2 = Integer.parseInt((String)o2);
		    return new Integer(i1).compareTo(new Integer(i2));
		}
	    });
	    Preferences node = JXM.myUserNode().node(FILTER_SET_KEY);
	    this.theTabbedPane.removeAll();
	    this.filterSets.clear();
	    for(int i = 0; i < keys.length; i++) {
		String name = orderNode.get(keys[i], null);
		if (name == null)
		     return;
		Filter f = new Filter();
		f.setName(name);
		byte[] sids = node.getByteArray(name, new byte[0]);
		for(int j = 0; j < sids.length; j++)
		    f.filterSid(sids[j] & 0xff, true);
		this.filterSets.add(f);
	    }
	}
	catch(BackingStoreException e) {
	    // ignore
	}
	if (this.filterSets.isEmpty()) {
	    Filter set = new Filter();
	    set.setName("Default");
	    this.filterSets.add(set);
	}
	for(int i = 0; i < this.filterSets.size(); i++)
	    this.theTabbedPane.add(((Filter)this.filterSets.get(i)).getName(), new JPanel());

	String defaultName = JXM.myUserNode().get(DEFAULT_FILTER_KEY, "");
	this.currentFilterIndex = 0; // default to the first one
	for(int i = 0; i < this.filterSets.size(); i++)
	    if (defaultName.equals(((Filter)this.filterSets.get(i)).getName())) {
		this.currentFilterIndex = i;
		break;
	    }
	this.theTabbedPane.setSelectedIndex(this.currentFilterIndex);

	this.deleteButton.setEnabled(this.filterSets.size() > 1);

	this.parent.setFilter(this.currentFilter().getFilterArray());
    }

    private void saveFilterSettings() {
	JXM.myUserNode().put(DEFAULT_FILTER_KEY, this.currentFilter().getName());
	Preferences node = JXM.myUserNode().node(FILTER_SET_KEY);
	try {
	    node.clear();
	    for(int i = 0; i < this.filterSets.size(); i++) {
		Filter f = (Filter)this.filterSets.get(i);
		node.putByteArray(f.getName(), f.getFilterArray());
	    }
	    node = JXM.myUserNode().node(FILTER_SET_ORDER);
	    node.clear();
	    for(int i = 0; i < this.filterSets.size(); i++) {
		Filter f = (Filter)this.filterSets.get(i);
		node.put(Integer.toString(i), f.getName());
	    }
	}
	catch(BackingStoreException e) {
	    // ignore
	}
	this.parent.setFilter(this.currentFilter().getFilterArray());
    }
}