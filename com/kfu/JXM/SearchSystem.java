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

 $Id: SearchSystem.java,v 1.4 2004/04/28 16:01:11 nsayer Exp $
 
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
import java.security.*;
import java.util.*;
import java.util.prefs.*;
import java.util.regex.*;
import javax.swing.*;

import com.kfu.xm.*;

import com.wcohen.secondstring.*;

public class SearchSystem {

    private static float winningScore = .9f;

    // Set by the preferences pane
    public static void setSearchMatchScore(float win) {
	winningScore = win;
    }

    private final static Pattern legal = Pattern.compile("^\\s*(\\d+|\\d+\\s*\\-\\s*\\d+)(\\s*,\\s*(\\d+|\\d+\\s*\\-\\s*\\d+))*\\s*$");
    private final static Pattern extract = Pattern.compile("((?:\\d+)(?:\\s*\\-\\s*\\d+)?)");
    private static int[] parseChannelList(String s) {
	Matcher m = legal.matcher(s);
	if (!m.matches()) {
	    throw new IllegalArgumentException("Channel list syntax error");
	}
	m = extract.matcher(s);
	ArrayList al = new ArrayList();
	while(m.find()) {
	    String group = m.group();
	    int where = group.indexOf('-');
	    if (where >= 0) {
		int start = Integer.parseInt(group.substring(0, where));
		int end = Integer.parseInt(group.substring(where + 1));
		if (start > 256 || end > 256)
		    throw new IllegalArgumentException("Channel number way too big");
		if (start > end) {
		    int j = start; start = end; end = j;
		}
		for(int i = start; i <= end; i++)
		    al.add(new Integer(i));
	    } else {
		int it = Integer.parseInt(group);
		al.add(new Integer(it));
	    }
	}
	int[] out = new int[al.size()];
	for(int i = 0; i < al.size(); i++)
	    out[i] = ((Integer)al.get(i)).intValue();

	Arrays.sort(out);

	return out;
    }

    public JComponent getPrefPanel() {
	return this.searchConfig;
    }

    public class SearchMatcher {
	private String artistString, titleString, acceptableChannelsString;
	private StringWrapper artistMatcher, titleMatcher;
	private int[] acceptableChannels;
	public SearchMatcher() {
	    this.setChannels(null);
	    this.setArtist(null);
	    this.setTitle(null);
	}

	public void setChannels(String list) {
	    if (list == null)
		list = "";
	    list = list.trim();
	    if (list.length() == 0) {
		this.acceptableChannels = new int[0];
	    } else {
		this.acceptableChannels = parseChannelList(list);
	    }
	    this.acceptableChannelsString = list;
	}
	public void setArtist(String artist) {
	    if (artist != null && artist.trim().length() == 0)
		artist = null;
	    if (artist == null) {
		this.artistString = null;
		this.artistMatcher = null;
	    } else {
		this.artistString = artist;
		this.artistMatcher = matchAlgorithm.prepare(artist.trim().toLowerCase());
	    }
	}
	public void setTitle(String title) {
	    if (title != null && title.trim().length() == 0)
		title = null;
	    if (title == null) {
		this.titleString = null;
		this.titleMatcher = null;
	    } else {
		this.titleString = title;
		this.titleMatcher = matchAlgorithm.prepare(title.trim().toLowerCase());
	    }
	}
	public String getArtist() {
	    return (this.artistString == null)?"":this.artistString;
	}
	public String getTitle() {
	    return (this.titleString == null)?"":this.titleString;
	}
	public String getChannels() {
	    return this.acceptableChannelsString;
	}
	// deserialization constructor
	public SearchMatcher(String serialized) {
	    this();
	    String[] split = serialized.split(":", -1);
	    if (split.length != 3)
		throw new IllegalArgumentException("Wrong number of fields.");
	    for(int i = 0; i < split.length; i++) {
		try {
		    split[i] = URLDecoder.decode(split[i], "US-ASCII");
		}
		catch(UnsupportedEncodingException ex) {
		    //impossible
		}
	    }
	    this.setChannels(split[0]);
	    this.setArtist(split[1]);
	    this.setTitle(split[2]);
	}
	// Create a string that will be fed into the serialization constructor at load time
	public String serialize() {
	    StringBuffer sb = new StringBuffer();
	    try {
		sb.append(URLEncoder.encode(this.getChannels(), "US-ASCII"));
		sb.append(':');
		sb.append(URLEncoder.encode(this.getArtist(), "US-ASCII"));
		sb.append(':');
		sb.append(URLEncoder.encode(this.getTitle(), "US-ASCII"));
	    }
	    catch(UnsupportedEncodingException ex) {
		//impossible
	    }
	    return sb.toString();
	}
	public boolean isMatch(int channel, StringWrapper artist, StringWrapper title) {
	    if (this.artistMatcher == null && this.titleMatcher == null) // You can't have it match *nothing*
		return false;

	    // Step 1... Do we have a list of acceptable channels? If so, is this one acceptable? If not, return false
	    if (this.acceptableChannels.length > 0) {
		if (Arrays.binarySearch(this.acceptableChannels, channel) < 0)
		    return false;
	    }

	    // Step 2... If there is an artist match, get the artist score
	    //           If lower than the winningScore, return false
	    if (this.artistMatcher != null && matchAlgorithm.score(this.artistMatcher, artist) < winningScore)
		return false;

	    // Step 3... If there is a title match, get the title score
	    //           If lower than the winningScore, return false
	    if (this.titleMatcher != null && matchAlgorithm.score(this.titleMatcher, title) < winningScore)
		return false;

	    return true;
	}
    }

