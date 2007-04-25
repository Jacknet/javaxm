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

 $Id: PreferencesDialog.java,v 1.33 2007/04/25 22:05:12 nsayer Exp $
 
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
    private JTabbedPane theTabs;
    private JComboBox deviceMenu;
    private JCheckBox deviceIsXmDirect;
    private JLabel radioID;
    private JTextField trackerURL;
    private JTextField trackerUser;
    private JPasswordField trackerPassword;
    private JCheckBox trackerEnabled;
    private JTextField browserPath = null;
    private IPreferenceCallbackHandler handler;
    private DefaultListModel bookmarks = new DefaultListModel();
    private JList bookmarkList;
    private JButton bookmarkDelButton;
    private JTextField bmName;
    private JTextField bmURL;
    private JButton moveUpButton;
    private JButton moveDownButton;
    private JCheckBox startupCheckbox;
    private JComboBox sleepAction;
    private JSlider searchAccuracy;

    String[] sleepActions = {"Mute Radio Volume", "Turn Radio Off", "Quit JXM Program" };
    static final int SLEEP_MUTE = 0;
    static final int SLEEP_OFF = 1;
    static final int SLEEP_QUIT = 2;
    static final int SLEEP_MAX = SLEEP_QUIT;

    public void setVisible(boolean b) {
		if (b)
		    this.reloadFromDefaults();
		super.setVisible(b);
	}
