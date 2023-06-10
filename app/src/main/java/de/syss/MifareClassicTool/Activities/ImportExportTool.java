/*
 * Copyright 2020 Gerhard Klostermeier
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
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.MCReader;
import de.syss.MifareClassicTool.R;


/**
 * A simple tool to import and export dump files in and from different file
 * formats. Supported are .mct (Mifare Classic Tool), .bin/.mfd (Proxmark,
 * libnfc, mfoc), .eml (Proxmark emulator) and .json (Proxmark, Chameleon
 * Mini GUI).
 * @author Gerhard Klostermeier
 */
public class ImportExportTool extends BasicActivity {

    /**
     * Boolean value to tell whether the file to export is a key file or a dump file.
     */
    public final static String EXTRA_IS_DUMP_FILE =
            "de.syss.MifareClassicTool.Activity.ImportExportTool.IS_DUMP_FILE";
    /**
     * Path to the file which should be exported.
     */
    public final static String EXTRA_FILE_PATH =
            "de.syss.MifareClassicTool.Activity.ImportExportTool.FILE_PATH";

    private final static int IMPORT_FILE_CHOSEN = 1;
    private final static int EXPORT_FILE_CHOSEN = 2;
    private final static int EXPORT_LOCATION_CHOSEN = 3;
    private final static int BACKUP_LOCATION_CHOSEN = 4;
    private boolean mIsCalledWithExportFile = false;
    private boolean mIsExport = false;
    private boolean mIsDumpFile = false;
    private String mFile;
    private String[] mConvertedContent;
    private FileType mFileType;
    private enum FileType {
        MCT(".mct"),
        KEYS(".keys"),
        JSON(".json"),
        BIN(".bin"),
        EML(".eml");

        private final String text;

        FileType(final String text) {
            this.text = text;
        }

        @NonNull
        @Override
        public String toString() {
            return text;
        }
    }

    /**
     * Initialize the activity layout and state if there is one.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_export_tool);

        if (savedInstanceState != null) {
            mIsDumpFile = savedInstanceState.getBoolean("is_dump_file");
            mIsExport = savedInstanceState.getBoolean("is_export");
            mIsCalledWithExportFile = savedInstanceState.getBoolean(
                    "is_called_with_export_file");
            mFile = savedInstanceState.getString("file");
            mConvertedContent = savedInstanceState.getStringArray(
                    "converted_content");
        }
    }

    /**
     * Check if there was a file appended to the intent calling this activity
     * and if so, export this file.
     * @see #EXTRA_FILE_PATH
     * @see #EXTRA_IS_DUMP_FILE
     */
    @Override
    public void onResume() {
        super.onResume();

        // Was this tool opened by another activity to export a file?
        Intent intent = getIntent();
        if (!mIsCalledWithExportFile && intent.hasExtra(EXTRA_FILE_PATH)) {
            final File path = new File(intent.getStringExtra(EXTRA_FILE_PATH));
            if (path.exists() && !path.isDirectory()) {
                mIsCalledWithExportFile = true;
                // File to export is known. Trigger the export process.
                // However, do this with a delay. Context menus (for choosing the
                // export file type) can only be shown, once the activity is running.
                final Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(() -> {
                    mIsExport = true;
                    if (getIntent().hasExtra(EXTRA_IS_DUMP_FILE)) {
                        mIsDumpFile = getIntent().getBooleanExtra(
                                EXTRA_IS_DUMP_FILE, false);
                    }
                    Intent intent1 = new Intent();
                    intent1.putExtra(FileChooser.EXTRA_CHOSEN_FILE, path.getAbsolutePath());
                    onActivityResult(EXPORT_FILE_CHOSEN, RESULT_OK, intent1);
                }, 300);
            }
        }
    }

