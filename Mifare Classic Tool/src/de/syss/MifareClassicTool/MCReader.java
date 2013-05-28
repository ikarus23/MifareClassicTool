/*
 * Copyright 2013 Gerhard Klostermeier
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.MifareClassic;
import android.util.Log;
import android.util.SparseArray;

/**
 * Provide functions to read/write/analyze a Mifare Classic tag.
 * @author Gerhard Klostermeier
 */
public class MCReader {

    private static final String LOG_TAG = MCReader.class.getSimpleName();
    /**
     * Placeholder for not found keys.
     */
    public  static final String NO_KEY = "------------";
    /**
     * Placeholder for unreadable blocks.
     */
    public  static final String NO_DATA = "--------------------------------";

    private final Tag mTag;
    private final MifareClassic mMFC;
    private SparseArray<byte[][]> mKeyMap = new SparseArray<byte[][]>();
    private int mKeyMapStatus = 0;
    private int mLastSector = -1;
    private int mFirstSector = 0;
    private HashSet<byte[]> mKeys;
    private ArrayList<byte[]> mKeysArray;

    /**
     * Initialize a Mifare Classic reader for the given tag.
     * @param tag The tag to operate on.
     */
    private MCReader(Tag tag) {
        mTag = tag;
        mMFC = MifareClassic.get(mTag);
    }

    /**
     * Get new instance of {@link MCReader}.
     * If the tag is "null" or if it is not a Mifare Classic tag, "null"
     * will be returned.
     * @param tag The tag to operate on.
     * @return {@link MCReader} object or "null" if tag is "null" or tag is
     * not Mifare Classic.
     */
    public static MCReader get(Tag tag) {
        MCReader mcr = null;
        if (tag != null) {
            mcr = new MCReader(tag);
            if (mcr.isMifareClassic() == false) {
                return null;
            }
        }
        return mcr;
    }

    /**
     * Read as much as possible from the tag with the given key information.
     * @param keyMap Keys (A and B) mapped to a sector.
     * See {@link #buildNextKeyMapPart()}.
     * @return A Key-Value Pair. Keys are the sector numbers, values
     * are the tag data. This tag data (values) are arrays containing
     * one block per field (index 0-3 or 0-15).
     * If a block is "null" it means that the block couldn't be
     * read with the given key information.<br />
     * On Error "null" will be returned (most likely the tag was removed
     * during reading). If none of the keys in the key map is valid for reading
     * and therefore no sector is read, an empty set (SparseArray.size() == 0)
     * will be returned.
     * @see #buildNextKeyMapPart()
     */
    public SparseArray<String[]> readAsMuchAsPossible(
            SparseArray<byte[][]> keyMap) {
        SparseArray<String[]> ret = null;
        if (keyMap != null && keyMap.size() > 0) {
            ret = new SparseArray<String[]>(keyMap.size());
            // For all entries in map do:
            for (int i = 0; i < keyMap.size(); i++) {
                String[][] results = new String[2][];
                try {
                    if (keyMap.valueAt(i)[0] != null) {
                        // Read with key A.
                        results[0] = readSector(
                                keyMap.keyAt(i), keyMap.valueAt(i)[0], false);
                    }
                    if (keyMap.valueAt(i)[1] != null) {
                        // Read with key B.
                        results[1] = readSector(
                                keyMap.keyAt(i), keyMap.valueAt(i)[1], true);
                    }
                } catch (TagLostException e) {
                    return null;
                }
                // Merge results.
                if (results[0] != null || results[1] != null) {
                    ret.put(keyMap.keyAt(i), mergeSectorData(
                            results[0], results[1]));
                }
            }
            return ret;
        }
        return ret;
    }

    /**
     * Read as much as possible from the tag depending on the
     * mapping range and the given key information.
     * The key information must be set before calling this method
     * (use {@link #setKeyFile(File[])}).
     * Also the mapping range must be specified before calling this method
     * (use {@link #setMappingRange(int, int)}).
     * Attention: This method builds a key map. Depending on the key count
     * in the given key file, this could take up to minutes and more.
     * The old key map from {@link #getKeyMap()} will be destroyed and
     * the full new one is getable afterwards.
     * @return A Key-Value Pair. Keys are the sector numbers, values
     * are the tag data. This tag data (values) are arrays containing
     * one block per field (index 0-3 or 0-15).
     * If a block is "NULL" it means that the block couldn't be
     * read with the given key information.
     * @see #buildNextKeyMapPart()
     * @see #setKeyFile(File[])
     */
    public SparseArray<String[]> readAsMuchAsPossible() {
        mKeyMapStatus = getSectorCount();
        while (buildNextKeyMapPart() < getSectorCount()-1);
        return readAsMuchAsPossible(mKeyMap);
    }

