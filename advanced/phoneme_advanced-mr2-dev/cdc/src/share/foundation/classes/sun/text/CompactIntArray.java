/*
 * 
 * @(#)CompactIntArray.java	1.22 06/10/10
 * 
 * Portions Copyright  2000-2006 Sun Microsystems, Inc. All Rights Reserved.
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

/*
 * (C) Copyright Taligent, Inc. 1996 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - All Rights Reserved
 *
 *   The original version of this source code and documentation is copyrighted
 * and owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These
 * materials are provided under terms of a License Agreement between Taligent
 * and Sun. This technology is protected by multiple US and International
 * patents. This notice and attribution to Taligent may not be removed.
 *   Taligent is a registered trademark of Taligent, Inc.
 *
 */

package sun.text;

/**
 * class CompactATypeArray : use only on primitive data types
 * Provides a compact way to store information that is indexed by Unicode
 * values, such as character properties, types, keyboard values, etc.This
 * is very useful when you have a block of Unicode data that contains
 * significant values while the rest of the Unicode data is unused in the
 * application or when you have a lot of redundance, such as where all 21,000
 * Han ideographs have the same value.  However, lookup is much faster than a
 * hash table.
 * A compact array of any primitive data type serves two purposes:
 * <UL type = round>
 *     <LI>Fast access of the indexed values.
 *     <LI>Smaller memory footprint.
 * </UL>
 * A compact array is composed of a index array and value array.  The index
 * array contains the indicies of Unicode characters to the value array.
 *
 * @see                CompactShortArray
 * @see                CompactByteArray
 * @version            1.22 10/10/06
 * @author             Helena Shih
 */
public final class CompactIntArray implements Cloneable {


    /**
     * The total number of Unicode characters.
     */
    public static  final int UNICODECOUNT =65536;

    /**
     * Default constructor for CompactIntArray, the default value of the
     * compact array is 0.
     */
    public CompactIntArray()
    {
        this(0);
    }
    /**
     * Constructor for CompactIntArray.
     * @param defaultValue the default value of the compact array.
     */

    public CompactIntArray(int defaultValue)
    {
        int i;
        values = new int[UNICODECOUNT];
        indices = new short[INDEXCOUNT];
        hashes = new int[INDEXCOUNT];

        for (i = 0; i < UNICODECOUNT; ++i) {
            values[i] = defaultValue;
        }
        for (i = 0; i < INDEXCOUNT; ++i) {
            indices[i] = (short)(i<<BLOCKSHIFT);
            hashes[i] = 0;
        }
        isCompact = false;
    }

    /**
     * Constructor for CompactIntArray.
     * @param indexArray the indicies of the compact array.
     * @param newValues the values of the compact array.
     * @exception IllegalArgumentException If the index is out of range.
     */
    public CompactIntArray(short indexArray[],
                           int newValues[])
    {
        int i;
        if (indexArray.length != INDEXCOUNT)
            throw new IllegalArgumentException("Index out of bounds.");
        for (i = 0; i < INDEXCOUNT; ++i) {
            short index = indexArray[i];
            if ((index < 0) || (index >= newValues.length+BLOCKCOUNT))
                throw new IllegalArgumentException("Index out of bounds.");
        }
        indices = indexArray;
        values = newValues;
        isCompact = true;
    }

    /**
     * Get the mapped value of a Unicode character.
     * @param index the character to get the mapped value with
     * @return the mapped value of the given character
     */
    public int elementAt(char index)
    {
        return (values[(indices[index >> BLOCKSHIFT] & 0xFFFF)
                       + (index & BLOCKMASK)]);
    }

    /**
     * Set a new value for a Unicode character.
     * Set automatically expands the array if it is compacted.
     * @param index the character to set the mapped value with
     * @param value the new mapped value
     */
    public void setElementAt(char index, int value)
    {
        if (isCompact) {
            expand();
        }
        values[(int)index] = value;
        touchBlock(index >> BLOCKSHIFT, value);
    }

    /**
     * Set new values for a range of Unicode character.
     * @param start the startting offset of the range
     * @param end the ending offset of the range
     * @param value the new mapped value
     */
    public void setElementAt(char start, char end, int value)
    {
        int i;
        if (isCompact) {
            expand();
        }
        for (i = start; i <= end; ++i) {
            values[i] = value;
            touchBlock(i >> BLOCKSHIFT, value);
        }
    }

