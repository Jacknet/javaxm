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
 
 */

package com.kfu.xm;

import java.io.*;
import java.util.*;

import javax.comm.*;

public class RadioCommander implements IAsyncExceptionHandler {
	
	public static class SongTiming {
		public Date start, end;
		public SongTiming(Date start, Date end) {
			this.start = start;
			this.end = end;
		}
		public Date start() {
			return this.start;
		}
		public Date end() {
			return this.end;
		}
	}
	
	public static class ExtendedSignalData {
		respExtendedSignalQuality response;
		ExtendedSignalData(respExtendedSignalQuality response) {
			this.response = response;
		}
		
		private float cnIntToFloat(int cn) {
			// C:N numbers are in 1/4 dB.
			return cn / 4F;			
		}
		private float berIntToFloat(int ber) {
			// The BER number is in 1/68ths of a %.
			float res = ber / 68F;
			if (res >= 100F)
				res = 100F;
			return res;
		}
		public boolean sat1QPSK() {
			return this.response.internalGetByte(5) != 0;
		}
		public boolean sat1TDM() {
			return this.response.internalGetByte(8) != 0;
		}
		public float satAGC() {
			return this.response.internalGetByte(21);
		}
		public float sat1BER() {
			int berInt = this.response.internalGetByte(11);
			berInt <<= 8;
			berInt += this.response.internalGetByte(12);
			return berIntToFloat(berInt);
		}
		public float sat1CN() {
			return cnIntToFloat(this.response.internalGetByte(23));
		}
		
		public boolean sat2QPSK() {
			return this.response.internalGetByte(6) != 0;
		}
		public boolean sat2TDM() {
			return this.response.internalGetByte(9) != 0;
		}
		public float sat2BER() {
			int berInt = this.response.internalGetByte(13);
			berInt <<= 8;
			berInt += this.response.internalGetByte(14);
			return berIntToFloat(berInt);
			
		}
		public float sat2CN() {
			return cnIntToFloat(this.response.internalGetByte(24));
		}
		
		public boolean terMCM() {
			return this.response.internalGetByte(7) != 0;
		}
		public boolean terTDM() {
			return this.response.internalGetByte(10) != 0;
		}
		public float terBER() {
			int berInt = this.response.internalGetByte(15);
			berInt <<= 8;
			berInt += this.response.internalGetByte(16);
			return berIntToFloat(berInt);			
		}
		public float terAGC() {
			return this.response.internalGetByte(22);
		}
		
		public int miscAntStat() {
			return this.response.internalGetByte(3);
		}
		public int miscSigStat() {
			return this.response.internalGetByte(2);
		}
		public int miscTunerStat() {
			return this.response.internalGetByte(4);
		}
		
	}
	
    private abstract class Command {
        private byte[] buf;
        protected Command(byte type, int commandLength) {
            this.buf = new byte[commandLength + 7];
            commandLength++; // make room for the command type byte itself
            this.buf[0] = 0x5a;
            this.buf[1] = (byte)0xa5;
            this.buf[2] = (byte)(commandLength >> 8);
            this.buf[3] = (byte)(commandLength & 0xff);
            this.buf[4] = type;
            this.buf[buf.length - 2] = (byte)0xed;
            this.buf[buf.length - 1] = (byte)0xed;
        }
        protected void setByte(int pos, byte val) {
            this.buf[pos + 5] = val;
        }
        public byte[] getData() {
            return buf;
        }
    }
    private class cmdPowerUp extends Command {
        public cmdPowerUp() {
            super((byte)0x00, 4);
            this.setByte(0, (byte)0x10);  // channel label size (8, 10 or 16)
            this.setByte(1, (byte)0x10);  // category label size (8, 10 or 16)
            this.setByte(2, (byte)0x24);  // artist & title size (8, 10 or 16)
            this.setByte(3, (byte)0x1);  // radio type - 1 mens "power can disappear at any time"
        }
    }
    private class cmdPowerDown extends Command {
        public cmdPowerDown(boolean powerSaving) {
            super((byte)0x01, 1);
            this.setByte(0, (byte)(powerSaving?1:0)); // 1 for 'power saving' instead of 'off'
        }
        public cmdPowerDown() {
            this(false);
        }
    }
    private class cmdGetExtendedSignalQuality extends Command {
        public cmdGetExtendedSignalQuality() {
            super((byte)0x43, 0);
        }
    }
    private class cmdGetRadioID extends Command {
        public cmdGetRadioID() {
            super((byte)0x31, 0);
        }
    }
    private class cmdChangeChannel extends Command {
        public cmdChangeChannel(int channel) {
            super((byte)0x10, 5);
            this.setByte(0, (byte)2);     // select method
            this.setByte(1, (byte)channel);
            this.setByte(2, (byte)0);     // 0 for audio, 1 for data
            this.setByte(3, (byte)0);     // program type
            this.setByte(4, (byte)1);     // routing - 1 for audio port
        }
    }
    private class cmdSetMute extends Command {
        public cmdSetMute(boolean state) {
            super((byte)0x13, 1);
            this.setByte(0, (byte)(state?1:0));
        }
    }
    private class cmdThisChannelInfo extends Command {
        public cmdThisChannelInfo(int channel) {
            super((byte)0x25, 3);
            this.setByte(0, (byte)8);     // selection method - label channel select
            this.setByte(1, (byte)channel);
            this.setByte(2, (byte)0);     // program type
        }
    }
    private class cmdThisChannelInfoBySID extends Command {
        public cmdThisChannelInfoBySID(int channel) {
            super((byte)0x25, 3);
            this.setByte(0, (byte)7);     // selection method - label SID select
            this.setByte(1, (byte)channel);
            this.setByte(2, (byte)0);     // program type
        }
    }
    private class cmdNextChannelInfo extends Command {
        public cmdNextChannelInfo(int channel) {
            super((byte)0x25, 3);
            this.setByte(0, (byte)9);     // selection method - label channel next
            this.setByte(1, (byte)channel);
            this.setByte(2, (byte)0);     // program type
        }
    }
    private class cmdExtendedChannelInfo extends Command {
        public cmdExtendedChannelInfo(int channel) {
            super((byte)0x22, 1);
            this.setByte(0, (byte)channel);
        }
    }
    private class cmdMonitorLabelChange extends Command {
        public cmdMonitorLabelChange(int channel, boolean service, boolean program, boolean info, boolean extended_info) {
            super((byte)0x50, 5);
            this.setByte(0, (byte)channel);
            this.setByte(1, (byte)(service?1:0));
            this.setByte(2, (byte)(program?1:0));
            this.setByte(3, (byte)(info?1:0));
            this.setByte(4, (byte)(extended_info?1:0));
        }
    }
	
