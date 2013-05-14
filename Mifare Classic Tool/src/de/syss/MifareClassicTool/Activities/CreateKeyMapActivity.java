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
import java.util.Arrays;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.MCReader;
import de.syss.MifareClassicTool.R;

/**
 * Configure key map process and create key map.
 * This Activity should be called via startActivityForResult() with
 * an Intent containing the {@link #EXTRA_KEYS_DIR}.
 * The result codes are:
 * <ul>
 * <li>{@link Activity#RESULT_OK} - Everything is O.K. The key map can be
 * retrieved by calling {@link Common#getKeyMap()}.</li>
 * <li>1 - Directory from {@link #EXTRA_KEYS_DIR} does not
 * exist.</li>
 * <li>2 - No directory specified in Intent
 * ({@link #EXTRA_KEYS_DIR})</li>
 * <li>3 - External Storage is not read/writable. This error is
 * displayed to the user via Toast.</li>
 * <li>4 - No key was found. {@link Common#getKeyMap()} will return "null".</li>
 * </ul>
 * @author Gerhard Klostermeier
 */
public class CreateKeyMapActivity extends BasicActivity {

    // Input parameters.
    /**
     * Path to a directory with key files. The files in the directory
     * are the files the user can choose from. This must be in the Intent.
     */
    public final static String EXTRA_KEYS_DIR =
            "de.syss.MifareClassicTool.Activity.KEYS_DIR";

    /**
     * A boolean value to enable (default) or disable the possibility for the
     * user to change the key mapping range.
     */
    public final static String EXTRA_SECTOR_CHOOSER =
            "de.syss.MifareClassicTool.Activity.ECTOR_CHOOSER";
    /**
     * An integer value that represents the number of the
     * first sector for the key mapping process.
     */
    public final static String EXTRA_SECTOR_CHOOSER_FROM =
            "de.syss.MifareClassicTool.Activity.SECTOR_CHOOSER_FROM";
    /**
     * An integer value that represents the number of the
     * last sector for the key mapping process.
     */
    public final static String EXTRA_SECTOR_CHOOSER_TO =
            "de.syss.MifareClassicTool.Activity.SECTOR_CHOOSER_TO";
    /**
     * The title of the activity. Optional.
     * e.g. "Map Keys to Sectors"
     */
    public final static String EXTRA_TITLE =
            "de.syss.MifareClassicTool.Activity.TITLE";
    /**
     * The text of the start key mapping button. Optional.
     * e.g. "Map Keys to Sectors"
     */
    public final static String EXTRA_BUTTON_TEXT =
            "de.syss.MifareClassicTool.Activity.BUTTON_TEXT";

    // Output parameters.
    // For later use.
//    public final static String EXTRA_KEY_MAP =
//            "de.syss.MifareClassicTool.Activity.KEY_MAP";


    // Sector count of the biggest Mifare Classic tag (4K Tag)
    public static final int MAX_SECTOR_COUNT = 40;
    // Block count of the biggest sector (4K Tag, Sector 32-39)
    public static final int MAX_BLOCK_COUNT_PER_SECTOR = 16;

    private static final String LOG_TAG =
            CreateKeyMapActivity.class.getSimpleName();

    private static final int DEFAULT_SECTOR_RANGE_FROM = 0;
    private static final int DEFAULT_SECTOR_RANGE_TO = 15;

    private Button mCreateKeyMap;
    private LinearLayout mKeyFilesGroup;
    private TextView mSectorRange;
    private Button mChangeSectorRange;
    private Handler mHandler = new Handler();
    private int mProgressStatus;
    private ProgressBar mProgressBar;
    private boolean mIsCreatingKeyMap;
    private String mKeyDirPath;
    private int mFirstSector;
    private int mLastSector;

