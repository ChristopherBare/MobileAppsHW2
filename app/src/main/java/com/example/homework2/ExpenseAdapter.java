package com.example.homework2;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ViewHolder> {
    private ArrayList<Expense> mDataset;

    // Provide a suitable constructor (depends on the kind of dataset)
    public ExpenseAdapter(ArrayList<Expense> myDataset) {
        mDataset = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.expense_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        final Expense expense = mDataset.get(position);
        holder.expense_name.setText(expense.getName());
        holder.expense_fCost.setText(expense.getCostAsString());
        holder.expense_rCost.setText(Double.toString(expense.getCost()));
        holder.expense_date.setText(expense.getDate());
        holder.expense_id.setText(expense.getUniqueID());
        holder.expense_id.setVisibility(View.GONE);

        holder.expense_editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.editExpense(expense);
            }
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        TextView expense_id, expense_name, expense_fCost, expense_rCost, expense_date;
        ImageView expense_editButton;

        public ViewHolder(View view) {
            super(view);
            expense_id = view.findViewById(R.id.expenseItem_id);
            expense_name = view.findViewById(R.id.expenseItem_name);
            expense_fCost = view.findViewById(R.id.expenseItem_formattedCost);
            expense_rCost = view.findViewById(R.id.expenseItem_rawCost);
            expense_rCost.setVisibility(View.GONE);
            expense_date = view.findViewById(R.id.expenseItem_date);
            expense_editButton = view.findViewById(R.id.expenseItem_editButton);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DecimalFormat decimalFormat = new DecimalFormat("#.00");
                    Number number = 0;
                    Expense expense = new Expense();
                    expense.setUniqueID(expense_id.getText().toString());
                    expense.setName(expense_name.getText().toString());
                    expense.setCost(Double.parseDouble(expense_rCost.getText().toString()));
                    expense.setDate(expense_date.getText().toString());
                    MainActivity.viewExpense(expense);
                }
            });
        }
    }
}
