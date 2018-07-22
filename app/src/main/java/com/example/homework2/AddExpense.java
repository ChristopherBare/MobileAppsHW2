package com.example.homework2;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

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

public class AddExpense extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 001;
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
        if ((getIntent() != null && getIntent().getExtras().getSerializable(MainActivity.EXPENSE_KEY) != null)) {

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
                dispatchTakePictureIntent();
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
    private void dispatchTakePictureIntent() {
            try {
                    final CharSequence[] options = {"Take Photo", "Choose From Gallery","Cancel"};
                    android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(AddExpense.this);
                    builder.setTitle("Select Option");
                    builder.setItems(options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int item) {
                            if (options[item].equals("Take Photo")) {
                                dialog.dismiss();
                                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                startActivityForResult(intent, PICK_IMAGE_CAMERA);
                            } else if (options[item].equals("Choose From Gallery")) {
                                dialog.dismiss();
                                Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                startActivityForResult(pickPhoto, PICK_IMAGE_GALLERY);
                            } else if (options[item].equals("Cancel")) {
                                dialog.dismiss();
                            }
                        }
                    });
                    builder.show();
            } catch (Exception e) {
                Toast.makeText(this, "Camera Permission error", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
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
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

                    byteData = baos.toByteArray();

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
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

                    byteData = baos.toByteArray();

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

}
