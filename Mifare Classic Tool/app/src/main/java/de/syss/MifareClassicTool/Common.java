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


package de.syss.MifareClassicTool;

import static de.syss.MifareClassicTool.Activities.Preferences.Preference.AutoCopyUID;
import static de.syss.MifareClassicTool.Activities.Preferences.Preference.UIDFormat;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Application;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcA;
import android.os.Build;
import android.provider.OpenableColumns;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Locale;

import de.syss.MifareClassicTool.Activities.IActivityThatReactsToSave;


/**
 * Common functions and variables for all Activities.
 * @author Gerhard Klostermeier
 */
public class Common extends Application {

    /**
     * True if this is the donate version of MCT.
     */
    public static final boolean IS_DONATE_VERSION = false;
    /**
     * The directory name of the root directory of this app.
     */
    public static final String HOME_DIR = "/MifareClassicTool";

    /**
     * The directory name  of the key files directory.
     * (sub directory of {@link #HOME_DIR}.)
     */
    public static final String KEYS_DIR = "key-files";

    /**
     * The directory name  of the dump files directory.
     * (sub directory of {@link #HOME_DIR}.)
     */
    public static final String DUMPS_DIR = "dump-files";

    /**
     * The directory name of the folder where temporary files are
     * stored. The directory will be cleaned during the creation of
     * the main activity
     * ({@link de.syss.MifareClassicTool.Activities.MainMenu}).
     * (sub directory of {@link #HOME_DIR}.)
     */
    public static final String TMP_DIR = "tmp";

    /**
     * This file contains some standard MIFARE keys.
     * <ul>
     * <li>0xFFFFFFFFFFFF - Un-formatted, factory fresh tags.</li>
     * <li>0xA0A1A2A3A4A5 - First sector of the tag (MIFARE MAD).</li>
     * <li>0xD3F7D3F7D3F7 - NDEF formatted tags.</li>
     * </ul>
     */
    public static final String STD_KEYS = "std.keys";

    /**
     * Keys taken from SLURP by Anders Sundman anders@4zm.org
     * the proxmark3 repositories and a short google search.
     * https://github.com/4ZM/slurp/blob/master/res/xml/mifare_default_keys.xml
     * https://github.com/RfidResearchGroup/proxmark3
     * https://github.com/Proxmark/proxmark3
     */
    public static final String STD_KEYS_EXTENDED = "extended-std.keys";

    /**
     * Log file with UIDs which have been discovered in the past.
     */
    public static final String UID_LOG_FILE = "uid-log-file.txt";

    /**
     * Possible operations the on a MIFARE Classic Tag.
     */
    public enum Operation {
        Read, Write, Increment, DecTransRest, ReadKeyA, ReadKeyB, ReadAC,
        WriteKeyA, WriteKeyB, WriteAC
    }

    private static final String LOG_TAG = Common.class.getSimpleName();

    /**
     * The last detected tag.
     * Set by {@link #treatAsNewTag(Intent, Context)}
     */
    private static Tag mTag = null;

    /**
     * The last detected UID.
     * Set by {@link #treatAsNewTag(Intent, Context)}
     */
    private static byte[] mUID = null;

    /**
     * Just a global storage to save key maps generated by
     * {@link de.syss.MifareClassicTool.Activities.KeyMapCreator}
     * @see de.syss.MifareClassicTool.Activities.KeyMapCreator
     * @see MCReader#getKeyMap()
     */
    private static SparseArray<byte[][]> mKeyMap = null;

    /**
     * Global storage for the point where
     * {@link de.syss.MifareClassicTool.Activities.KeyMapCreator} started to
     * create a key map.
     * @see de.syss.MifareClassicTool.Activities.KeyMapCreator
     * @see MCReader#getKeyMap()
     */
    private static int mKeyMapFrom = -1;

    /**
     * Global storage for the point where
     * {@link de.syss.MifareClassicTool.Activities.KeyMapCreator} ended to
     * create a key map.
     * @see de.syss.MifareClassicTool.Activities.KeyMapCreator
     * @see MCReader#getKeyMap()
     */
    private static int mKeyMapTo = -1;

    /**
     * The version code from the Android manifest.
     */
    private static String mVersionCode;

    /**
     * If NFC is disabled and the user chose to use MCT in editor only mode,
     * the choice is remembered here.
     */
    private static boolean mUseAsEditorOnly = false;

    /**
     * 1 if the device does support MIFARE Classic. -1 if it doesn't support
     * it. 0 if the support check was not yet performed.
     * Checking for MIFARE Classic support is really expensive. Therefore
     * remember the result here.
     */
    private static int mHasMifareClassicSupport = 0;

    /**
     * The component name of the activity that is in foreground and
     * should receive the new detected tag object by an external reader.
     */
    private static ComponentName mPendingComponentName = null;


    private static NfcAdapter mNfcAdapter;
    private static Context mAppContext;
    private static float mScale;

// ############################################################################

    /**
     * Initialize the {@link #mAppContext} with the application context.
     * Some functions depend on this context.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mAppContext = getApplicationContext();
        mScale = getResources().getDisplayMetrics().density;

        try {
            mVersionCode = getPackageManager().getPackageInfo(
                    getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            Log.d(LOG_TAG, "Version not found.");
        }
    }

    /**
     * Check if this is the first installation of this app or just an update.
     * @return True if app was not installed before. False otherwise.
     */
    public static boolean isFirstInstall() {
        try {
            long firstInstallTime = mAppContext.getPackageManager()
                    .getPackageInfo(mAppContext.getPackageName(), 0).firstInstallTime;
            long lastUpdateTime = mAppContext.getPackageManager()
                    .getPackageInfo(mAppContext.getPackageName(), 0).lastUpdateTime;
            return firstInstallTime == lastUpdateTime;
        } catch (PackageManager.NameNotFoundException e) {
            return true;
        }
    }

    /**
     * Create a File object with a path that consists of the apps file
     * directory, the {@link #HOME_DIR} and the relative path.
     * @param relativePath The relative path that gets appended to the
     * base path.
     * @return A File object with the absolute path of app file directory +
     * {@link #HOME_DIR} + relativePath.
     * @see Context#getFilesDir()
     */
    public static File getFile(String relativePath) {
        return new File(mAppContext.getFilesDir()
                + HOME_DIR + "/" + relativePath);
    }

