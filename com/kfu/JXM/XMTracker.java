/*

 JXM - XMPCR control program for OS X
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

 $Id: XMTracker.java,v 1.4 2004/05/11 00:06:04 nsayer Exp $

 */

package com.kfu.JXM;

import com.kfu.xm.*;

import java.lang.*;
import java.net.*;
import java.io.*;

public class XMTracker {

    private static XMTracker myTracker;

    public static XMTracker theTracker() {
        synchronized(XMTracker.class) {
            if (myTracker == null)
                myTracker = new XMTracker();
        }

        return myTracker;
    }

    private int lastChannel = -1;
    private String lastArtist = null;
    private String lastTitle = null;

    private String username;
    private String password;

    private boolean enabled = false;

    // To turn on the tracker, call setCredentials with a username and password.
    // To turn off the tracker, call Disable().
    public void Disable() {
        this.enabled = false;
        this.lastChannel = -1;
    }
    public boolean getState() {
        return this.enabled;
    }
    
    public void setBaseURL(String url) {
        this.baseURL = url;
    }
    
    public void setCredentials(String username, String password) throws TrackerException {
        this.username = username;
        this.password = password;

        StringBuffer sb = new StringBuffer();
        sb.append(this.baseURL);
        sb.append("auth.php?username=");
	try {
            sb.append(URLEncoder.encode(username, "US-ASCII"));
            sb.append("&password=");
            sb.append(URLEncoder.encode(password, "US-ASCII"));
	}
	catch(UnsupportedEncodingException e) {
	    throw new TrackerException("How is US-ASCII an unsupported encoding?");
	}

        try {
            URL u = new URL(sb.toString());
            URLConnection c = u.openConnection();
            c.setRequestProperty("User-Agent", JXM.userAgentString());
            boolean result = getResponse(c.getInputStream());
            if (!result)
                throw new TrackerException("Your username and/or password are not correct");
        }
        catch(IOException e) {
            throw new TrackerException(e.getMessage());
        }
        this.enabled = true;
    }
    private boolean getResponse(InputStream content) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(content));
            String result = br.readLine();
            return result.equals("yep");
        }
        catch(Exception e) {
            return false;
        }
    }

    // Call this every once in a while with the current ChannelInfo
    public void update(ChannelInfo info) throws TrackerException {
        if (!this.enabled || info == null || (info.getChannelNumber() == lastChannel && info.getChannelArtist().equals(lastArtist) &&
                              info.getChannelTitle().equals(lastTitle)))
            return;

        lastChannel = info.getChannelNumber();
        lastArtist = info.getChannelArtist();
        lastTitle = info.getChannelTitle();

        this.update(Integer.toString(lastChannel), lastArtist, lastTitle);
    }

    // Call this when the radio gets powered off.
    public void turnOff() throws TrackerException {
        if (!this.enabled)
            return;

        this.update("OFF", "", "");
	this.lastChannel = -1;
    }

    private String baseURL = "http://www.xmnation.net/tracker/";
    
    private void update(String number, String artist, String title) throws TrackerException {
        StringBuffer sb = new StringBuffer();
        sb.append(this.baseURL);
        sb.append("now.php?");
        sb.append("u=");
	try {
            sb.append(URLEncoder.encode(this.username, "US-ASCII"));
            sb.append("&p=");
            sb.append(URLEncoder.encode(this.password, "US-ASCII"));
            sb.append("&c=");
            sb.append(URLEncoder.encode(number, "US-ASCII"));
            sb.append("&a=");
            sb.append(URLEncoder.encode(artist, "US-ASCII"));
            sb.append("&t=");
            sb.append(URLEncoder.encode(title, "US-ASCII"));
	}
	catch(UnsupportedEncodingException e) {
	    // Oh, what EVER!
	    throw new TrackerException("I'm confused. How is US-ASCII an unsupported encoding?");
	}

        try {
            URL u = new URL(sb.toString());
            URLConnection c = u.openConnection();
            c.setRequestProperty("User-Agent", JXM.userAgentString());
            c.getContent();
        }
        catch(IOException e) {
            throw new TrackerException(e.getMessage());
        }
    }
}
