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

import java.util.ArrayList;

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
     * @return Indices where dump2 differs from dump1. The key represents
     * the sector number. The first dimension of the value represents the
     * block number and the second is a list of indices where dump2 is
     * different from dump1. If the value is null then the sector only
     * exists in dump2.
     */
    public static SparseArray<Integer[][]> diffIndices(
            String[] dump1, String[] dump2) {
        // Convert dump1 and dump2 to the format the other
        // diffIndices() function likes (SparseArrays).
        return diffIndices(convertDumpFormat(dump1), convertDumpFormat(dump2));
    }

    /**
     * TODO: doc.
     * @param dump1 The first dump. The sector number is key and the
     * string array represents the blocks.
     * @param dump2 The second dump. The sector number is key and the
     * string array represents the blocks.
     * @return Indices where dump2 differs from dump1. The key represents
     * the sector number. The first dimension of the value represents the
     * block number and the second is a list of indices where dump2 is
     * different from dump1. If the value is null then the sector only
     * exists in dump2.
     */
    public static SparseArray<Integer[][]> diffIndices(
            SparseArray<String[]> dump1, SparseArray<String[]> dump2) {
        SparseArray<Integer[][]> ret =
                new SparseArray<Integer[][]>();
        // Walk through all sectors of dump1.
        for (int i = 0; i < dump1.size(); i++) {
            String[] sector1 = dump1.valueAt(i);
            int sectorNr = dump1.keyAt(i);
            String[] sector2 = dump2.get(sectorNr);

            // Check if dump2 has the current sector of dump1.
            if (sector2 == null) {
                ret.put(sectorNr, null);
                continue;
            }

            // Check the blocks.
            Integer[][] diffSector = new Integer[sector1.length][];
            // Walk through all blocks.
            for (int j = 0; j < sector1.length; j++) {
                ArrayList<Integer> diffIndices = new ArrayList<Integer>();
                // Walk through all symbols.
                for (int k = 0; k < sector1[j].length(); k++) {
                    if (sector1[j].charAt(k) != sector2[j].charAt(k)) {
                        // Found different symbol at index k.
                        diffIndices.add(k);
                    }
                }
                if (diffIndices.size() == 0) {
                    // Block was identical.
                    diffSector[j] = new Integer[0];
                } else {
                    diffSector[j] = diffIndices.toArray(
                            new Integer[diffIndices.size()]);
                }
            }
            ret.put(sectorNr, diffSector);
        }

        // Are there sectors that only occur in dump2?
        for (int i = 0; i < dump2.size(); i++) {
            int sectorNr = dump2.keyAt(i);
            if (dump1.get(sectorNr) == null) {
                // Sector only exists in dump2.
                ret.put(sectorNr, null);
            }
        }

        return ret;
    }

    /**
     * Convert the format of an dump.
     * @param dump A dump in the same format a dump file is.
     * (with no comments, not multiple dumps (appended) and validated by
     * {@link Common#isValidDump(String[], boolean)})
     * @return The dump in a key value pair format. The key is the sector
     * number. The value is an String array. Each field of the array
     * represents a block.
     */
    private static SparseArray<String[]> convertDumpFormat(String[] dump) {
        SparseArray<String[]> ret = new SparseArray<String[]>();
        int i = 0;
        for (String line : dump) {
            int sector = 0;
            if (line.startsWith("+")) {
                String[] tmp = line.split(": ");
                sector = Integer.parseInt(tmp[tmp.length-1]);
                i = 0;
                if (sector < 32) {
                    ret.put(sector, new String[4]);
                } else {
                    ret.put(sector, new String[16]);
                }
            } else {
                ret.get(sector)[i++] = line;
            }
        }
        return ret;
    }

}
