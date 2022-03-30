package com.revature.contentproviderexample

import android.content.*
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri

class RevatureCP: ContentProvider() {

    companion object {
        const val DATABASE_NAME = "UserDB"
        const val DATABASE_VERSION = 1
        const val TABLE_NAME = "Users"

        const val name = "name"
        const val id = "id"

        //defining the authority(content URI)

        const val PROVIDER_NAME = "com.revature.contentproviderexample.RevatureCP.provider"

        //defining the content URI

        const val URL = "content://$PROVIDER_NAME/users"

        //parsing the content URI

        val CONTENT_URI = Uri.parse(URL)


        var uriMatcher: UriMatcher?=null

        private val values:HashMap<String, String>?=null

        //create a table

        const val CREATE_DB_TABLE = (

                "CREATE TABLE " + TABLE_NAME +
                        "(id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "name TEXT NOT NULL);"

                )
        const val uriCode = 1

        init {
            // to match the content URI
            uriMatcher = UriMatcher(UriMatcher.NO_MATCH)

            // to access whole table
            uriMatcher!!.addURI(
                PROVIDER_NAME,
                "users",
                uriCode
            )

            // to access particular row
            uriMatcher!!.addURI(
                PROVIDER_NAME,
                "users/*",
                uriCode
            )

        }

    }


    override fun onCreate(): Boolean {
        val context = context

        val dbHelper= context?.let { DatabaseHelper(it) }
        db = dbHelper?.writableDatabase

        return db!=null


    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {

        var sortOrder = sortOrder
        val qb = SQLiteQueryBuilder()
        qb.tables = TABLE_NAME

        when(uriMatcher!!.match(uri)) {

            uriCode -> qb.projectionMap = values

            else -> IllegalArgumentException("Unknown URI $uri")

        }

        if(sortOrder==null || sortOrder==="") {

            sortOrder = id

        }

        val c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder)

        c.setNotificationUri(context!!.contentResolver, uri)

        return c

    }

    override fun getType(p0: Uri): String? {
        TODO("Not yet implemented")
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val rowID=db!!.insert(TABLE_NAME, "", values)
        if(rowID>0) {
            val _uri = ContentUris.withAppendedId(uri, rowID)
            context!!.contentResolver.notifyChange(_uri,null)

            return _uri
        }

        throw SQLiteException("Failed to add record into $uri")

    }

    override fun delete(p0: Uri, p1: String?, p2: Array<out String>?): Int {
        TODO("Not yet implemented")
    }

    override fun update(p0: Uri, p1: ContentValues?, p2: String?, p3: Array<out String>?): Int {
        TODO("Not yet implemented")
    }


    private var db: SQLiteDatabase?=null

    private class DatabaseHelper
    internal constructor(context: Context): SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
        override fun onCreate(db: SQLiteDatabase?) {
            db?.execSQL(CREATE_DB_TABLE)
        }

        override fun onUpgrade(db: SQLiteDatabase?, p1: Int, p2: Int) {
            db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
            onCreate(db)
        }


    }

}