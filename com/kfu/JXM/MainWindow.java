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

 $Id: MainWindow.java,v 1.84 2004/04/07 08:03:26 nsayer Exp $
 
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
import java.lang.reflect.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.prefs.*;
import java.util.regex.*;

import com.kfu.xm.*;

public class MainWindow implements RadioEventHandler, IPlatformCallbackHandler, IPreferenceCallbackHandler {

    private class SignalProgressBar extends JProgressBar {
	public SignalProgressBar(int a, int b) { super(a,b); }
	// This makes the curve for creating the color arc from red to green
	private float scale(float in) {
	    if (in > .5)
		return 1;
	    in *= 2;
	    return (float)Math.sin(in * Math.PI / 2f);
	}
	public void paint(Graphics g) {
	    Rectangle bounds = this.getBounds();
	    g.setColor(this.getBackground());
	    g.fillRect(0, 0, (int)bounds.getWidth(), (int)bounds.getHeight());
	    int valueWidth = this.getMaximum() - this.getMinimum();
	    int valueSoFar = this.getValue() - this.getMinimum();
	    // We want posSoFar, posSoFar/bounds.getWidth() = valueSoFar / valueWidth, or posSoFar = valueSoFar * bounds.getWidth() / valueWidth;
	    int posSoFar = (int)((((float)valueSoFar) * ((float)bounds.getWidth())) / ((float)valueWidth));

	    for(int x = 0; x < posSoFar; x += 4) {
		// x is posHere. x / bounds.getWidth() = ? / valueWidth
		float fracHere = (float) ((float)valueWidth * ((float)x / (float)bounds.getWidth())) / (float)valueWidth;
		g.setColor(new Color(scale(1 - fracHere), scale(fracHere), 0, 1));
		g.fillRect(x, 0, 2, (int)bounds.getHeight() - 1);
	    }
	}
    }

    private class CompactViewPanel extends JWindow {
	class WindowMover extends MouseInputAdapter {
	    private Container window;
	    public WindowMover(Container c) { this.window = c; }
	    int originX, originY;
	    public void mousePressed(MouseEvent e) {
		this.originX = e.getX();
		this.originY = e.getY();
	    }
	    public void mouseDragged(MouseEvent e) {
		Point p = MainWindow.this.compactView.getLocation();
		MainWindow.this.compactView.setLocation(p.x + e.getX() - originX, p.y + e.getY() - originY);
	    }
	}

	private SpinnerListModel compactSpinnerModel;
	private JSpinner theSpinner;
	private JLabel compactViewName, compactViewArtist, compactViewTitle;

	public void refreshSpinnerModel() {
	    this.timerIgnore = true;
	    try {
	    ArrayList al = new ArrayList();
	    ChannelInfo[] sourceList = MainWindow.this.sortedChannelList;
	    for(int i = 0; i < sourceList.length; i++)
		al.add(new Integer(sourceList[i].getChannelNumber()));
	    Collections.sort(al);
	    if (al.size() != 0)
	        this.compactSpinnerModel.setList(al);
	    //this.compactSpinnerModel.setValue(new Integer(MainWindow.this.currentChannelInfo.getChannelNumber()));
	    }
	    finally {
		this.timerIgnore = false;
	    }
	}

	public void show() {
	    this.refreshSpinnerModel();
	    this.timerIgnore = true;
	    try {
		this.compactSpinnerModel.setValue(new Integer(MainWindow.this.currentChannelInfo.getChannelNumber()));
	    }
	    finally {
		this.timerIgnore = false;
	    }
	    super.show();
	    this.setChannelInfo(MainWindow.this.currentChannelInfo);
	}

	private boolean timerIgnore = false;
	private boolean changeInProgress = false;
	public synchronized void setTimerOff(final int chan) {
	    this.changeTimer.cancel();
	    this.changeTimer = null;
	    this.timerIgnore = true;
	    try {
		// If the radio powered off in the meantime, then just forget it.
		if (!RadioCommander.theRadio().isOn())
		    return;
		// If they did nothing, well, then we're done.
		if (chan == RadioCommander.theRadio().getChannel())
		    return;
		SwingUtilities.invokeAndWait(new Runnable() {
		    public void run() {
			// Ignore further pounding while we're busy
			CompactViewPanel.this.theSpinner.setEnabled(false);
		    }
		});
		// This is a separate invoke so we can be *sure* that
		// we're disabled *before* we go set the channel
		SwingUtilities.invokeAndWait(new Runnable() {
		    public void run() {
			MainWindow.this.setChannel(chan);
		    }
		});
		// And *this* is an invokeLater so we can get back
		// into timerIgnore=false position before re-enabling the spinner.
		SwingUtilities.invokeLater(new Runnable() {
		    public void run() {
			CompactViewPanel.this.theSpinner.setEnabled(true);
		    }
		});
	    }
	    catch(InterruptedException ex) { } // ignore
	    catch(InvocationTargetException ex) { } // Can't happen
	    finally {
		this.timerIgnore = false;
		this.changeInProgress = false;
	    }
	}

	private java.util.Timer changeTimer = null;

