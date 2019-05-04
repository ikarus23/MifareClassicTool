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

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.MCReader;
import de.syss.MifareClassicTool.R;


/**
 * Clone UID to "magic tag gen2". The gen2 magic tags allow direct write to
 * block 0 without the need for special "backdoor" commands.
 * @author Slawomir Jasek slawomir.jasek@smartlockpicking.com and Gerhard Klostermeier
 */
public class CloneUidTool extends BasicActivity {

    private EditText mUid;
    private EditText mEditTextBlock0Rest;
    private EditText mEditTextBlock0Key;
    private CheckBox mShowOptions;
    private RadioButton mRadioButtonKeyB;
    private TextView mStatusLogContent;

    private String mBlock0Complete = "";
    // Taken from sample card, in most cases it will not matter because bad
    // access control systems only check for the UID.
    private String mBlock0Rest = "08040001A2EC736FC3351D";
    // Default key to write to a factory formatted block 0 of "magic tag gen2".
    private String mBlock0Key = "FFFFFFFFFFFF";
    private enum Status { INIT, BLOCK0_CALCULATED, CLONED }
    private Status mStatus = Status.INIT;

    /**
     * Initialize some member variables.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clone_uid_tool);

        mUid = findViewById(R.id.editTextCloneUidToolOriginalUid);
        mEditTextBlock0Rest = findViewById(
                R.id.editTextCloneUidToolBlock0Rest);
        mEditTextBlock0Key = findViewById(
                R.id.editTextCloneUidToolWriteKey);
        mStatusLogContent = findViewById(
                R.id.textViewCloneUidToolStatusLogContent);
        mShowOptions = (CheckBox) findViewById(
                R.id.checkBoxCloneUidToolOptions);
        mRadioButtonKeyB = findViewById(
                R.id.radioButtonCloneUidToolKeyB);

        mEditTextBlock0Rest.setText(mBlock0Rest);
        mEditTextBlock0Key.setText(mBlock0Key);

        // If a tag was scanned before, fill the UID.
        if (Common.getTag() != null) {
            mUid.setText(Common.byte2HexString(Common.getUID()));
            appendToLog(getString(R.string.text_use_uid_of_scanned_tag)
                + " (" + Common.byte2HexString(Common.getUID()) + ")");
        }
    }

    /**
     * Handle new Intent as a new tag Intent and tread them according to
     * the {@link #mStatus}.
     */
    @Override
    public void onNewIntent(Intent intent) {
        int typeCheck = Common.treatAsNewTag(intent, this);
        if (typeCheck == -1 || typeCheck == -2) {
            // Device or tag does not support MIFARE Classic.
            // Run the only thing that is possible: The tag info tool.
            Intent i = new Intent(this, TagInfoTool.class);
            startActivity(i);
        }
        // Was the new intent a new tag?
        if (typeCheck != -4) {
            String uid = Common.byte2HexString(Common.getUID());
            switch (mStatus) {
                case INIT:
                    // Original tag scanned.
                    mUid.setText(uid);
                    appendToLog(getString(
                            R.string.text_use_uid_of_scanned_tag)
                            + " (" + Common.byte2HexString(Common.getUID())
                            + ")");
                    break;
                case BLOCK0_CALCULATED:
                    // UID is present, block 0 calculated.
                    // Write to magic tag gen2.
                    writeManufacturerBlock();
                    break;
                case CLONED:
                    // Confirm the UID is cloned.
                    // Read it again from current tag.
                    appendToLog(getString(R.string.text_checking_clone));
                    String uidOriginal = mUid.getText().toString();
                    if (uid.equals(uidOriginal)) {
                        appendToLog(getString(R.string.text_clone_successfully));
                        mStatus = Status.INIT;
                    } else {
                        appendToLog(getString(R.string.text_uid_match_error)
                                + " (" + uidOriginal + " <-> " + uid + ")");
                        appendToLog(getString(R.string.text_clone_error));
                        mStatus = Status.BLOCK0_CALCULATED;
                    }
            }
        }
    }

