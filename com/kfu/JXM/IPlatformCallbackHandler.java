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

 $Id: IPlatformCallbackHandler.java,v 1.3 2004/03/10 03:40:18 nsayer Exp $
 
 */

package com.kfu.JXM;

import com.kfu.xm.*;

public interface IPlatformCallbackHandler {

    public void platformNotify(int messageType, Object messageArg);

    public boolean radioIsOn();

    public Bookmark[] getBookmarks();

    public Favorite[] getFavorites();

    public ChannelInfo getChannelInfo();

    public int getMuteState();

}
