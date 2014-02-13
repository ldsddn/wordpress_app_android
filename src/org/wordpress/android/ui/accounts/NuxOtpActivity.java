package org.wordpress.android.ui.accounts;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Window;

import org.wordpress.android.R;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.widgets.WPTextView;

public class NuxOtpActivity extends SherlockFragmentActivity implements TextWatcher {
    private EditText mOtpCodeText;
    private WPTextView mSignInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.nux_otp_activity);
        mOtpCodeText = (EditText) findViewById(R.id.nux_otp_code);
        mOtpCodeText.addTextChangedListener(this);
        mOtpCodeText.setOnEditorActionListener(mEditorAction);
        mSignInButton = (WPTextView) findViewById(R.id.nux_sign_in_button);
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (fieldsFilled()) {
            mSignInButton.setEnabled(true);
        } else {
            mSignInButton.setEnabled(false);
        }
        mOtpCodeText.setError(null);
    }

    private void signin() {
        if (!isUserDataValid()) {
            return;
        }
        // new SetupBlogTask().execute();
    }

    private void onDoneAction() {
        signin();
    }

    private boolean fieldsFilled() {
        return !EditTextUtils.isEmpty(mOtpCodeText);
    }

    private boolean isUserDataValid() {
        if (EditTextUtils.isEmpty(mOtpCodeText)) {
            mOtpCodeText.setError(getString(R.string.required_field));
            mOtpCodeText.requestFocus();
            return false;
        }
        return true;
    }

    private TextView.OnEditorActionListener mEditorAction = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE || event != null &&
                (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                if (!isUserDataValid()) {
                    return true;
                }
                onDoneAction();
                return true;
            }
            return false;
        }
    };
}
