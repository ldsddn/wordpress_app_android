package org.wordpress.android.ui.reader;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import org.wordpress.android.models.ReaderRecommendedBlog;

import java.util.ArrayList;

/**
 * Created by dan on 1/16/14.
 */
public class ReaderRecommendedBlogsView extends LinearLayout {

    private ArrayList<ReaderRecommendedBlog> mRecommendedBlogs;

    public ReaderRecommendedBlogsView(Context context) {
        super(context);
    }

    public ReaderRecommendedBlogsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ArrayList<ReaderRecommendedBlog> getRecommendedBlogs() {
        return mRecommendedBlogs;
    }

    public void setRecommendedBlogs(ArrayList<ReaderRecommendedBlog> recommendedBlogs) {
        this.mRecommendedBlogs = recommendedBlogs;
    }

    public boolean hasRecommendedBlogs() {
        return mRecommendedBlogs != null;
    }
}