    private Response makeResponse(byte[] data) { // caller strips the 0x5a 0xa5, length, and postscript
        Response out = null;
        switch(data[0]) {
            case (byte)0x80:
                out = new respPoweredOn(data);
                break;
            case (byte)0x81:
                out = new respPoweredOff(data);
                break;
            case (byte)0x90:
                out = new respChannelChanged(data);
                break;
            case (byte)0x93:
                out = new respMuteSet(data);
                break;
            case (byte)0xA2:
                out = new respExtendedChannelInfo(data);
                break;
            case (byte)0xA5:
                out = new respChannelInfo(data);
                break;
            case (byte)0xB1:
                out = new respRadioID(data);
                break;
            case (byte)0xC3:
                out = new respExtendedSignalQuality(data);
                break;
            case (byte)0xD0:
                out = new respLabelChangeMonitored(data);
                break;
            case (byte)0xD1:
                out = new respMonitorChannelName(data);
                break;
            case (byte)0xD2:
                out = new respMonitorChannelGenre(data);
                break;
            case (byte)0xD3:
                out = new respMonitorArtistTitle(data);
                break;
            case (byte)0xD4:
                out = new respMonitorExtendedArtist(data);
                break;
            case (byte)0xD5:
                out = new respMonitorExtendedTitle(data);
                break;
            case (byte)0xD6:
                out = new respMonitorSongTime(data);
                break;
            case (byte)0xE0:
                out = new respActivationIndicated(data);
                break;
            case (byte)0xE1:
                out = new respDeactivationIndicated(data);
                break;
            case (byte)0xFF:
                out = new respFatalError(data);
                break;
            default:
                Log("Got unknown response: " + hexify(data));
                out = null;
        }
        return out;
    }
    private static String hexify(byte[] data) {
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < data.length; i++) {
            sb.append(hexify(data[i]));
            if (i != data.length - 1)
                sb.append(' ');
        }
        return sb.toString();
    }
    
    private static String hexify(byte i) {
        int ii = (i & 0xff);
        String s = Integer.toString(ii, 16);
        return (s.length() == 2)?s:("0" + s);
    }
	
    abstract class Response {
        private byte[] data;
        
        protected Response(byte[] data) {
            this.data = data;
        }
        
        // Some responses are asynchronous. They actually handle themselves rather than get sent
        // in response to commands. Those responses override the following two methods
        protected boolean isInternal() {
            return false;
        }
        protected void handle() {
            throw new IllegalArgumentException("Can't call handle on non-internal response.");
        }
        
        // Internal data access functions
        public byte getStatusCode() { // Always the first byte after the type
            return this.getByte(0);
        }
        public byte getStatusDetail() { // Always the first byte after the status code
            return this.getByte(1);
        }
        protected byte getByte(int which) {
            return this.data[which + 1];
        }
        protected String extractString(int pos, int length) {
            StringBuffer sb = new StringBuffer();
            for(int i = 0; i < length; i++) {
                byte b = this.getByte(pos + i);
                if (b <= 32 || b >= 127)
                    sb.append(' ');
                else
                    sb.append((char)b);
            }
            return sb.toString().trim();
        }
    }
    private class respPoweredOff extends Response {
        public respPoweredOff(byte[] data) {
            super(data);
        }
    }
    private class respPoweredOn extends Response {
        public respPoweredOn(byte[] data) {
            super(data);
        }
        public Map getHardwareVersionData() {
            HashMap map = new HashMap();
            map.put("RX version", Integer.toString(this.getByte(3) & 0xff));
            map.put("RX date", this.getDate(4));
            map.put("CBM version", Integer.toString(this.getByte(12) & 0xff));
            map.put("CBM date", this.getDate(13));
            return map;
        }
        public String getRadioID() {
            return extractString(18, this.getByte(17));
        }
        private String byte_to_bcd(int pos) {
            int bcd = this.getByte(pos) & 0xff;
            String s = Integer.toString(bcd, 16); // Printing in hex will give us the BCD digits
            if (s.length() == 1)
                return "0" + s;
            else
                return s;
        }
        private String getDate(int pos) {
            StringBuffer out = new StringBuffer();
            out.append(byte_to_bcd(pos + 2));
            out.append(byte_to_bcd(pos + 3));
            out.append('-');
            out.append(byte_to_bcd(pos));
            out.append('-');
            out.append(byte_to_bcd(pos + 1));
            return out.toString();
        }
        // What was the receiver doing when it was last powered off?
        /* We don't do data
        private int lastDataChannel() {
            if (this.getByte(9) == 1)
                return this.getByte(8);
            else if (this.getByte(11) == 1)
                return this.getByte(10);
            else return 0;
        }
        */
        private int getLastAudioService() {
            if (this.getByte(9) == 0)
                return this.getByte(8) & 0xff;
            else if (this.getByte(11) == 0)
                return this.getByte(10) & 0xff;
            else return 0;
        }
        
    }
    private class respChannelChanged extends Response {
        public respChannelChanged(byte[] data) {
            super(data);
        }
    }
    private class respMuteSet extends Response {
        public respMuteSet(byte[] data) {
            super(data);
        }
    }
    private class respChannelInfo extends Response {
        public respChannelInfo(byte[] data) {
            super(data);
        }
        public ChannelInfo getChannelInfo() {
            int channelNumber = this.getByte(2) & 0xff;
			int sid = this.getByte(3) & 0xff; // Service ID
            String channelName;
            if (this.getByte(4) != 0)
                channelName = this.extractString(5, 16);
            else
                channelName = "";
            String channelGenre;
            if (this.getByte(21) != 0)
                channelGenre = this.extractString(23, 16);
            else
                channelGenre = "";
            String channelArtist, channelTitle;
            if (this.getByte(39) != 0) {
                channelArtist = this.extractString(40, 16);
                channelTitle = this.extractString(56, 16);
            } else
                channelArtist = channelTitle = "";
            return new ChannelInfo(channelNumber, sid, channelGenre, channelName, channelArtist, channelTitle);
        }
    }
    private class respRadioID extends Response {
        public respRadioID(byte[] data) {
            super(data);
        }
        public String getRadioID() {
            return this.extractString(3, this.getByte(2));
        }
    }
    private class respExtendedSignalQuality extends Response {
        public respExtendedSignalQuality(byte[] data) {
            super(data);
        }
        
		// We need to expose getByte to the extendedSignalData class. 
		int internalGetByte(int which) {
			return this.getByte(which) & 0xff;
		}
		
        public double getSatSigPercent() {
            double sat1 = satCNtoPercent(((int)this.getByte(23)) & 0xff);
            double sat2 = satCNtoPercent(((int)this.getByte(24)) & 0xff);
            return (sat1>sat2)?sat1:sat2;
        }
        private double satCNtoPercent(int satCN) {
            satCN -= 6;
            if (satCN < 0)
                satCN = 0;
            int satDB = satCN / 4;
            
            double percent;
            
            if (satDB < 12)
                percent = (satCN * 80 / 12 / 4);
            else if (satDB < 16)
                percent = 80.0 + ((satCN-48) * 20 / 4 / 4);
            else
                percent = 99.9;
            
            return percent;
        }
        
        public double getTerSigPercent() {
            int TerrBER = ((int)this.getByte(15)) & 0xff;
            TerrBER <<= 8;
            TerrBER += ((int)this.getByte(16)) & 0xff;
            
            // TerrBER is in 1/68 % units
            double out = TerrBER / 68.0;
            // But 10% BER = 0% strength
            out *= 10.0;
            out = 100.0 - out;
            
            // Now limit it.
            if (out <= 0)
                return 0;
            else if (out >= 100)
                return 99.9;
            else return out;
        }
    }
    private class respActivationIndicated extends Response {
        public respActivationIndicated(byte[] data) {
            super(data);
        }
        protected boolean isInternal() {
            return true;
        }
        protected void handle() {
            theRadio().setActivated(true);
        }
        public byte getStatusCode() { // There isn't one
            return -1;
        }
        public byte getStatusDetail() { // There isn't one
            return -1;
        }
    }
    private class respDeactivationIndicated extends Response {
        protected respDeactivationIndicated(byte[] data) {
            super(data);
        }
        protected boolean isInternal() {
            return true;
        }
        protected void handle() {
            theRadio().setActivated(false);
        }
        public byte getStatusCode() { // There isn't one
            return -1;
        }
        public byte getStatusDetail() { // There isn't one
            return -1;
        }
    }
    private class respExtendedChannelInfo extends Response {
        public respExtendedChannelInfo(byte[] data) {
            super(data);
        }
        public String getArtist() {
            if (this.getByte(3) == 0)
                return null;
            else
                return this.extractString(4, 32);
        }
        public String getTitle() {
            if (this.getByte(40) == 0)
                return null;
            else
                return this.extractString(41, 32);
        }
    }
    private class respLabelChangeMonitored extends Response {
        public respLabelChangeMonitored(byte[] data) {
            super(data);
        }
        public int getChannel() {
            return this.getByte(2) & 0xff;
        }
    }
    private class respMonitorChannelName extends Response {
        public respMonitorChannelName(byte[] data) {
            super(data);
        }
        public boolean isInternal() {
            return true;
        }
        public void handle() {
            Log("Got channel name monitor response: ch=" + Integer.toString(this.getChannel()) +
				" -> " + this.getChannelName());
			RadioCommander.this.currentChannelInfo.setChannelName(this.getChannelName());
			RadioCommander.this.updateChannelInfo();
        }
        public int getChannel() {
            return this.getByte(0) & 0xff;
        }
        public String getChannelName() {
            if (this.getByte(1) == 0)
                return null;
            else
                return this.extractString(2, 16);
        }
    }
    private class respMonitorChannelGenre extends Response {
        public respMonitorChannelGenre(byte[] data) {
            super(data);
        }
        public boolean isInternal() {
            return true;
        }
        public void handle() {
            Log("Got channel genre monitor response: ch=" + Integer.toString(this.getChannel()) +
				" -> " + this.getChannelGenre());
			RadioCommander.this.currentChannelInfo.setChannelGenre(this.getChannelGenre());
			RadioCommander.this.updateChannelInfo();
        }
        public int getChannel() {
            return this.getByte(0) & 0xff;
        }
        public String getChannelGenre() {
            if (this.getByte(2) == 0)
                return null;
            else
                return this.extractString(3, 16);
        }
    }
    private class respMonitorArtistTitle extends Response {
        public respMonitorArtistTitle(byte[] data) {
            super(data);
        }
        public boolean isInternal() {
            return true;
        }
        public void handle() {
            Log("Got channel name artist / title response: ch=" + Integer.toString(this.getChannel()) +
				" -> " + this.getChannelArtist() + " / " + this.getChannelTitle());
			RadioCommander.this.currentChannelInfo.setChannelTitle(this.getChannelTitle());
			RadioCommander.this.currentChannelInfo.setChannelArtist(this.getChannelArtist());
			RadioCommander.this.updateChannelInfo();
        }
        public int getChannel() {
            return this.getByte(0) & 0xff;
        }
        public String getChannelArtist() {
            if (this.getByte(1) == 0)
                return null;
            else
                return this.extractString(2, 16);
        }
        public String getChannelTitle() {
            if (this.getByte(1) == 0)
                return null;
            else
                return this.extractString(18, 16);
        }
    }
    private class respMonitorExtendedArtist extends Response {
        public respMonitorExtendedArtist(byte[] data) {
            super(data);
        }
        public boolean isInternal() {
            return true;
        }
        public void handle() {
            Log("Got channel extended artist response: ch=" + Integer.toString(this.getChannel()) +
				" -> " + this.getChannelArtist());
			RadioCommander.this.currentChannelInfo.setChannelArtist(this.getChannelArtist());
			RadioCommander.this.updateChannelInfo();
        }
        public int getChannel() {
            return this.getByte(0) & 0xff;
        }
        public String getChannelArtist() {
            if (this.getByte(1) == 0)
                return null;
            else
                return this.extractString(2, 32);
        }
    }
    private class respMonitorExtendedTitle extends Response {
        public respMonitorExtendedTitle(byte[] data) {
            super(data);
        }
        public boolean isInternal() {
            return true;
        }
        public void handle() {
            Log("Got channel extended title response: ch=" + Integer.toString(this.getChannel()) +
				" -> " + this.getChannelTitle());
			RadioCommander.this.currentChannelInfo.setChannelTitle(this.getChannelTitle());
			RadioCommander.this.updateChannelInfo();
        }
        public int getChannel() {
            return this.getByte(0) & 0xff;
        }
        public String getChannelTitle() {
            if (this.getByte(1) == 0)
                return null;
            else
                return this.extractString(2, 32);
        }
    }
    private class respMonitorSongTime extends Response {
        public respMonitorSongTime(byte[] data) {
            super(data);
        }
        public boolean isInternal() {
            return true;
        }
        public void handle() {
            Date now = new Date();
            int prog = this.getProgressTime();
            if (prog < 0)
                currentSongStarted = null;
            else
                currentSongStarted = new Date(now.getTime() - (prog * 1000)); // when did it start?
            int dur = this.getDurationTime();
            if (dur < 0)
                currentSongEnds = null;
            else
                currentSongEnds = new Date(now.getTime() + ((dur - prog) * 1000)); // When will it end?
            Log("Got song time update message");
            Log("  Started: " + currentSongStarted);
            Log("  Now:     " + now);
            Log("  Ends:    " + currentSongEnds);
			SongTiming t = new SongTiming(currentSongStarted, currentSongEnds);
			notifyGUI(SONG_TIME_UPDATE, t);
        }
        public int getChannel() {
            return this.getByte(0) & 0xff;
        }
        public int getFormat() {
            return this.getByte(1) & 0xff;
        }
        public int getProgressTime() {
            if (this.getByte(3) == 0)
                return -1;
            
            int out = this.getByte(6) & 0xff;
            out <<= 8;
            out += this.getByte(7) & 0xff;
            return out;
        }
        public int getDurationTime() {
            if (this.getByte(2) == 0)
                return -1;
            
            int out = this.getByte(4) & 0xff;
            out <<= 8;
            out += this.getByte(5) & 0xff;
            return out;
        }
    }
    private class respFatalError extends Response {
        public respFatalError(byte[] data) {
            super(data);
        }
        public boolean isInternal() {
            return true;
        }
        public void handle() {
            // We can't throw from this context, but we need to simulate a RadioException
            Log("Received fatal error response: " + hexify(this.getStatusCode()) +
                " " + hexify (this.getStatusDetail()));
            RadioException e = new RadioException("Radio indicated fatal error: " + hexify(this.getStatusCode()) +
												  "/" + hexify(this.getStatusDetail()) + ".");
            handleException(e);
        }
    }
    
    // These are the possible values of a Radio event.
    public static final int POWERED_ON = 0;
    public static final int POWERED_OFF = 1;
    public static final int CHANNEL_CHANGED = 2;
    public static final int MUTE_CHANGED = 3;
    public static final int EXCEPTION = 4;
    public static final int ACTIVATION_CHANGED = 5;
    public static final int CHANNEL_INFO_UPDATE = 6;
    public static final int SONG_TIME_UPDATE = 7;
    public static final int CHANNEL_DELETE = 8;
    
    private InputStream myDeviceIn;
    private OutputStream myDeviceOut;
    private SerialPort mySerialPort;
    
    public static String[] getPotentialDevices() {
	ArrayList l = new ArrayList();
        Enumeration e = CommPortIdentifier.getPortIdentifiers();
        while(e.hasMoreElements()) {
            CommPortIdentifier cpi = (CommPortIdentifier)e.nextElement();
            if (cpi.getPortType() != CommPortIdentifier.PORT_SERIAL)
                continue;
            l.add(cpi.getName());
        }
        return (String[])l.toArray(new String[0]);
    }
	
    private static RadioCommander theSingleton;
    
    public static RadioCommander theRadio() {
        synchronized(RadioCommander.class) {
            if (theSingleton == null)
                theSingleton = new RadioCommander();
        }
        return theSingleton;
    }
    
    private RadioCommander() {
        this.myDeviceIn = null;
        this.myDeviceOut = null;
        this.mySerialPort = null;
    }
    
    private synchronized Response performCommand(Command command, Class expectedReplyClass) throws RadioException {
        for(int i = 0; i < 3; i++) {
            try {
                if (this.myDeviceIn == null)
                    throw new RadioException("Radio turned off unexpectedly.");
                
                this.sendCommand(command.getData());
                return waitForReply(expectedReplyClass);
            }
            catch(RadioTimeoutException e) {
                Log("Timeout #" + Integer.toString(i + 1) + " waiting for " + expectedReplyClass.toString());
                continue;
            }
	    catch(IllegalStateException e) {
		throw new RadioException("Radio reset while processing command.");
	    }
        }
        this.Dispose();
        throw new RadioException("Command processing failed after 3 retries");
    }
    
    private void sendCommand(byte[] command) throws RadioException {
	OutputStream handle = this.myDeviceOut;
	if (handle == null) {
	    this.Dispose();
	    throw new RadioException("Cannot talk to radio while it's off");
	}
        try {
            handle.write(command);
            handle.flush();
        }
        catch(IOException e) {
            this.Dispose();
            throw new RadioException("Cannot write to device: " + e.getMessage());
        }
    }
	
    private ArrayList replyQueue;
    private static final int TIMEOUT = 5000; // 5 seconds
    
    private replyWatcher myReplyWatcher = null;
    private class replyWatcher extends Thread {
        private class ThreadMustExitException extends Exception {
        }
        private byte getNextChar() throws IOException, ThreadMustExitException {
            while(true) {
                InputStream localCopy = myDeviceIn;
                if (localCopy == null)
                    throw new ThreadMustExitException();
                // Because of VMIN and VTIME setup in JNI, this will return -1 if no chars received after
                // one second. That will give us a chance to poll for a close.
                int c = localCopy.read();
                if (c >= 0)
                    return (byte)(c & 0xff);
            }
        }
        public void run() {
            while(true) {
                try {
                    if (this.getNextChar() != (byte)0x5a)
                        throw new RadioException("Mismatch waiting for first flag");
                    if (this.getNextChar() != (byte)0xa5)
                        throw new RadioException("Mismatch waiting for second flag");
                    int length = this.getNextChar() & 0xff;
                    length <<= 8;
                    length += this.getNextChar() & 0xff;
                    byte buf[] = new byte[length];
                    for(int i = 0; i < length; i++)
                        buf[i] = this.getNextChar();
                    this.getNextChar(); // throw away first trailer
                    this.getNextChar(); // throw away second trailer
                    Response r = makeResponse(buf);
                    if (r == null)
                        continue;
                    if (r.isInternal()) {
                        r.handle();
                        continue;
                    }
                    synchronized(replyQueue) {
                        replyQueue.add(r);
                        replyQueue.notify();
                    }
                }
                catch(IOException e) {
		    boolean disposing = (myDeviceIn == null);
                    Dispose();
		    // If we're disposing, then ignore these. They can happen when read() is interrupted in some javax.comm implementations
		    if (!disposing)
                        notifyGUI(EXCEPTION, e);
                }
                catch(RadioException e) {
                    Dispose();
                    notifyGUI(EXCEPTION, e);
                }
                catch(ThreadMustExitException e) {
                    return;
                }
            }
        }
    }
    
    private Response waitForReply(Class expectedResponse) throws RadioException {
        synchronized(replyQueue) {
		while(true) {
			if (this.myDeviceIn == null)
				throw new RadioTimeoutException("Radio powered off while waiting for reply");
			if(replyQueue.size() == 0) {
				try {
					replyQueue.wait(TIMEOUT);
				}
				catch(InterruptedException e) {
					// ignore
				}
				if (replyQueue.size() == 0) {
					throw new RadioTimeoutException("Timeout waiting for " + expectedResponse.getName());
				}
			}
			Response r = (Response)replyQueue.remove(0);
			if (expectedResponse.isInstance(r))
				return r;
			Log("Throwing away " + r.getClass().getName() + " while waiting for " + expectedResponse.getName());
		}
        }
    }
    
    private static IDebugLog myLogger = null;
	
    private static void Log(String in) {
        if (myLogger != null)
            myLogger.log(in);
    }
	
    public static void setDebugLog(IDebugLog logger) {
        myLogger = logger;
    }

    private HashSet handlers = new HashSet();

    public void registerEventHandler(RadioEventHandler which) {
	this.handlers.add(which);
    }
    public void unregisterEventHandler(RadioEventHandler which) {
	this.handlers.remove(which);
    }

    private void notifyGUI(int code) {
        this.notifyGUI(code, null);
    }
    private void notifyGUI(int code, Object arg) {
	Iterator i = this.handlers.iterator();
	while(i.hasNext()) {
	    RadioEventHandler reh = (RadioEventHandler)i.next();
	    try {
	        reh.notify(this, code, arg);
	    }
	    catch(Throwable t) {
System.err.println(t.getMessage());
t.printStackTrace();
		// ignore for now?
	    }
	}
    }
	
    // Device *must* be one of the strings returned from getPotentialDevices() !
    public void turnOn(String device) throws RadioException {
        if (this.myDeviceIn != null || this.myDeviceOut != null)
            throw new IllegalStateException("Radio is already on");
        
        try {
            CommPortIdentifier cpi = CommPortIdentifier.getPortIdentifier(device
);
            if (cpi.getPortType() != CommPortIdentifier.PORT_SERIAL)
                throw new RadioException("Port is not a PORT_SERIAL");
            SerialPort sp = (SerialPort)cpi.open("com.kfu.xm.RadioCommander", 0)
;
            sp.setSerialPortParams(9600, SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            sp.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
	    sp.disableReceiveThreshold();
	    sp.enableReceiveTimeout(1000);

            this.myDeviceOut = sp.getOutputStream();
            this.myDeviceIn = sp.getInputStream();
	    this.mySerialPort = sp;

        }
        catch (Exception e) {
            throw new RadioException("Cannot open device: " + e.getMessage());
        }
		
        Log("Opened file");
		
		// flush the input queue first. Should not be necessary
        try {
            while(this.myDeviceIn.available() > 0)
				this.myDeviceIn.read();
        }
        catch(IOException e) {
	    this.Dispose();
            throw new RadioException(e.getMessage());
        }
        
        // Start the watch thread
        replyQueue = new ArrayList();
        this.myReplyWatcher = new replyWatcher();
        this.myReplyWatcher.start();
		
        Log("Sending power-up");
        respPoweredOn reply = (respPoweredOn)this.performCommand(new cmdPowerUp(), respPoweredOn.class);	
        Log("Got power-up reply");
		
        Map m = reply.getHardwareVersionData();
        Iterator i = m.keySet().iterator();
        while(i.hasNext()) {
            String k = (String)i.next();
            String v = (String)m.get(k);
            Log(k + ": " + v);
        }
        Log("Radio ID: " + reply.getRadioID());
        
        this.activated = UNKNOWN;
		
        if (reply.getStatusCode() == 0x3) // status = SUBSCRIPTION
            theRadio().setActivated(false);
		
        // Powering on unmutes.
        this.mutestate = false;
		
        // Powering on sets an undetermined channel.
        this.currentChannel = -1;
		
	try {
		Thread.sleep(250);
	}
	catch(InterruptedException e) {
		// ignore
	}
		
	// The radio power-up may have failed, and we may only now be hearing about it.
	this.checkDisposed();

	// Audio is now going. Notify the UI.
        this.notifyGUI(POWERED_ON);

	// Argh! The last channel is stored by service ID, not by channel number.
	// So we're going to have to ask the surfer to look it up in the cache. Ick!
	int chan = this.getChannelInfoByServiceID(reply.getLastAudioService()).getChannelNumber();
	if (chan < 0)
		chan = 1;
		
	// Let's try going back where we once were. If it doesn't work, we'll wind up with -1, which is fine.
	this.setChannel(chan);

	// If it didn't work, then pick the preview channel
	if (this.currentChannel == -1)
		this.setChannel(1);
		
	// Set up the channel surfer
	this.lastChannel = 0;
	this.channelList = new HashSet();
	this.theSurfer = new Timer();
	this.theSurfer.schedule(new TimerTask() {
	    public void run() {
		RadioCommander.this.timerJob();
	    }
	}, 100, 100);

        // Powering up clears out the duration/progress times
        this.currentSongStarted = this.currentSongEnds = null;
    }

    public void handleException(Exception e) {
        this.Dispose(); // Probably unnecessary, but just in case
        this.notifyGUI(EXCEPTION, e);
    }
	
    private void checkDisposed() throws RadioException {
        if (this.myDeviceIn == null || this.myDeviceOut == null)
            throw new RadioException("Cannot talk to radio when it's off.");
    }
	
    public boolean isOn() {
        return this.myDeviceIn != null;
    }
	
    private int activated;
    private static final int UNKNOWN = 0;
    private static final int ACTIVATED = 1;
    private static final int UNACTIVATED = -1;
	
    void setActivated(boolean isactivated) {
        int newState = isactivated?ACTIVATED:UNACTIVATED;
        int oldState = this.activated;
		
        if (oldState == newState)
            return; // No change.
        this.activated = newState;
        if (oldState == UNKNOWN && newState == ACTIVATED) // Don't care about activated at startup
            return;
        // But in other cases, we want to know.
        this.notifyGUI(ACTIVATION_CHANGED, new Boolean(isactivated));
    }
    
    /*
	 public boolean isActivated() throws RadioException {
		 this.checkDisposed();
		 return this.activated;
	 }
     */
	
    // After calling Dispose(), you can no longer call any other methods on this object.
    private boolean disposing = false;
    public void Dispose() {
	if (this.disposing)
	    return;
	this.disposing = true;
        if (this.theSurfer != null)
            this.theSurfer.cancel();
        this.theSurfer = null;
        
	InputStream in = this.myDeviceIn;
	this.myDeviceIn = null;
        try {
            if (in != null)
                in.close();
	}
        catch (IOException e) {
            // This is really unlikely
            Log("Could not close device input: " + e.getMessage());
        }
	OutputStream out = this.myDeviceOut;
	this.myDeviceOut = null;
        try {
            if (out != null)
                out.close();
        }
        catch (IOException e) {
            // This is really unlikely
            Log("Could not close device output: " + e.getMessage());
        }
	if (this.mySerialPort != null)
	    this.mySerialPort.close();
	this.mySerialPort = null;
		
	/* 
	if (this.myReplyWatcher != null) {
	    try {
		this.myReplyWatcher.join();
	    }
	    catch(InterruptedException e) {
		// ignore
	    }
	}
	*/
        this.myReplyWatcher = null;
	this.disposing = false;
    }
    
    public void turnOff() throws RadioException {
        this.checkDisposed();

	this.theSurfer.cancel();
	this.theSurfer = null;

        Log("Sending power-off");
        //this.performCommand(POWER_DOWN, (byte)0x81);
        this.performCommand(new cmdPowerDown(), respPoweredOff.class);
        Log("Got power-off reply");
        this.Dispose();
		
        this.notifyGUI(POWERED_OFF);
    }
	
    private Date currentSongStarted, currentSongEnds;
    
    private int currentChannel = -1;
    public int getChannel() {
        return this.currentChannel;
    }

    public int getServiceID() {
	return this.currentChannelInfo.getServiceID();
    }
    
    public void setChannel(int channel) throws RadioException {
        this.checkDisposed();
		
        respLabelChangeMonitored reply2 = (respLabelChangeMonitored)this.performCommand(new cmdMonitorLabelChange(this.currentChannel, false, false, false, false), respLabelChangeMonitored.class);
		
        Log("Sending channel-set to " + Integer.toString(channel));
        respChannelChanged result = (respChannelChanged)this.performCommand(new cmdChangeChannel(channel), respChannelChanged.class);
        Log("Got set-channel reply");
        
        if (result.getStatusCode() != 1) // success
        {
            return;
        }
        currentSongStarted = currentSongEnds = null;
        
        // Do this so we can get song timing monitor messages
        reply2 = (respLabelChangeMonitored)this.performCommand(new cmdMonitorLabelChange(channel, true, true, true, true), respLabelChangeMonitored.class);
		
        this.currentChannel = channel;
		this.currentChannelInfo = this.getChannelInfo();
		
        this.notifyGUI(CHANNEL_CHANGED);
		this.updateChannelInfo();
    }
	
    private ChannelInfo currentChannelInfo = new ChannelInfo();
	
    private void updateChannelInfo() {
	this.updateChannelInfo(this.currentChannelInfo);
    }
    private void updateChannelInfo(ChannelInfo info) {
	this.notifyGUI(CHANNEL_INFO_UPDATE, new ChannelInfo(info));
    }

    public String getRadioID() throws RadioException {
        this.checkDisposed();
		
        Log("Sent radio id request");
        respRadioID result = (respRadioID)this.performCommand(new cmdGetRadioID(), respRadioID.class);
        Log("Got radio id reply");
        
        return result.getRadioID();
    }
	
    private boolean mutestate = false;
	
    public boolean isMuted() {
        return this.mutestate;
    }
    
    public void setMute(boolean mute) throws RadioException {
        this.checkDisposed();
        
        this.performCommand(new cmdSetMute(mute), respMuteSet.class);
        
        this.mutestate = mute;
		
        this.notifyGUI(MUTE_CHANGED);
    }
	
    public ExtendedSignalData getExtendedSignalData() throws RadioException {
	this.checkDisposed();
		
	respExtendedSignalQuality result = (respExtendedSignalQuality)this.performCommand(new cmdGetExtendedSignalQuality(), respExtendedSignalQuality.class);

	return new ExtendedSignalData(result);
    }

    public static final int SIGNAL_STRENGTH_SAT = 0;
    public static final int SIGNAL_STRENGTH_TER = 1;

    public double[] getSignalStrength() throws RadioException {
        this.checkDisposed();
		
        //Log("Sending signal strength request");
        respExtendedSignalQuality result = (respExtendedSignalQuality)this.performCommand(new cmdGetExtendedSignalQuality(), respExtendedSignalQuality.class);
        //Log("Got signal strength reply");
		
        return new double[] { result.getSatSigPercent(), result.getTerSigPercent() };
    }

    // The last channel we polled
    int lastChannel;
    // The list of channels we're aware of
    HashSet channelList;
    // The channel surfer
    Timer theSurfer;

    private void timerJob() {
	ChannelInfo info;
	try {
	    info = this.getNextChannelInfo(this.lastChannel);
	}
	catch(RadioException e) {
	    this.handleException(e);
	    return;
	}

	if (info == null) {
	    // We fell off the end. Delete any channels higher than the last one
	    Iterator i = this.channelList.iterator();
	    while(i.hasNext()) {
	        int chan = ((Integer)i.next()).intValue();
	        if (chan > this.lastChannel) {
		    i.remove();
		    this.notifyGUI(CHANNEL_DELETE, new Integer(chan));
	        }
	    }
	    this.lastChannel = 0;
	    return;
	}

        // This is, indeed, the next channel
	this.updateChannelInfo(info);

	// Now we need to go through the channelList sending delete notifications for any channels that
	// aren't there anymore.
	Iterator i = this.channelList.iterator();
	while(i.hasNext()) {
	    int chan = ((Integer)i.next()).intValue();
	    if (chan > this.lastChannel && chan < info.getChannelNumber()) {
		i.remove();
		this.notifyGUI(CHANNEL_DELETE, new Integer(chan));
	    }
	}
	this.lastChannel = info.getChannelNumber();
    }

    // Start with 0 and keep calling me to find out about the next channel. When I return null, you're done.
    private ChannelInfo getNextChannelInfo(int channel) throws RadioException {
        this.checkDisposed();
		
        //Log("Sending command asking for channel after " + Integer.toString(currentchannel));
        respChannelInfo result = (respChannelInfo)this.performCommand(new cmdNextChannelInfo(channel), respChannelInfo.class);
        ChannelInfo info = result.getChannelInfo();
        if (info.getChannelNumber() == 0)
            return null;
        
        this.tryToExtendChannelInfo(info);
        return info;
    }
	
    private void tryToExtendChannelInfo(ChannelInfo info) throws RadioException {
        respExtendedChannelInfo result2 = (respExtendedChannelInfo)this.performCommand(new cmdExtendedChannelInfo(info.getChannelNumber()), respExtendedChannelInfo.class);
        String a = result2.getArtist();
        if (a != null)
            info.setChannelArtist(a);
        String t = result2.getTitle();
        if (t != null)
            info.setChannelTitle(t);
    }
	
    // We pass all requests through to the surfer so he can cache the results
    public ChannelInfo getChannelInfo() throws RadioException {
	return this.getChannelInfo(this.getChannel());
    }

    public ChannelInfo getChannelInfoByServiceID(int sid) throws RadioException {
        respChannelInfo result = (respChannelInfo)this.performCommand(new cmdThisChannelInfoBySID(sid), respChannelInfo.class);
	ChannelInfo info = result.getChannelInfo();
	this.tryToExtendChannelInfo(info);

	this.updateChannelInfo(info);
	return info;
    }

    public ChannelInfo getChannelInfo(int channel) throws RadioException {
        this.checkDisposed();

        //Log("Sending command asking about channel " + Integer.toString(currentchannel));
        respChannelInfo result = (respChannelInfo)this.performCommand(new cmdThisChannelInfo(channel), respChannelInfo.class);
        //Log("Got channel info request reply");
        ChannelInfo info = result.getChannelInfo();
        this.tryToExtendChannelInfo(info);

	this.updateChannelInfo(info);
        return info;
    }

/*
    // When was the last time the channel info for this channel changed?
    public Date getChannelLastChanged(int channel) throws RadioException {
        this.checkDisposed();
		
        return this.mySurfer.getChannelLastChanged(channel);
    }
 */

}

