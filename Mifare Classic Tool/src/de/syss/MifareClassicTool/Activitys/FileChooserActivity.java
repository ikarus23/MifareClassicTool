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
import java.io.IOException;
import java.util.Arrays;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.R;

/**
 * A simple generic file chooser that lets the user choose a file from
 * a given directory.
 * This Activity should be called via startActivityForResult() with
 * an Intent containing the {@link #EXTRA_DIR}.
 * The result codes are:
 * <ul>
 * <li>{@link Activity#RESULT_OK} - Everything is O.K. The chosen file will be
 * in the Intent ({@link #EXTRA_CHOSEN_FILE}).</li>
 * <li>1 - Directory from {@link #EXTRA_DIR} does not
 * exist.</li>
 * <li>2 - No directory specified in Intent
 * ({@link #EXTRA_DIR})</li>
 * <li>3 - External Storage is not read/writable. This error is
 * displayed to the user via Toast.</li>
 * </ul>
 * @author Gerhard Klostermeier
 */
public class FileChooserActivity extends Activity {

    // Input parameters.
    /**
     * Path to a directory with files. The files in the directory
     * are the files the user can choose from. This must be in the Intent.
     */
    public final static String EXTRA_DIR =
            "de.syss.MifareClassicTool.Activity.DIR";
    /**
     * The title of the activity. Optional.
     * e.g. "Open Dump File"
     */
    public final static String EXTRA_TITLE =
            "de.syss.MifareClassicTool.Activity.TITLE";
    /**
     * The small text above the files. Optional.
     * e.g. "Please choose a file:
     */
    public final static String EXTRA_CHOOSER_TEXT =
            "de.syss.MifareClassicTool.Activity.CHOOSER_TEXT";
    /**
     * The text of the choose button. Optional.
     * e.g. "Open File"
     */
    public final static String EXTRA_BUTTON_TEXT =
            "de.syss.MifareClassicTool.Activity.BUTTON_TEXT";
    /**
     * Enable/Disable the button that allows the user to create a new file.
     * Optional. Boolean value. Disabled (false) by default.
     */
    public final static String EXTRA_ENABLE_NEW_FILE_BUTTON =
            "de.syss.MifareClassicTool.Activity.ENABLE_NEW_FILE_BUTTON";


    // Output parameter.
    /**
     * The file (with full path) that will be passed via Intent
     * to onActivityResult() method. The result code will be
     * {@link Activity#RESULT_OK}.
     */
    public final static String EXTRA_CHOSEN_FILE =
            "de.syss.MifareClassicTool.Activity.CHOSEN_FILE";