    /**
     * Save important state data before this activity gets destroyed.
     * @param outState The state to put data into.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("is_dump_file", mIsDumpFile);
        outState.putBoolean("is_export", mIsExport);
        outState.putBoolean("is_called_with_export_file", mIsCalledWithExportFile);
        outState.putString("file", mFile);
        outState.putStringArray("converted_content", mConvertedContent);
    }

    /**
     * Create the context menu with the supported dump/keys file types.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        if(v.getId() == R.id.buttonImportExportToolImportDump) {
            inflater.inflate(R.menu.dump_file_types, menu);
        } else if(v.getId() == R.id.buttonImportExportToolImportKeys) {
            inflater.inflate(R.menu.keys_file_types, menu);
        }
    }

    /**
     * Saves the selected file type in {@link #mFileType} and continue
     * with the import or export process (depending on {@link #mIsExport}).
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // Handle item selection.
        int id = item.getItemId();
        if (id == R.id.menuDumpFileTypesMct) {
            mFileType = FileType.MCT;
        } else if (id == R.id.menuDumpFileTypesJson) {
            mFileType = FileType.JSON;
        } else if (id == R.id.menuDumpFileTypesBinMfd) {
            mFileType = FileType.BIN;
        } else if (id == R.id.menuDumpFileTypesEml) {
            mFileType = FileType.EML;
        } else if (id == R.id.menuKeysFileTypesKeys) {
            mFileType = FileType.KEYS;
        } else if (id == R.id.menuKeysFileTypesBin) {
            mFileType = FileType.BIN;
        } else {
            return super.onContextItemSelected(item);
        }

        if (mIsExport) {
            // Convert file and export.
            readAndConvertExportData(mFile);
        } else {
            // Let the user pick the file to import.
            showImportFileChooser();
        }
        return true;
    }

    /**
     * Get the file chooser result (one or more files) and continue with
     * the import or export process.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode) {
            case IMPORT_FILE_CHOSEN: // File for importing has been selected.
                if (resultCode == RESULT_OK) {
                    if(data != null ) {
                        Uri[] uris;
                        if(data.getClipData() != null) {
                            // Multiple files where selected.
                            uris = new Uri[data.getClipData().getItemCount()];
                            for(int i = 0; i < data.getClipData().getItemCount(); i++) {
                                uris[i] = data.getClipData().getItemAt(i).getUri();
                            }
                        } else {
                            uris = new Uri[1];
                            uris[0] = data.getData();
                        }
                        readConvertAndSaveImportData(uris);
                    }
                    break;
                }
            case EXPORT_FILE_CHOSEN: // File for exporting has been selected.
                if (resultCode == RESULT_OK) {
                    mFile = data.getStringExtra(FileChooser.EXTRA_CHOSEN_FILE);
                    if (mIsDumpFile) {
                        showDumpFileTypeChooserMenu();
                    } else {
                        showKeysFileTypeChooserMenu();
                    }
                    break;
                }
            case EXPORT_LOCATION_CHOSEN: // Destination for exporting has been chosen.
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    saveConvertedDataToContent(mConvertedContent, uri);
                    break;
                }
            case BACKUP_LOCATION_CHOSEN: // Destination for the backup has been chosen.
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    backupDumpsAndKeys(uri);
                    break;
                }
        }
    }

    /**
     * Start the dump import process by showing the file type chooser
     * menu {@link #showDumpFileTypeChooserMenu()}.
     * @param view The View object that triggered the function
     *             (in this case the import dump button).
     */
    public void onImportDump(View view) {
        mIsExport = false;
        mIsDumpFile = true;
        showDumpFileTypeChooserMenu();
    }

