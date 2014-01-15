package org.wordpress.android.ui.reader;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderPostActions;
import org.wordpress.android.ui.reader.adapters.ReaderBlogPostAdapter;

/**
 * Created by nbradbury on 1/15/14.
 * Blog detail view, displayed when user taps a gravatar
 */
public class ReaderBlogDetailActivity  extends WPActionBarActivity {
    protected static final String ARG_BLOG_ID = "blog_id";

    private ReaderBlogPostAdapter mAdapter;
    private ListView mListView;
    private ProgressBar mProgress;
    private long mBlogId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reader_activity_blog_detail);

        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mBlogId = getIntent().getLongExtra(ARG_BLOG_ID, 0L);
        getListView().setAdapter(getAdapter());

        mProgress = (ProgressBar) findViewById(R.id.progress_loading);

        showBlogInfo();

        getAdapter().loadPosts();
        fetchPosts();
    }

    private ReaderBlogPostAdapter getAdapter() {
        if (mAdapter == null)
            mAdapter = new ReaderBlogPostAdapter(this, mBlogId, mDataLoadedListener);
        return mAdapter;
    }

    private ListView getListView() {
        if (mListView == null) {
            mListView = (ListView) findViewById(android.R.id.list);
        }
        return mListView;
    }

    /*
     * called by post adapter when data has been loaded
     */
    private ReaderActions.DataLoadedListener mDataLoadedListener = new ReaderActions.DataLoadedListener() {
        @Override
        public void onDataLoaded(boolean isEmpty) {
            if (isEmpty) {

            } else {

            }
        }
    };

    private void fetchPosts() {
        mProgress.setVisibility(View.VISIBLE);
        ReaderPostActions.requestPostsForBlog(mBlogId, new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                mProgress.setVisibility(View.GONE);
                if (succeeded) {
                    getAdapter().loadPosts();
                }
            }
        });
    }

    private void showBlogInfo() {
        final TextView txtBlogName = (TextView) findViewById(R.id.text_blog_name);
        final TextView txtBlogDescription = (TextView) findViewById(R.id.text_blog_description);
    }

    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home :
                onBackPressed();
                return true;
            default :
                return super.onOptionsItemSelected(item);
        }
    }
}
