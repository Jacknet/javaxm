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

 $Id: unknown.java,v 1.6 2004/04/10 07:59:16 nsayer Exp $
 
 */

package com.kfu.Platform;

import java.io.*;

import com.kfu.JXM.*;

public class unknown implements IPlatformHandler {

    String path;

    public boolean useMacMenus() { return false; }

    public boolean needsBrowserPath() { return true; }
    public void setBrowserPath(String path) { this.path = path; }

    public void openURL(String url) throws IOException {
	Runtime.getRuntime().exec(new String[] {this.path, url});
    }

    // return true if we know that the devices really are FTDI chips.
    // If there is only one potential device, and this method returns true,
    // then the radio will turn on at startup.
    public boolean devicesAreFiltered() { return false; }

    public boolean isDeviceValid(String in) { return true; }

    public void registerCallbackHandler(IPlatformCallbackHandler ignore) { }

    public void quit() {}
}