	public CompactViewPanel() {
	    super();
	    JPanel fake = new JPanel();
	    FlowLayout l = new FlowLayout();
	    l.setHgap(10);
	    fake.setLayout(l);
	    MouseInputAdapter mia = new WindowMover(this);
	    this.addMouseMotionListener(mia);
	    this.addMouseListener(mia);
	    this.compactSpinnerModel = new SpinnerListModel();
	    this.theSpinner = new JSpinner(this.compactSpinnerModel);
	    this.theSpinner.setPreferredSize(new Dimension(60, (int)this.theSpinner.getPreferredSize().getHeight()));
	    // If the spinner stays put for 1 second, then change the channel. This lets the user slam through a bunch of changes quickly.
	    this.theSpinner.addChangeListener(new ChangeListener() {
		public void stateChanged(ChangeEvent e) {
		    synchronized(CompactViewPanel.this) {
		    if (CompactViewPanel.this.timerIgnore)
			return;
		    // ARGH! If *WE'RE* why it's frigging changing, then we don't frigging care!
		    CompactViewPanel.this.changeInProgress = true;
		    final int chan = ((Integer)CompactViewPanel.this.compactSpinnerModel.getValue()).intValue();
		    int sid = MainWindow.this.sidForChannel(chan);
		    if (sid >= 0) {
			ChannelInfo info = (ChannelInfo)MainWindow.this.channelList.get(new Integer(sid));
			if (info != null) {
			    CompactViewPanel.this.compactViewName.setText(info.getChannelName());
			    CompactViewPanel.this.compactViewArtist.setText(info.getChannelArtist());
			    CompactViewPanel.this.compactViewTitle.setText(info.getChannelTitle());
			}
		    }
		    if (CompactViewPanel.this.changeTimer != null)
			CompactViewPanel.this.changeTimer.cancel();
		    CompactViewPanel.this.changeTimer = new java.util.Timer();
		    CompactViewPanel.this.changeTimer.schedule(new TimerTask() {
			public void run() {
			    CompactViewPanel.this.setTimerOff(chan);
			}
		    }, 1000);
		}
		}
	    });
	    fake.add(this.theSpinner);
	    this.compactViewName = new JLabel(" ");
	    this.compactViewName.setPreferredSize(new Dimension(150, (int)this.compactViewName.getPreferredSize().getHeight()));
	    fake.add(this.compactViewName);
	    this.compactViewArtist = new JLabel(" ");
	    this.compactViewArtist.setPreferredSize(new Dimension(150, (int)this.compactViewArtist.getPreferredSize().getHeight()));
	    this.compactViewArtist.setHorizontalAlignment(SwingConstants.CENTER);
	    fake.add(this.compactViewArtist);
	    this.compactViewTitle = new JLabel(" ");
	    this.compactViewTitle.setPreferredSize(new Dimension(150, (int)this.compactViewTitle.getPreferredSize().getHeight()));
	    this.compactViewTitle.setHorizontalAlignment(SwingConstants.CENTER);
	    fake.add(this.compactViewArtist);
	    fake.add(this.compactViewTitle);
	    JButton jb = new JButton("Restore");
	    jb.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    MainWindow.this.toggleCompactView();
		}
	    });
	    fake.add(jb);
	    fake.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED), BorderFactory.createBevelBorder(BevelBorder.RAISED)));
	    this.getContentPane().add(fake);
	    this.pack();
	}
	public void setChannelInfo(ChannelInfo i) {
	    // if we're not actually viewing the compact view, then it's moot.
	    if (!this.isVisible())
		return;
	// If we're in the middle of a change, we don't care about external updates, unless it's to power off.
	    if (this.changeInProgress && i != null)
		return;
	    if (i == null) {
		//this.compactSpinnerModel.setList();
		this.compactViewName.setText("");
		this.compactViewArtist.setText("");
		this.compactViewTitle.setText("");
	    } else {
		this.timerIgnore = true;
		try {
		    this.compactSpinnerModel.setValue(new Integer(i.getChannelNumber()));
		}
		finally {
		    this.timerIgnore = false;
		}
		this.compactViewName.setText(i.getChannelName());
		this.compactViewArtist.setText(i.getChannelArtist());
		this.compactViewTitle.setText(i.getChannelTitle());
	    }
	}
    }

    public static class ChannelInfoPanel extends JPanel {
	private class SongTimeProgressBar extends JProgressBar {
	    public SongTimeProgressBar() {
		super();
	    }
	    public void paintBorder(Graphics g) {
		g.setColor(this.getForeground());
		g.drawRect(0, 0, this.getWidth() - 1, this.getHeight() - 1);
	    }
	    public void paint(Graphics g) {
		Rectangle bounds = this.getBounds();
		g.setColor(this.getBackground());
		// Inside the border, please.
		g.fillRect(0, 0, (int)bounds.getWidth() - 1, (int)bounds.getHeight() - 1);
		this.paintBorder(g); // XXX - Why is this necessary?! Why doesn't setBorder(true) work?!
		int valueWidth = this.getMaximum() - this.getMinimum();
		int valueSoFar = this.getValue() - this.getMinimum();
		// We want posSoFar, posSoFar/bounds.getWidth() = valueSoFar / valueWidth, or posSoFar = valueSoFar * bounds.getWidth() / valueWidth;
		int posSoFar = (int)((((float)valueSoFar) * ((float)bounds.getWidth())) / ((float)valueWidth));
		int diamondHeightOffset = (int)(bounds.getHeight() * .1f);
		Polygon p = new Polygon();
		p.addPoint(posSoFar, diamondHeightOffset);
		p.addPoint(posSoFar + (((int)bounds.getHeight() - 1) / 2 - diamondHeightOffset), (int)bounds.getHeight() / 2);
		p.addPoint(posSoFar, (int)bounds.getHeight() - diamondHeightOffset - 1);
		p.addPoint(posSoFar - (((int)bounds.getHeight() - 1) / 2 - diamondHeightOffset), (int)bounds.getHeight() / 2);
		p.addPoint(posSoFar, diamondHeightOffset);
		g.setColor(this.getForeground());
		g.fillPolygon(p);
	    }
	}

	public final static Font chNumFont = new Font(null, Font.BOLD, 18);
	public final static Font chGenreFont = new Font(null, Font.PLAIN, 12);
	public final static Font chNameFont = new Font(null, Font.PLAIN, 14);
	public final static Font chArtistFont = new Font(null, Font.BOLD, 20);
	public final static Font chTitleFont = new Font(null, Font.BOLD, 20);

	private JLabel channelNumberLabel, channelGenreLabel, channelNameLabel, channelArtistLabel, channelTitleLabel;

	private JProgressBar songTimeBar;

	private Date songStart, songEnd;

	private javax.swing.Timer songTimeTimer;

	public ChannelInfoPanel() {
		this.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;

		this.channelNumberLabel = new JLabel();
		this.channelNumberLabel.setHorizontalAlignment(SwingConstants.CENTER);
		this.channelNumberLabel.setFont(chNumFont);
		gbc.weightx = 0.25;
		gbc.gridx = 0;
		gbc.gridy = 0;
		this.add(this.channelNumberLabel, gbc);
		this.channelNameLabel = new JLabel();
		this.channelNameLabel.setHorizontalAlignment(SwingConstants.CENTER);
		this.channelNameLabel.setFont(chNameFont);
		gbc.gridy = 1;
		this.add(this.channelNameLabel, gbc);
		this.channelGenreLabel = new JLabel();
		this.channelGenreLabel.setHorizontalAlignment(SwingConstants.CENTER);
		this.channelGenreLabel.setFont(chGenreFont);
		gbc.gridy = 2;
		this.add(this.channelGenreLabel, gbc);
		this.channelArtistLabel = new JLabel();
		this.channelArtistLabel.setHorizontalAlignment(SwingConstants.CENTER);
		this.channelArtistLabel.setFont(chArtistFont);
		gbc.gridx = 1;
		gbc.weightx = 0.75;
		gbc.gridwidth = 2;
		gbc.gridy = 0;
		this.add(this.channelArtistLabel, gbc);
		this.channelTitleLabel = new JLabel();
		this.channelTitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
		this.channelTitleLabel.setFont(chTitleFont);
		gbc.gridy = 1;
		this.add(this.channelTitleLabel, gbc);
		this.songTimeBar = new SongTimeProgressBar();
		this.songTimeBar.setVisible(false);
		this.songTimeBar.setMinimum(0);
		this.songTimeBar.setMaximum(1000);
		this.songTimeBar.setBorderPainted(true); // XXX - this doesn't appear to frigging work.
		this.songTimeBar.setBackground(this.channelTitleLabel.getBackground());
		this.songTimeBar.setForeground(this.channelTitleLabel.getForeground());
		this.songTimeBar.setPreferredSize(new Dimension((int)this.getPreferredSize().getWidth(), 10));
		gbc.gridy = 2;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(5, 20, 0, 20);
		this.add(this.songTimeBar, gbc);

		this.songTimeTimer = new javax.swing.Timer(50, new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			ChannelInfoPanel.this.timerTick();
		    }
		});
		this.songTimeTimer.stop();
	}

	public void timerTick() {
	    if (this.songStart == null || this.songEnd == null)
		return;

	    long len = this.songEnd.getTime() - this.songStart.getTime();
	    long soFar = new Date().getTime() - this.songStart.getTime();
	    float permill = (((float)soFar) / ((float)len)) * 1000.0f;
	    int val;
	    if (permill < 0f)
		val = 0;
	    else if (permill >= 1000f)
		val = 1000;
	    else
		val = (int)permill;
	    this.songTimeBar.setValue(val);
	    if (val == 1000) {
		this.songTimeBar.setVisible(false);
		this.songTimeTimer.stop();
	    }
	    this.songTimeBar.repaint();
	}

	public void setSongTime(Date start, Date end) {
	    if (start == null || end == null) {
		this.songStart = null;
		this.songEnd = null;
		this.songTimeTimer.stop();
		this.songTimeBar.setVisible(false);
	    } else {
		this.songStart = start;
		this.songEnd = end;
		this.songTimeTimer.start();
		this.songTimeBar.setVisible(true);
	    }
	}

	public void setChannelInfo(ChannelInfo info) {
	    if (info != null) {
		this.channelNumberLabel.setText(Integer.toString(info.getChannelNumber()));
		this.channelGenreLabel.setText(info.getChannelGenre());
		this.channelNameLabel.setText(info.getChannelName());
		this.channelArtistLabel.setText(info.getChannelArtist());
		this.channelTitleLabel.setText(info.getChannelTitle());
	    } else {
		this.channelNumberLabel.setText("");
		this.channelGenreLabel.setText("");
		this.channelNameLabel.setText("");
		this.channelArtistLabel.setText("");
		this.channelTitleLabel.setText("");
	    }
	}
    }
    private class BookmarkMenu extends JMenu {
	// This is a "throwaway" menu - it's not dynamic
	private ChannelInfo info;
	public BookmarkMenu(ChannelInfo info) {
	    super("Web Bookmarks");
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

    public final static int CHAN_POPUP_NO_MEM = 1;	// This popup won't add to memory
    public final static int CHAN_POPUP_NO_TUNE = 2;	// This popup won't tune to channel

    // A popup menu for a given channel
    public class ChannelPopupMenu extends JPopupMenu {
	ChannelInfo channelInfo;

	public ChannelPopupMenu(ChannelInfo info) {
	    this(info, 0);
	}
	public ChannelPopupMenu(ChannelInfo info, int flag) {
	    super();
	    this.channelInfo = info;

	    JMenuItem jmi = new JMenuItem("Tune to channel");
	    if (flag == MainWindow.CHAN_POPUP_NO_TUNE || !RadioCommander.theRadio().isOn()) {
		jmi.setEnabled(false);
	    } else {
		jmi.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			MainWindow.this.setChannel(ChannelPopupMenu.this.channelInfo.getChannelNumber());
		    }
		});
	    }
	    this.add(jmi);
	    jmi = new JMenuItem("Add to notebook");
	    if (flag == MainWindow.CHAN_POPUP_NO_MEM) {
		jmi.setEnabled(false);
	    } else {
		jmi.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			MainWindow.this.memoryPanel.memorize(ChannelPopupMenu.this.channelInfo);
		    }
		});
	    }
	    this.add(jmi);
	    this.add(new MainWindow.BookmarkMenu(this.channelInfo));
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
	    return MainWindow.this.sortedChannelList.length;
        }
        public int getColumnCount() {
 		return 6;           
        }
        public Object getValueAt(int row, int column) {
	    ChannelInfo i = (ChannelInfo)MainWindow.this.sortedChannelList[row];
	    switch(column) {
		case 0: return new Integer(i.getChannelNumber());
		case 1: return i.getChannelGenre();
		case 2: return i.getChannelName();
		case 3: return i.getChannelArtist();
		case 4: return i.getChannelTitle();
		case 5: return MainWindow.this.inUseForSID(i.getServiceID());
		default: throw new IllegalArgumentException("Which column?");
	    }
        }
    }

    private int sortField = 0;
    private boolean sortDirection = true;

    private HashSet filterList = new HashSet();

    private ChannelInfo[] sortedChannelList = new ChannelInfo[0];

    // Called by the filter panel
    public ChannelInfo[] getChannelList() {
	ChannelInfo[] out = new ChannelInfo[this.channelList.size()];
	Iterator i = this.channelList.values().iterator();
	int j = 0;
	while(i.hasNext()) {
	    out[j++] = (ChannelInfo)i.next();
	}
	return out;
    }
    public void setFilter(byte[] sids) {
	this.filterList.clear();
	for(int i = 0; i < sids.length; i++)
	    this.filterList.add(new Integer(sids[i] & 0xff));
	this.rebuildSortedChannelList();
    }

    // Wow. This operation is stunningly inefficient. The horror.
    private void rebuildSortedChannelList() {
	//ChannelInfo[] newList = new ChannelInfo[this.channelList.size() - this.filterList.size()];
	// Argh! Because the filter list *may* be filtering channels we don't have in the list yet, we don't know
	// in advance the size of the filtered channel set.
	ArrayList temp = new ArrayList();
	Iterator i = this.channelList.values().iterator();
	while(i.hasNext()) {
	    ChannelInfo info = (ChannelInfo)i.next();
	    if (this.filterList.contains(new Integer(info.getServiceID())))
		continue;

	    String filter = this.searchField.getText();
	    if (filter != null) {
		filter = filter.trim().toLowerCase();
		if (filter.length() == 0)
		    filter = null;
	    }
	    if (filter != null &&
		info.getChannelName().toLowerCase().indexOf(filter) < 0 &&
		info.getChannelGenre().toLowerCase().indexOf(filter) < 0 &&
		info.getChannelArtist().toLowerCase().indexOf(filter) < 0 &&
		info.getChannelTitle().toLowerCase().indexOf(filter) < 0)
		continue;

	    temp.add(info);
	}
	ChannelInfo[] newList = (ChannelInfo[])temp.toArray(new ChannelInfo[0]);
	Arrays.sort(newList, new Comparator() {
	    public int compare(Object o1, Object o2) {
		ChannelInfo ci1 = (ChannelInfo)o1;
		ChannelInfo ci2 = (ChannelInfo)o2;
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
	ChannelInfo oldList[] = this.sortedChannelList;
	this.sortedChannelList = newList;

	if (oldList.length < newList.length)
	    this.channelTableModel.fireTableRowsInserted(oldList.length - 1, newList.length - 1);
	else if (oldList.length > newList.length)
	    this.channelTableModel.fireTableRowsDeleted(newList.length - 1, oldList.length - 1);
	for(int j = 0; j < Math.min(oldList.length, newList.length); j++)
	    if (!oldList[j].equals(newList[j]))
		this.channelTableModel.fireTableRowsUpdated(j, j);
    }

    private HashMap tickList = new HashMap();

    // the IPreferencesCallbackInterface
    public void reload() {
	this.searchSystem.reload();
	this.filterPanel.reload();
    }
    public void save() {
	this.searchSystem.save();
	this.filterPanel.save();
    }
    public JComponent getSearchPreferencePanel() {
	return this.searchSystem.getPrefPanel();
    }
    public JComponent getFilterPreferencePanel() {
	return this.filterPanel;
    }
    public void clearChannelStats() {
	this.tickList.clear();
	Preferences node = JXM.myUserNode().node(TICK_NODE);
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
	ChannelInfo channels[] = this.sortedChannelList;
	for(int i = 0; i < channels.length; i++)
	    if (channels[i].getServiceID() == sid)
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

    private SearchSystem searchSystem;
    private FilterPanel filterPanel;
    private CompactViewPanel compactView;
    private MemoryPanel memoryPanel;
    private PreferencesDialog preferences;
    private AboutDialog aboutDialog;
    private ChannelInfoPanel nowPlayingPanel;
    private JLabel channelLogo;
    private ChannelTableModel channelTableModel;
    private JTextField searchField;
    private JButton searchFieldClear;
    private JTable channelTable;
    private JCheckBox powerCheckBox;
    private JCheckBox muteButton;
    private JCheckBox smartMuteButton;
    private JFrame myFrame;
    private JMenu bookmarkMenu;
    private JMenuItem powerMenuItem;
    //private JMenuItem filterMenuItem;
    private JComboBox filterMenu;
    private JMenuItem compactMenuItem;
    private Bookmark[] bookmarks;
    private JProgressBar satelliteMeter;
    private JProgressBar terrestrialMeter;
    //private JButton itmsButton;
    private JButton memoryButton;
    private JSlider ratingSlider;
    private JComboBox favoriteMenu;
    private JToggleButton favoriteCheckbox;
    private ChannelInfo currentChannelInfo;
   
    public void quit() { 
	if (RadioCommander.theRadio().isOn())
	    MainWindow.this.turnPowerOff();
	this.saveChannelTableLayout();
	this.saveTickList();
	this.memoryPanel.quit();
	System.exit(0);
    }
    public void prefs() {
	this.forceNormalView();
	this.preferences.show();
    }
    public void about() {
	this.forceNormalView();
	this.aboutDialog.show();
    }

    private void saveChannelTableLayout() {
	byte index[] = new byte[this.channelTableModel.getColumnCount()];

	for(int i = 0; i < this.channelTableModel.getColumnCount(); i++) {
	    TableColumn tc = this.channelTable.getColumnModel().getColumn(i);
	    index[i] = (byte)tc.getModelIndex();
	}
	JXM.myUserNode().putByteArray(CHAN_TABLE_COLS, index);
    }

    public final static Color stripeColor = new Color(.925f, .925f, 1f);
    public final static Color gridColor = new Color(.85f, .85f, .85f);

    private class XIcon implements Icon {
        public int getIconHeight() { return 17; }
        public int getIconWidth() { return this.getIconHeight(); }
	public void paintIcon(Component c, Graphics gg, int x, int y) {
	    Graphics2D g = (Graphics2D)gg;
	    if (c.isEnabled())
		g.setColor(c.getForeground());
	    else
		g.setColor(Color.GRAY);

	    g.translate(x,y);
	    g.fillArc(0, 0, this.getIconWidth() - 1, this.getIconHeight() - 1, 0, 360);
	    g.setColor(c.getBackground());
	    int centerx = this.getIconWidth() / 2;
	    int centery = this.getIconHeight() / 2;
	    int xlen = (this.getIconHeight() * 2) / 10;
	    g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
	    g.drawLine(centerx - xlen, centery - xlen, centerx + xlen, centery + xlen);
	    g.drawLine(centerx + xlen, centery - xlen, centerx - xlen, centery + xlen);

	    g.translate(-x,-y);
        }
    }

    private class ArrowIcon implements Icon, SwingConstants {
	private int dir;
	public ArrowIcon(int which) {
	    if (which != NORTH && which != SOUTH)
		throw new IllegalArgumentException("Arrow must point NORTH or SOUTH");
	    this.dir = which;
	}
	private int width = 9;
	private int height = 12;
	public int getIconHeight() { return this.height; }
	public int getIconWidth() { return this.width; }
	public void paintIcon(Component c, Graphics g, int x, int y) {
	    if (c.isEnabled())
		g.setColor(c.getForeground());
	    else
		g.setColor(Color.GRAY);

	    g.translate(x,y);
	    Polygon p = new Polygon();
	    switch(this.dir) {
		case NORTH:
			p.addPoint(0, this.height/2);
			p.addPoint(this.width - 1, this.height/2);
			p.addPoint(this.width/2, 1);
			p.addPoint(0, this.height/2);
		    break;
		case SOUTH:
			p.addPoint(0, this.height/2);
			p.addPoint(this.width - 1, this.height/2);
			p.addPoint(this.width/2, this.height - 2);
			p.addPoint(0, this.height/2);
		    break;
		default: throw new IllegalArgumentException("How did this happen?!");
	    }
	    g.fillPolygon(p);
	    // restore
	    g.translate(-x,-y);
	}
    }

    private final Icon upArrow = new ArrowIcon(SwingConstants.NORTH);
    private final Icon downArrow = new ArrowIcon(SwingConstants.SOUTH);

    private static void frontOrShow(Window w) {
	if (w.isVisible())
	    w.toFront();
	else
	    w.show();
    }

    public MainWindow() {

	PlatformFactory.ourPlatform().registerCallbackHandler(this);

	try {
	    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	}
	catch(Exception e) {
	    // Well, we tried
	}

	String[] logoPaths = {
	    // The current working dir
	    "file:" + System.getProperty("user.dir") + "/JXMlogos.jar",
	    // The user's home dir
	    "file:" + System.getProperty("user.home") + "/JXMlogos.jar",
	    // The user's home dir, but with a "." path
	    "file:" + System.getProperty("user.home") + "/.JXMlogos.jar",
	};
	for(int i = 0; i < logoPaths.length; this.logoJar = null, i++) {
	    try {
		this.logoJar = new URL(logoPaths[i]);
		this.logoJar.openConnection().connect();
	    }
	    catch(MalformedURLException e) {
		continue;
	    }
	    catch(IOException e) {
		continue;
	    }
	    if (this.logoJar != null)
		break;
	}
	// This is the ultimate fallback: Just use the logos dir built-in
	// to the application.

        this.myFrame = new JFrame("JXM");
	Image duke = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/images/xm_duke.png"));
	this.myFrame.setIconImage(duke);
	this.myFrame.setJMenuBar(new JMenuBar());
	this.myFrame.addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent e) {
		MainWindow.this.quit();
	    }
	});
	this.searchSystem = new SearchSystem(this);
	this.filterPanel = new FilterPanel(this);
	this.preferences = new PreferencesDialog(this.myFrame, this);
	this.aboutDialog = new AboutDialog(this.myFrame);

	JMenu jm;
	JMenuItem jmi;

	// If on a mac, don't do this - use the EAWT stuff instead
	if (!PlatformFactory.ourPlatform().useMacMenus()) {
	    jm = new JMenu("JXM");
	    jmi = new JMenuItem("Preferences...");
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
	jm = new JMenu("Actions");
	this.powerMenuItem = new JMenuItem("Turn Radio On");
	this.powerMenuItem.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		MainWindow.this.powerToggle();
	    }
	});
	jm.add(this.powerMenuItem);
	this.compactMenuItem = new JMenuItem("Compact view");
	this.compactMenuItem.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		MainWindow.this.toggleCompactView();
	    }
	});
	this.compactMenuItem.setEnabled(false);
	jm.add(this.compactMenuItem);
	this.myFrame.getJMenuBar().add(jm);

	class DynamicBookmarkMenu extends JMenu {
	    // Stupid Nick! How many times do we have to reimplement this same thing!?
	    // This one fetches the current channel info every time it is opened.
	    private ChannelInfo info;
	    public DynamicBookmarkMenu(String name) {
		super(name);
		super.addMenuListener(new MenuListener() {
		    public void menuDeselected(MenuEvent e) {}
		    public void menuCanceled(MenuEvent e) {}
		    public void menuSelected(MenuEvent e) {
			DynamicBookmarkMenu.this.info = new ChannelInfo(MainWindow.this.currentChannelInfo);
			DynamicBookmarkMenu.this.removeAll();
			for(int i = 0; i < MainWindow.this.bookmarks.length; i++) {
			    final Bookmark b = MainWindow.this.bookmarks[i];
			    JMenuItem jmi = new JMenuItem(b.getName());
			    jmi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
				    MainWindow.this.bookmarkSurf(b, DynamicBookmarkMenu.this.info);
				}
			    });
			    DynamicBookmarkMenu.this.add(jmi);
			}
		    }
		});
	    }
	}
 	this.bookmarkMenu = new DynamicBookmarkMenu("Web Bookmarks");
	this.bookmarkMenu.setEnabled(false);
	this.myFrame.getJMenuBar().add(this.bookmarkMenu);
	jm = new JMenu("Windows");
	if (PlatformFactory.ourPlatform().useMacMenus()) {
	    jmi = new JMenuItem("Main Window");
	    jmi.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    MainWindow.frontOrShow(MainWindow.this.myFrame);
		}
	    });
	    jm.add(jmi);
	}
	jmi = new JMenuItem("Notebook");
	jmi.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		MainWindow.frontOrShow(MainWindow.this.memoryPanel);
	    }
	});
	jm.add(jmi);
	this.myFrame.getJMenuBar().add(jm);
