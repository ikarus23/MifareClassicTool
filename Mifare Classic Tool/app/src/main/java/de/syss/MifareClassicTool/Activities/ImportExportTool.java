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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.MCReader;
import de.syss.MifareClassicTool.R;

import static de.syss.MifareClassicTool.Activities.Preferences.Preference.UseInternalStorage;

/**
 * TODO: doc.
 * @author Gerhard Klostermeier
 */
public class ImportExportTool extends BasicActivity {

    private final static int IMPORT_FILE_CHOSEN = 1;
    private final static int EXPORT_FILE_CHOSEN = 2;
    private boolean mIsExport;
    private boolean mIsDumpFile;
    private String mFile;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_export_tool);

    }

    // TODO: doc.
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.file_types, menu);
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
            onExportFile(mFile, true);
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
            case IMPORT_FILE_CHOSEN: // File for importing has been selected.
                if (resultCode == RESULT_OK) {
                    Uri selectedLocation = data.getData();
                    onImportFile(selectedLocation);
                    break;
                }
            case EXPORT_FILE_CHOSEN: // Dump for exporting has been selected.
                if (resultCode == RESULT_OK) {
                    mFile = data.getStringExtra(FileChooser.EXTRA_CHOSEN_FILE);
                    showTypeChooserMenu();
                    break;
                }
        }
    }

    // TODO: doc.
    public void onImportDump(View view) {
        mIsExport = false;
        mIsDumpFile = true;
        showTypeChooserMenu();
    }

    // TODO: doc.
    public void onExportDump(View view) {
        mIsExport = true;
        mIsDumpFile = true;
        if (!Common.getPreferences().getBoolean(UseInternalStorage.toString(),
                false) && !Common.isExternalStorageWritableErrorToast(this)) {
            return;
        }
        Intent intent = new Intent(this, FileChooser.class);
        intent.putExtra(FileChooser.EXTRA_DIR,
                Common.getFileFromStorage(Common.HOME_DIR + "/" +
                        Common.DUMPS_DIR).getAbsolutePath());
        intent.putExtra(FileChooser.EXTRA_TITLE,
                getString(R.string.text_choose_dump_file));
        intent.putExtra(FileChooser.EXTRA_BUTTON_TEXT,
                getString(R.string.action_export_dump));
        startActivityForResult(intent, EXPORT_FILE_CHOSEN);
    }

    // TODO: doc.
    public void onImportKeys(View view) {
        mIsExport = false;
        mIsDumpFile = false;
        showImportFileChooser();
    }

    // TODO: doc.
    private void onImportFile(Uri file) {
        // TODO: reading bin does not work with "LineByLine" functions. Implement readRaw().
        String[] content = null;
        if (mFileType != FileType.BIN) {
            content = Common.readUriLineByLine(file, this);
        } else {
            byte[] bytes = Common.readUriRaw(file, this);
            content = new String[1];
            // Convert to string, since convert() works only on strings.
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append((char)b);
            }
            content[0] = sb.toString();
        }
        if (content == null) {
            Toast.makeText(this, R.string.info_error_reading_file,
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Keys or dump?
        if (mIsDumpFile) {
            // Convert.
            String[] convertedContent = convert(
                    content, mFileType, FileType.MCT);
            if (convertedContent == null) {
                return;
            }
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
            }
        } else {
            // TODO: import key file (just copy).
        }
    }

    // TODO: doc.
    private void onExportFile(String path, boolean isDumpFile) {
        File source = new File(path);
        String fileName = source.getName();
        String[] content = Common.readFileLineByLine(source, false,this);
        if (content == null) {
            return;
        }
        // Key or dump file?
        if (isDumpFile) {
            // Convert.
            String[] convertedContent = convert(
                    content, FileType.MCT, mFileType);
            if (convertedContent == null) {
                return;
            }
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
            // Exporting key files is not supported.
            // (User can just grab the files from the folder or use share.)
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
            case JSON:
                json = new ArrayList<String>(Arrays.asList(source));
                break;
            case MCT:
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
                            Common.byte2HexString(blockBytes) + "\",";
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
                    if (source[i] == "") {
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

        // Convert json to destType.
        JSONObject blocks = null;
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

        String[] dest = null;
        switch (destType) {
            case JSON:
                dest = json.toArray(new String[json.size()]);
                break;
            case MCT:
                // Find highest block number to guess the MIFARE Classic tag size.
                Iterator<String> iter = blocks.keys();
                int maxBlock = -1;
                while (iter.hasNext()) {
                    int blockNr = Integer.parseInt(iter.next());
                    if (blockNr > maxBlock) {
                        maxBlock = blockNr;
                    }
                }
                // Find the next fitting MIFARE Classic tag size.
                if (maxBlock < 20) {
                    maxBlock = 19;
                    dest = new String[20+5];
                } else if (maxBlock < 64) {
                    maxBlock = 63;
                    dest = new String[64+16];
                } else if (maxBlock < 128) {
                    maxBlock = 127;
                    dest = new String[128+32];
                } else if (maxBlock < 256) {
                    maxBlock = 255;
                    dest = new String[256+40];
                }
                // Format blocks to dump in MCT format.
                int j = 0;
                int sector = 0;
                for (int i = 0; i <= maxBlock; i++){
                    if (i < 127 && i % 4 == 0) {
                        dest[j++] = "+Sector: " + sector++;
                    } else if (i >= 128 && i % 16 == 0) {
                        dest[j++] = "+Sector: " + sector++;
                    }
                    try {
                        dest[j] = blocks.getString(String.format("%d", i));
                    } catch (JSONException e) {
                        dest[j] = MCReader.NO_DATA;
                    }
                    j++;
                }
                break;
            case BIN:
                // TODO: Convert json to bin (export).
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

    // TODO: doc.
    private void showTypeChooserMenu() {
        // "button" is just used as a dummy because a context menu
        // always need a view.
        View button = findViewById(R.id.buttonImportExportToolExportDump);
        registerForContextMenu(button);
        openContextMenu(button);
    }

    // TODO: doc.
    private void showImportFileChooser() {
        Intent intent = new Intent();
        intent.setType("*/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        String title = getString(R.string.text_select_file);
        startActivityForResult(Intent.createChooser(intent, title), IMPORT_FILE_CHOSEN);
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
// TODO: Import key file.
// TODO: Add icon for this tool.
// TODO: save isKeyFile or other important vars.