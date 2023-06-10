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
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Arrays;

import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.R;


/**
 * A simple generic file chooser that lets the user choose a file from
 * a given directory. Optionally, it is also possible to delete files or to
 * create new ones. This Activity should be called via startActivityForResult()
 * with an Intent containing the {@link #EXTRA_DIR}.
 * The result codes are:
 * <ul>
 * <li>{@link Activity#RESULT_OK} - Everything is O.K. The chosen file will be
 * in the Intent ({@link #EXTRA_CHOSEN_FILE}).</li>
 * <li>1 - Directory from {@link #EXTRA_DIR} does not
 * exist.</li>
 * <li>2 - No directory specified in Intent
 * ({@link #EXTRA_DIR})</li>
 * <li>3 - RFU.</li>
 * <li>4 - Directory from {@link #EXTRA_DIR} is not a directory.</li>
 * </ul>
 * @author Gerhard Klostermeier
 */
public class FileChooser extends BasicActivity {

    // Input parameters.
    /**
     * Path to a directory with files. The files in the directory
     * are the files the user can choose from. This must be in the Intent.
     */
    public final static String EXTRA_DIR =
            "de.syss.MifareClassicTool.Activity.FileChooser.DIR";
    /**
     * The title of the activity. Optional.
     * e.g. "Open Dump File"
     */
    public final static String EXTRA_TITLE =
            "de.syss.MifareClassicTool.Activity.FileChooser.TITLE";
    /**
     * The small text above the files. Optional.
     * e.g. "Please choose a file:
     */
    public final static String EXTRA_CHOOSER_TEXT =
            "de.syss.MifareClassicTool.Activity.FileChooser.CHOOSER_TEXT";
    /**
     * The text of the choose button. Optional.
     * e.g. "Open File"
     */
    public final static String EXTRA_BUTTON_TEXT =
            "de.syss.MifareClassicTool.Activity.FileChooser.BUTTON_TEXT";

    /**
     * Set to True if file creation should be allowed.
     */
    public final static String EXTRA_ALLOW_NEW_FILE =
            "de.syss.MifareClassicTool.Activity.FileChooser.ALLOW_NEW_FILE";

    // Output parameter.
    /**
     * The file (with full path) that will be passed via Intent
     * to onActivityResult() method. The result code will be
     * {@link Activity#RESULT_OK}.
     */
    public final static String EXTRA_CHOSEN_FILE =
            "de.syss.MifareClassicTool.Activity.CHOSEN_FILE";
    /**
     * The filename (without path) that will be passed via Intent
     * to onActivityResult() method. The result code will be
     * {@link Activity#RESULT_OK}.
     */
    public final static String EXTRA_CHOSEN_FILENAME =
            "de.syss.MifareClassicTool.Activity.EXTRA_CHOSEN_FILENAME";


    private static final String LOG_TAG =
            FileChooser.class.getSimpleName();
    private RadioGroup mGroupOfFiles;
    private Button mChooserButton;
    private TextView mChooserText;
    private MenuItem mDeleteFile;
    private File mDir;
    private boolean mIsDirEmpty;
    private boolean mIsAllowNewFile;

