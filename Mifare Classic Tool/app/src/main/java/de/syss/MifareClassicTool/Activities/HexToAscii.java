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

import java.io.UnsupportedEncodingException;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.R;

/**
 * Display a (hex) dump as 7-Bit US-ASCII.
 * This Activity will be shown from the {@link DumpEditor}, if the user
 * clicks the corresponding menu item.
 * @author user Gerhard Klostermeier
 */
public class HexToAscii extends BasicActivity {

    private static final String LOG_TAG =
            HexToAscii.class.getSimpleName();

    /**
     * Initialize the activity with the data from the Intent
     * ({@link DumpEditor#EXTRA_DUMP}) by displaying them as
     * US-ASCII. Non printable ASCII characters will be displayed as ".".
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hex_to_ascii);

        if (getIntent().hasExtra(DumpEditor.EXTRA_DUMP)) {
            String[] dump = getIntent().getStringArrayExtra(
                    DumpEditor.EXTRA_DUMP);
            if (dump.length != 0) {
                String s = System.getProperty("line.separator");
                CharSequence ascii = "";
                for (String line : dump) {
                    if (line.startsWith("+")) {
                        // Header.
                        String sectorNumber = line.split(": ")[1];
                        ascii = TextUtils.concat(ascii, Common.colorString(
                                getString(R.string.text_sector)
                                + ": " + sectorNumber,
                                getResources().getColor(R.color.blue)), s);
                    } else {
                        // Data.
                        // Replace non printable ASCII with ".".
                        byte[] hex = Common.hexStringToByteArray(line);
                        for(int i = 0; i < hex.length; i++) {
                            if (hex[i] < (byte)0x20 || hex[i] == (byte)0x7F) {
                                hex[i] = (byte)0x2E;
                            }
                        }
                        // Hex to ASCII.
                        try {
                            ascii = TextUtils.concat(ascii, " ",
                                    new String(hex, "US-ASCII"), s);
                        } catch (UnsupportedEncodingException e) {
                            Log.e(LOG_TAG, "Error while encoding to ASCII", e);
                        }
                    }
                }
                TextView tv = (TextView) findViewById(R.id.textViewHexToAscii);
                tv.setText(ascii);
            }
            setIntent(null);
        }
    }
}
