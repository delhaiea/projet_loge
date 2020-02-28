package fr.insset.projectloge;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.net.URISyntaxException;

import fr.insset.projectloge.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final int GALLERY_REQUEST_CODE = 1;
    private Uri url = null;

    public class PhotoHandler {
        private String TAG = "fdp";

        public void choosePhotoOnClick(View v) {
            pickFromGallery();
            Log.d(TAG, "choosePhotoOnClick: fdp");
        }
    }

    private ActivityMainBinding layout;
    private PhotoHandler ph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        layout = DataBindingUtil.setContentView(this, R.layout.activity_main);
        ph = new PhotoHandler();
        layout.setPhotoHandler(ph);
    }

    @Override
    public void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GALLERY_REQUEST_CODE) {
                Uri selectedImage = data.getData();
                url = selectedImage;
                layout.imageView.setImageURI(selectedImage);
                if(selectedImage != null) {
                    Thread t = null;
                    try {
                        t = new Thread(new UploadRunnable(getPath(this, selectedImage)));
                        t.start();
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }else {
                    Log.d("MainActivity", "onActivityResult: it's null");
                }

            }
        }
    }

    private void pickFromGallery(){
        Intent intent=new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        String[] mimeTypes = {"image/jpeg", "image/png"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES,mimeTypes);
        startActivityForResult(intent,GALLERY_REQUEST_CODE);
    }

    public static String getPath(Context context, Uri uri) throws URISyntaxException {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { "_data" };
            Cursor cursor = null;

            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                assert cursor != null;
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it
            }
        }
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }
}
