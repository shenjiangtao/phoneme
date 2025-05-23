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

#include <stdio.h>
#include <string.h>
#include <ctype.h>
#include <midp_logging.h>
#include <midpAMS.h>
#include <suitestore_common.h>
#include <midpMalloc.h>
#include <jvm.h>
#include <jvmspi.h>
#include <findMidlet.h>
#include <midpUtilKni.h>
#include <midp_jc_event_defs.h>
#include <midp_properties_port.h>
#include <javacall_events.h>
#include <javacall_lifecycle.h>
#include <midpStorage.h>
#include <suitestore_task_manager.h>

#include "commandLineUtil.h"



#if ENABLE_MULTIPLE_ISOLATES
#define MIDP_HEAP_REQUIREMENT (4 * 1024 * 1024)
#else
#define MIDP_HEAP_REQUIREMENT (1024 * 1024)
#endif

/** Maximum number of command line arguments. */
#define RUNMIDLET_MAX_ARGS 128

unsigned char enable_java_debugger = 0;

/** Usage text for the run MIDlet executable. */
static const char* const runUsageText =
"\n"
"Usage: runMidlet [<VM args>] [-debug] [-loop] [-classpathext <path>]\n"
"           (<suite number> | <suite ID>)\n"
"           [<classname of MIDlet to run> [<arg0> [<arg1> [<arg2>]]]]\n"
"         Run a MIDlet of an installed suite. If the classname\n"
"         of the MIDlet is not provided and the suite has multiple MIDlets,\n"
"         the first MIDlet from the suite will be run.\n"
"          -debug: start the VM suspended in debug mode\n"
"          -loop: run the MIDlet in a loop until system shuts down\n"
"          -classpathext <path>: append <path> to classpath passed to VM\n"
"             (can access classes from <path> as if they were romized)\n"
"\n"
"  where <suite number> is the number of a suite as displayed by the\n"
"  listMidlets command, and <suite ID> is the unique ID a suite is \n"
"  referenced by\n\n";

/**
 * Runs a MIDlet from an installed MIDlet suite.
 * Javacall port uses this function to start MIDP.
 *
 * @param argc The total number of arguments
 * @param argv An array of 'C' strings containing the arguments
 *
 * @return <tt>0</tt> for success, otherwise <tt>-1</tt>
 *
 * IMPL_NOTE:determine if it is desirable for user targeted output
 *       messages to be sent via the log/trace service, or if
 *       they should remain as printf calls
 */
