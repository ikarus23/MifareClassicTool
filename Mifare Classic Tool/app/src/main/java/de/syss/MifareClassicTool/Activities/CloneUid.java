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

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.nfc.Tag;

import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.MCReader;
import de.syss.MifareClassicTool.R;


/**
 * Clone UID to "magic UID gen2" cards - direct write to block 0 without the need for special "backdoor" commands
 * @author Slawomir Jasek slawomir.jasek@smartlockpicking.com
 */
public class CloneUid extends BasicActivity {

    private EditText mUid;
    private EditText mManufacturerBlockRest;
    private EditText mManufacturerBlockKey;
    private TextView mManufacturerBlockFinal;
    private TextView mTextManufacturerBlockFinal;
    private TextView mTextWaitingForMagicTag;
    private TextView mTextWritingUid;
    private TextView mTextCheckingClone;

    private String manufacturerBlockFinal="";
    //taken from sample card, in most cases it will not matter (readers check only the UID)
    private String manufacturerBlockRest="08040001A2EC736FC3351D";
    //default key to write the block 0 to "magic UID" card
    private String manufacturerBlockKey="FFFFFFFFFFFF";
    private enum Status { INIT, UID_ENTERED, CLONED, CONFIRMED };
    Status status = Status.INIT;

    /**
     * Initialize some member variables.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clone_uid);

        mUid = findViewById(R.id.editTextCloneUidOriginalUid);
        mManufacturerBlockRest = findViewById(R.id.editTextCloneUidManufacturerBlockRest);
        mManufacturerBlockKey = findViewById(R.id.editTextCloneUidManufacturerBlockKey);
        mManufacturerBlockFinal = findViewById(R.id.textViewCloneUidManufacturerBlockFinalContent);
        mTextWaitingForMagicTag = findViewById(R.id.textViewCloneUidWaitingForMagicTag);
        mTextManufacturerBlockFinal = findViewById(R.id.textViewCloneUidManufacturerBlockFinal);
        mTextWritingUid = findViewById(R.id.textViewCloneUidWritingMagicTag);
        mTextCheckingClone = findViewById(R.id.textViewCloneUidCheckingClone);

        mManufacturerBlockRest.setText(manufacturerBlockRest);
        mManufacturerBlockKey.setText(manufacturerBlockKey);

        //if a tag was scanned before, fill the UID
        Tag tag = Common.getTag();
        if (tag != null) {
            int uidLen = tag.getId().length;
            if (uidLen == 4) {
                String uid = Common.byte2HexString(tag.getId());
                mUid.setText(uid);
            }
        }
    }


    /**
     * Handle new Intent as a new tag Intent
     */
    @Override
    public void onNewIntent(Intent intent) {
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {

            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            tag = MCReader.patchTag(tag);
            Common.setTag(tag);

            switch (status) {
                //scan the original tag
                case INIT:
                    mUid.setText(Common.byte2HexString(tag.getId()));
                    // Show Toast message with UID.
                    String id = this.getResources().getString(
                            R.string.info_new_tag_found) + " (UID: ";
                    id += Common.byte2HexString(tag.getId());
                    id += ")";
                    Toast.makeText(this, id, Toast.LENGTH_LONG).show();
                    break;

                //Magic UID tag to write
                case UID_ENTERED:
                    MCReader reader = Common.checkForTagAndCreateReader(this);
                    if (reader == null) {
                        return;
                    }
                    mTextWritingUid.setVisibility(View.VISIBLE);
                    mTextWritingUid.setText(R.string.text_clone_uid_writing);
                    writeManufacturerBlock(reader);
                    break;

                //confirm the UID is cloned - read it again from current tag
                case CLONED:
                    String uid = Common.byte2HexString(tag.getId());
                    String uidOriginal = mUid.getText().toString();

                    mTextCheckingClone.setVisibility(View.VISIBLE);
                    mTextCheckingClone.setText(getString(R.string.text_clone_uid_checking_clone));

                    if (uid.equals(uidOriginal)) {
                        mTextCheckingClone.setText(getString(R.string.text_clone_uid_checking_clone) + " " + getString(R.string.text_clone_uid_checking_clone_success));
                        status = Status.CONFIRMED;
                    }
                    else {
                        mTextCheckingClone.setText(getString(R.string.text_clone_uid_checking_clone) + " Expecting " + uidOriginal + " got " + uid + "... "+ getString(R.string.text_clone_uid_checking_clone_error));
                    }
            }
        }
    }

