package com.example.fruitveggiescanner;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

public class DetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Details");
        actionBar.setDisplayHomeAsUpEnabled(true);

        ImageView imageView = findViewById(R.id.poster_image);
        //TextView rating_tv = findViewById(R.id.mRating);
        TextView name = findViewById(R.id.name);
        TextView category = findViewById(R.id.category);
        TextView calories_tv = findViewById(R.id.calories_tv);
        TextView nutritional_tv = findViewById(R.id.nutritional_tv);


        Bundle bundle = getIntent().getExtras();

        String name_bundle = bundle.getString("name");
        String category_bundle = bundle.getString("category");
        int calories = bundle.getInt("calories");
        String photo = bundle.getString("photo");
        String nutritional_details = bundle.getString("nutritional_details");

        name.setText(name_bundle);
        category.setText(category_bundle);
        calories_tv.setText("Calories: "+ calories);
        nutritional_tv.setText("Nutritional Details: "+ nutritional_details);
        Glide.with(this).load(photo).into(imageView);


    }
}