    /**
     * Start the dump export process by showing the dump chooser dialog.
     * @param view The View object that triggered the function
     *             (in this case the export dump button).
     * @see FileChooser
     */
    public void onExportDump(View view) {
        mIsExport = true;
        mIsDumpFile = true;
        Intent intent = new Intent(this, FileChooser.class);
        intent.putExtra(FileChooser.EXTRA_DIR,
                Common.getFile(Common.DUMPS_DIR).getAbsolutePath());
        intent.putExtra(FileChooser.EXTRA_TITLE,
                getString(R.string.text_choose_dump_file));
        intent.putExtra(FileChooser.EXTRA_BUTTON_TEXT,
                getString(R.string.action_export_dump));
        startActivityForResult(intent, EXPORT_FILE_CHOSEN);
    }

    /**
     * Start the keys import process by showing the keys file type chooser
     * menu {@link #showKeysFileTypeChooserMenu()}.
     * @param view The View object that triggered the function
     *             (in this case the import keys button).
     */
    public void onImportKeys(View view) {
        mIsExport = false;
        mIsDumpFile = false;
        showKeysFileTypeChooserMenu();
    }

    /**
     * Start the keys export process by showing the keys chooser dialog.
     * @param view The View object that triggered the function
     *             (in this case the export keys button).
     * @see FileChooser
     */
    public void onExportKeys(View view) {
        mIsExport = true;
        mIsDumpFile = false;
        Intent intent = new Intent(this, FileChooser.class);
        intent.putExtra(FileChooser.EXTRA_DIR,
                Common.getFile(Common.KEYS_DIR).getAbsolutePath());
        intent.putExtra(FileChooser.EXTRA_TITLE,
                getString(R.string.text_choose_key_file));
        intent.putExtra(FileChooser.EXTRA_BUTTON_TEXT,
                getString(R.string.action_export_keys));
        startActivityForResult(intent, EXPORT_FILE_CHOSEN);
    }