    /**
     * Read a as much as possible from a sector with the given key.
     * Best results are gained from a valid key B (except key B is marked as
     * readable in the access conditions).
     * @param sectorIndex Index of the Sector to read. (For Mifare Classic 1K:
     * 0-63)
     * @param key Key for the authentication.
     * @param useAsKeyB If true, key will be treated as key B
     * for authentication.
     * @return Array of blocks (index 0-3 or 0-15). If a block or a key is
     * marked with {@link #NO_DATA} or {@link #NO_KEY}
     * it means that this data could be read or found. On authentication error
     * "null" will be returned.
     * @throws TagLostException When tag is lost.
     * @see #mergeSectorData(String[], String[])
     */
    public String[] readSector(int sectorIndex, byte[] key, boolean useAsKeyB)
            throws TagLostException {
        boolean auth = authenticate(sectorIndex, key, useAsKeyB);
        String[] ret = null;
        // Read sector.
        if (auth) {
            // Read all blocks.
            ArrayList<String> blocks = new ArrayList<String>();
            int firstBlock = mMFC.sectorToBlock(sectorIndex);
            int lastBlock = firstBlock + 4;
            if (mMFC.getSize() == MifareClassic.SIZE_4K
                    && sectorIndex > 31) {
                lastBlock = firstBlock + 16;
            }
            for (int i = firstBlock; i < lastBlock; i++) {
                try {
                    blocks.add(Common.byte2HexString(
                            mMFC.readBlock(i)));
                } catch (TagLostException e) {
                    throw e;
                } catch (IOException e) {
                    // Could not read block.
                    // (Maybe due to key/authentication method.)
                    Log.d(LOG_TAG, "Error while reading block "
                            + i + " from tag.");
                    blocks.add(NO_DATA);
                    if (!mMFC.isConnected()) {
                        throw new TagLostException(
                                "Tag removed during readSector(...)");
                    }
                    // After error reauthentication is needed.
                    auth = authenticate(sectorIndex, key, useAsKeyB);
                }
            }
            ret = blocks.toArray(new String[blocks.size()]);
            int last = ret.length -1;
            // Merge key in last block (sector trailer).
            if (!useAsKeyB) {
                if (isKeyBReadable(Common.hexStringToByteArray(
                        ret[last].substring(12, 20)))) {
                    ret[last] = Common.byte2HexString(key)
                            + ret[last].substring(12, 32);
                } else {
                    ret[last] = Common.byte2HexString(key)
                            + ret[last].substring(12, 20) + NO_KEY;
                }
            } else {
                if (ret[0].equals(NO_DATA)) {
                    // If Key B may be read in the corresponding Sector Trailer,
                    // it cannot serve for authentication (according to NXP).
                    // What they mean is that you can authenticate successfully,
                    // but can not read data. In this case the
                    // readBlock() result is 0 for each block.
                    ret = null;
                } else {
                    ret[last] = NO_KEY + ret[last].substring(12, 20)
                            + Common.byte2HexString(key);
                }
            }
        }
        return ret;
    }

    /**
     * Write a block of 16 byte data to tag.
     * @param sectorIndex The sector to where the data should be written
     * @param blockIndex The block to where the data should be written
     * @param data 16 byte of data.
     * @param key The Mifare Classic key for the given sector.
     * @param useAsKeyB If true, key will be treated as key B
     * for authentication.
     * @return The return codes are:<br />
     * <ul>
     * <li>0 - Everything went fine.</li>
     * <li>1 - Sector index is out of range.</li>
     * <li>2 - Block index is out of range.</li>
     * <li>3 - Data are not 16 byte.</li>
     * <li>4 - Authentication went wrong.</li>
     * <li>-1 - Error while writing to tag.</li>
     * </ul>
     * @see #authenticate(int, byte[], boolean)
     */
    public int writeBlock(int sectorIndex, int blockIndex, byte[] data,
            byte[] key, boolean useAsKeyB) {
        if (mMFC.getSectorCount()-1 < sectorIndex) {
            return 1;
        }
        if (mMFC.getBlockCountInSector(sectorIndex)-1 < blockIndex) {
            return 2;
        }
        if (data.length != 16) {
            return 3;
        }
        if (!authenticate(sectorIndex, key, useAsKeyB)) {
            return 4;
        }
        // Write block.
        int block = mMFC.sectorToBlock(sectorIndex) + blockIndex;
        try {
            mMFC.writeBlock(block, data);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error while writing block to tag.", e);
            return -1;
        }
        return 0;
    }

