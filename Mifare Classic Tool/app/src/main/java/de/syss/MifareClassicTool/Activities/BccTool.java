/*
 * Copyright 2015 Gerhard Klostermeier
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
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.R;


/**
 * Calculate the BCC value of a given UID.
 * @author Gerhard Klostermeier
 */
public class BccTool extends BasicActivity {

    private EditText mUid;
    private EditText mBcc;

    /**
     * Initialize the some member variables.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bcc_tool);

        mUid = findViewById(R.id.editTextBccToolUid);
        mBcc = findViewById(R.id.editTextBccToolBcc);
    }

    /**
     * Calculate the BCC value of the given UID (part). This is done calling
     * {@link Common#calcBcc(byte[])} after some input checks (is the length
     * of the given UID valid; are there only hex symbols).
     * @param view The View object that triggered the method
     * (in this case the calculate BCC button).
     * @see Common#calcBcc(byte[])
     */
    public void onCalculate(View view) {
        String data = mUid.getText().toString();
        if (data.length() != 8) {
            // Error. UID (parts) are 4 bytes long.
            Toast.makeText(this, R.string.info_invalid_uid_length,
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (!data.matches("[0-9A-Fa-f]+")) {
            // Error, not hex.
            Toast.makeText(this, R.string.info_not_hex_data,
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Calculate the BCC.
        byte bcc = Common.calcBcc(Common.hex2Bytes(data));
        mBcc.setText(String.format("%02X", bcc));
    }

    /**
     * Copy the calculated BCC to the Android clipboard.
     * @param view The View object that triggered the method
     * (in this case the copy button).
     */
    public void onCopyToClipboard(View view) {
        Common.copyToClipboard(mBcc.getText().toString(), this, true);
    }

    /**
     * Paste the content of the Android clipboard (if plain text) to the
     * UID edit text.
     * @param view The View object that triggered the method
     * (in this case the paste button).
     */
    public void onPasteFromClipboard(View view) {
        String text = Common.getFromClipboard(this);
        if (text != null) {
            mUid.setText(text);
        }
    }
}
