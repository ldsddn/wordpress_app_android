package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.NetworkImageView;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderDatabase;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.RecommendedBlog;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.reader.ReaderPostListFragment.RefreshType;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderAuthActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.actions.ReaderTagActions;
import org.wordpress.android.ui.reader.actions.ReaderUserActions;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPTextView;

import java.util.ArrayList;

/*
 * created by nbradbury
 * this activity serves as the host for ReaderPostListFragment
 */

public class ReaderActivity extends WPActionBarActivity {
    private static final String TAG_FRAGMENT_POST_LIST = "reader_post_list";
    private static final String KEY_INITIAL_UPDATE = "initial_update";
    private static final String KEY_HAS_PURGED = "has_purged";

    private MenuItem mRefreshMenuItem;
    private boolean mHasPerformedInitialUpdate = false;
    private boolean mHasPerformedPurge = false;
    private LinearLayout mRecommendedBlogsCardView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.reader_activity_main);

        getSupportActionBar().setDisplayShowTitleEnabled(false);
        setSupportProgressBarVisibility(false);

        createMenuDrawer(R.layout.reader_activity_main);

        if (savedInstanceState != null) {
            mHasPerformedInitialUpdate = savedInstanceState.getBoolean(KEY_INITIAL_UPDATE);
            mHasPerformedPurge = savedInstanceState.getBoolean(KEY_HAS_PURGED);
        }

        mRecommendedBlogsCardView = (LinearLayout)getLayoutInflater().inflate(R.layout.reader_listitem_recommended_blogs, null);

        ReaderUserActions.requestRecommendedBlogs(new ReaderActions.RecommendedBlogsListener() {
            @Override
            public void onRecommendedBlogsResult(ArrayList<RecommendedBlog> recommendedBlogs) {
                // Build the recommended blogs card view
                for (int i=0; i < recommendedBlogs.size(); i++ ) {
                    RecommendedBlog recommendedBlog = recommendedBlogs.get(i);
                    View recommendedBlogView = getLayoutInflater().inflate(R.layout.reader_recommended_blog, null);

                    NetworkImageView gravatarImageView = (NetworkImageView) recommendedBlogView.findViewById(R.id.recommended_blog_gravatar);
                    String blavatarUrl = GravatarUtils.resizedGravatarUrlForSize(recommendedBlog.getGravatarUrl(), DisplayUtils.dpToPx(ReaderActivity.this, 40));
                    gravatarImageView.setImageUrl(blavatarUrl, WordPress.imageLoader);

                    WPTextView titleTextView = (WPTextView) recommendedBlogView.findViewById(R.id.recommended_blog_title);
                    titleTextView.setText(recommendedBlog.getBlogTitle());

                    WPTextView reasonTextView = (WPTextView) recommendedBlogView.findViewById(R.id.recommended_blog_reason);
                    reasonTextView.setText(recommendedBlog.getReason());

                    mRecommendedBlogsCardView.addView(recommendedBlogView);

                    // Remove divider from last row
                    if (i == recommendedBlogs.size() - 1) {
                        View dividerView = recommendedBlogView.findViewById(R.id.row_divider);
                        dividerView.setVisibility(View.GONE);
                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter(ACTION_REFRESH_POSTS));
        if (getPostListFragment() == null)
            showPostListFragment();
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // purge the database of older data at startup
        if (!mHasPerformedPurge) {
            mHasPerformedPurge = true;
            ReaderDatabase.purgeAsync();
        }

        if (!mHasPerformedInitialUpdate) {
            mHasPerformedInitialUpdate = true;
            // update the current user the first time this is shown - ensures we have their user_id
            // as well as their latest info (in case they changed their avatar, name, etc. since last time)
            AppLog.i(T.READER, "updating current user");
            ReaderUserActions.updateCurrentUser(null);
            // also update cookies so that we can show authenticated images in WebViews
            AppLog.i(T.READER, "updating cookies");
            ReaderAuthActions.updateCookies(this);
            // update followed blogs
            AppLog.i(T.READER, "updating followed blogs");
            ReaderBlogActions.updateFollowedBlogs();
            // update list of followed tags
            updateTagList();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_INITIAL_UPDATE, mHasPerformedInitialUpdate);
        outState.putBoolean(KEY_HAS_PURGED, mHasPerformedPurge);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        boolean isResultOK = (resultCode==Activity.RESULT_OK);
        final ReaderPostListFragment readerFragment = getPostListFragment();

        switch (requestCode) {
            // user just returned from the tag editor
            case Constants.INTENT_READER_TAGS :
                if (isResultOK && readerFragment!=null && data!=null) {
                    // reload tags if they were changed, and set the last tag added as the current one
                    if (data.getBooleanExtra(ReaderTagActivity.KEY_TAGS_CHANGED, false)) {
                        readerFragment.reloadTags();
                        String lastAddedTag = data.getStringExtra(ReaderTagActivity.KEY_LAST_ADDED_TAG);
                        if (!TextUtils.isEmpty(lastAddedTag))
                            readerFragment.setCurrentTag(lastAddedTag);
                    }
                }
                break;

            // user just returned from post detail, reload the displayed post if it changed (will
            // only be RESULT_OK if changed)
            case Constants.INTENT_READER_POST_DETAIL:
                if (isResultOK && readerFragment!=null && data!=null) {
                    long blogId = data.getLongExtra(ReaderPostDetailActivity.ARG_BLOG_ID, 0);
                    long postId = data.getLongExtra(ReaderPostDetailActivity.ARG_POST_ID, 0);
                    boolean isBlogFollowStatusChanged = data.getBooleanExtra(ReaderPostDetailActivity.ARG_BLOG_FOLLOW_STATUS_CHANGED, false);
                    ReaderPost updatedPost = ReaderPostTable.getPost(blogId, postId);
                    if (updatedPost != null) {
                        readerFragment.reloadPost(updatedPost);
                        //Update 'following' status on all other posts in the same blog.
                        if (isBlogFollowStatusChanged) {
                            readerFragment.updateFollowStatusOnPostsForBlog(blogId, updatedPost.isFollowedByCurrentUser);
                        }
                    }
                }
                break;

            // user just returned from reblogging activity, reload the displayed post if reblogging
            // succeeded
            case Constants.INTENT_READER_REBLOG:
                if (isResultOK && readerFragment!=null && data!=null) {
                    long blogId = data.getLongExtra(ReaderReblogActivity.ARG_BLOG_ID, 0);
                    long postId = data.getLongExtra(ReaderReblogActivity.ARG_POST_ID, 0);
                    readerFragment.reloadPost(ReaderPostTable.getPost(blogId, postId));
                }
                break;
        }
    }

    protected void setIsUpdating(boolean isUpdating) {
        if (mRefreshMenuItem==null)
            return;
        if (isUpdating) {
            startAnimatingRefreshButton(mRefreshMenuItem);
        } else {
            stopAnimatingRefreshButton(mRefreshMenuItem);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.reader_native, menu);
        mRefreshMenuItem = menu.findItem(R.id.menu_refresh);
        if (shouldAnimateRefreshButton) {
            shouldAnimateRefreshButton = false;
            startAnimatingRefreshButton(mRefreshMenuItem);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_tags :
                ReaderActivityLauncher.showReaderTagsForResult(this, null);
                return true;
            case R.id.menu_refresh :
                ReaderPostListFragment fragment = getPostListFragment();
                if (fragment!=null) {
                    if (!NetworkUtils.isNetworkAvailable(this)) {
                        ToastUtils.showToast(this, R.string.reader_toast_err_no_connection, ToastUtils.Duration.LONG);
                    } else {
                        fragment.updatePostsWithCurrentTag(ReaderActions.RequestDataAction.LOAD_NEWER, RefreshType.MANUAL);
                    }
                    return true;
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSignout() {
        super.onSignout();
        mHasPerformedInitialUpdate = false;

        // reader database will have been cleared by the time this is called, but the fragment must
        // be removed or else it will continue to show the same articles - onResume() will take care
        // of re-displaying the fragment if necessary
        removePostListFragment();
    }

    /*
     * show fragment containing list of latest posts
     */
    private void showPostListFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, ReaderPostListFragment.newInstance(this), TAG_FRAGMENT_POST_LIST)
                .commit();
    }

    private void removePostListFragment() {
        ReaderPostListFragment fragment = getPostListFragment();
        if (fragment==null)
            return;

        getSupportFragmentManager()
                .beginTransaction()
                .remove(fragment)
                .commit();
    }

    private ReaderPostListFragment getPostListFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_POST_LIST);
        if (fragment==null)
            return null;
        return ((ReaderPostListFragment) fragment);
    }

    /*
     * request list of tags from the server
     */
    protected void updateTagList() {
        ReaderActions.UpdateResultListener listener = new ReaderActions.UpdateResultListener() {
            @Override
            public void onUpdateResult(ReaderActions.UpdateResult result) {
                // refresh tags if they've changed
                if (result == ReaderActions.UpdateResult.CHANGED) {
                    ReaderPostListFragment fragment = getPostListFragment();
                    if (fragment != null)
                        fragment.refreshTags();
                }
            }
        };
        ReaderTagActions.updateTags(listener);
    }

    /*
     * this broadcast receiver handles the ACTION_REFRESH_POSTS action, which may be called from the
     * post list fragment if the device is rotated while an update is in progress
     */
    protected static final String ACTION_REFRESH_POSTS = "action_refresh_posts";
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_REFRESH_POSTS.equals(intent.getAction())) {
                AppLog.i(T.READER, "received ACTION_REFRESH_POSTS");
                ReaderPostListFragment fragment = getPostListFragment();
                if (fragment != null)
                    fragment.refreshPosts();
            }
        }
    };

    public View getRecommendedBlogsView() {
        return mRecommendedBlogsCardView;
    }
}