    /**
     * Initialize class variables.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_chooser);
        mGroupOfFiles = findViewById(R.id.radioGroupFileChooser);
    }

    /**
     * Initialize the file chooser with the data from the calling Intent.
     *
     * @see #EXTRA_DIR
     * @see #EXTRA_TITLE
     * @see #EXTRA_CHOOSER_TEXT
     * @see #EXTRA_BUTTON_TEXT
     */
    @Override
    public void onStart() {
        super.onStart();

        mChooserText = findViewById(
                R.id.textViewFileChooser);
        mChooserButton = findViewById(
                R.id.buttonFileChooserChoose);
        Intent intent = getIntent();

        // Set title.
        if (intent.hasExtra(EXTRA_TITLE)) {
            setTitle(intent.getStringExtra(EXTRA_TITLE));
        }
        // Set chooser text.
        if (intent.hasExtra(EXTRA_CHOOSER_TEXT)) {
            mChooserText.setText(intent.getStringExtra(EXTRA_CHOOSER_TEXT));
        }
        // Set button text.
        if (intent.hasExtra(EXTRA_BUTTON_TEXT)) {
            mChooserButton.setText(intent.getStringExtra(EXTRA_BUTTON_TEXT));
        }
        // Check file creation.
        if (intent.hasExtra(EXTRA_ALLOW_NEW_FILE)) {
            mIsAllowNewFile = intent.getBooleanExtra(EXTRA_ALLOW_NEW_FILE, false);
        }

        // Check path and initialize file list.
        if (intent.hasExtra(EXTRA_DIR)) {
            File path = new File(intent.getStringExtra(EXTRA_DIR));
            if (path.exists()) {
                if (!path.isDirectory()) {
                    setResult(4);
                    finish();
                    return;
                }
                mDir = path;
                mIsDirEmpty = updateFileIndex(path);
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
     * Add the menu to the Activity.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.file_chooser_functions, menu);
        mDeleteFile = menu.findItem(R.id.menuFileChooserDeleteFile);
        MenuItem newFile = menu.findItem(R.id.menuFileChooserNewFile);

        // Enable/disable the delete menu item if there is a least one file.
        mDeleteFile.setEnabled(!mIsDirEmpty);

        // Enable/disable the new file menu item according to mIsAllowNewFile.
        newFile.setEnabled(mIsAllowNewFile);
        newFile.setVisible(mIsAllowNewFile);

        return true;
    }

    /**
     * Handle selected function form the menu (create new file,
     * delete file, etc.).
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection.
        int itemId = item.getItemId();
        if (itemId == R.id.menuFileChooserNewFile) {
            onNewFile();
            return true;
        } else if (itemId == R.id.menuFileChooserDeleteFile) {
            onDeleteFile();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Finish the Activity with an Intent containing
     * {@link #EXTRA_CHOSEN_FILE} and {@link #EXTRA_CHOSEN_FILENAME} as result.
     * You can catch that result by overriding onActivityResult() in the
     * Activity that called the file chooser via startActivityForResult().
     *
     * @param view The View object that triggered the function
     *             (in this case the choose file button).
     * @see #EXTRA_CHOSEN_FILE
     * @see #EXTRA_CHOSEN_FILENAME
     */
    public void onFileChosen(View view) {
        RadioButton selected = findViewById(
                mGroupOfFiles.getCheckedRadioButtonId());
        Intent intent = new Intent();
        File file = new File(mDir.getPath(), selected.getText().toString());
        intent.putExtra(EXTRA_CHOSEN_FILE, file.getPath());
        intent.putExtra(EXTRA_CHOSEN_FILENAME, file.getName());
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    /**
     * Update the file list and the components that depend on it
     * (e.g. disable the open file button if there is no file).
     *
     * @param path Path to the directory which will be listed.
     * @return True if directory is empty. False otherwise.
     */
    @SuppressLint("SetTextI18n")
    private boolean updateFileIndex(File path) {
        boolean isEmpty = true;
        File[] files = null;
        String chooserText = "";

        if (path != null) {
            files = path.listFiles();
        }
        mGroupOfFiles.removeAllViews();

        // Refresh file list.
        if (files != null && files.length > 0) {
            Arrays.sort(files);
            for (File f : files) {
                if (f.isFile()) { // Do not list directories.
                    RadioButton r = new RadioButton(this);
                    r.setText(f.getName());
                    mGroupOfFiles.addView(r);
                }
            }
            if (mGroupOfFiles.getChildCount() > 0) {
                isEmpty = false;
                ((RadioButton) mGroupOfFiles.getChildAt(0)).setChecked(true);
            }
        } else {
            // No files in directory.
            isEmpty = true;
        }

        // Update chooser text.
        // Add storage model update info, if MCT was updated and there are no
        // or only standard files.
        if ((!Common.isFirstInstall() && isEmpty) ||
                (!Common.isFirstInstall() && files != null && files.length == 2
                && files[0].getName().equals(Common.STD_KEYS_EXTENDED)
                && files[1].getName().equals(Common.STD_KEYS))) {
            chooserText += getString(R.string.text_missing_files_update) + "\n\n";
        }
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_CHOOSER_TEXT)) {
            chooserText += intent.getStringExtra(EXTRA_CHOOSER_TEXT);
        } else {
            chooserText += getString(R.string.text_chooser_info_text);
        }
        if (isEmpty) {
            chooserText += "\n\n   --- "
                    + getString(R.string.text_no_files_in_chooser)
                    + " ---";
        }
        mChooserText.setText(chooserText);

        mChooserButton.setEnabled(!isEmpty);
        if (mDeleteFile != null) {
            mDeleteFile.setEnabled(!isEmpty);
        }

        return isEmpty;
    }

    /**
     * Ask the user for a file name, create this file and choose it.
     * ({@link #onFileChosen(View)}).
     */
    private void onNewFile() {
        final Context cont = this;
        String prefill = "";
        if (mDir.getName().equals(Common.KEYS_DIR)) {
            prefill = ".keys";
        }
        // Init. layout.
        View dialogLayout = getLayoutInflater().inflate(
                R.layout.dialog_save_file,
                findViewById(android.R.id.content), false);
        TextView message = dialogLayout.findViewById(
                R.id.textViewDialogSaveFileMessage);
        final EditText input = dialogLayout.findViewById(
                R.id.editTextDialogSaveFileName);
        message.setText(R.string.dialog_new_file);
        input.setText(prefill);
        input.requestFocus();
        input.setSelection(0);

        // Show keyboard.
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        input.postDelayed(() -> {
            input.requestFocus();
            imm.showSoftInput(input, 0);
        }, 100);

        // Ask user for filename.
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_new_file_title)
                .setIcon(android.R.drawable.ic_menu_add)
                .setView(dialogLayout)
                .setPositiveButton(R.string.action_ok,
                        (dialog, whichButton) -> {
                            if (input.getText() != null
                                    && !input.getText().toString().equals("")
                                    && !input.getText().toString().contains("/")) {
                                File file = new File(mDir.getPath(),
                                        input.getText().toString());
                                if (file.exists()) {
                                    Toast.makeText(cont,
                                            R.string.info_file_already_exists,
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }
                                Intent intent = new Intent();
                                intent.putExtra(EXTRA_CHOSEN_FILE, file.getPath());
                                setResult(Activity.RESULT_OK, intent);
                                finish();
                            } else {
                                // Invalid file name.
                                Toast.makeText(cont, R.string.info_invalid_file_name,
                                        Toast.LENGTH_LONG).show();
                            }
                        })
                .setNegativeButton(R.string.action_cancel,
                        (dialog, whichButton) -> {
                            // Do nothing.
                        })
                .show();
    }

    /**
     * Delete the selected file and update the file list.
     *
     * @see #updateFileIndex(File)
     */
    private void onDeleteFile() {
        RadioButton selected = findViewById(
                mGroupOfFiles.getCheckedRadioButtonId());
        File file = new File(mDir.getPath(), selected.getText().toString());
        file.delete();
        mIsDirEmpty = updateFileIndex(mDir);
    }
}
