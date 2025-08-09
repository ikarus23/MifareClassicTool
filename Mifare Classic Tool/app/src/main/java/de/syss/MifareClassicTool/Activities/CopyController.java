package de.syss.MifareClassicTool.Activities;

import android.app.Activity;
import android.nfc.Tag;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;

import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.MCReader;
import de.syss.MifareClassicTool.R;

/**
 * Helper class that implements the two step copy state machine.
 */
public class CopyController {

    public enum CopyState { WAIT_SOURCE, READY_TO_READ, WAIT_TARGET, READY_TO_WRITE, DONE }

    private CopyState state = CopyState.WAIT_SOURCE;
    private final Activity activity;
    private SparseArray<String[]> dump;
    private String dumpFile;

    public CopyController(Activity activity) {
        this.activity = activity;
    }

    /**
     * Called when an NFC tag is discovered.
     */
    public void onTagDiscovered(Tag tag) {
        switch (state) {
            case WAIT_SOURCE:
                MCReader reader = MCReader.get(tag);
                if (reader == null) {
                    return;
                }
                dump = reader.readAsMuchAsPossible(Common.getKeyMap());
                reader.close();
                dumpFile = saveDump(dump);
                String uid = Common.byte2HexString(tag.getId());
                updateUi(activity.getString(R.string.text_copy_start_source, uid), true);
                state = CopyState.READY_TO_READ;
                break;
            case WAIT_TARGET:
                updateUi(activity.getString(R.string.text_copy_target_detected), true);
                state = CopyState.READY_TO_WRITE;
                break;
            default:
                break;
        }
    }

    /**
     * Called when the action button is pressed.
     */
    public void onActionButton() {
        switch (state) {
            case READY_TO_READ:
                updateUi(activity.getString(R.string.text_copy_wait_target), false);
                state = CopyState.WAIT_TARGET;
                break;
            case READY_TO_WRITE:
                writeDumpToTag(dumpFile);
                updateUi(activity.getString(R.string.text_copy_done), false);
                state = CopyState.DONE;
                break;
            default:
                break;
        }
    }

    private void updateUi(String message, boolean showButton) {
        TextView msg = activity.findViewById(R.id.copy_message);
        View button = activity.findViewById(R.id.copy_action);
        msg.setText(message);
        button.setVisibility(showButton ? View.VISIBLE : View.GONE);
    }

    // Placeholder for dump saving.
    private String saveDump(SparseArray<String[]> raw) {
        // Real implementation would persist the dump and return the filename.
        return "tmp.mct";
    }

    // Placeholder for writing the dump to a tag.
    private void writeDumpToTag(String file) {
        // Real implementation would load the dump and write to target tag.
    }
}
