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

 $Id: JXM.java,v 1.13 2004/04/08 04:39:13 nsayer Exp $
 
 */

package com.kfu.JXM;

import javax.swing.*;
import java.awt.*;
import java.lang.*;
import java.util.*;
import java.util.prefs.*;

public class JXM {
    private static String[] commandLine;

    public static void main(String[] args) {
	commandLine = args;
        try {
            Class.forName("javax.comm.CommPortIdentifier");
        }
        catch(Exception e) {
            try {
                JOptionPane.showMessageDialog(null, e.toString() + "\nIs javax.comm properly installed?",
                                              "Cannot load javax.comm",
                                              JOptionPane.ERROR_MESSAGE);
            } catch(HeadlessException ee) {
                System.err.println("Can't find javax.comm (" + e.getMessage() +
                               "). Is it installed?");
            }
            System.exit(-1);
        }
	new MainWindow();
    }

    public static String version() {
	return "0.9";
    }
    public static String userAgentString() {
	return "JXM v" + version();
    }

    public static String[] getCommandLine() { return commandLine; }

    public static Preferences myUserNode() {
        return Preferences.userNodeForPackage(JXM.class);
    }

    // You can't have one!
    private JXM() { }
}
