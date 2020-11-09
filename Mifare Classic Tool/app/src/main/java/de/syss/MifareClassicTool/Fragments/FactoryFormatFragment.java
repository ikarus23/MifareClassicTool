package de.syss.MifareClassicTool.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import de.syss.MifareClassicTool.Activities.WriteTagFragmentActivity;
import de.syss.MifareClassicTool.R;

public class FactoryFormatFragment extends Fragment {

    /**
     * Constructor used by fragment manager activity
     * {@link WriteTagFragmentActivity}
     */

    public FactoryFormatFragment() {}
    private OnFactoryFormatListener callback;
    private Button buttonWriteTagFactoryFormat;


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
        View view = inflater.inflate(R.layout.fragment_factory_format, container, false);
        buttonWriteTagFactoryFormat = view.findViewById(R.id.buttonWriteTagFactoryFormat);
        buttonWriteTagFactoryFormat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onFactoryFormat();
            }
        });
        return view;
    }

    /**
     * Interface used to communicate with fragment manager activity
     * {@link WriteTagFragmentActivity}
     */

    public interface OnFactoryFormatListener {
        void onFactoryFormatButtonClick();
    }

    /**
     * Interface setter used by fragment manager activity
     * {@link WriteTagFragmentActivity}
     * @param callback WriteTagFragmentActivity class
     */

    public void setOnFactoryFormatListener(OnFactoryFormatListener callback) {
        this.callback = callback;
    }

    /**
     * Method triggered by buttonWriteTagFactoryFormat clickListener
     */

    public void onFactoryFormat() {
        callback.onFactoryFormatButtonClick();
    }
}
