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

package com.kfu.JXM;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.util.prefs.*;

import com.kfu.xm.*;

public class PreferencesDialog extends JDialog {
    JComboBox deviceMenu;
    JLabel radioID;
    JTextField trackerURL;
    JTextField trackerUser;
    JTextField trackerPassword;
    JCheckBox trackerEnabled;

    public PreferencesDialog(JFrame parent) {
	super(parent, "JXM Preferences", true);
	this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
	this.getContentPane().setLayout(new BorderLayout());

	JTabbedPane jtp = new JTabbedPane();

	JPanel jp = new JPanel();
	jp.setLayout(new BoxLayout(jp, BoxLayout.PAGE_AXIS));
	JPanel jp2 = new JPanel();
	this.deviceMenu = new JComboBox();
	this.refreshDeviceMenu();
	jp2.add(this.deviceMenu);
	jp.add(jp2);

	jp2 = new JPanel();
	jp2.setLayout(new FlowLayout());
	JLabel jl = new JLabel("Radio ID:");
	jp2.add(jl);
	this.radioID = new JLabel(" ");
	this.radioID.setBorder(BorderFactory.createLineBorder(Color.BLACK));
	this.radioID.setHorizontalAlignment(SwingConstants.CENTER);
	this.radioID.setFont(new Font("Monospaced", Font.PLAIN, 20));
	this.radioID.setPreferredSize(new Dimension(150, (int)this.radioID.getPreferredSize().getHeight()));
	jp2.add(this.radioID);
	jp.add(jp2);
	
	jtp.addTab("Device", jp);

	jp = new JPanel();
	jp.setLayout(new GridBagLayout());
	GridBagConstraints gbc = new GridBagConstraints();
	gbc.insets = new Insets(5, 5, 0, 0);
	gbc.gridx = gbc.gridy = 0;
	gbc.weightx = 0.25;
	gbc.anchor = GridBagConstraints.LINE_END;
	jl = new JLabel("Base URL:");
	jp.add(jl, gbc);
	gbc.gridy = 1;
	jl = new JLabel("Username:");
	jp.add(jl, gbc);
	gbc.gridy = 2;
	jl = new JLabel("Password:");
	jp.add(jl, gbc);

	this.trackerURL = new JTextField();
	this.trackerURL.setPreferredSize(new Dimension(250, (int)this.trackerURL.getPreferredSize().getHeight()));
	gbc.weightx = 0.75;
	gbc.gridx = 1;
	gbc.gridy = 0;
	gbc.anchor = GridBagConstraints.LINE_START;
	jp.add(this.trackerURL, gbc);

	this.trackerUser = new JTextField();
	this.trackerUser.setPreferredSize(new Dimension(100, (int)this.trackerUser.getPreferredSize().getHeight()));
	gbc.gridy = 1;
	jp.add(this.trackerUser, gbc);

	this.trackerPassword = new JPasswordField();
	this.trackerPassword.setPreferredSize(new Dimension(100, (int)this.trackerPassword.getPreferredSize().getHeight()));
	gbc.gridy = 2;
	jp.add(this.trackerPassword, gbc);

	this.trackerEnabled = new JCheckBox("Enable XM Tracker");
	this.trackerEnabled.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		PreferencesDialog.this.trackerCheckboxClicked();
	    }
	});
	gbc.gridx = 0;
	gbc.gridy = 3;
	gbc.gridwidth = 2;
	gbc.anchor = GridBagConstraints.CENTER;
	jp.add(this.trackerEnabled, gbc);

	jtp.addTab("XM Tracker", jp);

	jp = new JPanel();

	jtp.addTab("Misc", jp);

	this.getContentPane().add(jtp, BorderLayout.CENTER);
	// -------

	jp = new JPanel();
	JButton ok = new JButton("Cancel");
	ok.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		PreferencesDialog.this.reloadFromDefaults();
		PreferencesDialog.this.hide();
	    }
	});
	jp.add(ok);
	ok = new JButton("OK");
	ok.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		PreferencesDialog.this.saveToDefaults();
		PreferencesDialog.this.hide();
	    }
	});
	jp.add(ok);
	this.getContentPane().add(jp, BorderLayout.PAGE_END);

	this.pack();
	this.reloadFromDefaults();
    }

    private void trackerCheckboxClicked() {
	boolean newState = this.trackerEnabled.isSelected();
	if (newState) {
	    try {
		XMTracker.theTracker().setBaseURL(this.trackerURL.getText());
		XMTracker.theTracker().setCredentials(this.trackerUser.getText(), this.trackerPassword.getText());
		this.trackerURL.setEnabled(false);
		this.trackerUser.setEnabled(false);
		this.trackerPassword.setEnabled(false);
		this.trackerEnabled.setSelected(true);
	    }
	    catch(TrackerException e) {
		this.trackerEnabled.setSelected(false);
		this.handleTrackerException(e);
	    }
	} else {
	    this.trackerURL.setEnabled(true);
	    this.trackerUser.setEnabled(true);
	    this.trackerPassword.setEnabled(true);
	    this.trackerEnabled.setSelected(false);
	    try {
		XMTracker.theTracker().turnOff();
	    }
	    catch(TrackerException e) {
		this.handleTrackerException(e);
		return;
	    }
	    XMTracker.theTracker().Disable();
	}
    }

    private Preferences myNode() {
	return Preferences.userNodeForPackage(this.getClass());
    }

    private final static String XMTRACKER_URL = "TrackerURL";
    private final static String XMTRACKER_USER = "TrackerUser";
    private final static String XMTRACKER_PASS = "TrackerPassword";
    private final static String XMTRACKER_ENABLED = "TrackerEnabled";

    private void reloadFromDefaults() {
	this.trackerURL.setText(this.myNode().get(XMTRACKER_URL, "http://www.xmnation.net/tracker/"));
	this.trackerUser.setText(this.myNode().get(XMTRACKER_USER, ""));
	this.trackerPassword.setText(this.myNode().get(XMTRACKER_PASS, ""));
	this.trackerEnabled.setSelected(this.myNode().getBoolean(XMTRACKER_ENABLED, false));
	this.trackerCheckboxClicked();
    }

    private void saveToDefaults() {
	this.myNode().put(XMTRACKER_URL, this.trackerURL.getText());
	this.myNode().put(XMTRACKER_USER, this.trackerUser.getText());
	this.myNode().put(XMTRACKER_PASS, this.trackerPassword.getText());
	this.myNode().putBoolean(XMTRACKER_ENABLED, this.trackerEnabled.isSelected());
    }

    // This is called when the radio is turned on
    public void turnOn(String radioID) {
	this.radioID.setText(radioID);
	this.deviceMenu.setEnabled(false);
    }

    // This is called when the radio is turned off
    public void turnOff() {
	this.radioID.setText("");
	this.deviceMenu.setEnabled(true);
    }

    private void handleTrackerException(Exception e) {
    }

    private final static String DEVICE_NAME_KEY = "DefaultDevice";
    private void refreshDeviceMenu() {
        this.deviceMenu = new JComboBox();
        this.deviceMenu.addItem("Pick device");

        String[] devices = RadioCommander.getPotentialDevices();
        for(int i = 0; i < devices.length; i++) {
            final String name = devices[i];
            this.deviceMenu.addItem(name);
        }
        this.deviceMenu.setSelectedIndex(0);
        this.deviceMenu.setSelectedItem(this.myNode().get(DEVICE_NAME_KEY, "Pick device"));
    }

    // -----------------------
    // Below are the properties we export to the world.

    public String getDevice() {
	String out = (String)this.deviceMenu.getSelectedItem();
	if (out == "" | out == "Pick device")
	    return null;
	return out;
    }
    // Call this after a successfull power-up
    public void saveDevice() {
	this.myNode().put(DEVICE_NAME_KEY, (String)this.deviceMenu.getSelectedItem());
    }
    public String getTrackerURL() { return this.trackerURL.getText(); }
    public String getTrackerUser() { return this.trackerUser.getText(); }
    public String getTrackerPassword() { return this.trackerPassword.getText(); }
    public boolean isTrackerEnabled() { return this.trackerEnabled.isSelected(); }
}
