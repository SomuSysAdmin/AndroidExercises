package com.example.somu.a078downimage;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    ImageView downloadedImg;

    public class ImageDownload extends AsyncTask<String, Void, Bitmap>
    {
        @Override
        protected Bitmap doInBackground(String... urls) {
            try {
                URL imgSrc = new URL(urls[0]);
                HttpURLConnection httpsCon = (HttpURLConnection) imgSrc.openConnection();
                httpsCon.connect();

                InputStream inputStream = httpsCon.getInputStream();
                Bitmap img = BitmapFactory.decodeStream(inputStream);

                return img;

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    void downloadImage(View view) {
        // http://www.freepngimg.com/thumb/internet_meme/4-2-yao-ming-meme-png-thumb.png

        ImageDownload task = new ImageDownload();
        try {
            Bitmap myImg = task.execute("http://www.freepngimg.com/thumb/internet_meme/4-2-yao-ming-meme-png-thumb.png").get();
            downloadedImg.setImageBitmap(myImg);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.i("Test", "Boilerplate Junk!");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        downloadedImg = (ImageView) findViewById(R.id.imageView);
    }
}
