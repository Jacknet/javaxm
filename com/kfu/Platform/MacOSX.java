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

 $Id: MacOSX.java,v 1.4 2004/03/10 03:40:19 nsayer Exp $
 
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

    private IPlatformCallbackHandler cb;

    public boolean useMacMenus() { return true; }

    public boolean needsBrowserPath() { return false; }
    public void setBrowserPath(String ignore) { throw new IllegalArgumentException("We don't care"); }
    public void openURL(String url) throws IOException {
	FileManager.openURL(url);
    }

    public void registerCallbackHandler(IPlatformCallbackHandler notifier) { this.cb = notifier; }

    public boolean isDeviceValid(String devname) {
	// Eliminate the /dev/-less names and the CU devices
	return devname.startsWith("/dev/tty.");
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

    public NSMenu applicationDockMenu(NSApplication sender) {

	if (!this.cb.radioIsOn())
	    return null;
	NSMenu out = new NSMenu();
	NSMenuItem nmi;

	ChannelInfo info = this.cb.getChannelInfo();
	if (info != null) {
	    nmi = new NSMenuItem(Integer.toString(info.getChannelNumber()) + " - " + info.getChannelName(), null, "");
	    out.addItem(nmi);
	    nmi = new NSMenuItem(info.getChannelArtist(), null, "");
	    out.addItem(nmi);
	    nmi = new NSMenuItem(info.getChannelTitle(), null, "");
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
	ChannelInfo info = this.cb.getChannelInfo();
	if (info == null)
	    return;
	try {
	    b.surf(info);
	}
	catch(IOException ex) {
	    // what?
	}
    }
    public void favoriteMenuClicked(NSObject sender) {
	NSMenuItem nmi = (NSMenuItem)sender;
	Integer i = (Integer)nmi.representedObject();
	this.cb.platformNotify(PlatformFactory.PLAT_CB_CHANNEL, i);
    }
}