    /**
     * Calculate the BCC of the UID and create block 0
     * (manufacturer block) by concatenating UID, BCC and the
     * rest of block 0 ({@link #mBlock0Rest}).
     * @param view The View object that triggered the method
     * (in this case the "calculate block 0 and clone uid" button).
     */
    public void onCalculateBlock0(View view) {
        String uid = mUid.getText().toString();
        mBlock0Rest = mEditTextBlock0Rest
                .getText().toString();
        mBlock0Key = mEditTextBlock0Key
                .getText().toString();

        // Check if all data is HEX.
        if (!mBlock0Rest.matches("[0-9A-Fa-f]+")
                || !uid.matches("[0-9A-Fa-f]+")
                || !mBlock0Key.matches("[0-9A-Fa-f]+")) {
            // Error, not hex.
            Toast.makeText(this, R.string.info_not_hex_data,
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Check key field.
        if (mBlock0Key.length() != 12) {
            Toast.makeText(this, R.string.info_valid_keys_not_6_byte,
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Check the UID length.
        if (uid.length() != 8) {
            // Error: no 4 bytes UID. 7 and 10 bytes UID not supported (yet).
            Toast.makeText(this, R.string.info_invalid_uid_length,
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Check rest of block 0..
        if (mBlock0Rest.length() != 22) {
            Toast.makeText(this,
                    R.string.info_rest_of_block_0_length,
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Calculate BCC and cuonstruct full block 0.
        byte bcc = Common.calcBCC(Common.hexStringToByteArray(uid));
        mBlock0Complete = uid + String.format("%02X", bcc) + mBlock0Rest;
        mStatus = Status.BLOCK0_CALCULATED;
        appendToLog(getString(R.string.text_block_0_calculated)
                + " (" + mBlock0Complete + ")");
        appendToLog(getString(R.string.text_waiting_for_magic_tag));
        // Hide options.
        mShowOptions.setChecked(false);
        onShowOptions(null);
    }

    /**
     * Write block 0 (manufacturer block) with {@link #mBlock0Complete}
     * containing the cloned UID.
     */
    private void writeManufacturerBlock() {

        boolean keyB = mRadioButtonKeyB.isChecked();
        byte[] key = Common.hexStringToByteArray(
                mEditTextBlock0Key.getText().toString());

        // Create reader.
        MCReader reader = Common.checkForTagAndCreateReader(this);
        if (reader == null) {
            return;
        }

        // Check UID length of magic card gen2.
        byte[] uid = Common.getUID();
        if (uid.length != 4) {
            // Error: no 4 bytes UID. 7 and 10 bytes UID not supported (yet).
            Toast.makeText(this, R.string.info_invalid_uid_length,
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Write to block 0.
        appendToLog(getString(R.string.text_writing_block_0));
        int result = reader.writeBlock(0, 0,
                Common.hexStringToByteArray(mBlock0Complete), key, keyB);

        // Error handling.
        switch (result) {
            case 4:
                Toast.makeText(this, R.string.info_incorrect_key,
                        Toast.LENGTH_LONG).show();
                return;
            case -1:
                Toast.makeText(this, R.string.info_write_error,
                        Toast.LENGTH_LONG).show();
                appendToLog(getString(R.string.text_clone_error));
                return;
        }

        appendToLog(getString(R.string.text_no_errors_on_write));
        appendToLog(getString(R.string.text_rescan_tag_to_check));
        mStatus = Status.CLONED;
        reader.close();
    }

    /**
     * Append a text to the status log.
     * @param text The text to append to the status log.
     */
    private void appendToLog(String text) {
        CharSequence content = mStatusLogContent.getText();
        String newline = "";
        if (!content.equals("")) {
            newline = "\n";
        }
        content = content + newline + "\u2022 " + text;
        mStatusLogContent.setText(content);
    }

    /**
     * Show / hide options.
     * @param view The View object that triggered the method
     * (in this case the "show options" check box).
     */
    public void onShowOptions(View view) {
        LinearLayout optionsLayout = (LinearLayout) findViewById(
                R.id.linearLayoutOptions);
        if (mShowOptions.isChecked()) {
            optionsLayout.setVisibility(View.VISIBLE);
        } else {
            optionsLayout.setVisibility(View.GONE);
        }
    }

    /**
     * Show general information about this tool and cloning
     * UIDs of MIFARE Classic tags.
     * @param view The View object that triggered the method
     * (in this case the "show info" button).
     */
    public void onShowInfo(View view) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_gen2_tags_info_title)
                .setMessage(R.string.dialog_gen2_tags_info)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton(R.string.action_ok,
                        (dialog, which) -> {
                            // Do nothing.
                        }).show();
    }

    /**
     * Show information about how the rest of the block 0 is
     * used during the UID cloning process.
     * @param view The View object that triggered the method
     * (in this case the "show info" button).
     */
    public void onShowBlock0RestInfo(View view) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_rest_of_block_0_title)
                .setMessage(R.string.dialog_rest_of_block_0)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton(R.string.action_ok,
                        (dialog, which) -> {
                            // Do nothing.
                        }).show();
    }

    /**
     * Show information about the key which is needed
     * to write to block 0.
     * @param view The View object that triggered the method
     * (in this case the "show info" button).
     */
    public void onShowWriteKeyInfo(View view) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_key_for_block_0_title)
                .setMessage(R.string.dialog_key_for_block_0)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton(R.string.action_ok,
                        (dialog, which) -> {
                            // Do nothing.
                        }).show();
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
}