    /**
     * Set layout, set the mapping range
     * and initialize some member variables.
     * @see #EXTRA_SECTOR_CHOOSER
     * @see #EXTRA_SECTOR_CHOOSER_FROM
     * @see #EXTRA_SECTOR_CHOOSER_TO
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_key_map);
        mCreateKeyMap = (Button) findViewById(R.id.buttonCreateKeyMap);
        mChangeSectorRange = (Button) findViewById(
                R.id.buttonCreateKeyMapChangeRange);
        mSectorRange = (TextView) findViewById(R.id.textViewCreateKeyMapFromTo);
        mKeyFilesGroup = (LinearLayout) findViewById(
                R.id.LinearLayoutCreateKeyMapKeyFiles);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBarCreateKeyMap);

        // Init. sector range.
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_SECTOR_CHOOSER)) {
            boolean value = intent.getBooleanExtra(EXTRA_SECTOR_CHOOSER, true);
            mChangeSectorRange.setEnabled(value);
        }
        int from = DEFAULT_SECTOR_RANGE_FROM;
        int to = DEFAULT_SECTOR_RANGE_TO;
        boolean custom = false;
        if (intent.hasExtra(EXTRA_SECTOR_CHOOSER_FROM)) {
            from = intent.getIntExtra(EXTRA_SECTOR_CHOOSER_FROM, 0);
            custom = true;
        }
        if (intent.hasExtra(EXTRA_SECTOR_CHOOSER_TO)) {
            to = intent.getIntExtra(EXTRA_SECTOR_CHOOSER_TO, 15);
            custom = true;
        }
        if (custom) {
            mSectorRange.setText((from) + " - " + (to));
        }

        // Init. title and button text.
        if (intent.hasExtra(EXTRA_TITLE)) {
            setTitle(intent.getStringExtra(EXTRA_TITLE));
        }
        if (intent.hasExtra(EXTRA_BUTTON_TEXT)) {
            ((Button) findViewById(R.id.buttonCreateKeyMap)).setText(
                    intent.getStringExtra(EXTRA_BUTTON_TEXT));
        }
    }

    /**
     * Cancel the mapping process and disable NFC foreground dispatch system.
     * This method is not called, if screen orientation changes.
     * @see Common#disableNfcForegroundDispatch(Activity)
     */
    @Override
    public void onPause() {
        super.onPause();
        mIsCreatingKeyMap = false;
    }

    /**
     * List files from the {@link #EXTRA_KEYS_DIR}.
     * Also restore the last marked key file (if there was one).
     */
    @Override
    public void onStart() {
        super.onStart();

        File dir = null;
        // Is there a key directory in the Intent?
        if (!getIntent().hasExtra(EXTRA_KEYS_DIR)) {
            setResult(2);
            finish();
        }
        dir = new File(getIntent().getStringExtra(EXTRA_KEYS_DIR));
        // Is external storage writable?
        if (!Common.isExternalStorageWritableErrorToast(this)) {
            setResult(3);
            finish();
        }
        // Does the directory exist?
        if (!dir.exists()) {
            setResult(1);
            finish();
        }

        // List key files.
        mKeyDirPath = dir.getPath();
        File[] keyFiles = dir.listFiles();
        Arrays.sort(keyFiles);
        mKeyFilesGroup.removeAllViews();
        for(File f : keyFiles) {
            CheckBox c = new CheckBox(this);
            c.setText(f.getName());
            mKeyFilesGroup.addView(c);
        }
    }

    /**
     * Select all of the key files.
     * @param view The View object that triggered the method
     * (in this case the select all button).
     */
    public void onSelectAll(View view) {
        selectKeyFiles(true);
    }

    /**
     * Select none of the key files.
     * @param view The View object that triggered the method
     * (in this case the select none button).
     */
    public void onSelectNone(View view) {
        selectKeyFiles(false);
    }

    /**
     * Select or deselect all key files.
     * @param allOrNone True for selecting all, False for none.
     */
    private void selectKeyFiles(boolean allOrNone) {
        for (int i = 0; i < mKeyFilesGroup.getChildCount(); i++) {
            CheckBox c = (CheckBox) mKeyFilesGroup.getChildAt(i);
            c.setChecked(allOrNone);
        }
    }

    /**
     * Inform the worker thread from {@link #createKeyMap(MCReader)}
     * to stop creating the key map. If the thread is already
     * informed or does not exists this button will finish the activity.
     * @param view The View object that triggered the method
     * (in this case the cancel button).
     * @see #createKeyMap(MCReader)
     */
    public void onCancelCreateKeyMap(View view) {
        if (mIsCreatingKeyMap == true) {
            mIsCreatingKeyMap = false;
        } else {
            finish();
        }
    }

