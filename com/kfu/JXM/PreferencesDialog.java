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

 $Id: PreferencesDialog.java,v 1.16 2004/03/13 18:13:20 nsayer Exp $
 
 */

package com.kfu.JXM;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.net.*;
import java.util.*;
import java.util.prefs.*;

import com.kfu.xm.*;

public class PreferencesDialog extends JDialog {
    JComboBox deviceMenu;
    JLabel radioID;
    JTextField trackerURL;
    JTextField trackerUser;
    JPasswordField trackerPassword;
    JCheckBox trackerEnabled;
    JTextField browserPath = null;
    IPreferenceCallbackHandler handler;
    DefaultListModel bookmarks = new DefaultListModel();
    JList bookmarkList;
    JButton bookmarkDelButton;
    JTextField bmName;
    JTextField bmURL;
    JButton moveUpButton;
    JButton moveDownButton;
    JCheckBox startupCheckbox;

    public void setVisible(boolean b) {
	if (b)
	    this.reloadFromDefaults();
	super.setVisible(b);
    }
    public void show() {
	this.reloadFromDefaults();
	super.show();
    }

    public PreferencesDialog(JFrame parent, IPreferenceCallbackHandler handler) {
	super(parent, "JXM Preferences", true);
	this.handler = handler;
	this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

	this.getContentPane().setLayout(new BorderLayout());

	JTabbedPane jtp = new JTabbedPane();

	JPanel jp = new JPanel();
	jp.setLayout(new BoxLayout(jp, BoxLayout.PAGE_AXIS));
	JPanel jp2 = new JPanel();
	this.deviceMenu = new JComboBox();
	this.refreshDeviceMenu();
	jp2.add(this.deviceMenu);
	jp.add(jp2);

	jp2 = new JPanel();
	jp2.setLayout(new FlowLayout());
	JLabel jl = new JLabel("Radio ID:");
	jp2.add(jl);
	this.radioID = new JLabel(" ");
	this.radioID.setBorder(BorderFactory.createLineBorder(Color.BLACK));
	this.radioID.setHorizontalAlignment(SwingConstants.CENTER);
	this.radioID.setFont(new Font("Monospaced", Font.PLAIN, 20));
	this.radioID.setPreferredSize(new Dimension(150, (int)this.radioID.getPreferredSize().getHeight()));
	jp2.add(this.radioID);
	jp.add(jp2);
	
	jtp.addTab("Device", jp);

	jp = new JPanel();
	jp.setLayout(new GridBagLayout());
	GridBagConstraints gbc = new GridBagConstraints();
	gbc.insets = new Insets(5, 5, 0, 0);
	gbc.gridx = gbc.gridy = 0;
	gbc.weightx = 0.25;
	gbc.anchor = GridBagConstraints.LINE_END;
	jl = new JLabel("Base URL:");
	jp.add(jl, gbc);
	gbc.gridy = 1;
	jl = new JLabel("Username:");
	jp.add(jl, gbc);
	gbc.gridy = 2;
	jl = new JLabel("Password:");
	jp.add(jl, gbc);

	this.trackerURL = new JTextField();
	this.trackerURL.setPreferredSize(new Dimension(250, (int)this.trackerURL.getPreferredSize().getHeight()));
	gbc.weightx = 0.75;
	gbc.gridx = 1;
	gbc.gridy = 0;
	gbc.anchor = GridBagConstraints.LINE_START;
	jp.add(this.trackerURL, gbc);

	this.trackerUser = new JTextField();
	this.trackerUser.setPreferredSize(new Dimension(100, (int)this.trackerUser.getPreferredSize().getHeight()));
	gbc.gridy = 1;
	jp.add(this.trackerUser, gbc);

	this.trackerPassword = new JPasswordField();
	this.trackerPassword.setEchoChar((char)0x2022); // Unicode 'Bullet'
	this.trackerPassword.setPreferredSize(new Dimension(100, (int)this.trackerPassword.getPreferredSize().getHeight()));
	gbc.gridy = 2;
	jp.add(this.trackerPassword, gbc);

	this.trackerEnabled = new JCheckBox("Enable XM Tracker");
	this.trackerEnabled.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		PreferencesDialog.this.trackerCheckboxClicked();
	    }
	});
	gbc.gridx = 0;
	gbc.gridy = 3;
	gbc.gridwidth = 2;
	gbc.anchor = GridBagConstraints.CENTER;
	jp.add(this.trackerEnabled, gbc);

	jtp.addTab("XM Tracker", jp);

	jp = new JPanel();
	jp.setLayout(new GridBagLayout());
	gbc = new GridBagConstraints();
	JList jlist = new JList();
	this.bookmarkList = jlist;
	jlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	jlist.setLayoutOrientation(JList.VERTICAL);
	jlist.setVisibleRowCount(5);
	jlist.setModel(this.bookmarks);
	jlist.addListSelectionListener(new ListSelectionListener() {
	    public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting())
		    return;
		int index = PreferencesDialog.this.bookmarkList.getSelectedIndex();
		PreferencesDialog.this.bookmarkDelButton.setEnabled(index >= 0);
		PreferencesDialog.this.moveUpButton.setEnabled(index >= 1);
		PreferencesDialog.this.moveDownButton.setEnabled(index >= 0 && index < PreferencesDialog.this.bookmarks.size() - 1);
		PreferencesDialog.this.bmName.setEnabled(index >= 0);
		PreferencesDialog.this.bmURL.setEnabled(index >= 0);
		if (index < 0) {
		    PreferencesDialog.this.bmName.setText("");
		    PreferencesDialog.this.bmURL.setText("");
		    return;
		}
		Bookmark b = (Bookmark)PreferencesDialog.this.bookmarkList.getSelectedValue();
		PreferencesDialog.this.bmName.setText(b.getName());
		PreferencesDialog.this.bmURL.setText(b.getURL());
	    }
	});
	jlist.setCellRenderer(new DefaultListCellRenderer() {
	    public Component getListCellRendererComponent(JList list,  Object value,  int index,  boolean isSelected,  boolean cellHasFocus) {
		String val = ((Bookmark)value).getName();
		if (val == null || val == "")
		    val = " "; // XXX - If you send back the null string, the renderer will be 0 height. Stupid Java.
		return super.getListCellRendererComponent(list, val, index, isSelected, cellHasFocus);
	    }
	});
	gbc.gridwidth = 2;
	gbc.fill = GridBagConstraints.BOTH;
	gbc.weightx = gbc.weighty = 1;
	jp.add(new JScrollPane(jlist), gbc);
	JPanel jp1 = new JPanel();

	JButton addButton = new JButton("+");
	addButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		PreferencesDialog.this.bookmarks.add(PreferencesDialog.this.bookmarks.size(), new Bookmark("", ""));
		PreferencesDialog.this.bookmarkList.setSelectedIndex(PreferencesDialog.this.bookmarks.size() - 1);
		PreferencesDialog.this.bookmarkList.ensureIndexIsVisible(PreferencesDialog.this.bookmarks.size() - 1);
	    }
	});
	jp1.add(addButton);
	this.bookmarkDelButton = new JButton("-");
	this.bookmarkDelButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		int index = PreferencesDialog.this.bookmarkList.getSelectedIndex();
		PreferencesDialog.this.bookmarks.remove(index);
		PreferencesDialog.this.bookmarkList.setSelectedIndex(-1);
	    }
	});
	this.bookmarkDelButton.setEnabled(false);
	jp1.add(this.bookmarkDelButton);

	this.moveUpButton = new JButton("move up");
	this.moveUpButton.setEnabled(false);
	this.moveUpButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) { 
		int index = PreferencesDialog.this.bookmarkList.getSelectedIndex();
		Bookmark b = (Bookmark)PreferencesDialog.this.bookmarks.remove(index);
		PreferencesDialog.this.bookmarks.add(index - 1, b);
		PreferencesDialog.this.bookmarkList.setSelectedIndex(index - 1);
		PreferencesDialog.this.bookmarkList.ensureIndexIsVisible(index - 1);
	    }
	});
	jp1.add(this.moveUpButton);

	this.moveDownButton = new JButton("move down");
	this.moveDownButton.setEnabled(false);
	this.moveDownButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) { 
		int index = PreferencesDialog.this.bookmarkList.getSelectedIndex();
		Bookmark b = (Bookmark)PreferencesDialog.this.bookmarks.remove(index);
		PreferencesDialog.this.bookmarks.add(index + 1, b);
		PreferencesDialog.this.bookmarkList.setSelectedIndex(index + 1);
		PreferencesDialog.this.bookmarkList.ensureIndexIsVisible(index + 1);
	    }
	});
	jp1.add(this.moveDownButton);
	gbc.gridy = 1;
	gbc.fill = GridBagConstraints.NONE;
	gbc.anchor = GridBagConstraints.CENTER;
	gbc.weightx = gbc.weighty = 0;
	jp.add(jp1, gbc);
	
	jl = new JLabel("Name: ");
	gbc.gridy = 2;
	gbc.gridx = 0;
	gbc.gridwidth = 1;
	gbc.anchor = GridBagConstraints.LINE_END;
	jp.add(jl, gbc);
	this.bmName = new JTextField();
	this.bmName.getDocument().addDocumentListener(new DocumentListener() {
	    public void changedUpdate(DocumentEvent e) { this.doIt(); }
	    public void insertUpdate(DocumentEvent e) { this.doIt(); }
	    public void removeUpdate(DocumentEvent e) { this.doIt(); }
	    private void doIt() {
		int index = PreferencesDialog.this.bookmarkList.getSelectedIndex();
		if (index < 0)
		    return;
		Bookmark b = (Bookmark)PreferencesDialog.this.bookmarks.get(index);
		b.setName(PreferencesDialog.this.bmName.getText());
		PreferencesDialog.this.bookmarks.set(index, b);
	    }
	});
	gbc.gridx = 1;
	gbc.gridwidth = 3;
	gbc.weightx = 1;
	gbc.fill = GridBagConstraints.HORIZONTAL;
	jp.add(this.bmName, gbc);

	jl = new JLabel("URL: ");
	gbc.gridy = 3;
	gbc.gridx = 0;
	gbc.gridwidth = 1;
	gbc.weightx = 1;
	gbc.weightx = 0;
	gbc.fill = GridBagConstraints.NONE;
	gbc.anchor = GridBagConstraints.LINE_END;
	jp.add(jl, gbc);
	this.bmURL = new JTextField();
	this.bmURL.getDocument().addDocumentListener(new DocumentListener() {
	    public void changedUpdate(DocumentEvent e) { this.doIt(); }
	    public void insertUpdate(DocumentEvent e) { this.doIt(); }
	    public void removeUpdate(DocumentEvent e) { this.doIt(); }
	    private void doIt() {
		int index = PreferencesDialog.this.bookmarkList.getSelectedIndex();
		if (index < 0)
		    return;
		Bookmark b = (Bookmark)PreferencesDialog.this.bookmarks.get(index);
		b.setURL(PreferencesDialog.this.bmURL.getText());
		PreferencesDialog.this.bookmarks.set(index, b);
	    }
	});
	gbc.weightx = 1;
	gbc.fill = GridBagConstraints.HORIZONTAL;
	gbc.gridx = 1;
	gbc.gridwidth = 3;
	jp.add(this.bmURL, gbc);

	jl = new JLabel("<html><center><b>URL tokens:</b><table><tr><td>{NUMBER}</td><td>Channel number</td></tr>"+
	    "<tr><td>{GENRE}</td><td>Channel Genre</td></tr>"+
	    "<tr><td>{NAME}</td><td>Channel Name</td></tr>"+
	    "<tr><td>{ARTIST}</td><td>Artist currently playing on Channel</td></tr>"+
	    "<tr><td>{TITLE}</td><td>Song title currently playing on Channel</td></tr>"+
	    "<tr><td>{SERVICE}</td><td>Channel Service ID</td></tr>"+
	    "</table></center></html>");
	jl.setFont(new Font(null, Font.PLAIN, 10));
	jl.setMinimumSize(new Dimension(300, 150));
	gbc.fill = GridBagConstraints.NONE;
	gbc.anchor = GridBagConstraints.CENTER;
	gbc.weightx = 1;
	gbc.weighty = 0;
	gbc.gridwidth = 2;
	gbc.gridx = 0;
	gbc.gridy = 4;
	jp.add(jl, gbc);

	jtp.addTab("Bookmarks", jp);

	jp = new JPanel();
	jp.setLayout(new GridBagLayout());
	gbc = new GridBagConstraints();
	if (PlatformFactory.ourPlatform().needsBrowserPath()) {
	    jl = new JLabel("Path to Browser: ");
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.anchor = GridBagConstraints.LINE_END;
	    jp.add(jl, gbc);

	    this.browserPath = new JTextField();
	    gbc.gridx = 1;
	    gbc.weightx = 1;
	    gbc.fill = GridBagConstraints.HORIZONTAL;
	    jp.add(this.browserPath, gbc);
	}
	JButton jb = new JButton("Clear channel use statistics");
	jb.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		PreferencesDialog.this.handler.clearChannelStats();
	    }
	});
	gbc.gridy++;
	gbc.gridx = 0;
	gbc.gridwidth = 2;
	jp.add(jb, gbc);

	this.startupCheckbox = new JCheckBox("Check for new version at startup");
	gbc.gridy++;
	jp.add(this.startupCheckbox, gbc);

	jtp.addTab("Misc", jp);

	this.getContentPane().add(jtp, BorderLayout.CENTER);
	// -------

	jp = new JPanel();
	JButton ok = new JButton("Cancel");
	ok.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		PreferencesDialog.this.reloadFromDefaults();
		PreferencesDialog.this.hide();
	    }
	});
	jp.add(ok);
	ok = new JButton("OK");
	ok.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		PreferencesDialog.this.saveToDefaults();
		PreferencesDialog.this.hide();
	    }
	});
	jp.add(ok);
	this.getContentPane().add(jp, BorderLayout.PAGE_END);

	this.pack();
	this.reloadFromDefaults();
	this.doBookmarkRebuild();
    }

    private void trackerCheckboxClicked() {
	boolean newState = this.trackerEnabled.isSelected();
	if (newState) {
	    try {
		XMTracker.theTracker().setBaseURL(this.trackerURL.getText());
		XMTracker.theTracker().setCredentials(this.trackerUser.getText(), new String(this.trackerPassword.getPassword()));
		this.trackerURL.setEnabled(false);
		this.trackerUser.setEnabled(false);
		this.trackerPassword.setEnabled(false);
		this.trackerEnabled.setSelected(true);
	    }
	    catch(TrackerException e) {
		this.trackerEnabled.setSelected(false);
		this.handleTrackerException(e);
	    }
	} else {
	    this.trackerURL.setEnabled(true);
	    this.trackerUser.setEnabled(true);
	    this.trackerPassword.setEnabled(true);
	    this.trackerEnabled.setSelected(false);
	    try {
		XMTracker.theTracker().turnOff();
	    }
	    catch(TrackerException e) {
		this.handleTrackerException(e);
		return;
	    }
	    XMTracker.theTracker().Disable();
	}
    }

    // Used by about class
    final static String STARTUP_CHECK = "StartupVersionCheck";
    private final static String XMTRACKER_URL = "TrackerURL";
    private final static String XMTRACKER_USER = "TrackerUser";
    private final static String XMTRACKER_PASS = "TrackerPassword";
    private final static String XMTRACKER_ENABLED = "TrackerEnabled";
    private final static String BROWSER_PATH = "BrowserPath";
    private final static String BOOKMARKS = "Bookmarks";

    private String[][] defaultBookMarks = {
	{ "Channel Home Page",			"http://www.xmradio.com/programming/channel_page.jsp?ch={NUMBER}" },
	{ "View song ratings for channel",	"http://xmpcr.kfu.com/ratings.php?channel={NUMBER}" },
	{ "XMNation Forum for Channel",		"http://www.xmnation.net/forum_for_channel.php?ch={NUMBER}" },
	{ "Google search for Artist",		"http://www.google.com/search?q=%22{ARTIST}%22" },
	{ "Google search for Title",		"http://www.google.com/search?q=%22{TITLE}%22" },
	{ "Google search for Artist and Title",	"http://www.google.com/search?q=%22{ARTIST}%22+%22{TITLE}%22" },
	{ "iTunes search for Artist",		"itms://phobos.apple.com/WebObjects/MZSearch.woa/wa/com.apple.jingle.search.DirectAction/advancedSearchResults?artistTerm={ARTIST}" },
	{ "iTunes search for Artist and Title",	"itms://phobos.apple.com/WebObjects/MZSearch.woa/wa/com.apple.jingle.search.DirectAction/advancedSearchResults?artistTerm={ARTIST}&songTerm={TITLE}" },
    };

    private void reloadFromDefaults() {
	this.startupCheckbox.setSelected(JXM.myUserNode().getBoolean(STARTUP_CHECK, true));

	try {
	if (!JXM.myUserNode().nodeExists(BOOKMARKS)) {
	    // Load up the default set
	    Preferences marks = JXM.myUserNode().node(BOOKMARKS);
	    for(int i = 0; i < defaultBookMarks.length; i++) {
		String out;
		try {
		    out = URLEncoder.encode(defaultBookMarks[i][0], "US-ASCII") + ":" + URLEncoder.encode(defaultBookMarks[i][1], "US-ASCII");
		}
		catch(UnsupportedEncodingException e) {
		    continue;
		}
		marks.put(Integer.toString(i), out);
	    }
	}
	}
	catch(BackingStoreException e) {}
	Preferences marks = JXM.myUserNode().node(BOOKMARKS);
	String[] keys;
	try {
	    keys = marks.keys();
	}
	catch(BackingStoreException e) {
	    keys = new String[0];
	}
	Arrays.sort(keys);
	this.bookmarks.clear();
	for(int i = 0; i < keys.length; i++) {
	    String parts[] = marks.get(keys[i], "").split(":");
	    if (parts.length != 2)
		continue;
	    String name, url;
	    try {
		name = URLDecoder.decode(parts[0], "US-ASCII");
		url = URLDecoder.decode(parts[1], "US-ASCII");
	    }
	    catch(UnsupportedEncodingException e) {
		continue;
	    }
	    Bookmark b = new Bookmark(name, url);
	    this.bookmarks.add(this.bookmarks.getSize(), b);
	}

	this.trackerURL.setText(JXM.myUserNode().get(XMTRACKER_URL, "http://www.xmnation.net/tracker/"));
	this.trackerUser.setText(JXM.myUserNode().get(XMTRACKER_USER, ""));
	this.trackerPassword.setText(JXM.myUserNode().get(XMTRACKER_PASS, ""));
	this.trackerEnabled.setSelected(JXM.myUserNode().getBoolean(XMTRACKER_ENABLED, false));
	this.trackerCheckboxClicked();
	if (this.browserPath != null) {
	    this.browserPath.setText(JXM.myUserNode().get(BROWSER_PATH, ""));
	    PlatformFactory.ourPlatform().setBrowserPath(this.browserPath.getText());
	}
    }

    private void saveToDefaults() {
	JXM.myUserNode().putBoolean(STARTUP_CHECK, this.startupCheckbox.isSelected());

	Preferences node = JXM.myUserNode().node(BOOKMARKS);
	try {
	    node.clear();
	for(int i = 0; i < this.bookmarks.getSize(); i++) {
	    Bookmark bm = (Bookmark)this.bookmarks.getElementAt(i);
	    String out;
	    try {
		out = URLEncoder.encode(bm.getName(), "US-ASCII") + ":" + URLEncoder.encode(bm.getURL(), "US-ASCII");
	    }
	    catch(UnsupportedEncodingException e) {
		continue;
	    }
	    node.put(Integer.toString(i), out);
	}
	    this.doBookmarkRebuild();
	}
	catch(BackingStoreException e) { }
	JXM.myUserNode().put(XMTRACKER_URL, this.trackerURL.getText());
	JXM.myUserNode().put(XMTRACKER_USER, this.trackerUser.getText());
	JXM.myUserNode().put(XMTRACKER_PASS, new String(this.trackerPassword.getPassword()));
	JXM.myUserNode().putBoolean(XMTRACKER_ENABLED, this.trackerEnabled.isSelected());
	if (this.browserPath != null) {
	    JXM.myUserNode().put(BROWSER_PATH, this.browserPath.getText());
	    PlatformFactory.ourPlatform().setBrowserPath(this.browserPath.getText());
	}
    }

    private void doBookmarkRebuild() {
	Object[] bm = this.bookmarks.toArray();
	Bookmark[] out = new Bookmark[bm.length];
	System.arraycopy(bm, 0, out, 0, bm.length);
	this.handler.rebuildBookmarksMenu(out);
    }

    // This is called when the radio is turned on
    public void turnOn(String radioID) {
	this.radioID.setText(radioID);
	this.deviceMenu.setEnabled(false);
    }

    // This is called when the radio is turned off
    public void turnOff() {
	this.radioID.setText("");
	this.deviceMenu.setEnabled(true);
    }

    private void handleTrackerException(Exception e) {
	JOptionPane.showMessageDialog(this, e.getMessage(), "XM Tracker error", JOptionPane.ERROR_MESSAGE);
    }

    private final static String DEVICE_NAME_KEY = "DefaultDevice";
    private void refreshDeviceMenu() {
        this.deviceMenu = new JComboBox();
        this.deviceMenu.addItem("Pick device");

        String[] devices = RadioCommander.getPotentialDevices();
        for(int i = 0; i < devices.length; i++) {
            final String name = devices[i];
	    if (!PlatformFactory.ourPlatform().isDeviceValid(name))
		continue;
            this.deviceMenu.addItem(name);
        }
        this.deviceMenu.setSelectedIndex(0);
        this.deviceMenu.setSelectedItem(JXM.myUserNode().get(DEVICE_NAME_KEY, "Pick device"));
    }

    // -----------------------
    // Below are the properties we export to the world.

    public String getDevice() {
	String out = (String)this.deviceMenu.getSelectedItem();
	if (out == "" | out == "Pick device")
	    return null;
	return out;
    }
    // Call this after a successfull power-up
    public void saveDevice() {
	JXM.myUserNode().put(DEVICE_NAME_KEY, (String)this.deviceMenu.getSelectedItem());
    }
    public String getTrackerURL() { return this.trackerURL.getText(); }
    public String getTrackerUser() { return this.trackerUser.getText(); }
    public String getTrackerPassword() { return new String(this.trackerPassword.getPassword()); }
    public boolean isTrackerEnabled() { return this.trackerEnabled.isSelected(); }
}