    /**
     * Build Key-Value Pairs in which keys represent the sector and
     * values are one or both of the Mifare keys (A/B).
     * The Mifare key information must be set before calling this method
     * (use {@link #setKeyFile(File[])}).
     * Also the mapping range must be specified before calling this method
     * (use {@link #setMappingRange(int, int)}).<br /><br />
     * The mapping works like some kind of dictionary attack.
     * All keys are checked against the next sector
     * with both authentication methods (A/B). If at least one key was found
     * for a sector, the map will be extended with an entry, containing the
     * key(s) and the information for what sector the key(s) are. You can get
     * this Key-Value Pairs by calling {@link #getKeyMap()}. A full
     * key map can be gained by calling this method as often as there are
     * sectors on the tag (See {@link #getSectorCount()}). If you call
     * this method once more after a full key map was created, it resets the
     * key map an starts all over.
     * @return The sector that was checked at the moment. On error it returns
     * "-1" and resets the key map to "null".
     * @see #getKeyMap()
     * @see #setKeyFile(File[])
     * @see #setMappingRange(int, int)
     * @see #readAsMuchAsPossible(SparseArray)
     */
    public int buildNextKeyMapPart() {
        // Clear status and key map before new walk through sectors.
        boolean error = false;
        if (mKeys != null && mLastSector != -1) {
            if (mKeyMapStatus == mLastSector+1) {
                mKeyMapStatus = mFirstSector;
                mKeyMap = new SparseArray<byte[][]>();
            }

            byte[][] keys = new byte[2][];
            boolean[] foundKeys = new boolean[] {false, false};
            try {
                // Check next sector against all keys (lines) with
                // authentication method A and B.
                for (byte[] key : mKeysArray) {
                    if (!foundKeys[0] &&
                            mMFC.authenticateSectorWithKeyA(
                                    mKeyMapStatus, key)) {
                        keys[0] = key;
                        foundKeys[0] = true;
                    }
                    if (!foundKeys[1] &&
                            mMFC.authenticateSectorWithKeyB(
                                    mKeyMapStatus, key)) {
                        keys[1] = key;
                        foundKeys[1] = true;
                    }
                    if (foundKeys[0] && foundKeys[1]) {
                        // Both keys found. Continue with next sector.
                        break;
                    }
                }
                if (foundKeys[0] || foundKeys[1]) {
                    // At least one key found. Add key(s).
                    mKeyMap.put(mKeyMapStatus, keys);
                    
                    /* Key reuse is very likely, so try these first for the next sector */
                    if (foundKeys[0]) { 
                    	mKeysArray.remove(keys[0]);
                    	mKeysArray.add(0, keys[0]);
                    }
                    if (foundKeys[1]) { 
                    	mKeysArray.remove(keys[1]);
                    	mKeysArray.add(0, keys[1]);
                    }
                }
                mKeyMapStatus++;
            } catch (Exception e) {
                Log.d(LOG_TAG, "Error while building next key map part");
                error = true;
            }
        } else {
            error = true;
        }

        if (error) {
            mKeyMapStatus = 0;
            mKeyMap = null;
            return -1;
        }
        return mKeyMapStatus - 1;
    }

