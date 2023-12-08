package com.example.fruitveggiescanner;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.fruitveggiescanner.config.Settings;
import com.example.fruitveggiescanner.utils.VolleyMultipartRequest;
import com.example.fruitveggiescanner.utils.VolleySingleton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private RequestQueue requestQueue;
    private ImageView imageSelected;
    private static final int REQUEST_PERMISSIONS = 100;
    private static final int CAMERA_REQUEST = 1888;
    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Home");

        requestQueue = VolleySingleton.getmInstance(this).getRequestQueue();

        imageSelected = findViewById(R.id.imageSelected);
        imageSelected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Check for permissions
                if ((ContextCompat.checkSelfPermission(getApplicationContext(),
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(getApplicationContext(),
                        android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
                    if ((ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) && (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE)) && (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                            Manifest.permission.CAMERA))) {

                    } else {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA},
                                REQUEST_PERMISSIONS);
                    }
                } else {
                    Log.e("Else", "Else");
                    openCamera();
                }
            }
        });
        Button verify = findViewById(R.id.verifyBtn);
        verify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doSubmit();
            }
        });
    }

    private void openCamera() {

        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            bitmap = (Bitmap) data.getExtras().get("data");
            imageSelected.setImageBitmap(bitmap);
        }
    }

    private void doSubmit() {
        if(bitmap == null) {
            Toast.makeText(MainActivity.this, "Tap on the image icon to capture a Fruit or Vegetable image", Toast.LENGTH_LONG).show();
        }
        else {
            //Initializing progress  indicator
            ProgressDialog mDialog = new ProgressDialog(this);
            mDialog.setMessage("Verifying image...");
            mDialog.show();

            String url = Settings.BASE_URL.concat("recognize");
            VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST, url, new Response.Listener<NetworkResponse>() {
                @Override
                public void onResponse(NetworkResponse response) {
                    mDialog.dismiss();
                    // parse success output
                    if (response.statusCode == 200) {
                        Toast.makeText(MainActivity.this, "Image validated successfully", Toast.LENGTH_SHORT).show();
                        String resultResponse = new String(response.data);
                        Log.d(TAG, "onResponse: " + resultResponse);
                        try {
                            JSONObject res = new JSONObject(resultResponse);
                            Log.d(TAG, "onResponse: " + res.getString("name"));
                            Intent intent = new Intent(MainActivity.this, DetailsActivity.class);

                            Bundle bundle = new Bundle();
                            bundle.putString("name", res.getString("name"));
                            bundle.putString("category", res.getString("category"));
                            bundle.putInt("calories", res.getInt("calories"));
                            bundle.putString("photo", res.getString("photo"));
                            bundle.putString("nutritional_details", res.getString("nutrition_details"));

                            intent.putExtras(bundle);

                            MainActivity.this.startActivity(intent);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        //Toast.makeText(MainActivity.this, "Image validated successfully", Toast.LENGTH_SHORT).show();
                        //startActivity(new Intent(MainActivity.this, DetailsActivity.class));
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    mDialog.dismiss();
                    error.printStackTrace();
                    Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();

                    return params;
                }

                @Override
                protected Map<String, DataPart> getByteData() {
                    Map<String, DataPart> params = new HashMap<>();
                    long imagename = System.currentTimeMillis();
                    params.put("image", new DataPart(imagename + ".png", getFileDataFromDrawable(bitmap)));
                    return params;
                }
            };

            multipartRequest.setRetryPolicy(new DefaultRetryPolicy(
                    0,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            this.requestQueue.add(multipartRequest);
        }
    }

    public byte[] getFileDataFromDrawable(Bitmap bitmap) {
        //Log.e("Original   dimensions", bitmap.getWidth()+" "+bitmap.getHeight()+" "+bitmap.getByteCount());
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        //Bitmap decoded = BitmapFactory.decodeStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));

        //Log.e("Compressed dimensions", decoded.getWidth()+" "+decoded.getHeight()+" "+decoded.getByteCount());
        return byteArrayOutputStream.toByteArray();
    }
}