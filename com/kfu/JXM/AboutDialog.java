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

 $Id: AboutDialog.java,v 1.4 2004/03/31 19:50:34 nsayer Exp $
 
 */

package com.kfu.JXM;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.net.*;

import com.kfu.xm.*;

public class AboutDialog extends JDialog {

    private Bookmark homePage = new Bookmark("", "http://www.javaxm.com/");

    public AboutDialog(JFrame frame) {
	super(frame, "About JXM", true);

	this.getContentPane().setLayout(new GridBagLayout());
	GridBagConstraints gbc = new GridBagConstraints();

	JLabel jl = new JLabel(new ImageIcon(this.getClass().getResource("/images/xm_duke.png")));
	jl.setBackground(new Color(0, 0, 0, 0));
	gbc.gridx = 0;
	gbc.gridy = 0;
	gbc.insets = new Insets(10, 0, 10, 0);
	this.getContentPane().add(jl, gbc);

	jl = new JLabel(JXM.userAgentString());
	jl.setFont(new Font(null, Font.BOLD, 18));
	jl.setBackground(new Color(0, 0, 0, 0));
	jl.setHorizontalAlignment(SwingConstants.CENTER);
	gbc.gridx = 0;
	gbc.gridy = 1;
	gbc.weightx = 1;
	gbc.fill = GridBagConstraints.HORIZONTAL;
	this.getContentPane().add(jl, gbc);

	jl = new JLabel("<html>&copy; 2003-2004 Nicholas Sayer</html>");
	jl.setBackground(new Color(0, 0, 0, 0));
	jl.setHorizontalAlignment(SwingConstants.CENTER);
	gbc.gridx = 0;
	gbc.gridy = 2;
	this.getContentPane().add(jl, gbc);

	JPanel jp = new JPanel();
	JButton jb = new JButton("JXM Home Page");
	jb.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		try {
		    AboutDialog.this.homePage.surf(new ChannelInfo(0, 0, "", "", "", ""));
		}
		catch(IOException ex) {
		    JOptionPane.showMessageDialog(AboutDialog.this, ex.getMessage(), "Error opening URL", JOptionPane.ERROR_MESSAGE);
		}
	    }
	});
	jp.add(jb);
	jb = new JButton("Check for new version");
	jb.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		AboutDialog.this.newVersionCheck();
	    }
	});
	jp.add(jb);
	gbc.gridx = 0;
	gbc.gridy = 3;
	this.getContentPane().add(jp, gbc);

	String[] columns = new String[] { "Property", "value" };
	String[][] data = new String[][] {
	    { "Operating system name", System.getProperty("os.name") },
	    { "Operating system arch", System.getProperty("os.arch") },
	    { "Operating system version", System.getProperty("os.version") },
	    { "Java vendor", System.getProperty("java.vendor") },
	    { "Java version", System.getProperty("java.version") },
	    { "Platform Handler", PlatformFactory.ourPlatform().getClass().getName() },
	};

	JTable jt = new JTable(data, columns);
	jt.setRowSelectionAllowed(false);
	jt.setColumnSelectionAllowed(false);
	jt.setCellSelectionEnabled(false);
	jt.clearSelection();
	gbc.gridx = 0;
	gbc.gridy = 4;
	gbc.weightx = 0;
	gbc.fill = GridBagConstraints.NONE;
	gbc.insets = new Insets(10, 20, 10, 20);
	JScrollPane jsp = new JScrollPane(jt);
	jsp.setMinimumSize(new Dimension(300, 100));
	this.getContentPane().add(jsp, gbc);

	StringBuffer sb = new StringBuffer("<html><pre>");
	try {
	InputStream is = this.getClass().getResourceAsStream("/COPYING");
	int avail;
	while((avail = is.available()) > 0) {
	    byte[] buf = new byte[4096];
	    int count = is.read(buf);
	    if (count < 0)
		break;
	    try {
		sb.append(new String(buf, 0, count, "US-ASCII"));
	    }
	    catch(UnsupportedEncodingException e) {
		// impossible
	    }
	}
	}
	catch(IOException e) {
	    // impossible
	}
	sb.append("</pre></html>");
	gbc.gridx = 0;
	gbc.gridy = 5;
	gbc.weightx = 0;
	gbc.weighty = 0;
	gbc.fill = GridBagConstraints.NONE;
	gbc.anchor = GridBagConstraints.CENTER;
	jl = new JLabel(sb.toString());
	jsp = new JScrollPane(jl);
	jsp.setMinimumSize(new Dimension(680, 300));
	this.getContentPane().add(jsp, gbc);
	this.pack();
	this.setSize(this.getMinimumSize());
    }

    public String getLatestVersion() throws IOException {
        StringBuffer sb = new StringBuffer("http://www.javaxm.com/version.php");
        String id = this.userID;
        if (id != null) {
            sb.append("?user=");
            sb.append(id);
	    sb.append("&");
        } else
	    sb.append("?");
	sb.append("platform=");
	sb.append(PlatformFactory.ourPlatform().getClass().getName());
        try {
            URLConnection c = new URL(sb.toString()).openConnection();
            c.setRequestProperty("User-Agent", JXM.userAgentString());
            BufferedReader bsr = new BufferedReader(new InputStreamReader(c.getInputStream()));
            return bsr.readLine().toLowerCase().trim();
        }
        catch(MalformedURLException e) {
            throw new IOException("On what planet is that a bad URL?!");
        }
    }

    private String userID;
    public void updateUserID(String id) { this.userID = id; }

    private void newVersionCheck() {
	try {
	    if (JXM.version().equals(this.getLatestVersion())) {
		JOptionPane.showMessageDialog(this, "You have the latest version of JXM.", "JXM version check", JOptionPane.INFORMATION_MESSAGE);
	    } else {
		int res = JOptionPane.showConfirmDialog(this, "A new version of JXM is available.\nWould you like to download it?", "JXM version check", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
		if (res == JOptionPane.YES_OPTION) {
		    this.downloadUpgrade(false);
		}
	    }
	}
	catch(IOException e) {
	    JOptionPane.showMessageDialog(AboutDialog.this, e.getMessage(), "Error checking version", JOptionPane.ERROR_MESSAGE);
	}
    }

    // Called at startup
    public void startupCheck() {
	boolean doCheck = JXM.myUserNode().getBoolean(PreferencesDialog.STARTUP_CHECK, true);
	if (!doCheck)
	    return;
	try {
	    if (JXM.version().equals(this.getLatestVersion()))
		return;
	}
	catch(IOException e) { return; } // don't pester them at startup if they're off the net

	final JDialog dialog = new JDialog((JFrame)null, "JXM version check", true);

	JPanel infoPanel = new JPanel();
	infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.PAGE_AXIS));
	JLabel jl = new JLabel("<html>A new version of JXM is available.<br>Would you like to download it?<p><hr><p></html>");
	infoPanel.add(jl);
	JCheckBox jcb = new JCheckBox("Perform this check at startup");
	jcb.setFont(new Font(null, Font.PLAIN, 10));
	jcb.setSelected(true);
	infoPanel.add(jcb);

	int value = JOptionPane.showConfirmDialog((JFrame)null, infoPanel, "JXM version check", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
	
	JXM.myUserNode().putBoolean(PreferencesDialog.STARTUP_CHECK, jcb.isSelected());
	//int value = ((Integer)jop.getValue()).intValue();
	if (value == JOptionPane.NO_OPTION)
	    return;
	this.downloadUpgrade(true);
    }

    private void downloadUpgrade(boolean startup) {
        StringBuffer sb = new StringBuffer("http://www.javaxm.com/download_latest.php?platform=");
	sb.append(PlatformFactory.ourPlatform().getClass().getName());
        try {
	    PlatformFactory.ourPlatform().openURL(sb.toString());
        }
        catch(IOException e) {
	    JOptionPane.showMessageDialog(startup?null:AboutDialog.this, e.getMessage(), "Error checking version", JOptionPane.ERROR_MESSAGE);
        }
    }
}