    private static final String LOG_TAG =
            FileChooserActivity.class.getSimpleName();
    private RadioGroup mGroupOfFiles;
    private Button mNewFile;
    private File mDir;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_chooser);
        mGroupOfFiles = (RadioGroup) findViewById(R.id.radioGroupFileChooser);
        mNewFile = (Button) findViewById(R.id.buttonFileChooserNewFile);
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
     * Initialize the file chooser with the data from the calling Intent
     * (if external storage is mounted).
     * @see #EXTRA_DIR
     * @see #EXTRA_TITLE
     * @see #EXTRA_CHOOSER_TEXT
     * @see #EXTRA_BUTTON_TEXT
     */
    @Override
    public void onStart() {
        super.onStart();

        if (!Common.isExternalStorageWritableErrorToast(this)) {
            setResult(3);
            finish();
        }
        TextView chooserText = (TextView) findViewById(
                R.id.textViewFileChooser);
        Button chooserButton = (Button) findViewById(
                R.id.buttonFileChooserChoose);
        Intent intent = getIntent();

        // Set title.
        if (intent.hasExtra(EXTRA_TITLE)) {
            setTitle(intent.getStringExtra(EXTRA_TITLE));
        }
        // Set chooser text.
        if (intent.hasExtra(EXTRA_CHOOSER_TEXT)) {
            chooserText.setText(intent.getStringExtra(EXTRA_CHOOSER_TEXT));
        }
        // Set button text.
        if (intent.hasExtra(EXTRA_BUTTON_TEXT)) {
            chooserButton.setText(intent.getStringExtra(EXTRA_BUTTON_TEXT));
        }
        // Enable/Disable new file button.
        if (intent.hasExtra(EXTRA_ENABLE_NEW_FILE_BUTTON)) {
            mNewFile.setEnabled(intent.getBooleanExtra(
                        EXTRA_ENABLE_NEW_FILE_BUTTON, false));
        }

        // Init. files. If there are no files disable chooser button.
        if (intent.hasExtra(EXTRA_DIR)) {
            File path = new File(intent.getStringExtra(EXTRA_DIR));
            if (path.exists()) {
                File[] files = path.listFiles();
                Arrays.sort(files);
                mGroupOfFiles.removeAllViews();
                // Refresh file list.
                if (files.length > 0) {
                    for(File f : files) {
                        RadioButton r = new RadioButton(this);
                        r.setText(f.getName());
                        mGroupOfFiles.addView(r);
                    }
                    // Check first file.
                    ((RadioButton)mGroupOfFiles.getChildAt(0)).setChecked(true);
                    mDir = path;
                    chooserButton.setEnabled(true);
                } else {
                    // No files in directory.
                    chooserButton.setEnabled(false);
                    chooserText.setText(chooserText.getText()
                            + "\n   --- "
                            + getString(R.string.text_no_files_in_chooser)
                            + " ---");
                }
            } else {
                // Path does not exist.
                Log.e(LOG_TAG, "Directory for FileChooser does not exist.");
                setResult(1);
                finish();
            }
        } else {
            Log.d(LOG_TAG, "Directory for FileChooser was not in intent.");
            setResult(2);
            finish();
        }
    }

    /**
     * Finish the Activity with an Intent containing
     * {@link #EXTRA_CHOSEN_FILE} as result.
     * You can catch that result by overriding onActivityResult() in the
     * Activity that called the file chooser via startActitivtyForResult().
     * @param view The View object that triggered the function
     * (in this case the choose file button).
     * @see #EXTRA_CHOSEN_FILE
     */
    public void onFileChosen(View view) {
        RadioButton selected = (RadioButton) findViewById(
                mGroupOfFiles.getCheckedRadioButtonId());
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CHOSEN_FILE, new File(mDir.getPath(),
                selected.getText().toString()).getPath());
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    /**
     * Ask the user for a file name, create this file and choose it
     * ({@link #onFileChosen(View)}).
     * @param view The View object that triggered the function
     * (in this case the new file button).
     */
    public void onNewFile(View view) {
        final Context cont = this;
        // Ask user for filename.
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setLines(1);
        input.setHorizontallyScrolling(true);
        new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_new_file_title)
            .setMessage(R.string.dialog_new_file)
            .setView(input)
            .setPositiveButton(R.string.button_ok,
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    if (input.getText() != null
                            && !input.getText().toString().equals("")) {
                        File file = new File(mDir.getPath(),
                                input.getText().toString());
                        boolean createdSuccessfully;
                        try {
                            createdSuccessfully = file.createNewFile();
                        } catch (IOException e) {
                            createdSuccessfully = false;
                        }
                        if (createdSuccessfully) {
                            Intent intent = new Intent();
                            intent.putExtra(EXTRA_CHOSEN_FILE, file.getPath());
                            setResult(Activity.RESULT_OK, intent);
                            finish();
                        } else {
                            Toast.makeText(cont,
                                    R.string.info_new_file_error,
                                    Toast.LENGTH_LONG).show();
                        }
                    } else {
                        // Empty name is not allowed.
                        Toast.makeText(cont, R.string.info_empty_file_name,
                                Toast.LENGTH_LONG).show();
                    }
                }
            })
            .setNegativeButton(R.string.button_cancel,
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Do nothing.
                }
            }).show();
    }
}
