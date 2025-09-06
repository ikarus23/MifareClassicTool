package de.syss.MifareClassicTool.Activities;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import de.syss.MifareClassicTool.R;

/**
 * Activity that guides the user through copying a tag using {@link CopyController}.
 */
public class CopyActivity extends AppCompatActivity {

    private CopyController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_copy);
        controller = new CopyController(this);
        findViewById(R.id.copy_action).setOnClickListener(v -> controller.onActionButton());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            controller.onTagDiscovered(tag);
        }
    }
}
