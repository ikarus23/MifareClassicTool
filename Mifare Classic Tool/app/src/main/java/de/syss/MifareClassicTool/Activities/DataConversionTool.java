/*
 * Copyright 2020 Gerhard Klostermeier
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

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.R;

// TODO (optional): Add more conversion formats like short/int/long/etc.
/**
 * Convert data from formats like ASCII/hex/bin to each other.
 * @author Gerhard Klostermeier
 */
public class DataConversionTool extends BasicActivity {

    EditText mAscii;
    EditText mHex;
    EditText mBin;

    /**
     * Initialize the some member variables.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_conversion_tool);
        mAscii = findViewById(R.id.editTextDataConversionToolAscii);
        mHex = findViewById(R.id.editTextDataConversionToolHex);
        mBin = findViewById(R.id.editTextDataConversionToolBin);
    }

    /**
     * Convert the data from the source input (determined by the view /
     * the button) to a hex string and call {@link #convertData(String)}.
     * @param view The View object that triggered the method
     * (in this case any of the convert button).
     * @see #convertData(String)
     */
    public void onConvert(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.imageButtonDataConversionToolAscii:
                String ascii = mAscii.getText().toString();
                convertData(Common.ascii2Hex(ascii));
                break;
            case R.id.imageButtonDataConversionToolHex:
                String hex = mHex.getText().toString();
                if (Common.isHex(hex, this)) {
                    convertData(hex);
                }
                break;
            case R.id.imageButtonDataConversionToolBin:
                String bin = mBin.getText().toString();
                if (isBin(bin, this)) {
                    convertData(Common.bin2Hex(bin));
                }
                break;
        }
    }

    /**
     * Convert the data from a hex string to different output
     * formats and update the corresponding UI object.
     * @param hex The hex string to be converted into different formats.
     */
    private void convertData(String hex) {
        if (hex == null || hex.equals("")) {
            Toast.makeText(this, R.string.info_convert_error,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        // Hex.
        mHex.setText(hex.toUpperCase());
        // ASCII.
        String ascii = Common.hex2Ascii(hex);
        if (ascii != null) {
            mAscii.setText(ascii);
        } else {
            mAscii.setText(R.string.text_not_ascii);
        }
        // Bin.
        mBin.setText(Common.hex2Bin(hex));
    }

    /**
     * Check if a string represents binary bytes (0/1, multiple of 8).
     * @param bin The binary string to check.
     * @param context The Context in which an error Toast will be shown.
     * @return True if string is binary. False otherwise.
     */
    private boolean isBin(String bin, Context context) {
        if (bin != null && bin.length() % 8 == 0
                && bin.matches("[0-1]+")) {
            return true;
        }
        Toast.makeText(context, R.string.info_not_bin_data,
                Toast.LENGTH_LONG).show();
        return false;
    }
}
