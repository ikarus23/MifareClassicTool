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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.nfc.NfcAdapter;
import android.nfc.tech.MifareClassic;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.R;

/**
 * Main App entry point showing the main menu.
 * Some stuff about the App:
 * <ul>
 * <li>Error/Debug messages (Log.e()/Log.d()) are hard coded</li>
 * <li>This is my first App, so please by decent with me ;)</li>
 * </ul>
 * @author Gerhard Klostermeier
 */
public class MainActivity extends Activity {

    private static final String LOG_TAG =
            MainActivity.class.getSimpleName();

    private final static int FILE_CHOOSER_DUMP_FILE = 1;
    private final static int FILE_CHOOSER_KEY_FILE = 2;
    private AlertDialog mEnableNfc;
    private Button mReadTag;
    private Button mWriteTag;
    private boolean mResume = true;
    private Intent mOldIntent = null;

    /**
     * Check for NFC hardware, Mifare Classic support and for external storage.
     * If the directory structure and the std. keys file is not already there
     * it will be created. Also, at the first run of this App, a warning
     * notice will be displayed.
     * @see #hasStdKeysFile()
     * @see #createStdKeysFile()
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Show App version and footer.
        try {
            String appVersion = getPackageManager().getPackageInfo(
                    getPackageName(), 0).versionName;
            TextView tv = (TextView) findViewById(R.id.textViewMainFooter);
            tv.setText(getString(R.string.app_version) + ": "
                    + appVersion + " - " + getString(R.string.text_footer));
        } catch (NameNotFoundException e) {
            Log.d(LOG_TAG, "Version not found.");
        }

        // Check if there is an NFC hardware component.
        Common.setNfcAdapter(NfcAdapter.getDefaultAdapter(this));
        if (Common.getNfcAdapter() == null) {
            new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_no_nfc_title)
                .setMessage(R.string.dialog_no_nfc)
                .setPositiveButton(R.string.button_exit_app,
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                 })
                 .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                 })
                 .show();
            mResume = false;
            return;
        }

        // Check if there is Mifare support.
        // LOW: Check for MFC support don't work always according to forum
        // posts... Find a better was?!
        if (!getPackageManager().hasSystemFeature("com.nxp.mifare")) {
            new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_no_mfc_title)
                .setMessage(R.string.dialog_no_mfc)
                .setPositiveButton(R.string.button_exit_app,
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                 })
                .show();
            mResume = false;
            return;
        }

        if (Common.isExternalStorageWritableErrorToast(this)) {
            // Create keys directory.
            File path = new File(Environment.getExternalStoragePublicDirectory(
                    Common.HOME_DIR) + Common.KEYS_DIR);
            if (path.exists() == false && !path.mkdirs()) {
                // Could not create directory.
                Log.e(LOG_TAG, "Error while crating '" + Common.HOME_DIR
                        + Common.KEYS_DIR + "' directory.");
                return;
            }

            // Create dumps directory.
            path = new File(Environment.getExternalStoragePublicDirectory(
                    Common.HOME_DIR) + Common.DUMPS_DIR);
            if (path.exists() == false && !path.mkdirs()) {
                // Could not create directory.
                Log.e(LOG_TAG, "Error while crating '" + Common.HOME_DIR
                        + Common.DUMPS_DIR + "' directory.");
                return;
            }

            // Create std. key file if there is none.
            if (!hasStdKeysFile()) {
                createStdKeysFile();
            }
        }

        // Find Read/Write buttons and bind them to member vars.
        mReadTag = (Button) findViewById(R.id.buttonMainReadTag);
        mWriteTag = (Button) findViewById(R.id.buttonMainWriteTag);

        // Create a dialog that send user to NFC settings if NFC is off.
        // (Or let the user use the App in editor only mode / exit the App.)
        mEnableNfc = new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_nfc_not_enabled_title)
            .setMessage(R.string.dialog_nfc_not_enabled)
            .setPositiveButton(R.string.button_nfc,
                    new DialogInterface.OnClickListener() {
                @SuppressLint("InlinedApi")
                public void onClick(DialogInterface dialog, int which) {
                    // Goto NFC Settings.
                    if (Build.VERSION.SDK_INT >= 16) {
                        startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
                    } else {
                        startActivity(new Intent(
                                Settings.ACTION_WIRELESS_SETTINGS));
                    }
                    // Enable read/write tag options.
                    mReadTag.setEnabled(true);
                    mWriteTag.setEnabled(true);
                }
             })
             .setNeutralButton(R.string.button_editor_only,
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // Only use Editor. Do nothing.
                }
             })
             .setNegativeButton(R.string.button_exit_app,
                     new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // Exit the App.
                    finish();
                }
             }).create();

        // Show first usage notice.
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        boolean isFirstRun = sharedPref.getBoolean("is_first_run", true);
        if (isFirstRun) {
            Editor e = sharedPref.edit();
            e.putBoolean("is_first_run", false);
            e.commit();
            new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_first_run_title)
                .setIcon(R.drawable.warning)
                .setMessage(R.string.dialog_first_run)
                .setPositiveButton(R.string.button_ok,
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        mResume = true;
                        checkNfc();
                    }
                 })
                .show();
            mResume = false;
        }
    }

    /**
     * If resuming is allowed because all dependencies from
     * {@link #onCreate(Bundle)} are satisfied, call
     * {@link #checkNfc()}
     * @see #onCreate(Bundle)
     * @see #checkNfc()
     */
    @Override
    public void onResume() {
        super.onResume();

        if (mResume) {
            checkNfc();
        }
    }

