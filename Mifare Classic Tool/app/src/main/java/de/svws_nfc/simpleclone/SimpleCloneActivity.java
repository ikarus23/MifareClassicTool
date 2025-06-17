package de.svws_nfc.simpleclone;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import androidx.lifecycle.ViewModelProvider;

import de.syss.MifareClassicTool.Activities.BasicActivity;

/**
 * 단순 2-단계 카드 복제를 위한 전용 화면.
 * READ 단계 → WRITE 단계로만 흐르며, 나머지 세부 옵션은 자동 처리된다.
 */
public class SimpleCloneActivity extends BasicActivity {
    private CloneViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_clone);  // layout 은 다음 단계에서 생성
        viewModel = new ViewModelProvider(this).get(CloneViewModel.class);

        viewModel.getUiState().observe(this, state -> {
            // TODO: 단계별 메시지/버튼 상태 업데이트
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) viewModel.onTagScanned(tag);
    }
}
