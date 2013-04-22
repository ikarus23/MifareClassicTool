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

import java.nio.ByteBuffer;

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
 * Display value blocks in a way a user can read easily (as integer).
 * NXP has PDFs describing what value blocks are.
 * Google something like "nxp mifare classic value blocks",
 * if you want to have a closer look.
 * This Activity will be shown from the {@link DumpEditorActivity}, if the user
 * clicks the corresponding menu item.
 * @author Gerhard Klostermeier
 */
public class ValueBlocksToIntActivity extends BasicActivity {

    // LOW: Pass a better object then a stringblobb separated by new line.
    // (See http://stackoverflow.com/a/2141166)
    public final static String EXTRA_VB =
            "de.syss.MifareClassicTool.Activity.VB";

    private static final String LOG_TAG =
            ValueBlocksToIntActivity.class.getSimpleName();

    private TableLayout mLayout;

    /**
     * Get value blocks from Intent and initialize Activity to
     * displaying them. If there is no Intent with
     * {@link #EXTRA_VB}, the Activity will be exited.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_value_blocks_to_int);

        boolean noValueBlocks = false;
        if (getIntent().hasExtra(EXTRA_VB)) {
            mLayout = (TableLayout) findViewById(
                    R.id.TableLayoutValueBlocksToInt);
            String extra = getIntent().getStringExtra(EXTRA_VB);
            if (!extra.equals("")) {
                String[] valueBlocks = extra.split(
                        System.getProperty("line.separator"));
                for (int i = 0; i < valueBlocks.length; i=i+2) {
                    String[] sectorAndBlock = valueBlocks[i].split(", ");
                    String sectorNumber = sectorAndBlock[0].split(": ")[1];
                    String blockNumber = sectorAndBlock[1].split(": ")[1];
                    addPosInfoRow(getString(R.string.text_sector)
                            + ": " + sectorNumber + ", "
                            + getString(R.string.text_block)
                            + ": " + blockNumber);
                    addValueBlock(valueBlocks[i+1]);
                }
            } else {
                noValueBlocks = true;
            }
        } else {
            noValueBlocks = true;
        }

        if (noValueBlocks) {
            Log.d(LOG_TAG, "There was no value block in intent.");
            finish();
        }
    }

    /**
     * Add a row with position information to the layout table.
     * This row shows the user where the value block is located (sector, block).
     * @param value The position information (e.g. "Sector: 1, Block: 2").
     */
    private void addPosInfoRow(String value) {
        TextView header = new TextView(this);
        header.setText(Common.colorString(value,
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
    }

    /**
     * Add full value block information (original
     * and integer format) to the layout table (two rows).
     * @param hexValueBlock The value block as hex string (32 chars.).
     */
    private void addValueBlock(String hexValueBlock) {
        TableRow tr = new TableRow(this);
        TextView what = new TextView(this);
        TextView value = new TextView(this);

        // Original.
        tr.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));
        what.setText(R.string.text_vb_orig);
        value.setText(Common.colorString(hexValueBlock.substring(0, 8),
                getResources().getColor(R.color.yellow)));
        tr.addView(what);
        tr.addView(value);
        mLayout.addView(tr, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));

        // Resolved to int.
        tr = new TableRow(this);
        tr.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));
        what = new TextView(this);
        what.setText(R.string.text_vb_as_int_decoded);
        value = new TextView(this);
        byte[] asBytes = Common.hexStringToByteArray(
                hexValueBlock.substring(0, 8));
        Common.reverseByteArrasInPlace(asBytes);
        ByteBuffer bb = ByteBuffer.wrap(asBytes);
        int i = bb.getInt();
        String asInt = "" + i;
        value.setText(Common.colorString(asInt,
                getResources().getColor(R.color.light_green)));
        tr.addView(what);
        tr.addView(value);
        mLayout.addView(tr, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));
    }
}