int
runMidlet(int argc, char** commandlineArgs) {
    int status = -1;
    SuiteIdType suiteId   = UNUSED_SUITE_ID;
    pcsl_string classname = PCSL_STRING_NULL;
    pcsl_string arg0 = PCSL_STRING_NULL;
    pcsl_string arg1 = PCSL_STRING_NULL;
    pcsl_string arg2 = PCSL_STRING_NULL;
    int repeatMidlet = 0;
    char* argv[RUNMIDLET_MAX_ARGS];
    int i, used;
    int debugOption = MIDP_NO_DEBUG;
    char *progName = commandlineArgs[0];
    char* midpHome = NULL;
    char* additionalPath;
    SuiteIdType* pSuites = NULL;
    int numberOfSuites = 0;

    JVM_Initialize(); /* It's OK to call this more than once */

    /*
     * Set Java heap capacity now so it can been overridden from command line.
     */
    JVM_SetConfig(JVM_CONFIG_HEAP_CAPACITY, MIDP_HEAP_REQUIREMENT);

    /*
     * Parse options for the VM. This is desirable on a 'development' platform
     * such as linux_qte. For actual device ports, copy this block of code only
     * if your device can handle command-line arguments.
     */

    /* JVM_ParseOneArg expects commandlineArgs[0] to contain the first actual
     * parameter */
    argc --;
    commandlineArgs ++;

    while ((used = JVM_ParseOneArg(argc, commandlineArgs)) > 0) {
        argc -= used;
        commandlineArgs += used;
    }

    /* Restore commandlineArgs[0] to contain the program name. */
    argc ++;
    commandlineArgs --;
    commandlineArgs[0] = progName;

    /*
     * Not all platforms allow rewriting the command line arg array,
     * make a copy
     */
    if (argc > RUNMIDLET_MAX_ARGS) {
        REPORT_ERROR(LC_AMS, "Number of arguments exceeds supported limit");
        JVMSPI_PrintRaw("Number of arguments exceeds supported limit\n");
        return -1;
    }
    for (i = 0; i < argc; i++) {
         argv[i] = commandlineArgs[i];
    }

    if (midpRemoveOptionFlag("-debug", argv, &argc) != NULL ||
        enable_java_debugger) {
        debugOption = MIDP_DEBUG_SUSPEND;
    }

    if (midpRemoveOptionFlag("-loop", argv, &argc) != NULL) {
        repeatMidlet = 1;
    }

    /* additionalPath gets appended to the classpath */
    additionalPath = midpRemoveCommandOption("-classpathext", argv, &argc);

    if (argc == 1) {
        REPORT_ERROR(LC_AMS, "Too few arguments given.");
        JVMSPI_PrintRaw(runUsageText);
        return -1;
    }

    if (argc > 6) {
        REPORT_ERROR(LC_AMS, "Too many arguments given\n");
        JVMSPI_PrintRaw("Too many arguments given\n");
        JVMSPI_PrintRaw(runUsageText);
        return -1;
    }

    /* get midp home directory, set it */
    midpHome = midpFixMidpHome(argv[0]);
    if (midpHome == NULL) {
        return -1;
    }
    /* set up midpHome before calling initialize */
    midpSetHomeDir(midpHome);

    if (midpInitialize() != 0) {
        REPORT_ERROR(LC_AMS, "Not enough memory");
        JVMSPI_PrintRaw("Not enough memory\n");
        return -1;
    }

    do {
        int onlyDigits;
        int len;
        int i;

        if (argc > 5) {
            if (PCSL_STRING_OK != pcsl_string_from_chars(argv[5], &arg2)) {
                REPORT_ERROR(LC_AMS, "Out of Memory");
                JVMSPI_PrintRaw("Out Of Memory\n");
                break;
            }
        }

        if (argc > 4) {
            if (PCSL_STRING_OK != pcsl_string_from_chars(argv[4], &arg1)) {
                REPORT_ERROR(LC_AMS, "Out of Memory");
                JVMSPI_PrintRaw("Out Of Memory\n");
                break;
            }
        }

        if (argc > 3) {
            if (PCSL_STRING_OK != pcsl_string_from_chars(argv[3], &arg0)) {
                REPORT_ERROR(LC_AMS, "Out of Memory");
                JVMSPI_PrintRaw("Out Of Memory\n");
                break;
            }
        }

        if (argc > 2) {
            if (PCSL_STRING_OK != pcsl_string_from_chars(argv[2], &classname)) {
                REPORT_ERROR(LC_AMS, "Out of Memory");
                JVMSPI_PrintRaw("Out Of Memory\n");
                break;
            }

        }

        /* if the storage name only digits, convert it */
        onlyDigits = 1;
        len = strlen(argv[1]);
        for (i = 0; i < len; i++) {
            if (!isdigit((argv[1])[i])) {
                onlyDigits = 0;
                break;
            }
        }

        if (onlyDigits) {
            /* Run by number */
            int suiteNumber;
            MIDPError err;

            /* the format of the string is "number:" */
            if (sscanf(argv[1], "%d", &suiteNumber) != 1) {
                REPORT_ERROR(LC_AMS, "Invalid suite number format");
                JVMSPI_PrintRaw("Invalid suite number format\n");
                break;
            }

            err = midp_get_suite_ids(&pSuites, &numberOfSuites);
            if (err != ALL_OK) {
                REPORT_ERROR1(LC_AMS, "Out of Memory, error %d.",err);
                JVMSPI_PrintRaw("Out Of Memory\n");
                break;
            }

            if (suiteNumber > numberOfSuites || suiteNumber < 1) {
                REPORT_ERROR(LC_AMS, "Suite number out of range");
                JVMSPI_PrintRaw("Suite number out of range\n");
                midp_free_suite_ids(pSuites, numberOfSuites);
                break;
            }

            suiteId = pSuites[suiteNumber - 1];
        } else {
            /* Run by ID */
            suiteId = INTERNAL_SUITE_ID;

            /* IMPL_NOTE: consider handling of other IDs. */
        }

        if (pcsl_string_is_null(&classname)) {
            int res = find_midlet_class(suiteId, 1, &classname);
            if (OUT_OF_MEM_LEN == res) {
                REPORT_ERROR(LC_AMS, "Out of Memory");
                JVMSPI_PrintRaw("Out Of Memory\n");
                break;
            }

            if (NULL_LEN == res) {
                REPORT_ERROR(LC_AMS, "Could not find the first MIDlet");
                JVMSPI_PrintRaw("Could not find the first MIDlet\n");
                break;
            }
        }

        do {
            status = midp_run_midlet_with_args_cp(suiteId, &classname,
                                             &arg0, &arg1, &arg2,
                                             debugOption, additionalPath);
        } while (repeatMidlet && status != MIDP_SHUTDOWN_STATUS);

        if (pSuites != NULL) {
            midp_free_suite_ids(pSuites, numberOfSuites);
            suiteId = UNUSED_SUITE_ID;
        }
    } while (0);

    pcsl_string_free(&arg0);
    pcsl_string_free(&arg1);
    pcsl_string_free(&arg2);
    pcsl_string_free(&classname);

    switch (status) {
    case MIDP_SHUTDOWN_STATUS:
        break;

    case MIDP_ERROR_STATUS:
        REPORT_ERROR(LC_AMS, "The MIDlet suite could not be run.");
        JVMSPI_PrintRaw("The MIDlet suite could not be run.\n");
        break;

    case SUITE_NOT_FOUND_STATUS:
        REPORT_ERROR(LC_AMS, "The MIDlet suite was not found.");
        JVMSPI_PrintRaw("The MIDlet suite was not found.\n");
        break;

    default:
        break;
    }

    midpFinalize();

    return status;
}


