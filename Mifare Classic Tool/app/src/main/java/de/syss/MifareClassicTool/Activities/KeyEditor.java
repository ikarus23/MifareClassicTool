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
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
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
        implements IActivityThatReactsToSave{

    private EditText mKeys;
    private String mFileName;
    /**
     * All keys and comments.
     * This will be updated with every
     * {@link #isValidKeyFile()} check.
     */
    private String[] mLines;

    /**
     * True if the user made changes to the key file.
     * Used by the "save before quitting" dialog.
     */
    private boolean mKeyChanged;

    /**
     * If true, the editor will close after a successful save.
     * @see #onSaveSuccessful()
     */
    private boolean mCloseAfterSuccessfulSave;


    /**
     * Initialize the key editor with key data from intent.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_editor);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(
                FileChooser.EXTRA_CHOSEN_FILE)) {
            mKeys = (EditText) findViewById(R.id.editTextKeyEditorKeys);

            // This is a (ugly) fix for a bug in Android 5.0+
            // https://code.google.com/p/android-developer-preview
            //    /issues/detail?id=110
            // (The EditText has the monospace typeface
            // property set via XML. But Android ignores it...)
            mKeys.setTypeface(Typeface.MONOSPACE);


            File keyFile = new File(getIntent().getStringExtra(
                    FileChooser.EXTRA_CHOSEN_FILE));
            mFileName = keyFile.getName();
            setTitle(getTitle() + " (" + mFileName + ")");
            if (keyFile.exists()) {
                String keyDump[] = Common.readFileLineByLine(keyFile,
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
                    mKeyChanged = true;
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
        switch (item.getItemId()) {
        case R.id.menuKeyEditorSave:
            onSave();
            return true;
        case R.id.menuKeyEditorShare:
            shareKeyFile();
            return true;
        case R.id.menuKeyEditorRemoveDuplicates:
            removeDuplicates();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Show a dialog in which the user can chose between "save", "don't save"
     * and "cancel", if there are unsaved changes.
     */
    @Override
    public void onBackPressed() {
        if (mKeyChanged) {
            new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_save_before_quitting_title)
            .setMessage(R.string.dialog_save_before_quitting)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton(R.string.action_save,
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Save.
                    mCloseAfterSuccessfulSave = true;
                    onSave();
                }
            })
            .setNeutralButton(R.string.action_cancel,
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Cancel. Do nothing.
                }
            })
            .setNegativeButton(R.string.action_dont_save,
                     new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    // Don't save.
                    finish();
                }
            }).show();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Set the state of {@link #mKeyChanged} to false and close the
     * editor if {@link #mCloseAfterSuccessfulSave} is true (due to exiting
     * with unsaved changes) after a successful save process.
     */
    @Override
    public void onSaveSuccessful() {
        if (mCloseAfterSuccessfulSave) {
            finish();
        }
        mKeyChanged = false;
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
     * Share a key file as "file://" stream resource (e.g. as e-mail
     * attachment). The key file will be checked and stored in the
     * {@link Common#TMP_DIR} directory. After this, a dialog will be displayed
     * in which the user can choose between apps that are willing to
     * handle the dump.
     * @see Common#TMP_DIR
     * @see #isValidKeyFileErrorToast()
     *
     */
    private void shareKeyFile() {
        if (!isValidKeyFileErrorToast()) {
            return;
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
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Common.HOME_DIR) + "/" + Common.TMP_DIR, fileName);
        if (!Common.saveFile(file, mLines, false)) {
            Toast.makeText(this, R.string.info_save_error,
                    Toast.LENGTH_LONG).show();
            return;
        }


        // Share file.
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(
                "file://" + file.getAbsolutePath()));
        sendIntent.putExtra(Intent.EXTRA_SUBJECT,
                getString(R.string.text_share_subject_key_file)
                + " (" + fileName + ")");
        startActivity(Intent.createChooser(sendIntent,
                getText(R.string.dialog_share_title)));
    }

    /**
     * Check if it is a valid key file
     * ({@link #isValidKeyFileErrorToast()}),
     * ask user for a save name and then call
     * {@link Common#checkFileExistenceAndSave(File, String[], boolean,
     * Context, IActivityThatReactsToSave)}
     * @see Common#checkFileExistenceAndSave(File, String[], boolean, Context,
     * IActivityThatReactsToSave)
     * @see #isValidKeyFileErrorToast()
     */
    private void onSave() {
        if (!isValidKeyFileErrorToast()) {
            return;
        }
        final File path = new File(
                Environment.getExternalStoragePublicDirectory(
                Common.HOME_DIR) + "/" + Common.KEYS_DIR);
        final Context cont = this;
        final IActivityThatReactsToSave activity =
                this;
        // Ask user for filename.
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setLines(1);
        input.setHorizontallyScrolling(true);
        input.setText(mFileName);
        input.setSelection(input.getText().length());
        new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_save_keys_title)
            .setMessage(R.string.dialog_save_keys)
            .setIcon(android.R.drawable.ic_menu_save)
            .setView(input)
            .setPositiveButton(R.string.action_ok,
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog,
                        int whichButton) {
                    if (input.getText() != null
                            && !input.getText().toString().equals("")) {
                        File file = new File(path.getPath(),
                                input.getText().toString());
                        Common.checkFileExistenceAndSave(file, mLines,
                                false, cont, activity);
                    } else {
                        // Empty name is not allowed.
                        Toast.makeText(cont, R.string.info_empty_file_name,
                                Toast.LENGTH_LONG).show();
                    }
                }
            })
            .setNegativeButton(R.string.action_cancel,
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog,
                        int whichButton) {
                    mCloseAfterSuccessfulSave = false;
                }
            }).show();
    }

    /**
     * Remove duplicates (keys) from key file.
     */
    private void removeDuplicates() {
        if (isValidKeyFileErrorToast()) {
            ArrayList<String> newLines = new ArrayList<String>();
            for (String line : mLines) {
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
            mLines = newLines.toArray(new String[newLines.size()]);
            setKeyArrayAsText(mLines);
        }
    }

    /**
     * Update the user input field for keys with the given lines.
     * @param lines The lines to set for the keys edit text.
     */
    private void setKeyArrayAsText(String[] lines) {
        String keyText = "";
        String s = System.getProperty("line.separator");
        for (int i = 0; i < lines.length-1; i++) {
            keyText += lines[i] + s;
        }
        keyText += lines[lines.length-1];

        mKeys.setText(keyText);
    }

    /**
     * Check if the user input is a valid key file and update
     * {@link #mLines}.
     * @return <ul>
     * <li>0 - All O.K.</li>
     * <li>1 - There is no key.</li>
     * <li>2 - At least one key has invalid characters (not hex).</li>
     * <li>3 - At least one key has not 6 byte (12 chars).</li>
     * </ul>
     */
    private int isValidKeyFile() {
        String[] lines = mKeys.getText().toString()
                .split(System.getProperty("line.separator"));
        boolean keyFound = false;
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].startsWith("#") && !lines[i].equals("")
                    && !lines[i].matches("[0-9A-Fa-f]+")) {
                // Not pure hex and not a comment.
                return 2;
            }

            if (!lines[i].startsWith("#") && !lines[i].equals("")
                    && lines[i].length() != 12) {
                // Not 12 chars per line.
                return 3;
            }

            if (!lines[i].startsWith("#") && !lines[i].equals("")) {
                // At least one key found.
                lines[i] = lines[i].toUpperCase(Locale.getDefault());
                keyFound = true;
            }
        }
        if (!keyFound) {
            // No key found.
            return 1;
        }
        mLines = lines;
        return 0;
    }

    /**
     * Check keys with {@link #isValidKeyFile()()} and show
     * a Toast message with error information (if an error occurred).
     * @return True if all keys were O.K. False otherwise.
     * @see #isValidKeyFile()
     */
    private boolean isValidKeyFileErrorToast() {
        int err = isValidKeyFile();
        if (err == 1) {
            Toast.makeText(this, R.string.info_valid_keys_no_keys,
                    Toast.LENGTH_LONG).show();
        } else if (err == 2) {
            Toast.makeText(this, R.string.info_valid_keys_not_hex,
                    Toast.LENGTH_LONG).show();
        } else if (err == 3) {
            Toast.makeText(this, R.string.info_valid_keys_not_6_byte,
                    Toast.LENGTH_LONG).show();
        }
        return err == 0;
    }
}
