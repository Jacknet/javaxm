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

 $Id: Win32.java,v 1.2 2004/03/09 07:12:42 nsayer Exp $
 
 */

package com.kfu.Platform;

import java.io.*;

import edu.stanford.ejalbert.*;

import com.kfu.JXM.*;

public class Win32 implements IPlatformHandler {

    public boolean useMacMenus() { return false; }

    public boolean needsBrowserPath() { return false; }
    public void setBrowserPath(String ignore) { throw new IllegalArgumentException("We don't care"); }
    public void openURL(String url) throws IOException {
	BrowserLauncher.openURL(url);
    }

    public boolean isDeviceValid(String in) { return true; } // Windows javax.comm is ok

    public void registerCallbackHandler(IPlatformCallbackHandler ignore) { }

    public Win32() throws Exception {
	if (!System.getProperty("os.name").startsWith("Windows"))
	    throw new Exception("We're not using Windows! Yay!");
    }

}
