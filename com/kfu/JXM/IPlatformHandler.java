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

 $Id: IPlatformHandler.java,v 1.5 2004/04/10 07:59:16 nsayer Exp $
 
 */

package com.kfu.JXM;

import java.io.*;

public interface IPlatformHandler {

	// The no-arg constructor throws if creation is attempted on the wrong platform

	// Is this platform able to filter out non-FTDI devices? If so, then at startup
	// if the list has only one member in it, it will be tried no matter what.
	public boolean devicesAreFiltered();

	// Is this device name actually a valid device?
	public boolean isDeviceValid(String device);

	// Do we need to suppress the About, Prefs and Quit menu items and provide them ourselves instead?
	public boolean useMacMenus();

	// Does this platform need to know a path to the user's choice of browser?
	public boolean needsBrowserPath();

	// If so, then set it here
	public void setBrowserPath(String path);

	// Open a browser
	public void openURL(String url) throws IOException;

	// The platform handler can send back messages here
        public void registerCallbackHandler(IPlatformCallbackHandler handler);

	// Called at exit
	public void quit();
}