    /**
     * Compact the array.
     */
    public void compact()
    {
        if (!isCompact) {
            int limitCompacted = 0;
            int iBlockStart = 0;
            short iUntouched = -1;

            for (int i = 0; i < indices.length; ++i, iBlockStart += BLOCKCOUNT) {
                indices[i] = -1;
                boolean touched = blockTouched(i);
                if (!touched && iUntouched != -1) {
                    // If no values in this block were set, we can just set its
                    // index to be the same as some other block with no values
                    // set, assuming we've seen one yet.
                    indices[i] = iUntouched;
                } else {
                    int jBlockStart = 0;
                    int j = 0;
                    for (j=0; j < limitCompacted;
                            ++j, jBlockStart += BLOCKCOUNT) {
                        if (hashes[i] == hashes[j] &&
                                Utility.arrayRegionMatches(values, iBlockStart,
                                values, jBlockStart, BLOCKCOUNT)) {
                            indices[i] = (short)jBlockStart;
                            break;
                        }
                    }
                    if (indices[i] == -1) {
                        // we didn't match, so copy & update
                        System.arraycopy(values, iBlockStart,
                            values, jBlockStart, BLOCKCOUNT);
                        indices[i] = (short)jBlockStart;
                        hashes[j] = hashes[i];
                        ++limitCompacted;

                        if (!touched) {
                            // If this is the first untouched block we've seen,
                            // remember its index.
                            iUntouched = (short)jBlockStart;
                        }
                    }
                }
            }

            // we are done compacting, so now make the array shorter
            int newSize = limitCompacted * BLOCKCOUNT;
            int[] result = new int[newSize];
            System.arraycopy(values, 0, result, 0, newSize);
            values = result;
            isCompact = true;
            hashes = null;
        }
    }

    /**
     * Remember that a specified block was "touched", i.e. had a value set.
     * Untouched blocks can be skipped when compacting the array
     */
    private final void touchBlock(int i, int value) {
        hashes[i] = (hashes[i] + (value<<1)) | 1;
    }

    /**
     * Query whether a specified block was "touched", i.e. had a value set.
     * Untouched blocks can be skipped when compacting the array
     */
    private final boolean blockTouched(int i) {
        return hashes[i] != 0;
    }

    /** For internal use only.  Do not modify the result, the behavior of
      * modified results are undefined.
      */
    public short getIndexArray()[]
    {
        return indices;
    }

    /** For internal use only.  Do not modify the result, the behavior of
      * modified results are undefined.
      */
    public int getStringArray()[]
    {
        return values;
    }

    /**
     * Overrides Cloneable
     */
    public Object clone()
    {
        try {
            CompactIntArray other = (CompactIntArray) super.clone();
            other.values = (int[])values.clone();
            other.indices = (short[])indices.clone();

            if (hashes != null) other.hashes = (int[])hashes.clone();
            return other;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    /**
     * Compares the equality of two compact array objects.
     * @param obj the compact array object to be compared with this.
     * @return true if the current compact array object is the same
     * as the compact array object obj; false otherwise.
     */
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj)                      // quick check
            return true;
        if (getClass() != obj.getClass())         // same class?
            return false;
        CompactIntArray other = (CompactIntArray) obj;
        for (int i = 0; i < UNICODECOUNT; i++) {
            // could be sped up later
            if (elementAt((char)i) != other.elementAt((char)i))
                return false;
        }
        return true; // we made it through the gauntlet.
    }

    /**
     * Generates the hash code for the compact array object
     */
    public int hashCode() {
        int result = 0;
        int increment = Math.min(3, values.length/16);
        for (int i = 0; i < values.length; i+= increment) {
            result = result * 37 + values[i];
        }
        return result;
    }

    // --------------------------------------------------------------
    // private
    // --------------------------------------------------------------
    /**
     * Expanded takes the array back to a 65536 element array
     */
    private void expand()
    {
        int i;
        if (isCompact) {
            int[]   tempArray;
            hashes = new int[INDEXCOUNT];
            tempArray = new int[UNICODECOUNT];
            for (i = 0; i < UNICODECOUNT; ++i) {
                int value = elementAt((char)i);
                tempArray[i] = value;
                touchBlock(i >> BLOCKSHIFT, value);
            }
            for (i = 0; i < INDEXCOUNT; ++i) {
                indices[i] = (short)(i<<BLOCKSHIFT);
            }
            values = tempArray;
            isCompact = false;
        }
    }

    private static  final int BLOCKSHIFT =7;
    private static  final int BLOCKCOUNT =(1<<BLOCKSHIFT);
    private static  final int INDEXSHIFT =(16-BLOCKSHIFT);
    private static  final int INDEXCOUNT =(1<<INDEXSHIFT);
    private static  final int BLOCKMASK = BLOCKCOUNT - 1;

    private int[] values;  // char -> int (char parameterized int)
    private short indices[];
    private boolean isCompact;
    private int[] hashes;
};