    /**
     * Create a key map and save it to
     * {@link Common#setKeyMap(android.util.SparseArray)}.
     * For doing so it uses other methods (
     * {@link #createKeyMap(MCReader)}, {@link #keyMapCreated(MCReader)}).
     * @param view The View object that triggered the method
     * (in this case the map keys to sectors button).
     * @see #createKeyMap(MCReader)
     * @see #keyMapCreated(MCReader)
     */
    public void onCreateKeyMap(View view) {
        // Check for checked chek boxes.
        ArrayList<String> fileNames = new ArrayList<String>();
        for (int i = 0; i < mKeyFilesGroup.getChildCount(); i++) {
            CheckBox c = (CheckBox) mKeyFilesGroup.getChildAt(i);
            if (c.isChecked()) {
                fileNames.add(c.getText().toString());
            }
        }
        if (fileNames.size() > 0) {
            // Check if key files still exists.
            ArrayList<File> keyFiles = new ArrayList<File>();
            for (String fileName : fileNames) {
                File keyFile = new File(mKeyDirPath, fileName);
                if (keyFile.exists()) {
                    keyFiles.add(keyFile);
                } else {
                    Log.d(LOG_TAG, "Key file "
                            + keyFile.getAbsolutePath()
                            + "doesn't exists anymore.");
                }
            }
            if (keyFiles.size() > 0) {
                MCReader reader = Common.checkForTagAndCreateReader(this);
                if (reader == null) {
                    return;
                }

                // Set key files.
                File[] keys = keyFiles.toArray(new File[keyFiles.size()]);
                reader.setKeyFile(keys);
                // Don't turn screen of while mapping.
                getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                // Get key map range.
                if (mSectorRange.getText().toString().equals(
                        getString(R.string.text_sector_range_all))) {
                    // Read all.
                    mFirstSector = 0;
                    mLastSector = reader.getSectorCount()-1;
                } else {
                    String[] fromAndTo = mSectorRange.getText()
                            .toString().split(" ");
                    mFirstSector = Integer.parseInt(fromAndTo[0]);
                    mLastSector = Integer.parseInt(fromAndTo[2]);
                }
                // Set map creation range.
                if (!reader.setMappingRange(
                        mFirstSector, mLastSector)) {
                    Toast.makeText(this,
                            R.string.info_mapping_sector_out_of_range,
                            Toast.LENGTH_LONG).show();
                    reader.close();
                    return;
                }
                // Init. GUI elements.
                mProgressStatus = -1;
                mProgressBar.setMax((mLastSector-mFirstSector)+1);
                mCreateKeyMap.setEnabled(false);
                mIsCreatingKeyMap = true;
                Toast.makeText(this, R.string.info_wait_key_map,
                        Toast.LENGTH_SHORT).show();
                // Read as much as possible with given key file.
                createKeyMap(reader);
            }
        }
    }

