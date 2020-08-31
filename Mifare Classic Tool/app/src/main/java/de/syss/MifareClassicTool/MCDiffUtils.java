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

import java.util.ArrayList;

/**
 * Provides functions to compare two dumps.
 * @author Gerhard Klostermeier
 */
public class MCDiffUtils {

    /**
     * Compare two dumps and get a list of all indices where
     * they differ from each other.
     * @param dump1 The first dump. The sector number is key and the
     * string array represents the blocks.
     * @param dump2 The second dump. The sector number is key and the
     * string array represents the blocks.
     * @return Indices where the two dumps differ. The key represents
     * the sector number. The first dimension of the value represents the
     * block number and the second is a list of indices where dump2 is
     * different from dump1. If the value is Integer[0][0] then the sector
     * exists only in dump1. If the value is Integer[1][0] then the sector
     * exists only in dump2.
     */
    public static SparseArray<Integer[][]> diffIndices(
            SparseArray<String[]> dump1, SparseArray<String[]> dump2) {
        SparseArray<Integer[][]> ret =
                new SparseArray<>();
        // Walk through all sectors of dump1.
        for (int i = 0; i < dump1.size(); i++) {
            String[] sector1 = dump1.valueAt(i);
            int sectorNr = dump1.keyAt(i);
            String[] sector2 = dump2.get(sectorNr);

            // Check if dump2 has the current sector of dump1.
            if (sector2 == null) {
                ret.put(sectorNr, new Integer[0][0]);
                continue;
            }

            // Check the blocks.
            Integer[][] diffSector = new Integer[sector1.length][];
            // Walk through all blocks.
            for (int j = 0; j < sector1.length; j++) {
                ArrayList<Integer> diffIndices = new ArrayList<>();
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
                            new Integer[0]);
                }
            }
            ret.put(sectorNr, diffSector);
        }

        // Are there sectors that occur only in dump2?
        for (int i = 0; i < dump2.size(); i++) {
            int sectorNr = dump2.keyAt(i);
            if (dump1.get(sectorNr) == null) {
                // Sector only exists in dump2.
                ret.put(sectorNr, new Integer[1][0]);
            }
        }

        return ret;
    }

}
