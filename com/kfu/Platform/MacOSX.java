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

package com.kfu.Platform;

import java.io.*;

import com.kfu.JXM.*;

import com.apple.eawt.*;
import com.apple.eio.*;

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
    }

}
