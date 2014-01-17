package org.wordpress.android.ui.reader.actions;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.datasets.ReaderUserTable;
import org.wordpress.android.models.ReaderRecommendedBlog;
import org.wordpress.android.models.ReaderUser;
import org.wordpress.android.ui.prefs.UserPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.ArrayList;

/**
 * Created by nbradbury on 8/25/13.
 */
public class ReaderUserActions {

    /*
     * request the current user's info, update locally if different than existing local
     */
    public static void updateCurrentUser(final ReaderActions.UpdateResultListener resultListener) {
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                final ReaderActions.UpdateResult result;
                if (jsonObject == null) {
                    result = ReaderActions.UpdateResult.FAILED;
                } else {
                    final ReaderUser serverUser = ReaderUser.fromJson(jsonObject);
                    final ReaderUser localUser = ReaderUserTable.getCurrentUser();
                    if (serverUser == null) {
                        result = ReaderActions.UpdateResult.FAILED;
                    } else if (serverUser.isSameUser(localUser)) {
                        result = ReaderActions.UpdateResult.UNCHANGED;
                    } else {
                        setCurrentUser(serverUser);
                        result = ReaderActions.UpdateResult.CHANGED;
                    }
                }

                if (resultListener != null)
                    resultListener.onUpdateResult(result);
            }
        };

        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
                if (resultListener != null)
                    resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED);
            }
        };

        WordPress.restClient.get("me", listener, errorListener);
    }

    /*
     * set the passed user as the current user in both the local db and prefs
     */
    public static void setCurrentUser(JSONObject jsonUser) {
        if (jsonUser == null)
            return;
        setCurrentUser(ReaderUser.fromJson(jsonUser));
    }
    private static void setCurrentUser(ReaderUser user) {
        if (user == null)
            return;
        ReaderUserTable.addOrUpdateUser(user);
        UserPrefs.setCurrentUserId(user.userId);
    }

    public static void requestRecommendedBlogs(final ReaderActions.RecommendedBlogsListener recommendedBlogsListener) {
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                ArrayList<ReaderRecommendedBlog> recommendedBlogs = new ArrayList<ReaderRecommendedBlog>();
                if (jsonObject != null && jsonObject.has("blogs")) {
                    try {
                        JSONArray blogsArray = jsonObject.getJSONArray("blogs");
                        for (int i=0; i < blogsArray.length(); i++) {
                            ReaderRecommendedBlog recommendedBlog = ReaderRecommendedBlog.fromJson(blogsArray.getJSONObject(i));
                            recommendedBlogs.add(recommendedBlog);
                            ReaderBlogTable.addOrUpdateRecommendedBlog(recommendedBlog);
                        }
                        recommendedBlogsListener.onRecommendedBlogsResult(recommendedBlogs);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        String endpointUrl = "read/recommendations/mine?source=mobile&number=3";
        long[] excludedBlogIds = ReaderBlogTable.getReaderRecommendedBlogIds();
        for (long blogId : excludedBlogIds) {
            endpointUrl += "&exclude[]=" + blogId;
        }

        WordPress.restClient.get(endpointUrl, listener, null);
    }

}