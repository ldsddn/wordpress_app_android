package org.wordpress.android.ui.reader.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderPostList;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.SysUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

/**
 * Created by nbradbury on 6/27/13.
 * adapter for list of posts in a specific blog
 */
public class ReaderBlogPostAdapter extends BaseAdapter {
    private int mPhotonWidth;
    private int mPhotonHeight;
    private int mAvatarSz;

    private long mBlogId;

    private LayoutInflater mInflater;
    private ReaderPostList mPosts = new ReaderPostList();

    private ReaderActions.DataLoadedListener mDataLoadedListener;

    public ReaderBlogPostAdapter(Context context, long blogId, ReaderActions.DataLoadedListener dataLoadedListener) {
        super();

        mInflater = LayoutInflater.from(context);
        mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_medium);

        int displayWidth = DisplayUtils.getDisplayPixelWidth(context);
        int listMargin = context.getResources().getDimensionPixelSize(R.dimen.reader_list_margin);
        mPhotonWidth = displayWidth - (listMargin * 2);
        mPhotonHeight = context.getResources().getDimensionPixelSize(R.dimen.reader_featured_image_height);

        mBlogId = blogId;
        mDataLoadedListener = dataLoadedListener;
    }

    private void clear() {
        if (!mPosts.isEmpty()) {
            mPosts.clear();
            notifyDataSetChanged();
        }
    }
    
    @SuppressLint("NewApi")
    public void loadPosts() {
        if (mIsTaskRunning)
            AppLog.w(T.READER, "reader blog posts task already running");

        if (SysUtils.canUseExecuteOnExecutor()) {
            new LoadPostsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            new LoadPostsTask().execute();
        }
    }

    @Override
    public int getCount() {
        return mPosts.size();
    }

    @Override
    public Object getItem(int position) {
        return mPosts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ReaderPost post = (ReaderPost) getItem(position);
        final BlogPostViewHolder holder;

        if (convertView==null) {
            convertView = mInflater.inflate(R.layout.reader_listitem_blog_post, parent, false);
            holder = new BlogPostViewHolder();
            holder.txtTitle = (TextView) convertView.findViewById(R.id.text_title);
            holder.txtText = (TextView) convertView.findViewById(R.id.text_excerpt);
            holder.imgFeatured = (WPNetworkImageView) convertView.findViewById(R.id.image_featured);
            convertView.setTag(holder);
        } else {
            holder = (BlogPostViewHolder) convertView.getTag();
        }

        holder.txtTitle.setText(post.getTitle());

        if (post.hasExcerpt()) {
            holder.txtText.setVisibility(View.VISIBLE);
            holder.txtText.setText(post.getExcerpt());
        } else {
            holder.txtText.setVisibility(View.GONE);
        }

        if (post.hasFeaturedImage()) {
            final String imageUrl = post.getFeaturedImageForDisplay(mPhotonWidth, mPhotonHeight);
            holder.imgFeatured.setImageUrl(imageUrl, WPNetworkImageView.ImageType.PHOTO);
            holder.imgFeatured.setVisibility(View.VISIBLE);
        } else if (post.hasFeaturedVideo()) {
            holder.imgFeatured.setVideoUrl(post.postId, post.getFeaturedVideo());
            holder.imgFeatured.setVisibility(View.VISIBLE);
        } else {
            holder.imgFeatured.setVisibility(View.GONE);
        }

        return convertView;
    }

    private static class BlogPostViewHolder {
        TextView txtTitle;
        TextView txtText;
        WPNetworkImageView imgFeatured;
        WPNetworkImageView imgAvatar;
    }

    /*
     * AsyncTask to load posts in the current blog
     */
    private boolean mIsTaskRunning = false;
    private class LoadPostsTask extends AsyncTask<Void, Void, Boolean> {
        ReaderPostList tmpPosts;
        @Override
        protected void onPreExecute() {
            mIsTaskRunning = true;
        }
        @Override
        protected void onCancelled() {
            mIsTaskRunning = false;
        }
        @Override
        protected Boolean doInBackground(Void... params) {
            tmpPosts = ReaderPostTable.getPostsInBlog(mBlogId, Constants.READER_MAX_POSTS_TO_DISPLAY);

            if (mPosts.isSameList(tmpPosts))
                return false;

            return true;
        }
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mPosts = (ReaderPostList)(tmpPosts.clone());
                notifyDataSetChanged();
            }

            if (mDataLoadedListener!=null)
                mDataLoadedListener.onDataLoaded(isEmpty());

            mIsTaskRunning = false;
        }
    }
}
