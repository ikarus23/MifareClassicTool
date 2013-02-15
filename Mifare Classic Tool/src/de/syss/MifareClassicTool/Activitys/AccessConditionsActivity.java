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


package de.syss.MifareClassicTool.Activitys;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.TableLayout.LayoutParams;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.R;

/**
 * Display the Mifare Access Conditions in a way a user can read.
 * NXP has PDFs describing what access conditions are.
 * Google something like "nxp mifare classic access conditions",
 * if you want to have a closer look.
 * This Activity will be shown from the {@link DumpEditorActivity}, if the user
 * clicks on the show ACs button.
 * @author Gerhard Klostermeier
 */
public class AccessConditionsActivity extends Activity {

    // LOW: Pass a better object then a stringblobb separated by new line.
    // (See http://stackoverflow.com/a/2141166)

    public final static String EXTRA_AC =
            "de.syss.MifareClassicTool.Activity.AC";

    private static final String LOG_TAG =
            AccessConditionsActivity.class.getSimpleName();

    private TableLayout mLayout;

    /**
     * Get access conditions from Intent and initialize Activity to
     * displaying them. If there is no Intent with
     * {@link #EXTRA_AC}, the Activity will be exited.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_access_conditions);

        if (getIntent().hasExtra(EXTRA_AC)) {
            mLayout = (TableLayout) findViewById(
                    R.id.TableLayoutAccessConditions);
            String extra = getIntent().getStringExtra(EXTRA_AC);
            String[] accessConditions = extra.split(
                    System.getProperty("line.separator"));
            for (int j = 0; j < accessConditions.length; j=j+2) {
                boolean hasMoreThan4Blocks = false;
                if (accessConditions[j+1].startsWith("+")) {
                    hasMoreThan4Blocks = true;
                    accessConditions[j+1] = accessConditions[j+1].substring(1);
                }

                // b6 = bAC[0], b7 = bAC[1], ...
                byte[] bAC = Common.hexStringToByteArray(accessConditions[j+1]);

                // acMatrix[C1-C3][Block1-Block3 + Sector Trailer]
                byte[][] acMatrix = new byte[3][4];

                // ACs correct?
                // C1 (Byte 7, 4-7) == ~C1 (Byte 6, 0-3) and
                // C2 (Byte 8, 0-3) == ~C2 (Byte 6, 4-7) and
                // C3 (Byte 8, 4-7) == ~C3 (Byte 7, 0-3)
                if ((byte)((bAC[1]>>>4)&0x0F)  == (byte)((bAC[0]^0xFF)&0x0F) &&
                    (byte)(bAC[2]&0x0F) == (byte)(((bAC[0]^0xFF)>>>4)&0x0F) &&
                    (byte)((bAC[2]>>>4)&0x0F)  == (byte)((bAC[1]^0xFF)&0x0F)) {
                    // C1, Block 0-4
                    for (int i = 0; i < 4; i++) {
                        acMatrix[0][i] = (byte)((bAC[1]>>>4+i)&0x01);
                    }
                    // C2, Block 0-4
                    for (int i = 0; i < 4; i++) {
                        acMatrix[1][i] = (byte)((bAC[2]>>>i)&0x01);
                    }
                    // C3, Block 0-4
                    for (int i = 0; i < 4; i++) {
                        acMatrix[2][i] = (byte)((bAC[2]>>>4+i)&0x01);
                    }
                    addSectorAC(acMatrix, accessConditions[j].substring(1),
                            hasMoreThan4Blocks);
                }
            }
        } else {
            Log.d(LOG_TAG, "There were no access conditions in intent.");
            finish();
        }
    }

    /**
     * Enable NFC foreground dispatch system.
     * @see Common#disableNfcForegroundDispatch(Activity)
     */
    @Override
    public void onResume() {
        super.onResume();
        Common.enableNfcForegroundDispatch(this);
    }

