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


package de.syss.MifareClassicTool.Activities;

import android.os.Bundle;
import android.text.SpannableString;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.TableLayout.LayoutParams;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import androidx.core.content.ContextCompat;

import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.Common.Operation;
import de.syss.MifareClassicTool.R;

/**
 * Display the MIFARE Access Conditions in a way a user can read.
 * NXP has PDFs describing what access conditions are.
 * Google something like "nxp MIFARE classic access conditions",
 * if you want to have a closer look.
 * This Activity will be shown from the {@link DumpEditor}, if the user
 * clicks the corresponding menu item.
 * @author Gerhard Klostermeier
 */
public class AccessConditionDecoder extends BasicActivity {

    public final static String EXTRA_AC =
            "de.syss.MifareClassicTool.Activity.AC";

    private static final String LOG_TAG =
            AccessConditionDecoder.class.getSimpleName();

    private TableLayout mLayout;

    /**
     * Get access conditions from Intent and initialize Activity to
     * displaying them. If there is no Intent with
     * {@link #EXTRA_AC}, the Activity will be exited.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_access_condition_decoder);

        if (getIntent().hasExtra(EXTRA_AC)) {
            mLayout = findViewById(
                    R.id.tableLayoutAccessConditionDecoder);
            String[] accessConditions =
                getIntent().getStringArrayExtra(EXTRA_AC);

            for (int j = 0; j < accessConditions.length; j=j+2) {
                boolean hasMoreThan4Blocks = false;
                if (accessConditions[j+1].startsWith("*")) {
                    hasMoreThan4Blocks = true;
                    accessConditions[j+1] = accessConditions[j+1].substring(1);
                }

                // b6 = bAC[0], b7 = bAC[1], ...
                byte[] bAC = Common.hex2Bytes(accessConditions[j+1]);

                // acMatrix[C1-C3][Block1-Block3 + Sector Trailer]
                byte[][] acMatrix = Common.acBytesToACMatrix(bAC);
                String sectorNumber = accessConditions[j].split(": ")[1];
                addSectorAC(acMatrix, getString(R.string.text_sector)
                        + ": " + sectorNumber, hasMoreThan4Blocks);
            }
        } else {
            Log.d(LOG_TAG, "There were no access conditions in intent.");
            finish();
        }
    }

    /**
     * Add full access condition information about one sector to the layout
     * table. (This method will trigger
     * {@link #addBlockAC(byte[][], boolean)} and
     * {@link #addSectorTrailerAC(byte[][])}
     * @param acMatrix Matrix of access conditions bits (C1-C3) where the first
     * dimension is the "C" parameter (C1-C3, Index 0-2) and the second
     * dimension is the block number
     * (Block0-Block2 + Sector Trailer, Index 0-3).
     * If this parameter is null, a "invalid ACs" error will be added.
     * @param sectorHeader The sector header to display (e.g. "Sector: 0").
     * @param hasMoreThan4Blocks True for the last 8 sectors
     * of a MIFARE Classic 4K tag.
     * @see #addBlockAC(byte[][], boolean)
     * @see #addSectorTrailerAC(byte[][])
     */
    private void addSectorAC(byte[][] acMatrix, String sectorHeader,
            boolean hasMoreThan4Blocks) {
        // Add sector header.
        TextView header = new TextView(this);
        header.setText(Common.colorString(sectorHeader,
                ContextCompat.getColor(this, R.color.blue)),
                BufferType.SPANNABLE);
        TableRow tr = new TableRow(this);
        tr.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));
        tr.addView(header);
        mLayout.addView(tr, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));

        if (acMatrix == null) {
            TextView error = new TextView(this);
            String errorText = getString(R.string.text_invalid_ac);
            error.setText(Common.colorString(errorText,
                    ContextCompat.getColor(this, R.color.red)),
                    BufferType.SPANNABLE);
            tr = new TableRow(this);
            tr.setLayoutParams(new LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT));
            tr.addView(error);
            mLayout.addView(tr, new LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT));
        } else {
            // Add Block 0-2.
            addBlockAC(acMatrix, hasMoreThan4Blocks);
            // Add Sector Trailer.
            addSectorTrailerAC(acMatrix);
        }
    }

    /**
     * Add full access condition information of the 3 data blocks to the table.
     * @param acMatrix A matrix of access conditions bits as generated by
     * {@link Common#acBytesToACMatrix(byte[])}.
     * @param hasMoreThan4Blocks True for the last 8 sectors
     * of a MIFARE Classic 4K tag, False otherwise.
     */
    private void addBlockAC(byte[][] acMatrix, boolean hasMoreThan4Blocks) {
        boolean isKeyBReadable = Common.isKeyBReadable(
                acMatrix[0][3], acMatrix[1][3], acMatrix[2][3]);

        for (int i = 0; i < 3; i++) {
            byte c1 = acMatrix[0][i];
            byte c2 = acMatrix[1][i];
            byte c3 = acMatrix[2][i];
            // Create row and header.
            TableRow tr = new TableRow(this);
            String blockHeader;
            if (hasMoreThan4Blocks) {
                blockHeader = getString(R.string.text_block)
                        + ": " + (i*4+i) + "-" + (i*4+4+i);
            } else {
                blockHeader = getString(R.string.text_block) + ": " + i;
            }
            tr.setLayoutParams(new LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT));
            // Create cells.
            TextView location = new TextView(this);
            location.setText(blockHeader);
            TextView read = new TextView(this);
            TextView write = new TextView(this);
            TextView incr = new TextView(this);
            TextView decr = new TextView(this);

            // Set cell texts to colored permissions.
            read.setText(getColoredPermissionText(c1, c2, c3,
                    Operation.Read, false, isKeyBReadable));
            write.setText(getColoredPermissionText(c1, c2, c3,
                    Operation.Write, false, isKeyBReadable));
            incr.setText(getColoredPermissionText(c1, c2, c3,
                    Operation.Increment, false, isKeyBReadable));
            decr.setText(getColoredPermissionText(c1, c2, c3,
                    Operation.DecTransRest, false, isKeyBReadable));

            // Add cells to row.
            tr.addView(location);
            tr.addView(read);
            tr.addView(write);
            tr.addView(incr);
            tr.addView(decr);
            // Add row to layout.
            mLayout.addView(tr, new LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT));
        }
    }

    /**
     * Add full access condition information of the sector trailer (last block)
     * to the table.
     * @param acMatrix A matrix of access conditions bits as generated by
     * {@link Common#acBytesToACMatrix(byte[])}.
     * (Block0-Block2 + Sector Trailer, Index 0-3).
     */
    private void addSectorTrailerAC(byte[][] acMatrix) {
        byte c1 = acMatrix[0][3];
        byte c2 = acMatrix[1][3];
        byte c3 = acMatrix[2][3];
        // Create rows.
        TextView[] read = new TextView[3];
        TextView[] write = new TextView[3];
        for (int i = 0; i < 3; i++) {
            read[i] = new TextView(this);
            write[i] = new TextView(this);
        }

        // Set row texts to colored permissions.
        read[0].setText(getColoredPermissionText(c1, c2, c3,
                Operation.ReadKeyA, true, false));
        write[0].setText(getColoredPermissionText(c1, c2, c3,
                Operation.WriteKeyA, true, false));
        read[1].setText(getColoredPermissionText(c1, c2, c3,
                Operation.ReadAC, true, false));
        write[1].setText(getColoredPermissionText(c1, c2, c3,
                Operation.WriteAC, true, false));
        read[2].setText(getColoredPermissionText(c1, c2, c3,
                Operation.ReadKeyB, true, false));
        write[2].setText(getColoredPermissionText(c1, c2, c3,
                Common.Operation.WriteKeyB, true, false));

        // Add rows to layout.
        String[] headers = new String[] {"Key A:", "AC Bits:", "Key B:"};
        for (int i = 0; i < 3; i++) {
            TableRow tr = new TableRow(this);
            tr.setLayoutParams(new LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT));
            TextView location = new TextView(this);
            location.setText(headers[i]);
            tr.addView(location);
            tr.addView(read[i]);
            tr.addView(write[i]);
            mLayout.addView(tr, new LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT));
        }
    }

    /**
     * A helper function for {@link #addBlockAC(byte[][], boolean)} and
     * {@link #addSectorTrailerAC(byte[][])} creating a colored text
     * depending on the Access Conditions and the requested operation
     * ({@link Operation}).
     * @param c1 Access Condition byte "C1"
     * @param c2 Access Condition byte "C2"
     * @param c3 Access Condition byte "C3"
     * @param op Operation on the tag (see {@link Operation}).
     * @param isSectorTrailer True if it is a Sector Trailer, False otherwise.
     * @param isKeyBReadable True if key B is readable, False otherwise.
     * @return A colored text depending on the return value of
     * {@link Common#getOperationRequirements(byte, byte, byte, Operation,
     * boolean, boolean)}. On Error an empty string will be returned.
     */
    private SpannableString getColoredPermissionText(byte c1, byte c2, byte c3,
                                                     Operation op, boolean isSectorTrailer,
                                                     boolean isKeyBReadable) {
        switch (Common.getOperationRequirements(c1, c2, c3, op,
                isSectorTrailer, isKeyBReadable)) {
        case 0:
            // Never.
            return Common.colorString(getString(R.string.text_never),
                    ContextCompat.getColor(this, R.color.orange));
        case 1:
            // Key A.
            return Common.colorString(getString(R.string.text_key_a),
                    ContextCompat.getColor(this, R.color.yellow));
        case 2:
            // Key B.
            return Common.colorString(getString(R.string.text_key_b),
                    ContextCompat.getColor(this, R.color.yellow));
        case 3:
            // Key A|B.
            return Common.colorString(getString(R.string.text_key_ab),
                    ContextCompat.getColor(this, R.color.light_green));
        case 4:
            // Access Condition Error.
            return Common.colorString(getString(R.string.text_ac_error),
                    ContextCompat.getColor(this, R.color.red));
        default:
            // Error:
            return new SpannableString("");
        }
    }
}
