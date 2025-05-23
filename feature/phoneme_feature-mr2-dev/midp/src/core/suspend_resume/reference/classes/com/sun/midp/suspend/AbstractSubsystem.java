/*
 *
 *
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

package com.sun.midp.suspend;

import com.sun.midp.security.ImplicitlyTrustedClass;
import com.sun.midp.security.SecurityToken;
import com.sun.midp.security.SecurityInitializer;

import java.util.Enumeration;
import java.util.Vector;

/**
 * An abstract subsystem that may contain other subsystems.
 * Contained subsytems are suspended after this subsystem suspend
 * and resumed prior to the subsystem resuming.
 *
 */
public abstract class AbstractSubsystem implements Subsystem {
    /** Class registered in SecurityInitializer. */
    private static class SecurityTrusted implements ImplicitlyTrustedClass {}

    /**
     * Security token for provileged access to internal API's.
     * Note it must stay package private.
     */
    static SecurityToken classSecurityToken =
        SecurityInitializer.requestToken(new SecurityTrusted());

    /** State transition synchronization lock. */
    final Object lock = new Object();
    
    /** Current subsystem state. */
    int state = SUSPENDED;
    
    /** Subsystems this one depends on. */
    private final Vector subsystems = new Vector();

    /**
     * Returns the current state.
     * @return current state if the subsystem
     */
    public final int getState() {
        return state;
    }

    /**
     * Registers a subsystem this one depends on.
     * @param s subsystem this one depends on.
     */
    public void registerSubsystem(Subsystem s) {
        synchronized (subsystems) {
            if (!subsystems.contains(s)) {
                subsystems.addElement(s);
            }
        }
    }
    
    /**
     * If the current state is <code>SUSPENDED</code>, changes the state to
     * <code>RESUMING</code>, performs object-specific activation
     * operations and then changes the state to <code>ACTIVE</code>.
     */
    public final void resume() throws StateTransitionException {
        synchronized (lock) {
            if (state == SUSPENDED) {
                state = RESUMING;
                
                Enumeration subs = subsystems.elements();
                while (subs.hasMoreElements()) {
                    ((Subsystem)subs.nextElement()).resume();
                }
            
                resumeImpl();
                state = ACTIVE;
                resumed();
            }
        }
    }
  
    /**
     * If the current state is <code>ACTIVE</code>, changes the state to
     * <code>PAUSING</code>, performs object-specific suspend
     * operations and then changes the state to <code>SUSPENDED</code>.
     */
    public final void suspend() throws StateTransitionException {
        synchronized (lock) {
            if (state == ACTIVE) {
                state = PAUSING;
                suspendImpl();
                
                Enumeration subs = subsystems.elements();
                while (subs.hasMoreElements()) {
                    ((Subsystem)subs.nextElement()).suspend();
                }
            
                checkSuspended();
            }
        }
    }

    /**
     * Checks if all the suspend conditions are satisfied, if they are,
     * sets subsystem state to SUSPENDED and calls suspended().
     */
    void checkSuspended() {
        synchronized (lock) {
            state = SUSPENDED;
            suspended();
        }

    }

    /**
     * Performs object-specific activation operations.
     * Default implementation makes nothing.
     */
    protected void resumeImpl() {}
  
    /**
     * Performs object-specific suspend operations.
     * Default implementation makes nothing.
     */
    protected void suspendImpl() {}

    /**
     * Confirms subsystem has been suspended. Listeners/waiters
     * can be invoked here.
     */
    void suspended() {}

    /**
     * Confirms subsystem has been resumed. Listeners/waiters
     * can be invoked here.
     */
    void resumed() {}
}
