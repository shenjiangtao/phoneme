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

#ifndef _SUSPEND_RESUME_TEST_H_
#define _SUSPEND_RESUME_TEST_H_

/**
 * Stack resume timeout in milliseconds.
 * In default (test) implementation stack is resumed by timeout.
 */
extern long sr_resume_timeout;

/** Timeout value identifying that timeout is not set. */
#define UNINITIALIZED_TIMEOUT (-1)

/** Default timeout value. */
#define DEFAULT_TIMEOUT 5000

/** Timeout value identifying that stack should resume immediately. */
#define RESUME_NOW_TIMEOUT 0

#endif /* _SUSPEND_RESUME_TEST_H_ */
