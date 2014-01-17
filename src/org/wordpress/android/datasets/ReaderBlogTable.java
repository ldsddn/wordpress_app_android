package org.wordpress.android.datasets;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import org.wordpress.android.models.ReaderRecommendedBlog;
import org.wordpress.android.models.ReaderUrlList;
import org.wordpress.android.models.ReaderUser;
import org.wordpress.android.models.ReaderUserList;
import org.wordpress.android.util.SqlUtils;
import org.wordpress.android.util.UrlUtils;

import java.util.Iterator;

/**
 * Created by nbradbury on 9/2/13.
 * urls of blogs we know about and whether they're followed by the current user
 */
public class ReaderBlogTable {

    private static final String RECOMMENDED_BLOG_COLUMN_NAMES =
            "blog_id, recommendation_id, follow_source, gravatar_url, blog_url, blog_title, reason";

    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_blog_urls ("
                + "	blog_url        TEXT COLLATE NOCASE PRIMARY KEY,"
                + " is_followed     INTEGER DEFAULT 0)");
        
        db.execSQL("CREATE TABLE tbl_recommended_blogs ("
                + "	blog_id             INTEGER PRIMARY KEY,"
                + "	recommendation_id   INTEGER DEFAULT 0,"
                + " follow_source       TEXT,"
                + "	gravatar_url        TEXT,"
                + "	blog_url            TEXT,"
                + "	blog_title          TEXT,"
                + "	reason              TEXT)");
    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS tbl_blog_urls");
        db.execSQL("DROP TABLE IF EXISTS tbl_recommended_blogs");
    }

    public static void setFollowedBlogUrls(ReaderUrlList urls) {
        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        db.beginTransaction();
        SQLiteStatement stmt = db.compileStatement("INSERT OR REPLACE INTO tbl_blog_urls (blog_url, is_followed) VALUES (?1,?2)");
        try {
            // first set all existing blogs to not followed
            db.execSQL("UPDATE tbl_blog_urls SET is_followed=0");

            // then insert/replace passed ones as followed
            if (urls!=null && urls.size() > 0) {
                Iterator<String> it = urls.iterator();
                long sqlTrue = SqlUtils.boolToSql(true);
                while (it.hasNext()) {
                    stmt.bindString(1, it.next());
                    stmt.bindLong  (2, sqlTrue);
                    stmt.execute();
                    stmt.clearBindings();
                }
            }

            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
            SqlUtils.closeStatement(stmt);
        }
    }

    public static ReaderUrlList getFollowedBlogUrls() {
        Cursor c = ReaderDatabase.getReadableDb().rawQuery("SELECT blog_url FROM tbl_blog_urls WHERE is_followed!=0", null);
        try {
            ReaderUrlList urls = new ReaderUrlList();
            if (c.moveToFirst()) {
                do {
                    urls.add(c.getString(0));
                } while (c.moveToNext());
            }
            return urls;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    public static boolean isFollowedBlogUrl(String url) {
        if (TextUtils.isEmpty(url))
            return false;
        String[] args = {UrlUtils.normalizeUrl(url)};
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(), "SELECT is_followed FROM tbl_blog_urls WHERE blog_url=?", args);
    }

    public static void setIsFollowedBlogUrl(String url, boolean isFollowed) {
        if (TextUtils.isEmpty(url))
            return;
        SQLiteStatement stmt = ReaderDatabase.getWritableDb().compileStatement("INSERT OR REPLACE INTO tbl_blog_urls (blog_url, is_followed) VALUES (?1,?2)");
        try {
            stmt.bindString(1, UrlUtils.normalizeUrl(url));
            stmt.bindLong  (2, SqlUtils.boolToSql(isFollowed));
            stmt.execute();
        } finally {
            SqlUtils.closeStatement(stmt);
        }
    }

    public static void addOrUpdateRecommendedBlog(ReaderRecommendedBlog recommendedBlog) {
        if (recommendedBlog == null)
            return;

        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        db.beginTransaction();
        SQLiteStatement stmt = db.compileStatement("INSERT OR REPLACE INTO tbl_recommended_blogs ("
                + RECOMMENDED_BLOG_COLUMN_NAMES + ") VALUES (?1,?2,?3,?4,?5,?6,?7)");
        try {
            stmt.bindLong(1, recommendedBlog.getBlogId());
            stmt.bindLong(2, recommendedBlog.getRecommendationId());
            stmt.bindString(3, recommendedBlog.getFollowSource());
            stmt.bindString(4, recommendedBlog.getGravatarUrl());
            stmt.bindString(5, recommendedBlog.getBlogUrl());
            stmt.bindString(6, recommendedBlog.getBlogTitle());
            stmt.bindString(7, recommendedBlog.getReason());
            stmt.execute();
            stmt.clearBindings();

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            SqlUtils.closeStatement(stmt);
        }
    }

    public static long[] getReaderRecommendedBlogIds() {
        String sql = "SELECT blog_id from tbl_recommended_blogs";

        Cursor c = ReaderDatabase.getReadableDb().rawQuery(sql, null);
        long[] blogIds = new long[c.getCount()];
        try {
            int i = 0;
            while (c.moveToNext()) {
                blogIds[i] = c.getLong(c.getColumnIndex("blog_id"));
                i++;
            }
        } finally {
            SqlUtils.closeCursor(c);
        }

        return blogIds;
    }

}
