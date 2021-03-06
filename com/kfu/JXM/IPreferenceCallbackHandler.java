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

 $Id: IPreferenceCallbackHandler.java,v 1.5 2004/05/11 00:09:51 nsayer Exp $
 
 */

package com.kfu.JXM;

import javax.swing.*;

public interface IPreferenceCallbackHandler {
    public SearchSystem getSearchSystem();
    public void clearChannelStats();
    public void rebuildBookmarksMenu(Bookmark[] list);

    // Liason with filter and search system

    public void reload(); // Preferences dialog was cancelled
    public void save(); // Preferences dialog was OKed
    public JComponent getFilterPreferencePanel(); // Get the tab contents for the filter tab
    public JComponent getSearchPreferencePanel(); // Get the tab contents for the search tab
}

