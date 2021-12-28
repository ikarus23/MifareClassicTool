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

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Locale;

import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.R;

/**
 * Show and edit key files.
 * @author Gerhard Klostermeier
 */
public class KeyEditor extends BasicActivity
        implements IActivityThatReactsToSave {

    private EditText mKeys;
    private String mFileName;
    /**
     * All keys and comments.
     * This will be updated with every
     * {@link #checkDumpAndUpdateLines()} check.
     */
    private String[] mLines;

    /**
     * True if the user made changes to the key file.
     * Used by the "save before quitting" dialog.
     */
    private boolean mKeysChanged;

    /**
     * If true, the editor will close after a successful save.
     * @see #onSaveSuccessful()
     */
    private boolean mCloseAfterSuccessfulSave;


    /**
     * Initialize the key editor with key data from intent
     * or recreated it previously stored state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_editor);

        mKeys = findViewById(R.id.editTextKeyEditorKeys);
        // This is a (ugly) fix for a bug in Android 5.0+
        // https://code.google.com/p/android-developer-preview
        //    /issues/detail?id=110
        // (The EditText has the monospace typeface
        // property set via XML. But Android ignores it...)
        mKeys.setTypeface(Typeface.MONOSPACE);

        Intent intent = getIntent();
        if (savedInstanceState != null) {
            mCloseAfterSuccessfulSave = savedInstanceState.getBoolean(
                    "close_after_successful_save");
            mKeysChanged = savedInstanceState.getBoolean("keys_changed");
            mFileName = savedInstanceState.getString("file_name");
            mLines = savedInstanceState.getStringArray("lines");
        } else if (intent != null && intent.hasExtra(
                FileChooser.EXTRA_CHOSEN_FILE)) {
            File keyFile = new File(getIntent().getStringExtra(
                    FileChooser.EXTRA_CHOSEN_FILE));
            mFileName = keyFile.getName();
            setTitle(getTitle() + " (" + mFileName + ")");
            if (keyFile.exists()) {
                String[] keyDump = Common.readFileLineByLine(keyFile,
                        true, this);
                if (keyDump == null) {
                    // Error. Exit.
                    finish();
                    return;
                }
                setKeyArrayAsText(keyDump);
            }

            mKeys.addTextChangedListener(new TextWatcher(){
                @Override
                public void afterTextChanged(Editable s) {
                    // Text was changed.
                    mKeysChanged = true;
                }
                @Override
                public void beforeTextChanged(CharSequence s,
                        int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s,
                        int start, int before, int count) {}
            });

            setIntent(null);
        } else {
            finish();
        }
    }

    /**
     * Save important state data before this activity gets destroyed.
     * @param outState The state to put data into.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("close_after_successful_save", mCloseAfterSuccessfulSave);
        outState.putBoolean("keys_changed", mKeysChanged);
        outState.putString("file_name", mFileName);
        outState.putStringArray("lines", mLines);
    }

    /**
     * Add the menu with the editor functions to the Activity.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.key_editor_functions, menu);
        return true;
    }

    /**
     * Handle the selected function from the editor menu.
     * @see #onSave()
     * @see #shareKeyFile()
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection.
        int id = item.getItemId();
        if (id == R.id.menuKeyEditorSave) {
            onSave();
            return true;
        } else if (id == R.id.menuKeyEditorShare) {
            shareKeyFile();
            return true;
        } else if (id == R.id.menuKeyEditorRemoveDuplicates) {
            removeDuplicates();
            return true;
        } else if (id == R.id.menuKeyEditorExportKeys) {
            exportKeys();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Show a dialog in which the user can chose between "save", "don't save"
     * and "cancel", if there are unsaved changes.
     */
    @Override
    public void onBackPressed() {
        if (mKeysChanged) {
            new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_save_before_quitting_title)
            .setMessage(R.string.dialog_save_before_quitting)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton(R.string.action_save,
                    (dialog, which) -> {
                        // Save.
                        mCloseAfterSuccessfulSave = true;
                        onSave();
                    })
            .setNeutralButton(R.string.action_cancel,
                    (dialog, which) -> {
                        // Cancel. Do nothing.
                    })
            .setNegativeButton(R.string.action_dont_save,
                    (dialog, id) -> {
                        // Don't save.
                        finish();
                    }).show();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Set the state of {@link #mKeysChanged} to false and close the
     * editor if {@link #mCloseAfterSuccessfulSave} is true (due to exiting
     * with unsaved changes) after a successful save process.
     */
    @Override
    public void onSaveSuccessful() {
        if (mCloseAfterSuccessfulSave) {
            finish();
        }
        mKeysChanged = false;
    }

    /**
     * Set the state of {@link #mCloseAfterSuccessfulSave} to false if
     * there was an error (or if the user hit cancel) during the save process.
     */
    @Override
    public void onSaveFailure() {
        mCloseAfterSuccessfulSave = false;
    }

    /**
     * Share a key file as "file://" stream resource (e.g. as e-mail attachment).
     * A dialog will be displayed in which the user can choose between apps
     * that are willing to handle the dump. For sharing, the dump will be
     * saved as temporary file.
     * @see #saveKeysToTemp()
     *
     */
    private void shareKeyFile() {
        File file = saveKeysToTemp();
        if (file == null || !file.exists() && file.isDirectory()) {
            return;
        }
        // Share file.
        Common.shareTextFile(this, file);
    }

    /**
     * Export the keys using the {@link ImportExportTool}. For exporting, the dump
     * will be saved as temporary file.
     * @see #saveKeysToTemp()
     */
    private void exportKeys() {
        File file = saveKeysToTemp();
        if (file == null || !file.exists() && file.isDirectory()) {
            return;
        }
        Intent intent = new Intent(this, ImportExportTool.class);
        intent.putExtra(ImportExportTool.EXTRA_FILE_PATH, file.getAbsolutePath());
        intent.putExtra(ImportExportTool.EXTRA_IS_DUMP_FILE, false);
        startActivity(intent);
    }

    /**
     * The keys will be checked and stored in the {@link Common#TMP_DIR} directory.
     * @return The temporary key file.
     * @see Common#TMP_DIR
     */
    private File saveKeysToTemp() {
        if (!Common.isValidKeyFileErrorToast(checkDumpAndUpdateLines(), this)) {
            return null;
        }
        // Save key file to to a temporary file which will be
        // attached for sharing (and stored in the tmp folder).
        String fileName;
        if (mFileName.equals("")) {
            // The key file has no name. Use date and time as name.
            GregorianCalendar calendar = new GregorianCalendar();
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss",
                    Locale.getDefault());
            fmt.setCalendar(calendar);
            fileName = fmt.format(calendar.getTime());
        } else {
            fileName = mFileName;
        }
        // Save file to tmp directory.
        File file = Common.getFile(Common.TMP_DIR + "/" + fileName);
        if (!Common.saveFile(file, mLines, false)) {
            Toast.makeText(this, R.string.info_save_error,
                    Toast.LENGTH_LONG).show();
            return null;
        }

        return file;
    }

    /**
     * Check if it is a valid key file
     * ask user for a save name and then call {@link #saveFile(String, File)}.
     * @see Common#isValidKeyFileErrorToast(int, Context)
     */
    private void onSave() {
        if (!Common.isValidKeyFileErrorToast(checkDumpAndUpdateLines(), this)) {
            return;
        }
        final File path = Common.getFile(Common.KEYS_DIR);
        // Init. layout.
        View dialogLayout = getLayoutInflater().inflate(
                R.layout.dialog_save_file,
                findViewById(android.R.id.content), false);
        TextView message = dialogLayout.findViewById(
                R.id.textViewDialogSaveFileMessage);
        final EditText input = dialogLayout.findViewById(
                R.id.editTextDialogSaveFileName);
        message.setText(R.string.dialog_save_keys);
        input.setText(mFileName);
        input.requestFocus();
        input.setSelection(0);

        // Ask user for filename.
        new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_save_keys_title)
            .setIcon(android.R.drawable.ic_menu_save)
            .setView(dialogLayout)
            .setPositiveButton(R.string.action_ok,
                    (dialog, whichButton) -> saveFile(input.getText().toString(), path))
            .setNegativeButton(R.string.action_cancel,
                    (dialog, whichButton) -> mCloseAfterSuccessfulSave = false)
            .show();
    }

    /**
     * Save {@link #mLines} to a file. Makes also sure not to overwrite a
     * standard key file.
     * @param fileName Name of the key file.
     * @param path Path of where to save the key file.
     * @see Common#checkFileExistenceAndSave(
     *          File, String[], boolean, Context, IActivityThatReactsToSave)
     */
    private void saveFile(String fileName, File path) {
        if (fileName == null || fileName.equals("") || fileName.contains("/")) {
            Toast.makeText(this, R.string.info_invalid_file_name,
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (fileName.equals(Common.STD_KEYS) || fileName.equals(Common.STD_KEYS_EXTENDED)) {
            Toast.makeText(this, R.string.info_std_key_overwrite,
                    Toast.LENGTH_LONG).show();
            return;
        }
        File file = new File(path.getPath(), fileName);
        Common.checkFileExistenceAndSave(file, mLines,
                false, this, this);
    }

    /**
     * Remove duplicates (keys) from key file.
     */
    private void removeDuplicates() {
        if (Common.isValidKeyFileErrorToast(checkDumpAndUpdateLines(), this)) {
            ArrayList<String> newLines = new ArrayList<>();
            for (String line : mLines) {
                line = line.trim();
                if (line.equals("") || line.startsWith("#")) {
                    // Add comments for sure.
                    newLines.add(line);
                    continue;
                }
                if (!newLines.contains(line)) {
                    // Add key if it is not already added.
                    newLines.add(line);
                }
            }
            mLines = newLines.toArray(new String[0]);
            setKeyArrayAsText(mLines);
        }
    }

    /**
     * Update the user input field for keys with the given lines.
     * @param lines The lines to set for the keys edit text.
     */
    private void setKeyArrayAsText(String[] lines) {
        StringBuilder keyText = new StringBuilder();
        String s = System.getProperty("line.separator");
        for (int i = 0; i < lines.length-1; i++) {
            keyText.append(lines[i]);
            keyText.append(s);
        }
        keyText.append(lines[lines.length-1]);

        mKeys.setText(keyText);
    }

    /**
     * Check if the user input is a valid key file, convert all keys
     * to upper case and update {@link #mLines} and {@link #mKeys}.
     * Return values should be compliant to
     * {@link Common#isValidKeyFileErrorToast(int, Context)}.
     * @return <ul>
     * <li>0 - All O.K.</li>
     * <li>1 - There is no key.</li>
     * <li>2 - At least one key has invalid characters (not hex).</li>
     * <li>3 - At least one key has not 6 byte (12 chars).</li>
     * </ul>
     * @see Common#isValidKeyFile(String[])
     */
    private int checkDumpAndUpdateLines() {
        Editable editorContent = mKeys.getText();
        if (editorContent == null) {
            return 1;
        }
        String editorText = editorContent.toString();
        if (editorText == null) {
            return 1;
        }
        String[] lines = editorText.split(System.getProperty("line.separator"));
        int ret = Common.isValidKeyFile(lines);
        if (ret != 0) {
            return ret;
        }

        // Convert keys to uppercase.
        mLines = new String[lines.length];
        for (int i = 0; i < lines.length; i++) {
            lines[i] = lines[i].trim();
            String line = lines[i];
            if (line.startsWith("#")) {
                mLines[i] = lines[i];
                continue;
            }
            line = line.split("#")[0];
            line = line.trim();
            if (line.equals("")) {
                mLines[i] = lines[i];
                continue;
            }
            // Line is a key. Convert to uppercase.
            line = line.toUpperCase(Locale.getDefault());
            mLines[i] = line + lines[i].substring(12);
        }

        setKeyArrayAsText(mLines);
        // Checking (and converting them to uppercase) does not count as change.
        mKeysChanged = false;
        return ret;
    }
}
