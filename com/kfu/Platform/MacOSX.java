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

 $Id: MacOSX.java,v 1.14 2007/11/25 17:27:01 nsayer Exp $
 
 */

package com.kfu.Platform;

import java.io.*;

import com.kfu.JXM.*;

import com.kfu.xm.*;

import com.apple.eawt.*;
import com.apple.eio.*;

import com.apple.cocoa.application.*;
import com.apple.cocoa.foundation.*;

public class MacOSX implements IPlatformHandler {

    static {
	System.loadLibrary("MacOSX");
    }

    private static native String getBundleNameForDevice(String filename);

    private IPlatformCallbackHandler cb;

    public boolean useMacMenus() { return true; }

    public boolean needsBrowserPath() { return false; }
    public void setBrowserPath(String ignore) { throw new IllegalArgumentException("We don't care"); }

    public void openURL(String url) throws IOException {
	FileManager.openURL(url);
    }

    public void registerCallbackHandler(IPlatformCallbackHandler notifier) { this.cb = notifier; }

    // return true if we know that the devices really are FTDI chips.
    // If there is only one potential device, and this method returns true,
    // then the radio will turn on at startup.
    public boolean devicesAreFiltered() { return true; }

    public boolean isDeviceValid(String devname) {
	// Eliminate the /dev/-less names and the CU devices

	if (!devname.startsWith("/dev/tty."))
	    return false;

	String driverBundle = getBundleNameForDevice(devname);
	if (driverBundle == null || !driverBundle.startsWith("com.FTDI."))
	    return false;

	return true;
    }

    public MacOSX() throws Exception {
	if (System.getProperty("mrj.version") == null)
	    throw new Exception("This isn't a macintosh");


	Application app = new Application();

	app.addAboutMenuItem();
	app.setEnabledAboutMenu(true);
	app.addPreferencesMenuItem();
	app.setEnabledPreferencesMenu(true);
	app.addApplicationListener(new ApplicationAdapter() {
	    public void handleAbout(ApplicationEvent e) {
		MacOSX.this.cb.platformNotify(PlatformFactory.PLAT_CB_ABOUT, null);
		e.setHandled(true);
	    }
	    public void handlePreferences(ApplicationEvent e) {
		MacOSX.this.cb.platformNotify(PlatformFactory.PLAT_CB_PREFS, null);
		e.setHandled(true);
	    }
	    public void handleQuit(ApplicationEvent e) {
		MacOSX.this.cb.platformNotify(PlatformFactory.PLAT_CB_QUIT, null);
		e.setHandled(true);
	    }
	});

	System.setProperty("com.apple.mrj.application.apple.menu.about.name", "JXM");

	NSApplication.sharedApplication().setDelegate(this);
    }

    ChannelInfo menuInfo;

    public NSMenu applicationDockMenu(NSApplication sender) {

	if (!this.cb.radioIsOn())
	    return null;

	NSMenu out = new NSMenu();
	NSMenuItem nmi;

	this.menuInfo = this.cb.getChannelInfo();
	if (this.menuInfo != null) {
	    nmi = new NSMenuItem(Integer.toString(this.menuInfo.getChannelNumber()) + " - " + this.menuInfo.getChannelName(), null, "");
	    out.addItem(nmi);
	    nmi = new NSMenuItem(this.menuInfo.getChannelArtist(), null, "");
	    out.addItem(nmi);
	    nmi = new NSMenuItem(this.menuInfo.getChannelTitle(), null, "");
	    out.addItem(nmi);
	}

	nmi = new NSMenuItem().separatorItem();
	out.addItem(nmi);

	// Add the bookmarks here
	Bookmark[] marks = this.cb.getBookmarks();
	NSMenu bookmarks = new NSMenu("Bookmarks");
	for(int i = 0; i < marks.length; i++) {
	    nmi = new NSMenuItem(marks[i].getName(), new NSSelector("bookmarkMenuClicked", new Class[] {NSObject.class}), "");
	    nmi.setTarget(this);
	    nmi.setRepresentedObject(marks[i]);
	    bookmarks.addItem(nmi);
	}
	nmi = new NSMenuItem("Bookmarks", null, "");
	out.addItem(nmi);
	out.setSubmenuForItem(bookmarks, nmi);

	nmi = new NSMenuItem().separatorItem();
	out.addItem(nmi);

	// Add the favorites here
	Favorite[] favs = this.cb.getFavorites();
	NSMenu favorites = new NSMenu("Favorites");
	for(int i = 0; i < favs.length; i++) {
	    nmi = new NSMenuItem(favs[i].getMenuString(), new NSSelector("favoriteMenuClicked", new Class[] {NSObject.class}), "");
	    nmi.setTarget(this);
	    nmi.setRepresentedObject(new Integer(favs[i].getChannelNumber()));
	    favorites.addItem(nmi);
        }
	nmi = new NSMenuItem("Favorites", null, "");
	out.addItem(nmi);
	out.setSubmenuForItem(favorites, nmi);

	nmi = new NSMenuItem().separatorItem();
	out.addItem(nmi);

	nmi = new NSMenuItem("Add to notebook", new NSSelector("memoryMenuClicked", new Class[] {NSObject.class}), "");
	nmi.setTarget(this);
	out.addItem(nmi);
	nmi = new NSMenuItem().separatorItem();
	out.addItem(nmi);

	int muteState = this.cb.getMuteState();
	nmi = new NSMenuItem("Mute", new NSSelector("normMuteClicked", new Class[] {NSObject.class}), "");
	nmi.setTarget(this);
	nmi.setState((muteState == PlatformFactory.NORM_MUTE_ON)?NSCell.OnState:NSCell.OffState);
	out.addItem(nmi);
	nmi = new NSMenuItem("Smart Mute", new NSSelector("smartMuteClicked", new Class[] {NSObject.class}), "");
	nmi.setTarget(this);
	nmi.setState((muteState == PlatformFactory.SMART_MUTE_ON)?NSCell.OnState:NSCell.OffState);
	out.addItem(nmi);

	return out;
    }

    // Stupid Apple. If we add a NSApplication delegate, the registration of the
    // quit handler stops working.
    public void applicationShouldTerminate(NSObject sender) {
	this.cb.platformNotify(PlatformFactory.PLAT_CB_QUIT, null);
    }

    public void normMuteClicked(NSObject sender) {
	this.cb.platformNotify(PlatformFactory.PLAT_CB_NORM_MUTE, null);
    }
    public void smartMuteClicked(NSObject sender) {
	this.cb.platformNotify(PlatformFactory.PLAT_CB_SMART_MUTE, null);
    }
    public void bookmarkMenuClicked(NSObject sender) {
	NSMenuItem nmi = (NSMenuItem)sender;
	Bookmark b = (Bookmark)nmi.representedObject();
	this.menuInfo = this.menuInfo;
	if (this.menuInfo == null)
	    return;
	try {
	    b.surf(this.menuInfo);
	}
	catch(IOException ex) {
	    // XXX what to do?
	}
    }
    public void favoriteMenuClicked(NSObject sender) {
	NSMenuItem nmi = (NSMenuItem)sender;
	Integer i = (Integer)nmi.representedObject();
	this.cb.platformNotify(PlatformFactory.PLAT_CB_CHANNEL, i);
    }
    public void memoryMenuClicked(NSObject sender) {
	this.cb.platformNotify(PlatformFactory.PLAT_CB_MEMORY, this.menuInfo);
    }
    public void quit() {}
}
