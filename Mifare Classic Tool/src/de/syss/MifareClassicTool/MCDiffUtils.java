/*
 * Copyright 2014 Gerhard Klostermeier
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


package de.syss.MifareClassicTool;

import android.util.SparseArray;

/**
 * TODO: doc. & explain why own class (and not the java-diff-utils lib).
 * @author Gerhard Klostermeier
 */
public class MCDiffUtils {

    /**
     * TODO doc.
     * @param dump1 The first dump. The dump has to be clean (no comments
     * and not multiple dumps).
     * @param dump2 The second dump. The dump has to be clean (no comments
     * and not multiple dumps).
     * @return First key is the sector number, second key is the block number
     * and the array contains the indices of the position where the blocks
     * were different.
     */
    public static SparseArray<SparseArray<Integer[]>> diffIndices(
            String[] dump1, String[] dump2) {
        // Convert dump1 and dump2 to the format the other
        // diffIndices() function likes (SparseArrays).

    }

    /**
     * TODO: doc.
     * @param dump1 The first dump. The sector number is key and the
     * string array represents the blocks.
     * @param dump2 The second dump. The sector number is key and the
     * string array represents the blocks.
     * @return First key is the sector number, second key is the block number
     * and the array contains the indices of the position where the blocks
     * were different.
     */
    public static SparseArray<SparseArray<Integer[]>> diffIndices(
            SparseArray<String[]> dump1, SparseArray<String[]> dump2) {

    }

}
