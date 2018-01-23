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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.R;

import static de.syss.MifareClassicTool.Activities.Preferences.Preference.UseInternalStorage;

/**
 * Main App entry point showing the main menu.
 * Some stuff about the App:
 * <ul>
 * <li>Error/Debug messages (Log.e()/Log.d()) are hard coded</li>
 * <li>This is my first App, so please by decent with me ;)</li>
 * </ul>
 * @author Gerhard Klostermeier
 */
public class MainMenu extends Activity {

    private static final String LOG_TAG =
            MainMenu.class.getSimpleName();

    private final static int FILE_CHOOSER_DUMP_FILE = 1;
    private final static int FILE_CHOOSER_KEY_FILE = 2;
    private static final int REQUEST_WRITE_STORAGE_CODE = 1;
    private boolean mDonateDialogWasShown = false;
    private Button mReadTag;
    private Button mWriteTag;
    private Button mKeyEditor;
    private Button mDumpEditor;
    private Intent mOldIntent = null;

    /**
     * Nodes (stats) MCT passes through during its startup.
     */
    private enum StartUpNode {
        FirstUseDialog, DonateDialog, HasNfc, HasMifareClassicSupport,
        HasNfcEnabled, HasExternalNfc, ExternalNfcServiceRunning,
        HandleNewIntent
    }

