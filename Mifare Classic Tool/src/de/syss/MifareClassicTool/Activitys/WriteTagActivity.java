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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.MCReader;
import de.syss.MifareClassicTool.R;

/**
 * Write data to tag. The user can choose to write
 * a single block of data or to write a dump to a Mifare
 * Classic tag providing its keys.
 * @author Gerhard Klostermeier
 */
public class WriteTagActivity extends BasicActivity {

    private static final int FC_WRITE_DUMP = 1;
    private static final int KMC_WRTIE_DUMP = 2;
    private static final int KMC_WRTIE_BLOCK = 3;

    private EditText mSectorText;
    private EditText mBlockText;
    private EditText mDataText;
    private HashMap<Integer, HashMap<Integer, byte[]>> mDumpWithPos;


    /**
     * Initialize the layout and some member0; variables.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_tag);

        mSectorText = (EditText) findViewById(R.id.editTextWriteTagSector);
        mBlockText = (EditText) findViewById(R.id.editTextWriteTagBlock);
        mDataText = (EditText) findViewById(R.id.editTextWriteTagData);
    }

    /**
     * Handle incoming results from {@link CreateKeyMapActivity} or
     * {@link FileChooserActivity}.
     * @see #writeBlock()
     * @see #checkTag()
     * @see #createKeyMapForDump(String)
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
        case FC_WRITE_DUMP:
            if (resultCode != Activity.RESULT_OK) {
                // Error.
            } else {
                // Create keys.
                createKeyMapForDump(data.getStringExtra(
                        FileChooserActivity.EXTRA_CHOSEN_FILE));
            }
            break;
        case KMC_WRTIE_DUMP:
            if (resultCode != Activity.RESULT_OK) {
                // Error.
                if (resultCode == 4) {
                    Toast.makeText(this, R.string.info_no_key_found,
                            Toast.LENGTH_LONG).show();
                }
            } else {
                checkTag();
            }
            break;
        case KMC_WRTIE_BLOCK:
            if (resultCode != Activity.RESULT_OK) {
                // Error.
                if (resultCode == 4) {
                    Toast.makeText(this, R.string.info_no_key_found,
                            Toast.LENGTH_LONG).show();
                }
            } else {
                // Write block.
                writeBlock();
            }
            break;
        }
    }

    /**
     * Check the user input and (if correct) show the
     * {@link CreateKeyMapActivity} with predefined mapping range
     * (see {@link #createKeyMapForBlock(int)}).
     * @param view The View object that triggered the method
     * (in this case the write block button).
     * @see CreateKeyMapActivity
     * @see #createKeyMapForBlock(int)
     */
    public void onWriteBlock(View view) {
        // Check input.
        if (mSectorText.getText().toString().equals("")
                || mBlockText.getText().toString().equals("")) {
            // Error, location not fully set.
            Toast.makeText(this, R.string.info_data_location_not_set,
                    Toast.LENGTH_LONG).show();
            return;
        }
        final int sector = Integer.parseInt(mSectorText.getText().toString());
        final int block = Integer.parseInt(mBlockText.getText().toString());
        if (sector > CreateKeyMapActivity.MAX_SECTOR_COUNT-1
                || sector < 0) {
            // Error, sector is out of range for any mifare tag.
            Toast.makeText(this, R.string.info_sector_out_of_range,
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (block > CreateKeyMapActivity.MAX_BLOCK_COUNT_PER_SECTOR-1
                || block < 0) {
            // Error, block is out of range for any mifare tag.
            Toast.makeText(this, R.string.info_block_out_of_range,
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (sector == 0 && block == 0) {
            // Error, read only (manuf.) block.
            Toast.makeText(this, R.string.info_manuf_block_not_writable,
                    Toast.LENGTH_LONG).show();
            return;
        }

        String data = mDataText.getText().toString();
        if (Common.isHexAnd16Byte(data, this) == false) {
            return;
        }
        if (block == 3 || block == 15) {
            // Warning, this is a sector trailer.
            new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_sector_trailer_warning_title)
            .setMessage(R.string.dialog_sector_trailer_warning)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(R.string.button_i_know_what_i_am_doing,
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // Show key map creator.
                    createKeyMapForBlock(sector);
                }
             })
             .setNegativeButton(R.string.button_cancel,
                     new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // Do nothing.
                }
             }).show();
        } else {
            createKeyMapForBlock(sector);
        }
    }

    /**
     * Helper function for {@link #onWriteBlock(View)} to show
     * the {@link CreateKeyMapActivity}.
     * @param sector The sector for the mapping range of
     * {@link CreateKeyMapActivity}
     * @see CreateKeyMapActivity
     * @see #onWriteBlock(View)
     */
    private void createKeyMapForBlock(int sector) {
        Intent intent = new Intent(this, CreateKeyMapActivity.class);
        intent.putExtra(CreateKeyMapActivity.EXTRA_KEYS_DIR,
                Environment.getExternalStoragePublicDirectory(Common.HOME_DIR)
                + Common.KEYS_DIR);
        intent.putExtra(CreateKeyMapActivity.EXTRA_SECTOR_CHOOSER, false);
        intent.putExtra(CreateKeyMapActivity.EXTRA_SECTOR_CHOOSER_FROM, sector);
        intent.putExtra(CreateKeyMapActivity.EXTRA_SECTOR_CHOOSER_TO, sector);
        intent.putExtra(CreateKeyMapActivity.EXTRA_BUTTON_TEXT,
                getString(R.string.button_create_key_map_and_write_block));
        startActivityForResult(intent, KMC_WRTIE_BLOCK);
    }

    /**
     * Called from {@link #onActivityResult(int, int, Intent)}
     * after a key map was created, this method tries to write the given
     * data to the tag. An error will be displayed to the user via Toast.
     * @see #onActivityResult(int, int, Intent)
     * @see #onWriteBlock(View)
     */
    private void writeBlock() {
        MCReader reader = Common.checkForTagAndCreateReader(this);
        if (reader == null) {
            return;
        }
        int sector = Integer.parseInt(mSectorText.getText().toString());
        int block = Integer.parseInt(mBlockText.getText().toString());
        byte[][] keys = Common.getKeyMap().get(sector);
        int result = -1;

        if (keys[1] != null) {
            result = reader.writeBlock(sector, block,
                    Common.hexStringToByteArray(mDataText.getText().toString()),
                    keys[1], true);
        }
        // Error while writing? Maybe tag has default factory settings ->
        // try to write with key a (if there is one).
        if (result == -1 && keys[0] != null) {
            result = reader.writeBlock(sector, block,
                    Common.hexStringToByteArray(mDataText.getText().toString()),
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
            Toast.makeText(this, R.string.info_error_writing_block,
                    Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, R.string.info_write_successful,
                Toast.LENGTH_LONG).show();
        finish();
    }

    /**
     * Open a file chooser ({@link FileChooserActivity}) and wait for its
     * result in {@link #onActivityResult(int, int, Intent)}.
     * This method triggers the call chain: open {@link FileChooserActivity}
     * (this method) -> open {@link CreateKeyMapActivity} (from
     * {@link #createKeyMapForDump(String)}) -> run {@link #checkTag()}
     * -> run {@link #writeDump(HashMap, SparseArray)}.
     * @param view The View object that triggered the method
     * (in this case the write full dump button).
     * @see FileChooserActivity
     * @see #onActivityResult(int, int, Intent)
     */
    public void onWriteDump(View view) {
        // Show file chooser (chose dump).
        Intent intent = new Intent(this, FileChooserActivity.class);
        intent.putExtra(FileChooserActivity.EXTRA_DIR,
                Environment.getExternalStoragePublicDirectory(
                        Common.HOME_DIR) + Common.DUMPS_DIR);
        intent.putExtra(FileChooserActivity.EXTRA_TITLE,
                getString(R.string.text_open_dump_title));
        intent.putExtra(FileChooserActivity.EXTRA_CHOOSER_TEXT,
                getString(R.string.text_choose_dump_to_write));
        intent.putExtra(FileChooserActivity.EXTRA_BUTTON_TEXT,
                getString(R.string.button_write_full_dump));
        startActivityForResult(intent, FC_WRITE_DUMP);
    }

    /**
     * Triggered by {@link #onActivityResult(int, int, Intent)} after the
     * dump was selected (by {@link FileChooserActivity}), this method
     * reads the dump (skipping all blocks with unknown data "-") and saves
     * the data including its position in {@link #mDumpWithPos}.
     * After reading the dump {@link CreateKeyMapActivity} is called to create
     * a key map for the present tag.
     * @param pathToDump path and filename of the dump
     * (selected by {@link FileChooserActivity}).
     */
    private void createKeyMapForDump(String pathToDump) {
        // Read dump.
        File file = new File(pathToDump);
        String[] dump = Common.readFileLineByLine(file, false);
        mDumpWithPos = new HashMap<Integer, HashMap<Integer,byte[]>>();
        int sector = 0;
        int block = 0;
        // Transform the simple dump array into a structure (mDumpWithPos)
        // where the sector and block information are known additionally.
        // Blocks containing unknown data ("-") are dropped.
        for (String line : dump) {
            if (line.startsWith("+")) {
                String[] tmp = line.split(": ");
                sector = Integer.parseInt(tmp[tmp.length-1]);
                block = 0;
                mDumpWithPos.put(sector, new HashMap<Integer, byte[]>());
            } else if (!line.contains("-")) {
                mDumpWithPos.get(sector).put(block++,
                        Common.hexStringToByteArray(line));
            } else {
                block++;
            }
        }
        // Show key map creator.
        Intent intent = new Intent(this, CreateKeyMapActivity.class);
        intent.putExtra(CreateKeyMapActivity.EXTRA_KEYS_DIR,
                Environment.getExternalStoragePublicDirectory(Common.HOME_DIR)
                + Common.KEYS_DIR);
        intent.putExtra(CreateKeyMapActivity.EXTRA_SECTOR_CHOOSER, false);
        intent.putExtra(CreateKeyMapActivity.EXTRA_SECTOR_CHOOSER_FROM,
                (int) Collections.min(mDumpWithPos.keySet()));
        intent.putExtra(CreateKeyMapActivity.EXTRA_SECTOR_CHOOSER_TO,
                (int) Collections.max(mDumpWithPos.keySet()));
        intent.putExtra(CreateKeyMapActivity.EXTRA_BUTTON_TEXT,
                getString(R.string.button_create_key_map_and_write_dump));
        startActivityForResult(intent, KMC_WRTIE_DUMP);
    }

    /**
     * Check if the tag is suitable for the selected dump.
     * This is done in three steps. The first check determines if the dump
     * fits on the tag (size check). The second check determines if the keys for
     * relevant sectors are known (key check). At last this method will check
     * whether the keys with write privileges are known and if some blocks
     * are read-only (write check).<br />
     * If some of these checks "fail", the user will get a report dialog
     * with the two options to cancel the whole write process or to
     * write as much as possible(call {@link #writeDump(HashMap,
     * SparseArray)}).
     * @see MCReader#isWritableOnPositions(HashMap, SparseArray)
     * @see Common#getOperationInfoForBlock(byte, byte,
     * byte, de.syss.MifareClassicTool.Common.Operations, boolean, boolean)
     * @see #writeDump(HashMap, SparseArray)
     */
    private void checkTag() {
        // Create reader.
        MCReader reader = Common.checkForTagAndCreateReader(this);
        if (reader == null) {
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
        // Reformat for reader.isWritabeOnPosition(...).
        final SparseArray<byte[][]> keyMap  =
                Common.getKeyMap();
        HashMap<Integer, int[]> dataPos =
                new HashMap<Integer, int[]>(mDumpWithPos.size());
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
            Toast.makeText(this, R.string.info_check_ac_error,
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Skip dialog:
        // Build a dialog showing all sectors and blocks containing data
        // that can not be overwritten with the reason why they are not
        // writable. The user can chose to skip all these blocks/sectors
        // or to cancel the whole write procedure.
        List<HashMap<String, String>> list = new
                ArrayList<HashMap<String, String>>();
        final HashMap<Integer, HashMap<Integer, Integer>> writeOnPosSafe =
                new HashMap<Integer, HashMap<Integer,Integer>>(
                        writeOnPos.size());
        // Keys that are missing completely (mDumpWithPos vs. keyMap).
        for (int sector : mDumpWithPos.keySet()) {
            if (keyMap.indexOfKey(sector) < 0) {
                // Problem. Keys for sector not found.
                addToList(list, getString(R.string.text_sector) + ": " + sector,
                        getString(R.string.text_keys_not_known));
                writeOnPosSafe.remove(sector);
            }
        }
        // Keys with write privileges that are missing or some
        // blocks (block-parts) are read-only (writeOnPos vs. keyMap).
        Set<Integer> sectors = writeOnPos.keySet();
        for (int sector : sectors) {
            if (writeOnPos.get(sector) == null) {
                // Error. Sector is dead (IO Error).
                addToList(list, getString(R.string.text_sector) + ": " + sector,
                        getString(R.string.text_sector_dead));
                continue;
            }
            writeOnPosSafe.put(sector, new HashMap<Integer, Integer>());
            byte[][] keys = keyMap.get(sector);
            Set<Integer> blocks = writeOnPos.get(sector).keySet();
            for (int block : blocks) {
                boolean isSafeForWriting = true;
                String position = getString(R.string.text_sector) + ": "
                        + sector + ", " + getString(R.string.text_block)
                        + ": " + block;
                int writeInfo = writeOnPos.get(sector).get(block);
                switch (writeInfo) {
                case 0:
                    if (!(sector == 0 && block == 0)) {
                        // Problem. Block is read-only.
                        addToList(list, position, getString(
                                R.string.text_block_read_only));
                    }
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
                    writeOnPosSafe.get(sector).put(
                            block, (keys[0] != null) ? 1 : 2);
                    isSafeForWriting = false; // Already added.
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
                    writeOnPosSafe.get(sector).put(block, writeInfo);
                }
            }
        }

        // Show skip/cancel dialog (if needed).
        if (list.size() != 0) {
            // If the user skips all sectors/blocks that are not writable,
            // the writeTag() method will be called.
            LinearLayout ll = new LinearLayout(this);
            ll.setPadding(10, 10, 10, 10);
            ll.setGravity(Gravity.CENTER);
            ll.setOrientation(LinearLayout.VERTICAL);
            TextView textView = new TextView(this);
            textView.setText(getString(R.string.dialog_not_writable));
            textView.setTextAppearance(this,
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
                .setTitle(R.string.dialog_not_writable_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setView(ll)
                .setPositiveButton(R.string.button_skip_blocks,
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Skip not writable blocks and start writing.
                        writeDump(writeOnPosSafe, keyMap);
                    }
                })
                .setNegativeButton(R.string.button_cancel_all,
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing.
                    }
                })
                .show();
        } else {
            // Write.
            writeDump(writeOnPosSafe, keyMap);
        }
    }

    /**
     * A helper function for {@link #checkTag()} adding an item to
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
        HashMap<String,String> item = new HashMap<String,String>();
        item.put( "position", position);
        item.put( "reason", reason);
        list.add(item);
    }

    /**
     * This method is triggered by {@link #checkTag()} and writes a dump
     * to a tag.
     * @param writeOnPos A map within a map (all with type = Integer).
     * The key of the outer map is the sector number and the value is another
     * map with key = block number and value = write information. The write
     * information must be filtered (by {@link #checkTag()}) return values
     * of {@link MCReader#isWritableOnPositions(HashMap, SparseArray)}.<br />
     * Attention: This method does not any checking. The position and write
     * information must be checked by {@link #checkTag()}.
     * @param keyMap A key map generated by {@link CreateKeyMapActivity}.
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
        ll.setPadding(20, 20, 20, 20);
        ll.setGravity(Gravity.CENTER);
        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        progressBar.setPadding(0, 0, 10, 0);
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
        final Handler handler = new Handler();
        new Thread(new Runnable() {
            public void run() {
                // Write dump to tag.
                for (int sector : writeOnPos.keySet()) {
                    byte[][] keys = keyMap.get(sector);
                    for (int block : writeOnPos.get(sector).keySet()) {
                        // Skip sector 0, block 0 (always read-only).
                        if (sector == 0 && block == 0) {
                            continue;
                        }
                        // Select key with write privileges.
                        byte writeKey[] = null;
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
                            handler.post(new Runnable() {
                                public void run() {
                                    Toast.makeText(a,
                                            R.string.info_write_error,
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                            reader.close();
                            warning.cancel();
                            return;
                        }
                    }
                }
                // Finished writing.
                reader.close();
                warning.cancel();
                handler.post(new Runnable() {
                    public void run() {
                        Toast.makeText(a, R.string.info_write_successful,
                                Toast.LENGTH_LONG).show();
                    }
                });
                a.finish();
            }
        }).start();
    }
}