/*
	public void show() {
		this.reloadFromDefaults();
		super.setVisible(true);
	}
*/

    public final static int TAB_DEVICE = 0;
    public final static int TAB_TRACKER = 1;
    public final static int TAB_BOOKMARKS = 2;
    public final static int TAB_FILTERS = 3;
    public final static int TAB_SEARCH = 4;
    public final static int TAB_MISC = 5;

	public void showTab(int tab) {
		this.theTabs.setSelectedIndex(tab);
		this.setVisible(true);
    }
	public void addNewSongSearch(ChannelInfo info) {
		this.reloadFromDefaults();
		this.theTabs.setSelectedIndex(TAB_SEARCH);
		this.handler.getSearchSystem().addNewSong(info);
		super.setVisible(true);
	}

    public PreferencesDialog(JFrame parent, IPreferenceCallbackHandler handler) {
		super(parent, "JXM Preferences", true);
		this.handler = handler;
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
	
		this.getContentPane().setLayout(new BorderLayout());

		// the main set of Tabs
		this.theTabs = new JTabbedPane();

		// The Devices Tab
		// =================================
		JPanel jp = new JPanel();
		jp.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		JPanel jp2 = new JPanel();
		this.deviceMenu = new JComboBox();
		jp2.add(this.deviceMenu);

                this.deviceIsXmDirect = new JCheckBox("XM Direct");
                this.deviceIsXmDirect.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			PreferencesDialog.this.refreshDeviceMenu();
		    }
		});
                jp2.add(this.deviceIsXmDirect);

                this.deviceIsXmDirect.setSelected(JXM.myUserNode().getBoolean(DEVICE_IS_XMDIRECT_KEY, false));
		this.refreshDeviceMenu();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.PAGE_END;
		jp.add(jp2, gbc);
	
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
		gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.PAGE_START;
		jp.add(jp2, gbc);
		
		this.theTabs.insertTab("Device", null, jp, null, TAB_DEVICE);


		// XM Tracker Tab
		// =================================
		jp = new JPanel();
		jp.setLayout(new GridBagLayout());
		gbc = new GridBagConstraints();
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
	
		this.theTabs.insertTab("XM Tracker", null, jp, null, TAB_TRACKER);


		// Bookmarks Tab
		// =================================
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
			int index = PreferencesDialog.this.bookmarkList.getSelectedIndex();
			if (index < 0)
			    PreferencesDialog.this.bookmarks.add(PreferencesDialog.this.bookmarks.size(), new Bookmark("", ""));
			else {
			    Bookmark b = (Bookmark)PreferencesDialog.this.bookmarks.get(index);
			    PreferencesDialog.this.bookmarks.add(PreferencesDialog.this.bookmarks.size(), new Bookmark("copy of " + b.getName(), b.getURL()));
			}
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
		this.bmName.setEnabled(false);
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
		this.bmURL.setEnabled(false);
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
	
		this.theTabs.insertTab("Web Bookmarks", null, jp, null, TAB_BOOKMARKS);


		// Filters tab
		// =================================
		this.theTabs.insertTab("Filters", null, this.handler.getFilterPreferencePanel(), null, TAB_FILTERS);
		this.theTabs.setEnabledAt(TAB_FILTERS, false);

		// Search tab
		// =================================
		this.theTabs.insertTab("Search", null, this.handler.getSearchPreferencePanel(), null, TAB_SEARCH);


		// Misc tab
		// =================================

		// Channel stats
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
		gbc.insets = new Insets(5, 0, 5, 0);
		jp.add(jb, gbc);

		// Version check
		this.startupCheckbox = new JCheckBox("Check for new version at startup");
		gbc.gridy++;
		jp.add(this.startupCheckbox, gbc);
	
		jl = new JLabel("When sleep timer expires: ");
		jl.setHorizontalAlignment(SwingConstants.TRAILING);
		gbc.gridx = 0;
		gbc.gridwidth = 1;
		gbc.gridy++;
		jp.add(jl, gbc);

		// Sleep action
		this.sleepAction = new JComboBox(sleepActions);
		gbc.gridx = 1;
		jp.add(this.sleepAction, gbc);

		// Search accuracy, how "fuzzy" of a match will we accept
		jl = new JLabel("search accuracy");
		gbc.gridx = 0;
		gbc.gridy++;
		gbc.gridwidth = 2;
		gbc.insets = new Insets(5, 0, 0, 0);
		jp.add(jl, gbc);
		this.searchAccuracy = new JSlider();
		this.searchAccuracy.setMinimum(500);
		this.searchAccuracy.setMaximum(1000);
		this.searchAccuracy.setMajorTickSpacing(500);
		this.searchAccuracy.setMinorTickSpacing(100);
		this.searchAccuracy.setPaintTicks(true);
		this.searchAccuracy.setSnapToTicks(false);
		this.searchAccuracy.setValue(900);
		gbc.gridy++;
		gbc.insets = new Insets(0, 0, 0, 0);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		jp.add(this.searchAccuracy, gbc);
		jl = new JLabel("loose");
		gbc.gridy++;
		gbc.gridwidth = 1;
		gbc.insets = new Insets(0, 0, 5, 0);
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.LINE_START;
		jp.add(jl, gbc);
		jl = new JLabel("tight");
		gbc.gridx = 1;
		gbc.anchor = GridBagConstraints.LINE_END;
		jp.add(jl, gbc);

		this.theTabs.insertTab("Misc", null, jp, null, TAB_MISC);


		// Add the tab set to the main content area
		this.getContentPane().add(this.theTabs, BorderLayout.CENTER);
		// -------

		// OK and Cancel buttons
		// ==================================
		jp = new JPanel();
		JButton ok = new JButton("Cancel");
		ok.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			PreferencesDialog.this.reloadFromDefaults();
			PreferencesDialog.this.setVisible(false);
		    }
		});
		jp.add(ok);
		ok = new JButton("OK");
		ok.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			PreferencesDialog.this.saveToDefaults();
			PreferencesDialog.this.setVisible(false);
		    }
		});
		jp.add(ok);
		this.getContentPane().add(jp, BorderLayout.PAGE_END);

		// All set, get ready to display
		// =================================
		this.reloadFromDefaults();
		this.doBookmarkRebuild();
		this.pack();

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
				// fail safe: Explicitely disable it for next time
				JXM.myUserNode().putBoolean(XMTRACKER_ENABLED, false);
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
    private final static String SLEEP_ACTION = "SleepAction";
    private final static String SEARCH_ACCURACY = "SearchAccuracy";
    private final static String XMTRACKER_URL = "TrackerURL";
    private final static String XMTRACKER_USER = "TrackerUser";
    private final static String XMTRACKER_PASS = "TrackerPassword";
    private final static String XMTRACKER_ENABLED = "TrackerEnabled";
    private final static String BROWSER_PATH = "BrowserPath";
    private final static String BOOKMARKS = "Bookmarks";

    private String[][] defaultBookMarks = {
		{ "Channel Home Page",			"http://www.xmradio.com/programming/channel_page.jsp?ch={NUMBER}" },
		{ "Channel Guide Page",			"http://www.xmradio.com/programming/channel_guide.jsp?ch={NUMBER}" },
		{ "View song ratings for channel",	"http://xmpcr.kfu.com/ratings.php?channel={NUMBER}" },
		{ "Forum for Channel",		"http://xmpcr.kfu.com/forums/forum_for_channel.php?ch={NUMBER}" },
		{ "Google search for Artist",		"http://www.google.com/search?q=%22{ARTIST}%22" },
		{ "Google search for Title",		"http://www.google.com/search?q=%22{TITLE}%22" },
		{ "Google search for Artist and Title",	"http://www.google.com/search?q=%22{ARTIST}%22+%22{TITLE}%22" },
		{ "iTunes search for Artist",		"itms://phobos.apple.com/WebObjects/MZSearch.woa/wa/com.apple.jingle.search.DirectAction/advancedSearchResults?artistTerm={ARTIST}" },
		{ "iTunes search for Artist and Title",	"itms://phobos.apple.com/WebObjects/MZSearch.woa/wa/com.apple.jingle.search.DirectAction/advancedSearchResults?artistTerm={ARTIST}&songTerm={TITLE}" },
    };

    private void reloadFromDefaults() {
		this.handler.reload();
		this.startupCheckbox.setSelected(JXM.myUserNode().getBoolean(STARTUP_CHECK, true));
		int val = JXM.myUserNode().getInt(SLEEP_ACTION, 0);
		if (val < 0 || val > SLEEP_MAX)
		    val = 0;
		this.sleepAction.setSelectedIndex(val);
	
		val = JXM.myUserNode().getInt(SEARCH_ACCURACY, 900);
		if (val < 500 || val > 1000)
		    val = 900;
		this.searchAccuracy.setValue(val);

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
		Arrays.sort(keys, new Comparator() {
		    public int compare(Object o1, Object o2) {
			try {
			int i1 = Integer.parseInt(o1.toString());
			int i2 = Integer.parseInt(o2.toString());
			return new Integer(i1).compareTo(new Integer(i2));
			}
			catch(NumberFormatException e) {
			    return 0;
			}
		    }
		});
		this.bookmarks.clear();
		for(int i = 0; i < keys.length; i++) {
		    String parts[] = marks.get(keys[i], "").split(":", -1);
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

		this.trackerURL.setText(JXM.myUserNode().get(XMTRACKER_URL, "http://xmpcr.kfu.com/forums/tracker/"));
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
		this.handler.save();
		JXM.myUserNode().putBoolean(STARTUP_CHECK, this.startupCheckbox.isSelected());
		JXM.myUserNode().putInt(SLEEP_ACTION, this.sleepAction.getSelectedIndex());
		JXM.myUserNode().putInt(SEARCH_ACCURACY, this.searchAccuracy.getValue());

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
		this.deviceIsXmDirect.setEnabled(false);
		this.theTabs.setEnabledAt(TAB_FILTERS, true);
    }

    // This is called when the radio is turned off
    public void turnOff() {
		this.radioID.setText("");
		this.deviceMenu.setEnabled(true);
		this.deviceIsXmDirect.setEnabled(true);
		if (this.theTabs.getSelectedIndex() == TAB_FILTERS)
		    this.theTabs.setSelectedIndex(TAB_DEVICE);
		this.theTabs.setEnabledAt(TAB_FILTERS, false);
    }

    private void handleTrackerException(Exception e) {
		JOptionPane.showMessageDialog(this, e.getMessage(), "XM Tracker error", JOptionPane.ERROR_MESSAGE);
    }

    private final static String DEVICE_NAME_KEY = "DefaultDevice";
    private final static String DEVICE_IS_XMDIRECT_KEY = "DeviceIsXmDirect";
    private void refreshDeviceMenu() {
        this.deviceMenu.removeAllItems();
        this.deviceMenu.addItem("Pick device");

        String[] devices = RadioCommander.getPotentialDevices();
        for(int i = 0; i < devices.length; i++) {
            final String name = devices[i];
	    if (!deviceIsXmDirect.isSelected() && !PlatformFactory.ourPlatform().isDeviceValid(name))
		continue;
            this.deviceMenu.addItem(name);
        }
		// This is good: If there is no DEVICE_NAME_KEY value, we won't change the selection
		// If it's a nonexistent device, same thing. If there's only one device
		// and the platform says we can trust it, then that will be it. Otherwise, the user
		// will have to choose. If "" is a legal device name, well, that's just Unamerican.
		this.deviceMenu.setSelectedItem(JXM.myUserNode().get(DEVICE_NAME_KEY, ""));
		if (this.deviceMenu.getSelectedIndex() <= 0) {
		    if (PlatformFactory.ourPlatform().devicesAreFiltered() && this.deviceMenu.getItemCount() == 2) { // device and "Pick Menu"
			this.deviceMenu.setSelectedIndex(1);
		    } else {
			this.deviceMenu.setSelectedIndex(0);
		    }
		}
    }

    // -----------------------
    // Below are the properties we export to the world.

    public String getDevice() {
		String out = (String)this.deviceMenu.getSelectedItem();
		if (out == "" | out == "Pick device")
		    return null;
		return out;
    }
    public boolean isDeviceXmDirect() {
        return this.deviceIsXmDirect.isSelected();
    }
    // Call this after a successfull power-up
    public void saveDevice() {
		JXM.myUserNode().put(DEVICE_NAME_KEY, (String)this.deviceMenu.getSelectedItem());
		JXM.myUserNode().putBoolean(DEVICE_IS_XMDIRECT_KEY, this.deviceIsXmDirect.isSelected());
    }

    public int getSleepAction() { return this.sleepAction.getSelectedIndex(); }
    public String getTrackerURL() { return this.trackerURL.getText(); }
    public String getTrackerUser() { return this.trackerUser.getText(); }
    public String getTrackerPassword() { return new String(this.trackerPassword.getPassword()); }
    public double getSearchAccuracy() { return this.searchAccuracy.getValue()/1000f; }
    public boolean isTrackerEnabled() { return this.trackerEnabled.isSelected(); }
}
