package de.syss.MifareClassicTool.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;

import androidx.fragment.app.Fragment;

import de.syss.MifareClassicTool.Activities.WriteTagFragmentActivity;
import de.syss.MifareClassicTool.R;

public class ValueBlockFragment extends Fragment {

    /**
     * Constructor used by fragment manager activity
     * {@link WriteTagFragmentActivity}
     */

    public ValueBlockFragment() {}
    private OnWriteValueListener callback;
    private EditText editTextWriteTagValueBlockSector;
    private EditText editTextWriteTagValueBlockBlock;
    private EditText editTextWriteTagValueBlockValue;
    private RadioButton radioButtonWriteTagWriteValueBlockIncr;
    private Button buttonWriteTagValueBlock;

    /**
     * Getters used by fragment manager activity
     *
     * @return
     */

    public EditText getEditTextWriteTagValueBlockSector() {
        return editTextWriteTagValueBlockSector;
    }

    public EditText getEditTextWriteTagValueBlockBlock() {
        return editTextWriteTagValueBlockBlock;
    }

    public EditText getEditTextWriteTagValueBlockValue() {
        return editTextWriteTagValueBlockValue;
    }

    public RadioButton getRadioButtonWriteTagWriteValueBlockIncr() {
        return radioButtonWriteTagWriteValueBlockIncr;
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
        View view = inflater.inflate(R.layout.fragment_value_block, container, false);
        editTextWriteTagValueBlockSector = view.findViewById(R.id.editTextWriteTagValueBlockSector);
        editTextWriteTagValueBlockBlock = view.findViewById(R.id.editTextWriteTagValueBlockBlock);
        editTextWriteTagValueBlockValue = view.findViewById(R.id.editTextWriteTagValueBlockValue);
        radioButtonWriteTagWriteValueBlockIncr = view.findViewById(R.id.radioButtonWriteTagWriteValueBlockIncr);
        buttonWriteTagValueBlock = view.findViewById(R.id.buttonWriteTagValueBlock);
        buttonWriteTagValueBlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onWriteValue();
            }
        });
        return view;
    }

    /**
     * Interface used to communicate with fragment manager activity
     * {@link WriteTagFragmentActivity}
     */

    public interface OnWriteValueListener {
        void onWriteValueButtonClick();
    }

    /**
     * Interface setter used by fragment manager activity
     * {@link WriteTagFragmentActivity}
     * @param callback WriteTagFragmentActivity class
     */

    public void setOnWriteValueListener(OnWriteValueListener callback) {
        this.callback = callback;
    }

    /**
     * Method triggered by buttonWriteTagValueBlock clickListener
     */

    public void onWriteValue() {
        callback.onWriteValueButtonClick();
    }
}
