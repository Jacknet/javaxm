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

 $Id: Win32.java,v 1.4 2004/03/31 15:21:33 nsayer Exp $
 
 */

package com.kfu.Platform;

import java.io.*;
import java.util.*;

import com.ice.jni.registry.*;

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

    public void registerCallbackHandler(IPlatformCallbackHandler ignore) { }

    private ArrayList ftdiDevices;

    private boolean crappyWindowsVersion; // any DOS based Windows: win95, 98, 98SE, ME

    public Win32() throws Exception {
	String osName = System.getProperty("os.name").toLowerCase();
	if (!osName.startsWith("windows"))
	    throw new Exception("We're not using Windows! Yay!");

	this.crappyWindowsVersion = (osName.indexOf("9") >= 0 || osName.indexOf("me") >= 0);
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

}
