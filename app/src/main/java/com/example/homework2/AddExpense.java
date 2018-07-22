package com.example.homework2;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.vansuita.pickimage.bean.PickResult;
import com.vansuita.pickimage.bundle.PickSetup;
import com.vansuita.pickimage.dialog.PickImageDialog;
import com.vansuita.pickimage.enums.EPickType;
import com.vansuita.pickimage.listeners.IPickClick;
import com.vansuita.pickimage.listeners.IPickResult;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddExpense extends AppCompatActivity implements IPickResult {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String TAG = "info";
    private final int PICK_IMAGE_CAMERA = 1, PICK_IMAGE_GALLERY = 2;

    private File destination = null;
    private InputStream inputStreamImg;
    private String imgPath = null;

    DatabaseReference db_root = FirebaseDatabase.getInstance().getReference();
    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageReference = storage.getReference();

    TextView label, id, date;
    EditText name, cost;
    Button dateButton, imageButton, saveButton, btnSelectImage;
    ImageView imageView;

    DateFormat dateFormat;
    int year, month, day;

    Expense expense;
    Bitmap bitmap;
    byte[] byteData;
    Uri image;

    PickSetup setup = new PickSetup()
            .setPickTypes(EPickType.GALLERY, EPickType.CAMERA);
    PickImageDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        // Date for datepicker
        dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        Date today = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(today);
        year = cal.get(Calendar.YEAR);
        month = cal.get(Calendar.MONTH);
        day = cal.get(Calendar.DAY_OF_MONTH);

        // Get the views
        label = findViewById(R.id.editExp_label);
        id = findViewById(R.id.editExp_id);
        id.setVisibility(View.GONE);
        name = findViewById(R.id.editExp_editName);
        cost = findViewById(R.id.editExp_editCost);
        date = findViewById(R.id.editExp_dateView);
        date.setVisibility(View.GONE);
        dateButton = findViewById(R.id.editExp_dateButton);
        imageButton = findViewById(R.id.editExp_imageButton);
        saveButton = findViewById(R.id.editExp_save);
        imageView = findViewById(R.id.editExp_imageView);

        // If the user is editing an expense
        if (getIntent() != null && getIntent().getExtras() != null) {

            // Get the expense being passed in
            expense = (Expense) getIntent().getSerializableExtra(MainActivity.EXPENSE_KEY);

            // Set the views
            label.setText(R.string.label_editExpense);
            id.setText(expense.getUniqueID());
            name.setText(expense.getName());
            cost.setText(Double.toString(expense.getCost()));
            date.setText(expense.getDate());
            date.setVisibility(View.VISIBLE);
            saveButton.setText(R.string.label_saveExpense);

            StorageReference imageRef = storageReference.child(expense.getUniqueID());

            try {
                Glide.with(this)
                    .using(new FirebaseImageLoader())
                    .load(imageRef)
                    .into(imageView);
            } catch (Error e) {
                Log.d("demo", "Error loading image: " + e);
            }
        }

        //Selecting a date
        dateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Create a new DatePickerDialog with a nested listener for when the date is selected
                DatePickerDialog dialog = new DatePickerDialog(AddExpense.this, new DatePickerDialog.OnDateSetListener() {

                    //Listener method
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        date.setText((month+1)+"/"+dayOfMonth+"/"+year);
                        date.setVisibility(View.VISIBLE);
                    }

                //Rest of DatePickerDialog setup
                }, year, month, day);

                //Display the DatePickerDialog
                dialog.show();
            }
        });

        //Loading receipt image
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ( ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {
                    ActivityCompat.requestPermissions(AddExpense.this, new String[] {Manifest.permission.CAMERA}, REQUEST_IMAGE_CAPTURE);
                }
                try {
                    PackageManager pm = getPackageManager();
                    int hasPerm = 1;
                    if (hasPerm != PackageManager.PERMISSION_GRANTED) {
                        pickImage();
                    } else {
                        Toast.makeText(AddExpense.this, "Camera Permission Error | else", Toast.LENGTH_SHORT).show();
                        Log.i(TAG, "onClick: " + hasPerm);
                    }
                } catch (Exception e) {
                    Toast.makeText(AddExpense.this, "Camera Permission error | catch", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });

        //Save the expense
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (name.getText()!=null && cost.getText()!=null && date!=null) {
                    //Initialize a new expense
                    expense = new Expense();

                    //Set the expense fields
                    if (!id.getText().toString().equals("I.D.")) expense.setUniqueID(id.getText().toString());
                    expense.setName(name.getText().toString());
                    expense.setCost(Double.parseDouble(cost.getText().toString()));
                    expense.setDate(date.getText().toString());

                    //Add the expense to the database
                    addExpenseToDb(expense);
                } else {
                    Toast.makeText(AddExpense.this, "Name, Cost, and Date required", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // Get photo from camera
    private void pickImage() {
        dialog = PickImageDialog.build(setup)
                .setOnClick(new IPickClick() {
                    @Override
                    public void onGalleryClick() {
                        Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(pickPhoto, PICK_IMAGE_GALLERY);
                    }

                    @Override
                    public void onCameraClick() {
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(intent, PICK_IMAGE_CAMERA);
                    }
                }).show(this);
        }

    // ADD TO DATABASE
    private void addExpenseToDb(Expense expense) {
        //Save the expense
        db_root.child("expenses").child(expense.getUniqueID()).setValue(expense);

        //Save the image
        StorageReference imageRef = storageReference.child(expense.getUniqueID());
        if (image != null) imageRef.putFile(image);

        //Finish the activity
        finish();
    }

    @Override
    public void onPickResult(PickResult r) {
        if (r.getError() == null) {
            //If you want the Uri.
            //Mandatory to refresh image from Uri.
            imageView.setImageURI(null);

            //Setting the real returned image.
            imageView.setImageURI(r.getUri());
            image = r.getUri();

            //If you want the Bitmap.
//            bitmap = r.getBitmap();
//            imageView.setImageBitmap(r.getBitmap());

            dialog.dismiss();

            //Image path
            //r.getPath();
        } else {
            //Handle possible errors
            //TODO: do what you have to do with r.getError();
            Toast.makeText(this, r.getError().getMessage(), Toast.LENGTH_LONG).show();
        }
    }

}
