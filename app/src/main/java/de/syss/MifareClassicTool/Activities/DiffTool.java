/*
 * Copyright 2014 Gerhard Klostermeier
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
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.widget.TextViewCompat;

import java.io.File;

import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.MCDiffUtils;
import de.syss.MifareClassicTool.R;

/**
 * A tool to show the difference between two dumps.
 * @author Gerhard Klostermeier
 */
public class DiffTool extends BasicActivity {

    /**
     * The corresponding Intent will contain a dump. Each field of the
     * String Array is one line of the dump. Headers (e.g. "Sector:1")
     * are marked with a "+"-symbol (e.g. "+Sector: 1").
     */
    public final static String EXTRA_DUMP =
            "de.syss.MifareClassicTool.Activity.DUMP";

    private final static int FILE_CHOOSER_DUMP_FILE_1 = 1;
    private final static int FILE_CHOOSER_DUMP_FILE_2 = 2;

    private LinearLayout mDiffContent;
    private Button mDumpFileButton1;
    private Button mDumpFileButton2;
    private SparseArray<String[]> mDump1;
    private SparseArray<String[]> mDump2;

    /**
     * Process {@link #EXTRA_DUMP} if they are part of the Intent and
     * initialize some member variables.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diff_tool);

        mDiffContent = findViewById(R.id.linearLayoutDiffTool);
        mDumpFileButton1 = findViewById(R.id.buttonDiffToolDump1);
        mDumpFileButton2 = findViewById(R.id.buttonDiffToolDump2);

        // Check if one or both dumps are already chosen via Intent
        // (from DumpEditor).
        if (getIntent().hasExtra(EXTRA_DUMP)) {
            mDump1 = convertDumpFormat(
                    getIntent().getStringArrayExtra(EXTRA_DUMP));
            mDumpFileButton1.setText(R.string.text_dump_from_editor);
            mDumpFileButton1.setEnabled(false);
            onChooseDump2(null);
        }
        runDiff();
    }

    /**
     * Handle the {@link FileChooser} results from {@link #onChooseDump1(View)}
     * and {@link #onChooseDump2(View)} by calling
     * {@link #processChosenDump(Intent)} and updating the UI and member vars.
     * Then {@link #runDiff()} will be called.
     * @see FileChooser
     * @see #runDiff()
     * @see #processChosenDump(Intent)
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
        case FILE_CHOOSER_DUMP_FILE_1:
            if (resultCode == Activity.RESULT_OK) {
                // Dump 1 has been chosen.
                String fileName = data.getStringExtra(
                        FileChooser.EXTRA_CHOSEN_FILENAME);
                mDumpFileButton1.setText(fileName);
                mDump1 = processChosenDump(data);
                runDiff();
            }
            break;
        case FILE_CHOOSER_DUMP_FILE_2:
            if (resultCode == Activity.RESULT_OK) {
                // Dump 2 has been chosen.
                String fileName = data.getStringExtra(
                        FileChooser.EXTRA_CHOSEN_FILENAME);
                mDumpFileButton2.setText(fileName);
                mDump2 = processChosenDump(data);
                runDiff();
            }
            break;
        }
    }

    /**
     * Run diff if there are two dumps and show the result in the GUI.
     * @see MCDiffUtils#diffIndices(SparseArray, SparseArray)
     */
    @SuppressLint("SetTextI18n")
    private void runDiff() {
        // Check if both dumps are there.
        if (mDump1 != null && mDump2 != null) {
            mDiffContent.removeAllViews();
            SparseArray<Integer[][]> diff = MCDiffUtils.diffIndices(
                    mDump1, mDump2);

            // Walk trough all possible sectors (this way the right
            // order will be guaranteed).
            for (int sector = 0; sector < 40; sector++) {
                Integer[][] blocks = diff.get(sector);
                if (blocks == null) {
                    // No such sector.
                    continue;
                }

                // Add sector header.
                TextView header = new TextView(this);
                TextViewCompat.setTextAppearance(header,
                        android.R.style.TextAppearance_Medium);
                header.setPadding(0, Common.dpToPx(20), 0, 0);
                header.setTextColor(Color.WHITE);
                header.setText(getString(R.string.text_sector) + ": " + sector);
                mDiffContent.addView(header);

                if (blocks.length == 0 || blocks.length == 1) {
                    TextView tv = new TextView(this);
                    if (blocks.length == 0) {
                        // Sector exists only in dump1.
                        tv.setText(getString(R.string.text_only_in_dump1));
                    } else {
                        // Sector exists only in dump2.
                        tv.setText(getString(R.string.text_only_in_dump2));
                    }
                    mDiffContent.addView(tv);
                    continue;
                }

                // Walk through all blocks.
                for (int block = 0; block < blocks.length; block++) {
                    // Initialize diff entry.
                    RelativeLayout rl = (RelativeLayout)
                            getLayoutInflater().inflate(
                                    R.layout.list_item_diff_block,
                                    findViewById(
                                            android.R.id.content), false);
                    TextView dump1 = rl.findViewById(
                            R.id.textViewDiffBlockDump1);
                    TextView dump2 = rl.findViewById(
                            R.id.textViewDiffBlockDump2);
                    TextView diffIndex = rl.findViewById(
                            R.id.textViewDiffBlockDiff);

                    // This is a (ugly) fix for a bug in Android 5.0+
                    // https://code.google.com/p/android-developer-preview
                    //    /issues/detail?id=110
                    // (All three TextViews have the monospace typeface
                    // property set via XML. But Android ignores it...)
                    dump1.setTypeface(Typeface.MONOSPACE);
                    dump2.setTypeface(Typeface.MONOSPACE);
                    diffIndex.setTypeface(Typeface.MONOSPACE);

                    StringBuilder diffString;
                    diffIndex.setTextColor(Color.RED);
                    // Populate the blocks of the diff entry.
                    dump1.setText(mDump1.get(sector)[block]);
                    dump2.setText(mDump2.get(sector)[block]);

                    if (blocks[block].length == 0) {
                        // Set diff line for identical blocks.
                        diffIndex.setTextColor(Color.GREEN);
                        diffString = new StringBuilder(
                                getString(R.string.text_identical_data));
                    } else {
                        diffString = new StringBuilder(
                                "                                ");
                        // Walk through all symbols to populate the diff line.
                        for (int i : blocks[block]) {
                            diffString.setCharAt(i, 'X');

                        }
                    }
                    // Add diff entry.
                    diffIndex.setText(diffString);
                    mDiffContent.addView(rl);
                }
            }
        }
    }

