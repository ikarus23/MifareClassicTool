package de.syss.MifareClassicTool.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Toast;
import android.nfc.tech.MifareClassic;
import android.util.SparseArray;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.MCReader;
import de.syss.MifareClassicTool.R;
import de.syss.MifareClassicTool.Fragments.FactoryFormatFragment;
import de.syss.MifareClassicTool.Fragments.ValueBlockFragment;
import de.syss.MifareClassicTool.Fragments.WriteBlockFragment;
import de.syss.MifareClassicTool.Fragments.WriteDumpFragment;

/**
 * Write data to tag. The user can choose to write
 * a single block of data or to write a dump to a tag providing its keys
 * or to factory format a tag.
 * @author Gerhard Klostermeier
 */

public class WriteTagFragmentActivity extends BasicFragmentActivity implements WriteBlockFragment.OnWriteBlockListener, WriteDumpFragment.OnWriteDumpListener, FactoryFormatFragment.OnFactoryFormatListener, ValueBlockFragment.OnWriteValueListener {

    /**
     * The corresponding Intent will contain a dump. Headers
     * (e.g. "Sector: 1") are marked with a "+"-symbol (e.g. "+Sector: 1").
     */
    public final static String EXTRA_DUMP = "de.syss.MifareClassicTool.Activity.DUMP";
    private static final int FC_WRITE_DUMP = 1;
    private static final int CKM_WRITE_DUMP = 2;
    private static final int CKM_WRITE_BLOCK = 3;
    private static final int CKM_FACTORY_FORMAT = 4;
    private static final int CKM_WRITE_NEW_VALUE = 5;
    private HashMap<Integer, HashMap<Integer, byte[]>> mDumpWithPos;
    private boolean mWriteDumpFromEditor = false;
    private String[] mDumpFromEditor;
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_tag_fragment_adapter);

        /**
         * Set Toolbar (R.id.toolbar) title in layout
         * ({@link de.syss.MifareClassicTool.R.layout#activity_write_tag_fragment_adapter})
         */

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getResources().getString(R.string.title_activity_write_tag));

        /**
         * Setup ViewPager to add ViewPagerAdapter
         * {@link #setupViewPager}
         * {@link ViewPagerAdapter}
         */

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        /**
         * Set TabLayout (R.id.tabLayout) with ViewPager (R.id.viewPager) in layout
         * ({@link de.syss.MifareClassicTool.R.layout#activity_write_tag_fragment_adapter})
         */

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);


        // Restore mDumpWithPos and the "write to manufacturer block"-state.
        if (savedInstanceState != null) {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(getCurrentFragmentTag());
            if (fragment instanceof WriteDumpFragment){
                ((WriteDumpFragment) fragment).getCheckBoxWriteTagDumpWriteManuf().setChecked(
                        savedInstanceState.getBoolean("write_manuf_block", false));
            }
            Serializable s = savedInstanceState
                    .getSerializable("dump_with_pos");
            if (s instanceof HashMap<?, ?>) {
                mDumpWithPos = (HashMap<Integer, HashMap<Integer, byte[]>>) s;
            }
        }

        Intent i = getIntent();
        if (i.hasExtra(EXTRA_DUMP)) {
            // Write dump directly from editor.
            mDumpFromEditor = i.getStringArrayExtra(EXTRA_DUMP);
            mWriteDumpFromEditor = true;
            // Update button text.
            Button writeDumpButton = findViewById(
                    R.id.buttonWriteTagDump);
            writeDumpButton.setText(R.string.action_write_dump);
        }

    }

    /**
     * Add fragments to {@link ViewPagerAdapter} and then set adapter to viewpager
     * @param viewPager
     * in this case R.id.viewpager in layout
     * ({@link de.syss.MifareClassicTool.R.layout#activity_write_tag_fragment_adapter})
     */

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFrag(new WriteBlockFragment(), getString(R.string.action_wirte_block));
        adapter.addFrag(new WriteDumpFragment(), getString(R.string.action_write_dump_clone));
        adapter.addFrag(new FactoryFormatFragment(), getString(R.string.action_factory_format));
        adapter.addFrag(new ValueBlockFragment(), getString(R.string.text_incr_decr_value_block));
        viewPager.setAdapter(adapter);
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFrag(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

    /**
     * Fragments are attached to activity and listener (this)
     * should be set to each fragment
     * @param fragment One of the four fragments in the ViewPagerAdapter
     */

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof WriteBlockFragment) {
            WriteBlockFragment writeBlockFragment = (WriteBlockFragment) fragment;
            writeBlockFragment.setOnWriteBlockListener(this);
        } else if (fragment instanceof WriteDumpFragment) {
            WriteDumpFragment writeDumpFragment = (WriteDumpFragment) fragment;
            writeDumpFragment.setOnWriteBlockListener(this);
        } else if (fragment instanceof FactoryFormatFragment) {
            FactoryFormatFragment factoryFormatFragment = (FactoryFormatFragment) fragment;
            factoryFormatFragment.setOnFactoryFormatListener(this);
        } else if (fragment instanceof ValueBlockFragment) {
            ValueBlockFragment valueBlockFragment = (ValueBlockFragment) fragment;
            valueBlockFragment.setOnWriteValueListener(this);
        }
    }


    /**
     * Interface method implementation
     * {@link WriteBlockFragment.OnWriteBlockListener}
     */

    @Override
    public void onWriteBlockButtonClick() {
        onWriteBlock();
    }

    /**
     * Interface methods implementation
     * {@link WriteDumpFragment.OnWriteDumpListener}
     */

    @Override
    public void onWriteDumpButtonClick() {
        onWriteDump();
    }

    @Override
    public void onShowStaticACInfoClick() {
        onShowStaticACInfo();
    }

    @Override
    public void onShowWriteManufInfoClick() {
        onShowWriteManufInfo();
    }

    /**
     * Interface method implementation
     * {@link FactoryFormatFragment.OnFactoryFormatListener}
     */

    @Override
    public void onFactoryFormatButtonClick() {
        onFactoryFormat();
    }

    /**
     * Interface method implementation
     * {@link ValueBlockFragment.OnWriteValueListener}
     */

    @Override
    public void onWriteValueButtonClick() {
        onWriteValue();
    }


    /**
     * Check the user input and, if necessary, the BCC value
     * ({@link #checkBCC(boolean)}). If everythin is O.K., show the
     * {@link KeyMapCreator} with predefined mapping range (se
     * {@link #createKeyMapForBlock(int, boolean)}).
     * After a key map was created, {@link #writeBlock()} will be triggered.
     * @see KeyMapCreator
     * @see #checkBCC(boolean)
     * @see #createKeyMapForBlock(int, boolean)
     */
    public void onWriteBlock() {
        // Check input.
        EditText mSectorTextBlock = null;
        EditText mBlockTextBlock = null;
        EditText mDataText = null;
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(getCurrentFragmentTag());
        if (fragment instanceof WriteBlockFragment) {
            mSectorTextBlock = ((WriteBlockFragment) fragment).getmSectorTextBlock();
            mBlockTextBlock = ((WriteBlockFragment) fragment).getmBlockTextBlock();
            mDataText = ((WriteBlockFragment) fragment).getmDataText();
        }
        if (mSectorTextBlock == null ||
                mBlockTextBlock == null ||
                mDataText == null ||
                !checkSectorAndBlock(mSectorTextBlock, mBlockTextBlock)) {
            return;
        }
        String data = mDataText.getText().toString();
        if (!Common.isHexAnd16Byte(data, this)) {
            return;
        }

        final int sector = Integer.parseInt(
                mSectorTextBlock.getText().toString());
        final int block = Integer.parseInt(
                mBlockTextBlock.getText().toString());

        if (!isSectorInRage(this, true)) {
            return;
        }

        if (block == 3 || block == 15) {
            // Warning. This is a sector trailer.
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_sector_trailer_warning_title)
                    .setMessage(R.string.dialog_sector_trailer_warning)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(R.string.action_i_know_what_i_am_doing,
                            (dialog, which) -> {
                                // Show key map creator.
                                createKeyMapForBlock(sector, false);
                            })
                    .setNegativeButton(R.string.action_cancel,
                            (dialog, id) -> {
                                // Do nothing.
                            }).show();
        } else if (sector == 0 && block == 0) {
            // Is the BCC valid?
            int bccCheck = checkBCC(true);
            if (bccCheck == 0 || bccCheck > 2) {
                // Warning. Writing to manufacturer block.
                showWriteManufInfo(true);
            }
        } else {
            createKeyMapForBlock(sector, false);
        }
    }

    /**
     * Open key map creator.
     * @see KeyMapCreator
     */
    public void onFactoryFormat() {
        // Show key map creator.
        Intent intent = new Intent(this, KeyMapCreator.class);
        intent.putExtra(KeyMapCreator.EXTRA_KEYS_DIR,
                Common.getFileFromStorage(Common.HOME_DIR + "/" +
                        Common.KEYS_DIR).getAbsolutePath());
        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER, false);
        intent.putExtra(KeyMapCreator.EXTRA_BUTTON_TEXT,
                getString(R.string.action_create_key_map_and_factory_format));
        startActivityForResult(intent, CKM_FACTORY_FORMAT);
    }


    /**
     * Check the user input of the sector and the block field. This is a
     * helper function for {@link #onWriteBlock()} and
     * {@link #onWriteBlock()}.
     * @param sector Sector input field.
     * @param block Block input field.
     * @return True if both values are okay. False otherwise.
     */
    private boolean checkSectorAndBlock(EditText sector, EditText block) {
        if (sector.getText().toString().equals("")
                || block.getText().toString().equals("")) {
            // Error, location not fully set.
            Toast.makeText(this, R.string.info_data_location_not_set,
                    Toast.LENGTH_LONG).show();
            return false;
        }
        int sectorNr = Integer.parseInt(sector.getText().toString());
        int blockNr = Integer.parseInt(block.getText().toString());
        if (sectorNr > KeyMapCreator.MAX_SECTOR_COUNT-1
                || sectorNr < 0) {
            // Error, sector is out of range for any MIFARE tag.
            Toast.makeText(this, R.string.info_sector_out_of_range,
                    Toast.LENGTH_LONG).show();
            return false;
        }
        if (blockNr > KeyMapCreator.MAX_BLOCK_COUNT_PER_SECTOR-1
                || blockNr < 0) {
            // Error, block is out of range for any MIFARE tag.
            Toast.makeText(this, R.string.info_block_out_of_range,
                    Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    /**
     * Check if the BCC of the dump ({@link #mDumpWithPos}) or of the block
     * ({@link WriteBlockFragment#getmDataText()}) is valid and show a error message if needed.
     * This check is only for 4 byte UIDs.
     * @param isWriteBlock If Ture, the UID and BCC are taken from
     * the {@link WriteBlockFragment#getmDataText()} input field. If False, the UID and BCC
     * is taken from the {@link #mDumpWithPos} dump.
     * @return <ul>
     * <li>0 - Everything is O.K.</li>
     * <li>1 - BCC is not valid.</li>
     * <li>2 - There is no tag.</li>
     * <li>3 - UID is not 4 bytes long.</li>
     * <li>4 - Dump does not contain the first sector.</li>
     * <li>5 - Dump does not contain the block 0.</li>
     * </ul>
     */
    private int checkBCC(boolean isWriteBlock) {
        MCReader reader = Common.checkForTagAndCreateReader(this);
        if (reader == null) {
            // Error. There is no tag.
            return 2;
        }
        reader.close();

        int uidLen = Common.getUID().length;
        if (uidLen != 4) {
            // UID is not 4 bytes long. The BCC does not matter for
            // tags with 7 byte UIDs.
            return 3;
        }

        byte bcc;
        byte[] uid;
        EditText mDataText = null;
        // The length of UID of the dump or the block 0 is expected to match
        // the UID length of the current tag. In this case 4 byte.
        if (isWriteBlock) {
            WriteBlockFragment writeBlockFragment = (WriteBlockFragment)getSupportFragmentManager().findFragmentByTag(getCurrentFragmentTag());
            bcc = Common.hexStringToByteArray(
                    writeBlockFragment.getmDataText().getText().toString()
                            .substring(8, 10))[0];
            uid = Common.hexStringToByteArray(
                    writeBlockFragment.getmDataText().getText().toString()
                            .substring(0, 8));
        } else {
            // Has to be called after mDumpWithPos is properly initialized.
            // (After checkDumpAndShowSectorChooserDialog().)
            HashMap<Integer, byte[]> sector0 = mDumpWithPos.get(0);
            if (sector0 == null) {
                // Error. There is no sector 0 in this dump. Checking the BCC
                // is therefore irrelevant.
                return 4;
            }
            byte[] block0 = sector0.get(0);
            if (block0 == null) {
                // Error. There is no block 0 in sector 0. Checking the BCC is
                // therefore irrelevant.
                return 5;
            }
            bcc = block0[4];
            uid = new byte[uidLen];
            System.arraycopy(block0, 0, uid, 0, uidLen);
        }
        boolean isValidBcc;
        try {
            isValidBcc = Common.isValidBCC(uid, bcc);
        } catch (IllegalArgumentException e) {
            // This should never happen, because we already know that the
            // length of the UID is 4 byte.
            return 3;
        }
        if (!isValidBcc) {
            // Error. BCC is not valid. Show error message.
            Toast.makeText(this, R.string.info_bcc_not_valid,
                    Toast.LENGTH_LONG).show();
            return 1;
        }
        // Everything was O.K.
        return 0;
    }

    /**
     * Check if the chosen sector or last sector of a dump is in the
     * range of valid sectors (according to {@link Preferences}).
     * @param context The context in error messages are displayed.
     * @return True if the sector is in range, False if not. Also,
     * if there was no tag False will be returned.
     */
    private boolean isSectorInRage(Context context, boolean isWriteBlock) {
        MCReader reader = Common.checkForTagAndCreateReader(this);
        if (reader == null) {
            return false;
        }
        int lastValidSector = reader.getSectorCount() - 1;
        int lastSector;
        reader.close();
        // Initialize last sector.
        if (isWriteBlock) {
            WriteBlockFragment writeBlockFragment = (WriteBlockFragment)getSupportFragmentManager().findFragmentByTag(getCurrentFragmentTag());
            lastSector = Integer.parseInt(
                    writeBlockFragment.getmSectorTextBlock().getText().toString());
        } else {
            lastSector = Collections.max(mDumpWithPos.keySet());
        }

        // Is last sector in range?
        if (lastSector > lastValidSector) {
            // Error. Tag too small for dump.
            Toast.makeText(context, R.string.info_tag_too_small,
                    Toast.LENGTH_LONG).show();
            reader.close();
            return false;
        }
        return true;
    }

    /**
     * Helper function for {@link #onWriteBlock()} and
     * {@link #onWriteValue()} to show
     * the {@link KeyMapCreator}.
     * @param sector The sector for the mapping range of
     * {@link KeyMapCreator}
     * @param isValueBlock If true, the key map will be created for a Value
     * Block ({@link #writeValueBlock()}).
     * @see KeyMapCreator
     * @see #onWriteBlock()
     * @see #onWriteValue()
     */
    private void createKeyMapForBlock(int sector, boolean isValueBlock) {
        Intent intent = new Intent(this, KeyMapCreator.class);
        intent.putExtra(KeyMapCreator.EXTRA_KEYS_DIR,
                Common.getFileFromStorage(Common.HOME_DIR + "/" +
                        Common.KEYS_DIR).getAbsolutePath());
        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER, false);
        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER_FROM, sector);
        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER_TO, sector);
        if (isValueBlock) {
            intent.putExtra(KeyMapCreator.EXTRA_BUTTON_TEXT, getString(
                    R.string.action_create_key_map_and_write_value_block));
            startActivityForResult(intent, CKM_WRITE_NEW_VALUE);
        } else {
            intent.putExtra(KeyMapCreator.EXTRA_BUTTON_TEXT, getString(
                    R.string.action_create_key_map_and_write_block));
            startActivityForResult(intent, CKM_WRITE_BLOCK);
        }
    }


    /**
     * Display information about writing to the manufacturer block and
     * optionally create a key map for the first sector.
     * @param createKeyMap If true {@link #createKeyMapForBlock(int, boolean)}
     * will be triggered the time the user confirms the dialog.
     */
    private void showWriteManufInfo(final boolean createKeyMap) {
        // Warning. Writing to the manufacturer block is not normal.
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.dialog_block0_writing_title);
        dialog.setMessage(R.string.dialog_block0_writing);
        dialog.setIcon(android.R.drawable.ic_dialog_info);

        int buttonID = R.string.action_ok;
        if (createKeyMap) {
            buttonID = R.string.action_i_know_what_i_am_doing;
            dialog.setNegativeButton(R.string.action_cancel,
                    (dialog12, which) -> {
                        // Do nothing.
                    });
        }
        dialog.setPositiveButton(buttonID,
                (dialog1, which) -> {
                    // Do nothing or create a key map.
                    if (createKeyMap) {
                        createKeyMapForBlock(0, false);
                    }
                });
        dialog.show();
    }



    /**
     * Handle incoming results from {@link KeyMapCreator} or
     * {@link FileChooser}.
     * @see #writeBlock()
     * @see #checkTag()
     * @see #checkDumpAndShowSectorChooserDialog(String[])
     * @see #createFactoryFormattedDump()
     * @see #writeValueBlock()
     */
    @Override
    public void onActivityResult(int requestCode,
                                 int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        int ckmError = -1;

        switch(requestCode) {
            case FC_WRITE_DUMP:
                if (resultCode == Activity.RESULT_OK) {
                    // Read dump and create keys.
                    readDumpFromFile(data.getStringExtra(
                            FileChooser.EXTRA_CHOSEN_FILE));
                }
                break;
            case CKM_WRITE_DUMP:
                if (resultCode != Activity.RESULT_OK) {
                    // Error.
                    ckmError = resultCode;
                } else {
                    checkTag();
                }
                break;
            case CKM_FACTORY_FORMAT:
                if (resultCode != Activity.RESULT_OK) {
                    // Error.
                    ckmError = resultCode;
                } else {
                    createFactoryFormattedDump();
                }
                break;
            case CKM_WRITE_BLOCK:
                if (resultCode != Activity.RESULT_OK) {
                    // Error.
                    ckmError = resultCode;
                } else {
                    // Write block.
                    writeBlock();
                }
                break;
            case CKM_WRITE_NEW_VALUE:
                if (resultCode != Activity.RESULT_OK) {
                    // Error.
                    ckmError = resultCode;
                } else {
                    // Write block.
                    writeValueBlock();
                }
                break;

        }

        // Error handling for the return value of KeyMapCreator.
        // So far, only error nr. 4 needs to be handled.
        switch (ckmError) {
            case 4:
                // Error. Path from the calling intend was null.
                // (This is really strange and should not occur.)
                Toast.makeText(this, R.string.info_strange_error,
                        Toast.LENGTH_LONG).show();
        }
    }


    /**
     * Called from {@link #onActivityResult(int, int, Intent)}
     * after a key map was created, this method tries to write the given
     * data to the tag. Possible errors are displayed to the user via Toast.
     * @see #onActivityResult(int, int, Intent)
     * @see #onWriteBlock()
     */
    private void writeBlock() {
        MCReader reader = Common.checkForTagAndCreateReader(this);
        if (reader == null) {
            return;
        }
        WriteBlockFragment writeBlockFragment = (WriteBlockFragment)getSupportFragmentManager().findFragmentByTag(getCurrentFragmentTag());
        int sector = Integer.parseInt(writeBlockFragment.getmSectorTextBlock().getText().toString());
        int block = Integer.parseInt(writeBlockFragment.getmBlockTextBlock().getText().toString());
        byte[][] keys = Common.getKeyMap().get(sector);
        int result = -1;

        if (keys[1] != null) {
            result = reader.writeBlock(sector, block,
                    Common.hexStringToByteArray(writeBlockFragment.getmDataText().getText().toString()),
                    keys[1], true);
        }
        // Error while writing? Maybe tag has default factory settings ->
        // try to write with key a (if there is one).
        if (result == -1 && keys[0] != null) {
            result = reader.writeBlock(sector, block,
                    Common.hexStringToByteArray(writeBlockFragment.getmDataText().getText().toString()),
                    keys[0], false);
        }
        reader.close();

        // Error handling.
        switch (result) {
            case 2:
                Toast.makeText(this, R.string.info_block_not_in_sector,
                        Toast.LENGTH_LONG).show();
                return;
            case -1:
                Toast.makeText(this, R.string.info_error_writing_block,
                        Toast.LENGTH_LONG).show();
                return;
        }
        Toast.makeText(this, R.string.info_write_successful,
                Toast.LENGTH_LONG).show();
        finish();
    }

    /**
     * Read the dump (skipping all blocks with unknown data "-") and
     * call {@link #checkDumpAndShowSectorChooserDialog(String[])}.
     * @param pathToDump path and filename of the dump
     * (selected by {@link FileChooser}).
     * @see #checkDumpAndShowSectorChooserDialog(String[])
     */
    private void readDumpFromFile(String pathToDump) {
        // Read dump.
        File file = new File(pathToDump);
        String[] dump = Common.readFileLineByLine(file, false, this);
        checkDumpAndShowSectorChooserDialog(dump);
    }

    /**
     * Triggered after the dump was selected (by {@link FileChooser})
     * and read (by {@link #readDumpFromFile(String)}), this method saves
     * the data including its position in {@link #mDumpWithPos}.
     * If the "use static Access Condition" option is enabled, all the ACs
     * will be replaced by the static ones. Also, the BCC value is
     * check if necessary ({@link #checkBCC(boolean)}). After all this it
     * will show a dialog in which the user can choose the sectors he wants
     * to write. When the sectors are chosen, this method calls
     * {@link #createKeyMapForDump()} to create a key map for the present tag.
     * @param dump Dump selected by {@link FileChooser} or directly
     * from the {@link DumpEditor} (via an Intent with{@link #EXTRA_DUMP})).
     * @see KeyMapCreator
     * @see #createKeyMapForDump()
     * @see #checkBCC(boolean)
     */
    private void checkDumpAndShowSectorChooserDialog(final String[] dump) {
        int err = Common.isValidDump(dump, false);
        if (err != 0) {
            // Error.
            Common.isValidDumpErrorToast(err, this);
            return;
        }

        initDumpWithPosFromDump(dump);

        // Create and show sector chooser dialog
        // (let the user select the sectors which will be written).
        View dialogLayout = getLayoutInflater().inflate(
                R.layout.dialog_write_sectors,
                findViewById(android.R.id.content), false);
        LinearLayout llCheckBoxes = dialogLayout.findViewById(
                R.id.linearLayoutWriteSectorsCheckBoxes);
        Button selectAll = dialogLayout.findViewById(
                R.id.buttonWriteSectorsSelectAll);
        Button selectNone = dialogLayout.findViewById(
                R.id.buttonWriteSectorsSelectNone);
        Integer[] sectors = mDumpWithPos.keySet().toArray(
                new Integer[mDumpWithPos.size()]);
        Arrays.sort(sectors);
        final Context context = this;
        final CheckBox[] sectorBoxes = new CheckBox[mDumpWithPos.size()];
        for (int i = 0; i< sectors.length; i++) {
            sectorBoxes[i] = new CheckBox(this);
            sectorBoxes[i].setChecked(true);
            sectorBoxes[i].setTag(sectors[i]);
            sectorBoxes[i].setText(getString(R.string.text_sector)
                    + " " + sectors[i]);
            llCheckBoxes.addView(sectorBoxes[i]);
        }
        View.OnClickListener listener = v -> {
            String tag = v.getTag().toString();
            for (CheckBox box : sectorBoxes) {
                box.setChecked(tag.equals("all"));
            }
        };
        selectAll.setOnClickListener(listener);
        selectNone.setOnClickListener(listener);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_write_sectors_title)
                .setIcon(android.R.drawable.ic_menu_edit)
                .setView(dialogLayout)
                .setPositiveButton(R.string.action_ok,
                        (dialog12, which) -> {
                            // Do nothing here because we override this button later
                            // to change the close behaviour. However, we still need
                            // this because on older versions of Android unless we
                            // pass a handler the button doesn't get instantiated
                        })
                .setNegativeButton(R.string.action_cancel,
                        (dialog1, which) -> {
                            // Do nothing.
                        })
                .create();
        dialog.show();
        final Context con = this;

        // Override/define behavior for positive button click.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
                v -> {
                    // Re-Init mDumpWithPos in order to remove unwanted sectors.
                    initDumpWithPosFromDump(dump);
                    boolean writeBlock0 = false;
                    Fragment fragment = getSupportFragmentManager().findFragmentByTag(getCurrentFragmentTag());
                    CheckBox mWriteManufBlock = null;
                    if (fragment instanceof WriteDumpFragment){
                        mWriteManufBlock = ((WriteDumpFragment) fragment).getCheckBoxWriteTagDumpWriteManuf();
                    }
                    for (CheckBox box : sectorBoxes) {
                        int sector = Integer.parseInt(box.getTag().toString());
                        if (!box.isChecked()) {
                            mDumpWithPos.remove(sector);
                        } else if (sector == 0 && box.isChecked()
                                && mWriteManufBlock != null
                                && mWriteManufBlock.isChecked()) {
                            writeBlock0 = true;
                        }
                    }
                    if (mDumpWithPos.size() == 0) {
                        // Error. There is nothing to write.
                        Toast.makeText(context, R.string.info_nothing_to_write,
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Check if last sector is out of range.
                    if (!isSectorInRage(con, false)) {
                        return;
                    }

                    // Do a BCC check if sector 0 is chosen and writing to
                    // the manufacturer block was enabled.
                    if (writeBlock0) {
                        int bccCheck = checkBCC(false);
                        if (bccCheck == 2) {
                            // Error. Redo.
                            return;
                        } else if (bccCheck == 1) {
                            // Error in BCC. Exit.
                            dialog.dismiss();
                            return;
                        }
                    }
                    // Create key map.
                    createKeyMapForDump();
                    dialog.dismiss();
                });
    }

    /**
     * Regular behavior: Check input, open a file chooser ({@link FileChooser})
     * to select a dump and wait for its result in
     * {@link #onActivityResult(int, int, Intent)}.
     * This method triggers the call chain: open {@link FileChooser}
     * (this method) -> read dump ({@link #readDumpFromFile(String)})
     * -> check dump ({@link #checkDumpAndShowSectorChooserDialog(String[])}) ->
     * open {@link KeyMapCreator} ({@link #createKeyMapForDump()})
     * -> run {@link #checkTag()} -> run
     * {@link #writeDump(HashMap, SparseArray)}.<br />
     * Behavior if the dump is already there (from the {@link DumpEditor}):
     * The same as before except the call chain will directly start from
     * {@link #checkDumpAndShowSectorChooserDialog(String[])}.<br />
     * (The static Access Conditions will be checked in any case, if the
     * option is enabled.)
     * @see FileChooser
     * @see #onActivityResult(int, int, Intent)
     */
    public void onWriteDump() {
        // Check the static Access Condition option.
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(getCurrentFragmentTag());
        if (fragment instanceof WriteDumpFragment){
            if (((WriteDumpFragment) fragment).getCheckBoxWriteTagDumpStaticAC().isChecked()) {
                String ac = ((WriteDumpFragment) fragment).getEditTextWriteTagDumpStaticAC().getText().toString();
                if (!ac.matches("[0-9A-Fa-f]+")) {
                    // Error, not hex.
                    Toast.makeText(this, R.string.info_ac_not_hex,
                            Toast.LENGTH_LONG).show();
                    return;
                }
                if (ac.length() != 6) {
                    // Error, not 3 byte (6 chars).
                    Toast.makeText(this, R.string.info_ac_not_3_byte,
                            Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }

        if (mWriteDumpFromEditor) {
            // Write dump directly from the dump editor.
            // (Dump has already been chosen.)
            checkDumpAndShowSectorChooserDialog(mDumpFromEditor);
        } else {
            // Show file chooser (chose dump).
            Intent intent = new Intent(this, FileChooser.class);
            intent.putExtra(FileChooser.EXTRA_DIR,
                    Common.getFileFromStorage(Common.HOME_DIR + "/" +
                            Common.DUMPS_DIR).getAbsolutePath());
            intent.putExtra(FileChooser.EXTRA_TITLE,
                    getString(R.string.text_open_dump_title));
            intent.putExtra(FileChooser.EXTRA_CHOOSER_TEXT,
                    getString(R.string.text_choose_dump_to_write));
            intent.putExtra(FileChooser.EXTRA_BUTTON_TEXT,
                    getString(R.string.action_write_full_dump));
            startActivityForResult(intent, FC_WRITE_DUMP);
        }
    }

    /**
     * Initialize {@link #mDumpWithPos} with the data from a dump.
     * Transform the simple dump array into a structure (mDumpWithPos)
     * where the sector and block information are known additionally.
     * Blocks containing unknown data ("-") are dropped.
     * @param dump The dump to initialize the mDumpWithPos with.
     */
    private void initDumpWithPosFromDump(String[] dump) {
        mDumpWithPos = new HashMap<>();
        int sector = 0;
        int block = 0;
        // Transform the simple dump array into a structure (mDumpWithPos)
        // where the sector and block information are known additionally.
        // Blocks containing unknown data ("-") are dropped.
        for (int i = 0; i < dump.length; i++) {
            if (dump[i].startsWith("+")) {
                String[] tmp = dump[i].split(": ");
                sector = Integer.parseInt(tmp[tmp.length-1]);
                block = 0;
                mDumpWithPos.put(sector, new HashMap<>());
            } else if (!dump[i].contains("-")) {
                // Use static Access Conditions for all sectors?
                CheckBox mEnableStaticAC = null;
                EditText mStaticAC = null;
                Fragment fragment = getSupportFragmentManager().findFragmentByTag(getCurrentFragmentTag());
                if (fragment instanceof WriteDumpFragment){
                    mEnableStaticAC = ((WriteDumpFragment) fragment).getCheckBoxWriteTagDumpStaticAC();
                    mStaticAC = ((WriteDumpFragment) fragment).getEditTextWriteTagDumpStaticAC();
                }
                if (mEnableStaticAC != null && mEnableStaticAC.isChecked()
                        && (i+1 == dump.length || dump[i+1].startsWith("+"))
                        && mStaticAC != null) {
                    // This is a Sector Trailer. Replace its ACs
                    // with the static ones.
                    String newBlock = dump[i].substring(0, 12)
                            + mStaticAC.getText().toString()
                            + dump[i].substring(18, dump[i].length());
                    dump[i] = newBlock;
                }
                mDumpWithPos.get(sector).put(block++,
                        Common.hexStringToByteArray(dump[i]));
            } else {
                block++;
            }
        }
    }

    /**
     * Check if the tag is suitable for the dump ({@link #mDumpWithPos}).
     * This is done in three steps. The first check determines if the dump
     * fits on the tag (size check). The second check determines if the keys for
     * relevant sectors are known (key check). At last this method will check
     * whether the keys with write privileges are known and if some blocks
     * are read-only (write check).<br />
     * If some of these checks "fail", the user will get a report dialog
     * with the two options to cancel the whole write process or to
     * write as much as possible(call {@link #writeDump(HashMap,
     * SparseArray)}).
     * @see MCReader#isWritableOnPositions(HashMap, SparseArray)
     * @see Common#getOperationInfoForBlock(byte, byte,
     * byte, de.syss.MifareClassicTool.Common.Operations, boolean, boolean)
     * @see #writeDump(HashMap, SparseArray)
     */
    private void checkTag() {
        // Create reader.
        MCReader reader = Common.checkForTagAndCreateReader(this);
        if (reader == null) {
            return;
        }

        // Check if tag is correct size for dump.
        if (reader.getSectorCount()-1 < Collections.max(
                mDumpWithPos.keySet())) {
            // Error. Tag too small for dump.
            Toast.makeText(this, R.string.info_tag_too_small,
                    Toast.LENGTH_LONG).show();
            reader.close();
            return;
        }

        // Check if tag is writable on needed blocks.
        // Reformat for reader.isWritableOnPosition(...).
        final SparseArray<byte[][]> keyMap  =
                Common.getKeyMap();
        HashMap<Integer, int[]> dataPos =
                new HashMap<>(mDumpWithPos.size());
        for (int sector : mDumpWithPos.keySet()) {
            int i = 0;
            int[] blocks = new int[mDumpWithPos.get(sector).size()];
            for (int block : mDumpWithPos.get(sector).keySet()) {
                blocks[i++] = block;
            }
            dataPos.put(sector, blocks);
        }
        HashMap<Integer, HashMap<Integer, Integer>> writeOnPos =
                reader.isWritableOnPositions(dataPos, keyMap);
        reader.close();

        if (writeOnPos == null) {
            // Error while checking for keys with write privileges.
            Toast.makeText(this, R.string.info_check_ac_error,
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Skip dialog:
        // Build a dialog showing all sectors and blocks containing data
        // that can not be overwritten with the reason why they are not
        // writable. The user can chose to skip all these blocks/sectors
        // or to cancel the whole write procedure.
        List<HashMap<String, String>> list = new
                ArrayList<>();
        final HashMap<Integer, HashMap<Integer, Integer>> writeOnPosSafe =
                new HashMap<>(
                        mDumpWithPos.size());
        // Keys that are missing completely (mDumpWithPos vs. keyMap).
        HashSet<Integer> sectors = new HashSet<>();
        for (int sector : mDumpWithPos.keySet()) {
            if (keyMap.indexOfKey(sector) < 0) {
                // Problem. Keys for sector not found.
                addToList(list, getString(R.string.text_sector) + ": " + sector,
                        getString(R.string.text_keys_not_known));
            } else {
                sectors.add(sector);
            }
        }
        // Keys with write privileges that are missing or some
        // blocks (block-parts) are read-only (writeOnPos vs. keyMap).
        for (int sector : sectors) {
            if (writeOnPos.get(sector) == null) {
                // Error. Sector is dead (IO Error) or ACs are invalid.
                addToList(list, getString(R.string.text_sector) + ": " + sector,
                        getString(R.string.text_invalid_ac_or_sector_dead));
                continue;
            }
            byte[][] keys = keyMap.get(sector);
            Set<Integer> blocks = mDumpWithPos.get(sector).keySet();
            for (int block : blocks) {
                boolean isSafeForWriting = true;
                Fragment fragment = getSupportFragmentManager().findFragmentByTag(getCurrentFragmentTag());
                if (fragment instanceof WriteDumpFragment){
                    if (!((WriteDumpFragment) fragment).getCheckBoxWriteTagDumpWriteManuf().isChecked()
                            && sector == 0 && block == 0) {
                        // Block 0 is read-only. This is normal.
                        // Do not add an entry to the dialog and skip the
                        // "write info" check (except for some
                        // special (non-original) MIFARE tags).
                        continue;
                    }
                }
                String position = getString(R.string.text_sector) + ": "
                        + sector + ", " + getString(R.string.text_block)
                        + ": " + block;
                int writeInfo = writeOnPos.get(sector).get(block);
                switch (writeInfo) {
                    case 0:
                        // Problem. Block is read-only.
                        addToList(list, position, getString(
                                R.string.text_block_read_only));
                        isSafeForWriting = false;
                        break;
                    case 1:
                        if (keys[0] == null) {
                            // Problem. Key with write privileges (A) not known.
                            addToList(list, position, getString(
                                    R.string.text_write_key_a_not_known));
                            isSafeForWriting = false;
                        }
                        break;
                    case 2:
                        if (keys[1] == null) {
                            // Problem. Key with write privileges (B) not known.
                            addToList(list, position, getString(
                                    R.string.text_write_key_b_not_known));
                            isSafeForWriting = false;
                        }
                        break;
                    case 3:
                        // No Problem. Both keys have write privileges.
                        // Set to key A or B depending on which one is available.
                        writeInfo = (keys[0] != null) ? 1 : 2;
                        break;
                    case 4:
                        if (keys[0] == null) {
                            // Problem. Key with write privileges (A) not known.
                            addToList(list, position, getString(
                                    R.string.text_write_key_a_not_known));
                            isSafeForWriting = false;
                        } else {
                            // Problem. ACs are read-only.
                            addToList(list, position, getString(
                                    R.string.text_ac_read_only));
                        }
                        break;
                    case 5:
                        if (keys[1] == null) {
                            // Problem. Key with write privileges (B) not known.
                            addToList(list, position, getString(
                                    R.string.text_write_key_b_not_known));
                            isSafeForWriting = false;
                        } else {
                            // Problem. ACs are read-only.
                            addToList(list, position, getString(
                                    R.string.text_ac_read_only));
                        }
                        break;
                    case 6:
                        if (keys[1] == null) {
                            // Problem. Key with write privileges (B) not known.
                            addToList(list, position, getString(
                                    R.string.text_write_key_b_not_known));
                            isSafeForWriting = false;
                        } else {
                            // Problem. Keys are read-only.
                            addToList(list, position, getString(
                                    R.string.text_keys_read_only));
                        }
                        break;
                    case -1:
                        // Error. Some strange error occurred. Maybe due to some
                        // corrupted ACs...
                        addToList(list, position, getString(
                                R.string.text_strange_error));
                        isSafeForWriting = false;
                }
                // Add if safe for writing.
                if (isSafeForWriting) {
                    if (writeOnPosSafe.get(sector) == null) {
                        // Create sector.
                        HashMap<Integer, Integer> blockInfo =
                                new HashMap<>();
                        blockInfo.put(block, writeInfo);
                        writeOnPosSafe.put(sector, blockInfo);
                    } else {
                        // Add to sector.
                        writeOnPosSafe.get(sector).put(block, writeInfo);
                    }
                }
            }
        }

        // Show skip/cancel dialog (if needed).
        if (list.size() != 0) {
            // If the user skips all sectors/blocks that are not writable,
            // the writeTag() method will be called.
            LinearLayout ll = new LinearLayout(this);
            int pad = Common.dpToPx(5);
            ll.setPadding(pad, pad, pad, pad);
            ll.setOrientation(LinearLayout.VERTICAL);
            TextView textView = new TextView(this);
            textView.setText(R.string.dialog_not_writable);
            textView.setTextAppearance(this,
                    android.R.style.TextAppearance_Medium);
            ListView listView = new ListView(this);
            ll.addView(textView);
            ll.addView(listView);
            String[] from = new String[] {"position", "reason"};
            int[] to = new int[] {android.R.id.text1, android.R.id.text2};
            ListAdapter adapter = new SimpleAdapter(this, list,
                    android.R.layout.two_line_list_item, from, to);
            listView.setAdapter(adapter);

            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_not_writable_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setView(ll)
                    .setPositiveButton(R.string.action_skip_blocks,
                            (dialog, which) -> {
                                // Skip not writable blocks and start writing.
                                writeDump(writeOnPosSafe, keyMap);
                            })
                    .setNegativeButton(R.string.action_cancel_all,
                            (dialog, which) -> {
                                // Do nothing.
                            })
                    .show();
        } else {
            // Write.
            writeDump(writeOnPosSafe, keyMap);
        }
    }

    /**
     * Create an factory formatted, empty dump with a size matching
     * the current tag size and then call {@link #checkTag()}.
     * Factory (default) MIFARE Classic Access Conditions are: 0xFF0780XX
     * XX = General purpose byte (GPB): Most of the time 0x69. At the end of
     * an Tag XX = 0xBC.
     * @see #checkTag()
     */
    private void createFactoryFormattedDump() {
        // This function is directly called after a key map was created.
        // So Common.getTag() will return den current present tag
        // (and its size/sector count).
        mDumpWithPos = new HashMap<>();
        int sectors = MifareClassic.get(Common.getTag()).getSectorCount();
        byte[] emptyBlock = new byte[]
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        byte[] normalSectorTrailer = new byte[] {-1, -1, -1, -1, -1, -1,
                -1, 7, -128, 105, -1, -1, -1, -1, -1, -1};
        byte[] lastSectorTrailer = new byte[] {-1, -1, -1, -1, -1, -1,
                -1, 7, -128, -68, -1, -1, -1, -1, -1, -1};
        // Empty 4 block sector.
        HashMap<Integer, byte[]> empty4BlockSector =
                new HashMap<>(4);
        for (int i = 0; i < 3; i++) {
            empty4BlockSector.put(i, emptyBlock);
        }
        empty4BlockSector.put(3, normalSectorTrailer);
        // Empty 16 block sector.
        HashMap<Integer, byte[]> empty16BlockSector =
                new HashMap<>(16);
        for (int i = 0; i < 15; i++) {
            empty16BlockSector.put(i, emptyBlock);
        }
        empty16BlockSector.put(15, normalSectorTrailer);
        // Last sector.
        HashMap<Integer, byte[]> lastSector;

        // Sector 0.
        HashMap<Integer, byte[]> firstSector =
                new HashMap<>(4);
        firstSector.put(1, emptyBlock);
        firstSector.put(2, emptyBlock);
        firstSector.put(3, normalSectorTrailer);
        mDumpWithPos.put(0, firstSector);
        // Sector 1 - (max.) 31.
        for (int i = 1; i < sectors && i < 32; i++) {
            mDumpWithPos.put(i, empty4BlockSector);
        }
        // Sector 32 - 39.
        if (sectors == 40) {
            // Add the large sectors (containing 16 blocks)
            // of a MIFARE Classic 4k tag.
            for (int i = 32; i < sectors && i < 39; i++) {
                mDumpWithPos.put(i, empty16BlockSector);
            }
            // In the last sector the Sector Trailer is different.
            lastSector = new HashMap<>(empty16BlockSector);
            lastSector.put(15, lastSectorTrailer);
        } else {
            // In the last sector the Sector Trailer is different.
            lastSector = new HashMap<>(empty4BlockSector);
            lastSector.put(3, lastSectorTrailer);
        }
        mDumpWithPos.put(sectors - 1, lastSector);
        checkTag();
    }

    /**
     * A helper function for {@link #checkTag()} adding an item to
     * the list of all blocks with write issues.
     * This list will be displayed to the user in a dialog before writing.
     * @param list The list in which to add the key-value-pair.
     * @param position The key (position) for the list item
     * (e.g. "Sector 2, Block 3").
     * @param reason The value (reason) for the list item
     * (e.g. "Block is read-only").
     */
    private void addToList(List<HashMap<String, String>> list,
                           String position, String reason) {
        HashMap<String, String> item = new HashMap<>();
        item.put( "position", position);
        item.put( "reason", reason);
        list.add(item);
    }

    /**
     * This method is triggered by {@link #checkTag()} and writes a dump
     * to a tag.
     * @param writeOnPos A map within a map (all with type = Integer).
     * The key of the outer map is the sector number and the value is another
     * map with key = block number and value = write information. The write
     * information must be filtered (by {@link #checkTag()}) return values
     * of {@link MCReader#isWritableOnPositions(HashMap, SparseArray)}.<br />
     * Attention: This method does not any checking. The position and write
     * information must be checked by {@link #checkTag()}.
     * @param keyMap A key map generated by {@link KeyMapCreator}.
     */
    private void writeDump(
            final HashMap<Integer, HashMap<Integer, Integer>> writeOnPos,
            final SparseArray<byte[][]> keyMap) {
        // Check for write data.
        if (writeOnPos.size() == 0) {
            // Nothing to write. Exit.
            Toast.makeText(this, R.string.info_nothing_to_write,
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Create reader.
        final MCReader reader = Common.checkForTagAndCreateReader(this);
        if (reader == null) {
            return;
        }

        // Display don't remove warning.
        LinearLayout ll = new LinearLayout(this);
        int pad = Common.dpToPx(10);
        ll.setPadding(pad, pad, pad, pad);
        ll.setGravity(Gravity.CENTER);
        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        pad = Common.dpToPx(5);
        progressBar.setPadding(0, 0, pad, 0);
        TextView tv = new TextView(this);
        tv.setText(getString(R.string.dialog_wait_write_tag));
        tv.setTextSize(18);
        ll.addView(progressBar);
        ll.addView(tv);
        final AlertDialog warning = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_wait_write_tag_title)
                .setView(ll)
                .create();
        warning.show();


        // Start writing in new thread.
        final Activity a = this;
        final Handler handler = new Handler();
        new Thread(() -> {
            // Write dump to tag.
            for (int sector : writeOnPos.keySet()) {
                byte[][] keys = keyMap.get(sector);
                for (int block : writeOnPos.get(sector).keySet()) {
                    // Select key with write privileges.
                    byte writeKey[] = null;
                    boolean useAsKeyB = true;
                    int wi = writeOnPos.get(sector).get(block);
                    if (wi == 1 || wi == 4) {
                        writeKey = keys[0]; // Write with key A.
                        useAsKeyB = false;
                    } else if (wi == 2 || wi == 5 || wi == 6) {
                        writeKey = keys[1]; // Write with key B.
                    }

                    // Write block.
                    int result = reader.writeBlock(sector, block,
                            mDumpWithPos.get(sector).get(block),
                            writeKey, useAsKeyB);

                    if (result != 0) {
                        // Error. Some error while writing.
                        handler.post(() -> Toast.makeText(a,
                                R.string.info_write_error,
                                Toast.LENGTH_LONG).show());
                        reader.close();
                        warning.cancel();
                        return;
                    }
                }
            }
            // Finished writing.
            reader.close();
            warning.cancel();
            handler.post(() -> Toast.makeText(a, R.string.info_write_successful,
                    Toast.LENGTH_LONG).show());
            a.finish();
        }).start();
    }

    /**
     * Display information about using custom Access Conditions for all
     * sectors of the dump.
     */
    public void onShowStaticACInfo() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_static_ac_title)
                .setMessage(R.string.dialog_static_ac)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton(R.string.action_ok,
                        (dialog, which) -> {
                            // Do nothing.
                        }).show();
    }

    /**
     * Display information about writing to the manufacturer block.
     * @see #showWriteManufInfo(boolean)
     */
    public void onShowWriteManufInfo() {
        showWriteManufInfo(false);
    }

    /**
     * Create a key map for the dump ({@link #mDumpWithPos}).
     * @see KeyMapCreator
     */
    private void createKeyMapForDump() {
        // Show key map creator.
        Intent intent = new Intent(this, KeyMapCreator.class);
        intent.putExtra(KeyMapCreator.EXTRA_KEYS_DIR,
                Common.getFileFromStorage(Common.HOME_DIR + "/" +
                        Common.KEYS_DIR).getAbsolutePath());
        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER, false);
        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER_FROM,
                (int) Collections.min(mDumpWithPos.keySet()));
        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER_TO,
                (int) Collections.max(mDumpWithPos.keySet()));
        intent.putExtra(KeyMapCreator.EXTRA_BUTTON_TEXT,
                getString(R.string.action_create_key_map_and_write_dump));
        startActivityForResult(intent, CKM_WRITE_DUMP);
    }

    /**
     * Check the user input and (if correct) show the
     * {@link KeyMapCreator} with predefined mapping range
     * (see {@link #createKeyMapForBlock(int, boolean)}).
     * After a key map was created {@link #writeValueBlock()} will be triggered.
     * @see KeyMapCreator
     * @see #checkSectorAndBlock(android.widget.EditText,
     * android.widget.EditText)
     */
    public void onWriteValue() {
        // Check input.
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(getCurrentFragmentTag());
        EditText mSectorTextVB = null;
        EditText mBlockTextVB = null;
        EditText mNewValueTextVB = null;
        if (fragment instanceof ValueBlockFragment){
            mSectorTextVB = ((ValueBlockFragment) fragment).getEditTextWriteTagValueBlockSector();
            mBlockTextVB = ((ValueBlockFragment) fragment).getEditTextWriteTagValueBlockBlock();
            mNewValueTextVB = ((ValueBlockFragment) fragment).getEditTextWriteTagValueBlockValue();
        }
        if (mSectorTextVB == null ||
                mBlockTextVB == null ||
                    mNewValueTextVB == null ||
                !checkSectorAndBlock(mSectorTextVB, mBlockTextVB)) {
            return;
        }

        int sector = Integer.parseInt(mSectorTextVB.getText().toString());
        int block = Integer.parseInt(mBlockTextVB.getText().toString());
        if (block == 3 || block == 15 || (sector == 0 && block == 0)) {
            // Error. Block can't be a Value Block.
            Toast.makeText(this, R.string.info_not_vb,
                    Toast.LENGTH_LONG).show();
            return;
        }

        try {
            Integer.parseInt(mNewValueTextVB.getText().toString());
        } catch (Exception e) {
            // Error. Value is too big.
            Toast.makeText(this, R.string.info_value_too_big,
                    Toast.LENGTH_LONG).show();
            return;
        }

        createKeyMapForBlock(sector, true);
    }

    /**
     * Called from {@link #onActivityResult(int, int, Intent)}
     * after a key map was created, this method tries to increment or
     * decrement the Value Block. Possible errors are displayed to the
     * user via Toast.
     * @see #onActivityResult(int, int, Intent)
     * @see #onWriteValue()
     */
    private void writeValueBlock() {
        // Write the new value (incr./decr. + transfer).
        MCReader reader = Common.checkForTagAndCreateReader(this);
        if (reader == null) {
            return;
        }
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(getCurrentFragmentTag());
        EditText mSectorTextVB = null;
        EditText mBlockTextVB = null;
        EditText mNewValueTextVB = null;
        RadioButton mIncreaseVB = null;
        if (fragment instanceof ValueBlockFragment){
            mSectorTextVB = ((ValueBlockFragment) fragment).getEditTextWriteTagValueBlockSector();
            mBlockTextVB = ((ValueBlockFragment) fragment).getEditTextWriteTagValueBlockBlock();
            mNewValueTextVB = ((ValueBlockFragment) fragment).getEditTextWriteTagValueBlockValue();
            mIncreaseVB = ((ValueBlockFragment) fragment).getRadioButtonWriteTagWriteValueBlockIncr();
        }
        if (mNewValueTextVB != null && mSectorTextVB != null && mBlockTextVB != null && mIncreaseVB != null) {
            int value = Integer.parseInt(mNewValueTextVB.getText().toString());
            int sector = Integer.parseInt(mSectorTextVB.getText().toString());
            int block = Integer.parseInt(mBlockTextVB.getText().toString());

            byte[][] keys = Common.getKeyMap().get(sector);
            int result = -1;

            if (keys[1] != null) {
                result = reader.writeValueBlock(sector, block, value,
                        mIncreaseVB.isChecked(),
                        keys[1], true);
            }
            // Error while writing? Maybe tag has default factory settings ->
            // try to write with key a (if there is one).
            if (result == -1 && keys[0] != null) {
                result = reader.writeValueBlock(sector, block, value,
                        mIncreaseVB.isChecked(),
                        keys[0], false);
            }
            reader.close();

            // Error handling.
            switch (result) {
                case 2:
                    Toast.makeText(this, R.string.info_block_not_in_sector,
                            Toast.LENGTH_LONG).show();
                    return;
                case -1:
                    Toast.makeText(this, R.string.info_error_writing_value_block,
                            Toast.LENGTH_LONG).show();
                    return;
            }
            Toast.makeText(this, R.string.info_write_successful,
                    Toast.LENGTH_LONG).show();
            finish();

        } else {
            return;
        }
    }

    /**
     * Get currently displaying fragment's tag
     * @return
     */

    private String getCurrentFragmentTag() {
        return "android:switcher:" + R.id.viewpager + ":" + viewPager.getCurrentItem();
    }

}
