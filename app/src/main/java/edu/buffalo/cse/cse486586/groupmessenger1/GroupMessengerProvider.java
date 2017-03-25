package edu.buffalo.cse.cse486586.groupmessenger1;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * <p>
 * Please read:
 * <p>
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * <p>
 * before you start to get yourself familiarized with ContentProvider.
 * <p>
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 *
 * @author stevko and vkumar25
 *
 * References : 1) https://developer.android.com/reference/android/database/sqlite/SQLiteDatabase.html
 *              2) https://developer.android.com/guide/topics/providers/content-provider-basics.html
 *              3) https://developer.android.com/guide/topics/providers/content-provider-creating.html
 *              4) https://developer.android.com/reference/android/database/sqlite/SQLiteQueryBuilder.html
 *              5) https://developer.android.com/training/basics/data-storage/databases.html
 */
public class GroupMessengerProvider extends ContentProvider {

    private static String TAG = GroupMessengerProvider.class.getName();

    private Context mContext;

    private SQLiteDatabase mSqLiteDatabase;
    private static final String DATABASE_NAME = "GMP1";
    private static final String TABLE_NAME = "KeyValueTable";
    private static final String KEY_COLUMN_NAME = "key";
    private static final String VALUE_COLUMN_NAME = "value";
    private static final int DATABASE_VERSION = 1;
    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME +
            "( " + KEY_COLUMN_NAME + " TEXT PRIMARY KEY NOT NULL, " + VALUE_COLUMN_NAME + " TEXT NOT NULL);";

    static final Uri CONTENT_URI = Uri.parse("edu.buffalo.cse.cse486586.groupmessenger1.GroupMessengerProvider");

    protected static final class MainDatabaseHelper extends SQLiteOpenHelper {

        public MainDatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public SQLiteDatabase getWritableDatabase() {
            return super.getWritableDatabase();
        }

        @Override
        public SQLiteDatabase getReadableDatabase() {
            return super.getReadableDatabase();
        }

        @Override
        public synchronized void close() {
            super.close();
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            super.onDowngrade(db, oldVersion, newVersion);
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            super.onOpen(db);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         * 
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */
        Log.v(TAG, "inserting : " + values.toString());

        try {
            long rowID = mSqLiteDatabase.replace(TABLE_NAME, "", values);

            if (rowID > 0) {
                uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
                mContext.getContentResolver().notifyChange(uri, null);
                return uri;
            }
        } catch (SQLException e) {
            Log.e(TAG, "Insert Function. SQLException. Cannot add item to" + uri);
            e.printStackTrace();
        } catch (Exception e) {
            Log.e(TAG, "Insert Function. Exception : " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        mContext = getContext();

        MainDatabaseHelper databaseHelper = new MainDatabaseHelper(mContext);

        mSqLiteDatabase = databaseHelper.getWritableDatabase();
        if (mSqLiteDatabase == null) {
            Log.e(TAG, "Function : onCreate, database is null.!!! Have Fun Fixing it now :P");
            return FALSE;
        } else {
            return TRUE;
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */
        Log.v(TAG, "Query Function with selection " + selection);
        try {
            String newSelection = KEY_COLUMN_NAME + " = ?";
            String[] newSelectionArgs = { selection };

            Cursor cursor = mSqLiteDatabase.query(TABLE_NAME, projection, newSelection, newSelectionArgs, null, null, sortOrder);
            cursor.setNotificationUri(mContext.getContentResolver(), uri);
            return cursor;
        } catch (Exception e) {
            Log.e(TAG, "Query Function. Exception : " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
