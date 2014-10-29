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

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.R;

/**
 * A tool to show the difference between two dumps.
 * @author Gerhard Klostermeier
 */
public class DiffTool extends BasicActivity {

    /**
     * The corresponding Intent will contain a dump. Each field of the
     * String Array is one line of the dump. Headers (e.g. "Sector 01")
     * are marked with a "+"-symbol (e.g. "+Sector 01").
     */
    public final static String EXTRA_DUMP_1 =
            "de.syss.MifareClassicTool.Activity.DUMP_1";
    /**
     * The corresponding Intent will contain a dump. Each field of the
     * String Array is one line of the dump. Headers (e.g. "Sector 01")
     * are marked with a "+"-symbol (e.g. "+Sector 01").
     */
    public final static String EXTRA_DUMP_2 =
            "de.syss.MifareClassicTool.Activity.DUMP_2";

    private final static int FILE_CHOOSER_DUMP_FILE_1 = 1;
    private final static int FILE_CHOOSER_DUMP_FILE_2 = 2;

    private Button mDumpFileButton1;
    private Button mDumpFileButton2;
    private String[] mDump1;
    private String[] mDump2;

    /**
     * Process {@link #EXTRA_DUMP_1} and {@link #EXTRA_DUMP_2} if they are
     * part of the Intent and initialize some member variables.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diff_tool);

        mDumpFileButton1 = (Button) findViewById(R.id.buttonDiffToolDump1);
        mDumpFileButton2 = (Button) findViewById(R.id.buttonDiffToolDump2);

        // Check if one or both dumps are already chosen via Intent
        // (from DumpEitor).
        if (getIntent().hasExtra(EXTRA_DUMP_1)) {
            mDump1 = getIntent().getStringArrayExtra(EXTRA_DUMP_1);
            mDumpFileButton1.setText(R.string.text_dump_from_editor);
            mDumpFileButton1.setEnabled(false);
        }
        if (getIntent().hasExtra(EXTRA_DUMP_2)) {
            mDump2 = getIntent().getStringArrayExtra(EXTRA_DUMP_2);
            mDumpFileButton2.setText(R.string.text_dump_from_editor);
            mDumpFileButton2.setEnabled(false);
        }
        runDiff();
    }

    /**
     * Handle the {@link FileChooser} results from {@link #onChooseDump1(View)}
     * and {@link #onChooseDump2(View)} by reading and checking the dump.
     * Then {@link #runDiff()} will be called.
     * @see FileChooser
     * @see #runDiff()
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
        case FILE_CHOOSER_DUMP_FILE_1:
            if (resultCode == Activity.RESULT_OK) {
                // Dump 1 has been chosen.
                String path = data.getStringExtra(
                        FileChooser.EXTRA_CHOSEN_FILE);
                File file = new File(path);
                mDumpFileButton1.setText(file.getName());
                mDump1 = Common.readFileLineByLine(file, false, this);
                int err = Common.isValidDump(mDump1, false);
                if (err != 0) {
                    Common.isValidDumpErrorToast(err, this);
                    mDump1 = null;
                    return;
                } else {
                    runDiff();
                }
            }
            break;
        case FILE_CHOOSER_DUMP_FILE_2:
            if (resultCode == Activity.RESULT_OK) {
                // Dump 2 has been chosen.
                String path = data.getStringExtra(
                        FileChooser.EXTRA_CHOSEN_FILE);
                File file = new File(path);
                mDumpFileButton2.setText(file.getName());
                mDump2 = Common.readFileLineByLine(file, false, this);
                int err = Common.isValidDump(mDump2, false);
                if (err != 0) {
                    Common.isValidDumpErrorToast(err, this);
                    mDump2 = null;
                    return;
                } else {
                    runDiff();
                }
            }
            break;
        }
    }

    /**
     * TODO: doc.
     */
    private void runDiff() {
        // Check if both dumps are there.
        if (mDump1 != null && mDump2 != null) {
            // TODO: implement.
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
     * Create an Intent that will open the {@link FileChooser} and
     * let the user select a dump file.
     * This is a helper function for {@link #onChooseDump1(View)}
     * and {@link #onChooseDump2(View)}.
     * @return An Intent for opening the {@link FileChooser}.
     */
    private Intent prepareFileChooserForDump() {
        Intent intent = new Intent(this, FileChooser.class);
        intent.putExtra(FileChooser.EXTRA_DIR,
                Environment.getExternalStoragePublicDirectory(
                        Common.HOME_DIR) + "/" + Common.DUMPS_DIR);
        intent.putExtra(FileChooser.EXTRA_TITLE,
                getString(R.string.text_open_dump_title));
        intent.putExtra(FileChooser.EXTRA_BUTTON_TEXT,
                getString(R.string.action_open_dump_file));
        intent.putExtra(FileChooser.EXTRA_ENABLE_DELETE_FILE, true);
        return intent;
    }
}
