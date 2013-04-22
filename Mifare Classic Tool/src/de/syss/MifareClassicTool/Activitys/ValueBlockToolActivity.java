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

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
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
            // Error. Not hex and 16 byte.
            return;
        }
        if (Common.isValueBlock(data) == false) {
             // Error. No value block.
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

    /**
     * Encode a integer (and addr.) into a Mifare Classic Value Block.
     * @param view The View object that triggered the method
     * (in this case the encode button).
     */
    public void onEncode(View view) {
        String vbText = mVBasInt.getText().toString();
        String addrText = mAddr.getText().toString();
        if (vbText.equals("")){
            // Error. There is no integer to encode.
            Toast.makeText(this, R.string.info_no_int_to_encode,
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (addrText.matches("[0-9A-Fa-f]{2}") == false) {
            // Error. There is no valid value block addr.
            Toast.makeText(this, R.string.info_addr_not_hex_byte,
                    Toast.LENGTH_LONG).show();
            return;
        }
        // Encode.
        // String -> Int.
        int vbAsInt = Integer.parseInt(vbText);
        // Int -> reverse -> byte array -> hex string.
        String vb = Common.byte2HexString(ByteBuffer.allocate(4).putInt(
                Integer.reverseBytes(vbAsInt)).array());
        // Int -> invert -> reverse -> byte array -> hex string.
        String vbInverted = Common.byte2HexString(ByteBuffer.allocate(4).putInt(
                Integer.reverseBytes(~vbAsInt)).array());
        String addr = addrText;
        String addrInverted = Integer.toHexString(
                ~Integer.parseInt(addr, 16)).toUpperCase().substring(6, 8);
        mVB.setText(vb + vbInverted + vb
                + addr + addrInverted + addr + addrInverted);

    }

    /**
     * Copy the Mifare Classic Value Block to the Android clipboard.
     * @param view The View object that triggered the method
     * (in this case the copy button).
     */
    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public void onCopyToClipboard(View view) {
        if (mVB.getText().toString().equals("") == false) {
            if (Build.VERSION.SDK_INT >= 11) {
                // Android API level 11+.
                android.content.ClipboardManager clipboard =
                        (android.content.ClipboardManager) getSystemService(
                                Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip =
                        android.content.ClipData.newPlainText(
                                "mifare classic value block", mVB.getText());
                clipboard.setPrimaryClip(clip);
            } else {
                // Android API level 10.
                android.text.ClipboardManager clipboard =
                        (android.text.ClipboardManager) getSystemService(
                                Context.CLIPBOARD_SERVICE);
                clipboard.setText(mVB.getText().toString());
            }
            Toast.makeText(this, R.string.info_copied_to_clipboard,
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Paste the content of the Android clipboard (if plain text) to the
     * value block edit text of this layout.
     * @param view The View object that triggered the method
     * (in this case the paste button).
     */
    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public void onPasteFromClipboard(View view) {
        if (Build.VERSION.SDK_INT >= 11) {
            // Android API level 11+.
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) getSystemService(
                            Context.CLIPBOARD_SERVICE);
            if (clipboard.getPrimaryClip() != null
                    && clipboard.getPrimaryClip().getItemCount() > 0
                    && clipboard.getPrimaryClipDescription().hasMimeType(
                        android.content.ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                mVB.setText(clipboard.getPrimaryClip().getItemAt(0).getText());
            }
        } else {
            // Android API level 10.
            android.text.ClipboardManager clipboard =
                    (android.text.ClipboardManager) getSystemService(
                            Context.CLIPBOARD_SERVICE);
            if (clipboard.hasText()) {
                mVB.setText(clipboard.getText());
            }
        }
    }
}
