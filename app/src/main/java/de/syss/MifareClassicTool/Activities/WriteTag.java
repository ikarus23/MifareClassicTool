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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.nfc.tech.MifareClassic;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.widget.TextViewCompat;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.MCReader;
import de.syss.MifareClassicTool.R;

/**
 * Write data to tag. The user can choose to write
 * a single block of data or to write a dump to a tag providing its keys
 * or to factory format a tag.
 * @author Gerhard Klostermeier
 */
public class WriteTag extends BasicActivity {

    /**
     * The corresponding Intent will contain a dump. Headers
     * (e.g. "Sector: 1") are marked with a "+"-symbol (e.g. "+Sector: 1").
     */
    public final static String EXTRA_DUMP =
            "de.syss.MifareClassicTool.Activity.DUMP";

    private static final int FC_WRITE_DUMP = 1;
    private static final int CKM_WRITE_DUMP = 2;
    private static final int CKM_WRITE_BLOCK = 3;
    private static final int CKM_FACTORY_FORMAT = 4;
    private static final int CKM_WRITE_NEW_VALUE = 5;

    private EditText mSectorTextBlock;
    private EditText mBlockTextBlock;
    private EditText mDataText;
    private EditText mSectorTextVB;
    private EditText mBlockTextVB;
    private EditText mNewValueTextVB;
    private RadioButton mIncreaseVB;
    private EditText mStaticAC;
    private ArrayList<View> mWriteModeLayouts;
    private CheckBox mWriteManufBlock;
    private CheckBox mEnableStaticAC;
    private HashMap<Integer, HashMap<Integer, byte[]>> mDumpWithPos;
    private boolean mWriteDumpFromEditor = false;
    private String[] mDumpFromEditor;


