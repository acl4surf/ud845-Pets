/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.pets;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.android.pets.data.PetContract.PetEntry;
import com.example.android.pets.data.PetDbHelper;

/**
 * Allows user to create a new pet or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int EXISTING_PET_LOADER = 0;
    /**
     * Content URI for existing pets, null for new pets
     */
    private Uri mCurrentPetUri;
    /**
     * EditText field to enter the pet's name
     */
    private EditText mNameEditText;
    /**
     * EditText field to enter the pet's breed
     */
    private EditText mBreedEditText;
    /**
     * EditText field to enter the pet's weight
     */
    private EditText mWeightEditText;
    /**
     * EditText field to enter the pet's gender
     */
    private Spinner mGenderSpinner;
    /**
     * Gender of the pet. The possible values are:
     * 0 for unknown gender, 1 for male, 2 for female.
     */
    private int mGender = PetEntry.GENDER_UNKNOWN;
    /**
     * Database helper that will provide us access to the database
     */
    private PetDbHelper mDbHelper;
    /**
     * Boolean flag, keeps track of if the pet was edited or not
     */
    private boolean mPetHasChanged = false;
    /**
     * OnTouchListener, listens for any user touches on a View
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mPetHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        //examine the intent that launched thia activity: creating new pet or editing
        Intent intent = getIntent();
        mCurrentPetUri = intent.getData();
        // Set appropriate title ie. if intent does not contain  URI, a new pet
        if (mCurrentPetUri == null) {
            setTitle(getString(R.string.editor_activity_title_new_pet));
            //Invalidate options menu so OnPreparedOtions Menu can be called on v3.0+
            invalidateOptionsMenu();
        } else {
            setTitle(getString(R.string.editor_activity_title_edit_pet));
            //Initialize a loader to read the pet data from db & display it
            getLoaderManager().initLoader(EXISTING_PET_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_pet_name);
        mBreedEditText = (EditText) findViewById(R.id.edit_pet_breed);
        mWeightEditText = (EditText) findViewById(R.id.edit_pet_weight);
        mGenderSpinner = (Spinner) findViewById(R.id.spinner_gender);
        //Set up OnTouchLIstener, to know of unsaved changed before user navs away
        mNameEditText.setOnTouchListener(mTouchListener);
        mBreedEditText.setOnTouchListener(mTouchListener);
        mWeightEditText.setOnTouchListener(mTouchListener);
        mGenderSpinner.setOnTouchListener(mTouchListener);

        setupSpinner();
    }

    /**
     * Setup the dropdown spinner that allows the user to select the gender of the pet.
     */
    private void setupSpinner() {
        // Create adapter for spinner. The list options are from the String array
        // the spinner will use the default layout
        ArrayAdapter genderSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_gender_options, android.R.layout.simple_spinner_item);

        // Specify dropdown layout style - simple list view with 1 item per line
        genderSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        // Apply the adapter to the spinner
        mGenderSpinner.setAdapter(genderSpinnerAdapter);
        // alt. code --> mGenderSpinner.setOnItemSelectedListener(EditorActivity.this);
        // Set the integer mSelected to the constant values
        mGenderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) parent.getItemAtPosition(position);
                if (!TextUtils.isEmpty(selection)) {
                    if (selection.equals(getString(R.string.gender_male))) {
                        mGender = PetEntry.GENDER_MALE; // Male
                    } else if (selection.equals(getString(R.string.gender_female))) {
                        mGender = PetEntry.GENDER_FEMALE; // Female
                    } else {
                        mGender = PetEntry.GENDER_UNKNOWN; // Unknown
                    }
                }
            }

            // Because AdapterView is an abstract class, onNothingSelected must be defined
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mGender = PetEntry.GENDER_UNKNOWN; // Unknown
            }
        });
    }

    //Save user input pet data into db
    private void savePet() {
        //read from editText input field then trim white space
        String nameString = mNameEditText.getText().toString().trim();
        String breedString = mBreedEditText.getText().toString().trim();
        String weightString = mWeightEditText.getText().toString().trim();
        //int weight = Integer.parseInt(weightString);
        //check if a new pet
        if (mCurrentPetUri == null && TextUtils.isEmpty(nameString) && TextUtils.isEmpty(breedString)
                && TextUtils.isEmpty(weightString) && mGender == PetEntry.GENDER_UNKNOWN) {
            return;
        }
        //Create ContentValues in key-value pairs to insert into db
        ContentValues values = new ContentValues();
        values.put(PetEntry.COLUMN_PET_NAME, nameString);
        values.put(PetEntry.COLUMN_PET_BREED, breedString);
        values.put(PetEntry.COLUMN_PET_GENDER, mGender);
        // if weight is empty don't parse use 0
        int weight = 0;
        if (!TextUtils.isEmpty(weightString)) {
            weight = Integer.parseInt(weightString);
        }
        values.put(PetEntry.COLUMN_PET_WEIGHT, weight);

        // Determine if a new pet
        if (mCurrentPetUri == null) {
            //Insert a new pet into the provider, returning content URI for the new pet
            Uri newUri = getContentResolver().insert(PetEntry.CONTENT_URI, values);
            //show a toast about insertion status
            if (newUri == null) {
                Toast.makeText(this, getString(R.string.editor_insert_pet_failed),
                        Toast.LENGTH_LONG).show();
            } else {
                //String rowId = newUri.getLastPathSegment();
                Toast.makeText(this, getString(R.string.editor_insert_pet_successful),
                        Toast.LENGTH_LONG).show();
            }
        } else {
            // Existing pet case: so update the pet using content URI mCurrentPetUri,
            // pass in new ContentValues. pass in null for selection, args, since
            // mCurrentUri will already identify the correct row to modify
            int rowsAffected = getContentResolver().update(mCurrentPetUri, values, null, null);
            //show status of update
            if (rowsAffected == 0) {
                //error w update
                Toast.makeText(this, getString(R.string.editor_update_pet_failed),
                        Toast.LENGTH_LONG).show();
            } else {
                // ok updated
                Toast.makeText(this, getString(R.string.editor_update_pet_successful),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if(mCurrentPetUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // call insert pet method
                savePet();
                //Exit activity
                finish();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Pop up confirm. dialog for deletion
                showDeleteConfirmDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                //  If pet data not changed, Navigate back to parent activity (CatalogActivity)
                if (!mPetHasChanged) {
                    NavUtils.navigateUpFromSameTask(this);
                return true;
                }
                //Else if unsaved changes, set up a dialog to warn user
                //clickListener confirming changes to be discarded
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // User clicked discard nav to parent activity
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };
                // Show dialog notif of unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDeleteConfirmDialog() {
        //Create AlertDialog.builder, set msg, clickListeners for +&- buttons
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //user clicked delete
                deletePet();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //user clicked cancel
                if(dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        //Create and show AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

    }

    private void deletePet() {
        //check if existing pet
        if(mCurrentPetUri != null) {
            //call ContentResolver to delete the pet at given content URI
            //selection args not needed
            int rowsDeleted = getContentResolver().delete(mCurrentPetUri, null, null);
            //show toast on deletion status
            if(rowsDeleted ==0) {
                //no rows deleted, error
                Toast.makeText(this, getString(R.string.editor_delete_pet_failed),
                        Toast.LENGTH_LONG).show();
            } else {
                // successfully deleted row
                Toast.makeText(this, getString(R.string.editor_delete_pet_successful),
                        Toast.LENGTH_LONG).show();
            }
        }
        //close the activity
        finish();
    }

    @Override
    public void onBackPressed() {
        if(!mPetHasChanged) {
            super.onBackPressed();
            return;
        }
        //Else if unsaved changes, set up dialog
        DialogInterface.OnClickListener discardButtonListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //user clicked "Discard" close current activity
                finish();
            }
        };
        // Show dialog of unsaved changes
        showUnsavedChangesDialog(discardButtonListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        //projection, columns to get from pets table
        String[] projection = {PetEntry._ID, PetEntry.COLUMN_PET_NAME, PetEntry.COLUMN_PET_BREED,
                PetEntry.COLUMN_PET_GENDER, PetEntry.COLUMN_PET_WEIGHT};
        //loader to execute ContentProvider's query method on a background thread
        return new CursorLoader(this, mCurrentPetUri, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // exit if cursor is null or -1 row in cursor
        if (cursor == null || cursor.getCount() < 1) return;
        //move to first row and read data from cursor only one row for current pet item
        if (cursor.moveToFirst()) {
            int nameColumnIndex = cursor.getColumnIndex(PetEntry.COLUMN_PET_NAME);
            int breedColumnIndex = cursor.getColumnIndex(PetEntry.COLUMN_PET_BREED);
            int genderColumnIndex = cursor.getColumnIndex(PetEntry.COLUMN_PET_GENDER);
            int weightColumnIndex = cursor.getColumnIndex(PetEntry.COLUMN_PET_WEIGHT);
            //Extract values
            String name = cursor.getString(nameColumnIndex);
            String breed = cursor.getString(breedColumnIndex);
            int gender = cursor.getInt(genderColumnIndex);
            int weight = cursor.getInt(weightColumnIndex);
            // Update views w. values
            mNameEditText.setText(name);
            mBreedEditText.setText(breed);
            mWeightEditText.setText(Integer.toString(weight));
            switch (gender) {
                case PetEntry.GENDER_MALE:
                    mGenderSpinner.setSelection(1);
                    break;
                case PetEntry.GENDER_FEMALE:
                    mGenderSpinner.setSelection(2);
                    break;
                default:
                    mGenderSpinner.setSelection(0);
                    break;
            }

        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //loader is invalid, clear all data from input fields'm
        mNameEditText.setText("");
        mBreedEditText.setText("");
        mWeightEditText.setText("");
        mGenderSpinner.setSelection(0);
    }
    /**
     * shows a dialog warning of unsaved msg
     * param discardButtonListener is to do when user confirm disgard
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //user clicked "keep Editing"
                if(dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        //Create and show AlertDialog
        AlertDialog alertdialog = builder.create();
        alertdialog.show();

    }
}