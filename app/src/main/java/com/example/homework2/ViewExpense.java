package com.example.homework2;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.DecimalFormat;

public class ViewExpense extends AppCompatActivity {

    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageReference = storage.getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_expense);

        //Used for formatting prices
        DecimalFormat decimalFormat = new DecimalFormat("#.00");

        Expense expense = (Expense) getIntent().getSerializableExtra(MainActivity.EXPENSE_KEY);
        StorageReference imageRef = storageReference.child(expense.getUniqueID());

        TextView name = findViewById(R.id.viewExp_name);
        TextView cost = findViewById(R.id.viewExp_formattedCost);
        TextView date = findViewById(R.id.viewExp_date);
        TextView id = findViewById(R.id.viewExp_id);
        ImageView image = findViewById(R.id.viewExp_imageView);

        name.setText(expense.getName());
        date.setText(expense.getDate());
        cost.setText(expense.getCostAsString());
        id.setText("Expense ID: " + expense.getUniqueID());
        try {
            Glide.with(this)
                    .using(new FirebaseImageLoader())
                    .load(imageRef)
                    .into(image);
        } catch (Error e) {}
    }
}