    /**
     * Merge the result of two {@link #readSector(int, byte[], boolean)}
     * calls on the same sector (with different keys or authentication methods).
     * In this case merging means empty blocks will be overwritten with non
     * empty ones and the keys will be added correctly to the sector trailer.
     * The access conditions will be taken from the first (firstResult)
     * parameter if it is not null.
     * @param firstResult First
     * {@link #readSector(int, byte[], boolean)} result.
     * @param secondResult Second
     * {@link #readSector(int, byte[], boolean)} result.
     * @return Array (sector) as result of merging the given
     * sectors. If a block is {@link #NO_DATA} it
     * means that none of the given sectors contained data from this block.
     * @see #readSector(int, byte[], boolean)
     * @see #authenticate(int, byte[], boolean)
     */
    public String[] mergeSectorData(String[] firstResult,
            String[] secondResult) {
        String[] ret = null;
        if (firstResult != null || secondResult != null) {
            if ((firstResult != null && secondResult != null)
                    && firstResult.length != secondResult.length) {
                return null;
            }
            int length  = (firstResult != null)
                    ? firstResult.length : secondResult.length;
            ArrayList<String> blocks = new ArrayList<String>();
            // Merge data blocks.
            for (int i = 0; i < length -1 ; i++) {
                if (firstResult != null && firstResult[i] != null
                        && !firstResult[i].equals(NO_DATA)) {
                    blocks.add(firstResult[i]);
                } else if (secondResult != null && secondResult[i] != null
                        && !secondResult[i].equals(NO_DATA)) {
                    blocks.add(secondResult[i]);
                } else {
                    // Non of the results got the data for the block.
                    blocks.add(NO_DATA);
                }
            }
            ret = blocks.toArray(new String[blocks.size() + 1]);
            int last = length - 1;
            // Merge sector trailer.
            if (firstResult != null && firstResult[last] != null
                    && !firstResult[last].equals(NO_DATA)) {
                // Take first for sector trailer.
                ret[last] = firstResult[last];
                if (secondResult != null && secondResult[last] != null
                        && !secondResult[last].equals(NO_DATA)) {
                    // Merge key form second result to sector trailer.
                    ret[last] = ret[last].substring(0, 20)
                            + secondResult[last].substring(20);
                }
            } else if (secondResult != null && secondResult[last] != null
                    && !secondResult[last].equals(NO_DATA)) {
                // No first result. Take second result as sector trailer.
                ret[last] = secondResult[last];
            } else {
                // No sector trailer at all.
                ret[last] = NO_DATA;
            }
        }
        return ret;
    }

    /**
     * This method checks if the present tag is writable with the provided keys
     * on the given positions (sectors, blocks). This is done by authenticating
     * with one of the keys followed by reading and interpreting
     * ({@link Common#getOperationInfoForBlock(byte, byte, byte,
     * de.syss.MifareClassicTool.Common.Operations, boolean, boolean)}) of the
     * Access Conditions.
     * @param pos A map of positions (key = sector, value = Array of blocks).
     * For each of these positions you will get the write information
     * (see return values).
     * @param keyMap A key map a generated by
     * {@link Activities.CreateKeyMapActivity}.
     * @return A map within a map (all with type = Integer).
     * The key of the outer map is the sector number and the value is another
     * map with key = block number and value = write information.
     * The write information indicates which key is needed to write to the
     * present tag on the given position.<br /><br />
     * Write informations are:<br />
     * <ul>
     * <li>0 - Never</li>
     * <li>1 - Key A</li>
     * <li>2 - Key B</li>
     * <li>3 - Key A|B</li>
     * <li>4 - Key A, but AC never</li>
     * <li>5 - Key B, but AC never</li>
     * <li>6 - Key B, but keys never</li>
     * <li>-1 - Error</li>
     * <li>Inner map == null - Whole sector is dead (IO Error)</li>
     * </ul>
     */
    public HashMap<Integer, HashMap<Integer, Integer>> isWritableOnPositions(
            HashMap<Integer, int[]> pos,
            SparseArray<byte[][]> keyMap) {
        HashMap<Integer, HashMap<Integer, Integer>> ret =
                new HashMap<Integer, HashMap<Integer,Integer>>();
        for (int i = 0; i < keyMap.size(); i++) {
            int sector = keyMap.keyAt(i);
            if (pos.containsKey(sector)) {
                byte[][] keys = keyMap.get(sector);
                byte[] ac = null;
                // Authenticate.
                if (keys[0] != null) {
                    if (authenticate(sector, keys[0], false) == false) {
                        return null;
                    }
                } else if (keys[1] != null) {
                    if (authenticate(sector, keys[1], true) == false) {
                        return null;
                    }
                } else {
                    return null;
                }
                // Read Mifare Access Conditions.
                int acBlock = mMFC.sectorToBlock(sector)
                        + mMFC.getBlockCountInSector(sector) -1;
                try {
                    ac = mMFC.readBlock(acBlock);
                } catch (IOException e) {
                    ret.put(sector, null);
                    continue;
                }
                ac = Arrays.copyOfRange(ac, 6, 9);
                byte[][] acMatrix = Common.acToACMatrix(ac);
                boolean isKeyBReadable = Common.isKeyBReadable(
                        acMatrix[0][3], acMatrix[1][3], acMatrix[2][3]);

                // Check all Blocks with data (!= null).
                HashMap<Integer, Integer> blockWithWriteInfo =
                        new HashMap<Integer, Integer>();
                for (int block : pos.get(sector)) {
                    if ((block == 3 && sector <= 31)
                            || (block == 15 && sector >= 32)) {
                        // Sector Trailer.
                        // Are the Access Bits writable?
                        int acValue = Common.getOperationInfoForBlock(
                                acMatrix[0][3],
                                acMatrix[1][3],
                                acMatrix[2][3],
                                Common.Operations.WriteAC,
                                true, isKeyBReadable);
                        // Is key A writable? (If so, key B will be writable
                        // with the same key.)
                        int keyABValue = Common.getOperationInfoForBlock(
                                acMatrix[0][3],
                                acMatrix[1][3],
                                acMatrix[2][3],
                                Common.Operations.WriteKeyA,
                                true, isKeyBReadable);

                        int result = keyABValue;
                        if (acValue == 0 && keyABValue != 0) {
                            // Write key found, but ac-bits are not writable.
                            result += 3;
                        } else if (acValue == 2 && keyABValue == 0) {
                            // Access Bits are writable with key B,
                            // but keys are not writable.
                            result = 6;
                        }
                        blockWithWriteInfo.put(block, result);
                    } else {
                        // Data block.
                        int acBitsForBlock = block;
                        // Handle Mifare Classic 4k Tags.
                        if (sector >= 32) {
                            if (block >= 0 && block <= 4) {
                                acBitsForBlock = 0;
                            } else if (block >= 5 && block <= 9) {
                                acBitsForBlock = 1;
                            } else if (block >= 10 && block <= 14) {
                                acBitsForBlock = 2;
                            }
                        }
                        blockWithWriteInfo.put(
                                block, Common.getOperationInfoForBlock(
                                        acMatrix[0][acBitsForBlock],
                                        acMatrix[1][acBitsForBlock],
                                        acMatrix[2][acBitsForBlock],
                                        Common.Operations.Write,
                                        false, isKeyBReadable));
                    }

                }
                if (blockWithWriteInfo.size() > 0) {
                    ret.put(sector, blockWithWriteInfo);
                }
            }
        }
        return ret;
    }

