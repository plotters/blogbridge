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
// $Id: FontPropertyHandler.java,v 1.1 2006/10/16 08:38:24 spyromus Exp $
//

package com.salas.bb.views.stylesheets.parser;

import com.salas.bb.views.stylesheets.domain.Rule;

import java.awt.*;

/**
 * Handler of font property.
 */
class FontPropertyHandler implements IPropertyHandler
{
    /** Default font we derive everything else from. */
    private final static Font DEFAULT_FONT = new Font("default", Font.PLAIN, 12);

    /**
     * Does something with the rule according to the value.
     *
     * @param rule  rule.
     * @param value value.
     */
    public void handle(Rule rule, String value)
    {
        boolean isBold = value.indexOf("bold") != -1;
        if (isBold) rule.setFont(DEFAULT_FONT.deriveFont(Font.BOLD));
    }
}
