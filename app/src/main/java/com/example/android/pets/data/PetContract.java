package com.example.android.pets.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by aei2 on 11/25/2016.
 */

public final class PetContract {
    // private constructor
    private PetContract() {
    }
    /**
     * Content Authority using package name
     */
    public static final String CONTENT_AUTHORITY = "com.example.android.pets";
    /**
     * Base URI for contacting content provider
     */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    /**
     * Path appended to base Content URI for possible URIs ie. content://package/pets
     */
    public static final String PATH_PETS = "pets";
    /**
     * Inner class for constant values for pets db table
     */
    public static final class PetEntry implements BaseColumns {
        /**
         * Content URI to access pet data in the provider
         */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_PETS);
        /**
         * MIME type CONTENT_URI for a list of pets
         */
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE+"/"+CONTENT_AUTHORITY+"/"+PATH_PETS;
        /**
         * MIME type CONTENT_URI for a single pet
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE+"/"+CONTENT_AUTHORITY+"/"+PATH_PETS;
        /**
         * DB table name
         */
        public static final String TABLE_NAME = "pets";
        /**
         * Unique primary ID Type:INTEGER
         */
        public static final String _ID = BaseColumns._ID;
        /**
         * Pet name Type:TEXT
         */
        public static final String COLUMN_PET_NAME = "name";
        /**
         * Pet breed Type:TEXT
         */
        public static final String COLUMN_PET_BREED = "breed";
        /**
         * Pet gender Type:INTEGER
         */
        public static final String COLUMN_PET_GENDER = "gender";
        /**
         * Pet gender values 0,1,2 ie. Unknown, M, F
         */
        public static final int GENDER_UNKNOWN = 0;
        public static final int GENDER_MALE = 1;
        public static final int GENDER_FEMALE = 2;
        /**
         * Pet weight Type:INTEGER
         */
        public static final String COLUMN_PET_WEIGHT = "weight";

    }
}
