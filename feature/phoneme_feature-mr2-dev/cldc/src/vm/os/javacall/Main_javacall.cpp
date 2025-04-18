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

#include "incls/_precompiled.incl"
#include "incls/_Main_javacall.cpp.incl"
#include <stdio.h>
#include <stdlib.h>

#if ENABLE_PCSL
extern "C" {
#include <pcsl_memory.h>
#include <pcsl_print.h>
}
#endif

#include "../utilities/JVM_Malloc.hpp"

#include <javacall_logging.h>

void JVMSPI_PrintRaw(const char* s) {
  /* Print the string to the standard output device */
#if ENABLE_PCSL
  pcsl_print(s);
#else
  javacall_print(s);
#endif
}

void JVMSPI_Exit(int code) {
  /* Terminate the current process */
  return ;
}

int main(int argc, char **argv) {

  int   size = 0x00200000;
  int code;

#if ENABLE_PCSL
  pcsl_mem_initialize(NULL, -1);
#endif

  JVM_Initialize();

  argc --;
  argv ++;

  while (true) {
    int n = JVM_ParseOneArg(argc, argv);
    if (n < 0) {
      printf("Unknown argument: %s\n", argv[0]);
      JVMSPI_DisplayUsage(NULL);
      code = -1;
      goto end;
    } else if (n == 0) {
      break;
    }
    argc -= n;
    argv += n;
  }

  if (JVM_GetConfig(JVM_CONFIG_SLAVE_MODE) == KNI_FALSE) {
    // Run the VM in regular mode -- JVM_Start won't return until
    // the VM completes execution.
    code = JVM_Start(NULL, NULL, argc, argv);
  } else {
    // Run the VM in slave mode -- we keep calling JVM_TimeSlice(),
    // which executes bytecodes for a small amount and returns. This
    // mode is necessary for platforms that need to keep the main
    // control loop outside of of the VM.

    JVM_Start(NULL, NULL, argc, argv);

    for (;;) {
      jlong timeout = JVM_TimeSlice();
      if (timeout <= -2) {
        break;
      } else {
        int blocked_threads_count;
        JVMSPI_BlockedThreadInfo * blocked_threads;

        blocked_threads = SNI_GetBlockedThreads(&blocked_threads_count);
        JVMSPI_CheckEvents(blocked_threads, blocked_threads_count, timeout);
      }
    }

    code = JVM_CleanUp();
  }

end:
#if ENABLE_PCSL
  pcsl_mem_finalize();
#endif

  return code;
}
