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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
    private final static int FILE_CHOOSER_DUMP_FILE2 = 3;
    private String tempfile = null;
    private AlertDialog mEnableNfc;
    private Button mReadTag;
    private Button mWriteTag;
    private boolean mResume = true;
    private Intent mOldIntent = null;

    /**
     * Check for NFC hardware, Mifare Classic support and for external storage.
     * If the directory structure and the std. keys files is not already there
     * it will be created. Also, at the first run of this App, a warning
     * notice will be displayed.
     * @see #copyStdKeysFilesIfNecessary()
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Show App version and footer.
        TextView tv = (TextView) findViewById(R.id.textViewMainFooter);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        try {
            String appVersion = getPackageManager().getPackageInfo(
                    getPackageName(), 0).versionName;
            tv.setText(TextUtils.concat(getString(R.string.app_version), ": ",
                    appVersion, " - ", getText(R.string.text_footer)));
        } catch (NameNotFoundException e) {
            Log.d(LOG_TAG, "Version not found.");
        }

        // Add the context menu to the tools button.
        Button tools = (Button) findViewById(R.id.buttonMainTools);
        registerForContextMenu(tools);

        // Check if there is an NFC hardware component.
        Common.setNfcAdapter(NfcAdapter.getDefaultAdapter(this));
        if (Common.getNfcAdapter() == null) {
            new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_no_nfc_title)
                .setMessage(R.string.dialog_no_nfc)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.action_exit_app,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                 })
                 .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
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
                    Common.HOME_DIR) + "/" + Common.KEYS_DIR);
            if (path.exists() == false && !path.mkdirs()) {
                // Could not create directory.
                Log.e(LOG_TAG, "Error while crating '" + Common.HOME_DIR
                        + "/" + Common.KEYS_DIR + "' directory.");
                return;
            }

            // Create dumps directory.
            path = new File(Environment.getExternalStoragePublicDirectory(
                    Common.HOME_DIR) + "/" + Common.DUMPS_DIR);
            if (path.exists() == false && !path.mkdirs()) {
                // Could not create directory.
                Log.e(LOG_TAG, "Error while crating '" + Common.HOME_DIR
                        + "/" + Common.DUMPS_DIR + "' directory.");
                return;
            }

            // Create tmp directory.
            path = new File(Environment.getExternalStoragePublicDirectory(
                    Common.HOME_DIR) + "/" + Common.TMP_DIR);
            if (path.exists() == false && !path.mkdirs()) {
                // Could not create directory.
                Log.e(LOG_TAG, "Error while crating '" + Common.HOME_DIR
                        + Common.TMP_DIR + "' directory.");
                return;
            }
            // Clean up tmp directory.
            for (File file : path.listFiles()) {
                file.delete();
            }

            // Create std. key file if there is none.
            copyStdKeysFilesIfNecessary();
        }

        // Find Read/Write buttons and bind them to member vars.
        mReadTag = (Button) findViewById(R.id.buttonMainReadTag);
        mWriteTag = (Button) findViewById(R.id.buttonMainWriteTag);

        // Create a dialog that send user to NFC settings if NFC is off.
        // (Or let the user use the App in editor only mode / exit the App.)
        mEnableNfc = new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_nfc_not_enabled_title)
            .setMessage(R.string.dialog_nfc_not_enabled)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton(R.string.action_nfc,
                    new DialogInterface.OnClickListener() {
                @Override
                @SuppressLint("InlinedApi")
                public void onClick(DialogInterface dialog, int which) {
                    // Goto NFC Settings.
                    if (Build.VERSION.SDK_INT >= 16) {
                        startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
                    } else {
                        startActivity(new Intent(
                                Settings.ACTION_WIRELESS_SETTINGS));
                    }
                }
             })
             .setNeutralButton(R.string.action_editor_only,
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Only use Editor. Do nothing.
                }
             })
             .setNegativeButton(R.string.action_exit_app,
                     new DialogInterface.OnClickListener() {
                @Override
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
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(R.string.dialog_first_run)
                .setPositiveButton(R.string.action_ok,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
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
     * Add the menu with the tools.
     * It will be shown if the user clicks on "Tools".
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        menu.setHeaderTitle(R.string.dialog_tools_menu_title);
        menu.setHeaderIcon(android.R.drawable.ic_menu_preferences);
        inflater.inflate(R.menu.tools, menu);
        // Enable/Disable tag info tool depending on NFC availability.
        menu.findItem(R.id.menuMainTagInfo).setEnabled(
                Common.getNfcAdapter() != null
                && Common.getNfcAdapter().isEnabled());
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
                int typeCheck = Common.treatAsNewTag(getIntent(), this);
                if (typeCheck == -1 || typeCheck == -2) {
                    // Device or tag does not support Mifare Classic.
                    // Run the only thing that is possible: The tag info tool.
                    Intent i = new Intent(this, TagInfoToolActivity.class);
                    startActivity(i);
                }
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
     * Handle new Intent as a new tag Intent and if the tag/device does not
     * support Mifare Classic, then run {@link TagInfoToolActivity}.
     * @see Common#treatAsNewTag(Intent, android.content.Context)
     * @see TagInfoToolActivity
     */
    @Override
    public void onNewIntent(Intent intent) {
        int typeCheck = Common.treatAsNewTag(intent, this);
        if (typeCheck == -1 || typeCheck == -2) {
            // Device or tag does not support Mifare Classic.
            // Run the only thing that is possible: The tag info tool.
            Intent i = new Intent(this, TagInfoToolActivity.class);
            startActivity(i);
        }
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
     * (in this case the help/info button).
     */
    public void onShowHelp(View view) {
        Intent intent = new Intent(this, HelpActivity.class);
        startActivity(intent);
    }

    /**
     * Show the tools menu (as context menu).
     * @param view The View object that triggered the method
     * (in this case the tools button).
     */
    public void onShowTools(View view) {
        openContextMenu(view);
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
                Common.HOME_DIR) + "/" + Common.DUMPS_DIR;
        if (Common.isExternalStorageWritableErrorToast(this)) {
            File file = new File(dumpsDir);
            if (file.isDirectory() && (file.listFiles() == null
                    || file.listFiles().length == 0)) {
                Toast.makeText(this, R.string.info_no_dumps,
                    Toast.LENGTH_LONG).show();
            }
            Intent intent = new Intent(this, FileChooserActivity.class);
            intent.putExtra(FileChooserActivity.EXTRA_DIR, dumpsDir);
            intent.putExtra(FileChooserActivity.EXTRA_TITLE,
                    getString(R.string.text_open_dump_title));
            intent.putExtra(FileChooserActivity.EXTRA_BUTTON_TEXT,
                    getString(R.string.action_open_dump_file));
            intent.putExtra(FileChooserActivity.EXTRA_ENABLE_DELETE_FILE, true);
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
        if (Common.isExternalStorageWritableErrorToast(this)) {
            Intent intent = new Intent(this, FileChooserActivity.class);
            intent.putExtra(FileChooserActivity.EXTRA_DIR,
                    Environment.getExternalStoragePublicDirectory(
                            Common.HOME_DIR) + "/" + Common.KEYS_DIR);
            intent.putExtra(FileChooserActivity.EXTRA_TITLE,
                    getString(R.string.text_open_key_file_title));
            intent.putExtra(FileChooserActivity.EXTRA_BUTTON_TEXT,
                    getString(R.string.action_open_key_file));
            intent.putExtra(FileChooserActivity.EXTRA_ENABLE_NEW_FILE, true);
            intent.putExtra(FileChooserActivity.EXTRA_ENABLE_DELETE_FILE, true);
            startActivityForResult(intent, FILE_CHOOSER_KEY_FILE);
        }
    }

    /**
     * Handle (start) the selected tool from the tools menu.
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Intent intent = null;
        Intent intent2 = null;
        switch (item.getItemId()) {
        case R.id.menuMainTagInfo:
            intent = new Intent(this, TagInfoToolActivity.class);
            startActivity(intent);
            return true;
        case R.id.menuMainValueBlockTool:
            intent = new Intent(this, ValueBlockToolActivity.class);
            startActivity(intent);
            return true;
        case R.id.menuMainAccessConditionTool:
            intent = new Intent(this, AccessConditionTool.class);
            startActivity(intent);
            return true;
            case R.id.menuMainCompareTool:
                String dumpsDir = Environment.getExternalStoragePublicDirectory(
                        Common.HOME_DIR) + "/" + Common.DUMPS_DIR;
                if (Common.isExternalStorageWritableErrorToast(this)) {
                    File file = new File(dumpsDir);
                    if (file.isDirectory() && (file.listFiles() == null
                            || file.listFiles().length == 0)) {
                        Toast.makeText(this, R.string.info_no_dumps,
                                Toast.LENGTH_LONG).show();
                    }
                    //Now we test the second file operand
                    intent2 = new Intent(this, FileChooserActivity.class);
                    intent2.putExtra(FileChooserActivity.EXTRA_DIR, dumpsDir);
                    intent2.putExtra(FileChooserActivity.EXTRA_TITLE,
                            getString(R.string.text_select_second_file_title));
                    intent2.putExtra(FileChooserActivity.EXTRA_BUTTON_TEXT,
                            getString(R.string.action_compare_files));
                    intent2.putExtra(FileChooserActivity.EXTRA_ENABLE_DELETE_FILE, true);
                    startActivityForResult(intent2, FILE_CHOOSER_DUMP_FILE2);
                    //First file selection
                    intent = new Intent(this, FileChooserActivity.class);
                    intent.putExtra(FileChooserActivity.EXTRA_DIR, dumpsDir);
                    intent.putExtra(FileChooserActivity.EXTRA_TITLE,
                            getString(R.string.text_select_first_file_title));
                    intent.putExtra(FileChooserActivity.EXTRA_BUTTON_TEXT,
                            getString(R.string.action_select_file));
                    intent.putExtra(FileChooserActivity.EXTRA_ENABLE_DELETE_FILE, true);
                    startActivityForResult(intent, FILE_CHOOSER_DUMP_FILE2);
                }
            default:
                return super.onContextItemSelected(item);
        }
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
            case FILE_CHOOSER_DUMP_FILE2:
                if (resultCode == Activity.RESULT_OK) {
                    if (tempfile == null) {
                        tempfile = data.getStringExtra(
                                FileChooserActivity.EXTRA_CHOSEN_FILE);
                    } else {
                        Intent intent = new Intent(this, CompareActivity.class);
                        intent.putExtra("FileOne", tempfile);
                        this.tempfile = null;
                        intent.putExtra("FileTwo", data.getStringExtra(
                                FileChooserActivity.EXTRA_CHOSEN_FILE));
                        startActivity(intent);
                    }
                }
                break;
        }
    }

    /**
     * Copy the standard key files ({@link Common#STD_KEYS} and
     * {@link Common#STD_KEYS_EXTENDED}) form assets to {@link Common#KEYS_DIR}.
     * Key files are simple text files. Any plain text editor will do the trick.
     * All key and dump data from this App is stored in
     * getExternalStoragePublicDirectory(Common.HOME_DIR) to remain
     * there after App uninstallation.
     * @see Common#KEYS_DIR
     * @see Common#HOME_DIR
     * @see Common#copyFile(InputStream, OutputStream)
     */
    private void copyStdKeysFilesIfNecessary() {
        File std = new File(Environment.getExternalStoragePublicDirectory(
                Common.HOME_DIR) + "/" + Common.KEYS_DIR, Common.STD_KEYS);
        File extended = new File(Environment.getExternalStoragePublicDirectory(
                Common.HOME_DIR) + "/" + Common.KEYS_DIR,
                Common.STD_KEYS_EXTENDED);
        AssetManager assetManager = getAssets();

        if (!std.exists()) {
            // Copy std.keys.
            try {
                InputStream in = assetManager.open(
                        Common.KEYS_DIR + "/" + Common.STD_KEYS);
                OutputStream out = new FileOutputStream(std);
                Common.copyFile(in, out);
                in.close();
                out.flush();
                out.close();
              } catch(IOException e) {
                  Log.e(LOG_TAG, "Error while copying 'std.keys' from assets "
                          + "to external storage.");
              }
        }
        if (!extended.exists()) {
            // Copy extended-std.keys.
            try {
                InputStream in = assetManager.open(
                        Common.KEYS_DIR + "/" + Common.STD_KEYS_EXTENDED);
                OutputStream out = new FileOutputStream(extended);
                Common.copyFile(in, out);
                in.close();
                out.flush();
                out.close();
              } catch(IOException e) {
                  Log.e(LOG_TAG, "Error while copying 'extended-std.keys' "
                          + "from assets to external storage.");
              }
        }
    }
}
