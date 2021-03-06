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

 $Id: Favorite.java,v 1.1 2004/03/11 04:17:12 nsayer Exp $
 
 */

package com.kfu.JXM;

import java.lang.*;

// This is used by the platform providers to present a list of favorite
// channels in the "system" menu (Dock for OS X, taskbar icon for Win32)

public class Favorite {
    private int channel;
    private String menuItem;
    public Favorite(int channel, String menuItem) {
	this.channel = channel;
	this.menuItem = menuItem;
    }

    public int getChannelNumber() { return this.channel; }
    public String getMenuString() { return this.menuItem; }
}