    /**
     * Initialize the layout and some member variables. If the Intent
     * contains {@link #EXTRA_DUMP} (and therefore was send from
     * {@link DumpEditor}), the write dump option will be adjusted
     * accordingly.
     */
    // It is checked but the IDE don't get it.
    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_tag);

        mSectorTextBlock = findViewById(R.id.editTextWriteTagSector);
        mBlockTextBlock = findViewById(R.id.editTextWriteTagBlock);
        mDataText = findViewById(R.id.editTextWriteTagData);
        mSectorTextVB = findViewById(
                R.id.editTextWriteTagValueBlockSector);
        mBlockTextVB = findViewById(
                R.id.editTextWriteTagValueBlockBlock);
        mNewValueTextVB = findViewById(
                R.id.editTextWriteTagValueBlockValue);
        mIncreaseVB = findViewById(
                R.id.radioButtonWriteTagWriteValueBlockIncr);
        mStaticAC = findViewById(R.id.editTextWriteTagDumpStaticAC);
        mEnableStaticAC = findViewById(
                R.id.checkBoxWriteTagDumpStaticAC);
        mWriteManufBlock = findViewById(
                R.id.checkBoxWriteTagDumpWriteManuf);

        mWriteModeLayouts = new ArrayList<>();
        mWriteModeLayouts.add(findViewById(
                R.id.relativeLayoutWriteTagWriteBlock));
        mWriteModeLayouts.add(findViewById(R.id.linearLayoutWriteTagDump));
        mWriteModeLayouts.add(findViewById(
                R.id.linearLayoutWriteTagCloneUid));
        mWriteModeLayouts.add(findViewById(
                R.id.linearLayoutWriteTagFactoryFormat));
        mWriteModeLayouts.add(findViewById(
                R.id.relativeLayoutWriteTagValueBlock));

        // Restore mDumpWithPos and the "write to manufacturer block"-state.
        if (savedInstanceState != null) {
            mWriteManufBlock.setChecked(
                    savedInstanceState.getBoolean("write_manuf_block", false));
            Serializable s = savedInstanceState
                    .getSerializable("dump_with_pos");
            if (s instanceof HashMap<?, ?>) {
                mDumpWithPos = (HashMap<Integer, HashMap<Integer, byte[]>>) s;
            }
        }

        Intent i = getIntent();
        if (i.hasExtra(EXTRA_DUMP)) {
            // Write dump directly from editor.
            mDumpFromEditor = i.getStringArrayExtra(EXTRA_DUMP);
            mWriteDumpFromEditor = true;
            // Show "Write Dump" option and disable other write options.
            RadioButton writeBlock = findViewById(
                    R.id.radioButtonWriteTagWriteBlock);
            RadioButton factoryFormat = findViewById(
                    R.id.radioButtonWriteTagFactoryFormat);
            RadioButton writeDump = findViewById(
                    R.id.radioButtonWriteTagWriteDump);
            writeDump.performClick();
            writeBlock.setEnabled(false);
            factoryFormat.setEnabled(false);
            // Update button text.
            Button writeDumpButton = findViewById(
                    R.id.buttonWriteTagDump);
            writeDumpButton.setText(R.string.action_write_dump);
        }
    }

    /**
     * Save important state data before this activity gets destroyed.
     * @param outState The state to put data into.
     */
    @Override
    protected void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("write_manuf_block", mWriteManufBlock.isChecked());
        outState.putSerializable("dump_with_pos", mDumpWithPos);
    }

    /**
     * Update the layout to the current selected write mode.
     * @param view The View object that triggered the method
     * (in this case one of the write mode radio buttons).
     */
    public void onChangeWriteMode(View view) {
        for (View layout : mWriteModeLayouts) {
            layout.setVisibility(View.GONE);
        }
        View parent = findViewById(R.id.linearLayoutWriteTag);
        parent.findViewWithTag(
                view.getTag() + "_layout").setVisibility(View.VISIBLE);
    }

    /**
     * Handle incoming results from {@link KeyMapCreator} or
     * {@link FileChooser}.
     * @see #writeBlock()
     * @see #checkDumpAgainstTag()
     * @see #checkDumpAndShowSectorChooserDialog(String[])
     * @see #createFactoryFormattedDump()
     * @see #writeValueBlock()
     */
    @Override
    public void onActivityResult(int requestCode,
            int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        int ckmError = -1;

        switch(requestCode) {
        case FC_WRITE_DUMP:
            if (resultCode == Activity.RESULT_OK) {
                // Read dump and create keys.
                readDumpFromFile(data.getStringExtra(
                        FileChooser.EXTRA_CHOSEN_FILE));
            }
            break;
        case CKM_WRITE_DUMP:
            if (resultCode != Activity.RESULT_OK) {
                // Error.
                ckmError = resultCode;
            } else {
                checkDumpAgainstTag();
            }
            break;
        case CKM_FACTORY_FORMAT:
            if (resultCode != Activity.RESULT_OK) {
                // Error.
                ckmError = resultCode;
            } else {
                createFactoryFormattedDump();
            }
            break;
        case CKM_WRITE_BLOCK:
            if (resultCode != Activity.RESULT_OK) {
                // Error.
                ckmError = resultCode;
            } else {
                // Write block.
                writeBlock();
            }
            break;
        case CKM_WRITE_NEW_VALUE:
            if (resultCode != Activity.RESULT_OK) {
                // Error.
                ckmError = resultCode;
            } else {
                // Write block.
                writeValueBlock();
            }
            break;

        }

        // Error handling for the return value of KeyMapCreator.
        // So far, only error nr. 4 needs to be handled.
        if (ckmError == 4) {// Error. Path from the calling intend was null.
            // (This is really strange and should not occur.)
            Toast.makeText(this, R.string.info_strange_error,
                Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Check the user input and, if necessary, possible issues with block 0.
     * If everythin is O.K., show the {@link KeyMapCreator} with predefined mapping range
     * (see {@link #createKeyMapForBlock(int, boolean)}).
     * After a key map was created, {@link #writeBlock()} will be triggered.
     * @param view The View object that triggered the method
     * (in this case the write block button).
     * @see KeyMapCreator
     * @see #checkBlock0(String, boolean)
     * @see #checkAccessConditions(String, boolean)
     * @see #createKeyMapForBlock(int, boolean)
     */
    public void onWriteBlock(View view) {
        // Check input.
        if (!checkSectorAndBlock(mSectorTextBlock, mBlockTextBlock)) {
            return;
        }
        String data = mDataText.getText().toString();
        if (!Common.isHexAnd16Byte(data, this)) {
            return;
        }

        final int sector = Integer.parseInt(
                mSectorTextBlock.getText().toString());
        final int block = Integer.parseInt(
                mBlockTextBlock.getText().toString());

        if (!isSectorInRage(this, true)) {
            return;
        }

        if (block == 3 || block == 15) {
            // Sector Trailer.
            int acCheck = checkAccessConditions(data, true);
            if (acCheck == 1) {
                // Invalid Access Conditions. Abort.
                return;
            }
            // Warning. This is a sector trailer.
            new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_sector_trailer_warning_title)
                .setMessage(R.string.dialog_sector_trailer_warning)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.action_i_know_what_i_am_doing,
                        (dialog, which) -> {
                            // Show key map creator.
                            createKeyMapForBlock(sector, false);
                        })
                 .setNegativeButton(R.string.action_cancel,
                         (dialog, id) -> {
                             // Do nothing.
                         }).show();
        } else if (sector == 0 && block == 0) {
            // Manufacturer block.
            // Is block 0 valid? Display warning.
            int block0Check = checkBlock0(data, true);
            if (block0Check == 1 || block0Check == 2) {
                // BCC not valid. Abort.
                return;
            }
            // Warning. Writing to manufacturer block.
            showWriteManufInfo(true);
        } else {
            // Normal data block.
            createKeyMapForBlock(sector, false);
        }
    }

    /**
     * Check the user input of the sector and the block field. This is a
     * helper function for {@link #onWriteBlock(android.view.View)} and
     * {@link #onWriteValue(android.view.View)}.
     * @param sector Sector input field.
     * @param block Block input field.
     * @return True if both values are okay. False otherwise.
     */
    private boolean checkSectorAndBlock(EditText sector, EditText block) {
        if (sector.getText().toString().equals("")
                || block.getText().toString().equals("")) {
            // Error, location not fully set.
            Toast.makeText(this, R.string.info_data_location_not_set,
                    Toast.LENGTH_LONG).show();
            return false;
        }
        int sectorNr = Integer.parseInt(sector.getText().toString());
        int blockNr = Integer.parseInt(block.getText().toString());
        if (sectorNr > KeyMapCreator.MAX_SECTOR_COUNT-1
                || sectorNr < 0) {
            // Error, sector is out of range for any MIFARE tag.
            Toast.makeText(this, R.string.info_sector_out_of_range,
                    Toast.LENGTH_LONG).show();
            return false;
        }
        if (blockNr > KeyMapCreator.MAX_BLOCK_COUNT_PER_SECTOR-1
                || blockNr < 0) {
            // Error, block is out of range for any MIFARE tag.
            Toast.makeText(this, R.string.info_block_out_of_range,
                    Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }


    /**
     * Show or hide the options section of write dump.
     * @param view The View object that triggered the method
     * (in this case the show options button).
     */
    public void onShowOptions(View view) {
        LinearLayout ll = findViewById(R.id.linearLayoutWriteTagDumpOptions);
        CheckBox cb = findViewById(R.id.checkBoxWriteTagDumpOptions);
        if (cb.isChecked()) {
            ll.setVisibility(View.VISIBLE);
        } else {
            ll.setVisibility(View.GONE);
        }
    }

    /**
     * Display information about writing to the manufacturer block.
     * @param view The View object that triggered the method
     * (in this case the info on write-to-manufacturer button).
     * @see #showWriteManufInfo(boolean)
     */
    public void onShowWriteManufInfo(View view) {
        showWriteManufInfo(false);
    }

    /**
     * Display information about writing to the manufacturer block and
     * optionally create a key map for the first sector.
     * @param createKeyMap If true {@link #createKeyMapForBlock(int, boolean)}
     * will be triggered the time the user confirms the dialog.
     */
    private void showWriteManufInfo(final boolean createKeyMap) {
        // Warning. Writing to the manufacturer block is not normal.
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.dialog_block0_writing_title);
        dialog.setMessage(R.string.dialog_block0_writing);
        dialog.setIcon(android.R.drawable.ic_dialog_info);

        int buttonID = R.string.action_ok;
        if (createKeyMap) {
            buttonID = R.string.action_i_know_what_i_am_doing;
            dialog.setNegativeButton(R.string.action_cancel,
                    (dialog12, which) -> {
                        // Do nothing.
                    });
        }
        dialog.setPositiveButton(buttonID,
                (dialog1, which) -> {
                    // Do nothing or create a key map.
                    if (createKeyMap) {
                        createKeyMapForBlock(0, false);
                    }
                });
        dialog.show();
    }

    /**
     * Check if block 0 data is valid and show a error message if needed.
     * @param block0 Hex string of block 0.
     * @param showToasts If true, show error mesages as toast.
     * @return <ul>
     * <li>0 - Everything is O.K.</li>
     * <li>1 - There is no tag.</li>
     * <li>2 - BCC is not valid.</li>
     * <li>3 - SAK or ATQA is not valid.</li>
     * </ul>
     */
    private int checkBlock0(String block0, boolean showToasts) {
        MCReader reader = Common.checkForTagAndCreateReader(this);
        if (reader == null) {
            // Error. There is no tag.
            return 1;
        }
        reader.close();
        int uidLen = Common.getUID().length;

        // BCC.
        if (uidLen == 4 ) {
            byte bcc = Common.hex2Bytes(block0.substring(8, 10))[0];
            byte[] uid = Common.hex2Bytes(block0.substring(0, 8));
            boolean isValidBcc = Common.isValidBcc(uid, bcc);
            if (!isValidBcc) {
                // Error. BCC is not valid. Show error message.
                if (showToasts) {
                    Toast.makeText(this, R.string.info_bcc_not_valid,
                            Toast.LENGTH_LONG).show();
                }
                return 2;
            }
        }

        // SAK & ATQA.
        boolean isValidBlock0 = Common.isValidBlock0(
                block0, uidLen, reader.getSize(), true);
        if (!isValidBlock0) {
            if (showToasts) {
                Toast.makeText(this, R.string.text_block0_warning,
                        Toast.LENGTH_LONG).show();
            }
            return 3;
        }

        // Everything was O.K.
        return 0;
    }

    /**
     * Check if the Access Conditions of a Sector Trailer are correct and
     * if they are irreversible (shows a error message if needed).
     * @param sectorTrailer The Sector Trailer as hex string.
     * @param showToasts If true, show error mesages as toast.
     * @return <ul>
     * <li>0 - Everything is O.K.</li>
     * <li>1 - The Access Conditions are invalid.</li>
     * <li>2 - The Access Conditions are irreversible.</li>
     * </ul>
     */
    private int checkAccessConditions(String sectorTrailer, boolean showToasts) {
        // Check if Access Conditions are valid.
        byte[] acBytes = Common.hex2Bytes(sectorTrailer.substring(12, 18));
        byte[][] acMatrix = Common.acBytesToACMatrix(acBytes);
        if (acMatrix == null) {
            // Error. Invalid ACs.
            if (showToasts) {
                Toast.makeText(this, R.string.info_ac_format_error,
                        Toast.LENGTH_LONG).show();
            }
            return 1;
        }
        // Check if Access Conditions are irreversible.
        boolean keyBReadable = Common.isKeyBReadable(
                acMatrix[0][3], acMatrix[1][3], acMatrix[2][3]);
        int writeAC = Common.getOperationRequirements(
                acMatrix[0][3], acMatrix[1][3], acMatrix[2][3],
                Common.Operation.WriteAC, true, keyBReadable);
        if (writeAC == 0) {
            // Warning. Access Conditions can not be changed after writing.
            if (showToasts) {
                Toast.makeText(this, R.string.info_irreversible_acs,
                        Toast.LENGTH_LONG).show();
            }
            return 2;
        }
        return 0;
    }

    /**
     * Display information about using custom Access Conditions for all
     * sectors of the dump.
     * @param view The View object that triggered the method
     * (in this case the info on "use-static-access-conditions" button).
     */
    public void onShowStaticACInfo(View view) {
        new AlertDialog.Builder(this)
        .setTitle(R.string.dialog_static_ac_title)
        .setMessage(R.string.dialog_static_ac)
        .setIcon(android.R.drawable.ic_dialog_info)
        .setPositiveButton(R.string.action_ok,
                (dialog, which) -> {
                    // Do nothing.
                }).show();
    }

    /**
     * Helper function for {@link #onWriteBlock(View)} and
     * {@link #onWriteValue(android.view.View)} to show
     * the {@link KeyMapCreator}.
     * @param sector The sector for the mapping range of
     * {@link KeyMapCreator}
     * @param isValueBlock If true, the key map will be created for a Value
     * Block ({@link #writeValueBlock()}).
     * @see KeyMapCreator
     * @see #onWriteBlock(View)
     * @see #onWriteValue(android.view.View)
     */
    private void createKeyMapForBlock(int sector, boolean isValueBlock) {
        Intent intent = new Intent(this, KeyMapCreator.class);
        intent.putExtra(KeyMapCreator.EXTRA_KEYS_DIR,
                Common.getFile(Common.KEYS_DIR).getAbsolutePath());
        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER, false);
        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER_FROM, sector);
        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER_TO, sector);
        if (isValueBlock) {
            intent.putExtra(KeyMapCreator.EXTRA_BUTTON_TEXT, getString(
                    R.string.action_create_key_map_and_write_value_block));
            startActivityForResult(intent, CKM_WRITE_NEW_VALUE);
        } else {
            intent.putExtra(KeyMapCreator.EXTRA_BUTTON_TEXT, getString(
                    R.string.action_create_key_map_and_write_block));
            startActivityForResult(intent, CKM_WRITE_BLOCK);
        }
    }

    /**
     * Called from {@link #onActivityResult(int, int, Intent)}
     * after a key map was created, this method tries to write the given
     * data to the tag. Possible errors are displayed to the user via Toast.
     * @see #onActivityResult(int, int, Intent)
     * @see #onWriteBlock(View)
     */
    private void writeBlock() {
        MCReader reader = Common.checkForTagAndCreateReader(this);
        if (reader == null) {
            return;
        }
        int sector = Integer.parseInt(mSectorTextBlock.getText().toString());
        int block = Integer.parseInt(mBlockTextBlock.getText().toString());
        String data = mDataText.getText().toString();
        byte[][] keys = Common.getKeyMap().get(sector);
        int result = -1;

        if (sector == 0 && block == 0) {
            // Write the manufacturer bock. This is only possible on gen2 tags.
            // There are some gen2 tags which report a successful write, although
            // the write was not successful. Therefore, we try write it using both keys.
            int resultKeyA = -1;
            int resultKeyB = -1;
            if (keys[1] != null) {
                resultKeyB = reader.writeBlock(sector, block,
                        Common.hex2Bytes(data),
                        keys[1], true);
            }
            if (keys[0] != null) {
                resultKeyA = reader.writeBlock(sector, block,
                        Common.hex2Bytes(data),
                        keys[0], false);
            }
            if (resultKeyA == 0 || resultKeyB == 0) {
                result = 0;
            }
        } else {
            // Normal block.
            // Try key B first.
            if (keys[1] != null) {
                result = reader.writeBlock(sector, block,
                        Common.hex2Bytes(data),
                        keys[1], true);
            }
            // Error while writing? Try to write with key A (if there is one).
            if ((result == -1 || result == 4) && keys[0] != null) {
                result = reader.writeBlock(sector, block,
                        Common.hex2Bytes(data),
                        keys[0], false);
            }
        }
        reader.close();

        // Error handling.
        switch (result) {
        case 2:
            Toast.makeText(this, R.string.info_block_not_in_sector,
                    Toast.LENGTH_LONG).show();
            return;
        case -1:
            Toast.makeText(this, R.string.info_error_writing_block,
                    Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, R.string.info_write_successful,
                Toast.LENGTH_LONG).show();
        finish();
    }

    /**
     * Regular behavior: Check input, open a file chooser ({@link FileChooser})
     * to select a dump and wait for its result in
     * {@link #onActivityResult(int, int, Intent)}.
     * This method triggers the call chain: open {@link FileChooser}
     * (this method) -> read dump ({@link #readDumpFromFile(String)})
     * -> check dump ({@link #checkDumpAndShowSectorChooserDialog(String[])}) ->
     * open {@link KeyMapCreator} ({@link #createKeyMapForDump()})
     * -> run {@link #checkDumpAgainstTag()} -> run
     * {@link #writeDump(HashMap, SparseArray)}.<br />
     * Behavior if the dump is already there (from the {@link DumpEditor}):
     * The same as before except the call chain will directly start from
     * {@link #checkDumpAndShowSectorChooserDialog(String[])}.<br />
     * (The static Access Conditions will be checked in any case, if the
     * option is enabled.)
     * @param view The View object that triggered the method
     * (in this case the write full dump button).
     * @see FileChooser
     * @see #onActivityResult(int, int, Intent)
     */
    public void onWriteDump(View view) {
        // Check the static Access Condition option.
        if (mEnableStaticAC.isChecked()) {
            String ac = mStaticAC.getText().toString();
            if (!ac.matches("[0-9A-Fa-f]+")) {
                // Error, not hex.
                Toast.makeText(this, R.string.info_ac_not_hex,
                        Toast.LENGTH_LONG).show();
                return;
            }
            if (ac.length() != 6) {
                // Error, not 3 byte (6 chars).
                Toast.makeText(this, R.string.info_ac_not_3_byte,
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        if (mWriteDumpFromEditor) {
            // Write dump directly from the dump editor.
            // (Dump has already been chosen.)
            checkDumpAndShowSectorChooserDialog(mDumpFromEditor);
        } else {
            // Show file chooser (chose dump).
            Intent intent = new Intent(this, FileChooser.class);
            intent.putExtra(FileChooser.EXTRA_DIR,
                    Common.getFile(Common.DUMPS_DIR).getAbsolutePath());
            intent.putExtra(FileChooser.EXTRA_TITLE,
                    getString(R.string.text_open_dump_title));
            intent.putExtra(FileChooser.EXTRA_CHOOSER_TEXT,
                    getString(R.string.text_choose_dump_to_write));
            intent.putExtra(FileChooser.EXTRA_BUTTON_TEXT,
                    getString(R.string.action_write_full_dump));
            startActivityForResult(intent, FC_WRITE_DUMP);
        }
    }

    /**
     * Read the dump and call {@link #checkDumpAndShowSectorChooserDialog(String[])}.
     * @param pathToDump path and filename of the dump
     * (selected by {@link FileChooser}).
     * @see #checkDumpAndShowSectorChooserDialog(String[])
     */
    private void readDumpFromFile(String pathToDump) {
        // Read dump.
        File file = new File(pathToDump);
        String[] dump = Common.readFileLineByLine(file, false, this);
        checkDumpAndShowSectorChooserDialog(dump);
    }

    /**
     * Triggered after the dump was selected (by {@link FileChooser})
     * and read (by {@link #readDumpFromFile(String)}), this method saves
     * the data including its position in {@link #mDumpWithPos}.
     * If the "use static Access Condition" option is enabled, all the ACs
     * will be replaced by the static ones. After this it will show a dialog
     * in which the user can choose the sectors he wants
     * to write. When the sectors are chosen, this method calls
     * {@link #createKeyMapForDump()} to create a key map for the present tag.
     * @param dump Dump selected by {@link FileChooser} or directly
     * from the {@link DumpEditor} (via an Intent with{@link #EXTRA_DUMP})).
     * @see KeyMapCreator
     * @see #createKeyMapForDump()
     * @see #checkBlock0(String, boolean)
     */
    @SuppressLint("SetTextI18n")
    private void checkDumpAndShowSectorChooserDialog(final String[] dump) {
        int err = Common.isValidDump(dump, false);
        if (err != 0) {
            // Error.
            Common.isValidDumpErrorToast(err, this);
            return;
        }

        initDumpWithPosFromDump(dump);

        // Create and show sector chooser dialog
        // (let the user select the sectors which will be written).
        View dialogLayout = getLayoutInflater().inflate(
                R.layout.dialog_write_sectors,
                findViewById(android.R.id.content), false);
        LinearLayout llCheckBoxes = dialogLayout.findViewById(
                R.id.linearLayoutWriteSectorsCheckBoxes);
        Button selectAll = dialogLayout.findViewById(
                R.id.buttonWriteSectorsSelectAll);
        Button selectNone = dialogLayout.findViewById(
                R.id.buttonWriteSectorsSelectNone);
        Integer[] sectors = mDumpWithPos.keySet().toArray(
                new Integer[0]);
        Arrays.sort(sectors);
        final Context context = this;
        final CheckBox[] sectorBoxes = new CheckBox[mDumpWithPos.size()];
        for (int i = 0; i< sectors.length; i++) {
            sectorBoxes[i] = new CheckBox(this);
            sectorBoxes[i].setChecked(true);
            sectorBoxes[i].setTag(sectors[i]);
            sectorBoxes[i].setText(getString(R.string.text_sector)
                    + " " + sectors[i]);
            llCheckBoxes.addView(sectorBoxes[i]);
        }
        OnClickListener listener = v -> {
            String tag = v.getTag().toString();
            for (CheckBox box : sectorBoxes) {
                box.setChecked(tag.equals("all"));
            }
        };
        selectAll.setOnClickListener(listener);
        selectNone.setOnClickListener(listener);

        final AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_write_sectors_title)
            .setIcon(android.R.drawable.ic_menu_edit)
            .setView(dialogLayout)
            .setPositiveButton(R.string.action_ok,
                    (dialog12, which) -> {
                        // Do nothing here because we override this button later
                        // to change the close behaviour. However, we still need
                        // this because on older versions of Android unless we
                        // pass a handler the button doesn't get instantiated
                    })
            .setNegativeButton(R.string.action_cancel,
                    (dialog1, which) -> {
                        // Do nothing.
                    })
            .create();
        dialog.show();
        final Context con = this;

        // Override/define behavior for positive button click.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
                v -> {
                    // Re-Init mDumpWithPos in order to remove unwanted sectors.
                    initDumpWithPosFromDump(dump);
                    for (CheckBox box : sectorBoxes) {
                        int sector = Integer.parseInt(box.getTag().toString());
                        if (!box.isChecked()) {
                            mDumpWithPos.remove(sector);
                        }
                    }
                    if (mDumpWithPos.size() == 0) {
                        // Error. There is nothing to write.
                        Toast.makeText(context, R.string.info_nothing_to_write,
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Check if last sector is out of range.
                    if (!isSectorInRage(con, false)) {
                        return;
                    }

                    // Create key map.
                    createKeyMapForDump();
                    dialog.dismiss();
                });
    }

    /**
     * Check if the chosen sector or last sector of a dump is in the
     * range of valid sectors (according to {@link Preferences}).
     * @param context The context in error messages are displayed.
     * @return True if the sector is in range, False if not. Also,
     * if there was no tag False will be returned.
     */
    private boolean isSectorInRage(Context context, boolean isWriteBlock) {
        MCReader reader = Common.checkForTagAndCreateReader(this);
        if (reader == null) {
            return false;
        }
        int lastValidSector = reader.getSectorCount() - 1;
        int lastSector;
        reader.close();
        // Initialize last sector.
        if (isWriteBlock) {
            lastSector = Integer.parseInt(
                    mSectorTextBlock.getText().toString());
        } else {
            lastSector = Collections.max(mDumpWithPos.keySet());
        }

        // Is last sector in range?
        if (lastSector > lastValidSector) {
            // Error. Tag too small for dump.
            Toast.makeText(context, R.string.info_tag_too_small,
                    Toast.LENGTH_LONG).show();
            reader.close();
            return false;
        }
        return true;
    }

    /**
     * Initialize {@link #mDumpWithPos} with the data from a dump.
     * Transform the simple dump array into a structure (mDumpWithPos)
     * where the sector and block information are known additionally.
     * Blocks containing unknown data ("-") are dropped.
     * @param dump The dump to initialize the mDumpWithPos with.
     */
    private void initDumpWithPosFromDump(String[] dump) {
        mDumpWithPos = new HashMap<>();
        int sector = 0;
        int block = 0;
        // Transform the simple dump array into a structure (mDumpWithPos)
        // where the sector and block information are known additionally.
        // Blocks containing unknown data ("-") are dropped.
        for (int i = 0; i < dump.length; i++) {
            if (dump[i].startsWith("+")) {
                String[] tmp = dump[i].split(": ");
                sector = Integer.parseInt(tmp[tmp.length-1]);
                block = 0;
                mDumpWithPos.put(sector, new HashMap<>());
            } else if (!dump[i].contains("-")) {
                // Use static Access Conditions for all sectors?
                if (mEnableStaticAC.isChecked()
                        && (i+1 == dump.length || dump[i+1].startsWith("+"))) {
                    // This is a Sector Trailer. Replace its ACs
                    // with the static ones.
                    String newBlock = dump[i].substring(0, 12)
                            + mStaticAC.getText().toString()
                            + dump[i].substring(18);
                    dump[i] = newBlock;
                }
                mDumpWithPos.get(sector).put(block++,
                        Common.hex2Bytes(dump[i]));
            } else {
                block++;
            }
        }
    }

    /**
     * Create a key map for the dump ({@link #mDumpWithPos}).
     * @see KeyMapCreator
     */
    private void createKeyMapForDump() {
        // Show key map creator.
        Intent intent = new Intent(this, KeyMapCreator.class);
        intent.putExtra(KeyMapCreator.EXTRA_KEYS_DIR,
                Common.getFile(Common.KEYS_DIR).getAbsolutePath());
        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER, false);
        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER_FROM,
                (int) Collections.min(mDumpWithPos.keySet()));
        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER_TO,
                (int) Collections.max(mDumpWithPos.keySet()));
        intent.putExtra(KeyMapCreator.EXTRA_BUTTON_TEXT,
                getString(R.string.action_create_key_map_and_write_dump));
        startActivityForResult(intent, CKM_WRITE_DUMP);
    }

    /**
     * Check if the tag is suitable for the dump ({@link #mDumpWithPos}).
     * This is done in four steps. The first check determines if the dump
     * fits on the tag (size check). The second check determines if the keys for
     * relevant sectors are known (key check). The third check determines if
     * specail blocks (block 0 and sector trailers) are correct. At last this
     * method will check whether the keys with write privileges are known and if
     * some blocks are read-only (write check).<br />
     * If some of these checks "fail", the user will get a report dialog
     * with the two options to cancel the whole write process or to
     * write as much as possible(call {@link #writeDump(HashMap,
     * SparseArray)}).
     * @see MCReader#isWritableOnPositions(HashMap, SparseArray)
     * @see Common#getOperationRequirements(byte, byte,
     * byte, Common.Operation, boolean, boolean)
     * @see #writeDump(HashMap, SparseArray)
     */
    private void checkDumpAgainstTag() {
        // Create reader.
        MCReader reader = Common.checkForTagAndCreateReader(this);
        if (reader == null) {
            Toast.makeText(this, R.string.info_tag_lost_check_dump,
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Check if tag is correct size for dump.
        if (reader.getSectorCount()-1 < Collections.max(
                mDumpWithPos.keySet())) {
            // Error. Tag too small for dump.
            Toast.makeText(this, R.string.info_tag_too_small,
                    Toast.LENGTH_LONG).show();
            reader.close();
            return;
        }

        // Check if tag is writable on needed blocks.
        // Reformat for reader.isWritableOnPosition(...).
        final SparseArray<byte[][]> keyMap  =
                Common.getKeyMap();
        HashMap<Integer, int[]> dataPos =
                new HashMap<>(mDumpWithPos.size());
        for (int sector : mDumpWithPos.keySet()) {
            int i = 0;
            int[] blocks = new int[mDumpWithPos.get(sector).size()];
            for (int block : mDumpWithPos.get(sector).keySet()) {
                blocks[i++] = block;
            }
            dataPos.put(sector, blocks);
        }
        HashMap<Integer, HashMap<Integer, Integer>> writeOnPos =
                reader.isWritableOnPositions(dataPos, keyMap);
        reader.close();

        if (writeOnPos == null) {
            // Error while checking for keys with write privileges.
            Toast.makeText(this, R.string.info_tag_lost_check_dump,
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Skip dialog:
        // Build a dialog showing all sectors and blocks containing data
        // that can not be overwritten with the reason why they are not
        // writable. The user can chose to skip all these blocks/sectors
        // or to cancel the whole write procedure.
        List<HashMap<String, String>> list = new
                ArrayList<>();
        final HashMap<Integer, HashMap<Integer, Integer>> writeOnPosSafe =
                new HashMap<>(
                        mDumpWithPos.size());

        // Check for keys that are missing completely (mDumpWithPos vs. keyMap).
        HashSet<Integer> sectors = new HashSet<>();
        for (int sector : mDumpWithPos.keySet()) {
            if (keyMap.indexOfKey(sector) < 0) {
                // Problem. Keys for sector not found.
                addToList(list, getString(R.string.text_sector) + ": " + sector,
                        getString(R.string.text_keys_not_known));
            } else {
                sectors.add(sector);
            }
        }

        // Check for keys with write privileges that are missing (writeOnPos vs. keyMap).
        // Check for blocks (block-parts) that are read-only.
        // Check for issues of block 0 of the dump about to be written.
        // Check the Access Conditions of the dump about to be written.
        for (int sector : sectors) {
            if (writeOnPos.get(sector) == null) {
                // Error. Sector is dead (IO Error) or ACs are invalid.
                addToList(list, getString(R.string.text_sector) + ": " + sector,
                        getString(R.string.text_invalid_ac_or_sector_dead));
                continue;
            }
            byte[][] keys = keyMap.get(sector);
            Set<Integer> blocks = mDumpWithPos.get(sector).keySet();
            for (int block : blocks) {
                boolean isSafeForWriting = true;
                String position = getString(R.string.text_sector) + ": "
                        + sector + ", " + getString(R.string.text_block)
                        + ": " + block;

                // Special block 0 checks.
                if (!mWriteManufBlock.isChecked()
                        && sector == 0 && block == 0) {
                    // Block 0 is read-only. This is normal. Skip.
                    continue;
                } else if (mWriteManufBlock.isChecked()
                        && sector == 0 && block == 0) {
                    // Block 0 should be written. Check it.
                    String block0 = Common.bytes2Hex(mDumpWithPos.get(0).get(0));
                    int block0Check = checkBlock0(block0, false);
                    switch (block0Check) {
                        case 1:
                            Toast.makeText(this, R.string.info_tag_lost_check_dump,
                                    Toast.LENGTH_LONG).show();
                            return;
                        case 2:
                            // BCC not valid. Abort.
                            Toast.makeText(this, R.string.info_bcc_not_valid,
                                    Toast.LENGTH_LONG).show();
                            return;
                        case 3:
                            addToList(list, position, getString(
                                    R.string.text_block0_warning));
                            break;
                    }
                }

                // Special Access Conditions checks.
                if ((sector < 31 && block == 3) || sector >= 31 && block == 15) {
                    String sectorTrailer = Common.bytes2Hex(
                            mDumpWithPos.get(sector).get(block));
                    int acCheck = checkAccessConditions(sectorTrailer, false);
                    switch (acCheck) {
                        case 1:
                            // Access Conditions not valid. Abort.
                            Toast.makeText(this, R.string.info_ac_format_error,
                                    Toast.LENGTH_LONG).show();
                            return;
                        case 2:
                            addToList(list, position, getString(
                                    R.string.info_irreversible_acs));
                            break;
                    }
                }

                // Normal write privileges checks.
                int writeInfo = writeOnPos.get(sector).get(block);
                switch (writeInfo) {
                case 0:
                    // Problem. Block is read-only.
                    addToList(list, position, getString(
                            R.string.text_block_read_only));
                    isSafeForWriting = false;
                    break;
                case 1:
                    if (keys[0] == null) {
                        // Problem. Key with write privileges (A) not known.
                        addToList(list, position, getString(
                                R.string.text_write_key_a_not_known));
                        isSafeForWriting = false;
                    }
                    break;
                case 2:
                    if (keys[1] == null) {
                        // Problem. Key with write privileges (B) not known.
                        addToList(list, position, getString(
                                R.string.text_write_key_b_not_known));
                        isSafeForWriting = false;
                    }
                    break;
                case 3:
                    // No Problem. Both keys have write privileges.
                    // Set to key A or B depending on which one is available.
                    writeInfo = (keys[0] != null) ? 1 : 2;
                    break;
                case 4:
                    if (keys[0] == null) {
                        // Problem. Key with write privileges (A) not known.
                        addToList(list, position, getString(
                                R.string.text_write_key_a_not_known));
                        isSafeForWriting = false;
                    } else {
                        // Problem. ACs are read-only.
                        addToList(list, position, getString(
                                R.string.text_ac_read_only));
                    }
                    break;
                case 5:
                    if (keys[1] == null) {
                        // Problem. Key with write privileges (B) not known.
                        addToList(list, position, getString(
                                R.string.text_write_key_b_not_known));
                        isSafeForWriting = false;
                    } else {
                        // Problem. ACs are read-only.
                        addToList(list, position, getString(
                                R.string.text_ac_read_only));
                    }
                    break;
                case 6:
                    if (keys[1] == null) {
                        // Problem. Key with write privileges (B) not known.
                        addToList(list, position, getString(
                                R.string.text_write_key_b_not_known));
                        isSafeForWriting = false;
                    } else {
                        // Problem. Keys are read-only.
                        addToList(list, position, getString(
                                R.string.text_keys_read_only));
                    }
                    break;
                case -1:
                    // Error. Some strange error occurred. Maybe due to some
                    // corrupted ACs...
                    addToList(list, position, getString(
                            R.string.text_strange_error));
                    isSafeForWriting = false;
                }
                // Add if safe for writing.
                if (isSafeForWriting) {
                    if (writeOnPosSafe.get(sector) == null) {
                        // Create sector.
                        HashMap<Integer, Integer> blockInfo =
                                new HashMap<>();
                        blockInfo.put(block, writeInfo);
                        writeOnPosSafe.put(sector, blockInfo);
                    } else {
                        // Add to sector.
                        writeOnPosSafe.get(sector).put(block, writeInfo);
                    }
                }
            }
        }

        // Show skip/cancel dialog (if needed).
        if (list.size() != 0) {
            // If the user skips all sectors/blocks that are not writable,
            // the writeTag() method will be called.
            LinearLayout ll = new LinearLayout(this);
            int pad = Common.dpToPx(5);
            ll.setPadding(pad, pad, pad, pad);
            ll.setOrientation(LinearLayout.VERTICAL);
            TextView textView = new TextView(this);
            textView.setText(R.string.dialog_write_issues);
            textView.setPadding(0,0,0, Common.dpToPx(5));
            TextViewCompat.setTextAppearance(textView,
                    android.R.style.TextAppearance_Medium);
            ListView listView = new ListView(this);
            ll.addView(textView);
            ll.addView(listView);
            String[] from = new String[] {"position", "reason"};
            int[] to = new int[] {android.R.id.text1, android.R.id.text2};
            ListAdapter adapter = new SimpleAdapter(this, list,
                    android.R.layout.two_line_list_item, from, to);
            listView.setAdapter(adapter);

            new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_write_issues_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setView(ll)
                .setPositiveButton(R.string.action_skip_blocks,
                        (dialog, which) -> {
                            // Skip not writable blocks and start writing.
                            writeDump(writeOnPosSafe, keyMap);
                        })
                .setNegativeButton(R.string.action_cancel_all,
                        (dialog, which) -> {
                            // Do nothing.
                        })
                .show();
        } else {
            // Write.
            writeDump(writeOnPosSafe, keyMap);
        }
    }

    /**
     * A helper function for {@link #checkDumpAgainstTag()} adding an item to
     * the list of all blocks with write issues.
     * This list will be displayed to the user in a dialog before writing.
     * @param list The list in which to add the key-value-pair.
     * @param position The key (position) for the list item
     * (e.g. "Sector 2, Block 3").
     * @param reason The value (reason) for the list item
     * (e.g. "Block is read-only").
     */
    private void addToList(List<HashMap<String, String>> list,
            String position, String reason) {
        HashMap<String, String> item = new HashMap<>();
        item.put( "position", position);
        item.put( "reason", reason);
        list.add(item);
    }

    /**
     * This method is triggered by {@link #checkDumpAgainstTag()} and writes a dump
     * to a tag.
     * @param writeOnPos A map within a map (all with type = Integer).
     * The key of the outer map is the sector number and the value is another
     * map with key = block number and value = write information. The write
     * information must be filtered (by {@link #checkDumpAgainstTag()}) return values
     * of {@link MCReader#isWritableOnPositions(HashMap, SparseArray)}.<br />
     * Attention: This method does not any checking. The position and write
     * information must be checked by {@link #checkDumpAgainstTag()}.
     * @param keyMap A key map generated by {@link KeyMapCreator}.
     */
    private void writeDump(
            final HashMap<Integer, HashMap<Integer, Integer>> writeOnPos,
            final SparseArray<byte[][]> keyMap) {
        // Check for write data.
        if (writeOnPos.size() == 0) {
            // Nothing to write. Exit.
            Toast.makeText(this, R.string.info_nothing_to_write,
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Create reader.
        final MCReader reader = Common.checkForTagAndCreateReader(this);
        if (reader == null) {
            return;
        }

        // Display don't remove warning.
        LinearLayout ll = new LinearLayout(this);
        int pad = Common.dpToPx(20);
        ll.setPadding(pad, pad, pad, pad);
        ll.setGravity(Gravity.CENTER);
        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        pad = Common.dpToPx(20);
        progressBar.setPadding(0, 0, pad, 0);
        TextView tv = new TextView(this);
        tv.setText(getString(R.string.dialog_wait_write_tag));
        tv.setTextSize(18);
        ll.addView(progressBar);
        ll.addView(tv);
        final AlertDialog warning = new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_wait_write_tag_title)
            .setView(ll)
            .create();
        warning.show();


        // Start writing in new thread.
        final Activity a = this;
        final Handler handler = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            // Write dump to tag.
            for (int sector : writeOnPos.keySet()) {
                byte[][] keys = keyMap.get(sector);
                for (int block : writeOnPos.get(sector).keySet()) {
                    // Select key with write privileges.
                    byte[] writeKey = null;
                    boolean useAsKeyB = true;
                    int wi = writeOnPos.get(sector).get(block);
                    if (wi == 1 || wi == 4) {
                        writeKey = keys[0]; // Write with key A.
                        useAsKeyB = false;
                    } else if (wi == 2 || wi == 5 || wi == 6) {
                        writeKey = keys[1]; // Write with key B.
                    }

                    // Write block.
                    int result = reader.writeBlock(sector, block,
                            mDumpWithPos.get(sector).get(block),
                            writeKey, useAsKeyB);

                    if (result != 0) {
                        // Error. Some error while writing.
                        handler.post(() -> Toast.makeText(a,
                                R.string.info_write_error,
                                Toast.LENGTH_LONG).show());
                        reader.close();
                        warning.cancel();
                        return;
                    }
                }
            }
            // Finished writing.
            reader.close();
            warning.cancel();
            handler.post(() -> Toast.makeText(a, R.string.info_write_successful,
                    Toast.LENGTH_LONG).show());
            a.finish();
        }).start();
    }

    /**
     * Open the clone UID tool.
     * @param view The View object that triggered the method
     * (in this case the clone UID button).
     * @see KeyMapCreator
     */
    public void onCloneUid(View view) {
        // Show the clone UID tool.
        Intent intent = new Intent(this, CloneUidTool.class);
        startActivity(intent);
    }

    /**
     * Open key map creator.
     * @param view The View object that triggered the method
     * (in this case the factory format button).
     * @see KeyMapCreator
     */
    public void onFactoryFormat(View view) {
        // Show key map creator.
        Intent intent = new Intent(this, KeyMapCreator.class);
        intent.putExtra(KeyMapCreator.EXTRA_KEYS_DIR,
                Common.getFile(Common.KEYS_DIR).getAbsolutePath());
        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER, false);
        intent.putExtra(KeyMapCreator.EXTRA_BUTTON_TEXT,
                getString(R.string.action_create_key_map_and_factory_format));
        startActivityForResult(intent, CKM_FACTORY_FORMAT);
    }

    /**
     * Create an factory formatted, empty dump with a size matching
     * the current tag size and then call {@link #checkDumpAgainstTag()}.
     * Factory (default) MIFARE Classic Access Conditions are: 0xFF0780XX
     * XX = General purpose byte (GPB): Most of the time 0x69. At the end of
     * an Tag XX = 0xBC.
     * @see #checkDumpAgainstTag()
     */
    private void createFactoryFormattedDump() {
        // This function is directly called after a key map was created.
        // So Common.getTag() will return den current present tag
        // (and its size/sector count).
        mDumpWithPos = new HashMap<>();
        int sectors = MifareClassic.get(Common.getTag()).getSectorCount();
        byte[] emptyBlock = new byte[]
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        byte[] normalSectorTrailer = new byte[] {-1, -1, -1, -1, -1, -1,
                -1, 7, -128, 105, -1, -1, -1, -1, -1, -1};
        byte[] lastSectorTrailer = new byte[] {-1, -1, -1, -1, -1, -1,
                -1, 7, -128, -68, -1, -1, -1, -1, -1, -1};
        // Empty 4 block sector.
        HashMap<Integer, byte[]> empty4BlockSector =
                new HashMap<>(4);
        for (int i = 0; i < 3; i++) {
            empty4BlockSector.put(i, emptyBlock);
        }
        empty4BlockSector.put(3, normalSectorTrailer);
        // Empty 16 block sector.
        HashMap<Integer, byte[]> empty16BlockSector =
                new HashMap<>(16);
        for (int i = 0; i < 15; i++) {
            empty16BlockSector.put(i, emptyBlock);
        }
        empty16BlockSector.put(15, normalSectorTrailer);
        // Last sector.
        HashMap<Integer, byte[]> lastSector;

        // Sector 0.
        HashMap<Integer, byte[]> firstSector =
                new HashMap<>(4);
        firstSector.put(1, emptyBlock);
        firstSector.put(2, emptyBlock);
        firstSector.put(3, normalSectorTrailer);
        mDumpWithPos.put(0, firstSector);
        // Sector 1 - (max.) 31.
        for (int i = 1; i < sectors && i < 32; i++) {
            mDumpWithPos.put(i, empty4BlockSector);
        }
        // Sector 32 - 39.
        if (sectors == 40) {
            // Add the large sectors (containing 16 blocks)
            // of a MIFARE Classic 4k tag.
            for (int i = 32; i < sectors && i < 39; i++) {
                mDumpWithPos.put(i, empty16BlockSector);
            }
            // In the last sector the Sector Trailer is different.
            lastSector = new HashMap<>(empty16BlockSector);
            lastSector.put(15, lastSectorTrailer);
        } else {
            // In the last sector the Sector Trailer is different.
            lastSector = new HashMap<>(empty4BlockSector);
            lastSector.put(3, lastSectorTrailer);
        }
        mDumpWithPos.put(sectors - 1, lastSector);
        checkDumpAgainstTag();
    }

    /**
     * Check the user input and (if correct) show the
     * {@link KeyMapCreator} with predefined mapping range
     * (see {@link #createKeyMapForBlock(int, boolean)}).
     * After a key map was created {@link #writeValueBlock()} will be triggered.
     * @param view The View object that triggered the method
     * (in this case the write Value Block button).
     * @see KeyMapCreator
     * @see #checkSectorAndBlock(android.widget.EditText,
     * android.widget.EditText)
     */
    public void onWriteValue(View view) {
        // Check input.
        if (!checkSectorAndBlock(mSectorTextVB, mBlockTextVB)) {
            return;
        }

        int sector = Integer.parseInt(mSectorTextVB.getText().toString());
        int block = Integer.parseInt(mBlockTextVB.getText().toString());
        if (block == 3 || block == 15 || (sector == 0 && block == 0)) {
            // Error. Block can't be a Value Block.
            Toast.makeText(this, R.string.info_not_vb,
                    Toast.LENGTH_LONG).show();
            return;
        }

        try {
            Integer.parseInt(mNewValueTextVB.getText().toString());
        } catch (Exception e) {
            // Error. Value is too big.
            Toast.makeText(this, R.string.info_value_too_big,
                    Toast.LENGTH_LONG).show();
            return;
        }

        createKeyMapForBlock(sector, true);
    }

    /**
     * Called from {@link #onActivityResult(int, int, Intent)}
     * after a key map was created, this method tries to increment or
     * decrement the Value Block. Possible errors are displayed to the
     * user via Toast.
     * @see #onActivityResult(int, int, Intent)
     * @see #onWriteValue(android.view.View)
     */
    private void writeValueBlock() {
        // Write the new value (incr./decr. + transfer).
        MCReader reader = Common.checkForTagAndCreateReader(this);
        if (reader == null) {
            return;
        }
        int value = Integer.parseInt(mNewValueTextVB.getText().toString());
        int sector = Integer.parseInt(mSectorTextVB.getText().toString());
        int block = Integer.parseInt(mBlockTextVB.getText().toString());
        byte[][] keys = Common.getKeyMap().get(sector);
        int result = -1;

        if (keys[1] != null) {
            result = reader.writeValueBlock(sector, block, value,
                    mIncreaseVB.isChecked(),
                    keys[1], true);
        }
        // Error while writing? Maybe tag has default factory settings ->
        // try to write with key a (if there is one).
        if (result == -1 && keys[0] != null) {
            result = reader.writeValueBlock(sector, block, value,
                    mIncreaseVB.isChecked(),
                    keys[0], false);
        }
        reader.close();

        // Error handling.
        switch (result) {
            case 2:
                Toast.makeText(this, R.string.info_block_not_in_sector,
                        Toast.LENGTH_LONG).show();
                return;
            case -1:
                Toast.makeText(this, R.string.info_error_writing_value_block,
                        Toast.LENGTH_LONG).show();
                return;
        }
        Toast.makeText(this, R.string.info_write_successful,
                Toast.LENGTH_LONG).show();
        finish();
    }
}
