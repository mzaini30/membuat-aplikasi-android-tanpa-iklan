package com.mzaini30.percobaan;

import android.content.res.AssetFileDescriptor;

import androidx.appcompat.app.AppCompatActivity;
import android.media.MediaPlayer;
import android.webkit.JavascriptInterface;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.net.Uri;
import android.webkit.SslErrorHandler;
import android.net.http.SslError;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;
import android.content.ContentValues;

public class MainActivity extends AppCompatActivity {
  private WebView webView;
  private AudioInterface audioInterface;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    webView = (WebView) findViewById(R.id.web);
    audioInterface = new AudioInterface();
    webView.addJavascriptInterface(audioInterface, "AudioInterface");
    webView.addJavascriptInterface(new LocalStorageInterface(this), "localStorage");

    WebSettings webSettings = webView.getSettings();
    webSettings.setJavaScriptEnabled(true);
    webSettings.setDatabaseEnabled(true);
    String databasePath = this.getApplicationContext().getDir("database", Context.MODE_PRIVATE).getPath();
    webSettings.setDatabasePath(databasePath);
    webSettings.setDomStorageEnabled(true);
    webSettings.setAllowFileAccess(true);
    webSettings.setAllowFileAccessFromFileURLs(true);
    webSettings.setAllowUniversalAccessFromFileURLs(true);

    webView.setWebViewClient(new WebViewClient() {
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        // return false;
        if (url.startsWith("file:///android_asset/") || url.startsWith("https://fonts.googleapis.com")
            || url.startsWith("https://fonts.gstatic.com")) {
          return false;
        } else {
          Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
          startActivity(i);
          return true;
        }
      }

      @Override
      public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        handler.proceed(); // Ignore SSL certificate errors
      }
    });

    webView.loadUrl("file:///android_asset/index.html");
  }


  public class AudioInterface {
    private List<MediaPlayer> mediaPlayers = new ArrayList<>();

    @JavascriptInterface
    public void playAudioLoop(String fileMp3) {
      try {
        AssetFileDescriptor afd = getAssets().openFd(fileMp3);
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
        mediaPlayer.setLooping(true);
        mediaPlayer.prepare();
        mediaPlayer.start();
        mediaPlayers.add(mediaPlayer);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    @JavascriptInterface
    public void stopAllAudio() {
      for (MediaPlayer mediaPlayer : mediaPlayers) {
        if (mediaPlayer.isPlaying()) {
          mediaPlayer.stop();
          mediaPlayer.reset();
          mediaPlayer.release();
        }
      }
      mediaPlayers.clear();
    }

  }

  public class LocalStorageInterface {
    private LocalStorageDatabaseHelper dbHelper;

    public LocalStorageInterface(Context context) {
      dbHelper = new LocalStorageDatabaseHelper(context);
    }

    @JavascriptInterface
    public void setItem(String key, String value) {
      dbHelper.setItem(key, value);
    }

    @JavascriptInterface
    public String getItem(String key) {
      return dbHelper.getItem(key);
    }

    @JavascriptInterface
    public void removeItem(String key) {
      dbHelper.removeItem(key);
    }
  }

  public class LocalStorageDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "localstorage.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "localstorage";
    private static final String COLUMN_KEY = "key";
    private static final String COLUMN_VALUE = "value";

    public LocalStorageDatabaseHelper(Context context) {
      super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
          COLUMN_KEY + " TEXT PRIMARY KEY, " +
          COLUMN_VALUE + " TEXT)";
      db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
      onCreate(db);
    }

    public void setItem(String key, String value) {
      SQLiteDatabase db = this.getWritableDatabase();
      ContentValues contentValues = new ContentValues();
      contentValues.put(COLUMN_KEY, key);
      contentValues.put(COLUMN_VALUE, value);
      db.replace(TABLE_NAME, null, contentValues);
    }

    public String getItem(String key) {
      SQLiteDatabase db = this.getReadableDatabase();
      Cursor cursor = db.query(TABLE_NAME, new String[] { COLUMN_VALUE }, COLUMN_KEY + "=?",
          new String[] { key }, null, null, null);
      if (cursor != null && cursor.moveToFirst()) {
        String value = cursor.getString(cursor.getColumnIndex(COLUMN_VALUE));
        cursor.close();
        return value;
      } else {
        return null;
      }
    }

    public void removeItem(String key) {
      SQLiteDatabase db = this.getWritableDatabase();
      db.delete(TABLE_NAME, COLUMN_KEY + "=?", new String[] { key });
    }
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (event.getAction() == KeyEvent.ACTION_DOWN) {
      switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
          if (webView.canGoBack()) {
            webView.goBack();
          } else {
            finish();
          }
          return true;
      }

    }
    return super.onKeyDown(keyCode, event);
  }
}
