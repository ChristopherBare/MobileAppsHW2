package com.example.homework2;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class AddExpense extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 001;

    DatabaseReference db_root = FirebaseDatabase.getInstance().getReference();
    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageReference = storage.getReference();

    TextView label, id, date;
    EditText name, cost;
    Button dateButton, imageButton, saveButton;
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
        if (getIntent() != null && getIntent().getExtras().getSerializable(MainActivity.EXPENSE_KEY) != null) {

            // Get the expense being passed in
            expense = (Expense) getIntent().getSerializableExtra(MainActivity.EXPENSE_KEY);
            StorageReference imageRef = storageReference.child(expense.getUniqueID());

            // Set the views
            label.setText(R.string.label_editExpense);
            id.setText(expense.getUniqueID());
            name.setText(expense.getName());
            cost.setText(expense.getCostAsString());
            date.setText(expense.getDate());
            date.setVisibility(View.VISIBLE);
            saveButton.setText(R.string.label_saveExpense);

            try {
                Glide.with(this)
                        .using(new FirebaseImageLoader())
                        .load(imageRef)
                        .into(imageView);
            } catch (Error e) { }
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
                        date.setText((month + 1) + "/" + dayOfMonth + "/" + year);
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

                if (name.getText() != null && cost.getText() != null && date != null) {
                    //Initialize a new expense
                    expense = new Expense();

                    //Set the expense fields
                    if (!id.getText().toString().equals("I.D."))
                        expense.setUniqueID(id.getText().toString());
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
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    // Get result from getting photo
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            Bundle extras = data.getExtras();
            Bitmap bitmap = (Bitmap) extras.get("data");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

            byteData = baos.toByteArray();
            imageView.setImageBitmap(bitmap);

        }
    }

    // ADD TO DATABASE
    private void addExpenseToDb(Expense expense) {
        //Save the expense
        db_root.child("expenses").child(expense.getUniqueID()).setValue(expense);

        //Save the image
        StorageReference imageRef = storageReference.child(expense.getUniqueID());
        if (byteData != null) {
            imageRef.putBytes(byteData);
        }

        //Finish the activity
        finish();
    }

}