static javacall_result
midpHandleStartEvent(midp_jc_event_start_midlet startMidletEvent);

static javacall_result
midpHandleStartTckEvent(midp_jc_event_start_tck startTckEvent);

static javacall_result
midpHandleStartInstallEvent(midp_jc_event_lifecycle startInstallEvent);

static javacall_result
midpHandleStartArbitraryArgEvent(midp_jc_event_start_arbitrary_arg startArbitraryArgEvent);

/**
 * An entry point of a thread devoted to run java
 */
void JavaTask(void) {
    static unsigned long binaryBuffer[BINARY_BUFFER_MAX_LEN/sizeof(long)];
    midp_jc_event_union *event;
    javacall_bool res = JAVACALL_OK;
    javacall_bool JavaTaskIsGoOn = JAVACALL_TRUE;
    long timeTowaitInMillisec = -1;
    int binaryBufferMaxLen = BINARY_BUFFER_MAX_LEN;
    int outEventLen;

    REPORT_CRIT(LC_CORE,"JavaTask() >>\n");

    /* Outer Event Loop */
    while (JavaTaskIsGoOn) {

        if (midpInitializeMemory(-1) != 0) {
            REPORT_CRIT(LC_CORE,"JavaTask() >> midpInitializeMemory()  Not enough memory.\n");
            break;
        }
        REPORT_INFO(LC_CORE,"JavaTask() >> memory initialized.\n");

        res = javacall_event_receive(timeTowaitInMillisec,
            (unsigned char *)binaryBuffer, binaryBufferMaxLen, &outEventLen);

        if (!JAVACALL_SUCCEEDED(res)) {
            REPORT_ERROR(LC_CORE,"JavaTask() >> Error javacall_event_receive()\n");
            continue;
        }

        event = (midp_jc_event_union *) binaryBuffer;
        
        switch (event->eventType) {
        case MIDP_JC_EVENT_START_MIDLET:
            REPORT_INFO(LC_CORE,"JavaTask() >> MIDP_JC_EVENT_START_MIDLET\n");
            javacall_lifecycle_state_changed(JAVACALL_LIFECYCLE_MIDLET_STARTED,
                                             JAVACALL_OK);
            midpHandleStartEvent(event->data.startMidletEvent);

            JavaTaskIsGoOn = JAVACALL_FALSE;
            break;

        case MIDP_JC_EVENT_START_TCK:
            REPORT_INFO(LC_CORE,"JavaTask() >> MIDP_JC_EVENT_START_TCK\n");
            javacall_lifecycle_state_changed(JAVACALL_LIFECYCLE_MIDLET_STARTED,
                                             JAVACALL_OK);
            midpHandleStartTckEvent(event->data.startTckEvent);

            JavaTaskIsGoOn = JAVACALL_FALSE;
            break;

        case MIDP_JC_EVENT_START_INSTALL:
            REPORT_INFO(LC_CORE,"JavaTask() >> MIDP_JC_EVENT_START_INSTALL\n");
            javacall_lifecycle_state_changed(JAVACALL_LIFECYCLE_MIDLET_STARTED,
                                             JAVACALL_OK);
            midpHandleStartInstallEvent(event->data.lifecycleEvent);

            JavaTaskIsGoOn = JAVACALL_FALSE;
            break;

        case MIDP_JC_EVENT_START_ARBITRARY_ARG:
            REPORT_INFO(LC_CORE,"JavaTask() MIDP_JC_EVENT_START_ARBITRARY_ARG>> \n");
            javacall_lifecycle_state_changed(JAVACALL_LIFECYCLE_MIDLET_STARTED,
                                             JAVACALL_OK);
            midpHandleStartArbitraryArgEvent(event->data.startMidletArbitraryArgEvent);

            JavaTaskIsGoOn = JAVACALL_FALSE;
            break;

        case MIDP_JC_EVENT_END:
            REPORT_INFO(LC_CORE,"JavaTask() >> MIDP_JC_EVENT_END\n");
            JavaTaskIsGoOn = JAVACALL_FALSE;
            break;

        default:
            REPORT_ERROR(LC_CORE,"Unknown event.\n");
            break;

        } /* end of switch */

        midpFinalizeMemory();

    }   /* end of while 'JavaTaskIsGoOn' */
        
        REPORT_CRIT(LC_CORE,"JavaTask() <<\n");

} /* end of JavaTask */


