package org.wordpress.android.ui.reader.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;

public class ReaderFollowButton extends LinearLayout {
    private TextView mTextFollow;
    private boolean mIsFollowed;

    public ReaderFollowButton(Context context){
        super(context);
        initView(context, null);
    }

    public ReaderFollowButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    public ReaderFollowButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs) {
        inflate(context, R.layout.reader_follow_button, this);
        mTextFollow = (TextView) findViewById(R.id.text_follow);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mTextFollow.setBackgroundResource(R.drawable.ripple_oval);
        }
    }

    private void updateFollowText() {
        mTextFollow.setSelected(mIsFollowed);
        mTextFollow.setText(mIsFollowed ? R.string.reader_btn_unfollow : R.string.reader_btn_follow);
    }

    public void setIsFollowed(boolean isFollowed, boolean animateChanges) {
        if (isFollowed == mIsFollowed) {
            return;
        }

        mIsFollowed = isFollowed;

        if (animateChanges) {
            ObjectAnimator anim = ObjectAnimator.ofFloat(mTextFollow, View.SCALE_X, 1f, 0f);
            anim.setRepeatMode(ValueAnimator.REVERSE);
            anim.setRepeatCount(1);

            // change the button text and selection state before scaling back in
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationRepeat(Animator animation) {
                    updateFollowText();
                }
            });

            AnimatorSet set = new AnimatorSet();
            set.play(anim);
            set.setDuration(250);
            set.setInterpolator(new AccelerateDecelerateInterpolator());

            set.start();
        } else {
            setSelected(isFollowed);
            updateFollowText();
        }
    }
}