    /**
     * Triggered by {@link #onCreateKeyMap(View)} this
     * method starts a worker thread that first creates a key map and then
     * calls {@link #keyMapCreated(MCReader)}.
     * It also updates the progress bar in the UI thread.
     * @param reader A connected {@link MCReader}.
     * @see #onCreateKeyMap(View)
     * @see #keyMapCreated(MCReader)
     */
    private void createKeyMap(final MCReader reader) {
        new Thread(new Runnable() {
            public void run() {
                // Build key map parts and update the progress bar.
                while (mProgressStatus < mLastSector) {
                    mProgressStatus = reader.buildNextKeyMapPart();
                    if (mProgressStatus == -1 || mIsCreatingKeyMap == false) {
                        // Error while building next key map part.
                        break;
                    }

                    mHandler.post(new Runnable() {
                        public void run() {
                            mProgressBar.setProgress(
                                    (mProgressStatus - mFirstSector) + 1);
                        }
                    });
                }

                mHandler.post(new Runnable() {
                    public void run() {
                        getWindow().clearFlags(
                                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        mProgressBar.setProgress(0);
                        mCreateKeyMap.setEnabled(true);
                        reader.close();
                        if (mIsCreatingKeyMap == true
                                && mProgressStatus != -1) {
                            keyMapCreated(reader);
                        } else {
                            Common.setKeyMap(null);
                        }
                        mIsCreatingKeyMap = false;
                    }
                });
            }
        }).start();
    }

    /**
     * Triggered by {@link #createKeyMap(MCReader)}, this method
     * sets the result code to {@link Activity#RESULT_OK},
     * saves the created key map to
     * {@link Common#setKeyMap(android.util.SparseArray)}
     * and finishes this Activity.
     * @param reader A {@link MCReader}.
     * @see #createKeyMap(MCReader)
     * @see #onCreateKeyMap(View)
     */
    private void keyMapCreated(MCReader reader) {
        // LOW: Return key map in intent.
        if (reader.getKeyMap().size() == 0) {
            Common.setKeyMap(null);
            setResult(4);
        } else {
            Common.setKeyMap(reader.getKeyMap());
//            Intent intent = new Intent();
//            intent.putExtra(EXTRA_KEY_MAP, mMCReader);
//            setResult(Activity.RESULT_OK, intent);
            setResult(Activity.RESULT_OK);
        }
        finish();
    }

    /**
     * Show a dialog which lets the user choose the key mapping range.
     * @param view The View object that triggered the method
     * (in this case the change button).
     */
    public void onChangeSectorRange(View view) {
        // Build dialog elements.
        LinearLayout ll = new LinearLayout(this);
        ll.setPadding(20, 20, 20, 20);
        ll.setGravity(Gravity.CENTER);
        TextView tvFrom = new TextView(this);
        tvFrom.setText(getString(R.string.text_from) + ": ");
        tvFrom.setTextSize(18);
        TextView tvTo = new TextView(this);
        tvTo.setText(" " + getString(R.string.text_to) + ": ");
        tvTo.setTextSize(18);

        InputFilter[] f = new InputFilter[1];
        f[0] = new InputFilter.LengthFilter(2);
        final EditText from = new EditText(this);
        from.setEllipsize(TruncateAt.END);
        from.setMaxLines(1);
        from.setSingleLine();
        from.setInputType(InputType.TYPE_CLASS_NUMBER);
        from.setMinimumWidth(60);
        from.setFilters(f);
        from.setGravity(Gravity.CENTER_HORIZONTAL);
        final EditText to = new EditText(this);
        to.setEllipsize(TruncateAt.END);
        to.setMaxLines(1);
        to.setSingleLine();
        to.setInputType(InputType.TYPE_CLASS_NUMBER);
        to.setMinimumWidth(60);
        to.setFilters(f);
        to.setGravity(Gravity.CENTER_HORIZONTAL);

        ll.addView(tvFrom);
        ll.addView(from);
        ll.addView(tvTo);
        ll.addView(to);
        final Toast err = Toast.makeText(this,
                R.string.info_invalid_range, Toast.LENGTH_LONG);
        // Build dialog and show him.
        new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_mapping_range_title)
            .setMessage(R.string.dialog_mapping_range)
            .setView(ll)
            .setPositiveButton(R.string.button_ok,
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Read from x to y.
                    String txtFrom = "" + (DEFAULT_SECTOR_RANGE_FROM);
                    String txtTo = "" + (DEFAULT_SECTOR_RANGE_TO);
                    if (!from.getText().toString().equals("")) {
                        txtFrom = from.getText().toString();
                    }
                    if (!to.getText().toString().equals("")) {
                        txtTo = to.getText().toString();
                    }
                    int intFrom = Integer.parseInt(txtFrom);
                    int intTo = Integer.parseInt(txtTo);
                    if (intFrom > intTo || intFrom < 0
                            || intTo > MAX_SECTOR_COUNT-1) {
                        // Error.
                        err.show();
                    } else {
                        mSectorRange.setText(txtFrom + " - " + txtTo);
                    }
                }
            })
            .setNeutralButton(R.string.button_read_all_sectors,
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Read all sectors.
                    mSectorRange.setText(
                            getString(R.string.text_sector_range_all));
                }
            })
            .setNegativeButton(R.string.button_cancel,
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Cancel dialog (do nothing).
                }
            }).show();
    }
}