static javacall_result
midpHandleStartEvent(midp_jc_event_start_midlet startMidletEvent) {
    char *argv[6];
    int argc = 0;
    javacall_result res;

    argv[argc++] = "runMidlet";
    argv[argc++] = startMidletEvent.suiteID;        /* "internal"; */
    argv[argc++] = startMidletEvent.classname;      /* midletClass; */
    if (startMidletEvent.arg0 != NULL) {
        argv[argc++] = startMidletEvent.arg0;
        if (startMidletEvent.arg1 != NULL) {
            argv[argc++] = startMidletEvent.arg1;
            if (startMidletEvent.arg2 != NULL) {
                argv[argc++] = startMidletEvent.arg2;
            }
        }
    }

    res = runMidlet(argc, argv);
    
    javacall_lifecycle_state_changed(JAVACALL_LIFECYCLE_MIDLET_SHUTDOWN,
                                     (res == 1) ? JAVACALL_OK: JAVACALL_FAIL);

    return res;

}  /* end of midpHandleStartEvent */


static javacall_result
midpHandleStartTckEvent(midp_jc_event_start_tck startTckEvent) {
    char *argv[5];
    int argc = 0;
    javacall_result res;

    argv[argc++] = "runMidlet";
    argv[argc++] = "-1";
    argv[argc++] = "com.sun.midp.installer.AutoTester";

    if(strcmp(startTckEvent.urlAddress, "none") != 0) {
        argv[argc++] = startTckEvent.urlAddress;
    }

    if(startTckEvent.domain == JAVACALL_LIFECYCLE_TCK_DOMAIN_UNTRUSTED) {
        argv[argc++] = "untrusted";
    } else if(startTckEvent.domain == JAVACALL_LIFECYCLE_TCK_DOMAIN_TRUSTED) {
        argv[argc++] = "trusted";
    } else if(startTckEvent.domain == JAVACALL_LIFECYCLE_TCK_DOMAIN_UNTRUSTED_MIN) {
        argv[argc++] = "minimum";
    } else if(startTckEvent.domain == JAVACALL_LIFECYCLE_TCK_DOMAIN_UNTRUSTED_MAX) {
        argv[argc++] = "maximum";
    }

    res = runMidlet(argc, argv);

    javacall_lifecycle_state_changed(JAVACALL_LIFECYCLE_MIDLET_SHUTDOWN,
                                     (res == 1) ? JAVACALL_OK: JAVACALL_FAIL);

    return res;
}


static javacall_result
midpHandleStartInstallEvent(midp_jc_event_lifecycle startInstallEvent) {
    char *argv[5];
    int argc = 0;
    javacall_result res;

    argv[argc++] = "runMidlet";
    argv[argc++] = "-1";
    argv[argc++] = "com.sun.midp.installer.GraphicalInstaller";
    argv[argc++] = "I";
    argv[argc++] = startInstallEvent.urlAddress;

    res = runMidlet(argc, argv);

    javacall_lifecycle_state_changed(JAVACALL_LIFECYCLE_MIDLET_SHUTDOWN,
                                     (res == 1) ? JAVACALL_OK: JAVACALL_FAIL);

    return res;
}

static javacall_result
midpHandleStartArbitraryArgEvent(midp_jc_event_start_arbitrary_arg startArbitraryArgEvent) {
    javacall_result res;

    res = runMidlet(startArbitraryArgEvent.argc, startArbitraryArgEvent.argv);

    javacall_lifecycle_state_changed(JAVACALL_LIFECYCLE_MIDLET_SHUTDOWN,
                                     (res == 1) ? JAVACALL_OK: JAVACALL_FAIL);

    return res;
}
