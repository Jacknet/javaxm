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
import java.lang.*;
import java.util.*;

import com.kfu.xm.*;

public class MainWindow implements RadioEventHandler {

public static void main(String[] args) { new MainWindow(); }

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
    private ChannelTableModel channelTableModel;
    private JTable channelTable;
    private JMenu deviceMenu;
    private JCheckBox powerCheckBox;
    
    public MainWindow() {

    try {
	UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch(Exception e) {
	// Well, we tried
    }

        JFrame frame = new JFrame("JXM");
	frame.addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent e) {
		MainWindow.this.turnPowerOff();
		System.exit(0);
	    }
	});

	frame.getContentPane().setLayout(new BorderLayout());

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
	frame.getContentPane().add(jp, BorderLayout.PAGE_START);

        final JTable channelTable = new JTable();
	this.channelTable = channelTable;

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
		    ChannelInfo i = (ChannelInfo)MainWindow.this.channelList.get(new Integer(sidForRow(row)));
		    try {
			RadioCommander.theRadio().setChannel(i.getChannelNumber());
		    }
		    catch(RadioException ex) {
			// XXX - ignore for now
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

        frame.getContentPane().add(new JScrollPane(channelTable), BorderLayout.CENTER);

	JPanel bottom = new JPanel();
	bottom.setLayout(new FlowLayout());
	this.powerCheckBox = new JCheckBox("Power");
	this.powerCheckBox.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		if (MainWindow.this.powerCheckBox.isSelected())
		    MainWindow.this.turnPowerOn();
		else
		    MainWindow.this.turnPowerOff();
	    }
	});
	bottom.add(this.powerCheckBox);
	JMenuBar jmb = new JMenuBar();
	this.deviceMenu = new JMenu("Pick Device");
	this.refreshDeviceMenu();
	jmb.add(this.deviceMenu);
	bottom.add(jmb);

	frame.getContentPane().add(bottom, BorderLayout.PAGE_END);
	
        frame.pack();
        frame.setResizable(true);
        frame.setVisible(true);
    }

    private String deviceName = null;

    // XXX - There appears to be no need to do this other than at startup
    // javax.comm will not give back a different CommPortIdentifier enumeration
    // once it has probed the drivers. :-(
    private void refreshDeviceMenu() {
	while(this.deviceMenu.getItemCount() > 0)
	    this.deviceMenu.remove(0);
	String[] devices = RadioCommander.getPotentialDevices();
	for(int i = 0; i < devices.length; i++) {
	    final String name = devices[i];
	    JMenuItem jmi = new JMenuItem(name);
	    jmi.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    MainWindow.this.deviceName = name;
		    MainWindow.this.deviceMenu.setText(name);
		}
	    });
	    this.deviceMenu.add(jmi);
	}
    }

    // the RadioEventHandler interface
    public void notify(RadioCommander theRadio, final int type, final Object item) {
	// First, get back on the main thread
	SwingUtilities.invokeLater(new Runnable() {
	    public void run() {
	    switch(type) {
	    case RadioCommander.POWERED_ON:
		MainWindow.this.poweredUp();
		break;
	    case RadioCommander.POWERED_OFF:
		MainWindow.this.poweredDown();
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
    }

    private void turnPowerOff() {
	try {
	    RadioCommander.theRadio().turnOff();
	}
	catch(RadioException e) {
	    // ignore for now?
	}
    }

    private void turnPowerOn() {
	// Figure out which device was selected
	if (this.deviceName == null) {
	    // XXX - complain
	    this.powerCheckBox.setSelected(false);
	    return;
	}
	// Attempt to power up the radio
	try {
	    RadioCommander.theRadio().turnOn(this.deviceName);
	}
	catch(RadioException e) {
	    this.powerCheckBox.setSelected(false);
	    // XXX - complain
	    return;
	}
	this.poweredUp();
	RadioCommander.theRadio().registerEventHandler(this);
    }

    private void poweredUp() {
	this.channelList = new HashMap();
	this.deviceMenu.setEnabled(false);
    }

    private void poweredDown() {
	RadioCommander.theRadio().unregisterEventHandler(this);
	this.channelList = null;
	this.channelNumberLabel.setText("");
	this.channelNameLabel.setText("");
	this.channelGenreLabel.setText("");
	this.channelArtistLabel.setText("");
	this.channelTitleLabel.setText("");
	this.deviceMenu.setEnabled(true);
    }

    private void handleError(Exception e) {
    }
    private void muteChanged() {
    }
    private void channelChanged() {
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
	if (this.channelTable.getSelectedRow() != row) {
	    this.channelTable.setRowSelectionInterval(row, row);
	}
    }

    private void update(ChannelInfo i) {
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
	    // This is an update for the current channel - fix the labels.
	    this.channelNumberLabel.setText(Integer.toString(i.getChannelNumber()));
	    this.channelGenreLabel.setText(i.getChannelGenre());
	    this.channelNameLabel.setText(i.getChannelName());
	    this.channelArtistLabel.setText(i.getChannelArtist());
	    this.channelTitleLabel.setText(i.getChannelTitle());
	}
	// Next, we pass it around to the various things around here
	// to see if anybody cares.
    }
}
