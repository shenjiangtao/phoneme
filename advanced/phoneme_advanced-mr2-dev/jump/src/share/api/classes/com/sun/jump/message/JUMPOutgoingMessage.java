/*
 * %W% %E%
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

package com.sun.jump.message;

import java.io.IOException;
import com.sun.jump.os.JUMPOSInterface;

/**
 * <code>JUMPOutgoingMessage</code> encapsulates a message that is filled in
 * with data.
 */
public abstract class JUMPOutgoingMessage extends JUMPMessage {
    protected JUMPMessagable sender;
    private int MESSAGE_DATA_INITIAL_SIZE = 512;
    /* offset just past header in message, for sanity checking by native
       side */
    protected int headerOffset;

    /**
     * Creates a new instance of JUMPOutgoingMessage 
     */
    protected JUMPOutgoingMessage(JUMPMessagable sender, 
				  String type,
				  String returnType,
				  int responseId) {
        this.sender = sender;
        this.type = type;
	this.returnType = returnType;
        this.responseId = responseId;
	this.messageDataBytes = new byte[MESSAGE_DATA_INITIAL_SIZE];
	this.messageDataOffset = MESSAGE_DATA_OFFSET;
	composeHeader();
	messageMarkOffset = messageDataOffset;
    }
    
    // used for deserializing the message
    protected JUMPOutgoingMessage() {
	super();
    }
    
    private void composeHeader() {
	/* Here we leave room for the header. The native layer will
	   fill some of this in */
	addInt(0);   // Room for message id
	addInt(this.responseId);    
	addInt(serializeMessagable(sender));   // Sender id
	addUTF(this.returnType); // Add return type 
	addUTF(this.type); // And add type
	//
	// Remember this offset so the native side can sanity check
	//
	this.headerOffset = messageDataOffset;
	
	// User data follows
    }

    //
    // Make sure we can accommodate n more bytes.
    //
    private void ensureCapacity(int n) {
	if (messageDataOffset + n >= messageDataBytes.length) {
	    // must expand
	    byte[] newData = new byte[messageDataBytes.length * 2];
	    System.arraycopy(messageDataBytes, 0,
			     newData, 0, messageDataBytes.length);
	    messageDataBytes = newData;
	}
    }

    /**
     * Add integer to message data
     */
    public void addInt(int i) {
	ensureCapacity(4);
	// Add int at messageDataOffset, big endian
	messageDataBytes[messageDataOffset]   = (byte)((i >>> 24) & 0xFF);
	messageDataBytes[messageDataOffset+1] = (byte)((i >>> 16) & 0xFF);
	messageDataBytes[messageDataOffset+2] = (byte)((i >>>  8) & 0xFF);
	messageDataBytes[messageDataOffset+3] = (byte)((i >>>  0) & 0xFF);
	messageDataOffset += 4;
    }

    public void addLong(long l) {
	ensureCapacity(8);
	// Add int at messageDataOffset, big endian
	messageDataBytes[messageDataOffset+0]   = (byte)(l >>> 56);
	messageDataBytes[messageDataOffset+1]   = (byte)(l >>> 48);
	messageDataBytes[messageDataOffset+2]   = (byte)(l >>> 40);
	messageDataBytes[messageDataOffset+3]   = (byte)(l >>> 32);
	messageDataBytes[messageDataOffset+4]   = (byte)(l >>> 24);
	messageDataBytes[messageDataOffset+5]   = (byte)(l >>> 16);
	messageDataBytes[messageDataOffset+6]   = (byte)(l >>> 8);
	messageDataBytes[messageDataOffset+7]   = (byte)(l >>> 0);
	messageDataOffset += 8;
    }

    public void addByte(byte b) {
	ensureCapacity(1);
	// Add byte at messageDataOffset
	messageDataBytes[messageDataOffset] = b;
	messageDataOffset += 1;
    }

    private byte[] getBytes(String s) {
	char[] val = s.toCharArray();
        int n = val.length;
	byte[] dst = new byte[n+1]; // Leave room for null termination
        int i = 0;
        int j = 0;

        while (i < n) {
            dst[j++] = (byte)val[i++];
        }
	return dst;
    }

    public void addUTF(String s) {
	//
	// FIXME
	// Add string in Java-modified utf-8
	// This code assumes ASCII (and so does the native layer)
	// Example implementation in DataOutputStream.writeUTF().
	//
	if (s == null) {
	    addByteArray(null);
	} else {
	    addByteArray(getBytes(s));
	}
    }

    public void addByteArray(byte[] barr) {
	if (barr == null) {
	    addInt(0);
	    return;
	}
	addInt(barr.length);
	ensureCapacity(barr.length); // the bytes+length
	System.arraycopy(barr, 0, messageDataBytes, messageDataOffset,
			 barr.length);
	messageDataOffset += barr.length;
    }

    public void addUTFArray(String[] arr) {
	if (arr == null) {
	    addInt(0);
	    return;
	}
	addInt(arr.length);
	ensureCapacity(arr.length); // the bytes+length
	for (int i = 0; i < arr.length; i++) {
	    addUTF(arr[i]);
	}
    }

    public byte[] serialize() {
	return messageDataBytes;
    }

    protected abstract int serializeMessagable(JUMPMessagable messagable);

    // This should not be happening for an outgoing message constructed
    // with a sender
    protected void readMessageSender(int id) {
    }
	
}