    /**
     * Set the key files for {@link #buildNextKeyMapPart()}.
     * @param keyFiles One or more key files.
     * These files are simple text files with one key
     * per line. Empty lines and lines STARTING with "#"
     * will not be interpreted.
     */
    public void setKeyFile(File[] keyFiles) {
        mKeys = new HashSet<byte[]>();
        for (File file : keyFiles) {
            String[] lines = Common.readFileLineByLine(file, false);
            if (lines != null) {
                for (String line : lines) {
                    if (!line.equals("") && line.length() == 12
                            && line.matches("[0-9A-Fa-f]+")) {
                        mKeys.add(Common.hexStringToByteArray(line));
                    }
                }
            }
        }
        mKeysArray = new ArrayList<byte[]>(mKeys);
    }

    /**
     * Set the mapping range for {@link #buildNextKeyMapPart()}.
     * @param firstSector Index of the first sector of the key map.
     * @param lastSector Index of the last sector of the key map.
     * @return True if range parameters were correct. False otherwise.
     */
    public boolean setMappingRange(int firstSector, int lastSector) {
        if (firstSector >= 0 && lastSector < mMFC.getSectorCount()
                && firstSector <= lastSector) {
            mFirstSector = firstSector;
            mLastSector = lastSector;
            // Init. status of buildNextKeyMapPart to create a new key map.
            mKeyMapStatus = lastSector+1;
            return true;
        }
        return false;
    }