/*
	this.filterMenuItem = new JMenuItem("Filters");
	this.filterMenuItem.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		MainWindow.frontOrShow(MainWindow.this.filterPanel);
	    }
	});
	this.filterMenuItem.setEnabled(false);
	jm.add(this.filterMenuItem);
*/
	// -----
	if (!PlatformFactory.ourPlatform().useMacMenus()) {
	    jm = new JMenu("Help");
	    jmi = new JMenuItem("About JXM...");
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
		    ChannelInfo info = new ChannelInfo(MainWindow.this.currentChannelInfo);
		    JPopupMenu popup = MainWindow.this.new ChannelPopupMenu(info, MainWindow.CHAN_POPUP_NO_TUNE);
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
	this.nowPlayingPanel = new ChannelInfoPanel();
	this.nowPlayingPanel.setBorder(BorderFactory.createTitledBorder(
	    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Now Playing",
	    TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION));
	this.nowPlayingPanel.addMouseListener(new MouseAdapter() {
	    public void mousePressed(MouseEvent e) { this.maybePopup(e); }
	    public void mouseReleased(MouseEvent e) { this.maybePopup(e); }
	    private void maybePopup(MouseEvent e) {
		if (!RadioCommander.theRadio().isOn())
		    return;
		if (!e.isPopupTrigger())
		    return;
		ChannelInfo info = new ChannelInfo(MainWindow.this.currentChannelInfo);
		JPopupMenu jpm = MainWindow.this.new ChannelPopupMenu(info, MainWindow.CHAN_POPUP_NO_TUNE);
		jpm.show(e.getComponent(), e.getX(), e.getY());
	    }
	});
	toptop.add(this.nowPlayingPanel);
	toptop.add(Box.createHorizontalStrut(5));
	JPanel buttons = new JPanel();
	buttons.setLayout(new GridBagLayout());
	GridBagConstraints gbc_but = new GridBagConstraints();
/*
	this.itmsButton = new JButton("iTunes Music Store");
	this.itmsButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		MainWindow.this.itmsButtonClicked();
	    }
	});
	this.itmsButton.setEnabled(false);
	buttons.add(this.itmsButton, gbc_but);
*/

	this.memoryButton = new JButton("Add to notebook");
	this.memoryButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		MainWindow.this.memoryPanel.memorize(MainWindow.this.currentChannelInfo);
	    }
	});
	this.memoryButton.setEnabled(false);
	//gbc_but.weightx = 1;
	gbc_but.weightx = 0;
	gbc_but.fill = GridBagConstraints.NONE;
	buttons.add(this.memoryButton, gbc_but);

	JPanel searchHolder = new JPanel();
	searchHolder.setLayout(new GridBagLayout());
	GridBagConstraints sh_gbc = new GridBagConstraints();
	searchHolder.setOpaque(false);
	searchHolder.setBorder(BorderFactory.createTitledBorder(/*BorderFactory.createEtchedBorder(EtchedBorder.LOWERED)*/BorderFactory.createLineBorder(new Color(0,0,0,0), 0) /* the null border */, "Quick Search", TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM, new Font(null, Font.PLAIN, 10)));
	this.searchField = new JTextField();
	this.searchField.getDocument().addDocumentListener(new DocumentListener() {
	    public void changedUpdate(DocumentEvent e) { this.doit(); }
	    public void insertUpdate(DocumentEvent e) { this.doit(); }
	    public void removeUpdate(DocumentEvent e) { this.doit(); }
	    private void doit() {
		MainWindow.this.rebuildSortedChannelList();
		MainWindow.this.selectCurrentChannel();
	    }
	});
	this.searchField.setPreferredSize(new Dimension(100, (int)this.searchField.getPreferredSize().getHeight()));
	this.searchField.setEnabled(false);
	sh_gbc.weightx = 1;
	sh_gbc.gridx = 0;
	sh_gbc.gridy = 0;
	sh_gbc.fill = GridBagConstraints.HORIZONTAL;
	searchHolder.add(this.searchField, sh_gbc);
	this.searchFieldClear = new JButton();
	this.searchFieldClear.setIcon(new XIcon());
	this.searchFieldClear.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		MainWindow.this.searchField.setText("");
	    }
	});
	this.searchFieldClear.setEnabled(false);
	sh_gbc.weightx = 0;
	sh_gbc.gridx = 1;
	sh_gbc.gridy = 0;
	// Yes. We want them to overlap.
	sh_gbc.fill = GridBagConstraints.NONE;
	sh_gbc.anchor = GridBagConstraints.LINE_END;
	searchHolder.add(this.searchFieldClear, sh_gbc);
	gbc_but.insets = new Insets(10, 0, 0, 0);
	gbc_but.gridy = 1;
	buttons.add(searchHolder, gbc_but);
	buttons.setMaximumSize(buttons.getPreferredSize());

	toptop.add(buttons);
	toptop.add(Box.createHorizontalStrut(20));
	top.add(toptop);
	top.add(Box.createVerticalStrut(10));
	JPanel stripe = new JPanel();
	stripe.setLayout(new GridBagLayout());
	GridBagConstraints gbc = new GridBagConstraints();

	JPanel favorites = new JPanel();
	favorites.setLayout(new GridBagLayout());
	GridBagConstraints gbc2 = new GridBagConstraints();
	this.favoriteMenu = new JComboBox();
	this.favoriteMenu.setPreferredSize(new Dimension(150, (int)this.favoriteMenu.getPreferredSize().getHeight()));
	this.favoriteMenu.addItem("Favorites");
	this.favoriteMenu.setSelectedIndex(0);
	this.favoriteMenu.setEnabled(false);
	this.favoriteMenu.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		//if (e.getActionCommand() != "comboBoxChanged")
		//     return;
		if (MainWindow.this.ignoreFavoriteMenu)
		    return;
		Object o = MainWindow.this.favoriteMenu.getSelectedItem();
		if (!(o instanceof Integer)) {
		    // They must have selected "Favorites"
		    // So go reselect the current channel
		    if (MainWindow.this.currentChannelInfo == null)
			return;
		    Integer sid = new Integer(MainWindow.this.currentChannelInfo.getServiceID());
		    if (MainWindow.this.favoriteList.contains(sid)) {
			MainWindow.this.ignoreFavoriteMenu = true;
			MainWindow.this.favoriteMenu.setSelectedItem(sid);
			MainWindow.this.ignoreFavoriteMenu = false;
		    }
		    return;
		}
		Integer sid = (Integer)o;
		MainWindow.this.favoriteMenu.setSelectedIndex(0);
		ChannelInfo i = (ChannelInfo)MainWindow.this.channelList.get(sid);
		if (i == null)
		    return;
		MainWindow.this.setChannel(i.getChannelNumber());
	    }
	});
	this.favoriteMenu.setRenderer(new DefaultListCellRenderer() {
	    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		if (value instanceof Integer && MainWindow.this.channelList != null) {
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
	gbc2.weightx = 1;
	gbc2.anchor = GridBagConstraints.CENTER;
	gbc2.fill = GridBagConstraints.HORIZONTAL;
	favorites.add(this.favoriteMenu, gbc2);
	this.favoriteCheckbox = new JToggleButton(new ImageIcon(this.getClass().getResource("/images/no_heart.png")));
	this.favoriteCheckbox.setSelectedIcon(new ImageIcon(this.getClass().getResource("/images/heart.png")));
	this.favoriteCheckbox.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		Integer sid = new Integer(MainWindow.this.currentChannelInfo.getServiceID());
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
	gbc2.weightx = 0;
	gbc2.gridx = 1;
	gbc2.anchor = GridBagConstraints.CENTER;
	gbc2.fill = GridBagConstraints.NONE;
	favorites.add(this.favoriteCheckbox, gbc2);
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
	jl.setHorizontalAlignment(SwingConstants.CENTER);
	gbc1.gridx = 1;
	gbc1.weightx = 1;
	gbc1.anchor = GridBagConstraints.CENTER;
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
	
	//this.filterPanel = new FilterPanel(this);
	this.filterMenu = this.filterPanel.getFilterMenu();
	gbc.gridx = 2;
	gbc.weightx = 0;
	gbc.fill = GridBagConstraints.HORIZONTAL;
	gbc.anchor = GridBagConstraints.LINE_END;
	gbc.insets = new Insets(0, 0, 0, 20);
	gbc.fill = GridBagConstraints.NONE;
	this.filterMenu.setPreferredSize(new Dimension((int)favorites.getPreferredSize().getWidth(), (int)this.filterMenu.getPreferredSize().getHeight()));
	stripe.add(this.filterMenu, gbc);

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
	this.channelTable.setAutoCreateColumnsFromModel(false);
	this.channelTable.setShowHorizontalLines(false);
	this.channelTable.setShowVerticalLines(true);
	this.channelTable.setGridColor(gridColor);
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
		ChannelInfo info = new ChannelInfo(MainWindow.this.sortedChannelList[row]);

		JPopupMenu jpm = MainWindow.this.new ChannelPopupMenu(info);
		jpm.show(e.getComponent(), e.getX(), e.getY());
	    }
	});

	this.channelTableModel = new ChannelTableModel();
	channelTable.setModel(this.channelTableModel);

	class HeaderRenderer extends DefaultTableCellRenderer {
	    public HeaderRenderer() {
		setHorizontalAlignment(SwingConstants.CENTER);
		setOpaque(true);
		setBorder(UIManager.getBorder("TableHeader.cellBorder"));
	    }

	    public void updateUI() {
		super.updateUI();
		setBorder(UIManager.getBorder("TableHeader.cellBorder"));
	    }

	    public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {

		int modelColumn = table.getColumnModel().getColumn(column).getModelIndex();

		if (modelColumn == MainWindow.this.sortField) {
		    this.setForeground(table.getSelectionForeground());
		    this.setBackground(table.getSelectionBackground());
		    this.setHorizontalTextPosition(SwingConstants.LEADING);
		    this.setVerticalTextPosition(SwingConstants.CENTER);
		    this.setIcon(MainWindow.this.sortDirection?MainWindow.this.upArrow:MainWindow.this.downArrow);
		} else {
		    this.setForeground(UIManager.getColor("TableHeader.foreground"));
		    this.setBackground(UIManager.getColor("TableHeader.background"));
		    this.setIcon(null);
		}
		this.setFont(UIManager.getFont("TableHeader.font"));
		this.setValue(value);
		return this;
	    }
	}

	TableCellRenderer tcr = new HeaderRenderer();

	byte cols[];

	cols = JXM.myUserNode().getByteArray(CHAN_TABLE_COLS, null);

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
	// show stripes, and don't show a focus ring!
	class MyTableCellRenderer extends DefaultTableCellRenderer {
	    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		Component c = super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
		    if (!isSelected) {
		        c.setForeground(Color.BLACK);
		        c.setBackground((row % 2 == 0)?Color.WHITE:MainWindow.stripeColor);
		    } else {
			c.setForeground(MainWindow.this.channelTable.getSelectionForeground());
			c.setBackground(MainWindow.this.channelTable.getSelectionBackground());
		    }
		return c;
	    }
	}
	DefaultTableCellRenderer centered = new MyTableCellRenderer();
	centered.setHorizontalAlignment(SwingConstants.CENTER);
	DefaultTableCellRenderer plain = new MyTableCellRenderer();
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
		    tc.setCellRenderer(plain);
		    tc.setHeaderValue("Genre");
		    break;
		case 2:
		    tc = new TableColumn(2, 100, null, null);
		    tc.setMinWidth(100);
		    tc.setCellRenderer(plain);
		    tc.setHeaderValue("Name");
		    break;
		case 3:
		    tc = new TableColumn(3, 160, null, null);
		    tc.setMinWidth(160);
		    tc.setCellRenderer(plain);
		    tc.setHeaderValue("Artist");
		    break;
		case 4:
		    tc = new TableColumn(4, 160, null, null);
		    tc.setMinWidth(160);
		    tc.setCellRenderer(plain);
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
		if (MainWindow.this.ignoreSelectionChange)
		    return;
		// While he's dragging through the list, bar 'selectCurrentChannel()' from ripping
		// out from under him.
		MainWindow.this.disallowSelectionChange = e.getValueIsAdjusting();
		if (e.getValueIsAdjusting()) // not done yet
		    return;
		ListSelectionModel lsm = (ListSelectionModel)e.getSource();
		if (lsm.isSelectionEmpty()) {
		    // XXX can never happen
		} else {
		    int row = lsm.getMinSelectionIndex();
		    if (row >= MainWindow.this.sortedChannelList.length)
			return;
		    ChannelInfo i = (ChannelInfo)MainWindow.this.sortedChannelList[row];
		    // Don't bother doing anything if we're already there.
		    if (RadioCommander.theRadio().getChannel() != i.getChannelNumber())
			MainWindow.this.setChannel(i.getChannelNumber());
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
		JXM.myUserNode().putInt(SORT_FIELD, MainWindow.this.sortField);
		JXM.myUserNode().putBoolean(SORT_DIR, MainWindow.this.sortDirection);
		MainWindow.this.rebuildSortedChannelList();
		MainWindow.this.scrollToCurrentChannel();
		MainWindow.this.selectCurrentChannel();
	    }
	});

	this.sortField = JXM.myUserNode().getInt(SORT_FIELD, 0);
	if (this.sortField < 0 || this.sortField > 5)
	    this.sortField = 0;
	this.sortDirection = JXM.myUserNode().getBoolean(SORT_DIR, true);

	Dimension size = this.channelTable.getPreferredScrollableViewportSize();
	this.channelTable.setPreferredScrollableViewportSize(new Dimension(tw, size.height));

	channelTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	class StripedViewport extends JViewport {
	    public StripedViewport() {
		MainWindow.this.channelTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
		    public void valueChanged(ListSelectionEvent e) {
			StripedViewport.this.repaint();
		    }
		});
	    }
	    public void paint(Graphics g) {
		// Paint stripes into the table area not used, well, by the table
		int stripeHeight = MainWindow.this.channelTable.getRowHeight();
		int y = 0;

		// We may not be at the origin relative to the table. Grr.
		y -= this.getViewPosition().getY() % (stripeHeight * 2);

		while(y < this.getHeight()) {
		    g.setColor(Color.WHITE);
		    g.fillRect(0, y - 1, (int)this.getWidth(), stripeHeight + 1);
		    y += stripeHeight;
		    g.setColor(MainWindow.stripeColor);
		    g.fillRect(0, y, (int)this.getWidth(), stripeHeight - 1);
		    y += stripeHeight;
		}
		int selRow = MainWindow.this.channelTable.getSelectedRow();
		int selectedStripePos = selRow * stripeHeight;
		selectedStripePos -= this.getViewPosition().getY();
		if (selectedStripePos >= -(stripeHeight) && selectedStripePos <= this.getHeight()) {
		    g.setColor(MainWindow.this.channelTable.getSelectionBackground());
		    g.fillRect(0, selectedStripePos, (int)this.getWidth(), stripeHeight - 1);
		}

		g.setColor(MainWindow.gridColor);
		int so_far = -1; // XXX this is what looks best on a mac, at least.
		Enumeration e = MainWindow.this.channelTable.getColumnModel().getColumns();
		while(e.hasMoreElements()) {
		    TableColumn tc = (TableColumn)e.nextElement();
		    so_far += tc.getWidth();
		    g.drawLine(so_far, 0, so_far, this.getHeight());
		}
		
		super.paint(g);
	    }
	}
	JScrollPane sp = new JScrollPane();
	JViewport jvp = new StripedViewport();
	jvp.setOpaque(false);
	jvp.setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
	sp.setViewport(jvp);
	sp.setViewportView(this.channelTable);
	sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
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
		MainWindow.this.powerToggle();
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

	this.satelliteMeter = new SignalProgressBar(0, 100);
	gbc.gridx = 3;
	gbc.gridy = 0;
	gbc.weightx = .25;
	gbc.insets = new Insets(0, 0, 0, 20);
	gbc.anchor = GridBagConstraints.LINE_START;
	bottom.add(this.satelliteMeter, gbc);
	this.terrestrialMeter = new SignalProgressBar(0, 100);
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

	this.compactView = new CompactViewPanel();

        this.myFrame.setVisible(true);

	java.util.Timer t = new java.util.Timer();
	t.schedule(new TimerTask() {
	    public void run() {
		if (!RadioCommander.theRadio().isOn())
		    return;
		Integer sid;
		final double[] out;
		try {
		    out = RadioCommander.theRadio().getSignalStrength();
		}
		catch(RadioException ex) {
		    MainWindow.this.handleError(ex);
		    return;
		}
		// Must take the updates back to the UI thread
		SwingUtilities.invokeLater(new Runnable() {
		    public void run() {
			MainWindow.this.satelliteMeter.setValue((int)out[RadioCommander.SIGNAL_STRENGTH_SAT]);
			MainWindow.this.terrestrialMeter.setValue((int)out[RadioCommander.SIGNAL_STRENGTH_TER]);
		    }
		});
		if (MainWindow.this.currentChannelInfo == null)
		    return;
		sid = new Integer(MainWindow.this.currentChannelInfo.getServiceID());
		Integer ticks = (Integer)MainWindow.this.tickList.get(sid);
		if (ticks == null)
		    ticks = new Integer(0);
		ticks = new Integer(ticks.intValue() + 1);
		MainWindow.this.tickList.put(sid, ticks);
		if (MainWindow.this.sortField == 5) {
		    // If we're not sorting by percentage, then this could not have changed the order.
		    // Otherwise, it just might.
		    MainWindow.this.rebuildSortedChannelList();
		    MainWindow.this.selectCurrentChannel();
		} else
		    MainWindow.this.firePercentChanges();
	    }
	}, 1000, 1000);

	this.loadFavorites();

	this.loadTickList();
	this.memoryPanel = new MemoryPanel(this);

	int initialChannel = -1;
	String[] args = JXM.getCommandLine();
	if (args.length >= 1) {
	    try {
		initialChannel = Integer.parseInt(args[0]);
	    }
	    catch(NumberFormatException ex) {
		// ignore
	    }
	}
	// We have a device saved... Try and power up
	String deviceName = this.preferences.getDevice();
	if (deviceName != null)
	    this.turnPowerOn(initialChannel);

	new java.util.Timer().schedule(new TimerTask() {
	    public void run() {
		SwingUtilities.invokeLater(new Runnable() {
		    public void run() {
			MainWindow.this.aboutDialog.startupCheck();
		    }
		});
	    }
	}, 2500);
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

