package com.cubContacts.metal_dent.testapp.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.support.v7.app.ActionBar;


import com.cubContacts.metal_dent.testapp.adapters.RecyclerViewAdapter;
import com.cubContacts.metal_dent.testapp.db.ContactContract;
import com.cubContacts.metal_dent.testapp.db.ContactDbHelper;
import com.cubContacts.metal_dent.testapp.models.City;
import com.example.metal_dent.testapp.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private List<City> cityList;
    private ContactDbHelper dbHelper;
    private ProgressBar bar;
    private RecyclerViewAdapter<City> mAdapter;

    private EditText searchText;
    private TextView noMatchText;

    private ActionBar actionBar;

    boolean isForceUpdate = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new GooglePlayStoreAppVersionNameLoader(getApplicationContext(), (WSCallerVersionListener) this).execute();

        recyclerView = findViewById(R.id.recycler_view_main);
        cityList = new ArrayList<>();
        dbHelper = new ContactDbHelper(this);
        bar = findViewById(R.id.progress_bar_main);
        noMatchText = findViewById(R.id.no_match_main);

        searchText = (EditText) findViewById(R.id.editSearchText);
        searchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                    filterList(editable.toString().trim());
            }
        });

        bar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        noMatchText.setVisibility(View.GONE);

        final Handler handler = new Handler();
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    dbHelper.createDatabase();
                    dbHelper.openDatabase();

                    getCityList();
                    setupRecyclerView(cityList);

                    dbHelper.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (SQLiteException e) {
                    e.printStackTrace();
                }
            }
        };
        handler.postDelayed(r, 100);
    }

    private void filterList(String text) {
        List<City> filteredCityList = new ArrayList<>();
        for(City city: cityList) {
            if(city.getName().toLowerCase().contains(text.toLowerCase())) {
                filteredCityList.add(city);
            }
        }
        mAdapter.addFilteredList(filteredCityList);

        if(filteredCityList.isEmpty()) {
            noMatchText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            noMatchText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void getCityList() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        db.beginTransaction();

        final String query = "SELECT DISTINCT " + ContactContract.ContactEntry.COL_CITY
                + " FROM " + ContactContract.ContactEntry.TABLE_NAME
                + " ORDER BY " + ContactContract.ContactEntry.COL_CITY;

        Cursor cursor = db.rawQuery(query, null);

        while(cursor != null && cursor.moveToNext()) {
            City city = new City();
            city.setName(cursor.getString(0));

            cityList.add(city);
        }

        Log.d("db", "Fetching City data...");

        if(cursor != null) {
            cursor.close();
        }

        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }

    private void setupRecyclerView(List<City> cityList) {
        bar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

        class CityViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            LinearLayout parentLayout;

            public CityViewHolder(View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.name);
                parentLayout = itemView.findViewById(R.id.model_layout_city);
            }
        }

        mAdapter = new RecyclerViewAdapter<City>(this, cityList) {
            @Override
            public RecyclerView.ViewHolder setViewHolder(ViewGroup parent) {
                final View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.model_city, parent, false);
                CityViewHolder viewHolder = new CityViewHolder(view);
                return viewHolder;
            }

            @Override
            public void onBindData(RecyclerView.ViewHolder holder1, City model) {
                CityViewHolder holder = (CityViewHolder) holder1;
                holder.name.setText(model.getName());

                holder.parentLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        TextView textView = view.findViewById(R.id.name);

                        Intent intent = new Intent(MainActivity.this, SubActivity.class);
                        intent.putExtra("cityName", textView.getText().toString());

                        MainActivity.this.startActivity(intent);
                    }
                });
            }
        };

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        //finish();
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }
}