    /**
     * Open {@link FileChooser} to select the first dump.
     * @param view The View object that triggered the function
     * (in this case the choose a dump button for dump 1).
     * @see #prepareFileChooserForDump()
     */
    public void onChooseDump1(View view) {
        Intent intent = prepareFileChooserForDump();
        startActivityForResult(intent, FILE_CHOOSER_DUMP_FILE_1);
    }

    /**
     * Open {@link FileChooser} to select the second dump.
     * @param view The View object that triggered the function
     * (in this case the choose a dump button for dump 2).
     * @see #prepareFileChooserForDump()
     */
    public void onChooseDump2(View view) {
        Intent intent = prepareFileChooserForDump();
        startActivityForResult(intent, FILE_CHOOSER_DUMP_FILE_2);
    }

    /**
     * Get the {@link FileChooser#EXTRA_CHOSEN_FILE} from the Intend,
     * read the file, check it for errors using
     * {@link Common#isValidDump(String[], boolean)} and convert its format
     * using {@link #convertDumpFormat(String[])}.
     * This is a helper function for
     * {@link #onActivityResult(int, int, Intent)}.
     * @param data The Intent returned by the {@link FileChooser}
     * @return The chosen dump in a key value pair format. The key is the sector
     * number. The value is an String array. Each field of the array
     * represents a block. If the dump was not valid null will be returned.
     * @see Common#isValidDump(String[], boolean)
     * @see Common#isValidDumpErrorToast(int, android.content.Context)
     * @see Common#readFileLineByLine(File, boolean, android.content.Context)
     * @see #convertDumpFormat(String[])
     */
    private SparseArray<String[]> processChosenDump(Intent data) {
        String path = data.getStringExtra(
                FileChooser.EXTRA_CHOSEN_FILE);
        File file = new File(path);
        String[] dump = Common.readFileLineByLine(file, false, this);
        int err = Common.isValidDump(dump, false);
        if (err != 0) {
            Common.isValidDumpErrorToast(err, this);
            return null;
        } else {
            return convertDumpFormat(dump);
        }
    }

    /**
     * Create an Intent that will open the {@link FileChooser} and
     * let the user select a dump file.
     * This is a helper function for {@link #onChooseDump1(View)}
     * and {@link #onChooseDump2(View)}.
     * @return An Intent for opening the {@link FileChooser}.
     */
    private Intent prepareFileChooserForDump() {
        Intent intent = new Intent(this, FileChooser.class);
        intent.putExtra(FileChooser.EXTRA_DIR,
                Common.getFile(Common.DUMPS_DIR).getAbsolutePath());
        intent.putExtra(FileChooser.EXTRA_TITLE,
                getString(R.string.text_open_dump_title));
        intent.putExtra(FileChooser.EXTRA_BUTTON_TEXT,
                getString(R.string.action_open_dump_file));
        return intent;
    }

    /**
     * Convert the format of an dump.
     * @param dump A dump in the same format a dump file is.
     * (with no comments, not multiple dumps (appended) and validated by
     * {@link Common#isValidDump(String[], boolean)})
     * @return The dump in a key value pair format. The key is the sector
     * number. The value is an String array. Each field of the array
     * represents a block.
     */
    private static SparseArray<String[]> convertDumpFormat(String[] dump) {
        SparseArray<String[]> ret = new SparseArray<>();
        int i = 0;
        int sector = 0;
        for (String line : dump) {
            if (line.startsWith("+")) {
                String[] tmp = line.split(": ");
                sector = Integer.parseInt(tmp[tmp.length-1]);
                i = 0;
                if (sector < 32) {
                    ret.put(sector, new String[4]);
                } else {
                    ret.put(sector, new String[16]);
                }
            } else {
                ret.get(sector)[i++] = line;
            }
        }
        return ret;
    }
}