    private MainWindow parent;

    private JPanel searchConfig;
    private DefaultListModel searchList = new DefaultListModel();
    private JList configList;
    private JTextField channelEditor, artistEditor, titleEditor;
    private JButton deleteButton;
    private SearchMatcher searchBeingEdited;
    private boolean ignoreUpdates = false;

    private JDialog searchMatches;
    private DefaultListModel matches = new DefaultListModel();
    private JList matchList;

    public SearchSystem(MainWindow parent) {
	this.parent = parent;

	GridBagConstraints gbc;

	this.searchConfig = new JPanel();
	this.searchConfig.setLayout(new GridBagLayout());
	this.configList = new JList(this.searchList);
	class SearchConfigRenderer extends JPanel implements ListCellRenderer {
	    JLabel chanLabel, artLabel, titLabel;
	    public SearchConfigRenderer() {
		this.setOpaque(true);
		this.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		JLabel jl = new JLabel("Channels:");
		jl.setHorizontalAlignment(SwingConstants.TRAILING);
		gbc.insets = new Insets(0, 10, 0, 10);
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		this.add(jl, gbc);
		jl = new JLabel("Artist:");
		jl.setHorizontalAlignment(SwingConstants.TRAILING);
		gbc.gridy = 1;
		this.add(jl, gbc);
		jl = new JLabel("Title:");
		jl.setHorizontalAlignment(SwingConstants.TRAILING);
		gbc.gridy = 2;
		this.add(jl, gbc);
		this.chanLabel = new JLabel();
		this.chanLabel.setHorizontalAlignment(SwingConstants.LEADING);
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.anchor = GridBagConstraints.LINE_START;
		this.add(this.chanLabel, gbc);
		this.artLabel = new JLabel();
		this.artLabel.setHorizontalAlignment(SwingConstants.LEADING);
		gbc.gridy = 1;
		this.add(this.artLabel, gbc);
		this.titLabel = new JLabel();
		this.titLabel.setHorizontalAlignment(SwingConstants.LEADING);
		gbc.gridy = 2;
		this.add(this.titLabel, gbc);
		this.validate();
	    }
	    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		SearchMatcher sm = (SearchMatcher)value;
		this.chanLabel.setText(sm.getChannels());
		this.artLabel.setText(sm.getArtist());
		this.titLabel.setText(sm.getTitle());
		if (isSelected) {
		    this.setBackground(SearchSystem.this.configList.getSelectionBackground());
		} else {
		    this.setBackground(SearchSystem.this.configList.getBackground());
		}
                this.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, MainWindow.gridColor));
		return this;
	    }
	}
	this.configList.setCellRenderer(new SearchConfigRenderer());
	this.configList.addListSelectionListener(new ListSelectionListener() {
	    public void valueChanged(ListSelectionEvent e) {
		SearchSystem.this.deleteButton.setEnabled(SearchSystem.this.configList.getSelectedValue() != null);
		if (SearchSystem.this.configList.getSelectedValues().length != 1) {
		    SearchSystem.this.ignoreUpdates = true;
		    try {
			SearchSystem.this.channelEditor.setText("");
			SearchSystem.this.channelEditor.setEnabled(false);
			SearchSystem.this.artistEditor.setText("");
			SearchSystem.this.artistEditor.setEnabled(false);
			SearchSystem.this.titleEditor.setText("");
			SearchSystem.this.titleEditor.setEnabled(false);
		    }
		    finally {
			SearchSystem.this.ignoreUpdates = false;
		    }
		} else {
		    SearchSystem.this.searchBeingEdited = (SearchMatcher)SearchSystem.this.configList.getSelectedValue();
		    SearchSystem.this.ignoreUpdates = true;
		    try {
			SearchSystem.this.channelEditor.setText(SearchSystem.this.searchBeingEdited.getChannels());
			SearchSystem.this.channelEditor.setEnabled(true);
			SearchSystem.this.artistEditor.setText(SearchSystem.this.searchBeingEdited.getArtist());
			SearchSystem.this.artistEditor.setEnabled(true);
			SearchSystem.this.titleEditor.setText(SearchSystem.this.searchBeingEdited.getTitle());
			SearchSystem.this.titleEditor.setEnabled(true);
		    }
		    finally {
			SearchSystem.this.ignoreUpdates = false;
		    }
		}
	    }
	});
	gbc = new GridBagConstraints();
	gbc.gridwidth = 2;
	gbc.weightx = 1;
	gbc.weighty = 1;
	gbc.fill = GridBagConstraints.BOTH;
	this.searchConfig.add(this.configList, gbc);

	JPanel jp = new JPanel();
	jp.setLayout(new GridBagLayout());
	GridBagConstraints gbc1 = new GridBagConstraints();

	JLabel jl = new JLabel("Channels: ");
	gbc1.insets = new Insets(0, 10, 0, 10);
	gbc1.fill = GridBagConstraints.NONE;
	gbc1.anchor = GridBagConstraints.LINE_END;
	gbc1.weightx = 0;
	jp.add(jl, gbc1);
	jl = new JLabel("Artist: ");
	gbc1.gridy = 1;
	jp.add(jl, gbc1);
	jl = new JLabel("Title: ");
	gbc1.gridy = 2;
	jp.add(jl, gbc1);
	this.channelEditor = new JTextField();
	this.channelEditor.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		try {
		    SearchSystem.this.searchBeingEdited.setChannels(SearchSystem.this.channelEditor.getText());
		    SearchSystem.this.searchList.set(SearchSystem.this.configList.getSelectedIndex(), SearchSystem.this.searchBeingEdited);
		}
		catch(IllegalArgumentException ex) {
		    Toolkit.getDefaultToolkit().beep();
		    SearchSystem.this.channelEditor.requestFocus();
		}
	    }
	});
	this.channelEditor.addFocusListener(new FocusListener() {
	    public void focusGained(FocusEvent e) { }
	    public void focusLost(FocusEvent e) {
		SearchSystem.this.channelEditor.postActionEvent();
	    }
	});
	this.channelEditor.setInputVerifier(new InputVerifier() {
	    public boolean verify(JComponent tf) {
		String val = ((JTextField)tf).getText();
		try {
		    parseChannelList(val);
		    return true;
		}
		catch(IllegalArgumentException ex) {
		    return false;
		}
	    }
	});
	gbc1.weightx = 1;
	gbc1.gridx = 1;
	gbc1.gridy = 0;
	gbc1.fill = GridBagConstraints.HORIZONTAL;
	jp.add(this.channelEditor, gbc1);
	this.artistEditor = new JTextField();
	this.artistEditor.getDocument().addDocumentListener(new DocumentListener() {
	    public void changedUpdate(DocumentEvent e) { this.doit(); }
	    public void insertUpdate(DocumentEvent e) { this.doit(); }
	    public void removeUpdate(DocumentEvent e) { this.doit(); }
	    private void doit() {
		if (SearchSystem.this.ignoreUpdates)
		    return;
		SearchSystem.this.searchBeingEdited.setArtist(SearchSystem.this.artistEditor.getText());
		SearchSystem.this.searchList.set(SearchSystem.this.configList.getSelectedIndex(), SearchSystem.this.searchBeingEdited);
	    }
	});
	gbc1.gridy = 1;
	jp.add(this.artistEditor, gbc1);
	this.titleEditor = new JTextField();
	this.titleEditor.getDocument().addDocumentListener(new DocumentListener() {
	    public void changedUpdate(DocumentEvent e) { this.doit(); }
	    public void insertUpdate(DocumentEvent e) { this.doit(); }
	    public void removeUpdate(DocumentEvent e) { this.doit(); }
	    private void doit() {
		if (SearchSystem.this.ignoreUpdates)
		    return;
		SearchSystem.this.searchBeingEdited.setTitle(SearchSystem.this.titleEditor.getText());
		SearchSystem.this.searchList.set(SearchSystem.this.configList.getSelectedIndex(), SearchSystem.this.searchBeingEdited);
	    }
	});
	gbc1.gridy = 2;
	jp.add(this.titleEditor, gbc1);

	gbc.weighty = 0;
	gbc.gridy = 1;
	this.searchConfig.add(jp, gbc);
	JButton jb = new JButton("+");
	jb.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		SearchSystem.this.searchList.add(SearchSystem.this.searchList.size(), new SearchMatcher());
		SearchSystem.this.configList.setSelectedIndex(SearchSystem.this.searchList.size() - 1);
	    }
	});
	gbc.gridy = 2;
	gbc.gridwidth = 1;
	gbc.fill = GridBagConstraints.NONE;
	gbc.anchor = GridBagConstraints.LINE_END;
	this.searchConfig.add(jb, gbc);
	this.deleteButton = new JButton("-");
	this.deleteButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		Object[] list = SearchSystem.this.configList.getSelectedValues();
		for(int i = 0; i < list.length; i++)
		    SearchSystem.this.searchList.removeElement(list[i]);
	    }
	});
	this.deleteButton.setEnabled(false);
	gbc.gridx = 1;
	gbc.anchor = GridBagConstraints.LINE_START;
	this.searchConfig.add(this.deleteButton, gbc);

	this.searchMatches = new JDialog(this.parent.getFrame(), "JXM - Search matches", false);
	this.searchMatches.getContentPane().setLayout(new GridBagLayout());
	gbc = new GridBagConstraints();
	this.matchList = new JList(this.matches);
	class SearchResultRenderer extends JPanel implements ListCellRenderer {
	    MainWindow.ChannelInfoPanel infoPanel = new MainWindow.ChannelInfoPanel();
	    public SearchResultRenderer() {
		this.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.BOTH;
		this.add(this.infoPanel, gbc);
	    }
	    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		this.infoPanel.setChannelInfo((ChannelInfo)value);
                this.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, MainWindow.gridColor));
		return this;
	    }
	}
	this.matchList.setCellRenderer(new SearchResultRenderer());
	this.matchList.addMouseListener(new MouseAdapter() {
	    public void mousePressed(MouseEvent e) { this.maybePopup(e); }
	    public void mouseReleased(MouseEvent e) { this.maybePopup(e); }
	    private void maybePopup(MouseEvent e) {
		if (!e.isPopupTrigger())
		    return;
		int row = SearchSystem.this.matchList.locationToIndex(e.getPoint());
		if (row < 0)
		    return;
		ChannelInfo info = (ChannelInfo)SearchSystem.this.matches.getElementAt(row);
		JPopupMenu jpm = SearchSystem.this.parent.new ChannelPopupMenu(info, 0);
		jpm.show(e.getComponent(), e.getX(), e.getY());
	    }
	});

	this.matchList.setFixedCellHeight(80);
	this.matchList.setFixedCellWidth(300);
	this.matchList.setVisibleRowCount(3);
	gbc.weightx = 1;
	gbc.weighty = 1;
	gbc.fill = GridBagConstraints.BOTH;
	this.searchMatches.getContentPane().add(new JScrollPane(this.matchList), gbc);
	this.searchMatches.pack();

	this.reloadFromPreferences();
    }

    private final static String SEARCH_LIST_KEY = "SearchItems";

    public void reload() {
	this.reloadFromPreferences();
    }
    public void save() {
	this.saveToPreferences();
    }

    private void reloadFromPreferences() {
	Preferences node = JXM.myUserNode().node(SEARCH_LIST_KEY);
	String[] keys;
	try {
	    keys = node.keys();
	}
	catch(BackingStoreException ex) {
	    return;
	}
	Arrays.sort(keys, new Comparator() {
	    public int compare(Object o1, Object o2) {
		try {
		    int i1 = Integer.parseInt(o1.toString());
		    int i2 = Integer.parseInt(o2.toString());
		    return new Integer(i1).compareTo(new Integer(i2));
		}
		catch(NumberFormatException ex) {
		    return 0;
		}
	    }
	});
	this.searchList.clear();
	for(int i = 0; i < keys.length; i++) {
	    try {
		String s = node.get(keys[i], "");
		SearchMatcher sm = new SearchMatcher(s);
		this.searchList.add(this.searchList.getSize(), sm);
	    }
	    catch(IllegalArgumentException ex) {
		// ignore
	    }
	}
    }
    private void saveToPreferences() {
	// Get rid of any that have a blank artist and title. They are Not Allowed.
	for(int i = 0; i < this.searchList.size(); i++) {
	    SearchMatcher sm = (SearchMatcher)this.searchList.get(i);
	    if (sm.getArtist().length() == 0 && sm.getTitle().length() == 0) {
		this.searchList.removeElement(sm);
		i--;
	    }
	}
	Preferences node = JXM.myUserNode().node(SEARCH_LIST_KEY);
	try {
	    node.clear();
	}
	catch(BackingStoreException ex) {
	    // don't care
	}
	for(int i = 0; i < this.searchList.size(); i++) {
	    SearchMatcher sm = (SearchMatcher)this.searchList.get(i);
	    node.put(Integer.toString(i), sm.serialize());
	}
    }

    private final static StringDistance matchAlgorithm = new JaroWinkler();

    private void turnOff() {
	this.matches.removeAllElements();
	this.searchMatches.hide();
    }

    public void update(final ChannelInfo info) {

	if (info == null) {
	    this.turnOff();
	    return;
	}

	int channel = info.getChannelNumber();
	StringWrapper artist = matchAlgorithm.prepare(info.getChannelArtist().trim().toLowerCase());
	StringWrapper title = matchAlgorithm.prepare(info.getChannelTitle().trim().toLowerCase());

	// Step 1. Find out whether this info is a match or not.
	boolean isMatch = false;
	for(int i = 0; i < this.searchList.size(); i++) {
	    SearchMatcher sm = (SearchMatcher)this.searchList.get(i);
	    isMatch = sm.isMatch(channel, artist, title);
	    if (isMatch)
		break; // Once it's a match, we're done
	}

	if (isMatch) {
	// CASES TO HANDLE:
	// Is Match, in matched channel list: A transition from one match to another
	// Is Match, not in matched channel list: A transition into a match
	// No match, in matched channel list: A transition away from a match
	    int chan = info.getChannelNumber();
	    for(int i = 0; i < this.matches.size(); i++) {
		ChannelInfo ci = (ChannelInfo)this.matches.getElementAt(i);
		if (ci.getChannelNumber() == chan) {
		    this.matches.removeElement(ci);
		    break;
		}
	    }
	    this.matches.add(this.matches.size(), info);
	    // Every time we add to the list, pop it open
	    if(!this.searchMatches.isVisible()) {
		this.parent.forceNormalView();
		this.searchMatches.show();
	    }
	} else {
	    int chan = info.getChannelNumber();
	    for(int i = 0; i < this.matches.size(); i++) {
		ChannelInfo ci = (ChannelInfo)this.matches.getElementAt(i);
		if (ci.getChannelNumber() == chan) {
		    // This was a match, but is not anymore.
		    this.matches.removeElement(ci);
		    if (this.matches.size() == 0 && this.searchMatches.isVisible()) {
			// When we remove the last item from the list, hide it.
			this.searchMatches.hide();
		    }
		    break;
		}
	    }
	}
    }
}