    /**
     * Create a full backup of all dump and key files.
     * @param view The View object that triggered the function
     *             (in this case the backup button).
     */
    public void onBackupAll(View view) {
        GregorianCalendar calendar = new GregorianCalendar();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd",
                Locale.getDefault());
        fmt.setCalendar(calendar);
        String dateFormatted = fmt.format(calendar.getTime());
        showExportFileChooser("MCT-Backup_" + dateFormatted + ".zip",
                BACKUP_LOCATION_CHOSEN);
    }

    /**
     * Import the file(s) by reading, converting and saving them.
     * The conversion is made by {@link #convertDump(String[], FileType, FileType)}.
     * @param files The file to read from.
     */
    private void readConvertAndSaveImportData(Uri[] files) {
        String[] content;
        for (Uri file : files) {
            try {
                // Read file.
                if (mFileType != FileType.BIN) {
                    // Read text file (including comments, if it is a key file).
                    content = Common.readUriLineByLine(file, !mIsDumpFile, this);
                } else {
                    // Read binary file.
                    byte[] bytes = Common.readUriRaw(file, this);
                    if (bytes != null) {
                        content = new String[1];
                        // Convert to string, since convert() works only on strings.
                        StringBuilder sb = new StringBuilder();
                        for (byte b : bytes) {
                            sb.append((char) b);
                        }
                        content[0] = sb.toString();
                    } else {
                        content = null;
                    }
                }
                if (content == null) {
                    Toast.makeText(this, R.string.info_error_reading_file,
                            Toast.LENGTH_LONG).show();
                    continue;
                }

                // Prepare file names and paths.
                String fileName = Common.getFileName(file, this);
                if (fileName.contains(".")) {
                    fileName = fileName.substring(0, fileName.lastIndexOf('.'));
                }
                String destFileName = fileName;
                String destPath;

                // Convert key or dump file.
                String[] convertedContent;
                if (mIsDumpFile) {
                    convertedContent = convertDump(
                            content, mFileType, FileType.MCT);
                    destFileName += FileType.MCT.toString();
                    destPath = Common.DUMPS_DIR;
                } else {
                    convertedContent = convertKeys(
                            content, mFileType, FileType.KEYS);
                    // TODO (optional): Remove duplicates.
                    destFileName += FileType.KEYS.toString();
                    destPath = Common.KEYS_DIR;
                }
                if (convertedContent == null) {
                    // Error during conversion.
                    continue;
                }

                // Save converted file.
                File destination = Common.getFile(
                        destPath + "/" + destFileName);
                if (Common.saveFile(destination, convertedContent, false)) {
                    Toast.makeText(this, R.string.info_file_imported,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.info_save_error,
                            Toast.LENGTH_LONG).show();
                    continue;
                }
            } catch (OutOfMemoryError e) {
                Toast.makeText(this, R.string.info_file_to_big,
                        Toast.LENGTH_LONG).show();
                continue;
            }
        }
    }

    /**
     * Export the file by reading, converting and showing the save to dialog.
     * The conversion is made by {@link #convertDump(String[], FileType, FileType)}.
     * @param path The file to read from.
     * @see #showExportFileChooser(String, int)
     * @see #onActivityResult(int, int, Intent)
     */
    private void readAndConvertExportData(String path) {
        File source = new File(path);
        // Include comments in key files that are exported as .keys/.txt/.dic.
        boolean includeComments = !mIsDumpFile  && mFileType == FileType.KEYS;
        String[] content = Common.readFileLineByLine(source, includeComments,this);
        if (content == null) {
            return;
        }

        // Prepare file names and paths.
        String fileName = source.getName();
        if (fileName.contains(".")) {
            fileName = fileName.substring(0, fileName.lastIndexOf('.'));
        }
        String destFileName = fileName + mFileType.toString();

        // Convert key or dump file.
        String[] convertedContent;
        if (mIsDumpFile) {
            convertedContent = convertDump(
                    content, FileType.MCT, mFileType);

        } else {
            convertedContent = convertKeys(
                    content, FileType.KEYS, mFileType);
        }
        if (convertedContent == null) {
            // Error during conversion.
            return;
        }

        // Save converted content and show destination chooser.
        mConvertedContent = convertedContent;
        showExportFileChooser(destFileName, EXPORT_LOCATION_CHOSEN);
    }

    /**
     * Save the converted content with respect to {@link #mFileType} to a given
     * content URI and exit the activity if {@link #mIsCalledWithExportFile} is true.
     * This is only used by the export process.
     * @param convertedContent Converted content (output of
     * {@link #convertDump(String[], FileType, FileType)} or
     * {@link #convertKeys(String[], FileType, FileType)}).
     * @param contentDestination Content URI to the destination where the data
     * should be stored.
     * @see Common#saveFile(Uri, String[], Context)
     */
    private void saveConvertedDataToContent(String[] convertedContent,
                Uri contentDestination) {
        if(convertedContent == null || contentDestination == null) {
            Toast.makeText(this, R.string.info_convert_error,
                    Toast.LENGTH_LONG).show();
            return;
        }
        boolean success;
        if (mFileType != FileType.BIN) {
            success = Common.saveFile(contentDestination, convertedContent, this);
        } else {
            byte[] bytes = new byte[convertedContent[0].length()];
            for (int i = 0; i < convertedContent[0].length(); i++) {
                bytes[i] = (byte) convertedContent[0].charAt(i);
            }
            success = Common.saveFile(contentDestination, bytes, this);
        }
        if (success) {
            Toast.makeText(this, R.string.info_file_exported,
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.info_save_error,
                    Toast.LENGTH_LONG).show();
        }

        if (mIsCalledWithExportFile) {
            // Exit this tool if it was called by another activity with
            // a file to export.
            finish();
        }
    }

    /**
     * Convert dump {@code source} from {@code srcType} to {@code destType}.
     * The formats .mct, .json, .eml and .bin are supported. The
     * intermediate is always JSON. If the {@code srcType} or the
     * {@code destType} is {@link FileType#BIN}, the the
     * {@code source}/return value must be a string array with only
     * one string with each char representing one byte (MSB=0).
     * @param source The data to be converted.
     * @param srcType The type of the {@code source} data.
     * @param destType The type for the return value.
     * @return The converted data. Null on error.
     * @see FileType
     */
    @SuppressLint("DefaultLocale")
    private String[] convertDump(String[] source, FileType srcType,
            FileType destType) {
        if (source == null || srcType == null || destType == null) {
            return null;
        }
        // Convert source to json.
        ArrayList<String> json = new ArrayList<>();
        String block = null;
        if (srcType != FileType.JSON) {
            json.add("{");
            json.add("  \"Created\": \"MifareClassicTool\",");
            json.add("  \"FileType\": \"mfcard\",");
            json.add("  \"blocks\": {");
        }
        switch (srcType) {
            case JSON:
                json = new ArrayList<>(Arrays.asList(source));
                break;
            case MCT:
                int err = Common.isValidDump(source, true);
                if (err != 0) {
                    Common.isValidDumpErrorToast(err, this);
                    return null;
                }
                int sectorNumber;
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
            case BIN:
                String binary = source[0];
                if (binary.length() != 320 && binary.length() != 1024 &&
                        binary.length() != 2048 && binary.length() != 4096) {
                    // Error. Not a complete dump (MIFARE mini, 1k, 2k, 4k).
                    Toast.makeText(this, R.string.info_incomplete_dump,
                            Toast.LENGTH_LONG).show();
                    return null;
                }
                // In this case: chars = bytes. Get 16 bytes and convert.
                for (int i = 0; i < binary.length(); i += 16) {
                    byte[] blockBytes = new byte[16];
                    for (int j = 0; j < 16; j++) {
                        blockBytes[j] = (byte) binary.charAt(i + j);
                    }
                    block = "    \"" + i/16 + "\": \"" +
                            Common.bytes2Hex(blockBytes) + "\",";
                    json.add(block);
                }
                break;
            case EML:
                if (source.length != 20 && source.length != 64 &&
                        source.length != 128 && source.length != 256) {
                    // Error. Not a complete dump (MIFARE mini, 1k, 2k, 4k).
                    Toast.makeText(this, R.string.info_incomplete_dump,
                            Toast.LENGTH_LONG).show();
                    return null;
                }
                for (int i = 0; i < source.length; i++) {
                    if (source[i].equals("")) {
                        // Error. Empty line in .eml file.
                        Toast.makeText(this, R.string.info_incomplete_dump,
                                Toast.LENGTH_LONG).show();
                        return null;
                    }
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

        // Check source conversion.
        if (json.size() <= 6) {
            // Error converting source file.
            Toast.makeText(this, R.string.info_convert_error,
                    Toast.LENGTH_LONG).show();
            return null;
        }

        JSONObject blocks;
        try {
            JSONObject parsedJson = new JSONObject(TextUtils.join("", json));
            blocks = parsedJson.getJSONObject("blocks");
            if (blocks.length() < 1) {
                throw new JSONException("No blocks in source file");
            }
        } catch (JSONException e) {
            // Error parsing json file.
            Toast.makeText(this, R.string.info_convert_error,
                    Toast.LENGTH_LONG).show();
            return null;
        }

        // Convert json to destType.
        String[] dest = null;
        switch (destType) {
            case JSON:
                dest = json.toArray(new String[0]);
                break;
            case MCT:
                ArrayList<String> export = new ArrayList<>();
                Iterator<String> iter = blocks.keys();
                int lastKnownSector = -1;
                while (iter.hasNext()) {
                    String blockKey = iter.next();
                    int blockNr = Integer.parseInt(blockKey);
                    int sector = MCReader.blockToSector(blockNr);
                    if (lastKnownSector != sector) {
                        lastKnownSector = sector;
                        export.add("+Sector: " + sector);
                    }
                    try {
                        block = blocks.getString(blockKey);
                        export.add(block);
                    } catch (JSONException ex) {
                        // Error. This should never happen.
                        continue;
                    }
                }
                dest = export.toArray(new String[0]);
                break;
            case BIN:
                if (blocks.length() != 20 && blocks.length() != 64 &&
                        blocks.length() != 128 && blocks.length() != 256) {
                    // Error. Not a complete dump (MIFARE mini, 1k, 2k, 4k).
                    Toast.makeText(this, R.string.info_incomplete_dump,
                            Toast.LENGTH_LONG).show();
                    return null;
                }
                dest = new String[1];
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < blocks.length(); i++) {
                    try {
                        block = blocks.getString(String.format("%d", i));
                    } catch (JSONException e) {
                        // Error. Not a complete dump (MIFARE mini, 1k, 2k, 4k).
                        Toast.makeText(this, R.string.info_incomplete_dump,
                                Toast.LENGTH_LONG).show();
                        return null;
                    }
                    byte[] bytes = Common.hex2Bytes(block);
                    if (bytes == null) {
                        // Error. Invalid block.
                        Toast.makeText(this, R.string.info_convert_error,
                                Toast.LENGTH_LONG).show();
                        return null;
                    }
                    for (byte b : bytes) {
                        sb.append((char)b);
                    }
                }
                dest[0] = sb.toString();
                break;
            case EML:
                if (blocks.length() != 20 && blocks.length() != 64 &&
                        blocks.length() != 128 && blocks.length() != 256) {
                    // Error. Not a complete dump (MIFARE mini, 1k, 2k, 4k).
                    Toast.makeText(this, R.string.info_incomplete_dump,
                            Toast.LENGTH_LONG).show();
                    return null;
                }
                dest = new String[blocks.length()];
                for (int i = 0; i < blocks.length(); i++) {
                    try {
                        dest[i] = blocks.getString(String.format("%d", i));
                    } catch (JSONException e) {
                        // Error. Not a complete dump (MIFARE mini, 1k, 2k, 4k).
                        Toast.makeText(this, R.string.info_incomplete_dump,
                                Toast.LENGTH_LONG).show();
                        return null;
                    }
                }
        }

        return dest;
    }

    /**
     * Convert keys {@code source} from {@code srcType} to {@code destType}.
     * The formats .keys and .bin are supported. The
     * intermediate is always a String. If the {@code srcType} or the
     * {@code destType} is {@link FileType#BIN}, the the
     * {@code source}/return value must be a string array with only
     * one string with each char representing one byte (MSB=0).
     * @param source The data to be converted.
     * @param srcType The type of the {@code source} data.
     * @param destType The type for the return value.
     * @return The converted data. Null on error.
     * @see FileType
     */
    @SuppressLint("DefaultLocale")
    private String[] convertKeys(String[] source, FileType srcType,
                                 FileType destType) {
        if (source == null || srcType == null || destType == null) {
            return null;
        }
        // Convert source to strings.
        String[] keys = null;
        switch (srcType) {
            case KEYS:
                int err = Common.isValidKeyFile(source);
                if (err != 0) {
                    Common.isValidKeyFileErrorToast(err, this);
                    return null;
                }
                keys = source;
                break;
            case BIN:
                String binary = source[0];
                int len = binary.length();
                if (len > 0 && len % 6 != 0) {
                    // Error. Not multiple of 6 byte.
                    Toast.makeText(this, R.string.info_invalid_key_file,
                            Toast.LENGTH_LONG).show();
                    return null;
                }
                keys = new String[binary.length() / 6];
                // In this case: chars = bytes. Get 6 bytes and convert.
                for (int i = 0; i < len; i += 6) {
                    byte[] keyBytes = new byte[6];
                    for (int j = 0; j < 6; j++) {
                        keyBytes[j] = (byte) binary.charAt(i + j);
                    }
                    keys[i/6] = Common.bytes2Hex(keyBytes);
                }
                break;
        }

        if (keys == null) {
            // Error converting source file.
            Toast.makeText(this, R.string.info_convert_error,
                    Toast.LENGTH_LONG).show();
            return null;
        }

        String[] dest = null;
        switch (destType) {
            case KEYS:
                dest = keys;
                break;
            case BIN:
                dest = new String[1];
                StringBuilder sb = new StringBuilder();
                for (String key : keys) {
                    byte[] bytes = Common.hex2Bytes(key);
                    if (bytes == null) {
                        // Error. Invalid key.
                        Toast.makeText(this, R.string.info_convert_error,
                                Toast.LENGTH_LONG).show();
                        return null;
                    }
                    for (byte b : bytes) {
                        sb.append((char)b);
                    }
                }
                dest[0] = sb.toString();
                break;
        }
        return dest;
    }

    /**
     * Show the "save-as" dialog as provided by Android to let the user chose a
     * destination for exported files.
     * @param fileName The file name of the file to export.
     */
    private void showExportFileChooser(String fileName, int context) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(intent, context);
    }

    /**
     * Show the dump file type chooser menu and save the result in
     * {@link #mFileType}.
     * @see #onContextItemSelected(MenuItem)
     */
    private void showDumpFileTypeChooserMenu() {
        View button = findViewById(R.id.buttonImportExportToolImportDump);
        registerForContextMenu(button);
        openContextMenu(button);
    }

    /**
     * Show the keys file type chooser menu and save the result in
     * {@link #mFileType}.
     * @see #onContextItemSelected(MenuItem)
     */
    private void showKeysFileTypeChooserMenu() {
        View button = findViewById(R.id.buttonImportExportToolImportKeys);
        registerForContextMenu(button);
        openContextMenu(button);
    }

    /**
     * Show Android's generic file chooser and let the user
     * pick the file to import from.
     * @see #onActivityResult(int, int, Intent)
     */
    private void showImportFileChooser() {
        Intent intent = new Intent();
        intent.setType("*/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        String title = getString(R.string.text_select_file);
        startActivityForResult(Intent.createChooser(intent, title), IMPORT_FILE_CHOSEN);
    }

    /**
     * Create a ZIP file containing all keys and dumps and save it to the
     * content URI.
     * @param contentDestUri Content URI to the ZIP file to be saved.
     * @return True is writing the ZIP file succeeded. False otherwise.
     */
    private boolean backupDumpsAndKeys(Uri contentDestUri) {
        final int BUFFER = 2048;
        File[] dirs = new File[2];
        dirs[0] = Common.getFile(Common.KEYS_DIR);
        dirs[1] = Common.getFile(Common.DUMPS_DIR);
        int commonPathLen = Common.getFile("")
                .getAbsolutePath().lastIndexOf("/");
        try {
            OutputStream dest =  getContentResolver().openOutputStream(
                    contentDestUri, "rw");
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
                    dest));
            for (File dir : dirs) {
                File[] fileList = dir.listFiles();
                if (fileList == null || fileList.length == 0) {
                    continue;
                }
                for (File file : fileList) {
                    byte[] data = new byte[BUFFER];
                    FileInputStream fi = new FileInputStream(file);
                    BufferedInputStream origin = new BufferedInputStream(fi, BUFFER);
                    ZipEntry entry = new ZipEntry(
                            file.getAbsolutePath().substring(commonPathLen));
                    entry.setTime(file.lastModified());
                    out.putNextEntry(entry);
                    int count;
                    while ((count = origin.read(data, 0, BUFFER)) != -1) {
                        out.write(data, 0, count);
                    }
                }
            }
            out.close();
        } catch (Exception ex) {
            Toast.makeText(this, R.string.info_backup_error,
                    Toast.LENGTH_LONG).show();
            return false;
        }
        Toast.makeText(this, R.string.info_backup_created,
                Toast.LENGTH_LONG).show();
        return true;
    }

}
