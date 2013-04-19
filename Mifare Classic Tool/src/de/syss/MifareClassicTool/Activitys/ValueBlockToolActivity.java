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
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.R;

/**
 * Decode Mifare Classic Value Blocks from their hex format
 * to an integer and vice versa (encode).
 * @author Gerhard Klostermeier
 */
public class ValueBlockToolActivity extends BasicActivity {

    private EditText mVB;
    private EditText mVBasInt;
    private EditText mAddr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_value_block_tool);

        mVB = (EditText) findViewById(R.id.editTextValueBlockToolVB);
        mVBasInt = (EditText) findViewById(R.id.editTextValueBlockToolVBasInt);
        mAddr = (EditText) findViewById(R.id.editTextValueBlockAddr);
    }

    /**
     * Decode a Mifare Classic Value Block into an integer and the Addr value.
     * @param view The View object that triggered the method
     * (in this case the decode button).
     */
    public void onDecode(View view) {
        String data = mVB.getText().toString();
        if (Common.isHexAnd16Byte(data, this) == false) {
            return;
        }
        if (Common.isValueBlock(data) == false) {
             // No value block.
            Toast.makeText(this, R.string.info_is_not_vb,
                    Toast.LENGTH_LONG).show();
            return;
        }
        // Decode.
        byte[] vbAsBytes = Common.hexStringToByteArray(
                data.substring(0, 8));
        Common.reverseByteArrasInPlace(vbAsBytes);
        ByteBuffer bb = ByteBuffer.wrap(vbAsBytes);
        mVBasInt.setText("" + bb.getInt());
        mAddr.setText(data.substring(24, 26));
    }

    // TODO: Implement & doc.
    public void onEncode(View view) {

    }

    // TODO: Implement & doc.
    public void onCopyToClipboard(View view) {

    }

    // TODO: Implement & doc.
    public void onPasteFromClipboard(View view) {

    }
}
