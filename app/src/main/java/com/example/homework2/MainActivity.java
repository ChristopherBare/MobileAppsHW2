package com.example.homework2;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;

import static java.util.Comparator.comparingDouble;

public class MainActivity extends AppCompatActivity {

    DatabaseReference db_root = FirebaseDatabase.getInstance().getReference();

    final static String EXPENSE_KEY = "EXPENSE";

    private static Context context;
    double total;

    //Used for the RecyclerView
    private RecyclerView recycler;
    private RecyclerView.Adapter expenseAdapter;
    private RecyclerView.LayoutManager layoutManager;

    //Expense ArrayList
    private ArrayList<Expense> expenses;

    @TargetApi(Build.VERSION_CODES.N)
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        //Initialize fields
        expenses = new ArrayList<>();

        //Used for formatting prices
        DecimalFormat decimalFormat = new DecimalFormat("#.00");

        //Setup recycler view to display data
        recycler = (RecyclerView) findViewById(R.id.expense_recycler);
        layoutManager = new LinearLayoutManager(this);
        recycler.setLayoutManager(layoutManager);
        expenseAdapter = new ExpenseAdapter(expenses);
        recycler.setAdapter(expenseAdapter);

        //Get the values
        db_root.child("expenses").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                expenses.clear();
                for (DataSnapshot node : dataSnapshot.getChildren() ) {
                    Expense expense = node.getValue(Expense.class);
                    total += expense.getCost();
                    expenses.add(expense);
                }
                expenseAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });

        //Floating Action Button
        findViewById(R.id.floatingActionButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddExpense.class);
                startActivity(intent);
            }
        });

        //Display the total for the expenses
        TextView totalView = findViewById(R.id.totalView);
        Log.d("demo", "Expenses length: " + expenses.size());
        for (Expense e : expenses) total+=e.getCost();
        totalView.setText("$"+decimalFormat.format(total));


    }//end onCreate

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu,menu);
        return true;
    }

    public static void editExpense(Expense expense) {
        Intent intent = new Intent(context, AddExpense.class);
        intent.putExtra(MainActivity.EXPENSE_KEY, expense);
        context.startActivity(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.option_1:
                expenses.sort(Comparator.comparing(Expense::getDate));
                expenseAdapter.notifyDataSetChanged();
                return true;
            case R.id.option_2:
                expenses.sort(comparingDouble(Expense::getCost));
                expenseAdapter.notifyDataSetChanged();
                return true;
            case R.id.option_3:
                db_root.child("expenses").removeValue();
                expenseAdapter.notifyDataSetChanged();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
