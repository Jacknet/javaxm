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

 $Id: PlatformFactory.java,v 1.7 2004/03/31 19:49:17 nsayer Exp $
 
 */

package com.kfu.JXM;

import java.lang.reflect.*;

public class PlatformFactory {

    public static final int PLAT_CB_PREFS = 0; // User requested "Preferences"
    public static final int PLAT_CB_ABOUT = 1; // User requested "About"
    public static final int PLAT_CB_QUIT = 2; // User requested "Quit"
    public static final int PLAT_CB_SMART_MUTE = 3; // User requested "Smart Mute"
    public static final int PLAT_CB_NORM_MUTE = 4; // User requested "Normal Mute"
    public static final int PLAT_CB_CHANNEL = 5; // User requested channel change
    public static final int PLAT_CB_MEMORY = 6; // User requested add channel info to memory

    public static final int SMART_MUTE_ON = 2;
    public static final int NORM_MUTE_ON = 1;

    private static String platformClassList[] = {
	"com.kfu.Platform.MacOSX",
	"com.kfu.Platform.Win32",
	"com.kfu.Platform.unknown",
    };

    private static IPlatformHandler platform = null;

    public static synchronized IPlatformHandler ourPlatform() {
	if (platform == null)
	    setUpPlatform();

	return platform;
    }

    private static void setUpPlatform() {
	for(int i = 0; i < platformClassList.length; i++) {
	    try {
		Class cl = Class.forName(platformClassList[i]);
		if (cl == null)
		    continue;
		Constructor co = cl.getConstructor(new Class[0]);
		Object o = co.newInstance(new Object[0]);
		platform = (IPlatformHandler)o;
		return; // First one wins
	    }
	    catch(Throwable t) {
		// Attempting to touch platform handdlers on the wrong
		// platform is likely to cough up all sorts of bad things.
		// But we don't care, since some other platform will do
		// the right thing.
		continue;
	    }
	}
    }
}
