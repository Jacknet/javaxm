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
import java.security.*;
import java.util.*;
import java.util.prefs.*;
import java.util.regex.*;

import com.kfu.xm.*;

public class MainWindow implements RadioEventHandler, IPlatformCallbackHandler, IPreferenceCallbackHandler {

    // A popup menu for a given channel
    private class ChannelPopupMenu extends JPopupMenu {
	ChannelInfo channelInfo;

	public ChannelPopupMenu(int sid) {
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
	    class BookmarkMenu extends JMenu {
		public BookmarkMenu() {
		    super("Bookmarks");
		    for(int i = 0; i < MainWindow.this.bookmarks.length; i++) {
			final Bookmark b = MainWindow.this.bookmarks[i];
			JMenuItem jmi = new JMenuItem(b.getName());
			jmi.addActionListener(new ActionListener() {
			    public void actionPerformed(ActionEvent e) {
				MainWindow.this.bookmarkSurf(b, ChannelPopupMenu.this.channelInfo);
			    }
			});
			this.add(jmi);
		    }
		}
	    };
	    this.add(new BookmarkMenu());
	}
    }

    private void bookmarkSurf(Bookmark b, ChannelInfo i) {
	try {
	    b.surf(i);
	}
	catch(IOException e) {
	    JOptionPane.showMessageDialog(MainWindow.this.myFrame, e.getMessage(), "Error opening URL", JOptionPane.ERROR_MESSAGE);
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
			case 5: Integer ticks1 = (Integer)MainWindow.this.tickList.get(new Integer(ci1.getServiceID()));
				Integer ticks2 = (Integer)MainWindow.this.tickList.get(new Integer(ci2.getServiceID()));
				if (ticks1 == null)
					ticks1 = new Integer(0);
				if (ticks2 == null)
					ticks2 = new Integer(0);
				out = ticks1.compareTo(ticks2);
				break;
				
		}
		// Stupid Java. If it's a tie, then sub-sort on channel number, always ascending.
		if (out == 0) {
		    return new Integer(ci1.getChannelNumber()).compareTo(new Integer(ci2.getChannelNumber()));
		}
		out *= MainWindow.this.sortDirection?1:-1;
		return out;
	    }
	});
	return sids;
    }

    private HashMap tickList = new HashMap();

    // the IPreferencesCallbackInterface
    public void clearChannelStats() {
	this.tickList.clear();
	Preferences node = this.myUserNode().node(TICK_NODE);
	try {
	    node.clear();
	}
	catch(BackingStoreException e) {
	}
    }

    public void rebuildBookmarksMenu(Bookmark[] list) {
	this.bookmarks = list;
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

    private int totalTicks() {
	int total = 0;
	Iterator i = this.tickList.values().iterator();
	while(i.hasNext())
	    total += ((Integer)i.next()).intValue();
	return total;
    }

    private String inUseForSID(int sid) {
	Integer ticks = (Integer)this.tickList.get(new Integer(sid));
	if (ticks == null)
	    ticks = new Integer(0);
	int percent = (int)(1000f * ((float)ticks.intValue())/((float)this.totalTicks()));
	if (percent < 1)
	    return "";
	StringBuffer sb = new StringBuffer();
	sb.append(percent / 10);
	sb.append(".");
	sb.append(percent % 10);
	sb.append('%');
	return sb.toString();
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
    private Bookmark[] bookmarks;
    private JProgressBar satelliteMeter;
    private JProgressBar terrestrialMeter;
    private JButton itmsButton;
    private PreferencesDialog preferences;
    private JSlider ratingSlider;
    private JComboBox favoriteMenu;
    private JToggleButton favoriteCheckbox;
   
    public void quit() { 
	if (RadioCommander.theRadio().isOn())
	    MainWindow.this.turnPowerOff();
	this.saveChannelTableLayout();
	System.exit(0);
    }
    public void prefs() {
	this.preferences.show();
    }
    public void about() {
System.err.println("SHOW ABOUT WINDOW!");
    }

    private void saveChannelTableLayout() {
	byte index[] = new byte[this.channelTableModel.getColumnCount()];

	for(int i = 0; i < this.channelTableModel.getColumnCount(); i++) {
	    TableColumn tc = this.channelTable.getColumnModel().getColumn(i);
	    index[i] = (byte)tc.getModelIndex();
	}
	this.myUserNode().putByteArray(CHAN_TABLE_COLS, index);
    }

    public MainWindow() {

	PlatformFactory.ourPlatform().registerCallbackHandler(this);

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
	this.preferences = new PreferencesDialog(this.myFrame, this);

	// If on a mac, don't do this - use the EAWT stuff instead
	if (!PlatformFactory.ourPlatform().useMacMenus()) {
	    JMenu jm = new JMenu("JXM");
	    JMenuItem jmi = new JMenuItem("Preferences...");
	    jmi.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
		MainWindow.this.prefs();
	        }
	    });
	    jm.add(jmi);
	    jmi = new JMenuItem("Exit");
	    jmi.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
		    MainWindow.this.quit();
	        }
	    });
	    jm.add(jmi);
	    this.myFrame.getJMenuBar().add(jm);
	}
	// -----
	// PUT MENUS HERE
	// -----
	if (!PlatformFactory.ourPlatform().useMacMenus()) {
	    JMenu jm = new JMenu("Help");
	    JMenuItem jmi = new JMenuItem("About JXM...");
	    jmi.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
		MainWindow.this.about();
	        }
	    });
	    jm.add(jmi);
	    this.myFrame.getJMenuBar().add(jm);
	}

	this.myFrame.getContentPane().setLayout(new GridBagLayout());
	GridBagConstraints frameGBC = new GridBagConstraints();

	JPanel top = new JPanel();
	top.setLayout(new BoxLayout(top, BoxLayout.PAGE_AXIS));
	JPanel toptop = new JPanel();
	toptop.setLayout(new BoxLayout(toptop, BoxLayout.LINE_AXIS));
	toptop.add(Box.createHorizontalStrut(20));

	JPanel pictureFrame = new JPanel();
	pictureFrame.setBorder(BorderFactory.createLoweredBevelBorder());
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
		    ChannelInfo info;
		    try {
			info = RadioCommander.theRadio().getChannelInfo();
		    }
		    catch(RadioException ee) {
			MainWindow.this.handleError(ee);
			return;
		    }
		    class BookmarkMenu extends JPopupMenu {
			private ChannelInfo info;
			public BookmarkMenu(ChannelInfo info) {
			    super();
			    this.info = info;
			    for(int i = 0; i < MainWindow.this.bookmarks.length; i++) {
				final Bookmark b = MainWindow.this.bookmarks[i];
				JMenuItem jmi = new JMenuItem(b.getName());
				jmi.addActionListener(new ActionListener() {
				    public void actionPerformed(ActionEvent e) {
					MainWindow.this.bookmarkSurf(b, BookmarkMenu.this.info);
				    }
				});
				this.add(jmi);
			    }
			}
		    };
		    JPopupMenu popup = new BookmarkMenu(info);
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
	toptop.add(jp);
	toptop.add(Box.createHorizontalStrut(5));
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
	this.favoriteMenu.setPreferredSize(new Dimension(150, (int)this.favoriteMenu.getPreferredSize().getHeight()));
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
	this.favoriteCheckbox = new JToggleButton(new ImageIcon(this.getClass().getResource("/images/heart.png")));
	//this.favoriteCheckbox.setText("<html><img src=\"" + this.getClass().getResource("/images/heart.png") + "\"></html>");
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
	gbc.insets = new Insets(0, 20, 0, 0);
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
	this.ratingSlider.setPreferredSize(new Dimension(275, (int)this.favoriteMenu.getPreferredSize().getHeight()));
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
	gbc.insets = new Insets(0, 20, 0, 20);
	gbc.fill = GridBagConstraints.HORIZONTAL;
	gbc.anchor = GridBagConstraints.CENTER;
	stripe.add(rating, gbc);
	
	JPanel extra = new JPanel();
	// XXX - what goes here?
	gbc.gridx = 2;
	gbc.weightx = 0;
	gbc.anchor = GridBagConstraints.LINE_END;
	gbc.insets = new Insets(0, 0, 0, 20);
	gbc.fill = GridBagConstraints.NONE;
	extra.setPreferredSize(favorites.getPreferredSize());
	stripe.add(extra, gbc);

	top.add(stripe);
	
	frameGBC.gridx = 0;
	frameGBC.gridy = 0;
	frameGBC.weighty = 0;
	frameGBC.weightx = 1;
	frameGBC.insets = new Insets(10, 0, 0, 0);
	frameGBC.fill = GridBagConstraints.HORIZONTAL;
	frameGBC.anchor = GridBagConstraints.PAGE_START;
	this.myFrame.getContentPane().add(top, frameGBC);

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

	final TableCellRenderer orig = this.channelTable.getTableHeader().getDefaultRenderer(); //tc.getHeaderRenderer();

	TableCellRenderer tcr = new DefaultTableCellRenderer() {
	    public Component getTableCellRendererComponent(JTable table,  Object value,  boolean isSelected, boolean hasFocus,  int row,  int column) {
		int modelColumn = MainWindow.this.channelTable.getColumnModel().getColumn(column).getModelIndex();
		if (MainWindow.this.sortField == modelColumn) {
		    StringBuffer sb = new StringBuffer();
		    sb.append("<html>");
		    sb.append((String)value);
		    sb.append("&nbsp;<img src=\"");
		    sb.append(this.getClass().getResource(MainWindow.this.sortDirection?"/images/arrow-down.png":"/images/arrow-up.png"));
		    sb.append("\"></html>");
		    value = sb.toString();
		}
		return orig.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		/*Component c = orig.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		if (MainWindow.this.sortField != modelColumn)
		    return ;
		c.setBackground(new Color(0.75f, 0.75f, 1f));
		c.setForeground(Color.BLACK);
		return c; */
	    }
	};

	byte cols[];

	cols = this.myUserNode().getByteArray(CHAN_TABLE_COLS, null);

	if (cols == null || cols.length != 6)
	    cols = new byte[] {0, 1, 2, 3, 4, 5};
	else {
	    for(int i = 0; i < cols.length; i++) {
		if (cols[i] < 0 || cols[i] > 5) {
		    cols = new byte[] {0, 1, 2, 3, 4, 5};
		    break;
		}
	    }
	}

	TableColumnModel tcm = new DefaultTableColumnModel();
	TableColumn tc;
	DefaultTableCellRenderer centered = new DefaultTableCellRenderer();
	centered.setHorizontalAlignment(SwingConstants.CENTER);
	for(int i = 0; i < cols.length; i++) {
	    switch(cols[i]) {
		case 0:
		    tc = new TableColumn(0, 80, null, null);
		    tc.setMinWidth(80);
		    tc.setCellRenderer(centered);
		    tc.setHeaderValue("Num.");
		    break;
		case 1:
		    tc = new TableColumn(1, 100, null, null);
		    tc.setMinWidth(100);
		    tc.setHeaderValue("Genre");
		    break;
		case 2:
		    tc = new TableColumn(2, 100, null, null);
		    tc.setMinWidth(100);
		    tc.setHeaderValue("Name");
		    break;
		case 3:
		    tc = new TableColumn(3, 160, null, null);
		    tc.setMinWidth(160);
		    tc.setHeaderValue("Artist");
		    break;
		case 4:
		    tc = new TableColumn(4, 160, null, null);
		    tc.setMinWidth(160);
		    tc.setHeaderValue("Title");
		    break;
		case 5:
		    tc = new TableColumn(5, 80, null, null);
		    tc.setMinWidth(80);
		    tc.setCellRenderer(centered);
		    tc.setHeaderValue("% In Use");
		    break;
		default:
		    throw new IllegalArgumentException("Which column?!");
	    }
	    tc.setHeaderRenderer(tcr);
	    tcm.addColumn(tc);
	}
	channelTable.setColumnModel(tcm);
	int tw = tcm.getTotalColumnWidth();

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
		MainWindow.this.myUserNode().putInt(SORT_FIELD, MainWindow.this.sortField);
		MainWindow.this.myUserNode().putBoolean(SORT_DIR, MainWindow.this.sortDirection);
	    }
	});

	this.sortField = this.myUserNode().getInt(SORT_FIELD, 0);
	if (this.sortField < 0 || this.sortField > 5)
	    this.sortField = 0;
	this.sortDirection = this.myUserNode().getBoolean(SORT_DIR, true);

	//channelTable.setMinimumSize(new Dimension(tw + 5, 0));
	//channelTable.setPreferredViewportSize(new Dimension((int)channelTable.getMinimumSize().getWidth(), (int)channelTable.getPreferredSize().getHeight()));
	channelTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	JScrollPane sp = new JScrollPane(channelTable);
	//sp.setMinimumSize(new Dimension(tw + 5, 0));
	sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
	//jp.add(sp, gbc);
	frameGBC.insets = new Insets(20, 20, 20, 20);
	frameGBC.gridx = 0;
	frameGBC.gridy = 1;
	frameGBC.weighty = 1;
	frameGBC.weightx = 1;
	frameGBC.fill = GridBagConstraints.BOTH;
	frameGBC.anchor = GridBagConstraints.CENTER;
        this.myFrame.getContentPane().add(sp, frameGBC);

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
	gbc.weightx = 0;
	gbc.weighty = 0;
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

	gbc.insets = new Insets(0, 20, 20, 10);
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
	gbc.weightx = .75;
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
	gbc.weightx = .25;
	gbc.insets = new Insets(0, 0, 0, 20);
	gbc.anchor = GridBagConstraints.LINE_START;
	bottom.add(this.satelliteMeter, gbc);
	this.terrestrialMeter = new JProgressBar(0, 100);
	gbc.gridy = 1;
	gbc.insets = new Insets(0, 0, 20, 20);
	bottom.add(this.terrestrialMeter, gbc);

	frameGBC.insets = new Insets(0, 0, 0, 0);
	frameGBC.gridx = 0;
	frameGBC.gridy = 2;
	frameGBC.weighty = 0;
	frameGBC.weightx = 1;
	frameGBC.fill = GridBagConstraints.HORIZONTAL;
	frameGBC.anchor = GridBagConstraints.PAGE_END;
	this.myFrame.getContentPane().add(bottom, frameGBC);
	
        this.myFrame.pack();
        this.myFrame.setResizable(true);
        this.myFrame.setVisible(true);

	java.util.Timer t = new java.util.Timer();
	t.schedule(new TimerTask() {
	    public void run() {
		if (!RadioCommander.theRadio().isOn())
		    return;
		Integer sid;
		try {
		    double[] out = RadioCommander.theRadio().getSignalStrength();
		    MainWindow.this.satelliteMeter.setValue((int)out[RadioCommander.SIGNAL_STRENGTH_SAT]);
		    MainWindow.this.terrestrialMeter.setValue((int)out[RadioCommander.SIGNAL_STRENGTH_TER]);
		    sid = new Integer(MainWindow.this.sidForChannel(RadioCommander.theRadio().getChannel()));
		}
		catch(RadioException e) {
		    // sigh. If we get an exception while powering down, then ignore it.
		    // XXX this is probably a sign we should do more locking
		    if (RadioCommander.theRadio().isOn())
		        MainWindow.this.handleError(e);
		    return;
		}
		if (sid.intValue() < 0) // just skip it if we don't know the SID (yet)
		  return;
		Integer ticks = (Integer)MainWindow.this.tickList.get(sid);
		if (ticks == null)
		    ticks = new Integer(0);
		ticks = new Integer(ticks.intValue() + 1);
		MainWindow.this.tickList.put(sid, ticks);
		MainWindow.this.firePercentChanges();
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

    private void firePercentChanges() {
	Iterator i = this.tickList.keySet().iterator();
	while(i.hasNext()) {
	    Integer sid = (Integer)i.next();
	    int row = this.rowForSID(sid.intValue());
	    if (row < 0)
		continue;
	    this.channelTableModel.fireTableRowsUpdated(row, row);
	}
    }

    private void itmsButtonClicked() {
	String u = "itms://phobos.apple.com/WebObjects/MZSearch.woa/wa/com.apple.jingle.search.DirectAction/advancedSearchResults?artistTerm={ARTIST}&songTerm={TITLE}";
	ChannelInfo i;
	try {
	    i = RadioCommander.theRadio().getChannelInfo();
	}
	catch(RadioException e) {
	    this.handleError(e);
	    return;
	}
	this.genericSurf(u, i);
/*
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
*/
    }

    private void surfToChannel(int chan) {
	String u = "http://www.xmradio.com/programming/channel_page.jsp?ch=" + chan;
	this.openURL(u);
    }

    private Pattern numPattern = Pattern.compile("\\{NUMBER\\}");
    private Pattern genrePattern = Pattern.compile("\\{GENRE\\}");
    private Pattern namePattern = Pattern.compile("\\{NAME\\}");
    private Pattern artistPattern = Pattern.compile("\\{ARTIST\\}");
    private Pattern titlePattern = Pattern.compile("\\{TITLE\\}");
    private Pattern sidPattern = Pattern.compile("\\{SERVICE\\}");
    private void genericSurf(String urlPattern, ChannelInfo info) {
	String url = urlPattern;
	try {
	    url = this.numPattern.matcher(url).replaceAll(Integer.toString(info.getChannelNumber()));
	    url = this.genrePattern.matcher(url).replaceAll(URLEncoder.encode(info.getChannelGenre(), "US-ASCII"));
	    url = this.namePattern.matcher(url).replaceAll(URLEncoder.encode(info.getChannelName(), "US-ASCII"));
	    url = this.artistPattern.matcher(url).replaceAll(URLEncoder.encode(info.getChannelArtist(), "US-ASCII"));
	    url = this.titlePattern.matcher(url).replaceAll(URLEncoder.encode(info.getChannelTitle(), "US-ASCII"));
	    url = this.sidPattern.matcher(url).replaceAll(Integer.toString(info.getServiceID()));
	}
	catch(UnsupportedEncodingException e) {
	    // Oh, whatever!
System.err.println(e.getMessage());
e.printStackTrace();
	    return;
	}
	this.openURL(url);
    }

    private void openURL(String u) {
	try {
	    PlatformFactory.ourPlatform().openURL(u);
	}
	catch(IOException e) {
	    JOptionPane.showMessageDialog(this.myFrame, e.getMessage(), "Error opening URL", JOptionPane.ERROR_MESSAGE);
	}
    }

    private void setChannelLogo(int chan) {
	URL logoUrl = this.getClass().getResource("/logos/" + chan + ".gif");
	if (logoUrl == null)
	    logoUrl = this.getClass().getResource("/logos/default.gif");
	if (logoUrl == null)
	    return; // just give up rather than throw outwards
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

    // the IPlatformCallbackHandler interface
    public void platformNotify(int type, Object arg) {
	switch(type) {
	    case PlatformFactory.PLAT_CB_PREFS:	this.prefs(); break;
	    case PlatformFactory.PLAT_CB_ABOUT:	this.about(); break;
	    case PlatformFactory.PLAT_CB_QUIT:	this.quit(); break;
	    default: throw new IllegalArgumentException("Which platform callback type??");
	}
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
	    this.prefs();
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

    private final static String CHAN_TABLE_COLS = "ChannelTableColumnOrder";
    private final static String GRID_NODE = "ChannelGrid";
    private final static String TICK_NODE = "ChannelUsage";
    private final static String SORT_DIR = "SortDirection";
    private final static String SORT_FIELD = "SortColumn";
    private final static String FAVORITE_LIST = "FavoriteChannels";
    private final static String XMTRACKER_URL = "TrackerURL";
    private final static String XMTRACKER_USER = "TrackerUser";
    private final static String XMTRACKER_PASS = "TrackerPassword";

    private void poweredUp() {
	this.loadChannelList();
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
	    this.updateUniqueUserID(rid);
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
	this.updateRatingSlider(null);
	this.saveChannelList();
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

    HashMap channelList = new HashMap();

    private void deleteChannel(int sid) {
	this.channelList.remove(new Integer(sid));
	if (this.channelList.containsKey(new Integer(sid)))
	    this.rebuildFavoritesMenu();
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
	ArrayList l = new ArrayList();
	Iterator i = this.favoriteList.iterator();
	while (i.hasNext()) {
	    Integer sid = (Integer)i.next();
	    ChannelInfo info = (ChannelInfo)this.channelList.get(sid);
	    if (info == null)
		continue;
	    l.add(info);
	}
	Collections.sort(l, new Comparator() {
	    public int compare(Object o1, Object o2) {
		ChannelInfo i1 = (ChannelInfo)o1;
		ChannelInfo i2 = (ChannelInfo)o2;
		return new Integer(i1.getChannelNumber()).compareTo(new Integer(i2.getChannelNumber()));
	    }
	});
	this.favoriteMenu.removeAllItems();
	this.favoriteMenu.addItem("Favorites");
	this.favoriteMenu.setSelectedIndex(0);
	if (l.isEmpty()) {
	    this.favoriteMenu.setEnabled(false);
	} else {
	    this.favoriteMenu.setEnabled(true);	
	    for(int j = 0; j < l.size(); j++)
		this.favoriteMenu.addItem(new Integer(((ChannelInfo)(l.get(j))).getServiceID()));
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

    private void saveChannelList() {
	Preferences node = this.myUserNode().node(GRID_NODE);
	try {
	    node.clear();
	}
	catch(BackingStoreException e) {
	    return;
	}
	if (this.channelList == null)
	    return;
	Iterator i = this.channelList.values().iterator();
	while(i.hasNext()) {
	    ChannelInfo info = (ChannelInfo)i.next();
	    StringBuffer sb = new StringBuffer();
	    sb.append(info.getChannelNumber());
	    sb.append(':');
	    sb.append(info.getServiceID());
	    sb.append(':');
	    try {
		sb.append(URLEncoder.encode(info.getChannelGenre(), "US-ASCII"));
		sb.append(':');
		sb.append(URLEncoder.encode(info.getChannelName(), "US-ASCII"));
	    }
	    catch(UnsupportedEncodingException e) {
		// cannot happen!
		return;
	    }
	    node.put(Integer.toString(info.getServiceID()), sb.toString());
	}
	node = this.myUserNode().node(TICK_NODE);
	try {
	    node.clear();
	}
	catch(BackingStoreException e) {
	    return;
	}
	if (this.tickList == null)
	    return;
	i = this.tickList.keySet().iterator();
	while(i.hasNext()) {
	    Integer sid = (Integer)i.next();
	    int ticks = ((Integer)this.tickList.get(sid)).intValue();
	    node.putInt(sid.toString(), ticks);
	}
    }

    private void loadChannelList() {
	Preferences node = this.myUserNode().node(GRID_NODE);
	String[] keys;
	try {
	    keys = node.keys();
	}
	catch(BackingStoreException e) {
	    // ignore
	    return;
	}
	this.channelList.clear();
	for(int i = 0; i < keys.length; i++) {
	    String val = node.get(keys[i], null);
	    if (val == null)
		continue;
	    String[] val_list =  val.split(":");
	    if (val_list.length != 4)
		continue;
	    ChannelInfo info;
	    try {
		int num = Integer.parseInt(val_list[0]);
		int sid = Integer.parseInt(val_list[1]);
		String genre = URLDecoder.decode(val_list[2], "US-ASCII");
		String name = URLDecoder.decode(val_list[3], "US-ASCII");
		info = new ChannelInfo(num, sid, genre, name, "", "");
	    }
	    catch(Exception e) {
		continue;
	    }
	    this.channelList.put(new Integer(info.getServiceID()), info);
	}
	node = this.myUserNode().node(TICK_NODE);
	try {
	    keys = node.keys();
	}
	catch(BackingStoreException e) {
	    // ignore
	    return;
	}
	this.tickList.clear();
	for(int i = 0; i < keys.length; i++) {
	    try {
		Integer sid = new Integer(Integer.parseInt(keys[i]));
		int ticks = node.getInt(keys[i], 0);
		this.tickList.put(sid, new Integer(ticks));
	    }
	    catch(NumberFormatException e) {
		// ignore
	    }
	}
	this.channelTableModel.fireTableDataChanged();
    }

    private ChannelInfo ratingChannelInfo;

    private void updateRatingSlider(final ChannelInfo newInfo) {
	final ChannelInfo toRate = this.ratingChannelInfo;
	boolean disable_it = false;

	if (newInfo != null) {
	    if (newInfo.equals(this.ratingChannelInfo))
		return; // it hasn't yet changed

	    disable_it = (newInfo.getChannelArtist().length() == 0 || newInfo.getChannelTitle().length() == 0);

	    // We have new info - make a note for the next transition
	    this.ratingChannelInfo = newInfo;
	} else {
	    disable_it = true;
	}

	if (toRate != null) {
	    final int rating = (this.ratingSlider.getValue());
	    if (rating != 0) {
	    new Thread() {
		public void run() {
		    try {
			MainWindow.this.rateSong(toRate, rating);
		    }
		    catch(final Exception e) {
			SwingUtilities.invokeLater(new Runnable() {
			    public void run() {
				JOptionPane.showMessageDialog(MainWindow.this.myFrame, e.getMessage(), "Error while rating song", JOptionPane.ERROR_MESSAGE);
			    }
			});
		    }
		}
	    }.start();
	    }
	}

	this.ratingSlider.setValue(0);
	this.ratingSlider.setEnabled(!disable_it);
    }

    private String lastRecordedUserID;
        private static MessageDigest myDigestMaker;
        static {
                try {
                        myDigestMaker = MessageDigest.getInstance("MD5");
                }
                catch(NoSuchAlgorithmException e) {
                        myDigestMaker = null; // This should never happen!
                }
        }

    // For the census and the song rating, we want to be able to uniquely identify users. But transmitting their
    // actual radio ID would be rude and potentially a security problem.
    public void updateUniqueUserID(String radioID) {
                byte[] digest;
                synchronized(myDigestMaker) { // We only have the one.
                        myDigestMaker.reset();
                        myDigestMaker.update(radioID.getBytes());
                        digest = myDigestMaker.digest();
                }
                StringBuffer sb = new StringBuffer();
                for(int i = 0; i < digest.length; i++) {
                        String b = Integer.toString(digest[i] & 0xff, 16);
                        if (b.length() == 1)
                                sb.append('0');
                        sb.append(b);
                }
                this.lastRecordedUserID =  sb.toString();
    }

    private void rateSong(ChannelInfo info, int rating) throws Exception {
	StringBuffer sb = new StringBuffer();
                
	sb.append("channel=");
	sb.append(info.getChannelNumber());
	sb.append("&title=");
	sb.append(URLEncoder.encode(info.getChannelTitle(), "US-ASCII"));
	sb.append("&artist=");
	sb.append(URLEncoder.encode(info.getChannelArtist(), "US-ASCII"));
	sb.append("&rating=");
	sb.append(rating);
	sb.append("&user=");
	sb.append(this.lastRecordedUserID);
                
	URL u = new URL("http://xmpcr.kfu.com/rate");
	URLConnection c = u.openConnection();
	c.setDoOutput(true);
	c.setRequestProperty("User-Agent", JXM.userAgentString());
	OutputStreamWriter os = new OutputStreamWriter(c.getOutputStream());
	os.write(sb.toString());
	os.close();
	c.getContent();
    }

    private void update(final ChannelInfo i) {
	// We got an update. First, we file it, firing table update events
	// while we're at it.
	if (this.channelList == null) // spurious update
	    return;
	int oldSize = this.channelList.size();
	this.channelList.put(new Integer(i.getServiceID()), i);
	int row = this.rowForSID(i.getServiceID());
	if (this.channelList.size() != oldSize) {
	    this.channelTableModel.fireTableRowsInserted(row, row);
	    if (this.channelList.containsKey(new Integer(i.getServiceID())))
		this.rebuildFavoritesMenu();
	} else {
	    this.channelTableModel.fireTableRowsUpdated(row, row);
	}

	this.selectCurrentChannel();

	if (RadioCommander.theRadio().getChannel() == i.getChannelNumber()) {
	    // update the favorite checkbox
	    this.favoriteCheckbox.setSelected(this.favoriteList.contains(new Integer(i.getServiceID())));
	    // update the rating slider
	    this.updateRatingSlider(i);
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
