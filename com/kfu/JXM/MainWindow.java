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
 
 */

package com.kfu.JXM;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.prefs.*;

// BrowserLauncher
import edu.stanford.ejalbert.*;

import com.kfu.xm.*;

public class MainWindow implements RadioEventHandler {

    // A popup menu for a given channel
    private class ChannelPopupMenu extends JPopupMenu {
	ChannelInfo channelInfo;

	public ChannelPopupMenu(int sid) {
/*
	    int sid = MainWindow.this.sidForChannel(chan);
	    if (sid < 0)
		return;
*/
	    this.channelInfo = (ChannelInfo)MainWindow.this.channelList.get(new Integer(sid));

	    JMenuItem jmi = new JMenuItem("Tune to channel");
	    jmi.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    try {
			RadioCommander.theRadio().setChannel(ChannelPopupMenu.this.channelInfo.getChannelNumber());
		    }
		    catch(RadioException ee) {
			MainWindow.this.handleError(ee);
		    }
		}
	    });
	    this.add(jmi);

	    jmi = new JMenuItem("Google Search for Artist");
	    jmi.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    MainWindow.this.googleSearch(ChannelPopupMenu.this.channelInfo.getChannelArtist(), null);
		}
	    });
	    this.add(jmi);

	    jmi = new JMenuItem("Google Search for Title");
	    jmi.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    MainWindow.this.googleSearch(null, ChannelPopupMenu.this.channelInfo.getChannelTitle());
		}
	    });
	    this.add(jmi);

	    jmi = new JMenuItem("Google Search for Artist and Title");
	    jmi.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    MainWindow.this.googleSearch(ChannelPopupMenu.this.channelInfo.getChannelArtist(), ChannelPopupMenu.this.channelInfo.getChannelTitle());
		}
	    });
	    this.add(jmi);

	}
    }

    private class ChannelTableModel extends AbstractTableModel {
        public int getRowCount() {
	    if (MainWindow.this.channelList == null)
		return 0;
	    return MainWindow.this.channelList.size();
        }
        public int getColumnCount() {
 		return 6;           
        }
        public Object getValueAt(int row, int column) {
	    int id = MainWindow.this.sidForRow(row);
	    ChannelInfo i = (ChannelInfo)MainWindow.this.channelList.get(new Integer(id));
	    switch(column) {
		case 0: return new Integer(i.getChannelNumber());
		case 1: return i.getChannelGenre();
		case 2: return i.getChannelName();
		case 3: return i.getChannelArtist();
		case 4: return i.getChannelTitle();
		case 5: return MainWindow.this.inUseForSID(id);
		default: throw new IllegalArgumentException("Which column?");
	    }
        }
    }

    private int sortField = 0;
    private boolean sortDirection = true;
    private Integer[] getSortedSidList() {
	Integer sids[] = (Integer[])this.channelList.keySet().toArray(new Integer[0]);
	Arrays.sort(sids, new Comparator() {
	    public int compare(Object o1, Object o2) {
		Integer sid1 = (Integer)o1;
		Integer sid2 = (Integer)o2;
		ChannelInfo ci1 = (ChannelInfo)MainWindow.this.channelList.get(sid1);
		ChannelInfo ci2 = (ChannelInfo)MainWindow.this.channelList.get(sid2);
		int out = 0;
		switch(MainWindow.this.sortField) {
			case 0:	out = new Integer(ci1.getChannelNumber()).compareTo(new Integer(ci2.getChannelNumber()));
				break;
			case 1: out = ci1.getChannelGenre().compareTo(ci2.getChannelGenre());
				break;
			case 2: out = ci1.getChannelName().compareTo(ci2.getChannelName());
				break;
			case 3: out = ci1.getChannelArtist().compareTo(ci2.getChannelArtist());
				break;
			case 4: out = ci1.getChannelTitle().compareTo(ci2.getChannelTitle());
				break;
		}
		out *= MainWindow.this.sortDirection?1:-1;
		return out;
	    }
	});
	return sids;
    }

    private int rowForSID(int sid) {
	Integer sids[] = this.getSortedSidList();
	for(int i = 0; i < sids.length; i++)
	    if (sids[i].intValue() == sid)
		return i;
	return -1;
    }

    private int sidForChannel(int chan) {
	Iterator i = this.channelList.values().iterator();
	while(i.hasNext()) {
	    ChannelInfo info = (ChannelInfo)(i.next());
	    if (info.getChannelNumber() == chan)
		return info.getServiceID();
	}
	return -1;
    }

    private int sidForRow(int row) {
	return (this.getSortedSidList()[row]).intValue();
    }

    private String inUseForSID(int sid) {
        return "";
    }

    private JLabel channelNumberLabel;
    private JLabel channelNameLabel;
    private JLabel channelGenreLabel;
    private JLabel channelArtistLabel;
    private JLabel channelTitleLabel;
    private JLabel channelLogo;
    private ChannelTableModel channelTableModel;
    private JTable channelTable;
    private JCheckBox powerCheckBox;
    private JCheckBox muteButton;
    private JCheckBox smartMuteButton;
    private JFrame myFrame;
    private JProgressBar satelliteMeter;
    private JProgressBar terrestrialMeter;
    private JButton itmsButton;
    private PreferencesDialog preferences;
    private JSlider ratingSlider;
    private JComboBox favoriteMenu;
    private JCheckBox favoriteCheckbox;
   
    public void quit() { 
	if (RadioCommander.theRadio().isOn())
	    MainWindow.this.turnPowerOff();
	System.exit(0);
    }
    public void prefs() {
	this.preferences.show();
    }

    public MainWindow() {

    try {
	UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch(Exception e) {
	// Well, we tried
    }

        this.myFrame = new JFrame("JXM");
	this.myFrame.setJMenuBar(new JMenuBar());
	this.myFrame.addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent e) {
		MainWindow.this.quit();
	    }
	});
	this.preferences = new PreferencesDialog(this.myFrame);

	// -----
	// If on a mac, don't do this - use the MRJ stuff instead
	JMenu jm = new JMenu("File");
	JMenuItem jmi = new JMenuItem("Preferences...");
	jmi.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		MainWindow.this.prefs();
	    }
	});
	jm.add(jmi);
	jmi = new JMenuItem("Quit");
	jmi.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		MainWindow.this.quit();
	    }
	});
	jm.add(jmi);
	this.myFrame.getJMenuBar().add(jm);
	// -----

	this.myFrame.getContentPane().setLayout(new BorderLayout());

	JPanel top = new JPanel();
	top.setLayout(new BoxLayout(top, BoxLayout.PAGE_AXIS));
	JPanel toptop = new JPanel();
	toptop.setLayout(new BoxLayout(toptop, BoxLayout.LINE_AXIS));
	toptop.add(Box.createHorizontalStrut(20));

	JPanel pictureFrame = new JPanel();
	pictureFrame.setBorder(BorderFactory.createLineBorder(Color.BLACK));
	this.channelLogo = new JLabel();
	this.channelLogo.setPreferredSize(new Dimension(150, 100));
	this.setChannelLogo(-1);
	this.channelLogo.addMouseListener(new MouseAdapter() {
	    boolean didPopup = false;

	    public void mousePressed(MouseEvent e) { this.maybePopup(e); }
	    public void mouseReleased(MouseEvent e) { this.maybePopup(e); }
	    public void maybePopup(MouseEvent e) {
		if (!RadioCommander.theRadio().isOn())
		    return;
		if (e.isPopupTrigger()) {
		    didPopup = true;
		    int sid = MainWindow.this.sidForChannel(RadioCommander.theRadio().getChannel());
		    if (sid < 0)
			return;
		    JPopupMenu popup = new ChannelPopupMenu(sid);
		    popup.show(e.getComponent(), e.getX(), e.getY());
		}
	    }
	    public void mouseClicked(MouseEvent e) {
		if (didPopup) {
		    didPopup = false;
		    return;
		}
		if (!RadioCommander.theRadio().isOn())
		    return;
		MainWindow.this.surfToChannel(RadioCommander.theRadio().getChannel());
	    }
	});
	pictureFrame.add(this.channelLogo);
	toptop.add(pictureFrame);
	toptop.add(Box.createHorizontalStrut(5));

	// First, the "now playing" panel
	JPanel jp = new JPanel();
	jp.setBorder(BorderFactory.createTitledBorder(
	    BorderFactory.createLineBorder(Color.BLACK), "Now Playing"));
	jp.setLayout(new GridBagLayout());
	GridBagConstraints gbc = new GridBagConstraints();
	gbc.fill = GridBagConstraints.BOTH;

	this.channelNumberLabel = new JLabel();
	this.channelNumberLabel.setHorizontalAlignment(SwingConstants.CENTER);
	this.channelNumberLabel.setFont(new Font(null, Font.BOLD, 18));
	gbc.weightx = 0.25;
	gbc.gridx = 0;
	gbc.gridy = 0;
	jp.add(this.channelNumberLabel, gbc);
	this.channelNameLabel = new JLabel();
	this.channelNameLabel.setHorizontalAlignment(SwingConstants.CENTER);
	this.channelNameLabel.setFont(new Font(null, Font.PLAIN, 14));
	gbc.gridy = 1;
	jp.add(this.channelNameLabel, gbc);
	this.channelGenreLabel = new JLabel();
	this.channelGenreLabel.setHorizontalAlignment(SwingConstants.CENTER);
	this.channelGenreLabel.setFont(new Font(null, Font.PLAIN, 12));
	gbc.gridy = 2;
	jp.add(this.channelGenreLabel, gbc);
	this.channelArtistLabel = new JLabel();
	this.channelArtistLabel.setHorizontalAlignment(SwingConstants.CENTER);
	this.channelArtistLabel.setFont(new Font(null, Font.BOLD, 20));
	gbc.gridx = 1;
	gbc.weightx = 0.75;
	gbc.gridwidth = 2;
	gbc.gridy = 0;
	jp.add(this.channelArtistLabel, gbc);
	this.channelTitleLabel = new JLabel();
	this.channelTitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
	this.channelTitleLabel.setFont(new Font(null, Font.BOLD, 20));
	gbc.gridy = 1;
	jp.add(this.channelTitleLabel, gbc);
	jp.setMinimumSize(new Dimension(0, 75));
	jp.setPreferredSize(jp.getMinimumSize());
	toptop.add(jp);
	JPanel buttons = new JPanel();
	buttons.setLayout(new BoxLayout(buttons, BoxLayout.PAGE_AXIS));
	this.itmsButton = new JButton("iTunes Music Store");
	this.itmsButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		MainWindow.this.itmsButtonClicked();
	    }
	});
	this.itmsButton.setEnabled(false);
	buttons.add(this.itmsButton);

	toptop.add(buttons);
	toptop.add(Box.createHorizontalStrut(20));
	top.add(toptop);
	top.add(Box.createVerticalStrut(10));
	JPanel stripe = new JPanel();
	stripe.setLayout(new GridBagLayout());
	gbc = new GridBagConstraints();

	JPanel favorites = new JPanel();
	this.favoriteMenu = new JComboBox();
	this.favoriteMenu.addItem("Favorites");
	this.favoriteMenu.setSelectedIndex(0);
	this.favoriteMenu.setEnabled(false);
	this.favoriteMenu.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() != "comboBoxChanged")
		     return;
		if (MainWindow.this.favoriteMenu.getItemCount() == 0)
		    return; // XXX - this looks like a bug in JComboBox. Why is clearing it a "Change"?
		Object o = MainWindow.this.favoriteMenu.getSelectedItem();
		if (!(o instanceof Integer))
		    return; // They must have selected "Favorites"
		Integer sid = (Integer)o;
		MainWindow.this.favoriteMenu.setSelectedIndex(0);
		ChannelInfo i = (ChannelInfo)MainWindow.this.channelList.get(sid);
		if (i == null)
		    return;
		try {
		    RadioCommander.theRadio().setChannel(i.getChannelNumber());
		}
		catch(RadioException ee) {
		    MainWindow.this.handleError(ee);
		}
	    }
	});
	this.favoriteMenu.setRenderer(new DefaultListCellRenderer() {
	    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		if (value instanceof Integer) {
		    Integer sid = (Integer)value;
		    ChannelInfo info = (ChannelInfo)MainWindow.this.channelList.get(sid);
		    if (info == null) {
			value = "Service ID " + sid;
		    } else {
			value = Integer.toString(info.getChannelNumber()) + " - " + info.getChannelName();
		    }
		}
        	return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
	    }
	});
	favorites.add(this.favoriteMenu);
	this.favoriteCheckbox = new JCheckBox();
	this.favoriteCheckbox.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		Integer sid;
		try {
		    sid = new Integer(RadioCommander.theRadio().getChannelInfo().getServiceID());
		}
		catch(RadioException ee) {
		    MainWindow.this.handleError(ee);
		    return;
		}
		boolean becoming = MainWindow.this.favoriteCheckbox.isSelected();
		if (becoming) {
		    MainWindow.this.favoriteList.add(sid);
		} else {
		    MainWindow.this.favoriteList.remove(sid);
		}
		MainWindow.this.saveFavorites();
		MainWindow.this.rebuildFavoritesMenu();
	    }
	});
	this.favoriteCheckbox.setEnabled(false);
	favorites.add(this.favoriteCheckbox);
	gbc.weightx = 0;
	gbc.anchor = GridBagConstraints.LINE_START;
	stripe.add(favorites, gbc);

	JPanel rating = new JPanel();
	rating.setLayout(new GridBagLayout());
	GridBagConstraints gbc1 = new GridBagConstraints();
	this.ratingSlider = new JSlider(-5, 5);
	this.ratingSlider.setMajorTickSpacing(5);
	this.ratingSlider.setMinorTickSpacing(1);
	this.ratingSlider.setSnapToTicks(true);
	this.ratingSlider.setPaintTicks(true);
	this.ratingSlider.setEnabled(false);
	gbc1.weightx = 1;
	gbc1.gridwidth = 3;
	gbc1.fill = GridBagConstraints.HORIZONTAL;
	rating.add(this.ratingSlider, gbc1);
	JLabel jl = new JLabel("-");
	gbc1.gridwidth = 1;
	gbc1.fill = GridBagConstraints.NONE;
	gbc1.anchor = GridBagConstraints.LINE_START;
	gbc1.gridy = 1;
	gbc1.gridx = 0;
	gbc1.weightx = 0;
	rating.add(jl, gbc1);
	jl = new JLabel("Rate Song");
	gbc1.weightx = 1;
	gbc1.anchor = GridBagConstraints.CENTER;
	gbc.gridx = 2;
	rating.add(jl, gbc1);
	jl = new JLabel("+");
	gbc1.weightx = 0;
	gbc1.gridx = 2;
	gbc1.anchor = GridBagConstraints.LINE_END;
	rating.add(jl, gbc1);

	gbc.weightx = 1;
	gbc.gridx = 1;
	gbc.fill = GridBagConstraints.HORIZONTAL;
	stripe.add(rating, gbc);
	
	JPanel extra = new JPanel();
	// XXX - what goes here?
	gbc.gridx = 2;
	gbc.weightx = 0;
	gbc.fill = GridBagConstraints.NONE;
	extra.setPreferredSize(favorites.getPreferredSize());
	stripe.add(extra, gbc);

	top.add(stripe);
	
	this.myFrame.getContentPane().add(top, BorderLayout.PAGE_START);
	//this.myFrame.getContentPane().add(this.channelLogo, BorderLayout.PAGE_START);

	jp = new JPanel();
	jp.setLayout(new GridBagLayout());
	gbc = new GridBagConstraints();
	gbc.weightx = gbc.weighty = 1;
	gbc.insets = new Insets(20, 20, 20, 20);
	gbc.fill = GridBagConstraints.BOTH;
	
	this.channelTable = new JTable();
	this.channelTable.addMouseListener(new MouseAdapter() {
	    public void mousePressed(MouseEvent e) { this.maybePopup(e); }
	    public void mouseReleased(MouseEvent e) { this.maybePopup(e); }
	    private void maybePopup(MouseEvent e) {
		if (!RadioCommander.theRadio().isOn())
		    return;
		if (!e.isPopupTrigger())
		    return;
		int row = MainWindow.this.channelTable.rowAtPoint(e.getPoint());
		if (row < 0)
		    return;
		int sid = MainWindow.this.sidForRow(row);
		if (sid < 0)
		    return;
		JPopupMenu jpm = new ChannelPopupMenu(sid);
		jpm.show(e.getComponent(), e.getX(), e.getY());
	    }
	});

	this.channelTableModel = new ChannelTableModel();
	channelTable.setModel(this.channelTableModel);

	TableColumnModel tcm = new DefaultTableColumnModel();
	TableColumn tc;
	tc = new TableColumn(0, 30, null, null);
	DefaultTableCellRenderer centered = new DefaultTableCellRenderer();
	centered.setHorizontalAlignment(SwingConstants.CENTER);
	tc.setCellRenderer(centered);
	tc.setHeaderValue("Number");
	tcm.addColumn(tc);
	tc = new TableColumn(1, 60, null, null);
	tc.setHeaderValue("Genre");
	tcm.addColumn(tc);
	tc = new TableColumn(2, 80, null, null);
	tc.setHeaderValue("Name");
	tcm.addColumn(tc);
	tc = new TableColumn(3, 120, null, null);
	tc.setHeaderValue("Artist");
	tcm.addColumn(tc);
	tc = new TableColumn(4, 120, null, null);
	tc.setHeaderValue("Title");
	tcm.addColumn(tc);
	tc = new TableColumn(5, 30, null, null);
	tc.setCellRenderer(centered);
	tc.setHeaderValue("% In Use");
	tcm.addColumn(tc);
	channelTable.setColumnModel(tcm);

	channelTable.setColumnSelectionAllowed(false);
	channelTable.setRowSelectionAllowed(true);
	channelTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	channelTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
	    public void valueChanged(ListSelectionEvent e) {
		MainWindow.this.selectionInProgress = e.getValueIsAdjusting();
		if (MainWindow.this.selectionInProgress)
		    return;
		ListSelectionModel lsm = (ListSelectionModel)e.getSource();
		if (lsm.isSelectionEmpty()) {
		    // XXX can never happen
		} else {
		    int row = lsm.getMinSelectionIndex();
		    if (row >= MainWindow.this.channelList.size())
			return;
		    ChannelInfo i = (ChannelInfo)MainWindow.this.channelList.get(new Integer(sidForRow(row)));
		    try {
			// The problem here is that we will get called even when we do the selecting
			// ourselves (as in this.selectCurrentChannel(); ). This means we have to
			// efficiently fall through if there's nothing to be done (as will be the case)
			// when we select the currently selected row. Argh!
			if (RadioCommander.theRadio().getChannel() == i.getChannelNumber())
			    return;
			RadioCommander.theRadio().setChannel(i.getChannelNumber());
		    }
		    catch(RadioException ex) {
			MainWindow.this.handleError(ex);
		    }
		}
	    }
	});

	channelTable.getTableHeader().addMouseListener(new MouseAdapter() {
	    public void mouseClicked(MouseEvent e) {
		TableColumnModel columnModel = channelTable.getColumnModel();
                int viewColumn = columnModel.getColumnIndexAtX(e.getX());
                int column = channelTable.convertColumnIndexToModel(viewColumn);
		if (column == MainWindow.this.sortField) {
		    MainWindow.this.sortDirection = !MainWindow.this.sortDirection;
		} else {
		    MainWindow.this.sortField = column;
		    MainWindow.this.sortDirection = true;
		}
	    }
	});

	jp.add(new JScrollPane(channelTable), gbc);
        this.myFrame.getContentPane().add(jp, BorderLayout.CENTER);

	JPanel bottom = new JPanel();
	bottom.setLayout(new GridBagLayout());
	gbc = new GridBagConstraints();

	this.muteButton = new JCheckBox("Mute");
	this.muteButton.setEnabled(false);
	this.muteButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		MainWindow.this.muteClicked();
	    }
	});
	gbc.gridx = gbc.gridy = 0;
	gbc.weightx = 1;
	gbc.anchor = GridBagConstraints.LINE_START;
	gbc.fill = GridBagConstraints.HORIZONTAL;
	gbc.insets = new Insets(0, 20, 0, 0);
	bottom.add(this.muteButton, gbc);

	this.smartMuteButton = new JCheckBox("Smart Mute");
	this.smartMuteButton.setEnabled(false);
	this.smartMuteButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		MainWindow.this.smartMuteClicked();
	    }
	});
	gbc.gridy = 1;
	gbc.insets = new Insets(0, 20, 20, 0);
	bottom.add(this.smartMuteButton, gbc);

	this.powerCheckBox = new JCheckBox("Power");
	this.powerCheckBox.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		if (MainWindow.this.powerCheckBox.isSelected())
		    MainWindow.this.turnPowerOn();
		else
		    MainWindow.this.turnPowerOff();
	    }
	});

	gbc.insets = new Insets(0, 0, 20, 10);
	gbc.gridx = 1;
	gbc.gridy = 0;
	gbc.gridheight = 2;
	gbc.weightx = 0;
	gbc.anchor = GridBagConstraints.LINE_END;
	bottom.add(this.powerCheckBox, gbc);

	jl = new JLabel("Satellite: ");
	jl.setHorizontalAlignment(SwingConstants.TRAILING);
	gbc.gridx = 2;
	gbc.gridy = 0;
	gbc.weightx = 1;
	gbc.gridheight = 1;
	gbc.insets = new Insets(0, 0, 0, 0);
	gbc.anchor = GridBagConstraints.LINE_END;
	bottom.add(jl, gbc);
	jl = new JLabel("Terrestrial: ");
	jl.setHorizontalAlignment(SwingConstants.TRAILING);
	gbc.gridy = 1;
	gbc.insets = new Insets(0, 0, 20, 0);
	bottom.add(jl, gbc);

	this.satelliteMeter = new JProgressBar(0, 100);
	gbc.gridx = 3;
	gbc.gridy = 0;
	gbc.insets = new Insets(0, 0, 0, 20);
	gbc.anchor = GridBagConstraints.LINE_START;
	bottom.add(this.satelliteMeter, gbc);
	this.terrestrialMeter = new JProgressBar(0, 100);
	gbc.gridy = 1;
	gbc.insets = new Insets(0, 0, 20, 20);
	bottom.add(this.terrestrialMeter, gbc);

	this.myFrame.getContentPane().add(bottom, BorderLayout.PAGE_END);
	
        this.myFrame.pack();
        this.myFrame.setResizable(true);
        this.myFrame.setVisible(true);

	java.util.Timer t = new java.util.Timer();
	t.schedule(new TimerTask() {
	    public void run() {
		if (!RadioCommander.theRadio().isOn())
		    return;
		try {
		    double[] out = RadioCommander.theRadio().getSignalStrength();
		    MainWindow.this.satelliteMeter.setValue((int)out[RadioCommander.SIGNAL_STRENGTH_SAT]);
		    MainWindow.this.terrestrialMeter.setValue((int)out[RadioCommander.SIGNAL_STRENGTH_TER]);
		}
		catch(RadioException e) {
		    // sigh. If we get an exception while powering down, then ignore it.
		    // XXX this is probably a sign we should do more locking
		    if (RadioCommander.theRadio().isOn())
		        MainWindow.this.handleError(e);
		}
	    }
	}, 0, 1000);

	String url = this.myUserNode().get(XMTRACKER_URL, null);
	String user = this.myUserNode().get(XMTRACKER_USER, null);
	String pass = this.myUserNode().get(XMTRACKER_PASS, null);

	if (url != null && user != null && pass != null) {
	    try {
		XMTracker.theTracker().setBaseURL(url);
		XMTracker.theTracker().setCredentials(user, pass);
	    }
	    catch(TrackerException e) {
		this.myUserNode().put(XMTRACKER_USER, null);
		this.myUserNode().put(XMTRACKER_PASS, null);
		this.handleTrackerError(e);
	    }
	}

	this.loadFavorites();

	// We have a device saved... Try and power up
	String deviceName = this.preferences.getDevice();
	if (deviceName != null)
	    this.turnPowerOn();
    }

    private void itmsButtonClicked() {
	String u = "itms://phobos.apple.com/WebObjects/MZSearch.woa/wa/com.apple.jingle.search.DirectAction/advancedSearchResults?";
	ChannelInfo i;
	try {
	    i = RadioCommander.theRadio().getChannelInfo();
	}
	catch(RadioException e) {
	    this.handleError(e);
	    return;
	}
	try {
	    String artist = URLEncoder.encode(i.getChannelArtist(), "US-ASCII");
	    String title = URLEncoder.encode(i.getChannelTitle(), "US-ASCII");
	    if (artist != "")
		u += "artistTerm=" + artist;
	    if (artist != "" && title != "");
		u += "&";
	    if (title != "")
		u += "songTerm=" + title;
	}
	catch(UnsupportedEncodingException e) {
	    // Oh, whatever!
System.err.println(e.getMessage());
e.printStackTrace();
	    return;
	}
	this.openURL(u.toString());
    }

    private void surfToChannel(int chan) {
	String u = "http://www.xmradio.com/programming/channel_page.jsp?ch=" + chan;
	this.openURL(u);
    }

    private void googleSearch(String artist, String title) {
	StringBuffer sb = new StringBuffer("http://www.google.com/search?q=");
	if (artist != null) {
	    try {
		sb.append(URLEncoder.encode("\"" + artist + "\"", "US-ASCII"));
	    }
	    catch(UnsupportedEncodingException e) {
		// Mhmm.
	    }
	}
	if (artist != null && title != null)
	    sb.append('+');
	if (title != null) {
	    try {
		sb.append(URLEncoder.encode("\"" + title + "\"", "US-ASCII"));
	    }
	    catch(UnsupportedEncodingException e) {
		// Mhmm.
	    }
	}
	this.openURL(sb.toString());
    }

    private void openURL(String u) {
	try {
	    BrowserLauncher.openURL(u);
	}
	catch(IOException e) {
System.err.println(e.getMessage());
e.printStackTrace();
	    // XXX what do we do about this?
	}
    }

    private void setChannelLogo(int chan) {
	URL logoUrl = this.getClass().getResource("/logos/" + chan + ".gif");
	if (logoUrl == null)
	    logoUrl = this.getClass().getResource("/logos/default.gif");
	Icon logo = new ImageIcon(logoUrl);
	this.channelLogo.setIcon(logo);
    }

    private void muteClicked() {
	this.smartMuteButton.setSelected(false);
	boolean muteState;
	try {
	    muteState = !RadioCommander.theRadio().isMuted();
	    RadioCommander.theRadio().setMute(muteState);
	}
	catch(RadioException e) {
	    this.handleError(e);
	    return;
	}
	this.smartMuteInfo = null;
	this.muteButton.setSelected(muteState);
    }

    ChannelInfo smartMuteInfo = null;
    private void smartMuteClicked() {
	this.muteButton.setSelected(false);
	boolean muteState;
	ChannelInfo i = null;
	try {
	    muteState = !RadioCommander.theRadio().isMuted();
	    RadioCommander.theRadio().setMute(muteState);
	    if (muteState)
		this.smartMuteInfo = RadioCommander.theRadio().getChannelInfo();
	    else
		this.smartMuteInfo = null;
	}
	catch(RadioException e) {
	    this.handleError(e);
	    return;
	}
	this.smartMuteButton.setSelected(muteState);
    }

    // the RadioEventHandler interface
    public void notify(RadioCommander theRadio, final int type, final Object item) {
	// This must be handled immediately to make sure we close the Tracker when quitting.
	if (type == RadioCommander.POWERED_OFF) {
	    this.poweredDown();
	    return;
	}
	//try {
	// First, get back on the main thread
	    SwingUtilities.invokeLater(new Runnable() {
		public void run() {
		    switch(type) {
			case RadioCommander.POWERED_ON:
				MainWindow.this.poweredUp();
				break;
			case RadioCommander.CHANNEL_DELETE:
				MainWindow.this.deleteChannel(((Integer)item).intValue());
				break;
			case RadioCommander.CHANNEL_INFO_UPDATE:
				MainWindow.this.update((ChannelInfo) item);
				break;
			case RadioCommander.EXCEPTION:
				MainWindow.this.handleError((Exception)item);
				break;
			case RadioCommander.CHANNEL_CHANGED:
				MainWindow.this.channelChanged();
				break;
			case RadioCommander.MUTE_CHANGED:
				MainWindow.this.muteChanged();
				break;
			case RadioCommander.ACTIVATION_CHANGED:
			case RadioCommander.SONG_TIME_UPDATE:
				// XXX ignore for now
				break;
			default:
				throw new IllegalArgumentException("Which kind of notification?");
		    }
		}
	    });
	//}
	//catch(Exception e) {
//System.err.println(e.getMessage());
//e.printStackTrace();
	//}
    }

    private void turnPowerOff() {
	try {
	    RadioCommander.theRadio().turnOff();
	}
	catch(RadioException e) {
	    this.handleError(e);
	}
    }

    private void turnPowerOn() {
	// Figure out which device was selected
	String deviceName = this.preferences.getDevice();
	if (deviceName == null) {
	    this.powerCheckBox.setSelected(false);
	    JOptionPane.showMessageDialog(this.myFrame, "Please pick a device before powering up.", "No device selected", JOptionPane.ERROR_MESSAGE);
	    return;
	}
	// Attempt to power up the radio
	try {
	    RadioCommander.theRadio().turnOn(deviceName);
	}
	catch(RadioException e) {
	    this.powerCheckBox.setSelected(false);
	    this.handleError(e);
	    return;
	}
	this.poweredUp();
	RadioCommander.theRadio().registerEventHandler(this);
    }

    private Preferences myUserNode() {
	return Preferences.userNodeForPackage(this.getClass());
    }

    private final static String FAVORITE_LIST = "FavoriteChannels";
    private final static String XMTRACKER_URL = "TrackerURL";
    private final static String XMTRACKER_USER = "TrackerUser";
    private final static String XMTRACKER_PASS = "TrackerPassword";

    private void poweredUp() {
	this.channelList = new HashMap();
	this.muteButton.setEnabled(true);
	this.smartMuteButton.setEnabled(true);
	this.itmsButton.setEnabled(true);
	this.muteButton.setSelected(false);
	this.smartMuteButton.setSelected(false);
	this.smartMuteInfo = null;
	this.powerCheckBox.setSelected(true);
	this.rebuildFavoritesMenu();
	this.favoriteCheckbox.setEnabled(true);
	this.preferences.saveDevice();
	this.channelChanged(); // We need to fake the first one
	try {
	    String rid = RadioCommander.theRadio().getRadioID();
	    this.preferences.turnOn(rid);
	}
	catch(RadioException e) {
	    this.handleError(e);
	}
    }

    private void poweredDown() {
	RadioCommander.theRadio().unregisterEventHandler(this);
	/*new Thread() {
	    public void run() { */
		try {
		    XMTracker.theTracker().turnOff();
		}
		catch(TrackerException e) {
		    MainWindow.this.handleTrackerError(e);
		}
	/*    }
	}.start(); */
	this.channelList = null;
	this.channelTableModel.fireTableDataChanged();
	SwingUtilities.invokeLater(new Runnable() {
	    public void run() {
		MainWindow.this.channelNumberLabel.setText("");
		MainWindow.this.channelNameLabel.setText("");
		MainWindow.this.channelGenreLabel.setText("");
		MainWindow.this.channelArtistLabel.setText("");
		MainWindow.this.channelTitleLabel.setText("");
		MainWindow.this.powerCheckBox.setSelected(false);
		MainWindow.this.muteButton.setEnabled(false);
		MainWindow.this.smartMuteButton.setEnabled(false);
		MainWindow.this.itmsButton.setEnabled(false);
		MainWindow.this.muteButton.setSelected(false);
		MainWindow.this.smartMuteButton.setSelected(false);
		MainWindow.this.satelliteMeter.setValue(0);
		MainWindow.this.terrestrialMeter.setValue(0);
		MainWindow.this.setChannelLogo(-1);
		MainWindow.this.favoriteMenu.setEnabled(false);
		MainWindow.this.favoriteCheckbox.setEnabled(false);
	        MainWindow.this.preferences.turnOff();
	    }
	});
    }

    private void handleTrackerError(final Exception e) {
	XMTracker.theTracker().Disable();
	SwingUtilities.invokeLater(new Runnable() {
	    public void run() {
		JOptionPane.showMessageDialog(MainWindow.this.myFrame, e.getMessage(),
		    "XM Tracker error", JOptionPane.ERROR_MESSAGE);
	    }
	});
    }

    private void handleError(final Exception e) {
	System.err.println(e.getMessage());
	e.printStackTrace();
	RadioCommander.theRadio().unregisterEventHandler(this);
	RadioCommander.theRadio().Dispose();
	SwingUtilities.invokeLater(new Runnable() {
	    public void run() {
		MainWindow.this.poweredDown();
		try {
		   JOptionPane.showMessageDialog(MainWindow.this.myFrame, e.getMessage(),
			"Error communicating with radio", JOptionPane.ERROR_MESSAGE);
		}
		catch(HeadlessException e) {
		    System.err.println("Error communicating with radio: " + e.getMessage());
		}
	    }
	});
    }
    private void muteChanged() {
    }
    private void channelChanged() {
	int channel = RadioCommander.theRadio().getChannel();

	this.setChannelLogo(channel);

	Integer sid = new Integer(this.sidForChannel(channel));
	this.favoriteCheckbox.setSelected(this.favoriteList.contains(sid));
    }

    HashMap channelList;

    private void deleteChannel(int sid) {
	this.channelList.remove(new Integer(sid));
    }

    private boolean selectionInProgress = false;
    private void selectCurrentChannel() {
	if (this.selectionInProgress)
	    return;
	int sid = RadioCommander.theRadio().getServiceID();
	int row = this.rowForSID(sid);
	if (row < 0)
	    return;
	this.channelTable.addRowSelectionInterval(row, row);
    }

    private void rebuildFavoritesMenu() {
	this.favoriteMenu.removeAllItems();
	this.favoriteMenu.addItem("Favorites");
	this.favoriteMenu.setSelectedIndex(0);
	if (this.favoriteList.isEmpty()) {
	    this.favoriteMenu.setEnabled(false);
	} else {
	    this.favoriteMenu.setEnabled(true);
	    Iterator i = this.favoriteList.iterator();
	    while(i.hasNext()) {
		Integer sid = (Integer)i.next();
		this.favoriteMenu.addItem(sid);
	    }
	}
    }

    HashSet favoriteList = new HashSet();

    private void loadFavorites() {
	this.favoriteList.clear();
	byte[] list = this.myUserNode().getByteArray(FAVORITE_LIST, new byte[0]);
	for(int i = 0; i < list.length; i++)
	    this.favoriteList.add(new Integer(list[i] & 0xff));
    }
    private void saveFavorites() {
	byte[] list = new byte[this.favoriteList.size()];
	int n = 0;
	Iterator i = this.favoriteList.iterator();
	while(i.hasNext())
	    list[n++] = (byte)((Integer)i.next()).intValue();
	this.myUserNode().putByteArray(FAVORITE_LIST, list);
    }

    private void update(final ChannelInfo i) {
	// We got an update. First, we file it, firing table update events
	// while we're at it.
	int oldSize = this.channelList.size();
	this.channelList.put(new Integer(i.getServiceID()), i);
	int row = this.rowForSID(i.getServiceID());
	if (this.channelList.size() != oldSize) {
	    this.channelTableModel.fireTableRowsInserted(row, row);
	} else {
	    this.channelTableModel.fireTableRowsUpdated(row, row);
	}

	this.selectCurrentChannel();

	if (RadioCommander.theRadio().getChannel() == i.getChannelNumber()) {
	    // update the favorite checkbox
	    this.favoriteCheckbox.setSelected(this.favoriteList.contains(new Integer(i.getServiceID())));
	    // This is an update for the current channel - fix the labels.
	    this.channelNumberLabel.setText(Integer.toString(i.getChannelNumber()));
	    this.channelGenreLabel.setText(i.getChannelGenre());
	    this.channelNameLabel.setText(i.getChannelName());
	    this.channelArtistLabel.setText(i.getChannelArtist());
	    this.channelTitleLabel.setText(i.getChannelTitle());
	    if (this.smartMuteInfo != null && !i.equals(this.smartMuteInfo)) {
		this.smartMuteClicked(); // Quickie hack! Since we're muted, this will unmute.
	    }
	    new Thread() {
		public void run() {
	    	    try {
	    		XMTracker.theTracker().update(i);
		    }
		    catch(TrackerException e) {
			MainWindow.this.handleTrackerError(e);
		    }
		}
	    }.start();
	}
	// Next, we pass it around to the various things around here
	// to see if anybody cares.
    }
}
