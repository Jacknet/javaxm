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

 $Id: MemoryListItem.java,v 1.2 2004/03/30 17:18:21 nsayer Exp $
 
 */

package com.kfu.JXM;

import java.io.*;
import java.net.*;
import java.util.*;

import com.kfu.xm.*;

public class MemoryListItem {
    private Date when;
    private ChannelInfo info;
    private String notes;

    public MemoryListItem(ChannelInfo i) {
	this.when = new Date();
	this.info = i;
    }
    public MemoryListItem(Date when, ChannelInfo i) {
	this.when = when;
	this.info = i;
    }
    public MemoryListItem(String serialization) {
	String[] split = serialization.split(":");
	if (split.length != 7 && split.length != 8)
	    throw new IllegalArgumentException("Wrong number of serialized fields.");
	try {
	    this.when = new Date(Long.parseLong(split[0]));
	    this.info = new ChannelInfo(Integer.parseInt(split[1]), Integer.parseInt(split[2]),
		URLDecoder.decode(split[4], "US-ASCII"), // NOTE - for some dumb reason, I but name before genre here.
		URLDecoder.decode(split[3], "US-ASCII"),
		URLDecoder.decode(split[5], "US-ASCII"),
		URLDecoder.decode(split[6], "US-ASCII"));
	    if (split.length >= 8)
		this.notes = URLDecoder.decode(split[7], "US-ASCII");
	}
	catch(NumberFormatException e) {
	    throw new IllegalArgumentException("Invalid serialization");
	}
	catch(UnsupportedEncodingException e) {
	    // impossible
	}
    }

    public Date getDate() { return this.when; }
    public ChannelInfo getChannelInfo() { return this.info; }

    public String getNotes() { return this.notes; }
    public void setNotes(String val) { this.notes = val; }

    public String serialize() {
	StringBuffer sb = new StringBuffer();
	sb.append(Long.toString(this.getDate().getTime()));
	sb.append(":");
	sb.append(this.getChannelInfo().getChannelNumber());
	sb.append(":");
	sb.append(this.getChannelInfo().getServiceID());
	sb.append(":");
	try {
	    sb.append(URLEncoder.encode(this.getChannelInfo().getChannelName(), "US-ASCII"));
	    sb.append(":");
	    sb.append(URLEncoder.encode(this.getChannelInfo().getChannelGenre(), "US-ASCII"));
	    sb.append(":");
	    sb.append(URLEncoder.encode(this.getChannelInfo().getChannelArtist(), "US-ASCII"));
	    sb.append(":");
	    sb.append(URLEncoder.encode(this.getChannelInfo().getChannelTitle(), "US-ASCII"));
	    sb.append(":");
	    sb.append(URLEncoder.encode(this.getNotes(), "US-ASCII"));
	}
	catch(UnsupportedEncodingException e) {
	    // impossible
	}
	return sb.toString();
    }
}
