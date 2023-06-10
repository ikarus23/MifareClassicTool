import android.content.SharedPreferences;
import android.widget.Button;
import androidx.core.text.HtmlCompat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MainMenuTest {

    @Mock
    private NfcAdapter mockNfcAdapter;

    @Mock
    private SharedPreferences mockSharedPreferences;

    @Mock
    private SharedPreferences.Editor mockSharedPreferencesEditor;

    @Mock
    private Activity mockActivity;

    @Mock
    private TextView mockTextView;

    @Mock
    private Button mockButton;

    private MainMenu mainMenu;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mainMenu = spy(new MainMenu());
        when(mainMenu.getPreferences(anyInt())).thenReturn(mockSharedPreferences);
        when(mockSharedPreferences.edit()).thenReturn(mockSharedPreferencesEditor);
        when(mockSharedPreferences.getBoolean(anyString(), anyBoolean())).thenReturn(false);
        when(mockSharedPreferences.getInt(anyString(), anyInt())).thenReturn(0);
        when(mockActivity.findViewById(eq(R.id.textViewMainFooter))).thenReturn(mockTextView);
        when(mockActivity.findViewById(eq(R.id.buttonMainTools))).thenReturn(mockButton);
        mainMenu.setNfcAdapter(mockNfcAdapter);
    }

    @Test
    public void testOnCreate() {
        mainMenu.onCreate(null);
        verify(mockTextView).setText(anyString());
        verify(mainMenu).initFolders();
        verify(mainMenu).copyStdKeysFiles();
    }

    @Test
    public void testOnSaveInstanceState() {
        Bundle mockBundle = mock(Bundle.class);
        mainMenu.onSaveInstanceState(mockBundle);
        verify(mockBundle).putBoolean(eq("donate_dialog_was_shown"), anyBoolean());
        verify(mockBundle).putBoolean(eq("info_external_nfc_dialog_was_shown"), anyBoolean());
        verify(mockBundle).putBoolean(eq("has_no_nfc"), anyBoolean());
        verify(mockBundle).putParcelable(eq("old_intent"), any());
    }

    // Add more test methods to cover other functionality of MainMenu class

}
