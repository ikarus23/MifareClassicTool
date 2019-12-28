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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.R;

import static de.syss.MifareClassicTool.Activities.Preferences.Preference.UseInternalStorage;

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
 * <li>3 - External Storage is not read/writable. This error is
 * displayed to the user via Toast.</li>
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

    // TODO: doc.
    public final static String EXTRA_IS_KEY_FILE =
            "de.syss.MifareClassicTool.Activity.IS_KEY_FILE";
    public final static String EXTRA_IS_DUMP_FILE =
            "de.syss.MifareClassicTool.Activity.IS_DUMP_FILE";


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
    private MenuItem mExportFile;
    private File mDir;
    private boolean mIsDirEmpty;
    private boolean mIsKeyFile = false;
    private boolean mIsDumpFile = false;
    private boolean mIsExport = false;
    private FileType mFileType;
    private enum FileType {
        MCT(".mct"),
        JSON(".json"),
        BIN(".bin"),
        EML(".eml");

        private final String text;

        private FileType(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_chooser);
        mGroupOfFiles = findViewById(R.id.radioGroupFileChooser);
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

        if (!Common.getPreferences().getBoolean(UseInternalStorage.toString(),
                false) && !Common.isExternalStorageWritableErrorToast(this)) {
            setResult(3);
            finish();
            return;
        }
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
        // Remember file functionality.
        if (intent.hasExtra(EXTRA_IS_KEY_FILE)) {
            mIsKeyFile = intent.getBooleanExtra(
                    EXTRA_IS_KEY_FILE, false);
        }
        if (intent.hasExtra(EXTRA_IS_DUMP_FILE)) {
            mIsDumpFile = intent.getBooleanExtra(
                    EXTRA_IS_DUMP_FILE, false);
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
        mExportFile = menu.findItem(R.id.menuFileChooserExportFile);

        // Enable/disable the delete/export menu item if there is a least one
        // file.
        mDeleteFile.setEnabled(!mIsDirEmpty);
        mExportFile.setEnabled(!mIsDirEmpty);


        if (mIsKeyFile) {
            // Exporting key files is not supported.
            menu.removeItem(mExportFile.getItemId());
        }
        if (mIsDumpFile) {
            // Creating new dump files is not supported.
            menu.removeItem(menu.findItem(R.id.menuFileChooserNewFile)
                    .getItemId());
        }

        return true;
    }

    // TODO: doc.
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.file_types, menu);
    }

    /**
     * Handle selected function form the menu (create new file,
     * delete file, etc.).
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection.
        switch (item.getItemId()) {
            case R.id.menuFileChooserNewFile:
                onNewFile();
                return true;
            case R.id.menuFileChooserDeleteFile:
                onDeleteFile();
                return true;
            case R.id.menuFileChooserImportFile:
                mIsExport = false;
                if (mIsDumpFile) {
                    showTypeChooserMenu();
                } else {
                    showImportFileChooser();
                }
                return true;
            case R.id.menuFileChooserExportFile:
                mIsExport = true;
                if (mIsDumpFile) {
                    showTypeChooserMenu();
                } else {
                    // Exporting key files is not supported.
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // TODO: doc.
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // Handle item selection.
        switch (item.getItemId()) {
            case R.id.menuFileTypesMct:
                mFileType = FileType.MCT;
                break;
            case R.id.menuFileTypesJson:
                mFileType = FileType.JSON;
                break;
            case R.id.menuFileTypesBinMfd:
                mFileType = FileType.BIN;
                break;
            case R.id.menuFileTypesEml:
                mFileType = FileType.EML;
                break;
            default:
                return super.onContextItemSelected(item);
        }

        if (mIsExport) {
            // Convert file and export.
            onExportFile();
        } else {
            // Let the user pick the file to import.
            showImportFileChooser();
        }
        return true;
    }

    // TODO: doc.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode) {
            case 1: // File for importing has been selected.
            if (resultCode == RESULT_OK) {
                Uri selectedLocation = data.getData();
                onImportFile(selectedLocation);
            }
        }
    }

    /**
     * Finish the Activity with an Intent containing
     * {@link #EXTRA_CHOSEN_FILE} and {@link #EXTRA_CHOSEN_FILENAME} as result.
     * You can catch that result by overriding onActivityResult() in the
     * Activity that called the file chooser via startActivityForResult().
     * @param view The View object that triggered the function
     * (in this case the choose file button).
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
     * @param path Path to the directory which will be listed.
     * @return True if directory is empty. False otherwise.
     */
    private boolean updateFileIndex(File path) {
        boolean isEmpty;
        File[] files = path.listFiles();
        mGroupOfFiles.removeAllViews();

        // Refresh file list.
        if (files != null && files.length > 0) {
            isEmpty = false;
            Arrays.sort(files);

            for(File f : files) {
                if (f.isFile()) { // Do not list directories.
                    RadioButton r = new RadioButton(this);
                    r.setText(f.getName());
                    mGroupOfFiles.addView(r);
                }
            }
            // Check first file.
            ((RadioButton)mGroupOfFiles.getChildAt(0)).setChecked(true);
        } else {
            // No files in directory.
            isEmpty = true;
            Intent intent = getIntent();
            String chooserText = "";
            if (intent.hasExtra(EXTRA_CHOOSER_TEXT)) {
                chooserText = intent.getStringExtra(EXTRA_CHOOSER_TEXT);
            }
            mChooserText.setText(chooserText
                    + "\n   --- "
                    + getString(R.string.text_no_files_in_chooser)
                    + " ---");
        }

        mChooserButton.setEnabled(!isEmpty);
        if (mDeleteFile != null && mExportFile != null) {
            mDeleteFile.setEnabled(!isEmpty);
            mExportFile.setEnabled(!isEmpty && !mIsKeyFile);
        }

        return isEmpty;
    }

    /**
     * Ask the user for a file name, create this file and choose it.
     * ({@link #onFileChosen(View)}).
     */
    private void onNewFile() {
        final Context cont = this;
        // Ask user for filename.
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setLines(1);
        input.setHorizontallyScrolling(true);
        new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_new_file_title)
            .setMessage(R.string.dialog_new_file)
            .setIcon(android.R.drawable.ic_menu_add)
            .setView(input)
            .setPositiveButton(R.string.action_ok,
                    (dialog, whichButton) -> {
                        if (input.getText() != null
                                && !input.getText().toString().equals("")) {
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
                            // Empty name is not allowed.
                            Toast.makeText(cont, R.string.info_empty_file_name,
                                    Toast.LENGTH_LONG).show();
                        }
                    })
            .setNegativeButton(R.string.action_cancel,
                    (dialog, whichButton) -> {
                        // Do nothing.
                    }).show();
    }

    /**
     * Delete the selected file and update the file list.
     * @see #updateFileIndex(File)
     */
    private void onDeleteFile() {
        RadioButton selected = findViewById(
                mGroupOfFiles.getCheckedRadioButtonId());
        File file  = new File(mDir.getPath(), selected.getText().toString());
        file.delete();
        mIsDirEmpty = updateFileIndex(mDir);
    }

    // TODO: doc.
    private void onImportFile(Uri file) {
        String[] content = Common.readUriLineByLine(file,this);
        if (content == null) {
            return;
        }
        // Key or dump?
        if (mIsKeyFile) {
            // Importing key files is not supported.
        } else if (mIsDumpFile) {
            // Convert.
            String[] convertedContent = convert(
                    content, mFileType, FileType.MCT);
            // Remove file extension and replace it with ".mct".
            String fileName = Common.getFileName(file, this);
            if (fileName.contains(".")) {
                fileName = fileName.substring(0, fileName.lastIndexOf('.'));
            }
            String destFileName = fileName + FileType.MCT.toString();
            // Save converted file.
            String destPath = Common.HOME_DIR + "/" + Common.DUMPS_DIR;
            File destination = Common.getFileFromStorage(
                    destPath + "/" + destFileName, true);
            if (Common.saveFile(destination, convertedContent,false)) {
                Toast.makeText(this, R.string.info_file_imported,
                        Toast.LENGTH_LONG).show();
                mIsDirEmpty = updateFileIndex(mDir);
            }
        } else {
            // TODO: Implement error (should never occur).
        }
    }

    // TODO: doc.
    private void onExportFile() {
        RadioButton selected = findViewById(
                mGroupOfFiles.getCheckedRadioButtonId());
        String fileName = selected.getText().toString();
        File source = new File(mDir.getPath(), fileName);
        String[] content = Common.readFileLineByLine(source, false,this);
        if (content == null) {
            return;
        }
        // Key or dump file?
        if (mIsKeyFile) {
            // Exporting key files is not supported.
            // (User can just grab the files from the folder or use share.)
        } else if (mIsDumpFile) {
            // Convert.
            String[] convertedContent = convert(
                    content, FileType.MCT, mFileType);
            // Remove ".mct" file extension and replace it with export type.
            fileName = fileName.replace(FileType.MCT.toString(), "");
            String destFileName = fileName + mFileType.toString();
            // Save converted file.
            String destPath = Common.HOME_DIR + "/" + Common.EXPORT_DIR;
            File destination = Common.getFileFromStorage(
                    destPath + "/" + destFileName, true);
            if (Common.saveFile(destination, convertedContent,false)) {
                Toast.makeText(this, R.string.info_file_exported,
                        Toast.LENGTH_LONG).show();
            }
        } else {
            // TODO: Implement error (should never occur).
        }
    }

    // TODO: doc.
    private String[] convert(String[] source, FileType srcType,
            FileType destType) {
        // TODO: implement.
        // TODO: Write "SectorKeys" information into JSON as well.
        // Convert source to json.
        ArrayList<String> json = new ArrayList<String>();
        String block = null;
        if (srcType != FileType.JSON) {
            json.add("{");
            json.add("  \"Created\": \"MifareClassicTool\",");
            json.add("  \"FileType\": \"mfcard\",");
            json.add("  \"blocks\": {");
        }
        switch (srcType) {
            case MCT:
                // Convert MCT to json (export).
                int err = Common.isValidDump(source, true);
                if (err != 0) {
                    Common.isValidDumpErrorToast(err, this);
                    return null;
                }
                int sectorNumber = 0;
                int blockNumber = 0;
                for (String line : source) {
                    if (line.startsWith("+")) {
                        sectorNumber = Integer.parseInt(line.split(": ")[1]);
                        if (sectorNumber < 32) {
                            blockNumber = sectorNumber * 4;
                        } else {
                            blockNumber =  32 * 4 + (sectorNumber - 32) * 16;
                        }
                        continue;
                    }
                    block = "    \"" + blockNumber + "\": \"" + line + "\",";
                    json.add(block);
                    blockNumber += 1;
                }
                break;
            case JSON:
                // Convert json to json (import).
                json = new ArrayList<String>(Arrays.asList(source));
                break;
            case BIN:
                // TODO: Convert bin to json (import).
                // Convert bin to json (import).
                break;
            case EML:
                // Convert eml to json (import).
                for (int i = 0; i < source.length; i++) {
                    block = "    \"" + i + "\": \"" + source[i] + "\",";
                    json.add(block);
                }
                break;
        }
        if (srcType != FileType.JSON) {
            block = block.replace(",", "");
            json.remove(json.size()-1);
            json.add(block);
            json.add("  }");
            json.add("}");
        }

        // Check source convertion.
        if (json.size() <= 6) {
            // Error converting source file.
            return null;
        }

        // TODO: Only temporary.
        destType = FileType.JSON;

        // Convert json to destType.
        String[] dest = null;
        switch (destType) {
            case JSON:
                // Export json.
                dest = json.toArray(new String[json.size()]);
                break;
            case BIN:
                // TODO: Convert json to bin (export).
                break;
            case MCT:
                // TODO: Only temporary.
                if (srcType == FileType.MCT) {
                    // Import/Export.
                    dest = source;
                } else {
                    // TODO: Convert json to MCT (import).
                }
                break;
            case EML:
                // TODO: Convert json to eml (export).
        }

        return dest;
    }

    // TODO: doc.
    private void showTypeChooserMenu() {
        // mGroupOfFiles is just used as a dummy because a context menu
        // always need a view.
        registerForContextMenu(mGroupOfFiles);
        openContextMenu(mGroupOfFiles);
    }

    // TODO: doc.
    private void showImportFileChooser() {
        Intent intent = new Intent();
        intent.setType("*/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        String title = getString(R.string.text_select_file);
        startActivityForResult(Intent.createChooser(intent, title), 1);
    }

    // TODO: Once we've dropped Android 4 and have API level 21, let the user
    // Choose the export directory.
    private void showExportDirectoryChooser() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        String title = getString(R.string.text_select_directory);
        startActivityForResult(Intent.createChooser(intent, title), 2);
    }
}

// TODO: implement convert function completely.
// TODO: Import key file:
//  isKeyFile->showImpotFileChooser->Copy file (no convert).
// TODO: save isKeyFile or other important vars.
