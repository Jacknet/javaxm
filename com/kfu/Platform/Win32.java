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

 $Id: Win32.java,v 1.7 2004/04/10 17:44:46 nsayer Exp $
 
 */

package com.kfu.Platform;

import java.awt.event.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

import com.kfu.xm.*;

import com.ice.jni.registry.*;

import com.jeans.trayicon.*;

import edu.stanford.ejalbert.BrowserLauncher;

import com.kfu.JXM.*;

public class Win32 implements IPlatformHandler {

    public boolean useMacMenus() { return false; }

    public boolean needsBrowserPath() { return false; }
    public void setBrowserPath(String ignore) { throw new IllegalArgumentException("We don't care"); }
    public void openURL(String url) throws IOException {
	BrowserLauncher.openURL(url);
    }

    // return true if we know that the devices really are FTDI chips.
    // If there is only one potential device, and this method returns true,
    // then the radio will turn on at startup.
    public boolean devicesAreFiltered() { return !this.crappyWindowsVersion; }

    public boolean isDeviceValid(String in) {
	if (this.crappyWindowsVersion)
	    return true;
	if (this.ftdiDevices == null)
	    this.getFTDIlist();
	return this.ftdiDevices.contains(in);
    }

    public void registerCallbackHandler(IPlatformCallbackHandler cb) { this.cb = cb; }

    private IPlatformCallbackHandler cb;

    private ArrayList ftdiDevices;

    private boolean crappyWindowsVersion; // any DOS based Windows: win95, 98, 98SE, ME

    private WindowsTrayIcon trayIcon;

    public Win32() throws Exception {
	String osName = System.getProperty("os.name").toLowerCase();
	if (!osName.startsWith("windows"))
	    throw new Exception("We're not using Windows! Yay!");

	this.crappyWindowsVersion = (osName.indexOf("9") >= 0 || osName.indexOf("me") >= 0);
	WindowsTrayIcon.initTrayIcon("JXM");
	ImageIcon ii = new ImageIcon(this.getClass().getResource("/images/trayicon.png"));
	this.trayIcon = new WindowsTrayIcon(ii.getImage(), 16, 16);
	this.trayIcon.setVisible(true);
	new TrayIconMenu().setTrayIcon(this.trayIcon);
    }

    private class TrayIconMenu extends SwingTrayPopup {
	public void showMenu(int x, int y) {
	    this.rebuild();
	    super.showMenu(x, y);
	}

	private JMenuItem chanItem, artistItem, titleItem, memoryItem;
	private JCheckBoxMenuItem muteItem, smartMuteItem;
	private JMenu bookmarkMenu, favoriteMenu;

	private ChannelInfo menuInfo;

