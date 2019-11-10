package de.syss.MifareClassicTool.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;

import de.syss.MifareClassicTool.Activities.WriteTagFragmentActivity;
import de.syss.MifareClassicTool.R;

public class WriteDumpFragment extends Fragment {

    /**
     * Constructor used by fragment manager activity
     * {@link WriteTagFragmentActivity}
     */

    public WriteDumpFragment() {}
    private OnWriteDumpListener callback;
    private CheckBox checkBoxWriteTagDumpWriteManuf;
    private CheckBox checkBoxWriteTagDumpStaticAC;
    private CheckBox checkBoxWriteTagDumpOptions;
    private EditText editTextWriteTagDumpStaticAC;
    private Button buttonWriteTagDump;
    private ImageButton imageButtonWriteTagDumpStaticACInfo;
    private ImageButton imageButtonWriteTagDumpWriteManufInfo;

    /**
     * Getters used by fragment manager activity
     *
     * @return
     */

    public CheckBox getCheckBoxWriteTagDumpWriteManuf() {
        return checkBoxWriteTagDumpWriteManuf;
    }

    public CheckBox getCheckBoxWriteTagDumpStaticAC() {
        return checkBoxWriteTagDumpStaticAC;
    }

    public EditText getEditTextWriteTagDumpStaticAC() {
        return editTextWriteTagDumpStaticAC;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Lifecycle's method that inflates fragment layout
     * OnClickListeners should be set to layout views here
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_write_dump, container, false);
        LinearLayout ll = view.findViewById(R.id.linearLayoutWriteTagDumpOptions);
        checkBoxWriteTagDumpWriteManuf = view.findViewById(R.id.checkBoxWriteTagDumpWriteManuf);
        checkBoxWriteTagDumpStaticAC = view.findViewById(R.id.checkBoxWriteTagDumpStaticAC);
        checkBoxWriteTagDumpOptions = view.findViewById(R.id.checkBoxWriteTagDumpOptions);
        editTextWriteTagDumpStaticAC = view.findViewById(R.id.editTextWriteTagDumpStaticAC);
        buttonWriteTagDump = view.findViewById(R.id.buttonWriteTagDump);
        imageButtonWriteTagDumpStaticACInfo = view.findViewById(R.id.imageButtonWriteTagDumpStaticACInfo);
        imageButtonWriteTagDumpWriteManufInfo = view.findViewById(R.id.imageButtonWriteTagDumpWriteManufInfo);
        checkBoxWriteTagDumpOptions.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    ll.setVisibility(View.VISIBLE);
                } else {
                    ll.setVisibility(View.GONE);
                }
            }
        });
        buttonWriteTagDump.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onWriteDump();
            }
        });
        imageButtonWriteTagDumpStaticACInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onShowStaticACInfo();
            }
        });
        imageButtonWriteTagDumpWriteManufInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onShowWriteManufInfo();
            }
        });
        return view;
    }

    /**
     * Interface used to communicate with fragment manager activity
     * {@link WriteTagFragmentActivity}
     */

    public interface OnWriteDumpListener {
        void onWriteDumpButtonClick();
        void onShowStaticACInfoClick();
        void onShowWriteManufInfoClick();
    }

    /**
     * Interface setter used by fragment manager activity
     * {@link WriteTagFragmentActivity}
     * @param callback WriteTagFragmentActivity class
     */

    public void setOnWriteBlockListener(OnWriteDumpListener callback) {
        this.callback = callback;
    }

    /**
     * Method triggered by buttonWriteTagDump clickListener
     */
    public void onWriteDump() {
        callback.onWriteDumpButtonClick();
    }

    /**
     * Method triggered by imageButtonWriteTagDumpStaticACInfo clickListener
     */
    public void onShowStaticACInfo() {
        callback.onShowStaticACInfoClick();
    }

    /**
     * Method triggered by imageButtonWriteTagDumpStaticACInfo clickListener
     */
    public void onShowWriteManufInfo() {
        callback.onShowWriteManufInfoClick();
    }

}