/*
    private Bookmark itmsButtonMark = new Bookmark("", "itms://phobos.apple.com/WebObjects/MZSearch.woa/wa/com.apple.jingle.search.DirectAction/advancedSearchResults?artistTerm={ARTIST}&songTerm={TITLE}");
    private void itmsButtonClicked() {
	this.bookmarkSurf(this.itmsButtonMark, this.currentChannelInfo);
    }
*/

    private Bookmark channelMark = new Bookmark("", "http://www.xmradio.com/programming/channel_page.jsp?ch={NUMBER}");
    private void surfToChannel(int chan) {
	if (this.currentChannelInfo == null)
	    return;
	this.bookmarkSurf(this.channelMark, this.currentChannelInfo);
    }

    private URL logoJar;
    private Icon findLogo(String filename) {
	// XXX At some point, we want to set up some infrastructure
	// to allow this to be auto-updated separately from the
	// application. For now, stuff it in the jar.

	URL logoURL;
	// we never got initialized. This means there is no logo jar
	// out there, so let's just use the logos dir built-in to the
	// app.
	if (this.logoJar == null) {
	    logoURL = this.getClass().getResource("/logos/" + filename);
	} else {
	    try {
		logoURL = new URL("jar:" + this.logoJar.toString() + "!/" + filename);
	    }
	    catch(MalformedURLException e) {
		return null;
	    }
	}
	if (logoURL == null)
	    return null;

	// Now go get the image. Note that we must be synchronous.
	// It is not an error for there to be no image for this filename.
	// just return null.
	ImageIcon img = new ImageIcon(logoURL);
	if (img.getImageLoadStatus() != MediaTracker.COMPLETE)
	    return null;
	return img;
    }
    MediaTracker myMT = new MediaTracker(this.channelLogo);
    private void setChannelLogo(int chan) {
	Icon logo = this.findLogo(chan + ".gif");
	if (logo == null) {
	    logo = this.findLogo("default.gif");
	}
	this.channelLogo.setIcon(logo);
    }

    private void toggleCompactView() {
	if (this.compactView.isVisible()) {
	    this.compactView.hide();
	    this.myFrame.show();
	} else {
	    this.myFrame.hide();
	    this.compactView.show();
	}
    }

    private void forceNormalView() {
	if (this.compactView.isVisible())
	    this.toggleCompactView();
    }
    private void muteClicked() {
	this.smartMuteButton.setSelected(false);
	boolean muteState;
	try {
	    muteState = RadioCommander.theRadio().isMuted();
	    if (muteState && this.smartMuteInfo != null) {
		// This is a smart-to-normal mute transition.
		// So don't invert the state
	    } else
		muteState = !muteState;
	    RadioCommander.theRadio().setMute(muteState);
	    this.smartMuteInfo = null;
	}
	catch(RadioException e) {
	    this.handleError(e);
	    return;
	}
	this.muteButton.setSelected(muteState);
    }

    ChannelInfo smartMuteInfo = null;
    private void smartMuteClicked() {
	this.muteButton.setSelected(false);
	boolean muteState;
	ChannelInfo i = null;
	try {
	    muteState = RadioCommander.theRadio().isMuted();
	    RadioCommander.theRadio().setMute(muteState);
	    if (muteState && this.smartMuteInfo == null) {
		// This is a normal-to-smart mute transition
		// So don't invert the state
	    } else
		muteState = !muteState;
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
	    case PlatformFactory.PLAT_CB_SMART_MUTE:	this.smartMuteClicked(); break;
	    case PlatformFactory.PLAT_CB_NORM_MUTE:	this.muteClicked(); break;
	    case PlatformFactory.PLAT_CB_CHANNEL:	this.setChannel(((Integer)arg).intValue()); break;
	    case PlatformFactory.PLAT_CB_MEMORY:	this.memoryPanel.memorize((ChannelInfo)arg); break;
	    default: throw new IllegalArgumentException("Which platform callback type??");
	}
    }
    public boolean radioIsOn() { return RadioCommander.theRadio().isOn(); }
    public int getMuteState() {
	if (!RadioCommander.theRadio().isMuted())
	    return 0;
	if (this.smartMuteInfo != null)
	    return PlatformFactory.SMART_MUTE_ON;
	else
	    return PlatformFactory.NORM_MUTE_ON;
    }
    public ChannelInfo getChannelInfo() { return this.currentChannelInfo; }
    public Favorite[] getFavorites() {
	ArrayList l = new ArrayList();
	Iterator i = this.favoriteList.iterator();
	while (i.hasNext()) {
	    Integer sid = (Integer)i.next();
	    ChannelInfo info = (ChannelInfo)this.channelList.get(sid);
	    if (info == null)
		continue;
	    l.add(new Favorite(info.getChannelNumber(), Integer.toString(info.getChannelNumber()) + " - " + info.getChannelName()));
	}
	Collections.sort(l, new Comparator() {
	    public int compare(Object o1, Object o2) {
		Favorite i1 = (Favorite)o1;
		Favorite i2 = (Favorite)o2;
		return new Integer(i1.getChannelNumber()).compareTo(new Integer(i2.getChannelNumber()));
	    }
	});
	return (Favorite[])l.toArray(new Favorite[0]);
    }
    public Bookmark[] getBookmarks() { return this.bookmarks; }

    // the RadioEventHandler interface
    public void notify(RadioCommander theRadio, final int type, final Object item) {
	// This must be handled immediately to make sure we close the Tracker when quitting.
	if (type == RadioCommander.POWERED_OFF) {
	    this.poweredDown();
	    return;
	}
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
			case RadioCommander.SONG_TIME_UPDATE:
				MainWindow.this.channelSongTime((RadioCommander.SongTiming)item);
				break;
			case RadioCommander.ACTIVATION_CHANGED:
				// XXX ignore for now
				break;
			default:
				throw new IllegalArgumentException("Which kind of notification?");
		    }
		}
	    });
    }

    private int doNotSelectChannelsUntil = -1;
    private void setChannel(int chan) {
	this.doNotSelectChannelsUntil = chan;
	try {
	    RadioCommander.theRadio().setChannel(chan);
	}
	catch(RadioException e) {
	    this.handleError(e);
	}
    }

    public JFrame getFrame() { return this.myFrame; }

    private void turnPowerOff() {
	try {
	    RadioCommander.theRadio().turnOff();
	}
	catch(RadioException e) {
	    this.handleError(e);
	}
    }

    private void powerToggle() {
	if (!RadioCommander.theRadio().isOn())
	    MainWindow.this.turnPowerOn();
	else
	    MainWindow.this.turnPowerOff();
    }

    private void turnPowerOn() {
	this.turnPowerOn(-1);
    }
    private void turnPowerOn(int initialChannel) {
	// Figure out which device was selected
	String deviceName = this.preferences.getDevice();
	if (deviceName == null) {
	    this.powerCheckBox.setSelected(false);
	    JOptionPane.showMessageDialog(this.myFrame, "Please pick a device before powering up.", "No device selected", JOptionPane.ERROR_MESSAGE);
	    this.preferences.showTab(PreferencesDialog.TAB_DEVICE);
	    return;
	}
	// Attempt to power up the radio
	try {
	    RadioCommander.theRadio().registerEventHandler(this);
	    RadioCommander.theRadio().turnOn(deviceName, initialChannel);
	}
	catch(RadioException e) {
	    RadioCommander.theRadio().unregisterEventHandler(this);
	    this.powerCheckBox.setSelected(false);
	    this.handleError(e);
	    return;
	}
	//this.poweredUp();
    }

    private final static String CHAN_TABLE_COLS = "ChannelTableColumnOrder";
    private final static String GRID_NODE = "ChannelGrid";
    private final static String TICK_NODE = "ChannelUsage";
    private final static String SORT_DIR = "SortDirection";
    private final static String SORT_FIELD = "SortColumn";
    private final static String FAVORITE_LIST = "FavoriteChannels";

    private void poweredUp() {
	this.loadChannelList();
	this.rebuildSortedChannelList();
	this.compactView.refreshSpinnerModel(); // we need to force this once
	this.muteButton.setEnabled(true);
	this.smartMuteButton.setEnabled(true);
	//this.itmsButton.setEnabled(true);
	this.memoryButton.setEnabled(true);
	this.muteButton.setSelected(false);
	this.smartMuteButton.setSelected(false);
	this.smartMuteInfo = null;
	this.powerCheckBox.setSelected(true);
	this.rebuildFavoritesMenu();
	this.favoriteCheckbox.setEnabled(true);
	//this.filterMenuItem.setEnabled(true);
	this.filterMenu.setEnabled(this.filterMenu.getItemCount() > 1);
	this.searchField.setEnabled(true);
	this.searchFieldClear.setEnabled(true);
	this.compactMenuItem.setEnabled(true);
	this.preferences.saveDevice();
	this.bookmarkMenu.setEnabled(true);
	this.powerCheckBox.setSelected(true);
	this.powerMenuItem.setText("Turn Radio Off");
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
	// If the list is empty right now, then don't do an update, just in case.
	if (this.channelList.size() != 0)
	    this.saveChannelList();
	SwingUtilities.invokeLater(new Runnable() {
	    public void run() {
		MainWindow.this.searchSystem.update(null);
		MainWindow.this.channelList.clear();
		MainWindow.this.sortedChannelList = new ChannelInfo[0];
		MainWindow.this.channelTableModel.fireTableDataChanged();
		MainWindow.this.nowPlayingPanel.setChannelInfo(null);
		MainWindow.this.compactView.setChannelInfo(null);
		MainWindow.this.nowPlayingPanel.setSongTime(null, null);
		MainWindow.this.powerCheckBox.setSelected(false);
		MainWindow.this.searchFieldClear.setEnabled(false);
		MainWindow.this.muteButton.setEnabled(false);
		MainWindow.this.smartMuteButton.setEnabled(false);
		//MainWindow.this.filterMenuItem.setEnabled(false);
		MainWindow.this.filterMenu.setEnabled(false);
		MainWindow.this.compactMenuItem.setEnabled(false);
		MainWindow.this.forceNormalView();
		MainWindow.this.powerCheckBox.setSelected(false);
		MainWindow.this.powerMenuItem.setText("Turn Radio On");
		MainWindow.this.searchField.setEnabled(false);
		//MainWindow.this.itmsButton.setEnabled(false);
		MainWindow.this.memoryButton.setEnabled(false);
		MainWindow.this.muteButton.setSelected(false);
		MainWindow.this.smartMuteButton.setSelected(false);
		MainWindow.this.satelliteMeter.setValue(0);
		MainWindow.this.terrestrialMeter.setValue(0);
		MainWindow.this.setChannelLogo(-1);
		MainWindow.this.favoriteMenu.setEnabled(false);
		MainWindow.this.favoriteMenu.setSelectedIndex(0);
		MainWindow.this.favoriteCheckbox.setEnabled(false);
		MainWindow.this.bookmarkMenu.setEnabled(false);
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

    private void channelSongTime(RadioCommander.SongTiming t) {
	this.nowPlayingPanel.setSongTime(t.start(), t.end());
    }

    private void channelChanged() {
	int channel = RadioCommander.theRadio().getChannel();

	this.setChannelLogo(channel);

	final Integer sid = new Integer(this.sidForChannel(channel));
	this.currentChannelInfo = (ChannelInfo)this.channelList.get(sid);
	this.nowPlayingPanel.setChannelInfo(this.currentChannelInfo);
	this.compactView.setChannelInfo(this.currentChannelInfo);
	this.nowPlayingPanel.setSongTime(null, null);
	boolean isFavorite = this.favoriteList.contains(sid);
	this.favoriteCheckbox.setSelected(isFavorite);
	MainWindow.this.ignoreFavoriteMenu = true;
	try {
	    if (isFavorite) {
		this.favoriteMenu.setSelectedItem(sid);
	    } else {
		this.favoriteMenu.setSelectedIndex(0);
	    }
	}
	finally {
	    MainWindow.this.ignoreFavoriteMenu = false;
	}
	this.scrollToCurrentChannel();
    }

    private Map channelList = Collections.synchronizedMap(new HashMap());

    private void deleteChannel(int chan) {
	int sid;
	Iterator i = this.channelList.values().iterator();
	while(i.hasNext()) {
	    ChannelInfo info = (ChannelInfo)i.next();
	    if (info.getChannelNumber() == chan) {
		i.remove();
		if (this.favoriteList.contains(new Integer(info.getServiceID())))
		    this.rebuildFavoritesMenu();
	    }
	}
    }

    private boolean ignoreFavoriteMenu = false;
    private boolean disallowSelectionChange = false;
    private boolean ignoreSelectionChange = false;
    private void selectCurrentChannel() {
	if (!RadioCommander.theRadio().isOn()) // How can this happen?!
	    return;
	if (this.disallowSelectionChange)
	    return;
	if (this.doNotSelectChannelsUntil >= 0 && this.doNotSelectChannelsUntil != this.currentChannelInfo.getChannelNumber())
	    return;
	this.doNotSelectChannelsUntil = -1;
	this.ignoreSelectionChange = true;
	try {
	if (this.currentChannelInfo == null)
	    return;
	int row = this.rowForSID(this.currentChannelInfo.getServiceID());
	if (row < 0)
	    return;
	this.channelTable.addRowSelectionInterval(row, row);
	}
	finally {
	    this.ignoreSelectionChange = false;
	}
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
	this.ignoreFavoriteMenu = true;
	try {
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
	if (this.currentChannelInfo == null || !RadioCommander.theRadio().isOn()) {
	    return;
	}
	Integer sid = new Integer(this.currentChannelInfo.getServiceID());
	if (this.favoriteList.contains(sid)) {
	    this.favoriteMenu.setSelectedItem(sid);
	} else {
	    this.favoriteMenu.setSelectedIndex(0);
	}
	}
	finally {
	    this.ignoreFavoriteMenu = false;
	}
    }

    HashSet favoriteList = new HashSet();

    private void loadFavorites() {
	this.favoriteList.clear();
	byte[] list = JXM.myUserNode().getByteArray(FAVORITE_LIST, new byte[0]);
	for(int i = 0; i < list.length; i++)
	    this.favoriteList.add(new Integer(list[i] & 0xff));
    }
    private void saveFavorites() {
	byte[] list = new byte[this.favoriteList.size()];
	int n = 0;
	Iterator i = this.favoriteList.iterator();
	while(i.hasNext())
	    list[n++] = (byte)((Integer)i.next()).intValue();
	JXM.myUserNode().putByteArray(FAVORITE_LIST, list);
    }

    private void saveChannelList() {
	Preferences node = JXM.myUserNode().node(GRID_NODE);
	try {
	    node.clear();
	}
	catch(BackingStoreException e) {
	    return;
	}
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
    }
    private void saveTickList() {
	Preferences node = JXM.myUserNode().node(TICK_NODE);
	try {
	    node.clear();
	}
	catch(BackingStoreException e) {
	    return;
	}
	if (this.tickList == null)
	    return;
	Iterator i = this.tickList.keySet().iterator();
	while(i.hasNext()) {
	    Integer sid = (Integer)i.next();
	    int ticks = ((Integer)this.tickList.get(sid)).intValue();
	    node.putInt(sid.toString(), ticks);
	}
    }

    private void loadChannelList() {
	this.channelList.clear();
	Preferences node = JXM.myUserNode().node(GRID_NODE);
	String[] keys;
	try {
	    keys = node.keys();
	}
	catch(BackingStoreException e) {
	    // ignore
	    return;
	}
	for(int i = 0; i < keys.length; i++) {
	    String val = node.get(keys[i], null);
	    if (val == null)
		continue;
	    String[] val_list =  val.split(":", -1);
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
	int[] chanList = new int[this.channelList.size()];
	Iterator it = this.channelList.values().iterator();
	int i = 0;
	while(it.hasNext()) {
	    chanList[i++] = ((ChannelInfo)it.next()).getChannelNumber();
	}
	RadioCommander.theRadio().setChannelList(chanList);
    }
    private void loadTickList() {
	Preferences node = JXM.myUserNode().node(TICK_NODE);
	String[] keys;
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
    }

    private ChannelInfo ratingChannelInfo;

    private void updateRatingSlider(final ChannelInfo newInfo) {
	final ChannelInfo toRate = this.ratingChannelInfo;
	boolean disable_it = false;

	if (newInfo != null) {
	    if (newInfo.equals(this.ratingChannelInfo))
		return; // it hasn't yet changed

	    disable_it = (newInfo.getChannelArtist().length() == 0 || newInfo.getChannelTitle().length() == 0);

	} else {
	    // We're powering down. Disable the slider when we're done
	    disable_it = true;
	}
	// We have new info - make a note for the next transition
	this.ratingChannelInfo = newInfo;

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
	this.aboutDialog.updateUserID(this.lastRecordedUserID);
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

    private synchronized void update(final ChannelInfo i) {
	// Because of the synchronization, it's possible for this to be called after power-off.
	if (!RadioCommander.theRadio().isOn())
	    return;

	// Ok, Mark, you get your way.
	// Occasionally, usually shortly after powerup, the radio will lose track
	// of the channel genre and name. If they're blank, then we will prefer the
	// cache.
	if (i.getChannelName().length() == 0) {
	    ChannelInfo cache = (ChannelInfo)this.channelList.get(new Integer(i.getServiceID()));
	    if (cache != null)
		i.setChannelName(cache.getChannelName());
	}
	if (i.getChannelGenre().length() == 0) {
	    ChannelInfo cache = (ChannelInfo)this.channelList.get(new Integer(i.getServiceID()));
	    if (cache != null)
		i.setChannelGenre(cache.getChannelGenre());
	}

	// If this update doesn't actually *change* anything, then
	// just ignore it.
	if (i.equals(this.channelList.get(new Integer(i.getServiceID()))))
	    return;

	this.searchSystem.update(i);

	// We got an update. Is it a new favorite? If so, remember that for later.
	boolean newFav =  (!this.channelList.containsKey(new Integer(i.getServiceID()))) && this.favoriteList.contains(new Integer(i.getServiceID()));
	// We got an update. First, we file it, firing table update events
	// while we're at it.
	this.channelList.put(new Integer(i.getServiceID()), i);
	if (newFav)
	    this.rebuildFavoritesMenu();

	// Alas, if we're sorting on artist or title, then any update could dirty the sort list
	this.rebuildSortedChannelList();
	//int row = this.rowForSID(i.getServiceID());
	//this.channelTableModel.fireTableRowsUpdated(row, row);

	this.selectCurrentChannel();

	if (RadioCommander.theRadio().getChannel() == i.getChannelNumber()) {
	    // This update is for the current channel
	    this.currentChannelInfo = i;
	    // update the favorite checkbox
	    Integer sid = new Integer(i.getServiceID());
	    boolean isFavorite = this.favoriteList.contains(sid);
	    this.favoriteCheckbox.setSelected(isFavorite);
	    this.ignoreFavoriteMenu = true;
	    try{
		if (isFavorite) {
		    this.favoriteMenu.setSelectedItem(sid);
		} else {
		    this.favoriteMenu.setSelectedIndex(0);
		}
	    }
	    finally {
		this.ignoreFavoriteMenu = false;
	    }
	    // update the rating slider
	    this.updateRatingSlider(i);
	    // This is an update for the current channel - fix the labels.
	    this.nowPlayingPanel.setChannelInfo(i);
	    this.compactView.setChannelInfo(i);
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

    private void scrollToCurrentChannel() {
	int row = this.rowForSID(RadioCommander.theRadio().getServiceID());
	if (row < 0)
	    return;
	Rectangle rect = this.channelTable.getCellRect(row, 0, true);
	this.channelTable.scrollRectToVisible(rect);
    }

}