    /**
     * Check if NFC adapter is enabled. If not, show the user a dialog and let
     * him choose between "Goto NFC Setting", "Use Editor Only" and "Exit App".
     * Also enable NFC foreground dispatch system.
     * @see Common#enableNfcForegroundDispatch(Activity)
     */
    private void checkNfc() {
        // Check if the NFC hardware is enabled.
        if (Common.getNfcAdapter() != null
                && !Common.getNfcAdapter().isEnabled()) {
            // NFC is disabled. Show dialog.
            mEnableNfc.show();
            // Disable read/write tag options.
            mReadTag.setEnabled(false);
            mWriteTag.setEnabled(false);
            return;
        } else {
            // NFC is enabled. Hide dialog and enable NFC
            // foreground dispatch.
            if (mOldIntent != getIntent()) {
                Common.treatAsNewTag(getIntent(), this);
                mOldIntent = getIntent();
            }
            Common.enableNfcForegroundDispatch(this);
            mEnableNfc.hide();
            mReadTag.setEnabled(true);
            mWriteTag.setEnabled(true);
        }
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
     * Handel new Intent as a new tag Intent.
     * @see Common#treatAsNewTag(Intent, android.content.Context)
     */
    @Override
    public void onNewIntent(Intent intent) {
        Common.treatAsNewTag(intent, this);
    }

    /**
     * Show the {@link ReadTagActivity}.
     * @param view The View object that triggered the method
     * (in this case the read tag button).
     * @see ReadTagActivity
     */
    public void onShowReadTag(View view) {
        Intent intent = new Intent(this, ReadTagActivity.class);
        startActivity(intent);
    }

    /**
     * Show the {@link WriteTagActivity}.
     * @param view The View object that triggered the method
     * (in this case the write tag button).
     * @see WriteTagActivity
     */
    public void onShowWriteTag(View view) {
        Intent intent = new Intent(this, WriteTagActivity.class);
        startActivity(intent);
    }

    /**
     * Show the help Activity.
     * @param view The View object that triggered the method
     * (in this case the help/infos button).
     */
    public void onShowHelp(View view) {
        Intent intent = new Intent(this, HelpActivity.class);
        startActivity(intent);
    }

    /**
     * Open a file chooser ({@link FileChooserActivity}). The
     * Activity result will be processed in
     * {@link #onActivityResult(int, int, Intent)}.
     * If the dump files folder is empty display an additional error
     * message.
     * @param view The View object that triggered the method
     * (in this case the show/edit tag dump button).
     * @see FileChooserActivity
     * @see #onActivityResult(int, int, Intent)
     */
    public void onOpenTagDumpEditor(View view) {
        String dumpsDir = Environment.getExternalStoragePublicDirectory(
                Common.HOME_DIR) + Common.DUMPS_DIR;
        if (Common.isExternalStorageWritableErrorToast(this)) {
            File file = new File(dumpsDir);
            if (file.listFiles() == null || file.listFiles().length == 0) {
                Toast.makeText(this, R.string.info_no_dumps,
                    Toast.LENGTH_LONG).show();
            }
            Intent intent = new Intent(this, FileChooserActivity.class);
            intent.putExtra(FileChooserActivity.EXTRA_DIR, dumpsDir);
            intent.putExtra(FileChooserActivity.EXTRA_TITLE,
                    getString(R.string.text_open_dump_title));
            intent.putExtra(FileChooserActivity.EXTRA_BUTTON_TEXT,
                    getString(R.string.button_open_dump_file));
            startActivityForResult(intent, FILE_CHOOSER_DUMP_FILE);
        }
    }

    /**
     * Open a file chooser ({@link FileChooserActivity}). The
     * Activity result will be processed in
     * {@link #onActivityResult(int, int, Intent)}.
     * @param view The View object that triggered the method
     * (in this case the show/edit key button).
     * @see FileChooserActivity
     * @see #onActivityResult(int, int, Intent)
     */
    public void onOpenKeyEditor(View view) {
        Intent intent = new Intent(this, FileChooserActivity.class);
        intent.putExtra(FileChooserActivity.EXTRA_DIR,
                Environment.getExternalStoragePublicDirectory(
                        Common.HOME_DIR) + Common.KEYS_DIR);
        intent.putExtra(FileChooserActivity.EXTRA_TITLE,
                getString(R.string.text_open_key_file_title));
        intent.putExtra(FileChooserActivity.EXTRA_BUTTON_TEXT,
                getString(R.string.button_open_key_file));
        intent.putExtra(FileChooserActivity.EXTRA_ENABLE_NEW_FILE_BUTTON, true);
        startActivityForResult(intent, FILE_CHOOSER_KEY_FILE);
    }

    /**
     * Run the {@link DumpEditorActivity} or the {@link KeyEditorActivity}
     * if file chooser result is O.K.
     * @see DumpEditorActivity
     * @see KeyEditorActivity
     * @see #onOpenTagDumpEditor(View)
     * @see #onOpenKeyEditor(View)
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
        case FILE_CHOOSER_DUMP_FILE:
            if (resultCode == Activity.RESULT_OK) {
                Intent intent = new Intent(this, DumpEditorActivity.class);
                intent.putExtra(FileChooserActivity.EXTRA_CHOSEN_FILE,
                        data.getStringExtra(
                                FileChooserActivity.EXTRA_CHOSEN_FILE));
                startActivity(intent);
            }
            break;
        case FILE_CHOOSER_KEY_FILE:
            if (resultCode == Activity.RESULT_OK) {
                Intent intent = new Intent(this, KeyEditorActivity.class);
                intent.putExtra(FileChooserActivity.EXTRA_CHOSEN_FILE,
                        data.getStringExtra(
                                FileChooserActivity.EXTRA_CHOSEN_FILE));
                startActivity(intent);
            }
            break;
        }
    }

    /**
     * Create a standard key file ({@link Common#STD_KEYS}) in
     * {@link Common#KEYS_DIR}. This file contains some std. Mifare keys:
     * <ul>
     * <li>0xFFFFFFFFFFFF - Unformatted, factory fresh tags.</li>
     * <li>0xA0A1A2A3A4A5 - First sector of the tag (Mifare MAD).</li>
     * <li>0xD3F7D3F7D3F7 - All other sectors.</li>
     * <li>Others from {@link Common#SOME_CLASSICAL_KNOWN_KEYS}.</li>
     * </ul>
     * The file is a simple text file, any plain text editor will do the trick.
     * Data from this App are stored in
     * getExternalStoragePublicDirectory(Common.HOME_DIR) to remain
     * there after App uninstallation.
     * @see Common#SOME_CLASSICAL_KNOWN_KEYS
     * @see Common#KEYS_DIR
     * @see Common#HOME_DIR
     */
    private void createStdKeysFile() {
        // Create std. keys file.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Common.HOME_DIR) + Common.KEYS_DIR, Common.STD_KEYS);
        String[] lines = new String[Common.SOME_CLASSICAL_KNOWN_KEYS.length+4];
        lines[0] = "# " + getString(R.string.text_std_keys_comment);
        lines[1] = Common.byte2HexString(MifareClassic.KEY_DEFAULT);
        lines[2] = Common.byte2HexString(
                MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY);
        lines[3] = Common.byte2HexString(MifareClassic.KEY_NFC_FORUM);
        System.arraycopy(Common.SOME_CLASSICAL_KNOWN_KEYS, 0,
                lines, 4, Common.SOME_CLASSICAL_KNOWN_KEYS.length);
        Common.saveFile(file, lines);
    }

    /**
     * Check if there is a {@link Common#STD_KEYS} file
     * in {@link Common#HOME_DIR}/{@link Common#KEYS_DIR}.
     * @return True if there is such a file, False otherwise.
     */
    private boolean hasStdKeysFile() {
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Common.HOME_DIR) + Common.KEYS_DIR, Common.STD_KEYS);
        return file.exists();
    }

}
