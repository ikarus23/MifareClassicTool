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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.tech.MifareClassic;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.MCReader;
import de.syss.MifareClassicTool.MCTApp;
import de.syss.MifareClassicTool.R;

/**
 * Write data to tag. The user can choose to write
 * a single block of data or to write a full dump to an empty Mifare
 * Classic tag.
 * @author Gerhard Klostermeier
 */
public class WriteTagActivity extends Activity {

    private static final int FILE_CHOOSER = 1;
    private static final int KEY_MAP_CREATOR = 2;

    private EditText mSectorText;
    private EditText mBlockText;
    private EditText mDataText;


    /**
     * Initialize the layout and some member variables.
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

    /**
     * Handle incoming results from {@link CreateKeyMapActivity} or
     * {@link FileChooserActivity}. If there was an error, display informations.
     * If there was no error call {@link #writeBlock()} or
     * {@link #writeFullDump(String)}.
     * @see #writeBlock()
     * @see #writeFullDump(String)
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
        case FILE_CHOOSER:
            if (resultCode != Activity.RESULT_OK) {
                // Error.

            } else {
                // Write full dump.
                String fileName = data.getStringExtra(
                        FileChooserActivity.EXTRA_CHOSEN_FILE);
                writeFullDump(fileName);
            }
            break;
        case KEY_MAP_CREATOR:
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
     * (see {@link #showKeyMapCreator(int)}).
     * @param view The View object that triggered the method
     * (in this case the write block button).
     * @see CreateKeyMapActivity
     * @see #showKeyMapCreator(int)
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
        if (data.matches("[0-9A-Fa-f]+") == false) {
            // Error, not hex.
            Toast.makeText(this, R.string.info_not_hex_data,
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (data.length() != 32) {
            // Error, not 16 byte (32 chars).
            Toast.makeText(this, R.string.info_not_16_byte,
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (block == 3 || block == 15) {
            // Warning, this is a sector trailer.
            new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_sector_trailer_warning_title)
            .setMessage(R.string.dialog_sector_trailer_warning)
            .setPositiveButton(R.string.button_i_know_what_i_am_doing,
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // Show key map creator.
                    showKeyMapCreator(sector);
                }
             })
             .setNegativeButton(R.string.button_cancel,
                     new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // Do nothing.
                }
             }).show();
        } else {
            showKeyMapCreator(sector);
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
    private void showKeyMapCreator(int sector) {
        Intent intent = new Intent(this, CreateKeyMapActivity.class);
        intent.putExtra(CreateKeyMapActivity.EXTRA_KEYS_DIR,
                Environment.getExternalStoragePublicDirectory(Common.HOME_DIR)
                + Common.KEYS_DIR);
        intent.putExtra(CreateKeyMapActivity.EXTRA_SECTOR_CHOOSER, false);
        intent.putExtra(CreateKeyMapActivity.EXTRA_SECTOR_CHOOSER_FROM, sector);
        intent.putExtra(CreateKeyMapActivity.EXTRA_SECTOR_CHOOSER_TO, sector);
        intent.putExtra(CreateKeyMapActivity.EXTRA_BUTTON_TEXT,
                getString(R.string.button_create_key_map_and_write_block));
        startActivityForResult(intent, KEY_MAP_CREATOR);
    }

    /**
     * Open a file chooser ({@link FileChooserActivity}).
     * @param view The View object that triggered the method
     * (in this case the write full dump button).
     * @see FileChooserActivity
     */
    public void onWriteFullDump(View view) {
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
        startActivityForResult(intent, FILE_CHOOSER);
    }

    /**
     * Called from {@link #onActivityResult(int, int, Intent)}
     * after a key map was created, this method tries to write the given
     * data to the tag. An error will be displayed to the user via Toast.
     * @see #onActivityResult(int, int, Intent)
     * @see #onWriteBlock(View)
     */
    private void writeBlock() {
        MCReader reader = new MCReader(Common.getTag());
        reader.connect();
        if (!reader.isConnected()) {
            return;
        }
        int sector = Integer.parseInt(mSectorText.getText().toString());
        int block = Integer.parseInt(mBlockText.getText().toString());
        byte[][] keys = ((MCTApp)getApplication()).getKeyMap().get(sector);
        int result = -1;

        if (keys[1] != null) {
            result = reader.writeBlock(sector, block,
                    Common.hexStringToByteArray(mDataText.getText().toString()),
                    keys[1], true);
        }
        if ((result == 5 || result == -1) && keys[0] != null) {
            // Error while writing. Maybe tag has default factory settings ->
            // try to write with key a (if there is one).
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
        case 5:
            Toast.makeText(this, R.string.info_error_writing_block,
                    Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, R.string.info_write_successful,
                Toast.LENGTH_LONG).show();
        finish();
    }

    /**
     * Write the given dump file to the present Mifare Classic tag,
     * if the size (dump<->tag) matches and the tag is clean
     * (see {@link MCReader#isCleanTag()}).
     * @param fileName The filename (with path) of the dump.
     * @see MCReader#isCleanTag()
     */
    private void writeFullDump(String fileName) {
        if (Common.getTag() == null) {
            // Error. There is no tag.
            Toast.makeText(this, R.string.info_no_tag_found,
                    Toast.LENGTH_LONG).show();
            return;
        }
        final MCReader reader = new MCReader(Common.getTag());
        reader.connect();
        if (!reader.isConnected()) {
            // Error. The tag is gone.
            Toast.makeText(this, R.string.info_no_tag_found,
                    Toast.LENGTH_LONG).show();
            reader.close();
            return;
        }
        if (!reader.isCleanTag()) {
            // Error. Not a clean tag.
            Toast.makeText(this, R.string.info_tag_not_clean,
                    Toast.LENGTH_LONG).show();
            reader.close();
            return;
        }
        // Read dump.
        File file = new File(fileName);
        final String[] lines = Common.readFileLineByLine(file, false);

        // Check if dump is correct for tag.
        boolean err = false;
        if (lines.length / (double)5 > 32) {
            // Must be a Mifare Classic 4k tag.
            if (lines.length != 296) {
                err = true;
            }
        } else {
            if ((lines.length / (double)5)*8*8 != reader.getSize()) {
               err = true;
            }
        }
        if (err == true) {
            // Error. Dump size does not match the tag size.
            Toast.makeText(this, R.string.info_tag_dump_size_not_match,
                    Toast.LENGTH_LONG).show();
            reader.close();
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
        tv.setText(getString(R.string.text_wait_write_tag));
        tv.setTextSize(18);
        ll.addView(progressBar);
        ll.addView(tv);
        final AlertDialog warning = new AlertDialog.Builder(this)
        .setView(ll)
        .create();
        warning.show();

        // Start writing in new thread.
        final Activity a = this;
        final Handler handler = new Handler();
        new Thread(new Runnable() {
            public void run() {
                // Write dump to tag.
                int sector = 0;
                int block = 0;
                for (String line : lines) {
                    if (line.startsWith("+")) {
                        String[] tmp = line.split(": ");
                        sector = Integer.parseInt(tmp[tmp.length-1]);
                        block = 0;
                    } else {
                        if (!(sector == 0 && block == 0)) {
                            // Write.
                            if (reader.writeBlock(sector, block,
                                    Common.hexStringToByteArray(line),
                                    MifareClassic.KEY_DEFAULT, false) != 0) {
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
                        block++;
                    }
                }

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
