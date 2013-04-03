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

import java.io.UnsupportedEncodingException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.R;

/**
 * Display a (hex) dump as 7-Bit US-ASCII.
 * This Activity will be shown from the {@link DumpEditorActivity}, if the user
 * clicks the corresponding menu item.
 * @author user Gerhard Klostermeier
 */
public class HexToAsciiActivity extends Activity {

    // LOW: Pass a better object then a stringblobb separated by new line.
    // (See http://stackoverflow.com/a/2141166)

    private static final String LOG_TAG =
            HexToAsciiActivity.class.getSimpleName();

    /**
     * Initialize the activity with the data from the Intent
     * ({@link DumpEditorActivity#EXTRA_DUMP}) by displaying them as
     * US-ASCII. Non printable ASCII characters will be displayed as ".".
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hex_to_ascii);

        if (getIntent().hasExtra(DumpEditorActivity.EXTRA_DUMP)) {
            String dump = getIntent().getStringExtra(
                    DumpEditorActivity.EXTRA_DUMP);
            if (!dump.equals("")) {
                String s = System.getProperty("line.separator");
                String[] data = dump.split(s);
                CharSequence ascii = "";
                for (String line : data) {
                    if (line.startsWith("+")) {
                        // Header.
                        ascii = TextUtils.concat(ascii, Common.colorString(
                                line.substring(1),
                                getResources().getColor(R.color.blue)), s);
                    } else {
                        // Data.
                        // Replace non printable ACSII with ".".
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
}