    /**
     * Calculate the BCC and rest of manufacturer block (0)
     */
    public void onCalculateManufacturerBlock(View view) {

        //hide advanced options
        final LinearLayout advancedLayout = (LinearLayout) findViewById(R.id.linearLayoutAdvancedOptions);
        advancedLayout.setVisibility(View.GONE);
        //hide cloning progress output
        mTextManufacturerBlockFinal.setVisibility(View.INVISIBLE);
        mManufacturerBlockFinal.setVisibility(View.INVISIBLE);
        mTextWaitingForMagicTag.setVisibility(View.INVISIBLE);
        mTextWritingUid.setVisibility(View.INVISIBLE);
        mTextCheckingClone.setVisibility(View.INVISIBLE);

        //check the UID field
        //taken from de.syss.MifareClassicTool.BccTool
        String uid = mUid.getText().toString();
        if (uid.length() != 8) {
            // Error. UID (parts) are 4 bytes long.
            Toast.makeText(this, R.string.info_invalid_uid_length,
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (!uid.matches("[0-9A-Fa-f]+")) {
            // Error, not hex.
            Toast.makeText(this, R.string.info_not_hex_data,
                    Toast.LENGTH_LONG).show();
            return;
        }

        //check manufacturer block field
        manufacturerBlockRest = mManufacturerBlockRest.getText().toString();
        if (manufacturerBlockRest.length() != 22) {
            Toast.makeText(this, R.string.info_not_11_byte,
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (!manufacturerBlockRest.matches("[0-9A-Fa-f]+")) {
            // Error, not hex.
            Toast.makeText(this, R.string.info_not_hex_data,
                    Toast.LENGTH_LONG).show();
            return;
        }

        //check key field
        manufacturerBlockKey = mManufacturerBlockKey.getText().toString();
        if (!manufacturerBlockKey.matches("[0-9A-Fa-f]+")) {
            // Error, not hex.
            Toast.makeText(this, R.string.info_not_hex_data,
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (manufacturerBlockKey.length() != 12) {
            Toast.makeText(this, R.string.info_valid_keys_not_6_byte,
                    Toast.LENGTH_LONG).show();
            return;
        }


        // Calculate the BCC and full block 0
        byte bcc = Common.calcBCC(Common.hexStringToByteArray(uid));
        manufacturerBlockFinal = mUid.getText().toString() + String.format("%02X", bcc) + manufacturerBlockRest;

        mTextManufacturerBlockFinal.setVisibility(View.VISIBLE);
        mManufacturerBlockFinal.setVisibility(View.VISIBLE);
        mManufacturerBlockFinal.setText(manufacturerBlockFinal);
        status = Status.UID_ENTERED;
        mTextWaitingForMagicTag.setVisibility(TextView.VISIBLE);

    }

    /**
     * Write the manufacturer block 0 to tag.
     * @param reader The MCReader object with open session to "magic UID" tag
     */
    private int writeManufacturerBlock(MCReader reader) {

        int sector = 0;
        int block = 0;
        byte[] key = Common.hexStringToByteArray(mManufacturerBlockKey.getText().toString());
        int result = -1;

        result = reader.writeBlock(sector, block,
                    Common.hexStringToByteArray(manufacturerBlockFinal), key, false);
        reader.close();

        // Error handling.
        switch (result) {
            case 2:
                Toast.makeText(this, R.string.info_block_not_in_sector,
                        Toast.LENGTH_LONG).show();
                return -1;
            case 4:
                Toast.makeText(this, R.string.text_clone_uid_invalid_key,
                        Toast.LENGTH_LONG).show();
                return -1;
            case -1:
                Toast.makeText(this, R.string.info_error_writing_block,
                        Toast.LENGTH_LONG).show();
                return -1;
        }
        //note: the same output will be for standard, non-magic cards that do not allow to write block 0
        Toast.makeText(this, R.string.info_write_successful,
                Toast.LENGTH_LONG).show();

        status = Status.CLONED;

        return 0;
    }

    /**
     * Show / hide advanced options
     * @param view The View object that triggered the method
     * (in this case the "advanced options" button)
     */
    public void onShowAdvancedOptions(View view) {
        final LinearLayout advancedLayout = (LinearLayout) findViewById(R.id.linearLayoutAdvancedOptions);
        if (advancedLayout.getVisibility() == View.VISIBLE)
        {
            advancedLayout.setVisibility(View.GONE);
        }
        else
        {
            advancedLayout.setVisibility(View.VISIBLE);
        }

    }


    /**
     * Paste the content of the Android clipboard (if plain text) to the
     * UID edit text.
     * @param view The View object that triggered the method
     * (in this case the paste button).
     */
    public void onPasteUidFromClipboard(View view) {
        String text = Common.getFromClipboard(this);
        if (text != null) {
            mUid.setText(text);
        }
    }

    /**
     * Paste the content of the Android clipboard (if plain text) to the
     * Manufacturer block (advanced options) edit text.
     * @param view The View object that triggered the method
     * (in this case the paste button).
     */
    public void onPasteManufacturerBlockFromClipboard(View view) {
        String text = Common.getFromClipboard(this);
        if (text != null) {
            mManufacturerBlockRest.setText(text);
        }
    }

    /**
     * Paste the content of the Android clipboard (if plain text) to the
     * Manufacturer block key edit text.
     * @param view The View object that triggered the method
     * (in this case the paste button).
     */
    public void onPasteKeyFromClipboard(View view) {
        String text = Common.getFromClipboard(this);
        if (text != null) {
            mManufacturerBlockKey.setText(text);
        }
    }


}