    /**
     * Check for NFC hardware, MIFARE Classic support and for external storage.
     * If the directory structure and the std. keys files is not already there
     * it will be created. Also, at the first run of this App, a warning
     * notice and a donate message will be displayed.
     * @see #copyStdKeysFilesIfNecessary()
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        // Show App version and footer.
        TextView tv = (TextView) findViewById(R.id.textViewMainFooter);
        tv.setText(getString(R.string.app_version)
                + ": " + Common.getVersionCode());

        // Add the context menu to the tools button.
        Button tools = (Button) findViewById(R.id.buttonMainTools);
        registerForContextMenu(tools);

        // Bind main layout buttons.
        mReadTag = (Button) findViewById(R.id.buttonMainReadTag);
        mWriteTag = (Button) findViewById(R.id.buttonMainWriteTag);
        mKeyEditor = (Button) findViewById(R.id.buttonMainEditKeyDump);
        mDumpEditor = (Button) findViewById(R.id.buttonMainEditCardDump);

        // Check if the user granted the app write permissions.
        if (Common.hasWritePermissionToExternalStorage(this)) {
            initFolders();
        } else {
            // Request the permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE_CODE);
        }
    }

    /**
     * Each phase of the MCTs startup is called "node" (see {@link StartUpNode})
     * and can be started by this function. The following nodes will be
     * started automatically (e.g. if the "has NFC support?" node is triggered
     * the "has MIFARE classic support?" node will be run automatically
     * after that).
     * @param startUpNode The node of the startup checks chain.
     * @see StartUpNode
     */
    private void runSartUpNode(StartUpNode startUpNode) {
        SharedPreferences sharedPref =
                getPreferences(Context.MODE_PRIVATE);
        Editor sharedEditor = sharedPref.edit();
        switch (startUpNode) {
            case FirstUseDialog:
                boolean isFirstRun = sharedPref.getBoolean(
                        "is_first_run", true);
                if (isFirstRun) {
                    createFirstUseDialog().show();
                } else {
                    runSartUpNode(StartUpNode.HasNfc);
                }
                break;
            case HasNfc:
                Common.setNfcAdapter(NfcAdapter.getDefaultAdapter(this));
                if (Common.getNfcAdapter() == null) {
                    runSartUpNode(StartUpNode.HasExternalNfc);
                } else {
                    runSartUpNode(StartUpNode.HasMifareClassicSupport);
                }
                break;
            case HasMifareClassicSupport:
                if (!Common.hasMifareClassicSupport()
                        && !Common.useAsEditorOnly()) {
                    AlertDialog ad = createHasNoMifareClassicSupportDialog();
                    ad.show();
                    // Make links clickable.
                    ((TextView) ad.findViewById(android.R.id.message))
                            .setMovementMethod(
                                    LinkMovementMethod.getInstance());
                } else {
                    runSartUpNode(StartUpNode.HasNfcEnabled);
                }
                break;
            case HasNfcEnabled:
                Common.setNfcAdapter(NfcAdapter.getDefaultAdapter(this));
                if (!Common.getNfcAdapter().isEnabled()) {
                    if (!Common.useAsEditorOnly()) {
                        createNfcEnableDialog().show();
                    } else {
                        runSartUpNode(StartUpNode.DonateDialog);
                    }
                } else {
                    // NOTE: Use MCT with internal NFC controller.
                    useAsEditorOnly(false);
                    Common.enableNfcForegroundDispatch(this);
                    runSartUpNode(StartUpNode.DonateDialog);
                }
                break;
            case HasExternalNfc:
                if (!Common.hasExternalNfcInstalled(this)
                        && !Common.useAsEditorOnly()) {
                    createInstallExternalNfcDialog().show();
                } else {
                    runSartUpNode(StartUpNode.ExternalNfcServiceRunning);
                }
                break;
            case ExternalNfcServiceRunning:
                if (!Common.isExternalNfcServiceRunning(this)) {
                    if (!Common.useAsEditorOnly()) {
                        createStartExternalNfcServiceDialog().show();
                    } else {
                        runSartUpNode(StartUpNode.DonateDialog);
                    }
                } else {
                    // NOTE: Use MCT with External NFC.
                    useAsEditorOnly(false);
                    runSartUpNode(StartUpNode.DonateDialog);
                }
                break;
            case DonateDialog:
                if (Common.IS_DONATE_VERSION) {
                    runSartUpNode(StartUpNode.HandleNewIntent);
                    break;
                }
                if (mDonateDialogWasShown) {
                    runSartUpNode(StartUpNode.HandleNewIntent);
                    break;
                }
                int currentVersion = 0;
                try {
                    currentVersion = getPackageManager().getPackageInfo(
                            getPackageName(), 0).versionCode;
                } catch (NameNotFoundException e) {
                    Log.d(LOG_TAG, "Version not found.");
                }
                int lastVersion = sharedPref.getInt("mct_version",
                        currentVersion - 1);
                boolean showDonateDialog = sharedPref.getBoolean(
                        "show_donate_dialog", true);

                if (lastVersion < currentVersion || showDonateDialog) {
                    // This is either a new version of MCT or the user
                    // wants to see the donate dialog.
                    if (lastVersion < currentVersion) {
                        // Update the version.
                        sharedEditor.putInt("mct_version", currentVersion);
                        sharedEditor.putBoolean("show_donate_dialog", true);
                        sharedEditor.apply();
                    }
                    createDonateDialog().show();
                    mDonateDialogWasShown = true;
                } else {
                    runSartUpNode(StartUpNode.HandleNewIntent);
                }
                break;
            case HandleNewIntent:
                Common.setPendingComponentName(null);
                Intent intent = getIntent();
                if (intent != null) {
                    boolean isIntentWithTag = intent.getAction().equals(
                            NfcAdapter.ACTION_TECH_DISCOVERED);
                    if (isIntentWithTag && intent != mOldIntent) {
                        // If MCT was called by another app or the dispatch
                        // system with a tag delivered by intent, handle it as
                        // new tag intent.
                        mOldIntent = intent;
                        onNewIntent(getIntent());
                    } else {
                        // Last node. Do nothing.
                        break;
                    }
                }
                break;
        }
    }

    /**
     * Set whether to use the app in editor only mode or not.
     * @param useAsEdtiorOnly True if the app should be used in editor
     * only mode.
     */
    private void useAsEditorOnly(boolean useAsEdtiorOnly) {
        Common.setUseAsEditorOnly(useAsEdtiorOnly);
        mReadTag.setEnabled(!useAsEdtiorOnly);
        mWriteTag.setEnabled(!useAsEdtiorOnly);
    }

