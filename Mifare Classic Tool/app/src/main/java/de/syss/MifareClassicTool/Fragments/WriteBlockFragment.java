package de.syss.MifareClassicTool.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import de.syss.MifareClassicTool.Activities.WriteTagFragmentActivity;

import androidx.fragment.app.Fragment;

import de.syss.MifareClassicTool.R;

public class WriteBlockFragment extends Fragment {

    /**
     * Constructor used by fragment manager activity
     * {@link WriteTagFragmentActivity}
     */

    public WriteBlockFragment() {}
    private OnWriteBlockListener callback;
    private Button buttonWriteTagBlock;
    private EditText mSectorTextBlock;
    private EditText mBlockTextBlock;
    private EditText mDataText;

    /**
     * Getters used by fragment manager activity
     *
     * @return
     */

    public EditText getmSectorTextBlock() {
        return mSectorTextBlock;
    }

    public EditText getmBlockTextBlock() {
        return mBlockTextBlock;
    }

    public EditText getmDataText() {
        return mDataText;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_write_block, container, false);

        buttonWriteTagBlock = view.findViewById(R.id.buttonWriteTagBlock);
        mSectorTextBlock = view.findViewById(R.id.editTextWriteTagSector);
        mBlockTextBlock = view.findViewById(R.id.editTextWriteTagBlock);
        mDataText = view.findViewById(R.id.editTextWriteTagData);

        buttonWriteTagBlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onWriteBlock();
            }
        });

        return view;
    }

    /**
     * Interface used to communicate with fragment manager activity
     * {@link WriteTagFragmentActivity}
     */

    public interface OnWriteBlockListener {
        void onWriteBlockButtonClick();
    }

    /**
     * Interface setter used by fragment manager activity
     * {@link WriteTagFragmentActivity}
     * @param callback WriteTagFragmentActivity class
     */

    public void setOnWriteBlockListener(OnWriteBlockListener callback) {
        this.callback = callback;
    }

    /**
     * Method triggered by buttonWriteTagBlock clickListener
     */
    public void onWriteBlock() {
        callback.onWriteBlockButtonClick();
    }


}
