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

 $Id: ChannelInfo.java,v 1.4 2004/03/11 06:51:04 nsayer Exp $

 */

package com.kfu.xm;

public class ChannelInfo {
    private int chnum, chSid;
    private String chName, chGenre, chArtist, chTitle;

    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (! (o instanceof ChannelInfo))
            return false;
        ChannelInfo oc = (ChannelInfo)o;
        if (this.getServiceID() != oc.getServiceID())
            return false;
        if (!this.getChannelName().equals(oc.getChannelName()))
            return false;
        if (!this.getChannelGenre().equals(oc.getChannelGenre()))
            return false;
        if (!this.getChannelArtist().equals(oc.getChannelArtist()))
            return false;
        if (!this.getChannelTitle().equals(oc.getChannelTitle()))
            return false;
        return true;
    }
    
    public int hashCode() {
        return this.getServiceID() ^ this.getChannelName().hashCode() ^
            this.getChannelGenre().hashCode() ^ this.getChannelArtist().hashCode() ^
            this.getChannelTitle().hashCode();
    }
    
	// Make an invalid one.
	public ChannelInfo() {
		this(-1, -1, "", "", "", "");
	}

    // Cloning constructor
    public ChannelInfo(ChannelInfo orig) {
	this.chnum = orig.chnum;
	this.chSid = orig.chSid;
	this.chName = new String(orig.chName);
	this.chGenre = new String(orig.chGenre);
	this.chArtist = new String(orig.chArtist);
	this.chTitle = new String(orig.chTitle);
    }

    // This needs to be public only so it can be deserialized by the memory drawer
    public ChannelInfo(int number, int id, String genre, String name, String artist, String title) {
        this.chnum = number;
		this.chSid = id;
        this.chGenre = genre;
        this.chName = name;
        this.chArtist = artist;
        this.chTitle = title;
    }

    public int getChannelNumber() {
        return this.chnum;
    }
	public int getServiceID() {
		return this.chSid;
	}
	public String getChannelName() {
        return this.chName;
    }
	public String getChannelGenre() {
        return this.chGenre;
    }
	public String getChannelArtist() {
        return this.chArtist;
    }
    public String getChannelTitle() {
        return this.chTitle;
    }

    public void setChannelName(String name) {
        this.chName = name;
    }
    public void setChannelGenre(String genre) {
        this.chGenre = genre;
    }
    public void setChannelArtist(String artist) {
        this.chArtist = artist;
    }
    public void setChannelTitle(String title) {
        this.chTitle = title;
    }	
}
