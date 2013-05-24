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

import java.io.File;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.R;

/**
 * Show and edit key files.
 * @author Gerhard Klostermeier
 */
public class KeyEditorActivity extends BasicActivity {

    private EditText mKeys;
    private String mFileName;
    /**
     * All keys and comments.
     * This will be updated with every
     * {@link #isValidKeyFile()} check.
     */
    private String[] mLines;

    /**
     * Initialize the key editor with key data from intent.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_editor);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(
                FileChooserActivity.EXTRA_CHOSEN_FILE)) {
            mKeys = (EditText) findViewById(R.id.editTextKeyEditorKeys);
            File keyFile = new File(getIntent().getStringExtra(
                    FileChooserActivity.EXTRA_CHOSEN_FILE));
            mFileName = keyFile.getName();
            setTitle(getTitle() + " (" + mFileName + ")");
            String keyDump[] = Common.readFileLineByLine(keyFile, true);
            setKeyArrayAsText(keyDump);
            setIntent(null);
        } else {
            setResult(1);
            finish();
        }
    }

    /**
     * Check if it is a valid key file
     * ({@link #isValidKeyFileErrorToast()}),
     * ask user for a save name and then call
     * {@link Common#saveFile(File, String[])}
     * @param view The View object that triggered the method
     * (in this case the save button).
     * @see Common#saveFile(File, String[])
     * @see #isValidKeyFileErrorToast()
     */
    public void onSave(View view) {
        if (isValidKeyFileErrorToast()) {
            if (!Common.isExternalStorageWritableErrorToast(this)) {
                return;
            }
            final File path = new File(
                    Environment.getExternalStoragePublicDirectory(
                    Common.HOME_DIR) + Common.KEYS_DIR);
            final Context cont = this;
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
                .setPositiveButton(R.string.button_ok,
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (input.getText() != null
                                && !input.getText().toString().equals("")) {
                            File file = new File(path.getPath(),
                                    input.getText().toString());
                            if (Common.saveFile(file, mLines)) {
                                Toast.makeText(cont,
                                        R.string.info_save_successful,
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(cont,
                                        R.string.info_save_error,
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

    /**
     * Remove duplicates (keys) from key file.
     * @param view The View object that triggered the method
     * (in this case the save button).
     */
    public void onRemoveDuplicates(View view) {
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
                    && lines[i].matches("[0-9A-Fa-f]+") == false) {
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
                lines[i] = lines[i].toUpperCase();
                keyFound = true;
            }
        }
        if (keyFound == false) {
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
