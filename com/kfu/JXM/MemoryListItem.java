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

 $Id: MemoryListItem.java,v 1.1 2004/03/11 04:18:13 nsayer Exp $
 
 */

package com.kfu.JXM;

import java.util.*;

import com.kfu.xm.*;

public class MemoryListItem {
    private Date when;
    private ChannelInfo info;
    public MemoryListItem(ChannelInfo i) {
	this.when = new Date();
	this.info = i;
    }
    public MemoryListItem(Date when, ChannelInfo i) {
	this.when = when;
	this.info = i;
    }

    public Date getDate() { return this.when; }
    public ChannelInfo getChannelInfo() { return this.info; }
}
