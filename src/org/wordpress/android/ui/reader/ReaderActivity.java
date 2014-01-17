package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderDatabase;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.reader.ReaderPostListFragment.PostListType;
import org.wordpress.android.ui.reader.ReaderPostListFragment.RefreshType;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderAuthActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.actions.ReaderTagActions;
import org.wordpress.android.ui.reader.actions.ReaderUserActions;
import org.wordpress.android.ui.reader.adapters.ReaderActionBarTagAdapter;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;

/*
 * created by nbradbury
 * this activity serves as the host for ReaderPostListFragment
 */

public class ReaderActivity extends WPActionBarActivity
                            implements FragmentManager.OnBackStackChangedListener,
                            ActionBar.OnNavigationListener
{
    private static final String TAG_FRAGMENT_POST_LIST = "reader_post_list";
    private static final String KEY_INITIAL_UPDATE = "initial_update";
    private static final String KEY_HAS_PURGED = "has_purged";
    protected static final String ARG_LIST_TYPE = "list_type";
    protected static final String ARG_LIST_TYPE_ID = "list_type_id";

    private MenuItem mRefreshMenuItem;
    private boolean mHasPerformedInitialUpdate = false;
    private boolean mHasPerformedPurge = false;

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

        getSupportFragmentManager().addOnBackStackChangedListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter(ACTION_REFRESH_POSTS));
        if (getPostListFragment() == null) {
            final PostListType listType;
            final long listTypeId;
            if (getIntent() != null && getIntent().hasExtra(ARG_LIST_TYPE)) {
                listType = (PostListType) getIntent().getSerializableExtra(ARG_LIST_TYPE);
                listTypeId = getIntent().getLongExtra(ARG_LIST_TYPE_ID, 0);
            } else {
                listType = PostListType.TAG;
                listTypeId = 0;
            }
            showPostListFragment(listType, listTypeId);
        }
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
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
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
                        getActionBarAdapter().reloadTags();
                        readerFragment.checkCurrentTag();
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
                        // update 'following' status on all other posts in the same blog.
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        // only show tag item when viewing posts with a specific tag
        MenuItem mnuTags = menu.findItem(R.id.menu_tags);
        if (mnuTags != null)
            mnuTags.setVisible(getCurrentPostListType() == PostListType.TAG);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_tags :
                ReaderActivityLauncher.showReaderTagsForResult(this, null);
                return true;
            case R.id.menu_refresh :
                ReaderPostListFragment fragment = getPostListFragment();
                if (fragment != null) {
                    if (!NetworkUtils.isNetworkAvailable(this)) {
                        ToastUtils.showToast(this, R.string.reader_toast_err_no_connection, ToastUtils.Duration.LONG);
                    } else {
                        PostListType listType = fragment.getPostListType();
                        switch (listType) {
                            case TAG:
                                fragment.updatePostsWithCurrentTag(ReaderActions.RequestDataAction.LOAD_NEWER, RefreshType.MANUAL);
                                break;
                            case BLOG:
                                fragment.updatePostsInCurrentBlog(RefreshType.MANUAL);
                                break;
                            default:
                                break;
                        }
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

        // reader database will have been cleared by the time this is called, but the fragments must
        // be removed or else they will continue to show the same articles - onResume() will take care
        // of re-displaying the fragment if necessary
        removePostListFragments();
    }

    /*
     * show fragment containing list of latest posts
     */
    protected void showPostListFragment(PostListType listType, long listTypeId) {
        ReaderPostListFragment newFragment = ReaderPostListFragment.newInstance(this, listType, listTypeId);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // if a fragment already exists, and this one to backstack and provide custom animation
        if (getPostListFragment() != null) {
            transaction.addToBackStack(null);
            transaction.setCustomAnimations(R.anim.reader_flyin_from_bottom,
                                            R.anim.reader_flyout,
                                            R.anim.reader_flyin_from_bottom,
                                            R.anim.reader_flyout);
        }

       transaction.add(R.id.fragment_container, newFragment, TAG_FRAGMENT_POST_LIST)
                  .commit();

        setupActionBar(listType);
        invalidateOptionsMenu();
    }

    /*
     * removes all post list fragments
     */
    private void removePostListFragments() {
        ReaderPostListFragment fragment = getPostListFragment();
        while (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .remove(fragment)
                    .commit();
            fragment = getPostListFragment();
        }
    }

    /*
     * returns the current post list fragment (if any exists)
     */
    private ReaderPostListFragment getPostListFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_POST_LIST);
        if (fragment == null)
            return null;
        return ((ReaderPostListFragment) fragment);
    }

    protected PostListType getCurrentPostListType() {
        ReaderPostListFragment fragment = getPostListFragment();
        if (fragment == null)
            return PostListType.TAG;
        return fragment.getPostListType();
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
                    getActionBarAdapter().refreshTags();
                    ReaderPostListFragment fragment = getPostListFragment();
                    if (fragment != null)
                        fragment.checkCurrentTag();
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

    private ReaderActionBarTagAdapter mActionBarAdapter;
    protected ReaderActionBarTagAdapter getActionBarAdapter() {
        if (mActionBarAdapter == null) {
            ReaderActions.DataLoadedListener dataListener = new ReaderActions.DataLoadedListener() {
                @Override
                public void onDataLoaded(boolean isEmpty) {
                    ReaderPostListFragment fragment = getPostListFragment();
                    if (fragment != null)
                        selectTagInActionBar(fragment.getCurrentTagName());
                }
            };

            mActionBarAdapter = new ReaderActionBarTagAdapter(this, isStaticMenuDrawer(), dataListener);
        }
        return mActionBarAdapter;
    }

    private boolean hasActionBarAdapter() {
        return (mActionBarAdapter != null);
    }

    /*
     * make sure the passed tag is the one selected in the actionbar
     */
    protected void selectTagInActionBar(String tagName) {
        if (tagName==null)
            return;

        ActionBar actionBar = getSupportActionBar();
        if (actionBar==null)
            return;

        int position = getActionBarAdapter().getIndexOfTagName(tagName);
        if (position == -1)
            return;
        if (position == actionBar.getSelectedNavigationIndex())
            return;

        actionBar.setSelectedNavigationItem(position);
    }

    private PostListType mPreviousListType = null;
    private void setupActionBar(PostListType listType) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null)
            return;

        if (mPreviousListType != null && mPreviousListType.equals(listType))
            return;

        mPreviousListType = listType;

        switch (listType) {
            case TAG:
                actionBar.setDisplayShowTitleEnabled(false);
                actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
                if (!hasActionBarAdapter())
                    actionBar.setListNavigationCallbacks(getActionBarAdapter(), this);
                break;

            case BLOG:
                actionBar.setDisplayShowTitleEnabled(true);
                actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
                break;

            default:
                break;
        }
    }

    @Override
    public void onBackStackChanged() {
        setupActionBar(getCurrentPostListType());
        invalidateOptionsMenu();
    }

    // called when tag selected in the ActionBar dropdown
    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        ReaderTag tag = (ReaderTag) getActionBarAdapter().getItem(itemPosition);
        if (tag != null) {
            ReaderPostListFragment fragment = getPostListFragment();
            if (fragment != null)
                fragment.setCurrentTag(tag.getTagName());
            AppLog.d(T.READER, "tag chosen from actionbar: " + tag.getTagName());
        }
        return true;
    }
}