    /**
     * Check if the Mifare Classic tag has the factory Mifare Classic Access
     * Conditions (0xFF0780) and the standard key A
     * (0xFFFFFFFFFFFF).
     * @return True if tag has factory ACs and factory key A, False otherwise.
     */
    public boolean isCleanTag() {
        int blockIndex = 0;
        for (int i = 0; i < mMFC.getSectorCount(); i++) {
            // Authenticate.
            if (!authenticate(i, MifareClassic.KEY_DEFAULT, false)) {
                return false;
            }
            // Read.
            byte[] data = null;
            blockIndex += mMFC.getBlockCountInSector(i);
            try {
                data = mMFC.readBlock(blockIndex-1);
            } catch (IOException e) {
                Log.d(LOG_TAG, "Error while reading block from tag.");
                return false;
            }
            // Extract Access Conditions.
            String ac = Common.byte2HexString(data).substring(12, 18);
            // Check Access Conditions (= Factory settings).
            if (!ac.equals("FF0780")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Authenticate to given sector of the tag.
     * @param sectorIndex The sector to authenticate to.
     * @param key Key for the authentication.
     * @param useAsKeyB If true, key will be treated as key B
     * for authentication.
     * @return True if authentication was successful. False otherwise.
     */
    private boolean authenticate(int sectorIndex, byte[] key,
            boolean useAsKeyB) {
        try {
            if (!useAsKeyB) {
                // Key A.
                return mMFC.authenticateSectorWithKeyA(sectorIndex, key);
            } else {
                // Key B.
                return mMFC.authenticateSectorWithKeyB(sectorIndex, key);
            }
        } catch (IOException e) {
            Log.d(LOG_TAG, "Error while authenticate with tag.");
        }
        return false;
    }

    /**
     * Check if key B is readable.
     * Key B is readable for the following configurations:
     * <ul>
     * <li>C1 = 0, C2 = 0, C3 = 0</li>
     * <li>C1 = 0, C2 = 0, C3 = 1</li>
     * <li>C1 = 0, C2 = 1, C3 = 0</li>
     * </ul>
     * @param ac The access conditions (4 bytes).
     * @return True if key B is readable. False otherwise.
     */
    private boolean isKeyBReadable(byte[] ac) {
        byte c1 = (byte) ((ac[1] & 0x80) >>> 7);
        byte c2 = (byte) ((ac[2] & 0x08) >>> 3);
        byte c3 = (byte) ((ac[2] & 0x80) >>> 7);
        if (c1 == 0
                && (c2 == 0 && c3 == 0)
                || (c2 == 1 && c3 == 0)
                || (c2 == 0 && c3 == 1)) {
            return true;
        }
        return false;
    }

    /**
     * Get the key map build from {@link #buildNextKeyMapPart()} with
     * the given key file ({@link #setKeyFile(File[])}). If you want a
     * full key map, you have to call {@link #buildNextKeyMapPart()} as
     * often as there are sectors on the tag
     * (See {@link #getSectorCount()}).
     * @return A Key-Value Pair. Keys are the sector numbers,
     * values are the Mifare keys.
     * The Mifare keys are 2D arrays with key type (first dimension, 0-1,
     * 0 = KeyA / 1 = KeyB) and key (second dimension, 0-6). If a key "null"
     * it means that the key A or B (depending of first dimension) could not
     * be found.
     * @see #getSectorCount()
     * @see #buildNextKeyMapPart()
     */
    public SparseArray<byte[][]> getKeyMap() {
        return mKeyMap;
    }

    public boolean isMifareClassic() {
        if (mMFC == null) {
            return false;
        }
        return true;
    }

    /**
     * Return the size of the Mifare Classic tag in bit.
     * (e.g. Mifare Classic 1k = 1024)
     * @return The size of the current tag.
     */
    public int getSize() {
        return mMFC.getSize();
    }

    /**
     * Return the sector count of the Mifare Classic tag.
     * @return The sector count of the current tag.
     */
    public int getSectorCount() {
        return mMFC.getSectorCount();
    }

    /**
     * Return the block count of the Mifare Classic tag.
     * @return The block count of the current tag.
     */
    public int getBlockCount() {
        return mMFC.getBlockCount();
    }

    /**
     * Return the block count in a specific sector.
     * @param sectorIndex Index of a sector.
     * @return Block count in given sector.
     */
    public int getBlockCountInSector(int sectorIndex) {
        return mMFC.getBlockCountInSector(sectorIndex);
    }

    /**
     * Check if the reader is connected to the tag.
     * @return True if the reader is connected. False otherwise.
     */
    public boolean isConnected() {
        return mMFC.isConnected();
    }

    /**
     * Connect the reader to the tag.
     */
    public void connect() {
        try {
            mMFC.connect();
        } catch (IOException e) {
            Log.d(LOG_TAG, "Error while connecting to tag.");
        }
    }

    /**
     * Close the connection between reader an tag.
     */
    public void close() {
        try {
            mMFC.close();
        }
        catch (IOException e) {
            Log.d(LOG_TAG, "Error on closing tag.");
        }
    }
}