    /**
     * Disable NFC foreground dispatch system.
     * @see Common#disableNfcForegroundDispatch(Activity)
     */
    @Override
    public void onPause() {
        super.onPause();
        Common.disableNfcForegroundDispatch(this);
    }

    /**
     * Handle new Intent as a new tag Intent.
     * @see Common#treatAsNewTag(Intent, android.content.Context)
     */
    @Override
    public void onNewIntent(Intent intent) {
        Common.treatAsNewTag(intent, this);
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
     * @param sectorHeader The sector header to display (e.g. "Sector: 0").
     * @param hasMoreThan4Blocks True for the last 8 sectors
     * of a Mifare Classic 4K tag.
     * @see #addBlockAC(byte[][], boolean)
     * @see #addSectorTrailerAC(byte[][])
     */
    private void addSectorAC(byte[][] acMatrix, String sectorHeader,
            boolean hasMoreThan4Blocks) {
        // Add sector header.
        TextView header = new TextView(this);
        header.setText(Common.colorString(sectorHeader,
                getResources().getColor(R.color.blue)),
                BufferType.SPANNABLE);
        TableRow tr = new TableRow(this);
        tr.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));
        tr.addView(header);
        mLayout.addView(tr, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));
        // Add Block 0-2.
        addBlockAC(acMatrix, hasMoreThan4Blocks);
        // Add Sector Trailer.
        addSectorTrailerAC(acMatrix);
    }

    /**
     * Add full access condition information of the 3 data blocks to the table.
     * This method contains the hard coded access condition table for
     * data blocks from the NXP PDF mentioned in
     * {@link AccessConditionsActivity}.
     * @param acMatrix Matrix of access conditions bits (C1-C3) where the first
     * dimension is the "C" parameter (C1-C3, Index 0-2) and the second
     * dimension is the block number
     * (Block0-Block2 + Sector Trailer, Index 0-3).
     * @param hasMoreThan4Blocks True for the last 8 sectors
     * of a Mifare Classic 4K tag.
     */
    private void addBlockAC(byte[][] acMatrix, boolean hasMoreThan4Blocks) {
        boolean isKeyBReadable = isKeyBReadable(
                acMatrix[0][3], acMatrix[1][3], acMatrix[2][3]);

        for (int i = 0; i < 3; i++) {
            byte c1 = acMatrix[0][i];
            byte c2 = acMatrix[1][i];
            byte c3 = acMatrix[2][i];
            TableRow tr = new TableRow(this);
            String blockHeader = "";
            if (hasMoreThan4Blocks) {
                blockHeader = getString(R.string.text_block)
                        + " " + (i*4+i) + "-" + (i*4+4+i);
            } else {
                blockHeader = getString(R.string.text_block) + " " + i;
            }
            tr.setLayoutParams(new LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT));
            TextView location = new TextView(this);
            location.setText(blockHeader);
            TextView read = new TextView(this);
            TextView write = new TextView(this);
            TextView incr = new TextView(this);
            TextView decr = new TextView(this);

            if          (c1 == 0 && c2 == 0 && c3 == 0) {
                if (isKeyBReadable) {
                    read.setText("Key A");
                    read.setTextColor(getResources().getColor(R.color.yellow));
                    write.setText("Key A");
                    write.setTextColor(getResources().getColor(R.color.yellow));
                    incr.setText("Key A");
                    incr.setTextColor(getResources().getColor(R.color.yellow));
                    decr.setText("Key A");
                    decr.setTextColor(getResources().getColor(R.color.yellow));
                } else {
                    read.setText("Key A|B");
                    read.setTextColor(getResources().getColor(
                            R.color.light_green));
                    write.setText("Key A|B");
                    write.setTextColor(getResources().getColor(
                            R.color.light_green));
                    incr.setText("Key A|B");
                    incr.setTextColor(getResources().getColor(
                            R.color.light_green));
                    decr.setText("Key A|B");
                    decr.setTextColor(getResources().getColor(
                            R.color.light_green));
                }
            } else if   (c1 == 0 && c2 == 1 && c3 == 0) {
                if (isKeyBReadable) {
                    read.setText("Key A");
                    read.setTextColor(getResources().getColor(R.color.yellow));
                } else {
                    read.setText("Key A|B");
                    read.setTextColor(getResources().getColor(
                            R.color.light_green));
                }
                write.setText("Never");
                write.setTextColor(getResources().getColor(R.color.orange));
                incr.setText("Never");
                incr.setTextColor(getResources().getColor(R.color.orange));
                decr.setText("Never");
                decr.setTextColor(getResources().getColor(R.color.orange));
            } else if   (c1 == 1 && c2 == 0 && c3 == 0) {
                if (isKeyBReadable) {
                    read.setText("Key A");
                    read.setTextColor(getResources().getColor(R.color.yellow));
                } else {
                    read.setText("Key A|B");
                    read.setTextColor(getResources().getColor(
                            R.color.light_green));
                }
                write.setText("Key B");
                write.setTextColor(getResources().getColor(R.color.yellow));
                incr.setText("Never");
                incr.setTextColor(getResources().getColor(R.color.orange));
                decr.setText("Never");
                decr.setTextColor(getResources().getColor(R.color.orange));
            } else if   (c1 == 1 && c2 == 1 && c3 == 0) {
                if (isKeyBReadable) {
                    read.setText("Key A");
                    read.setTextColor(getResources().getColor(R.color.yellow));
                    decr.setText("Key A");
                    decr.setTextColor(getResources().getColor(R.color.yellow));
                } else {
                    read.setText("Key A|B");
                    read.setTextColor(getResources().getColor(
                            R.color.light_green));
                    decr.setText("Key A|B");
                    decr.setTextColor(getResources().getColor(
                            R.color.light_green));
                }
                incr.setText("Key B");
                write.setTextColor(getResources().getColor(R.color.yellow));
                write.setText("Key B");
                incr.setTextColor(getResources().getColor(R.color.yellow));
            } else if   (c1 == 0 && c2 == 0 && c3 == 1) {
                if (isKeyBReadable) {
                    read.setText("Key A");
                    read.setTextColor(getResources().getColor(R.color.yellow));
                    decr.setText("Key A");
                    decr.setTextColor(getResources().getColor(R.color.yellow));
                } else {
                    read.setText("Key A|B");
                    read.setTextColor(getResources().getColor(
                            R.color.light_green));
                    decr.setText("Key A|B");
                    decr.setTextColor(getResources().getColor(
                            R.color.light_green));
                }
                write.setText("Never");
                write.setTextColor(getResources().getColor(R.color.orange));
                incr.setText("Never");
                incr.setTextColor(getResources().getColor(R.color.orange));
            } else if   (c1 == 0 && c2 == 1 && c3 == 1) {
                read.setText("Key B");
                read.setTextColor(getResources().getColor(R.color.yellow));
                write.setText("Key B");
                write.setTextColor(getResources().getColor(R.color.yellow));
                incr.setText("Never");
                incr.setTextColor(getResources().getColor(R.color.orange));
                decr.setText("Never");
                decr.setTextColor(getResources().getColor(R.color.orange));
            } else if   (c1 == 1 && c2 == 0 && c3 == 1) {
                read.setText("Key B");
                read.setTextColor(getResources().getColor(R.color.yellow));
                write.setText("Never");
                write.setTextColor(getResources().getColor(R.color.orange));
                incr.setText("Never");
                incr.setTextColor(getResources().getColor(R.color.orange));
                decr.setText("Never");
                decr.setTextColor(getResources().getColor(R.color.orange));
            } else if   (c1 == 1 && c2 == 1 && c3 == 1) {
                read.setText("Never");
                read.setTextColor(getResources().getColor(R.color.orange));
                write.setText("Never");
                write.setTextColor(getResources().getColor(R.color.orange));
                incr.setText("Never");
                incr.setTextColor(getResources().getColor(R.color.orange));
                decr.setText("Never");
                decr.setTextColor(getResources().getColor(R.color.orange));
            } else {
                read.setText(R.string.text_ac_error);
                read.setTextColor(getResources().getColor(R.color.red));
            }
            // Add fields to row.
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
     * to the table. This method contains the hard coded access condition table
     * for sector trailers from the NXP PDF mentioned in
     * {@link AccessConditionsActivity}.
     * @param acMatrix Matrix of access conditions bits (C1-C3) where the first
     * dimension is the "C" parameter (C1-C3, Index 0-2) and the second
     * dimension is the block number
     * (Block0-Block2 + Sector Trailer, Index 0-3).
     */
    private void addSectorTrailerAC(byte[][] acMatrix) {
        byte c1 = acMatrix[0][3];
        byte c2 = acMatrix[1][3];
        byte c3 = acMatrix[2][3];
        TextView[] read = new TextView[3];
        TextView[] write = new TextView[3];
        for (int i = 0; i < 3; i++) {
            read[i] = new TextView(this);
            write[i] = new TextView(this);
        }

        if          (c1 == 0 && c2 == 0 && c3 == 0) {
            read[0].setText("Never");
            read[0].setTextColor(getResources().getColor(R.color.orange));
            write[0].setText("Key A");
            write[0].setTextColor(getResources().getColor(R.color.yellow));
            read[1].setText("Key A");
            read[1].setTextColor(getResources().getColor(R.color.yellow));
            write[1].setText("Never");
            write[1].setTextColor(getResources().getColor(R.color.orange));
            read[2].setText("Key A");
            read[2].setTextColor(getResources().getColor(R.color.yellow));
            write[2].setText("Key A");
            write[2].setTextColor(getResources().getColor(R.color.yellow));
        } else if   (c1 == 0 && c2 == 1 && c3 == 0) {
            read[0].setText("Never");
            read[0].setTextColor(getResources().getColor(R.color.orange));
            write[0].setText("Never");
            write[0].setTextColor(getResources().getColor(R.color.orange));
            read[1].setText("Key A");
            read[1].setTextColor(getResources().getColor(R.color.yellow));
            write[1].setText("Never");
            write[1].setTextColor(getResources().getColor(R.color.orange));
            read[2].setText("Key A");
            read[2].setTextColor(getResources().getColor(R.color.yellow));
            write[2].setText("Never");
            write[2].setTextColor(getResources().getColor(R.color.orange));
        } else if   (c1 == 1 && c2 == 0 && c3 == 0) {
            read[0].setText("Never");
            read[0].setTextColor(getResources().getColor(R.color.orange));
            write[0].setText("Key B");
            write[0].setTextColor(getResources().getColor(R.color.yellow));
            read[1].setText("Key A|B");
            read[1].setTextColor(getResources().getColor(R.color.light_green));
            write[1].setText("Never");
            write[1].setTextColor(getResources().getColor(R.color.orange));
            read[2].setText("Never");
            read[2].setTextColor(getResources().getColor(R.color.orange));
            write[2].setText("Key B");
            write[2].setTextColor(getResources().getColor(R.color.yellow));
        } else if   (c1 == 1 && c2 == 1 && c3 == 0) {
            read[0].setText("Never");
            read[0].setTextColor(getResources().getColor(R.color.orange));
            write[0].setText("Never");
            write[0].setTextColor(getResources().getColor(R.color.orange));
            read[1].setText("Key A|B");
            read[1].setTextColor(getResources().getColor(R.color.light_green));
            write[1].setText("Never");
            write[1].setTextColor(getResources().getColor(R.color.orange));
            read[2].setText("Never");
            read[2].setTextColor(getResources().getColor(R.color.orange));
            write[2].setText("Never");
            write[2].setTextColor(getResources().getColor(R.color.orange));
        } else if   (c1 == 0 && c2 == 0 && c3 == 1) {
            read[0].setText("Never");
            read[0].setTextColor(getResources().getColor(R.color.orange));
            write[0].setText("Key A");
            write[0].setTextColor(getResources().getColor(R.color.yellow));
            read[1].setText("Key A");
            read[1].setTextColor(getResources().getColor(R.color.yellow));
            write[1].setText("Key A");
            write[1].setTextColor(getResources().getColor(R.color.yellow));
            read[2].setText("Key A");
            read[2].setTextColor(getResources().getColor(R.color.yellow));
            write[2].setText("Key A");
            write[2].setTextColor(getResources().getColor(R.color.yellow));
        } else if   (c1 == 0 && c2 == 1 && c3 == 1) {
            read[0].setText("Never");
            read[0].setTextColor(getResources().getColor(R.color.orange));
            write[0].setText("Key B");
            write[0].setTextColor(getResources().getColor(R.color.yellow));
            read[1].setText("Key A|B");
            read[1].setTextColor(getResources().getColor(R.color.light_green));
            write[1].setText("Key B");
            write[1].setTextColor(getResources().getColor(R.color.yellow));
            read[2].setText("Never");
            read[2].setTextColor(getResources().getColor(R.color.orange));
            write[2].setText("Key B");
            write[2].setTextColor(getResources().getColor(R.color.yellow));
        } else if   (c1 == 1 && c2 == 0 && c3 == 1) {
            read[0].setText("Never");
            read[0].setTextColor(getResources().getColor(R.color.orange));
            write[0].setText("Never");
            write[0].setTextColor(getResources().getColor(R.color.orange));
            read[1].setText("Key A|B");
            read[1].setTextColor(getResources().getColor(R.color.light_green));
            write[1].setText("Key B");
            write[1].setTextColor(getResources().getColor(R.color.yellow));
            read[2].setText("Never");
            read[2].setTextColor(getResources().getColor(R.color.orange));
            write[2].setText("Never");
            write[2].setTextColor(getResources().getColor(R.color.orange));
        } else if   (c1 == 1 && c2 == 1 && c3 == 1) {
            read[0].setText("Never");
            read[0].setTextColor(getResources().getColor(R.color.orange));
            write[0].setText("Never");
            write[0].setTextColor(getResources().getColor(R.color.orange));
            read[1].setText("Key A|B");
            read[1].setTextColor(getResources().getColor(R.color.light_green));
            write[1].setText("Never");
            write[1].setTextColor(getResources().getColor(R.color.orange));
            read[2].setText("Never");
            read[2].setTextColor(getResources().getColor(R.color.orange));
            write[2].setText("Never");
            write[2].setTextColor(getResources().getColor(R.color.orange));
        } else {
            for (int i = 0; i < 3; i++) {
                read[i].setText(R.string.text_ac_error);
                read[i].setTextColor(getResources().getColor(R.color.red));
            }
        }

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
     * Check if key B is readable.
     * Key B is readable for the following configurations:
     * <ul>
     * <li>C1 = 0, C2 = 0, C3 = 0</li>
     * <li>C1 = 0, C2 = 0, C3 = 1</li>
     * <li>C1 = 0, C2 = 1, C3 = 0</li>
     * </ul>
     * @param c1 Access bit "C1"
     * @param c2 Access bit "C2"
     * @param c3 Access bit "C3"
     * @return True if key B is readable. False otherwise.
     */
    private boolean isKeyBReadable(byte c1, byte c2, byte c3) {
        if (c1 == 0
                && (c2 == 0 && c3 == 0)
                || (c2 == 1 && c3 == 0)
                || (c2 == 0 && c3 == 1)) {
            return true;
        }
        return false;
    }

}
