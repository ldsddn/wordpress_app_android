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

/**
 * Follow button used throughout the reader
 */
public class ReaderFollowButton extends LinearLayout {
    private TextView mTextFollow;
    private boolean mIsFollowed;

    public ReaderFollowButton(Context context){
        super(context);
        initView(context);
    }

    public ReaderFollowButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public ReaderFollowButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }

    private void initView(Context context) {
        inflate(context, R.layout.reader_follow_button, this);
        mTextFollow = (TextView) findViewById(R.id.text_follow_button);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setBackgroundResource(R.drawable.ripple_rect);
        }
    }

    /*
     * inverse is used when follow button is shown in toolbar
     */
    public void setIsInverse(boolean isInverse) {
        int color;
        if (isInverse) {
            color = getContext().getResources().getColor(R.color.reader_follow_text_inverse);
        } else {
            color = getContext().getResources().getColor(R.color.reader_follow_text);
        }
        mTextFollow.setTextColor(color);
    }

    private void updateFollowText() {
        mTextFollow.setSelected(mIsFollowed);
        mTextFollow.setText(mIsFollowed ? R.string.reader_btn_unfollow : R.string.reader_btn_follow);
    }

    public void setIsFollowed(boolean isFollowed, boolean animateChanges) {
        if (isFollowed == mIsFollowed && mTextFollow.isSelected() == isFollowed) {
            return;
        }

        mIsFollowed = isFollowed;

        if (animateChanges) {
            ObjectAnimator anim = ObjectAnimator.ofFloat(mTextFollow, View.SCALE_X, 1f, 0f);
            anim.setRepeatMode(ValueAnimator.REVERSE);
            anim.setRepeatCount(1);

            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationRepeat(Animator animation) {
                    updateFollowText();
                }
            });

            AnimatorSet set = new AnimatorSet();
            set.play(anim);
            set.setDuration(200);
            set.setInterpolator(new AccelerateDecelerateInterpolator());

            set.start();
        } else {
            updateFollowText();
        }
    }
}