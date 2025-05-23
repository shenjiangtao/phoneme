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

#include <suspend_resume.h>
#include <suspend_resume_vm.h>
#include <midpNativeThread.h>

/**
 * Default implementation of suspending routine for the VM.
 */
extern "C"
MIDPError suspend_vm(void *resource) {
    VM *vm = (VM*) resource;
    vm->isSuspended = KNI_TRUE;

    return ALL_OK;
}

/**
 * Default implementation of resuming routine for the VM.
 */
extern "C"
MIDPError resume_vm(void *resource) {
    VM *vm = (VM*) resource;
    vm->isSuspended = KNI_FALSE;

    return ALL_OK;
}


extern "C"
void midp_waitWhileSuspended() {
    while (SR_SUSPENDED == midp_getSRState()) {
        midp_checkAndResume();
        if (!vm.isSuspended) {
            break;
        }

        /* IMPL_NOTE: Sleep delay 1 here means 1 second since
         * midp_sleepNativeThread() takes seconds. Beter solution
         * is rewriting midp_sleepNativeThread() for it to take
         * milliseconds and use SR_RESUME_CHECK_TIMEOUT here.
         */
        midp_sleepNativeThread(1);
    }
}
