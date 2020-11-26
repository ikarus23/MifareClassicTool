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
import android.text.TextUtils;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.R;

/**
 * Display a (hex) dump as 7-Bit US-ASCII.
 * This Activity will be shown from the {@link DumpEditor}, if the user
 * clicks the corresponding menu item.
 * @author user Gerhard Klostermeier
 */
public class HexToAscii extends BasicActivity {

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
            if (dump != null && dump.length != 0) {
                String s = System.getProperty("line.separator");
                CharSequence ascii = "";
                for (String line : dump) {
                    if (line.startsWith("+")) {
                        // Header.
                        String sectorNumber = line.split(": ")[1];
                        ascii = TextUtils.concat(ascii, Common.colorString(
                                getString(R.string.text_sector)
                                + ": " + sectorNumber,
                                ContextCompat.getColor(this, R.color.blue)), s);
                    } else {
                        // Data.
                        String converted = Common.hex2Ascii(line);
                        if (converted == null) {
                            converted = getString(R.string.text_invalid_data);
                        }
                        ascii = TextUtils.concat(ascii, " ", converted, s);
                    }
                }
                TextView tv = findViewById(R.id.textViewHexToAscii);
                tv.setText(ascii);
            }
            setIntent(null);
        }
    }
}
