/*
 * Copyright  1990-2006 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version
 * 2 only, as published by the Free Software Foundation. 
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included at /legal/license.txt). 
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA 
 * 
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, CA 95054 or visit www.sun.com if you need additional
 * information or have any questions.
 */

package com.sun.j2me.log;

/**
 * Intermediate class for logging facilities
 */
public class LogChannels {
    
   /** No channel specified */
   public final static int LC_NONE = 0;

   /** AMS module channel */
   public final static int LC_AMS = 500;

   /** Core module channel */
   public final static int LC_CORE = 1000;

   /** Core midpString channel */
   public final static int LC_MIDPSTRING = 1100;

   /** Core malloc channel */
   public final static int LC_MALLOC = 1200;

   /** Core stack usage tracing channel */
   public final static int LC_CORE_STACK = 1300;

   /** Low level UI module channel */
   public final static int LC_LOWUI = 2000;

   /** I18N module channel */
   public final static int LC_I18N = 3000;

   /** High level UI module channel */
   public final static int LC_HIGHUI = 4000;

   /** High level UI item layout channel */
   public final static int LC_HIGHUI_ITEM_LAYOUT = 4100;

   /** High level UI item repaint channel */
   public final static int LC_HIGHUI_ITEM_REPAINT = 4200;

   /** High level UI form layout channel */
   public final static int LC_HIGHUI_FORM_LAYOUT = 4300;

   /** High level UI form paint channel */
   public final static int LC_HIGHUI_ITEM_PAINT = 4400;

   /** Protocol module channel */
   public final static int LC_PROTOCOL = 5000;

   /** RMS module channel */
   public final static int LC_RMS = 6000;

   /** Security module channel */
   public final static int LC_SECURITY = 7000;

   /** Services module channel */
   public final static int LC_SERVICES = 8000;

   /** Tools module channel */
   public final static int LC_TOOL = 9000;

   /** Event queue module channel */
   public final static int LC_EVENTS = 9500;

   /** File Storage module channel */
   public final static int LC_STORAGE = 10000;

   /** Push registry module channel */
   public final static int LC_PUSH = 10500;

   /** MMAPI module channel */
   public final static int LC_MMAPI = 11000;
    
}
