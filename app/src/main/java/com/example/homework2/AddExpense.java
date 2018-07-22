package com.example.homework2;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
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

    PickSetup setup = new PickSetup()
            .setPickTypes(EPickType.GALLERY, EPickType.CAMERA);

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
                try {
                    PackageManager pm = getPackageManager();
                    int hasPerm = pm.checkPermission(Manifest.permission.CAMERA, getPackageName());
                    if (hasPerm == PackageManager.PERMISSION_GRANTED) {
                        pickImage();
                    } else {
                        Toast.makeText(AddExpense.this, "Camera Permission Error", Toast.LENGTH_SHORT).show();
                        Log.i(TAG, "onClick: " + hasPerm);
                    }
                } catch (Exception e) {
                    Toast.makeText(AddExpense.this, "Camera Permission error", Toast.LENGTH_SHORT).show();
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
        PickImageDialog.build(setup)
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

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            inputStreamImg = null;
            if (requestCode == PICK_IMAGE_CAMERA) {
                try {
                    Uri selectedImage = data.getData();
                    bitmap = (Bitmap) data.getExtras().get("data");
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, bytes);

                    Log.e("Activity", "Pick from Camera::>>> ");

                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                    destination = new File(Environment.getExternalStorageDirectory() + "/" +
                            getString(R.string.app_name), "IMG_" + timeStamp + ".jpg");
                    FileOutputStream fo;
                    try {
                        destination.createNewFile();
                        fo = new FileOutputStream(destination);
                        fo.write(bytes.toByteArray());
                        fo.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    imgPath = destination.getAbsolutePath();
                    imageView.setImageBitmap(bitmap);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (requestCode == PICK_IMAGE_GALLERY) {
                Uri selectedImage = data.getData();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, bytes);
                    Log.e("Activity", "Pick from Gallery::>>> ");

                    imgPath = getRealPathFromURI(selectedImage);
                    destination = new File(imgPath.toString());
                    imageView.setImageBitmap(bitmap);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public String getRealPathFromURI(Uri contentUri) {
            String[] proj = {MediaStore.Audio.Media.DATA};
            Cursor cursor = managedQuery(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }


    // ADD TO DATABASE
    private void addExpenseToDb(Expense expense) {
        //Save the expense
        db_root.child("expenses").child(expense.getUniqueID()).setValue(expense);

        //Save the image
        StorageReference imageRef = storageReference.child(expense.getUniqueID());
        if (byteData != null) imageRef.putBytes(byteData);

        //Finish the activity
        finish();
    }

    @Override
    public void onPickResult(PickResult r) {
        if (r.getError() == null) {
            //If you want the Uri.
            //Mandatory to refresh image from Uri.
            //getImageView().setImageURI(null);

            //Setting the real returned image.
            //getImageView().setImageURI(r.getUri());

            //If you want the Bitmap.
            imageView.setImageBitmap(r.getBitmap());

            //Image path
            //r.getPath();
        } else {
            //Handle possible errors
            //TODO: do what you have to do with r.getError();
            Toast.makeText(this, r.getError().getMessage(), Toast.LENGTH_LONG).show();
        }
    }

}
