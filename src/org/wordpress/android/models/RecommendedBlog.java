package org.wordpress.android.models;

import org.json.JSONObject;
import org.wordpress.android.util.JSONUtil;

/**
 * Created by dan on 1/15/14.
 * For recommended blogs returned in read/recommended/mine
 */
public class RecommendedBlog {

    private long blogId;
    private long recommendationId;
    private String followSource;
    private String gravatarUrl;
    private String blogUrl;
    private String blogTitle;
    private String reason;

    public static RecommendedBlog fromJson(JSONObject json) {
        if (json==null)
            throw new IllegalArgumentException("null json recommended post");
        RecommendedBlog recommendedBlog = new RecommendedBlog();

        recommendedBlog.setBlogId(json.optLong("blog_id"));
        recommendedBlog.setRecommendationId(json.optLong("follow_reco_id"));
        recommendedBlog.setFollowSource(JSONUtil.getString(json, "follow_source"));
        recommendedBlog.setGravatarUrl(JSONUtil.getString(json, "image"));
        recommendedBlog.setBlogUrl(JSONUtil.getString(json, "blog_domain"));
        recommendedBlog.setBlogTitle(JSONUtil.getString(json, "title"));
        recommendedBlog.setReason(JSONUtil.getString(json, "reason"));

        return recommendedBlog;
    }

    public String getFollowSource() {
        return followSource;
    }

    public void setFollowSource(String followSource) {
        this.followSource = followSource;
    }

    public long getBlogId() {
        return blogId;
    }

    public void setBlogId(long blogId) {
        this.blogId = blogId;
    }

    public long getRecommendationId() {
        return recommendationId;
    }

    public void setRecommendationId(long recommendationId) {
        this.recommendationId = recommendationId;
    }

    public String getGravatarUrl() {
        return gravatarUrl;
    }

    public void setGravatarUrl(String gravatarUrl) {
        this.gravatarUrl = gravatarUrl;
    }

    public String getBlogUrl() {
        return blogUrl;
    }

    public void setBlogUrl(String blogUrl) {
        this.blogUrl = blogUrl;
    }

    public String getBlogTitle() {
        return blogTitle;
    }

    public void setBlogTitle(String blogTitle) {
        this.blogTitle = blogTitle;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