    /**
     * Create the dialog which is displayed once the app was started for the
     * first time. After showing the dialog, {@link #runSartUpNode(StartUpNode)}
     * with {@link StartUpNode#HasNfc} will be called.
     * @return The created alert dialog.
     * @see #runSartUpNode(StartUpNode)
     */
    private AlertDialog createFirstUseDialog() {
        return new AlertDialog.Builder(this)
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
                .setOnCancelListener(
                new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        SharedPreferences sharedPref =
                                getPreferences(Context.MODE_PRIVATE);
                        Editor sharedEditor = sharedPref.edit();
                        sharedEditor.putBoolean("is_first_run", false);
                        sharedEditor.apply();
                        // Continue with "has NFC" check.
                        runSartUpNode(StartUpNode.HasNfc);
                    }
                })
                .create();
    }

    /**
     * Create the dialog which is displayed if the device does not have
     * MIFARE classic support. After showing the dialog,
     * {@link #runSartUpNode(StartUpNode)} with {@link StartUpNode#DonateDialog}
     * will be called or the app will be exited.
     * @return The created alert dialog.
     * @see #runSartUpNode(StartUpNode)
     */
    private AlertDialog createHasNoMifareClassicSupportDialog() {
        CharSequence styledText = Html.fromHtml(
                getString(R.string.dialog_no_mfc_support_device));
        return new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_no_mfc_support_device_title)
                .setMessage(styledText)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.action_exit_app,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton(R.string.action_editor_only,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        useAsEditorOnly(true);
                        runSartUpNode(StartUpNode.DonateDialog);
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                })
                .create();
    }

    /**
     * Create a dialog that send user to NFC settings if NFC is off.
     * Alternatively the user can chose to use the App in editor only
     * mode or exit the App.
     * @return The created alert dialog.
     * @see #runSartUpNode(StartUpNode)
     */
    private AlertDialog createNfcEnableDialog() {
        return new AlertDialog.Builder(this)
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
                            startActivity(new Intent(
                                    Settings.ACTION_NFC_SETTINGS));
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
                        // Only use Editor.
                        useAsEditorOnly(true);
                        runSartUpNode(StartUpNode.DonateDialog);
                    }
                })
                .setNegativeButton(R.string.action_exit_app,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // Exit the App.
                        finish();
                    }
                })
                .setOnCancelListener(
                new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                })
                .create();
    }

    /**
     * Create the dialog which is displayed if the device has not "External NFC"
     * installed. After showing the dialog, {@link #runSartUpNode(StartUpNode)}
     * with {@link StartUpNode#DonateDialog} will be called or MCT will
     * redirect the user to the play store page of "External NFC"  or
     * the app will be exited.
     * @return The created alert dialog.
     * @see #runSartUpNode(StartUpNode)
     */
    private AlertDialog createInstallExternalNfcDialog() {
        return new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_no_nfc_support_title)
                .setMessage(R.string.dialog_no_nfc_support)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.action_install_external_nfc,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Open Google Play for the donate version of MCT.
                        Uri uri = Uri.parse(
                                "market://details?id=eu.dedb.nfc.service");
                        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                        try {
                            startActivity(goToMarket);
                        } catch (ActivityNotFoundException e) {
                            startActivity(new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store"
                                            + "/apps/details?id=eu.dedb.nfc"
                                            + ".service")));
                        }
                    }
                })
                .setNeutralButton(R.string.action_editor_only,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Only use Editor.
                        useAsEditorOnly(true);
                        runSartUpNode(StartUpNode.DonateDialog);
                    }
                })
                .setNegativeButton(R.string.action_exit_app,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // Exit the App.
                        finish();
                    }
                })
                .setOnCancelListener(
                new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                })
                .create();
    }

    /**
     * Create the dialog which is displayed if the "External NFC" service is
     * not running. After showing the dialog,
     * {@link #runSartUpNode(StartUpNode)} with {@link StartUpNode#DonateDialog}
     * will be called or MCT will redirect the user to the settings of
     * "External NFC" or the app will be exited.
     * @return The created alert dialog.
     * @see #runSartUpNode(StartUpNode)
     */
    private AlertDialog createStartExternalNfcServiceDialog() {
        final Context context = this;
        return new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_start_external_nfc_title)
                .setMessage(R.string.dialog_start_external_nfc)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.action_start_external_nfc,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        useAsEditorOnly(true);
                        Common.openApp(context, "eu.dedb.nfc.service");
                    }
                })
                .setNeutralButton(R.string.action_editor_only,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Only use Editor.
                        useAsEditorOnly(true);
                        runSartUpNode(StartUpNode.DonateDialog);
                    }
                })
                .setNegativeButton(R.string.action_exit_app,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // Exit the App.
                        finish();
                    }
                })
                .setOnCancelListener(
                new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                })
                .create();
    }

    /**
     * Create the donate dialog. After showing the dialog,
     * {@link #runSartUpNode(StartUpNode)} with
     * {@link StartUpNode#HandleNewIntent} will be called.
     * @return The created alert dialog.
     * @see #runSartUpNode(StartUpNode)
     */
    private AlertDialog createDonateDialog() {
        View dialogLayout = getLayoutInflater().inflate(
                R.layout.dialog_donate,
                (ViewGroup)findViewById(android.R.id.content), false);
        TextView textView = (TextView) dialogLayout.findViewById(
                R.id.textViewDonateDialog);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        final CheckBox showDonateDialogCheckBox = (CheckBox) dialogLayout
                .findViewById(R.id.checkBoxDonateDialog);
        return new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_donate_title)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(dialogLayout)
                .setPositiveButton(R.string.action_ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setOnCancelListener(
                new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        if (showDonateDialogCheckBox.isChecked()) {
                            // Do not show the donate dialog again.
                            SharedPreferences sharedPref =
                                    getPreferences(Context.MODE_PRIVATE);
                            Editor sharedEditor = sharedPref.edit();
                            sharedEditor.putBoolean(
                                    "show_donate_dialog", false);
                            sharedEditor.apply();
                        }
                        runSartUpNode(StartUpNode.HandleNewIntent);
                    }
                })
                .create();
    }

    /**
     * Create the directories needed by MCT and clean out the tmp folder.
     */
    @SuppressLint("ApplySharedPref")
    private void initFolders() {
        boolean isUseInternalStorage = Common.getPreferences().getBoolean(
                UseInternalStorage.toString(), false);

        // Run twice and init the folders on the internal and external storage.
        for (int i = 0; i < 2; i++) {
            if (!isUseInternalStorage &&
                    !Common.isExternalStorageWritableErrorToast(this)) {
                continue;
            }

            // Create keys directory.
            File path = Common.getFileFromStorage(
                    Common.HOME_DIR + "/" + Common.KEYS_DIR);

            if (!path.exists() && !path.mkdirs()) {
                // Could not create directory.
                Log.e(LOG_TAG, "Error while creating '" + Common.HOME_DIR
                        + "/" + Common.KEYS_DIR + "' directory.");
                return;
            }

            // Create dumps directory.
            path = Common.getFileFromStorage(
                    Common.HOME_DIR + "/" + Common.DUMPS_DIR);
            if (!path.exists() && !path.mkdirs()) {
                // Could not create directory.
                Log.e(LOG_TAG, "Error while creating '" + Common.HOME_DIR
                        + "/" + Common.DUMPS_DIR + "' directory.");
                return;
            }

            // Create tmp directory.
            path = Common.getFileFromStorage(
                    Common.HOME_DIR + "/" + Common.TMP_DIR);
            if (!path.exists() && !path.mkdirs()) {
                // Could not create directory.
                Log.e(LOG_TAG, "Error while creating '" + Common.HOME_DIR
                        + Common.TMP_DIR + "' directory.");
                return;
            }
            // Try to clean up tmp directory.
            File[] tmpFiles = path.listFiles();
            if (tmpFiles != null) {
                for (File file : tmpFiles) {
                    file.delete();
                }
            }

            // Create std. key file if there is none.
            copyStdKeysFilesIfNecessary();

            // Change the storage for the second run.
            Common.getPreferences().edit().putBoolean(
                    UseInternalStorage.toString(),
                    !isUseInternalStorage).commit();
        }
        // Restore the storage preference.
        Common.getPreferences().edit().putBoolean(
                UseInternalStorage.toString(),
                isUseInternalStorage).commit();

    }

    /**
     * Add a menu with "preferences", "about", etc. to the Activity.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.general_options, menu);
        return true;
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
                !Common.useAsEditorOnly());
        // Enable/Disable diff tool depending on write permissions.
        menu.findItem(R.id.menuMainDiffTool).setEnabled(
                Common.hasWritePermissionToExternalStorage(this));
    }

    /**
     * Resume by triggering MCT's startup system
     * ({@link #runSartUpNode(StartUpNode)}).
     * @see #runSartUpNode(StartUpNode)
     */
    @Override
    public void onResume() {
        super.onResume();

        if (Common.hasWritePermissionToExternalStorage(this)) {
            mKeyEditor.setEnabled(true);
            mDumpEditor.setEnabled(true);
            useAsEditorOnly(Common.useAsEditorOnly());
            runSartUpNode(StartUpNode.FirstUseDialog);
        } else {
            Common.setUseAsEditorOnly(true);
            enableMenuButtons(false);
        }
    }

    /**
     * Disable NFC foreground dispatch system.
     * @see Common#disableNfcForegroundDispatch(Activity)
     */
    @Override
    public void onPause() {
        Common.disableNfcForegroundDispatch(this);
        super.onPause();
    }

    /**
     * Handle new Intent as a new tag Intent and if the tag/device does not
     * support MIFARE Classic, then run {@link TagInfoTool}.
     * @see Common#treatAsNewTag(Intent, android.content.Context)
     * @see TagInfoTool
     */
    @Override
    public void onNewIntent(Intent intent) {
        if(Common.getPendingComponentName() != null) {
            intent.setComponent(Common.getPendingComponentName());
            startActivity(intent);
        } else {
            int typeCheck = Common.treatAsNewTag(intent, this);
            if (typeCheck == -1 || typeCheck == -2) {
                // Device or tag does not support MIFARE Classic.
                // Run the only thing that is possible: The tag info tool.
                Intent i = new Intent(this, TagInfoTool.class);
                startActivity(i);
            }
        }
    }

    /**
     * Handle answered permission requests. Until now, the app only asks for
     * the permission to access the external storage.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
            String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,
                permissions, grantResults);

        switch (requestCode) {
            case REQUEST_WRITE_STORAGE_CODE:
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initFolders();
                } else {
                    Toast.makeText(this, R.string.info_write_permission,
                            Toast.LENGTH_LONG).show();
                    enableMenuButtons(false);

                }
                break;

        }
    }

    /**
     * Enable or disable all menu buttons which provide functionality that
     * uses the external storage.
     * @param enable True to enable the buttons. False to disable them.
     */
    private void enableMenuButtons(boolean enable) {
        mWriteTag.setEnabled(enable);
        mReadTag.setEnabled(enable);
        mKeyEditor.setEnabled(enable);
        mDumpEditor.setEnabled(enable);
    }

    /**
     * Show the {@link ReadTag}.
     * @param view The View object that triggered the method
     * (in this case the read tag button).
     * @see ReadTag
     */
    public void onShowReadTag(View view) {
        Intent intent = new Intent(this, ReadTag.class);
        startActivity(intent);
    }

    /**
     * Show the {@link WriteTag}.
     * @param view The View object that triggered the method
     * (in this case the write tag button).
     * @see WriteTag
     */
    public void onShowWriteTag(View view) {
        Intent intent = new Intent(this, WriteTag.class);
        startActivity(intent);
    }

    /**
     * Show the {@link HelpAndInfo}.
     * @param view The View object that triggered the method
     * (in this case the help/info button).
     */
    public void onShowHelp(View view) {
        Intent intent = new Intent(this, HelpAndInfo.class);
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
     * Open a file chooser ({@link FileChooser}). The
     * Activity result will be processed in
     * {@link #onActivityResult(int, int, Intent)}.
     * If the dump files folder is empty display an additional error
     * message.
     * @param view The View object that triggered the method
     * (in this case the show/edit tag dump button).
     * @see FileChooser
     * @see #onActivityResult(int, int, Intent)
     */
    public void onOpenTagDumpEditor(View view) {
        if (!Common.getPreferences().getBoolean(UseInternalStorage.toString(),
                false) && !Common.isExternalStorageWritableErrorToast(this)) {
            return;
        }
        File file = Common.getFileFromStorage(
            Common.HOME_DIR + "/" + Common.DUMPS_DIR);
        if (file.isDirectory() && (file.listFiles() == null
                || file.listFiles().length == 0)) {
            Toast.makeText(this, R.string.info_no_dumps,
                Toast.LENGTH_LONG).show();
        }
        Intent intent = new Intent(this, FileChooser.class);
        intent.putExtra(FileChooser.EXTRA_DIR, file.getAbsolutePath());
        intent.putExtra(FileChooser.EXTRA_TITLE,
                getString(R.string.text_open_dump_title));
        intent.putExtra(FileChooser.EXTRA_BUTTON_TEXT,
                getString(R.string.action_open_dump_file));
        intent.putExtra(FileChooser.EXTRA_ENABLE_DELETE_FILE, true);
        startActivityForResult(intent, FILE_CHOOSER_DUMP_FILE);
    }

    /**
     * Open a file chooser ({@link FileChooser}). The
     * Activity result will be processed in
     * {@link #onActivityResult(int, int, Intent)}.
     * @param view The View object that triggered the method
     * (in this case the show/edit key button).
     * @see FileChooser
     * @see #onActivityResult(int, int, Intent)
     */
    public void onOpenKeyEditor(View view) {
        if (!Common.getPreferences().getBoolean(UseInternalStorage.toString(),
                false) && !Common.isExternalStorageWritableErrorToast(this)) {
            return;
        }
        Intent intent = new Intent(this, FileChooser.class);
        intent.putExtra(FileChooser.EXTRA_DIR,
                Common.getFileFromStorage(Common.HOME_DIR + "/" +
                        Common.KEYS_DIR).getAbsolutePath());
        intent.putExtra(FileChooser.EXTRA_TITLE,
                getString(R.string.text_open_key_file_title));
        intent.putExtra(FileChooser.EXTRA_BUTTON_TEXT,
                getString(R.string.action_open_key_file));
        intent.putExtra(FileChooser.EXTRA_ENABLE_NEW_FILE, true);
        intent.putExtra(FileChooser.EXTRA_ENABLE_DELETE_FILE, true);
        startActivityForResult(intent, FILE_CHOOSER_KEY_FILE);
    }

    /**
     * Show the {@link Preferences}.
     */
    private void onShowPreferences() {
        Intent intent = new Intent(this, Preferences.class);
        startActivity(intent);
    }

    /**
     * Show the about dialog.
     */
    private void onShowAboutDialog() {
        CharSequence styledText = Html.fromHtml(
                getString(R.string.dialog_about_mct,
                Common.getVersionCode()));
        AlertDialog ad = new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_about_mct_title)
            .setMessage(styledText)
            .setIcon(R.mipmap.ic_launcher)
            .setPositiveButton(R.string.action_ok,
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Do nothing.
                }
            }).create();
         ad.show();
         // Make links clickable.
         ((TextView)ad.findViewById(android.R.id.message)).setMovementMethod(
                 LinkMovementMethod.getInstance());
    }

    /**
     * Handle the user input from the general options menu
     * (e.g. show the about dialog).
     * @see #onShowAboutDialog()
     * @see #onShowPreferences()
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection.
        switch (item.getItemId()) {
        case R.id.menuMainPreferences:
            onShowPreferences();
            return true;
        case R.id.menuMainAbout:
            onShowAboutDialog();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Handle (start) the selected tool from the tools menu.
     * @see TagInfoTool
     * @see ValueBlockTool
     * @see AccessConditionTool
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
        case R.id.menuMainTagInfo:
            intent = new Intent(this, TagInfoTool.class);
            startActivity(intent);
            return true;
        case R.id.menuMainValueBlockTool:
            intent = new Intent(this, ValueBlockTool.class);
            startActivity(intent);
            return true;
        case R.id.menuMainAccessConditionTool:
            intent = new Intent(this, AccessConditionTool.class);
            startActivity(intent);
            return true;
        case R.id.menuMainDiffTool:
            intent = new Intent(this, DiffTool.class);
            startActivity(intent);
            return true;
            case R.id.menuMainBccTool:
                intent = new Intent(this, BccTool.class);
                startActivity(intent);
                return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    /**
     * Run the {@link DumpEditor} or the {@link KeyEditor}
     * if file chooser result is O.K.
     * @see DumpEditor
     * @see KeyEditor
     * @see #onOpenTagDumpEditor(View)
     * @see #onOpenKeyEditor(View)
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
        case FILE_CHOOSER_DUMP_FILE:
            if (resultCode == Activity.RESULT_OK) {
                Intent intent = new Intent(this, DumpEditor.class);
                intent.putExtra(FileChooser.EXTRA_CHOSEN_FILE,
                        data.getStringExtra(
                                FileChooser.EXTRA_CHOSEN_FILE));
                startActivity(intent);
            }
            break;
        case FILE_CHOOSER_KEY_FILE:
            if (resultCode == Activity.RESULT_OK) {
                Intent intent = new Intent(this, KeyEditor.class);
                intent.putExtra(FileChooser.EXTRA_CHOSEN_FILE,
                        data.getStringExtra(
                                FileChooser.EXTRA_CHOSEN_FILE));
                startActivity(intent);
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
        File std = Common.getFileFromStorage(Common.HOME_DIR + "/" +
                Common.KEYS_DIR + "/" + Common.STD_KEYS);
        File extended = Common.getFileFromStorage(Common.HOME_DIR + "/" +
                        Common.KEYS_DIR + "/" + Common.STD_KEYS_EXTENDED);
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
