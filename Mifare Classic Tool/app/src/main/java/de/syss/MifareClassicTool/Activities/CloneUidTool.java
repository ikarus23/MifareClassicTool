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
import android.nfc.tech.MifareClassic;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

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
    private int mUidLen;
    private EditText mEditTextBlock0Rest;
    private EditText mEditTextBlock0Key;
    private CheckBox mShowOptions;
    private CheckBox mCalcSakAtqa;
    private RadioButton mRadioButtonKeyB;
    private TextView mStatusLogContent;

    private String mBlock0Complete = "";
    // Taken from original MIFARE Classic tag with 4 byte UID.
    // In most cases it will not matter because bad
    // access control systems only check for the UID.
    private String mBlock0Rest = "080400475955D141103607";
    // Default key to write to a factory formatted block 0 of "magic tag gen2".
    private String mBlock0Key = MCReader.DEFAULT_KEY;
    private boolean mIgnoreIncorrectBlock0 = false;
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
        mShowOptions = findViewById(
                R.id.checkBoxCloneUidToolOptions);
        mRadioButtonKeyB = findViewById(
                R.id.radioButtonCloneUidToolKeyB);
        mCalcSakAtqa = findViewById(
                R.id.checkBoxCloneUidToolSakAtqa);

        mEditTextBlock0Rest.setText(mBlock0Rest);
        mEditTextBlock0Key.setText(mBlock0Key);

        // If a tag was scanned before, fill the UID.
        if (Common.getTag() != null) {
            mUid.setText(Common.bytes2Hex(Common.getUID()));
            appendToLog(getString(R.string.text_use_uid_of_scanned_tag)
                + " (" + Common.bytes2Hex(Common.getUID()) + ")");
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
            String uid = Common.bytes2Hex(Common.getUID());
            switch (mStatus) {
                case INIT:
                    // Original tag scanned.
                    mUid.setText(uid);
                    appendToLog(getString(
                            R.string.text_use_uid_of_scanned_tag)
                            + " (" + Common.bytes2Hex(Common.getUID())
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
                        appendToLog(getString(R.string.text_reset_clone_process));
                        mStatus = Status.INIT;
                        mIgnoreIncorrectBlock0 = false;
                    } else {
                        appendToLog(getString(R.string.text_uid_match_error)
                                + " (" + uidOriginal + " <-> " + uid + ")");
                        appendToLog(getString(R.string.text_clone_error));
                        mStatus = Status.BLOCK0_CALCULATED;
                    }
                    break;
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
        mUidLen = uid.length();
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
        if (mUidLen != 8 && mUidLen != 14 && mUidLen != 20) {
            // Error. No 4, 7 or 10 bytes UID.
            Toast.makeText(this, R.string.info_invalid_uid_length,
                    Toast.LENGTH_LONG).show();
            return;
        }
        mUidLen = mUidLen / 2; // Change from char length to byte length.

        // Check rest of block 0..
        int block0RestLen = (mUidLen == 4) ? 22 : 32 - mUidLen * 2;
        if (mBlock0Rest.length() < block0RestLen) {
            Toast.makeText(this,
                    R.string.info_rest_of_block_0_length,
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Calculate BCC for 4-byte UIDs and construct full block 0.
        if (mUidLen == 4) {
            byte bcc = Common.calcBcc(Common.hex2Bytes(uid));
            mBlock0Complete = uid + String.format("%02X", bcc) + mBlock0Rest;
        } else {
            mBlock0Complete = uid + mBlock0Rest.substring(
                    mBlock0Rest.length() - block0RestLen);
        }
        mStatus = Status.BLOCK0_CALCULATED;
        appendToLog(getString(R.string.text_block_0_generated));
        appendToLog(getString(R.string.text_waiting_for_magic_tag));
        // Hide options.
        mShowOptions.setChecked(false);
        onShowOptions(null);
    }

    /**
     * Check and write block 0 (manufacturer block) with {@link #mBlock0Complete}
     * containing the cloned UID.
     */
    private void writeManufacturerBlock() {

        boolean keyB = mRadioButtonKeyB.isChecked();
        byte[] key = Common.hex2Bytes(
                mEditTextBlock0Key.getText().toString());

        // Create reader.
        MCReader reader = Common.checkForTagAndCreateReader(this);
        if (reader == null) {
            return;
        }

        // Check for UID length mismatch between user input and detected card.
        int uidLen = Common.getUID().length;
        if (uidLen != mUidLen) {
            // Error. UID length does not match the source.
            appendToLog(getString(R.string.text_uid_length_error));
            reader.close();
            return;
        }

        // Automatically calculate the SAK and ATQA value?
        if (mCalcSakAtqa.isChecked()) {
            String sakAndAtqa = calcSakAtqa(uidLen, reader.getSize());
            if (sakAndAtqa == null) {
                // Error.
                appendToLog(getString(R.string.text_sak_atqa_calc_warning));
            } else {
                // Replace SAK & ATQA.
                int sakStart = (uidLen == 4) ? uidLen * 2 + 2 : uidLen * 2;
                mBlock0Complete = mBlock0Complete.substring(0, sakStart)
                        + sakAndAtqa
                        + mBlock0Complete.substring(sakStart + sakAndAtqa.length());
            }
        }
        appendToLog(getString(R.string.text_data_to_write)
                + " " + mBlock0Complete);

        // Check for block 0 issues (BCC, SAK, ATQA, UID0, ...).
        if (!mIgnoreIncorrectBlock0 && !Common.isValidBlock0(
                mBlock0Complete, uidLen, reader.getSize(), false)) {
            appendToLog(getString(R.string.text_block0_warning));
            showBlock0Warning();
            reader.close();
            return;
        }

        // Write to block 0.
        appendToLog(getString(R.string.text_writing_block_0));
        int result = reader.writeBlock(0, 0,
                Common.hex2Bytes(mBlock0Complete), key, keyB);

        // Error handling.
        switch (result) {
            case 4:
                appendToLog(getString(R.string.info_incorrect_key));
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
     * Calculate a SAK and ATQA value according to NXP's specifications.
     * https://www.nxp.com/docs/en/application-note/AN10833.pdf
     * @param uidLen Length of the UID.
     * @param tagSize Size of the tag.
     * @return Hex string representing 1 byte SAK and 2 byte ATQA. Null on error.
     */
    private String calcSakAtqa(int uidLen, int tagSize) {
        if ((uidLen != 4 && uidLen != 7) || (
                tagSize != MifareClassic.SIZE_MINI &&
                tagSize != MifareClassic.SIZE_1K &&
                tagSize != MifareClassic.SIZE_2K &&
                tagSize != MifareClassic.SIZE_4K)) {
            return null;
        }

        // ATQA.
        String atqa = null;
        if (uidLen == 4 && (tagSize == MifareClassic.SIZE_1K ||
                tagSize == MifareClassic.SIZE_2K ||
                tagSize == MifareClassic.SIZE_MINI)) {
            atqa = "0400";
        } else if (uidLen == 4 && tagSize == MifareClassic.SIZE_4K) {
            atqa = "0200";
        } else if (uidLen == 7 && (tagSize == MifareClassic.SIZE_1K ||
                tagSize == MifareClassic.SIZE_2K ||
                tagSize == MifareClassic.SIZE_MINI)) {
            atqa = "4400";
        } else if (uidLen == 7 && tagSize == MifareClassic.SIZE_4K) {
            atqa = "4200";
        }

        // SAK.
        // NOTE: Tags might not appear to be genuine/valid NXP/Infinion tags.
        // For a 4b UID/1k MFC tag the SAK should be 0x08 but the "corresponding" byte in
        // block 0 (byte nr. 5) must by 0x88. This is not possible with some gen2 tags,
        // because they use byte nr. 5 as SAK value.
        String sak = null;
        if (tagSize == MifareClassic.SIZE_MINI) {
            sak = "09";
        } else if (tagSize == MifareClassic.SIZE_1K || tagSize == MifareClassic.SIZE_2K) {
            sak = "08";
        } else if (tagSize == MifareClassic.SIZE_4K) {
            sak = "18";
        }

        if (sak == null || atqa == null) {
            return null;
        }
        return sak + atqa;
    }

    /**
     * Show a warning dialog to inform the user, he is about to write what may be
     * invalid data to block 0. On ignore, continue with writing. On cancel, reset
     * the clone process.
     * @see #writeManufacturerBlock()
     * @see #mIgnoreIncorrectBlock0
     * @see #mStatus
     */
    private void showBlock0Warning() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_block0_data_warning_title)
                .setMessage(R.string.dialog_block0_data_warning)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.action_i_know_what_i_am_doing,
                        (dialog, which) -> {
                            // Write.
                            mIgnoreIncorrectBlock0 = true;
                            writeManufacturerBlock();
                        })
                .setNegativeButton(R.string.action_cancel,
                        (dialog, id) -> dialog.cancel())
                .setOnCancelListener(
                        dialog -> {
                            // Cancel.
                            mStatus = Status.INIT;
                            appendToLog(getString(R.string.text_reset_clone_process));
                        })
                .show();
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
        LinearLayout optionsLayout = findViewById(
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
     * Show information about SAK and ATQA being part of block 0
     * (or at least might be).
     * @param view The View object that triggered the method
     * (in this case the "show info" button).
     */
    public void onShowSakAtqaInfo(View view) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_auto_calc_sak_atqa_title)
                .setMessage(R.string.dialog_auto_calc_sak_atqa)
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

    /**
     * Generate a random 4 byte UID to be written to the tag.
     * UID 00000000 will be ignored because it my cause errors
     * on some readers.
     * @param view The View object that triggered the method
     * (in this case the random UID button).
     */
    public void onRandomUid(View view) {
        String uid = "00000000";
        byte[] bytesUid = new byte[4];
        while (uid.equals("00000000")) {
            new Random().nextBytes(bytesUid);
            uid = Common.bytes2Hex(bytesUid);
        }
        mUid.setText(uid);
        Toast.makeText(this, R.string.info_random_uid,
                Toast.LENGTH_SHORT).show();
    }
}
