package com.example.android.pets.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.example.android.pets.data.PetContract.PetEntry;


/**
 * ContentProvider for Pets app
 */
public class PetProvider extends ContentProvider {
    public static final String LOG_TAG = PetProvider.class.getSimpleName();
    /**
     * URI matcher code for content URI for pets table
     */
    private static final int PETS = 100;
    /**
     * URI matcher code for content URI for a single pet in the pets table
     */
    private static final int PET_ID = 101;
    /**
     * UriMatcher object to match a content URI to a corresponding code
     * Input passed into the constructor represents the code to return for the root URI
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    //Static initializer. This is run the first time anything is called from this class.
    static {
        //Multiple rows
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS, PETS);
        //Single row # placeholder for a number
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS + "/#", PET_ID);
    }

    /**
     * Initialize the db helper object
     */
    private PetDbHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new PetDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        // cursor to hold results of the query
        Cursor cursor;
        //URI matcher to match URI to returned code ie. set path ids
        int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                //query the pets table directly for a cursor containing multiple rows
                cursor = database.query(PetEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case PET_ID:
                //extract the ID from URI ie. selection "_id=?" selectionArgs int[int ?]
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                //perform the query where _id equals row # to return a Cursor containing that row
                cursor = database.query(PetEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }
        //Set notif URI on the Cursor
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return PetEntry.CONTENT_LIST_TYPE;
            case PET_ID:
                return PetEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI "+uri+" with match "+match);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return insertPet(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Insert a pet into db w/ content values.  Return the new content URI for inserted row
     */
    private Uri insertPet(Uri uri, ContentValues values) {
        //check that name is not null
        String name = values.getAsString(PetEntry.COLUMN_PET_NAME);
        if (name.isEmpty()) {
            // Toast.makeText(getContext(),"name field :" +name+"/", Toast.LENGTH_LONG ).show();
            throw new IllegalArgumentException("Pet requires a name");
        }
        //validate weight not 0kg
        Integer weight = values.getAsInteger(PetEntry.COLUMN_PET_WEIGHT);
        if (weight == null || weight < 0) {
            throw new IllegalArgumentException("Pet requires valid weight");
        }
        // Get writable db object
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        // Insert the new pet w/ content values
        long id = database.insert(PetEntry.TABLE_NAME, null, values);
        // If the ID -1, insertion failed. Log an error and return null.
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }
        //Notify listeners data has changed for pet content URI
        getContext().getContentResolver().notifyChange(uri, null);
        // Return the new URI with the ID appended at the end
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        int rowsDeleted;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                rowsDeleted = database.delete(PetEntry.TABLE_NAME, selection, selectionArgs);
                break;

            case PET_ID:
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[]{ String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(PetEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }
        if(rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return updatePet(uri, contentValues, selection, selectionArgs);
            case PET_ID:
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updatePet(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not suppported for " + uri);
        }
    }

    private int updatePet(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (values.size() == 0) {
            return 0;
        }
        if (values.containsKey(PetEntry.COLUMN_PET_NAME)) {
            String name = values.getAsString(PetEntry.COLUMN_PET_NAME);
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Pet requires a name");
            }
        }
        if (values.containsKey(PetEntry.COLUMN_PET_WEIGHT)) {
            //validate weight not 0kg
            Integer weight = values.getAsInteger(PetEntry.COLUMN_PET_WEIGHT);
            if (weight == null || weight < 0) {
                throw new IllegalArgumentException("Pet requires valid weight");
            }
        }
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        //returns number of rows affected
        //return database.update(PetEntry.TABLE_NAME, values, selection, selectionArgs);
        //Perform the update on db and get num rows affedted
        int rowsUpdated = database.update(PetEntry.TABLE_NAME, values, selection, selectionArgs);
        //if 1 or more rows were updated, notify all listeners the data at given URI has changed
        if(rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

}