	private void rebuild() {
	    if (!Win32.this.cb.radioIsOn() || (menuInfo = Win32.this.cb.getChannelInfo()) == null) {
		this.chanItem.setText("");
		this.artistItem.setText("");
		this.titleItem.setText("");
		this.setEnabled(false); // why isn't this sufficient?
		this.muteItem.setEnabled(false);
		this.muteItem.setState(false);
		this.smartMuteItem.setEnabled(false);
		this.smartMuteItem.setState(false);
		this.memoryItem.setEnabled(false);
		this.bookmarkMenu.removeAll();
		this.favoriteMenu.removeAll();
		return;
	    }
	    this.chanItem.setText(Integer.toString(menuInfo.getChannelNumber()) + " - " + menuInfo.getChannelName());
	    this.artistItem.setText(menuInfo.getChannelArtist());
	    this.titleItem.setText(menuInfo.getChannelTitle());
	    this.setEnabled(true);

	    int muteState = Win32.this.cb.getMuteState();
	    this.muteItem.setState(muteState == PlatformFactory.NORM_MUTE_ON);
	    this.smartMuteItem.setState(muteState == PlatformFactory.SMART_MUTE_ON);
	    this.muteItem.setEnabled(true);
	    this.smartMuteItem.setEnabled(true);
	    this.memoryItem.setEnabled(true);

	    // rebuild the bookmark menu
	    Bookmark[] marks = Win32.this.cb.getBookmarks();
	    this.bookmarkMenu.removeAll();
	    for(int i = 0; i < marks.length; i++) {
		final Bookmark mark = marks[i];
		JMenuItem jmi = new JMenuItem(mark.getName());
		jmi.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			try {
			    mark.surf(menuInfo);
			}
			catch(IOException ex) {
			    // now what?
			}
		    }
		});
		this.bookmarkMenu.add(jmi);
	    }

	    // rebuild the favorites menu
	    Favorite[] favs = Win32.this.cb.getFavorites();
	    this.favoriteMenu.removeAll();
	    for(int i = 0; i < favs.length; i++) {
		final Favorite fav = favs[i];
		JMenuItem jmi = new JMenuItem(fav.getMenuString());
		jmi.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			Win32.this.cb.platformNotify(PlatformFactory.PLAT_CB_CHANNEL, new Integer(fav.getChannelNumber()));
		    }
		});
		this.favoriteMenu.add(jmi);
	    }
	}

	public TrayIconMenu() {
	    this.chanItem = new JMenuItem("");
	    this.chanItem.setEnabled(false);
	    this.add(this.chanItem);
	    this.artistItem = new JMenuItem("");
	    this.artistItem.setEnabled(false);
	    this.add(this.artistItem);
	    this.titleItem = new JMenuItem("");
	    this.titleItem.setEnabled(false);
	    this.add(this.titleItem);
	    this.addSeparator();

	    this.bookmarkMenu = new JMenu("Bookmarks");
	    this.add(this.bookmarkMenu);
	    this.addSeparator();

	    this.favoriteMenu = new JMenu("Favorites");
	    this.add(this.favoriteMenu);
	    this.addSeparator();

	    this.memoryItem = new JMenuItem("Add to notebook");
	    this.memoryItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    Win32.this.cb.platformNotify(PlatformFactory.PLAT_CB_MEMORY, menuInfo);
		}
	    });
	    this.memoryItem.setEnabled(false);
	    this.add(this.memoryItem);
	    this.addSeparator();

	    this.muteItem = new JCheckBoxMenuItem("Mute");
	    this.muteItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    Win32.this.cb.platformNotify(PlatformFactory.PLAT_CB_NORM_MUTE, null);
		}
	    });
	    this.add(this.muteItem);

	    this.smartMuteItem = new JCheckBoxMenuItem("Smart Mute");
	    this.smartMuteItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    Win32.this.cb.platformNotify(PlatformFactory.PLAT_CB_SMART_MUTE, null);
		}
	    });
	    this.add(this.smartMuteItem);

	    this.setEnabled(false);
	}
    }

    private void getFTDIlist() {
	if (this.crappyWindowsVersion)
	    throw new IllegalStateException("Can't do device filtering on win9x/winME");

	this.ftdiDevices = new ArrayList();
	try {
	    // Paw through the registry looking for FTDI serial ports.
	    RegistryKey currentControlSet = Registry.HKEY_LOCAL_MACHINE.openSubKey("System\\CurrentControlSet");

	    RegistryKey rk = currentControlSet.openSubKey("Services\\serenum\\Enum");
	    Enumeration e = rk.valueElements();
	    while(e.hasMoreElements()) {
		String key = (String)e.nextElement();
		try {
		    Integer.parseInt(key);
		}
		catch(NumberFormatException ex) {
		// If the value name is not an integer, then skip it.
		continue;
		}
		String deviceEnum = ((RegStringValue)rk.getValue(key)).getData();
		RegistryKey rk2 = currentControlSet.openSubKey("Enum\\" + deviceEnum);
		String Mfg = ((RegStringValue)rk2.getValue("Mfg")).getData();
		if (!Mfg.equals("FTDI"))
		    continue;
		// We now have a confirmed FTDI port. Find it's "COM" name
		rk2 = rk2.openSubKey("Device Parameters");
		String portName = ((RegStringValue)rk2.getValue("PortName")).getData();
		this.ftdiDevices.add(portName);
	    }
	}
	catch(Exception ex) {
	    // ignore
	}
    }

    public void quit() {
	WindowsTrayIcon.cleanUp();
    }
}
