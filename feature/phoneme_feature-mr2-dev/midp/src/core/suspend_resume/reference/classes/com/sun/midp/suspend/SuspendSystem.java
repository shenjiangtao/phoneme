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

import com.sun.midp.main.*;
import com.sun.midp.security.SecurityToken;
import com.sun.midp.security.Permissions;

import java.util.Vector;

/**
 * Main system of the current isolate that contains all 
 * pausable subsystems in current isolate. 
 * There is a singleton instance in each isolate. The 
 * instance kept in the AMS isolate is a special one and 
 * belongs to <code>MIDPSystem</code> subtype.
 */
public class SuspendSystem extends AbstractSubsystem {
    /**
     * Listeners interested in suspend/resume operations.
     */
    private Vector listeners = new Vector(1, 2);

    /**
     * Main subsystem that implements suspend actions for
     * whole MIDP system. It works in the AMS Isolate. There
     * is the only instance for all isolates.
     */
    private static class MIDPSystem extends SuspendSystem
            implements MIDletProxyListListener {
        /**
         * Constructs the only instance.
         */
        private MIDPSystem() {
            state = ACTIVE;
            MIDletProxyList.getMIDletProxyList(classSecurityToken).
                    addListener(this);
        }

        /**
         * Notifies of system suspend.
         */
        protected void suspended() {
            super.suspended();
            suspended0();
        }

        /**
         * Notifies native functionality that MIDP activities in java
         * have been suspended.
         */
        protected native void suspended0();

        /**
         * Recieves notifications on MIDlet updates and removes corresponding
         * MIDlet proxy from suspend dependencies if required.
         * @param midlet MIDletProxy that represents the MIDlet updated
         * @param reason kind of changes that took place, see
         */
        public void midletUpdated(MIDletProxy midlet, int reason) {
            if (reason == MIDletProxyListListener.RESOURCES_SUSPENDED) {
                removeSuspendDependency(midlet);
            }
        }

        /**
         * Receives MIDlet removal notification and removes corresponding
         * MIDlet proxy from suspend dependencies.
         * @param midlet MIDletProxy that represents the MIDlet removed
         */
        public void midletRemoved(MIDletProxy midlet) {
            removeSuspendDependency(midlet);
        }

        /**
         * Not used. MIDletProxyListListener interface method;
         */
        public void midletAdded(MIDletProxy midlet) {}
        /**
         * Not used. MIDletProxyListListener interface method;
         */
        public void midletStartError(int externalAppId, int suiteId,
                                     String className, int error) {}
    }

    // IMPL_NOTE: the dependecies are now processed after suspend routines.
    // It may have sense first wait for all dependencies resolution, then
    // suspend system and subsystems.
    /**
     * A set of suspend dependencies. System is considered to be suspended
     * when all the subsystems are suspended and all the dependencies
     * removed.
     */
    protected final Vector dependencies = new Vector(4, 2);

    /**
     * The singleton instance.
     */
    private static SuspendSystem instance =
        MIDletSuiteUtils.getIsolateId() == MIDletSuiteUtils.getAmsIsolateId()?
                new MIDPSystem() : new SuspendSystem();

    /**
     * Retrieves the singleton instance.
     * @param token security token that identifies caller permissions for
     *        accessing this API
     * @return the singleton instance
     */
    public static SuspendSystem getInstance(SecurityToken token) {
        token.checkIfPermissionAllowed(Permissions.MIDP);
        return instance;
    }

    /**
     * Constructs an instance.
     */
    private SuspendSystem() {}

    /**
     * Adds a dependency that prevents from system suspend.
     * @param dep dependency to add
     */
    public void addSuspendDependency(SuspendDependency dep) {
        synchronized (lock) {
            if (!dependencies.contains(dep)) {
                dependencies.addElement(dep);
            }
        }

    }

    /**
     * Removes dependency that does not prevent from system suspend any more.
     * Then invokes suspend notification if there are no dependencies left.
     * @param dep dependency to remove
     */
    public void removeSuspendDependency(SuspendDependency dep) {
        synchronized (lock) {
            dependencies.removeElement(dep);
            checkSuspended();
        }
    }

    /**
     * Checks if there are dependencies that prevent from system suspend,
     * if there are no ones, and the state is PAUSING sets state to
     * SUSPENDED and calls suspended().
     */
    protected void checkSuspended() {
        synchronized (lock) {
            if (state == PAUSING && 0 == dependencies.size()) {
                state = SUSPENDED;
                suspended();
            }
        }
    }

    /**
     * Registers a lisener interested in system suspend/resume operations.
     * IMPL_NOTE: method for removing listeners is not needed currently.
     *
     * @param listener the listener to be added
     */
    public void addListener(SuspendSystemListener listener) {
        synchronized (listeners) {
            listeners.addElement(listener);
        }
    }

    /**
     * Notifies listeners of system suspend.
     */
    protected void suspended() {
        synchronized (listeners) {
            for (int i = listeners.size() - 1; i >= 0; i-- ) {
                SuspendSystemListener listener =
                        (SuspendSystemListener)listeners.elementAt(i);
                listener.midpSuspended();
            }
        }
    }

    /**
     * Notifies listeners of system resume.
     */
    protected void resumed() {
        synchronized (listeners) {
            for (int i = listeners.size() - 1; i >= 0; i-- ) {
                SuspendSystemListener listener =
                        (SuspendSystemListener)listeners.elementAt(i);
                listener.midpResumed();
            }
        }
    }
}
