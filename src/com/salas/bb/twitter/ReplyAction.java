// BlogBridge -- RSS feed reader, manager, and web based service
// Copyright (C) 2002-2006 by R. Pito Salas
//
// This program is free software; you can redistribute it and/or modify it under
// the terms of the GNU General Public License as published by the Free Software Foundation;
// either version 2 of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
// without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
// See the GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along with this program;
// if not, write to the Free Software Foundation, Inc., 59 Temple Place,
// Suite 330, Boston, MA 02111-1307 USA
//
// Contact: R. Pito Salas
// mailto:pitosalas@users.sourceforge.net
// More information: about BlogBridge
// http://www.blogbridge.com
// http://sourceforge.net/projects/blogbridge
//
// $Id$
//

package com.salas.bb.twitter;

import com.salas.bb.core.GlobalController;

import java.net.URL;

/**
 * Reply twitter action.
 */
public class ReplyAction extends AbstractTwitterAction
{
    private static ReplyAction instance;
    private String screenName;

    /** Creates action. */
    private ReplyAction()
    {
    }

    /**
     * Returns the instance.
     *
     * @return instance.
     */
    public static synchronized ReplyAction getInstance()
    {
        if (instance == null) instance = new ReplyAction();
        return instance;
    }

    /**
     * Sets user URL.
     *
     * @param url URL.
     */
    public void setUserURL(URL url)
    {
        screenName = TwitterGateway.urlToScreenName(url);
        setEnabled(screenName != null);
    }

    @Override
    protected boolean canAct()
    {
        // Continue if all is good, or if the BB user account isn't registered --
        // the dialog will explain.
        return super.canAct() || !TwitterFeature.isAvaiable();
    }

    @Override
    protected void customAction()
    {
        TweetThisDialog ttd = new TweetThisDialog(GlobalController.SINGLETON.getMainFrame());
        ttd.open("@" + screenName + " ");
    }
}
