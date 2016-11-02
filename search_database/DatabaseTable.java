package textimage.appbarpractice;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by yanghee on 2016. 11. 2..
 */

public class DatabaseTable {

    private static final String TAG = "DictionaryDatabase";

    public static final String COL_WORD = "WORD";
    public static final String COL_DEFINITION = "DEFINITION";

    private static final String DATABASE_NAME = "dictionary";
    private static final String FTS_VIRTUAL_TABLE = "FTS";
    private static final int DATABASE_VERSION = 1;

    private final DatabaseOpenHelper mDatabaseOpenHelper;

    public DatabaseTable(Context mContext) {
        mDatabaseOpenHelper = new DatabaseOpenHelper(mContext);
    }

    public class DatabaseOpenHelper extends SQLiteOpenHelper {
        private final Context mHelperContext;
        private SQLiteDatabase mDatabase;

        private static final String FTS_TABLE_CREATE =
                "CREATE VIRTUAL TABLE " + FTS_VIRTUAL_TABLE + " USING fts3 (" +
                        COL_WORD + ", " + COL_DEFINITION + ")";

        public DatabaseOpenHelper(Context mContext) {
            super(mContext, DATABASE_NAME, null, DATABASE_VERSION);
            mHelperContext = mContext;
        }
        @Override
        public void onCreate(SQLiteDatabase db) {
            mDatabase = db;
            mDatabase.execSQL(FTS_TABLE_CREATE);
            loadDictionary();
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP IF EXISTS " + FTS_VIRTUAL_TABLE);
            onCreate(db);
        }

        public void loadDictionary() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        loadWords();
                    }catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                }
            }).start();
        }

        private void loadWords() throws IOException {
            final Resources resources = mHelperContext.getResources();
            InputStream is = resources.openRawResource(R.raw.definitions);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            try {
                String line;
                while( (line = br.readLine()) != null ) {
                    String[] strings = TextUtils.split(line, "-");
                    if(strings.length > 2) continue;
                    long id = addWord(strings[0].trim(), strings[1].trim());
                    if(id < 0) {
                        Log.d(TAG, "Unable to add word: " + strings[0].trim());
                    }
                }
            } finally {
                br.close();
                is.close();
            }
        }

        private long addWord(String word, String definition) {
            ContentValues values = new ContentValues();
            values.put(COL_WORD, word);
            values.put(COL_DEFINITION, definition);
            return mDatabase.insert(FTS_VIRTUAL_TABLE, null, values);
        }
    }

    public Cursor getWordMatches(String query, String[] colums) {

    }

    private Cursor query(String selection, String selectionArgs, String colums[]) {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(FTS_VIRTUAL_TABLE);

        Cursor cursor = builder.query(mDatabaseOpenHelper.getReadableDatabase(),
                colums, selection, selectionArgs, null, null, null);
        if(cursor == null) {
            return null;
        } else if(!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        return cursor;

    }


}
