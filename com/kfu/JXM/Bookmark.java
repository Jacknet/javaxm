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

 $Id: Bookmark.java,v 1.2 2004/03/09 07:11:04 nsayer Exp $
 
 */

package com.kfu.JXM;

import java.io.*;
import java.lang.*;
import java.net.*;
import java.util.regex.*;

import com.kfu.xm.*;

public class Bookmark {
    private String name, url;

    public Bookmark(String name, String url) {
	this.name = name;
	this.url = url;
    }

    public String getName() { return this.name; }
    public String getURL() { return this.url; }
    public void setName(String name) { this.name = name; }
    public void setURL(String url) { this.url = url; }

    private Pattern numPattern = Pattern.compile("\\{NUMBER\\}");
    private Pattern genrePattern = Pattern.compile("\\{GENRE\\}");
    private Pattern namePattern = Pattern.compile("\\{NAME\\}");
    private Pattern artistPattern = Pattern.compile("\\{ARTIST\\}");
    private Pattern titlePattern = Pattern.compile("\\{TITLE\\}");
    private Pattern sidPattern = Pattern.compile("\\{SERVICE\\}");
    public void surf(ChannelInfo info) throws IOException {
        String url = this.url;
        try {
            url = this.numPattern.matcher(url).replaceAll(Integer.toString(info.getChannelNumber()));
            url = this.genrePattern.matcher(url).replaceAll(URLEncoder.encode(info.getChannelGenre(), "US-ASCII"));
            url = this.namePattern.matcher(url).replaceAll(URLEncoder.encode(info.getChannelName(), "US-ASCII"));
            url = this.artistPattern.matcher(url).replaceAll(URLEncoder.encode(info.getChannelArtist(), "US-ASCII"));
            url = this.titlePattern.matcher(url).replaceAll(URLEncoder.encode(info.getChannelTitle(), "US-ASCII"));
            url = this.sidPattern.matcher(url).replaceAll(Integer.toString(info.getServiceID()));
        }
        catch(UnsupportedEncodingException e) {
            // Oh, whatever!
System.err.println(e.getMessage());
e.printStackTrace();
            return;
        }
        PlatformFactory.ourPlatform().openURL(url);
    }
}

