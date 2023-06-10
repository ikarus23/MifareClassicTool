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

import android.content.Context;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

import de.syss.MifareClassicTool.Activities.Preferences.Preference;
import de.syss.MifareClassicTool.Common.Operation;

/**
 * Provides functions to read/write/analyze a MIFARE Classic tag.
 * @author Gerhard Klostermeier
 */
public class MCReader {

    private static final String LOG_TAG = MCReader.class.getSimpleName();
    /**
     * Placeholder for not found keys.
     */
    public static final String NO_KEY = "------------";
    /**
     * Placeholder for unreadable blocks.
     */
    public static final String NO_DATA = "--------------------------------";
    /**
     * Default key of MIFARE Classic tags.
     */
    public static final String DEFAULT_KEY = "FFFFFFFFFFFF";

    private final MifareClassic mMFC;
    private SparseArray<byte[][]> mKeyMap = new SparseArray<>();
    private int mKeyMapStatus = 0;
    private int mLastSector = -1;
    private int mFirstSector = 0;
    private ArrayList<String> mKeysWithOrder;
    private boolean mHasAllZeroKey = false;

    /**
     * Initialize a MIFARE Classic reader for the given tag.
     * @param tag The tag to operate on.
     */
    private MCReader(Tag tag) {
        MifareClassic tmpMFC;
        try {
            tmpMFC = MifareClassic.get(tag);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Could not create MIFARE Classic reader for the"
                    + "provided tag (even after patching it).");
            throw e;
        }
        mMFC = tmpMFC;
    }

    /**
     * Patch a possibly broken Tag object of HTC One (m7/m8) or Sony
     * Xperia Z3 devices (with Android 5.x.)
     *
     * HTC One: "It seems, the reason of this bug is TechExtras of NfcA is null.
     * However, TechList contains MifareClassic." -- bildin.
     * This method will fix this. For more information please refer to
     * https://github.com/ikarus23/MifareClassicTool/issues/52
     * This patch was provided by bildin (https://github.com/bildin).
     *
     * Sony Xperia Z3 (+ emmulated MIFARE Classic tag): The buggy tag has
     * two NfcA in the TechList with different SAK values and a MifareClassic
     * (with the Extra of the second NfcA). Both, the second NfcA and the
     * MifareClassic technique, have a SAK of 0x20. According to NXP's
     * guidelines on identifying MIFARE tags (Page 11), this a MIFARE Plus or
     * MIFARE DESFire tag. This method creates a new Extra with the SAK
     * values of both NfcA occurrences ORed (as mentioned in NXP's
     * MIFARE type identification procedure guide) and replace the Extra of
     * the first NfcA with the new one. For more information please refer to
     * https://github.com/ikarus23/MifareClassicTool/issues/64
     * This patch was provided by bildin (https://github.com/bildin).
     *
     * @param tag The possibly broken tag.
     * @return The fixed tag.
     */
    public static Tag patchTag(Tag tag) {
        if (tag == null) {
            return null;
        }

        String[] techList = tag.getTechList();

        Parcel oldParcel = Parcel.obtain();
        tag.writeToParcel(oldParcel, 0);
        oldParcel.setDataPosition(0);

        int len = oldParcel.readInt();
        byte[] id = new byte[0];
        if (len >= 0) {
            id = new byte[len];
            oldParcel.readByteArray(id);
        }
        int[] oldTechList = new int[oldParcel.readInt()];
        oldParcel.readIntArray(oldTechList);
        Bundle[] oldTechExtras = oldParcel.createTypedArray(Bundle.CREATOR);
        int serviceHandle = oldParcel.readInt();
        int isMock = oldParcel.readInt();
        IBinder tagService;
        if (isMock == 0) {
            tagService = oldParcel.readStrongBinder();
        } else {
            tagService = null;
        }
        oldParcel.recycle();

        int nfcaIdx = -1;
        int mcIdx = -1;
        short sak = 0;
        boolean isFirstSak = true;

        for (int i = 0; i < techList.length; i++) {
            if (techList[i].equals(NfcA.class.getName())) {
                if (nfcaIdx == -1) {
                    nfcaIdx = i;
                }
                if (oldTechExtras[i] != null
                        && oldTechExtras[i].containsKey("sak")) {
                    sak = (short) (sak
                            | oldTechExtras[i].getShort("sak"));
                    isFirstSak = nfcaIdx == i;
                }
            } else if (techList[i].equals(MifareClassic.class.getName())) {
                mcIdx = i;
            }
        }

        boolean modified = false;

        // Patch the double NfcA issue (with different SAK) for
        // Sony Z3 devices.
        if (!isFirstSak) {
            oldTechExtras[nfcaIdx].putShort("sak", sak);
            modified = true;
        }

        // Patch the wrong index issue for HTC One devices.
        if (nfcaIdx != -1 && mcIdx != -1 && oldTechExtras[mcIdx] == null) {
            oldTechExtras[mcIdx] = oldTechExtras[nfcaIdx];
            modified = true;
        }

        if (!modified) {
            // Old tag was not modivied. Return the old one.
            return tag;
        }

        // Old tag was modified. Create a new tag with the new data.
        Parcel newParcel = Parcel.obtain();
        newParcel.writeInt(id.length);
        newParcel.writeByteArray(id);
        newParcel.writeInt(oldTechList.length);
        newParcel.writeIntArray(oldTechList);
        newParcel.writeTypedArray(oldTechExtras, 0);
        newParcel.writeInt(serviceHandle);
        newParcel.writeInt(isMock);
        if (isMock == 0) {
            newParcel.writeStrongBinder(tagService);
        }
        newParcel.setDataPosition(0);
        Tag newTag = Tag.CREATOR.createFromParcel(newParcel);
        newParcel.recycle();

        return newTag;
    }

    /**
     * Get new instance of {@link MCReader}.
     * If the tag is "null" or if it is not a MIFARE Classic tag, "null"
     * will be returned.
     * @param tag The tag to operate on.
     * @return {@link MCReader} object or "null" if tag is "null" or tag is
     * not MIFARE Classic.
     */
    public static MCReader get(Tag tag) {
        MCReader mcr = null;
        if (tag != null) {
            try {
                mcr = new MCReader(tag);
                if (!mcr.isMifareClassic()) {
                    return null;
                }
            } catch (RuntimeException ex) {
                // Should not happen. However, it did happen for OnePlus5T
                // user according to Google Play crash reports.
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
     * On Error, "null" will be returned (tag was removed during reading or
     * keyMap is null). If none of the keys in the key map are valid for reading
     * (and therefore no sector is read), an empty set (SparseArray.size() == 0)
     * will be returned.
     * @see #buildNextKeyMapPart()
     */
    public SparseArray<String[]> readAsMuchAsPossible(
            SparseArray<byte[][]> keyMap) {
        SparseArray<String[]> resultSparseArray;
        if (keyMap != null && keyMap.size() > 0) {
            resultSparseArray = new SparseArray<>(keyMap.size());
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
                    resultSparseArray.put(keyMap.keyAt(i), mergeSectorData(
                            results[0], results[1]));
                }
            }
            return resultSparseArray;
        }
        return null;
    }

    /**
     * Read as much as possible from the tag depending on the
     * mapping range and the given key information.
     * The key information must be set before calling this method
     * (use {@link #setKeyFile(File[], Context)}).
     * Also the mapping range must be specified before calling this method
     * (use {@link #setMappingRange(int, int)}).
     * Attention: This method builds a key map. Depending on the key count
     * in the given key file, this could take more than a few minutes.
     * The old key map from {@link #getKeyMap()} will be destroyed and
     * the full new one is gettable afterwards.
     * @return A Key-Value Pair. Keys are the sector numbers, values
     * are the tag data. The tag data (values) are arrays containing
     * one block per field (index 0-3 or 0-15).
     * If a block is "null" it means that the block couldn't be
     * read with the given key information.
     * @see #buildNextKeyMapPart()
     * @see #setKeyFile(File[], Context)
     */
    public SparseArray<String[]> readAsMuchAsPossible() {
        mKeyMapStatus = getSectorCount();
        while (buildNextKeyMapPart() < getSectorCount()-1);
        return readAsMuchAsPossible(mKeyMap);
    }

    /**
     * Read as much as possible from a sector with the given key.
     * Best results are gained from a valid key B (except key B is marked as
     * readable in the access conditions).
     * @param sectorIndex Index of the Sector to read. (For MIFARE Classic 1K:
     * 0-63)
     * @param key Key for authentication.
     * @param useAsKeyB If true, key will be treated as key B
     * for authentication.
     * @return Array of blocks (index 0-3 or 0-15). If a block or a key is
     * marked with {@link #NO_DATA} or {@link #NO_KEY}
     * it means that this data could not be read or found. On authentication error
     * "null" will be returned.
     * @throws TagLostException When connection with/to tag is lost.
     * @see #mergeSectorData(String[], String[])
     */
    public String[] readSector(int sectorIndex, byte[] key,
            boolean useAsKeyB) throws TagLostException {
        boolean auth = authenticate(sectorIndex, key, useAsKeyB);
        String[] ret = null;
        // Read sector.
        if (auth) {
            // Read all blocks.
            ArrayList<String> blocks = new ArrayList<>();
            int firstBlock = mMFC.sectorToBlock(sectorIndex);
            int lastBlock = firstBlock + 4;
            if (mMFC.getSize() == MifareClassic.SIZE_4K
                    && sectorIndex > 31) {
                lastBlock = firstBlock + 16;
            }
            for (int i = firstBlock; i < lastBlock; i++) {
                try {
                    byte[] blockBytes = mMFC.readBlock(i);
                    // mMFC.readBlock(i) must return 16 bytes or throw an error.
                    // At least this is what the documentation says.
                    // On Samsung's Galaxy S5 and Sony's Xperia Z2 however, it
                    // sometimes returns < 16 bytes for unknown reasons.
                    // Update: Aaand sometimes it returns more than 16 bytes...
                    // The appended byte(s) are 0x00.
                    if (blockBytes.length < 16) {
                        throw new IOException();
                    }
                    if (blockBytes.length > 16) {
                        blockBytes = Arrays.copyOf(blockBytes,16);
                    }

                    blocks.add(Common.bytes2Hex(blockBytes));
                } catch (TagLostException e) {
                    throw e;
                } catch (IOException e) {
                    // Could not read block.
                    // (Maybe due to key/authentication method.)
                    Log.d(LOG_TAG, "(Recoverable) Error while reading block "
                            + i + " from tag.");
                    blocks.add(NO_DATA);
                    if (!mMFC.isConnected()) {
                        throw new TagLostException(
                                "Tag removed during readSector(...)");
                    }
                    // After an error, a re-authentication is needed.
                    authenticate(sectorIndex, key, useAsKeyB);
                }
            }
            ret = blocks.toArray(new String[0]);
            int last = ret.length -1;

            // Validate if it was possible to read any data.
            boolean noData = true;
            for (String s : ret) {
                if (!s.equals(NO_DATA)) {
                    noData = false;
                    break;
                }
            }
            if (noData) {
                // Was is possible to read any data (especially with key B)?
                // If Key B may be read in the corresponding Sector Trailer,
                // it cannot serve for authentication (according to NXP).
                // What they mean is that you can authenticate successfully,
                // but can not read data. In this case the
                // readBlock() result is 0 for each block.
                // Also, a tag might be bricked in a way that the authentication
                // works, but reading data does not.
                ret = null;
            } else {
                // Merge key in last block (sector trailer).
                if (!useAsKeyB) {
                    if (isKeyBReadable(Common.hex2Bytes(
                            ret[last].substring(12, 20)))) {
                        ret[last] = Common.bytes2Hex(key)
                                + ret[last].substring(12, 32);
                    } else {
                        ret[last] = Common.bytes2Hex(key)
                                + ret[last].substring(12, 20) + NO_KEY;
                    }
                } else {
                    ret[last] = NO_KEY + ret[last].substring(12, 20)
                            + Common.bytes2Hex(key);
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
     * @param key The MIFARE Classic key for the given sector.
     * @param useAsKeyB If true, key will be treated as key B
     * for authentication.
     * @return The return codes are:<br />
     * <ul>
     * <li>0 - Everything went fine.</li>
     * <li>1 - Sector index is out of range.</li>
     * <li>2 - Block index is out of range.</li>
     * <li>3 - Data are not 16 bytes.</li>
     * <li>4 - Authentication went wrong.</li>
     * <li>-1 - Error while writing to tag.</li>
     * </ul>
     * @see #authenticate(int, byte[], boolean)
     */
    public int writeBlock(int sectorIndex, int blockIndex, byte[] data,
            byte[] key, boolean useAsKeyB) {
        if (getSectorCount()-1 < sectorIndex) {
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
        // NOTE: See warning on writeBlock0Gen3().
//        if (block == 0) {
//            // Try first to write block 0 using the gen3 approach. This must be done
//            // before using the gen2 (normal) approach, because gen3 always just return
//            // a write success even if it fails.
//            int writeGen3block0 = 0;
//            writeGen3block0 = writeBlock0Gen3(data, key, useAsKeyB);
//            if (writeGen3block0 == 0) {
//                return 0;
//            }
//        }
        try {
            // Normal write (also feasible for block 0 of gen2 cards).
            mMFC.writeBlock(block, data);
        } catch (IOException e) {
//            if (block == 0) {
//                // Writing to block 0 failed. Maybe it is a gen3 card. Try it.
//                return writeBlock0Gen3(data);
//            }
            Log.e(LOG_TAG, "Error while writing block to tag.", e);
            return -1;
        }
        return 0;
    }

    // WARNING: This function is based on the description from here:
    // https://github.com/RfidResearchGroup/proxmark3/blob/master/doc/magic_cards_notes.md#mifare-classic-apdu-aka-gen3
    // When tested, it did work, however, sectors 0-31 bricked on the 4k tag that was used.
    // Changing the UID again was still possible. However, something does not seem to be stable,
    // Therefore this function is not triggered right now.
    /**
     * Write block 0 of a gen3 card using an APDU (no authentication needed).
     * @param data The data of block 0, 16 bytes.
     * @return
     * <ul>
     * <li>0 - success</li>
     * <li>1 - block 0 data are not 16 bytes long</li>
     * <li>-1 - Something went wrong during the attempt to write block 0</li>
     * </ul>
     */
    public int writeBlock0Gen3(byte[] data) {
        if (data.length != 16) {
            return 1;
        }
        // Write block.
        byte[] writeCommand = {(byte)0x90, (byte)0xF0, (byte)0xCC, (byte)0xCC, (byte)0x10};
        byte[] fullCommand = new byte[writeCommand.length + data.length];
        System.arraycopy(writeCommand, 0, fullCommand, 0, writeCommand.length);
        System.arraycopy(data, 0, fullCommand, writeCommand.length, data.length);
        try {
            NfcA gen3Tag = NfcA.get(mMFC.getTag());
            if (gen3Tag == null) {
                throw new IOException("Tag is not IsoDep compatible.");
            }
            mMFC.close();
            gen3Tag.connect();
            byte[] response = gen3Tag.transceive(fullCommand);
            // TODO: check response for success.
            gen3Tag.close();
            mMFC.connect();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error while writing block to tag.", e);
            return -1;
        }
        return 0;
    }

    /**
     * Increase or decrease a Value Block.
     * @param sectorIndex The sector to where the data should be written
     * @param blockIndex The block to where the data should be written
     * @param value Increase or decrease Value Block by this value.
     * @param increment If true, increment Value Block by value. Decrement
     * if false.
     * @param key The MIFARE Classic key for the given sector.
     * @param useAsKeyB If true, key will be treated as key B
     * for authentication.
     * @return The return codes are:<br />
     * <ul>
     * <li>0 - Everything went fine.</li>
     * <li>1 - Sector index is out of range.</li>
     * <li>2 - Block index is out of range.</li>
     * <li>3 - Authentication went wrong.</li>
     * <li>-1 - Error while writing to tag.</li>
     * </ul>
     * @see #authenticate(int, byte[], boolean)
     */
    public int writeValueBlock(int sectorIndex, int blockIndex, int value,
                          boolean increment, byte[] key, boolean useAsKeyB) {
        if (getSectorCount()-1 < sectorIndex) {
            return 1;
        }
        if (mMFC.getBlockCountInSector(sectorIndex)-1 < blockIndex) {
            return 2;
        }
        if (!authenticate(sectorIndex, key, useAsKeyB)) {
            return 3;
        }
        // Write Value Block.
        int block = mMFC.sectorToBlock(sectorIndex) + blockIndex;
        try {
            if (increment) {
                mMFC.increment(block, value);
            } else {
                mMFC.decrement(block, value);
            }
            mMFC.transfer(block);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error while writing Value Block to tag.", e);
            return -1;
        }
        return 0;
    }

    /**
     * Build Key-Value Pairs in which keys represent the sector and
     * values are one or both of the MIFARE keys (A/B).
     * The MIFARE key information must be set before calling this method
     * (use {@link #setKeyFile(File[], Context)}).
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
     * key map and starts all over.
     * @return The sector that was just checked. On an error condition,
     * it returns "-1" and resets the key map to "null".
     * @see #getKeyMap()
     * @see #setKeyFile(File[], Context)
     * @see #setMappingRange(int, int)
     * @see #readAsMuchAsPossible(SparseArray)
     */
    public int buildNextKeyMapPart() {
        // Clear status and key map before new walk through sectors.
        boolean error = false;
        if (mKeysWithOrder != null && mLastSector != -1) {
            if (mKeyMapStatus == mLastSector+1) {
                mKeyMapStatus = mFirstSector;
                mKeyMap = new SparseArray<>();
            }

            // Get auto reconnect setting.
            boolean autoReconnect = Common.getPreferences().getBoolean(
                    Preference.AutoReconnect.toString(), false);
            // Get retry authentication option.
            boolean retryAuth = Common.getPreferences().getBoolean(
                    Preference.UseRetryAuthentication.toString(), false);
            int retryAuthCount = Common.getPreferences().getInt(
                    Preference.RetryAuthenticationCount.toString(), 1);

            String[] keys = new String[2];
            boolean[] foundKeys = new boolean[] {false, false};
            boolean auth;

            // Check next sector against all keys (lines) with
            // authentication method A and B.
            keysloop:
            for (int i = 0; i < mKeysWithOrder.size(); i++) {
                String key = mKeysWithOrder.get(i);
                byte[] bytesKey = Common.hex2Bytes(key);
                for (int j = 0; j < retryAuthCount+1;) {
                    try {
                        if (!foundKeys[0]) {
                            auth = mMFC.authenticateSectorWithKeyA(
                                    mKeyMapStatus, bytesKey);
                            if (auth) {
                                keys[0] = key;
                                foundKeys[0] = true;
                            }
                        }
                        if (!foundKeys[1]) {
                            auth = mMFC.authenticateSectorWithKeyB(
                                    mKeyMapStatus, bytesKey);
                            if (auth) {
                                keys[1] = key;
                                foundKeys[1] = true;
                            }
                        }
                    } catch (Exception e) {
                        Log.d(LOG_TAG,
                                "Error while building next key map part");
                        if (autoReconnect) {
                            // Is the tag still in range?
                            if (isConnectedButTagLost()) {
                                close();
                            }
                            while (!isConnected()) {
                                // Sleep for 500ms.
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException ex) {
                                    // Do nothing.
                                }
                                // Try to reconnect.
                                try {
                                    connect();
                                } catch (Exception ex) {
                                    // Do nothing.
                                }
                            }
                            // Repeat last loop (do not incr. j).
                            continue;
                        } else {
                            error = true;
                            break keysloop;
                        }
                    }
                    // Retry?
                    if((foundKeys[0] && foundKeys[1]) || !retryAuth) {
                        // Both keys found or no retry wanted. Stop retrying.
                        break;
                    }
                    j++;
                }
                // Next key?
                if ((foundKeys[0] && foundKeys[1])) {
                    // Both keys found. Stop searching for keys.
                    break;
                }
            }
            if (!error && (foundKeys[0] || foundKeys[1])) {
                // At least one key found. Add key(s).
                byte[][] bytesKeys = new byte[2][];
                bytesKeys[0] = Common.hex2Bytes(keys[0]);
                bytesKeys[1] = Common.hex2Bytes(keys[1]);
                mKeyMap.put(mKeyMapStatus, bytesKeys);
                // Key reuse is very likely, so try the found keys first or,
                // if a all all-0 key is present, second.
                // The all-F key has to be tested always first if there
                // is a all-0 key in the key file, because of a bug in
                // some tags and/or devices.
                // https://github.com/ikarus23/MifareClassicTool/issues/66
                if (mKeysWithOrder.size() > 2) {
                    if (foundKeys[0]) {
                        mKeysWithOrder.remove(keys[0]);
                        if (mHasAllZeroKey && !keys[0].equals(DEFAULT_KEY)) {
                            mKeysWithOrder.add(1, keys[0]);
                        } else {
                            mKeysWithOrder.add(0, keys[0]);
                        }
                    }
                    if (foundKeys[1]) {
                        mKeysWithOrder.remove(keys[1]);
                        if (mHasAllZeroKey && !keys[1].equals(DEFAULT_KEY)) {
                            mKeysWithOrder.add(1, keys[1]);
                        } else {
                            mKeysWithOrder.add(0, keys[1]);
                        }
                    }
                }
            }
            mKeyMapStatus++;
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
            ArrayList<String> blocks = new ArrayList<>();
            // Merge data blocks.
            for (int i = 0; i < length -1 ; i++) {
                if (firstResult != null && firstResult[i] != null
                        && !firstResult[i].equals(NO_DATA)) {
                    blocks.add(firstResult[i]);
                } else if (secondResult != null && secondResult[i] != null
                        && !secondResult[i].equals(NO_DATA)) {
                    blocks.add(secondResult[i]);
                } else {
                    // None of the results got the data form the block.
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
     * at the given positions (sectors, blocks). This is done by authenticating
     * with one of the keys followed by reading and interpreting
     * ({@link Common#getOperationRequirements(byte, byte, byte,
     * Common.Operation, boolean, boolean)}) of the
     * Access Conditions.
     * @param pos A map of positions (key = sector, value = Array of blocks).
     * For each of these positions you will get the write information
     * (see return values).
     * @param keyMap A key map generated by
     * {@link de.syss.MifareClassicTool.Activities.KeyMapCreator}.
     * @return A map within a map (all with type = Integer).
     * The key of the outer map is the sector number and the value is another
     * map with key = block number and value = write information.
     * The write information indicates which key is needed to write to the
     * present tag at the given position.<br /><br />
     * Write return codes are:<br />
     * <ul>
     * <li>0 - Never</li>
     * <li>1 - Key A</li>
     * <li>2 - Key B</li>
     * <li>3 - Key A|B</li>
     * <li>4 - Key A, but AC never</li>
     * <li>5 - Key B, but AC never</li>
     * <li>6 - Key B, but keys never</li>
     * <li>-1 - Error</li>
     * <li>Inner map == null - Whole sector is dead (IO Error) or ACs are
     *  incorrect</li>
     * <li>null - Authentication error</li>
     * </ul>
     */
    public HashMap<Integer, HashMap<Integer, Integer>> isWritableOnPositions(
            HashMap<Integer, int[]> pos,
            SparseArray<byte[][]> keyMap) {
        HashMap<Integer, HashMap<Integer, Integer>> ret =
                new HashMap<>();
        for (int i = 0; i < keyMap.size(); i++) {
            int sector = keyMap.keyAt(i);
            if (pos.containsKey(sector)) {
                byte[][] keys = keyMap.get(sector);
                byte[] ac;
                // Authenticate.
                if (keys[0] != null) {
                    if (!authenticate(sector, keys[0], false)) {
                        return null;
                    }
                } else if (keys[1] != null) {
                    if (!authenticate(sector, keys[1], true)) {
                        return null;
                    }
                } else {
                    return null;
                }
                // Read MIFARE Access Conditions.
                int acBlock = mMFC.sectorToBlock(sector)
                        + mMFC.getBlockCountInSector(sector) -1;
                try {
                    ac = mMFC.readBlock(acBlock);
                } catch (Exception e) {
                    ret.put(sector, null);
                    continue;
                }
                // mMFC.readBlock(i) must return 16 bytes or throw an error.
                // At least this is what the documentation says.
                // On Samsung's Galaxy S5 and Sony's Xperia Z2 however, it
                // sometimes returns < 16 bytes for unknown reasons.
                // Update: Aaand sometimes it returns more than 16 bytes...
                // The appended byte(s) are 0x00.
                if (ac.length < 16) {
                    ret.put(sector, null);
                    continue;
                }

                ac = Arrays.copyOfRange(ac, 6, 9);
                byte[][] acMatrix = Common.acBytesToACMatrix(ac);
                if (acMatrix == null) {
                    ret.put(sector, null);
                    continue;
                }
                boolean isKeyBReadable = Common.isKeyBReadable(
                        acMatrix[0][3], acMatrix[1][3], acMatrix[2][3]);

                // Check all Blocks with data (!= null).
                HashMap<Integer, Integer> blockWithWriteInfo =
                        new HashMap<>();
                for (int block : pos.get(sector)) {
                    if ((block == 3 && sector <= 31)
                            || (block == 15 && sector >= 32)) {
                        // Sector Trailer.
                        // Are the Access Bits writable?
                        int acValue = Common.getOperationRequirements(
                                acMatrix[0][3],
                                acMatrix[1][3],
                                acMatrix[2][3],
                                Operation.WriteAC,
                                true, isKeyBReadable);
                        // Is key A writable? (If so, key B will be writable
                        // with the same key.)
                        int keyABValue = Common.getOperationRequirements(
                                acMatrix[0][3],
                                acMatrix[1][3],
                                acMatrix[2][3],
                                Operation.WriteKeyA,
                                true, isKeyBReadable);

                        int result = keyABValue;
                        if (acValue == 0 && keyABValue != 0) {
                            // Write key found, but AC-bits are not writable.
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
                        // Handle MIFARE Classic 4k Tags.
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
                                block, Common.getOperationRequirements(
                                        acMatrix[0][acBitsForBlock],
                                        acMatrix[1][acBitsForBlock],
                                        acMatrix[2][acBitsForBlock],
                                        Operation.Write,
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
     * Key duplicates from the key file will be removed.
     * @param keyFiles One or more key files.
     * These files are simple text files with one key
     * per line. Empty lines and lines STARTING with "#"
     * will not be interpreted.
     * @param context The context in which the possible "Out of memory"-Toast
     * will be shown.
     * @return Number of keys loaded. -1 on error.
     */
    public int setKeyFile(File[] keyFiles, Context context) {
        if (keyFiles == null || keyFiles.length == 0 || context == null) {
            return -1;
        }
        HashSet<String> keys = new HashSet<>();
        for (File file : keyFiles) {
            String[] lines = Common.readFileLineByLine(file, false, context);
            if (lines != null) {
                for (String line : lines) {
                    if (!line.equals("") && line.length() == 12
                            && line.matches("[0-9A-Fa-f]+")) {
                        try {
                            keys.add(line);
                        } catch (OutOfMemoryError e) {
                            // Error. Too many keys (out of memory).
                            Toast.makeText(context, R.string.info_to_many_keys,
                                    Toast.LENGTH_LONG).show();
                            return -1;
                        }
                    }
                }
            }
        }
        if (keys.size() > 0) {
            mHasAllZeroKey = keys.contains("000000000000");
            mKeysWithOrder = new ArrayList<>(keys);
            if (mHasAllZeroKey) {
                // NOTE: The all-F key has to be tested always first if there
                // is a all-0 key in the key file, because of a bug in
                // some tags and/or devices.
                // https://github.com/ikarus23/MifareClassicTool/issues/66
                mKeysWithOrder.remove(DEFAULT_KEY);
                mKeysWithOrder.add(0, DEFAULT_KEY);
            }
            return keys.size();
        }
        return 0;
    }

    /**
     * Set the mapping range for {@link #buildNextKeyMapPart()}.
     * @param firstSector Index of the first sector of the key map.
     * @param lastSector Index of the last sector of the key map.
     * @return True if range parameters were correct. False otherwise.
     */
    public boolean setMappingRange(int firstSector, int lastSector) {
        if (firstSector >= 0 && lastSector < getSectorCount()
                && firstSector <= lastSector) {
            mFirstSector = firstSector;
            mLastSector = lastSector;
            // Init. status of buildNextKeyMapPart to create a new key map.
            mKeyMapStatus = lastSector+1;
            return true;
        }
        return false;
    }

    // TODO: Make this a function with three return values.
    // 0 = Auth. successful.
    // 1 = Auth. not successful.
    // 2 = Error. Most likely tag lost.
    // Once done, update the code of buildNextKeyMapPart().
    /**
     * Authenticate with given sector of the tag.
     * @param sectorIndex The sector with which to authenticate.
     * @param key Key for the authentication.
     * @param useAsKeyB If true, key will be treated as key B
     * for authentication.
     * @return True if authentication was successful. False otherwise.
     */
    private boolean authenticate(int sectorIndex, byte[] key,
            boolean useAsKeyB) {
        // Fetch the retry authentication option. Some tags and
        // devices have strange issues and need a retry in order to work...
        // Info: https://github.com/ikarus23/MifareClassicTool/issues/134
        // and https://github.com/ikarus23/MifareClassicTool/issues/106
        boolean retryAuth = Common.getPreferences().getBoolean(
                Preference.UseRetryAuthentication.toString(), false);
        int retryCount = Common.getPreferences().getInt(
                Preference.RetryAuthenticationCount.toString(), 1);
        if (key == null) {
            return false;
        }
        boolean ret = false;
        for (int i = 0; i < retryCount+1; i++) {
            try {
                if (!useAsKeyB) {
                    // Key A.
                    ret = mMFC.authenticateSectorWithKeyA(sectorIndex, key);
                } else {
                    // Key B.
                    ret = mMFC.authenticateSectorWithKeyB(sectorIndex, key);
                }
            } catch (IOException | ArrayIndexOutOfBoundsException e) {
                Log.d(LOG_TAG, "Error authenticating with tag.");
                return false;
            }
            // Retry?
            if (ret || !retryAuth) {
                break;
            }
        }
        return ret;
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
        if (ac == null) {
            return false;
        }
        byte c1 = (byte) ((ac[1] & 0x80) >>> 7);
        byte c2 = (byte) ((ac[2] & 0x08) >>> 3);
        byte c3 = (byte) ((ac[2] & 0x80) >>> 7);
        return c1 == 0
                && (c2 == 0 && c3 == 0)
                || (c2 == 1 && c3 == 0)
                || (c2 == 0 && c3 == 1);
    }

    /**
     * Get the key map built from {@link #buildNextKeyMapPart()} with
     * the given key file ({@link #setKeyFile(File[], Context)}). If you want a
     * full key map, you have to call {@link #buildNextKeyMapPart()} as
     * often as there are sectors on the tag
     * (See {@link #getSectorCount()}).
     * @return A Key-Value Pair. Keys are the sector numbers,
     * values are the MIFARE keys.
     * The MIFARE keys are 2D arrays with key type (first dimension, 0-1,
     * 0 = KeyA / 1 = KeyB) and key (second dimension, 0-6). If a key is "null"
     * it means that the key A or B (depending in the first dimension) could not
     * be found.
     * @see #getSectorCount()
     * @see #buildNextKeyMapPart()
     */
    public SparseArray<byte[][]> getKeyMap() {
        return mKeyMap;
    }

    public boolean isMifareClassic() {
        return mMFC != null;
    }

    /**
     * Return the size of the MIFARE Classic tag in bits.
     * (e.g. MIFARE Classic 1k = 1024)
     * @return The size of the current tag.
     */
    public int getSize() {
        return mMFC.getSize();
    }

    /**
     * Return the sector count of the MIFARE Classic tag.
     * @return The sector count of the current tag.
     */
    public int getSectorCount() {
        boolean useCustomSectorCount = Common.getPreferences().getBoolean(
                Preference.UseCustomSectorCount.toString(), false);
        if (useCustomSectorCount) {
            return Common.getPreferences().getInt(
                    Preference.CustomSectorCount.toString(), 16);

        }
        return mMFC.getSectorCount();
    }

    /**
     * Return the block count of the MIFARE Classic tag.
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
     * Return the sector that contains a given block.
     * (Taken from https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/nfc/tech/MifareClassic.java)
     * @param blockIndex index of block to lookup, starting from 0
     * @return sector index that contains the block
     */
    public static int blockToSector(int blockIndex) {
        if (blockIndex < 0 || blockIndex >= 256) {
            throw new IndexOutOfBoundsException(
                    "Block out of bounds: " + blockIndex);
        }
        if (blockIndex < 32 * 4) {
            return blockIndex / 4;
        } else {
            return 32 + (blockIndex - 32 * 4) / 16;
        }
    }

    /**
     * Check if the reader is connected to the tag.
     * This is NOT an indicator that the tag is in range.
     * @return True if the reader is connected. False otherwise.
     */
    public boolean isConnected() {
        return mMFC.isConnected();
    }

    /**
     * Check if the reader is connected, but the tag is lost
     * (not in range anymore).
     * @return True if tag is lost. False otherwise.
     */
    public boolean isConnectedButTagLost() {
        if (isConnected()) {
            try {
                mMFC.readBlock(0);
            } catch (IOException e) {
                return true;
            }
        }
        return false;
    }

    /**
     * Connect the reader to the tag. If the reader is already connected the
     * "connect" will be skipped. If "connect" will block for more than 500ms
     * then connecting will be aborted.
     * @throws Exception Something went wrong while connecting to the tag.
     */
    public void connect() throws Exception {
        final AtomicBoolean error = new AtomicBoolean(false);

        // Do not connect if already connected.
        if (isConnected()) {
            return;
        }

        // Connect in a worker thread. (connect() might be blocking).
        Thread t = new Thread(() -> {
            try {
                mMFC.connect();
            } catch (IOException | IllegalStateException ex) {
                error.set(true);
            }
        });
        t.start();

        // Wait for the connection (max 500millis).
        try {
            t.join(500);
        } catch (InterruptedException ex) {
            error.set(true);
        }

        // If there was an error log it and throw an exception.
        if (error.get()) {
            Log.d(LOG_TAG, "Error while connecting to tag.");
            throw new Exception("Error while connecting to tag.");
        }
    }

    /**
     * Close the connection between reader and tag.
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