    /**
     * Read a file line by line. The file should be a simple text file.
     * Empty lines will not be read.
     * @param file The file to read.
     * @param readAll If true, comments and empty lines will be read too.
     * @param context The context in which the possible "Out of memory"-Toast
     * will be shown.
     * @return Array of strings representing the lines of the file.
     * If the file is empty or an error occurs "null" will be returned.
     */
    public static String[] readFileLineByLine(File file, boolean readAll,
            Context context) {
        if (file == null || !file.exists()) {
            return null;
        }
        String[] ret;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            ret = readLineByLine(reader, readAll, context);
        } catch (FileNotFoundException ex) {
            ret = null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error while closing file.", e);
                    ret = null;
                }
            }
        }
        return ret;
    }

    /**
     * Read the URI source line by line.
     * @param uri The URI to read from.
     * @param readAll If true, comments and empty lines will be read too.
     * @param context The context for the content resolver and in which
     * error/info Toasts are shown.
     * @return The content of the URI, each line representing an array item
     * or Null in case of an read error.
     * @see #readLineByLine(BufferedReader, boolean, Context)
     */
    public static String[] readUriLineByLine(Uri uri, boolean readAll, Context context) {
        InputStream contentStream;
        String[] ret;
        if (uri == null || context == null) {
            return null;
        }
        try {
            contentStream = context.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException | SecurityException ex) {
            return null;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(contentStream));
        ret = readLineByLine(reader, readAll, context);
        try {
            reader.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error while closing file.", e);
            return null;
        }
        return ret;
    }

    /**
     * Read the URI as raw bytes.
     * @param uri The URI to read from.
     * @param context The context for the content resolver.
     * @return The content of the URI as raw bytes or Null in case of
     * an read error.
     */
    public static byte[] readUriRaw(Uri uri, Context context) {
        InputStream contentStream;
        if (uri == null || context == null) {
            return null;
        }
        try {
            contentStream = context.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException | SecurityException ex) {
            return null;
        }

        int len;
        byte[] data = new byte[16384];
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            while ((len = contentStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, len);
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error while reading from file.", e);
            return null;
        }

        return buffer.toByteArray();
    }

    /**
     * Read a as BufferedReader line by line with some exceptions.
     * Empty lines and leading/tailing whitespaces will be ignored.
     * @param reader The reader object initialized with a file (data).
     * @param readAll If true, comments and empty lines will be read too.
     * @param context The Context in which error Toasts will be shown.
     * @return The content with each line representing an array item
     * or Null in case of an read error.
     */
    private static String[] readLineByLine(BufferedReader reader,
            boolean readAll, Context context) {
        String[] ret;
        String line;
        ArrayList<String> linesArray = new ArrayList<>();
        try {
            while ((line = reader.readLine()) != null) {
                // Ignore leading/tailing whitespaces of line.
                line = line.trim();
                // Remove comments if readAll is false.
                if (!readAll) {
                    if (line.startsWith("#") || line.equals("")) {
                        continue;
                    }
                    // Look for content (ignore the comment).
                    line = line.split("#")[0];
                    // Ignore leading/tailing whitespaces of content.
                    line = line.trim();
                }
                try {
                    linesArray.add(line);
                } catch (OutOfMemoryError e) {
                    // Error. File is too big
                    // (too many lines, out of memory).
                    Toast.makeText(context, R.string.info_file_to_big,
                            Toast.LENGTH_LONG).show();
                    return null;
                }
            }
        } catch (IOException ex) {
            Log.e(LOG_TAG, "Error while reading from file.", ex);
            ret = null;
        }
        if (linesArray.size() > 0) {
            ret = linesArray.toArray(new String[0]);
        } else {
            ret = new String[]{""};
        }
        return ret;
    }

    /**
     * Get the file name from an URI object.
     * Taken from https://stackoverflow.com/a/25005243
     * @param uri The URI to get the file name from,
     * @param context The Context for the content resolver.
     * @return The file name of the URI object.
     */
    public static String getFileName(Uri uri, Context context) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(
                uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    /**
     * Check if the file already exists. If so, present a dialog to the user
     * with the options: "Replace", "Append" and "Cancel".
     * @param file File that will be written.
     * @param lines The lines to save.
     * @param isDump Set to True if file and lines are a dump file.
     * @param context The Context in which the dialog and Toast will be shown.
     * @param activity An object (most likely an Activity) that implements the
     * onSaveSuccessful() and onSaveFailure() methods. These methods will
     * be called according to the save process. Also, onSaveFailure() will
     * be called if the user hints cancel.
     * @see #saveFile(File, String[], boolean)
     * @see #saveFileAppend(File, String[], boolean)
     */
    public static void checkFileExistenceAndSave(final File file,
            final String[] lines, final boolean isDump, final Context context,
            final IActivityThatReactsToSave activity) {
        if (file.exists()) {
            // Save conflict for dump file or key file?
            int message = R.string.dialog_save_conflict_keyfile;
            if (isDump) {
                message = R.string.dialog_save_conflict_dump;
            }

            // File already exists. Replace? Append? Cancel?
            new AlertDialog.Builder(context)
            .setTitle(R.string.dialog_save_conflict_title)
            .setMessage(message)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(R.string.action_replace,
                    (dialog, which) -> {
                        // Replace.
                        if (Common.saveFile(file, lines, false)) {
                            Toast.makeText(context, R.string.info_save_successful,
                                    Toast.LENGTH_LONG).show();
                            activity.onSaveSuccessful();
                        } else {
                            Toast.makeText(context, R.string.info_save_error,
                                    Toast.LENGTH_LONG).show();
                            activity.onSaveFailure();
                        }
                    })
            .setNeutralButton(R.string.action_append,
                    (dialog, which) -> {
                        // Append.
                        if (Common.saveFileAppend(file, lines, isDump)) {
                            Toast.makeText(context, R.string.info_save_successful,
                                    Toast.LENGTH_LONG).show();
                            activity.onSaveSuccessful();
                        } else {
                            Toast.makeText(context, R.string.info_save_error,
                                    Toast.LENGTH_LONG).show();
                            activity.onSaveFailure();
                        }
                    })
            .setNegativeButton(R.string.action_cancel,
                    (dialog, id) -> {
                        // Cancel.
                        activity.onSaveFailure();
                    }).show();
        } else {
            if (Common.saveFile(file, lines, false)) {
                Toast.makeText(context, R.string.info_save_successful,
                        Toast.LENGTH_LONG).show();
                activity.onSaveSuccessful();
            } else {
                Toast.makeText(context, R.string.info_save_error,
                        Toast.LENGTH_LONG).show();
                activity.onSaveFailure();
            }
        }
    }

    /**
     * Append an array of strings (each field is one line) to a given file.
     * @param file The file to write to.
     * @param lines The lines to save.
     * @param comment If true, add a comment before the appended section.
     * @return True if file writing was successful. False otherwise.
     */
    public static boolean saveFileAppend(File file, String[] lines,
            boolean comment) {
        if (comment) {
            // Append to a existing file.
            String[] newLines = new String[lines.length + 4];
            System.arraycopy(lines, 0, newLines, 4, lines.length);
            newLines[1] = "";
            newLines[2] = "# Append #######################";
            newLines[3] = "";
            lines = newLines;
        }
        return saveFile(file, lines, true);
    }

    /**
     * Write an array of strings (each field is one line) to a given file.
     * @param file The file to write to.
     * @param lines The lines to save.
     * @param append Append to file (instead of replacing its content).
     * @return True if file writing was successful. False otherwise or if
     * parameters were wrong (e.g. null)..
     */
    public static boolean saveFile(File file, String[] lines, boolean append) {
        boolean error = false;
        if (file != null && lines != null && lines.length > 0) {
            BufferedWriter bw = null;
            try {
                bw = new BufferedWriter(new FileWriter(file, append));
                // Add new line before appending.
                if (append) {
                    bw.newLine();
                }
                int i;
                for(i = 0; i < lines.length-1; i++) {
                    bw.write(lines[i]);
                    bw.newLine();
                }
                bw.write(lines[i]);
            } catch (IOException | NullPointerException ex) {
                Log.e(LOG_TAG, "Error while writing to '"
                        + file.getName() + "' file.", ex);
                error = true;

            } finally {
                if (bw != null) {
                    try {
                        bw.close();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Error while closing file.", e);
                        error = true;
                    }
                }
            }
        } else {
            error = true;
        }
        return !error;
    }

    /**
     * Write text lines to a given content URI.
     * @param contentUri The content URI to write to.
     * @param lines The text lines to save.
     * @param context The context for the ContentProvider.
     * @return True if file writing was successful. False otherwise.
     */
    public static boolean saveFile(Uri contentUri, String[] lines, Context context) {
        if (contentUri == null || lines == null || context == null || lines.length == 0) {
            return false;
        }
        String concatenatedLines = TextUtils.join(
                System.getProperty("line.separator"), lines);
        byte[] bytes = concatenatedLines.getBytes();
        return saveFile(contentUri, bytes, context);
    }

    /**
     * Write an array of bytes (raw data) to a given content URI.
     * @param contentUri The content URI to write to.
     * @param bytes The bytes to save.
     * @param context The context for the ContentProvider.
     * @return True if file writing was successful. False otherwise.
     */
    public static boolean saveFile(Uri contentUri, byte[] bytes, Context context) {
        OutputStream output;
        if (contentUri == null || bytes == null || context == null || bytes.length == 0) {
            return false;
        }
        try {
            output = context.getContentResolver().openOutputStream(
                    contentUri, "rw");
        } catch (FileNotFoundException ex) {
            return false;
        }
        if (output != null) {
            try {
                output.write(bytes);
                output.flush();
                output.close();
            } catch (IOException ex) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get the shared preferences with application context for saving
     * and loading ("global") values.
     * @return The shared preferences object with application context.
     */
    public static SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(mAppContext);
    }

    /**
     * Enables the NFC foreground dispatch system for the given Activity.
     * @param targetActivity The Activity that is in foreground and wants to
     * have NFC Intents.
     * @see #disableNfcForegroundDispatch(Activity)
     */
    public static void enableNfcForegroundDispatch(Activity targetActivity) {
        if (mNfcAdapter != null && mNfcAdapter.isEnabled()) {

            Intent intent = new Intent(targetActivity,
                    targetActivity.getClass()).addFlags(
                            Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    targetActivity, 0, intent, PendingIntent.FLAG_MUTABLE);
            try {
                mNfcAdapter.enableForegroundDispatch(
                        targetActivity, pendingIntent, null, new String[][]{
                                new String[]{NfcA.class.getName()}});
            } catch (IllegalStateException ex) {
                Log.d(LOG_TAG, "Error: Could not enable the NFC foreground" +
                        "dispatch system. The activity was not in foreground.");
            }
        }
    }

    /**
     * Disable the NFC foreground dispatch system for the given Activity.
     * @param targetActivity An Activity that is in foreground and has
     * NFC foreground dispatch system enabled.
     * @see #enableNfcForegroundDispatch(Activity)
     */
    public static void disableNfcForegroundDispatch(Activity targetActivity) {
        if (mNfcAdapter != null && mNfcAdapter.isEnabled()) {
            try {
                mNfcAdapter.disableForegroundDispatch(targetActivity);
            } catch (IllegalStateException ex) {
                Log.d(LOG_TAG, "Error: Could not disable the NFC foreground" +
                        "dispatch system. The activity was not in foreground.");
            }
        }
    }

    /**
     * Log the UID to a file. This is called by {@link #treatAsNewTag(Intent, Context)}
     * and needed for the {@link de.syss.MifareClassicTool.Activities.UidLogTool}.
     * @param uid The UID to append to the log file.
     * @see #UID_LOG_FILE
     * @see #treatAsNewTag(Intent, Context)
     * @see de.syss.MifareClassicTool.Activities.UidLogTool
     */
    public static void logUid(String uid) {
        File log = new File(mAppContext.getFilesDir(),
                HOME_DIR + File.separator + UID_LOG_FILE);
        GregorianCalendar calendar = new GregorianCalendar();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss",
                Locale.getDefault());
        fmt.setCalendar(calendar);
        String dateFormatted = fmt.format(calendar.getTime());
        String[] logEntry = new String[1];
        logEntry[0] = dateFormatted + ": " + uid;
        saveFile(log, logEntry, true);
    }

    /**
     * For Activities which want to treat new Intents as Intents with a new
     * Tag attached. If the given Intent has a Tag extra, it will be patched
     * by {@link MCReader#patchTag(Tag)} and {@link #mTag} as well as
     * {@link #mUID} will be updated. The UID will be loged using
     * {@link #logUid(String)}. A Toast message will be shown in the
     * Context of the calling Activity. This method will also check if the
     * device/tag supports MIFARE Classic (see return values and
     * {@link #checkMifareClassicSupport(Tag, Context)}).
     * @param intent The Intent which should be checked for a new Tag.
     * @param context The Context in which the Toast will be shown.
     * @return
     * <ul>
     * <li>0 - The device/tag supports MIFARE Classic</li>
     * <li>-1 - Device does not support MIFARE Classic.</li>
     * <li>-2 - Tag does not support MIFARE Classic.</li>
     * <li>-3 - Error (tag or context is null).</li>
     * <li>-4 - Wrong Intent (action is not "ACTION_TECH_DISCOVERED").</li>
     * </ul>
     * @see #mTag
     * @see #mUID
     * @see #checkMifareClassicSupport(Tag, Context)
     */
    public static int treatAsNewTag(Intent intent, Context context) {
        // Check if Intent has a NFC Tag.
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            tag = MCReader.patchTag(tag);
            if (tag == null) {
                return -3;
            }
            setTag(tag);
            logUid(bytes2Hex(tag.getId()));

            boolean isCopyUID = getPreferences().getBoolean(
                    AutoCopyUID.toString(), false);
            if (isCopyUID) {
                int format = getPreferences().getInt(
                        UIDFormat.toString(), 0);
                String fmtUID = byte2FmtString(tag.getId(),format);
                // Show Toast with copy message.
                Toast.makeText(context,
                        "UID " + context.getResources().getString(
                                R.string.info_copied_to_clipboard)
                                .toLowerCase() + " (" + fmtUID + ")",
                        Toast.LENGTH_SHORT).show();
                copyToClipboard(fmtUID, context, false);
            } else {
                // Show Toast message with UID.
                String id = context.getResources().getString(
                        R.string.info_new_tag_found) + " (UID: ";
                id += bytes2Hex(tag.getId());
                id += ")";
                Toast.makeText(context, id, Toast.LENGTH_LONG).show();
            }
            return checkMifareClassicSupport(tag, context);
        }
        return -4;
    }

    /**
     * Check if the device supports the MIFARE Classic technology.
     * In order to do so, there is a first check ensure the device actually has
     * a NFC hardware (if not, {@link #mUseAsEditorOnly} is set to true).
     * After this, this function will check if there are files
     * like "/dev/bcm2079x-i2c" or "/system/lib/libnfc-bcrm*". Files like
     * these are indicators for a NFC controller manufactured by Broadcom.
     * Broadcom chips don't support MIFARE Classic.
     * @return True if the device supports MIFARE Classic. False otherwise.
     * @see #mHasMifareClassicSupport
     * @see #mUseAsEditorOnly
     */
    public static boolean hasMifareClassicSupport() {
        if (mHasMifareClassicSupport != 0) {
            return mHasMifareClassicSupport == 1;
        }

        // Check for the MifareClassic class.
        // It is most likely there on all NFC enabled phones.
        // Therefore this check is not needed.
        /*
        try {
            Class.forName("android.nfc.tech.MifareClassic");
        } catch( ClassNotFoundException e ) {
            // Class not found. Devices does not support MIFARE Classic.
            return false;
        }
        */

        // Check if ther is any NFC hardware at all.
        if (NfcAdapter.getDefaultAdapter(mAppContext) == null) {
            mUseAsEditorOnly = true;
            mHasMifareClassicSupport = -1;
            return false;
        }

        // Check if there is the NFC device "bcm2079x-i2c".
        // Chips by Broadcom don't support MIFARE Classic.
        // This could fail because on a lot of devices apps don't have
        // the sufficient permissions.
        // Another exception:
        // The Lenovo P2 has a device at "/dev/bcm2079x-i2c" but is still
        // able of reading/writing MIFARE Classic tags. I don't know why...
        // https://github.com/ikarus23/MifareClassicTool/issues/152
        boolean isLenovoP2 = Build.MANUFACTURER.equals("LENOVO")
                && Build.MODEL.equals("Lenovo P2a42");
        File device = new File("/dev/bcm2079x-i2c");
        if (!isLenovoP2 && device.exists()) {
            mHasMifareClassicSupport = -1;
            return false;
        }

        // Check if there is the NFC device "pn544".
        // The PN544 NFC chip is manufactured by NXP.
        // Chips by NXP support MIFARE Classic.
        device = new File("/dev/pn544");
        if (device.exists()) {
            mHasMifareClassicSupport = 1;
            return true;
        }

        // Check if there are NFC libs with "brcm" in their names.
        // "brcm" libs are for devices with Broadcom chips. Broadcom chips
        // don't support MIFARE Classic.
        File libsFolder = new File("/system/lib");
        File[] libs = libsFolder.listFiles();
        for (File lib : libs) {
            if (lib.isFile()
                    && lib.getName().startsWith("libnfc")
                    && lib.getName().contains("brcm")
                    // Add here other non NXP NFC libraries.
                    ) {
                mHasMifareClassicSupport = -1;
                return false;
            }
        }

        mHasMifareClassicSupport = 1;
        return true;
    }

    /**
     * Check if the tag and the device support the MIFARE Classic technology.
     * @param tag The tag to check.
     * @param context The context of the package manager.
     * @return
     * <ul>
     * <li>0 - Device and tag support MIFARE Classic.</li>
     * <li>-1 - Device does not support MIFARE Classic.</li>
     * <li>-2 - Tag does not support MIFARE Classic.</li>
     * <li>-3 - Error (tag or context is null).</li>
     * </ul>
     */
    public static int checkMifareClassicSupport(Tag tag, Context context) {
        if (tag == null || context == null) {
            // Error.
            return -3;
        }

        if (Arrays.asList(tag.getTechList()).contains(
                MifareClassic.class.getName())) {
            // Device and tag should support MIFARE Classic.
            // But is there something wrong the the tag?
            try {
                MifareClassic.get(tag);
            } catch (RuntimeException ex) {
                // Stack incorrectly reported a MifareClassic.
                // Most likely not a MIFARE Classic tag.
                // See: https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/nfc/tech/MifareClassic.java#196
                return -2;
            }
            return 0;

        // This is no longer valid. There are some devices (e.g. LG's F60)
        // that have this system feature but no MIFARE Classic support.
        // (The F60 has a Broadcom NFC controller.)
        /*
        } else if (context.getPackageManager().hasSystemFeature(
                "com.nxp.mifare")){
            // Tag does not support MIFARE Classic.
            return -2;
        */

        } else {
            // Check if device does not support MIFARE Classic.
            // For doing so, check if the SAK of the tag indicate that
            // it's a MIFARE Classic tag.
            // See: https://www.nxp.com/docs/en/application-note/AN10833.pdf (page 6)
            NfcA nfca = NfcA.get(tag);
            byte sak = (byte)nfca.getSak();
            if ((sak>>1 & 1) == 1) {
                // RFU.
                return -2;
            } else {
                if ((sak>>3 & 1) == 1) { // SAK bit 4 = 1?
                    // Note: Other SAK bits are irrelevant. Tag is MIFARE Classic compatible.
                    // MIFARE Mini
                    // MIFARE Classic 1K/2K/4K
                    // MIFARE SmartMX 1K/4K
                    // MIFARE Plus S 2K/4K SL1
                    // MIFARE Plus X 2K/4K SL1
                    // MIFARE Plus SE 1K
                    // MIFARE Plus EV1 2K/4K SL1
                    return -1;
                } else { // SAK bit 4 = 0
                    // Note: Other SAK bits are irrelevant. Tag is *not* MIFARE Classic compatible.
                    // Tags like MIFARE Plus in SL2, MIFARE Ultralight, MIFARE DESFire, etc.
                    return -2;
                }
            }

            // Old MIFARE Classic support check. No longer valid.
            // Check if the ATQA + SAK of the tag indicate that it's a MIFARE Classic tag.
            // See: http://www.nxp.com/documents/application_note/AN10833.pdf
            // (Table 5 and 6)
            // 0x28 is for some emulated tags.
            /*
            NfcA nfca = NfcA.get(tag);
            byte[] atqa = nfca.getAtqa();
            if (atqa[1] == 0 &&
                    (atqa[0] == 4 || atqa[0] == (byte)0x44 ||
                     atqa[0] == 2 || atqa[0] == (byte)0x42)) {
                // ATQA says it is most likely a MIFARE Classic tag.
                byte sak = (byte)nfca.getSak();
                if (sak == 8 || sak == 9 || sak == (byte)0x18 ||
                                            sak == (byte)0x88 ||
                                            sak == (byte)0x28) {
                    // SAK says it is most likely a MIFARE Classic tag.
                    // --> Device does not support MIFARE Classic.
                    return -1;
                }
            }
            // Nope, it's not the device (most likely).
            // The tag does not support MIFARE Classic.
            return -2;
            */
        }
    }

    /**
     * Open another app.
     * @param context current Context, like Activity, App, or Service
     * @param packageName the full package name of the app to open
     * @return true if likely successful, false if unsuccessful
     */
    public static boolean openApp(Context context, String packageName) {
        PackageManager manager = context.getPackageManager();
        try {
            Intent i = manager.getLaunchIntentForPackage(packageName);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            context.startActivity(i);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check whether the service of the "External NFC" app is running or not.
     * This will only work for Android < 8.
     * @param context The context for the system service.
     * @return
     * <ul>
     * <li>0 - Service is not running.</li>
     * <li>1 - Service is running.</li>
     * <li>-1 - Can not check because Android version is >= 8.</li>
     * </ul>
     */
    public static int isExternalNfcServiceRunning(Context context) {
        // getRunningServices() is deprecated since Android 8.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            ActivityManager manager =
                    (ActivityManager) context.getSystemService(
                            Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningServiceInfo service
                    : manager.getRunningServices(Integer.MAX_VALUE)) {
                if ("eu.dedb.nfc.service.NfcService".equals(
                        service.service.getClassName())) {
                    return 1;
                }
            }
            return 0;
        }
        return -1;
    }

    /**
     * Find out whether the "External NFC" app is installed or not.
     * @param context The context for the package manager.
     * @return True if "External NFC" is installed. False otherwise.
     */
    public static boolean hasExternalNfcInstalled(Context context) {
        return Common.isAppInstalled("eu.dedb.nfc.service", context);
    }

    /**
     * Check whether an app is installed or not.
     * @param uri The URI (package name) of the app.
     * @param context The context for the package manager.
     * @return True if the app is installed. False otherwise.
     */
    public static boolean isAppInstalled(String uri, Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (Exception e) {
            // Should only throw PackageManager.NameNotFoundException, but
            // might throw TransactionTooLargeException in some cases...
            return false;
        }
    }

    /**
     * Create a connected {@link MCReader} if there is a present MIFARE Classic
     * tag. If there is no MIFARE Classic tag an error
     * message will be displayed to the user.
     * @param context The Context in which the error Toast will be shown.
     * @return A connected {@link MCReader} or "null" if no tag was present.
     */
    public static MCReader checkForTagAndCreateReader(Context context) {
        MCReader reader;
        boolean tagLost = false;
        // Check for tag.
        if (mTag != null && (reader = MCReader.get(mTag)) != null) {
            try {
                reader.connect();
            } catch (Exception e) {
                tagLost = true;
            }
            if (!tagLost && !reader.isConnected()) {
                reader.close();
                tagLost = true;
            }
            if (!tagLost) {
                return reader;
            }
        }

        // Error. The tag is gone.
        Toast.makeText(context, R.string.info_no_tag_found,
                Toast.LENGTH_LONG).show();
        return null;
    }

    /**
     * Depending on the provided Access Conditions, this method will return
     * which key is required to achieve the operation ({@link Operation}).
     * @param c1 Access Condition bit "C1".
     * @param c2 Access Condition bit "C2".
     * @param c3 Access Condition bit "C3".
     * @param op The operation you want to do.
     * @param isSectorTrailer True if it is a Sector Trailer, False otherwise.
     * @param isKeyBReadable True if key B is readable, False otherwise.
     * @return The operation "op" is possible with:<br />
     * <ul>
     * <li>0 - Never.</li>
     * <li>1 - Key A.</li>
     * <li>2 - Key B.</li>
     * <li>3 - Key A or B.</li>
     * <li>-1 - Error.</li>
     * </ul>
     */
    public static int getOperationRequirements (byte c1, byte c2, byte c3,
                Operation op, boolean isSectorTrailer, boolean isKeyBReadable) {
        // Is Sector Trailer?
        if (isSectorTrailer) {
            // Sector Trailer.
            if (op != Operation.ReadKeyA && op != Operation.ReadKeyB
                    && op != Operation.ReadAC
                    && op != Operation.WriteKeyA
                    && op != Operation.WriteKeyB
                    && op != Operation.WriteAC) {
                // Error. Sector Trailer but no Sector Trailer permissions.
                return 4;
            }
            if          (c1 == 0 && c2 == 0 && c3 == 0) {
                if (op == Operation.WriteKeyA
                        || op == Operation.WriteKeyB
                        || op == Operation.ReadKeyB
                        || op == Operation.ReadAC) {
                    return 1;
                }
                return 0;
            } else if   (c1 == 0 && c2 == 1 && c3 == 0) {
                if (op == Operation.ReadKeyB
                        || op == Operation.ReadAC) {
                    return 1;
                }
                return 0;
            } else if   (c1 == 1 && c2 == 0 && c3 == 0) {
                if (op == Operation.WriteKeyA
                        || op == Operation.WriteKeyB) {
                    return 2;
                }
                if (op == Operation.ReadAC) {
                    return 3;
                }
                return 0;
            } else if   (c1 == 1 && c2 == 1 && c3 == 0) {
                if (op == Operation.ReadAC) {
                    return 3;
                }
                return 0;
            } else if   (c1 == 0 && c2 == 0 && c3 == 1) {
                if (op == Operation.ReadKeyA) {
                    return 0;
                }
                return 1;
            } else if   (c1 == 0 && c2 == 1 && c3 == 1) {
                if (op == Operation.ReadAC) {
                    return 3;
                }
                if (op == Operation.ReadKeyA
                        || op == Operation.ReadKeyB) {
                    return 0;
                }
                return 2;
            } else if   (c1 == 1 && c2 == 0 && c3 == 1) {
                if (op == Operation.ReadAC) {
                    return 3;
                }
                if (op == Operation.WriteAC) {
                    return 2;
                }
                return 0;
            } else if   (c1 == 1 && c2 == 1 && c3 == 1) {
                if (op == Operation.ReadAC) {
                    return 3;
                }
                return 0;
            } else {
                return -1;
            }
        } else {
            // Data Block.
            if (op != Operation.Read && op != Operation.Write
                    && op != Operation.Increment
                    && op != Operation.DecTransRest) {
                // Error. Data block but no data block permissions.
                return -1;
            }
            if          (c1 == 0 && c2 == 0 && c3 == 0) {
                return (isKeyBReadable) ? 1 : 3;
            } else if   (c1 == 0 && c2 == 1 && c3 == 0) {
                if (op == Operation.Read) {
                    return (isKeyBReadable) ? 1 : 3;
                }
                return 0;
            } else if   (c1 == 1 && c2 == 0 && c3 == 0) {
                if (op == Operation.Read) {
                    return (isKeyBReadable) ? 1 : 3;
                }
                if (op == Operation.Write) {
                    return 2;
                }
                return 0;
            } else if   (c1 == 1 && c2 == 1 && c3 == 0) {
                if (op == Operation.Read
                        || op == Operation.DecTransRest) {
                    return (isKeyBReadable) ? 1 : 3;
                }
                return 2;
            } else if   (c1 == 0 && c2 == 0 && c3 == 1) {
                if (op == Operation.Read
                        || op == Operation.DecTransRest) {
                    return (isKeyBReadable) ? 1 : 3;
                }
                return 0;
            } else if   (c1 == 0 && c2 == 1 && c3 == 1) {
                if (op == Operation.Read || op == Operation.Write) {
                    return 2;
                }
                return 0;
            } else if   (c1 == 1 && c2 == 0 && c3 == 1) {
                if (op == Operation.Read) {
                    return 2;
                }
                return 0;
            } else if   (c1 == 1 && c2 == 1 && c3 == 1) {
                return 0;
            } else {
                // Error.
                return -1;
            }
        }
    }

    /**
     * Check if key B is readable.
     * Key B is readable for the following configurations:
     * <ul>
     * <li>C1 = 0, C2 = 0, C3 = 0</li>
     * <li>C1 = 0, C2 = 0, C3 = 1</li>
     * <li>C1 = 0, C2 = 1, C3 = 0</li>
     * </ul>
     * @param c1 Access Condition bit "C1" of the Sector Trailer.
     * @param c2 Access Condition bit "C2" of the Sector Trailer.
     * @param c3 Access Condition bit "C3" of the Sector Trailer.
     * @return True if key B is readable. False otherwise.
     */
    public static boolean isKeyBReadable(byte c1, byte c2, byte c3) {
        return c1 == 0
                && ((c2 == 0 && c3 == 0)
                || (c2 == 1 && c3 == 0)
                || (c2 == 0 && c3 == 1));
    }

    /**
     * Convert the Access Condition bytes to a matrix containing the
     * resolved C1, C2 and C3 for each block.
     * @param acBytes The Access Condition bytes (3 byte).
     * @return Matrix of access conditions bits (C1-C3) where the first
     * dimension is the "C" parameter (C1-C3, Index 0-2) and the second
     * dimension is the block number (Index 0-3). If the ACs are incorrect
     * null will be returned.
     */
    public static byte[][] acBytesToACMatrix(byte[] acBytes) {
        if (acBytes == null) {
            return null;
        }
        // ACs correct?
        // C1 (Byte 7, 4-7) == ~C1 (Byte 6, 0-3) and
        // C2 (Byte 8, 0-3) == ~C2 (Byte 6, 4-7) and
        // C3 (Byte 8, 4-7) == ~C3 (Byte 7, 0-3)
        byte[][] acMatrix = new byte[3][4];
        if (acBytes.length > 2 &&
                (byte)((acBytes[1]>>>4)&0x0F)  ==
                        (byte)((acBytes[0]^0xFF)&0x0F) &&
                (byte)(acBytes[2]&0x0F) ==
                        (byte)(((acBytes[0]^0xFF)>>>4)&0x0F) &&
                (byte)((acBytes[2]>>>4)&0x0F)  ==
                        (byte)((acBytes[1]^0xFF)&0x0F)) {
            // C1, Block 0-3
            for (int i = 0; i < 4; i++) {
                acMatrix[0][i] = (byte)((acBytes[1]>>>4+i)&0x01);
            }
            // C2, Block 0-3
            for (int i = 0; i < 4; i++) {
                acMatrix[1][i] = (byte)((acBytes[2]>>>i)&0x01);
            }
            // C3, Block 0-3
            for (int i = 0; i < 4; i++) {
                acMatrix[2][i] = (byte)((acBytes[2]>>>4+i)&0x01);
            }
            return acMatrix;
        }
        return null;
    }

    /**
     * Convert a matrix with Access Conditions bits into normal 3
     * Access Condition bytes.
     * @param acMatrix Matrix of access conditions bits (C1-C3) where the first
     * dimension is the "C" parameter (C1-C3, Index 0-2) and the second
     * dimension is the block number (Index 0-3).
     * @return The Access Condition bytes (3 byte).
     */
    public static byte[] acMatrixToACBytes(byte[][] acMatrix) {
        if (acMatrix != null && acMatrix.length == 3) {
            for (int i = 0; i < 3; i++) {
                if (acMatrix[i].length != 4)
                    // Error.
                    return null;
            }
        } else {
            // Error.
            return null;
        }
        byte[] acBytes = new byte[3];
        // Byte 6, Bit 0-3.
        acBytes[0] = (byte)((acMatrix[0][0]^0xFF)&0x01);
        acBytes[0] |= (byte)(((acMatrix[0][1]^0xFF)<<1)&0x02);
        acBytes[0] |= (byte)(((acMatrix[0][2]^0xFF)<<2)&0x04);
        acBytes[0] |= (byte)(((acMatrix[0][3]^0xFF)<<3)&0x08);
        // Byte 6, Bit 4-7.
        acBytes[0] |= (byte)(((acMatrix[1][0]^0xFF)<<4)&0x10);
        acBytes[0] |= (byte)(((acMatrix[1][1]^0xFF)<<5)&0x20);
        acBytes[0] |= (byte)(((acMatrix[1][2]^0xFF)<<6)&0x40);
        acBytes[0] |= (byte)(((acMatrix[1][3]^0xFF)<<7)&0x80);
        // Byte 7, Bit 0-3.
        acBytes[1] = (byte)((acMatrix[2][0]^0xFF)&0x01);
        acBytes[1] |= (byte)(((acMatrix[2][1]^0xFF)<<1)&0x02);
        acBytes[1] |= (byte)(((acMatrix[2][2]^0xFF)<<2)&0x04);
        acBytes[1] |= (byte)(((acMatrix[2][3]^0xFF)<<3)&0x08);
        // Byte 7, Bit 4-7.
        acBytes[1] |= (byte)((acMatrix[0][0]<<4)&0x10);
        acBytes[1] |= (byte)((acMatrix[0][1]<<5)&0x20);
        acBytes[1] |= (byte)((acMatrix[0][2]<<6)&0x40);
        acBytes[1] |= (byte)((acMatrix[0][3]<<7)&0x80);
        // Byte 8, Bit 0-3.
        acBytes[2] = (byte)(acMatrix[1][0]&0x01);
        acBytes[2] |= (byte)((acMatrix[1][1]<<1)&0x02);
        acBytes[2] |= (byte)((acMatrix[1][2]<<2)&0x04);
        acBytes[2] |= (byte)((acMatrix[1][3]<<3)&0x08);
        // Byte 8, Bit 4-7.
        acBytes[2] |= (byte)((acMatrix[2][0]<<4)&0x10);
        acBytes[2] |= (byte)((acMatrix[2][1]<<5)&0x20);
        acBytes[2] |= (byte)((acMatrix[2][2]<<6)&0x40);
        acBytes[2] |= (byte)((acMatrix[2][3]<<7)&0x80);

        return acBytes;
    }

    /**
     * Check if a (hex) string is pure hex (0-9, A-F, a-f) and 16 byte
     * (32 chars) long. If not show an error Toast in the context.
     * @param hexString The string to check.
     * @param context The Context in which the Toast will be shown.
     * @return True if sting is hex an 16 Bytes long, False otherwise.
     * @see #isHex(String, Context)
     */
    public static boolean isHexAnd16Byte(String hexString, Context context) {
        boolean isHex = isHex(hexString, context);
        if (!isHex) {
            return false;
        }
        if (hexString.length() != 32) {
            // Error, not 16 byte (32 chars).
            Toast.makeText(context, R.string.info_not_16_byte,
                    Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    /**
     * Check if a (hex) string is pure hex (0-9, A-F, a-f).
     * If not show an error Toast in the context.
     * @param hex The string to check.
     * @param context The Context in which an error Toast will be shown.
     * @return True if string is hex. False otherwise.
     */
    public static boolean isHex(String hex, Context context) {
        if (!(hex != null && hex.length() % 2 == 0
                && hex.matches("[0-9A-Fa-f]+"))) {
            // Error, not hex.
            Toast.makeText(context, R.string.info_not_hex_data,
                    Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    /**
     * Check if the given block (hex string) is a value block.
     * NXP has PDFs describing what value blocks are. Google something
     * like "nxp MIFARE classic value block" if you want to have a
     * closer look.
     * @param hexString Block data as hex string.
     * @return True if it is a value block. False otherwise.
     */
    public static boolean isValueBlock(String hexString) {
        byte[] b = Common.hex2Bytes(hexString);
        if (b != null && b.length == 16) {
            // Google some NXP info PDFs about MIFARE Classic to see how
            // Value Blocks are formatted.
            // For better reading (~ = invert operator):
            // if (b0=b8 and b0=~b4) and (b1=b9 and b9=~b5) ...
            // ... and (b12=b14 and b13=b15 and b12=~b13) then
            return (b[0] == b[8] && (byte) (b[0] ^ 0xFF) == b[4]) &&
                (b[1] == b[9] && (byte) (b[1] ^ 0xFF) == b[5]) &&
                (b[2] == b[10] && (byte) (b[2] ^ 0xFF) == b[6]) &&
                (b[3] == b[11] && (byte) (b[3] ^ 0xFF) == b[7]) &&
                (b[12] == b[14] && b[13] == b[15] &&
                    (byte) (b[12] ^ 0xFF) == b[13]);
        }
        return false;
    }

    /**
     * Check if all blocks (lines) contain valid data.
     * @param lines Blocks (incl. their sector header, e.g. "+Sector: 1").
     * @param ignoreAsterisk Ignore lines starting with "*" and move on
     * to the next sector (header).
     * @return <ul>
     * <li>0 - Everything is (most likely) O.K.</li>
     * <li>1 - Found a sector that has not 4 or 16 blocks.</li>
     * <li>2 - Found a block that has invalid characters (not hex or "-" as
     * marker for no key/no data).</li>
     * <li>3 - Found a block that has not 16 bytes (32 chars).</li>
     * <li>4 - A sector index is out of range.</li>
     * <li>5 - Found two times the same sector number (index).
     * Maybe this is a file containing multiple dumps
     * (the dump editor->save->append function was used)</li>
     * <li>6 - There are no lines (lines == null or len(lines) == 0).</li>
     * </ul>
     */
    public static int isValidDump(String[] lines, boolean ignoreAsterisk) {
        ArrayList<Integer> knownSectors = new ArrayList<>();
        int blocksSinceLastSectorHeader = 4;
        boolean is16BlockSector = false;
        if (lines == null || lines.length == 0) {
            // There are no lines.
            return 6;
        }
        for(String line : lines) {
            if ((!is16BlockSector && blocksSinceLastSectorHeader == 4)
                    || (is16BlockSector && blocksSinceLastSectorHeader == 16)) {
                // A sector header is expected.
                if (!line.matches("^\\+Sector: [0-9]{1,2}$")) {
                    // Not a valid sector length or not a valid sector header.
                    return 1;
                }
                int sector;
                try {
                    sector = Integer.parseInt(line.split(": ")[1]);
                } catch (Exception ex) {
                    // Not a valid sector header.
                    // Should not occur due to the previous check (regex).
                    return 1;
                }
                if (sector < 0 || sector > 39) {
                    // Sector out of range.
                    return 4;
                }
                if (knownSectors.contains(sector)) {
                    // Two times the same sector number (index).
                    // Maybe this is a file containing multiple dumps
                    // (the dump editor->save->append function was used).
                    return 5;
                }
                knownSectors.add(sector);
                is16BlockSector = (sector >= 32);
                blocksSinceLastSectorHeader = 0;
                continue;
            }
            if (line.startsWith("*") && ignoreAsterisk) {
                // Ignore line and move to the next sector.
                // (The line was a "No keys found or dead sector" message.)
                is16BlockSector = false;
                blocksSinceLastSectorHeader = 4;
                continue;
            }
            if (!line.matches("[0-9A-Fa-f-]+")) {
                // Not pure hex (or NO_DATA).
                return 2;
            }
            if (line.length() != 32) {
                // Not 32 chars per line.
                return 3;
            }
            blocksSinceLastSectorHeader++;
        }
        return 0;
    }

    /**
     * Check if the user input is a valid key file.
     * Empty lines, leading/tailing whitespaces and comments (marked with #)
     * will be ignored.
     * @param lines Lines of a key file.
     * @return <ul>
     * <li>0 - All O.K.</li>
     * <li>1 - There is no key.</li>
     * <li>2 - At least one key has invalid characters (not hex).</li>
     * <li>3 - At least one key has not 6 byte (12 chars).</li>
     * </ul>
     */
    public static int isValidKeyFile(String[] lines) {
        boolean keyFound = false;
        if (lines == null || lines.length == 0) {
            return 1;
        }
        for (String line : lines) {
            // Remove comments.
            if (line.startsWith("#")) {
                continue;
            }
            line = line.split("#")[0];

            // Ignore leading/tailing whitespaces.
            line = line.trim();

            // Ignore empty lines.
            if (line.equals("")) {
                continue;
            }

            // Is hex?
            if (!line.matches("[0-9A-Fa-f]+")) {
                return 2;
            }

            // Is 6 byte long (12 chars)?
            if (line.length() != 12) {
                return 3;
            }

            // At least one key found.
            keyFound = true;
        }

        if (!keyFound) {
            // No key found.
            return 1;
        }
        return 0;
    }

    /**
     * Show a Toast message with error information according to
     * {@link #isValidDump(String[], boolean)}.
     * @see #isValidDump(String[], boolean)
     */
    public static void isValidDumpErrorToast(int errorCode,
            Context context) {
        switch (errorCode) {
        case 1:
            Toast.makeText(context, R.string.info_valid_dump_not_4_or_16_lines,
                    Toast.LENGTH_LONG).show();
            break;
        case 2:
            Toast.makeText(context, R.string.info_valid_dump_not_hex,
                    Toast.LENGTH_LONG).show();
            break;
        case 3:
            Toast.makeText(context, R.string.info_valid_dump_not_16_bytes,
                    Toast.LENGTH_LONG).show();
            break;
        case 4:
            Toast.makeText(context, R.string.info_valid_dump_sector_range,
                    Toast.LENGTH_LONG).show();
            break;
        case 5:
            Toast.makeText(context, R.string.info_valid_dump_double_sector,
                    Toast.LENGTH_LONG).show();
            break;
        case 6:
            Toast.makeText(context, R.string.info_valid_dump_empty_dump,
                    Toast.LENGTH_LONG).show();
            break;
        }
    }

    /**
     * Show a Toast message with error information according to
     * {@link #isValidKeyFile(String[]).}
     * @return True if all keys were O.K. False otherwise.
     * @see #isValidKeyFile(String[])
     */
    public static boolean isValidKeyFileErrorToast(
            int errorCode, Context context) {
        switch (errorCode) {
            case 0:
                return true;
            case 1:
                Toast.makeText(context, R.string.info_valid_keys_no_keys,
                        Toast.LENGTH_LONG).show();
                break;
            case 2:
                Toast.makeText(context, R.string.info_valid_keys_not_hex,
                        Toast.LENGTH_LONG).show();
                break;
            case 3:
                Toast.makeText(context, R.string.info_valid_keys_not_6_byte,
                        Toast.LENGTH_LONG).show();
                break;
        }
        return false;
    }

    /**
     * Check if a block 0 contains valid data. This covers the first and
     * third byte of the UID, the BCC, the ATQA and the SAK value.
     * The rules for these values have been taken from:
     * UID0/UID3/BCC: https://www.nxp.com/docs/en/application-note/AN10927.pdf section 2.x.x
     * ATQA: https://www.nxp.com/docs/en/application-note/AN10833.pdf section 3.1
     * SAK: https://www.nxp.com/docs/en/application-note/AN10834.pdf (page 7)
     * @param block0 Block 0 as hex string.
     * @param uidLen Length of the UID.
     * @param tagSize Size of the tag according to {@link MifareClassic#getSize()}
     * @return True if block 0 is valid. False otherwise.
     */
    public static boolean isValidBlock0(String block0, int uidLen, int tagSize,
                                        boolean skipBccCheck) {
        if (block0 == null || block0.length() != 32
                || (uidLen != 4 && uidLen != 7 && uidLen != 10)) {
            return false;
        }
        block0 = block0.toUpperCase();
        String byte0 = block0.substring(0, 2);
        String bcc = block0.substring(8, 10);
        int sakStart = (uidLen == 4) ? uidLen * 2 + 2 : uidLen * 2;
        String sak = block0.substring(sakStart, sakStart + 2);
        String atqa = block0.substring(sakStart + 2, sakStart + 6);
        boolean valid = true;
        // BCC.
        if (!skipBccCheck && valid && uidLen == 4) {
            // The 5th byte of block 0 should be the BCC.
            byte byteBcc = hex2Bytes(bcc)[0];
            byte[] uid = hex2Bytes(block0.substring(0, 8));
            valid = isValidBcc(uid, byteBcc);
        }
        // Byte0.
        if (valid && uidLen == 4) {
            // First byte of single size UID must not be 0x88.
            valid = !byte0.equals("88");
        }
        if (valid && uidLen == 4) {
            // First byte of single size UID must not be 0xF8.
            valid = !byte0.equals("F8");
        }
        if (valid && (uidLen == 7 || uidLen == 10)) {
            // First byte of double/triple sized UID shall not be 0x81-0xFE.
            byte firstByte = hex2Bytes(byte0)[0];
            valid = (firstByte < 0x81 || firstByte > 0xFE);
        }
        if (valid && (uidLen == 7 || uidLen == 10)) {
            // First byte of double/triple sized UIDs shall not be 0x00.
            // ISO14443-3 says it's defined in 7816-6 and 7816-6:2016 has
            // still 0x00 as "Reserved for future use by ISO/IEC JTC 1/SC 17".
            valid = !byte0.equals("00");
        }
        // Byte3.
        if (valid && (uidLen == 7 || uidLen == 10)) {
            // The 3rd byte of a double/triple sized UID shall not be 0x88.
            valid = !block0.startsWith("88", 4);
        }
        // ATQA.
        // Check if there is a special ATQA tied to MIFARE SmartMX or TNP3xxx.
        // If not, check if there is a valid ATQA with respect to the UID length
        // and tag size in use.
        if (valid && (atqa.matches("040[1-9A-F]") ||
                atqa.matches("020[1-9A-F]") ||
                atqa.matches("480.") ||
                atqa.matches("010F"))) {
            // Special ATQA value found. Must be SmartMX with MIFARE emulation or TNP3xxx.
            // This is a valid ATQA, do nothing.
        } else if (valid) {
            // Check for common ATQA values.
            if (valid && uidLen == 4 && (tagSize == MifareClassic.SIZE_1K ||
                    tagSize == MifareClassic.SIZE_2K ||
                    tagSize == MifareClassic.SIZE_MINI)) {
                // ATQA must be 0x0400 for a single size UID tag with 320b/1k/2k memory.
                valid = atqa.equals("0400");
            } else if (valid && uidLen == 4 && tagSize == MifareClassic.SIZE_4K) {
                // ATQA must be 0x0200 for a single size UID tag with 4k memory.
                valid = atqa.equals("0200");
            } else if (valid && uidLen == 7 && (tagSize == MifareClassic.SIZE_1K ||
                    tagSize == MifareClassic.SIZE_2K ||
                    tagSize == MifareClassic.SIZE_MINI)) {
                // ATQA must be 0x4400 for a double size UID tag with 320b/1k/2k memory.
                valid = atqa.equals("4400");
            } else if (valid && uidLen == 7 && tagSize == MifareClassic.SIZE_4K) {
                // ATQA must be 0x4200 for a double size UID tag with 4k memory.
                valid = atqa.equals("4200");
            }
        }
        // SAK.
        // Check if there is a valid MIFARE Classic/SmartMX/Plus SAK.
        byte byteSak = hex2Bytes(sak)[0];
        boolean validSak = false;
        if (valid) {
            if ((byteSak >> 1 & 1) == 0) { // SAK bit 2 = 1?
                if ((byteSak >> 3 & 1) == 1) { // SAK bit 4 = 1?
                    if ((byteSak >> 4 & 1) == 1) { // SAK bit 5 = 1?
                        // MIFARE Classic 2K
                        // MIFARE Classic 4K
                        // MIFARE SmartMX 4K
                        // MIFARE Plus S 4K SL1
                        // MIFARE Plus X 4K SL1
                        // MIFARE Plus EV1 2K/4K SL1
                        validSak =  (tagSize == MifareClassic.SIZE_2K ||
                                tagSize == MifareClassic.SIZE_4K);
                    } else {
                        if ((byteSak & 1) == 1) { // SAK bit 1 = 1?
                            // MIFARE Mini
                            validSak = tagSize == MifareClassic.SIZE_MINI;
                        } else {
                            // MIFARE Classic 1k
                            // MIFARE SmartMX 1k
                            // MIFARE Plus S 2K SL1
                            // MIFARE Plus X 2K SL1
                            // MIFARE Plus SE 1K
                            // MIFARE Plus EV1 2K/4K SL1
                            validSak =  (tagSize == MifareClassic.SIZE_2K ||
                                    tagSize == MifareClassic.SIZE_1K);
                        }
                    }
                }
            }
        }
        valid = validSak;

        return valid;
    }

    /**
     * Reverse a byte Array (e.g. Little Endian -> Big Endian).
     * Hmpf! Java has no Array.reverse(). And I don't want to use
     * Commons.Lang (ArrayUtils) from Apache....
     * @param array The array to reverse (in-place).
     */
    public static void reverseByteArrayInPlace(byte[] array) {
        for(int i = 0; i < array.length / 2; i++) {
            byte temp = array[i];
            array[i] = array[array.length - i - 1];
            array[array.length - i - 1] = temp;
        }
    }


    /**
     * Convert byte array to a string of the specified format.
     * Format value corresponds to the pref radio button sequence.
     * @param bytes Bytes to convert.
     * @param fmt Format (0=Hex; 1=DecBE; 2=DecLE).
     * @return The bytes in the specified format.
     */
    public static String byte2FmtString(byte[] bytes, int fmt) {
        switch(fmt) {
            case 2:
                byte[] revBytes = bytes.clone();
                reverseByteArrayInPlace(revBytes);
                return hex2Dec(bytes2Hex(revBytes));
            case 1:
                return hex2Dec(bytes2Hex(bytes));
        }
        return bytes2Hex(bytes);
    }

    /**
     * Convert a hexadecimal string to a decimal string.
     * Uses BigInteger only if the hexadecimal string is longer than 7 bytes.
     * @param hex The hexadecimal value to convert.
     * @return String representation of the decimal value of hexString.
     */
    public static String hex2Dec(String hex) {
        if (!(hex != null && hex.length() % 2 == 0
                && hex.matches("[0-9A-Fa-f]+"))) {
            return null;
        }
        String ret;
        if (hex == null || hex.isEmpty()) {
            ret = "0";
        } else if (hex.length() <= 14) {
            ret = Long.toString(Long.parseLong(hex, 16));
        } else {
            BigInteger bigInteger = new BigInteger(hex , 16);
            ret = bigInteger.toString();
        }
        return ret;
    }

    /**
     * Convert an array of bytes into a string of hex values.
     * @param bytes Bytes to convert.
     * @return The bytes in hex string format.
     */
    public static String bytes2Hex(byte[] bytes) {
        StringBuilder ret = new StringBuilder();
        if (bytes != null) {
            for (Byte b : bytes) {
                ret.append(String.format("%02X", b.intValue() & 0xFF));
            }
        }
        return ret.toString();
    }

    /**
     * Convert a string of hex data into a byte array.
     * Original author is: Dave L. (http://stackoverflow.com/a/140861).
     * @param hex The hex string to convert
     * @return An array of bytes with the values of the string.
     */
    public static byte[] hex2Bytes(String hex) {
        if (!(hex != null && hex.length() % 2 == 0
                && hex.matches("[0-9A-Fa-f]+"))) {
            return null;
        }
        int len = hex.length();
        byte[] data = new byte[len / 2];
        try {
            for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                     + Character.digit(hex.charAt(i+1), 16));
            }
        } catch (Exception e) {
            Log.d(LOG_TAG, "Argument(s) for hexStringToByteArray(String s)"
                    + "was not a hex string");
        }
        return data;
    }

    /**
     * Convert a hex string to ASCII string.
     * @param hex Hex string to convert.
     * @return Converted ASCII string. Null on error.
     */
    public static String hex2Ascii(String hex) {
        if (!(hex != null && hex.length() % 2 == 0
                && hex.matches("[0-9A-Fa-f]+"))) {
            return null;
        }
        byte[] bytes = hex2Bytes(hex);
        String ret;
        // Replace non printable ASCII with ".".
        for(int i = 0; i < bytes.length; i++) {
            if (bytes[i] < (byte)0x20 || bytes[i] == (byte)0x7F) {
                bytes[i] = (byte)0x2E;
            }
        }
        // Hex to ASCII.
        ret = new String(bytes, StandardCharsets.US_ASCII);
        return ret;
    }

    /**
     * Convert a ASCII string to a hex string.
     * @param ascii ASCII string to convert.
     * @return Converted hex string.
     */
    public static String ascii2Hex(String ascii) {
        if (!(ascii != null && !ascii.equals(""))) {
            return null;
        }
        char[] chars = ascii.toCharArray();
        StringBuilder hex = new StringBuilder();
        for (char aChar : chars) {
            hex.append(String.format("%02X", (int) aChar));
        }
        return hex.toString();
    }

    /**
     * Convert a hex string to a binary string (with leading zeros).
     * @param hex Hex string to convert.
     * @return Converted binary string.
     */
    public static String hex2Bin(String hex) {
        if (!(hex != null && hex.length() % 2 == 0
                && hex.matches("[0-9A-Fa-f]+"))) {
            return null;
        }
        String bin = new BigInteger(hex, 16).toString(2);
        // Pad left with zeros (have not found a better way...).
        if(bin.length() < hex.length() * 4){
            int diff = hex.length() * 4 - bin.length();
            StringBuilder pad = new StringBuilder();
            for(int i = 0; i < diff; i++){
                pad.append("0");
            }
            pad.append(bin);
            bin = pad.toString();
        }
        return bin;
    }

    public static String bin2Hex(String bin) {
        if (!(bin != null && bin.length() % 8 == 0
                && bin.matches("[0-1]+"))) {
            return null;
        }
        String hex = new BigInteger(bin, 2).toString(16);
        if (hex.length() % 2 != 0) {
            hex = "0" + hex;
        }
        return hex;
    }

    /**
     * Create a colored string.
     * @param data The text to be colored.
     * @param color The color for the text.
     * @return A colored string.
     */
    public static SpannableString colorString(String data, int color) {
        SpannableString ret = new SpannableString(data);
        ret.setSpan(new ForegroundColorSpan(color),
                0, data.length(), 0);
        return ret;
    }

    /**
     * Copy a text to the Android clipboard.
     * @param text The text that should by stored on the clipboard.
     * @param context Context of the SystemService
     * (and the Toast message that will by shown).
     * @param showMsg Show a "Copied to clipboard" message.
     */
    public static void copyToClipboard(String text, Context context,
                                       boolean showMsg) {
        if (!text.equals("")) {
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager)
                    context.getSystemService(
                            Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip =
                    android.content.ClipData.newPlainText(
                            "MIFARE Classic Tool data", text);
            clipboard.setPrimaryClip(clip);
            if (showMsg) {
                Toast.makeText(context, R.string.info_copied_to_clipboard,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Get the content of the Android clipboard (if it is plain text).
     * @param context Context of the SystemService
     * @return The content of the Android clipboard. On error
     * (clipboard empty, clipboard content not plain text, etc.) null will
     * be returned.
     */
    public static String getFromClipboard(Context context) {
        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager)
                context.getSystemService(
                        Context.CLIPBOARD_SERVICE);
        if (clipboard.getPrimaryClip() != null
                && clipboard.getPrimaryClip().getItemCount() > 0
                && clipboard.getPrimaryClipDescription().hasMimeType(
                    android.content.ClipDescription.MIMETYPE_TEXT_PLAIN)
                && clipboard.getPrimaryClip().getItemAt(0) != null
                && clipboard.getPrimaryClip().getItemAt(0)
                    .getText() != null) {
            return clipboard.getPrimaryClip().getItemAt(0)
                    .getText().toString();
        }

        // Error.
        return null;
    }

    /**
     * Share a file from the "tmp" directory as attachment.
     * @param context The context the FileProvider and the share intent.
     * @param file The file to share (from the "tmp" directory).
     * @see #TMP_DIR
     */
    public static void shareTextFile(Context context, File file) {
        // Share file.
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Uri uri;
        try {
            uri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".fileprovider", file);
        } catch (IllegalArgumentException ex) {
            Toast.makeText(context, R.string.info_share_error,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        intent.setDataAndType(uri, "text/plain");
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        context.startActivity(Intent.createChooser(intent,
                context.getText(R.string.dialog_share_title)));
    }

    /**
     * Copy file.
     * @param in Input file (source).
     * @param out Output file (destination).
     * @throws IOException Error upon coping.
     */
    public static void copyFile(InputStream in, OutputStream out)
            throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
          out.write(buffer, 0, read);
        }
    }

    /**
     * Convert Dips to pixels.
     * @param dp Dips.
     * @return Dips as px.
     */
    public static int dpToPx(int dp) {
        return (int) (dp * mScale + 0.5f);
    }

    /**
     * Get the current active (last detected) Tag.
     * @return The current active Tag.
     * @see #mTag
     */
    public static Tag getTag() {
        return mTag;
    }

    /**
     * Set the new active Tag (and update {@link #mUID}).
     * @param tag The new Tag.
     * @see #mTag
     * @see #mUID
     */
    public static void setTag(Tag tag) {
        mTag = tag;
        mUID = tag.getId();
    }

    /**
     * Get the App wide used NFC adapter.
     * @return NFC adapter.
     */
    public static NfcAdapter getNfcAdapter() {
        return mNfcAdapter;
    }

    /**
     * Set the App wide used NFC adapter.
     * @param nfcAdapter The NFC adapter that should be used.
     */
    public static void setNfcAdapter(NfcAdapter nfcAdapter) {
        mNfcAdapter = nfcAdapter;
    }

    /**
     * Remember the choice whether to use MCT in editor only mode or not.
     * @param value True if the user wants to use MCT in editor only mode.
     */
    public static void setUseAsEditorOnly(boolean value) {
        mUseAsEditorOnly = value;
    }

    /**
     * Get the key map generated by
     * {@link de.syss.MifareClassicTool.Activities.KeyMapCreator}.
     * @return A key map (see {@link MCReader#getKeyMap()}).
     */
    public static SparseArray<byte[][]> getKeyMap() {
        return mKeyMap;
    }

    /**
     * Set {@link #mKeyMapFrom} and {@link #mKeyMapTo}.
     * The {@link de.syss.MifareClassicTool.Activities.KeyMapCreator} will do
     * this for every created key map.
     * @param from {@link #mKeyMapFrom}
     * @param to {@link #mKeyMapTo}
     */
    public static void setKeyMapRange (int from, int to){
        mKeyMapFrom = from;
        mKeyMapTo = to;
    }

    /**
     * Get the key map start point.
     * @return {@link #mKeyMapFrom}
     */
    public static int getKeyMapRangeFrom() {
        return mKeyMapFrom;
    }

    /**
     * Get the key map end point
     * @return {@link #mKeyMapTo}
     */
    public static int getKeyMapRangeTo() {
        return mKeyMapTo;
    }

    /**
     * Set the key map.
     * @param value A key map (see {@link MCReader#getKeyMap()}).
     */
    public static void setKeyMap(SparseArray<byte[][]> value) {
        mKeyMap = value;
    }

    /**
     * Set the compnent name of a new pending activity.
     * @param pendingActivity The new pending activities component name.
     * @see #mPendingComponentName
     */
    public static void setPendingComponentName(ComponentName pendingActivity) {
        mPendingComponentName = pendingActivity;
    }

    /**
     * Get the component name of the current pending activity.
     * @return The compnent name of the current pending activity.
     * @see #mPendingComponentName
     */
    public static ComponentName getPendingComponentName() {
        return mPendingComponentName;
    }

    /**
     * Get the UID of the current tag.
     * @return The UID of the current tag.
     * @see #mUID
     */
    public static byte[] getUID() {
        return mUID;
    }

    /**
     * Check whether the provided BCC is valid for the UID or not. The BCC
     * is the first byte after the UID in the manufacturers block. It
     * is calculated by XOR-ing all bytes of the UID.
     * @param uid The UID to calculate the BCC from.
     * @param bcc The BCC the calculated BCC gets compared with.
     * @return True if the BCC if valid for the UID. False otherwise.
     */
    public static boolean isValidBcc(byte[] uid, byte bcc) {
        return calcBcc(uid) == bcc;
    }

    /**
     * Calculate the BCC of a 4 byte UID. For tags with a 4 byte UID the
     * BCC is the first byte after the UID in the manufacturers block.
     * It is calculated by XOR-ing the 4 bytes of the UID.
     * @param uid The UID of which the BCC should be calculated.
     * @exception IllegalArgumentException Thrown if the uid parameter
     * has not 4 bytes.
     * @return The BCC of the given UID.
     */
    public static byte calcBcc(byte[] uid) throws IllegalArgumentException {
        if (uid.length != 4) {
            throw new IllegalArgumentException("UID length is not 4 bytes.");
        }
        byte bcc = uid[0];
        for(int i = 1; i < uid.length; i++) {
            bcc = (byte)(bcc ^ uid[i]);
        }
        return bcc;
    }

    /**
     * Get the version code.
     * @return The version code.
     */
    public static String getVersionCode() {
        return mVersionCode;
    }

    /**
     * If NFC is disabled and the user chose to use MCT in editor only mode,
     * this method will return true.
     * @return True if the user wants to use MCT in editor only mode.
     * False otherwise.
     */
    public static boolean useAsEditorOnly() {
        return mUseAsEditorOnly;
    }


}
