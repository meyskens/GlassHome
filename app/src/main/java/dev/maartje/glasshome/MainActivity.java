package dev.maartje.glasshome;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;
import android.content.SharedPreferences;
import com.google.android.glass.view.WindowUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.json.JSONException;
import org.json.JSONObject;

import dev.maartje.glasshome.qrscan.CaptureActivity;

public class MainActivity extends Activity {
    private static final int SPEECH_REQUEST = 0;
    private static final int QR_CODE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);
        getWindow().requestFeature(Window.FEATURE_OPTIONS_PANEL);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == SPEECH_REQUEST && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            callHomeAssistant(results.get(0));
        }
        if (requestCode == QR_CODE_REQUEST && resultCode == RESULT_OK) {
            String contents = data.getStringExtra("qr_data");

            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(contents);
                String url = jsonObject.getString("url");
                String token = jsonObject.getString("token");

                if (url == null || token == null) {
                    TextView textView = (TextView) findViewById(R.id.textView);
                    textView.setText(R.string.invalid_qr_code);
                    return;
                }

                SharedPreferences preferences = getSharedPreferences("HA", MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("url", url);
                editor.putString("token", token);
                editor.apply();
            } catch (JSONException e) {
                TextView textView = (TextView) findViewById(R.id.textView);
                textView.setText(e.toString());
            }

            TextView textView = (TextView) findViewById(R.id.textView);
            textView.setText(R.string.logged_in);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (getIntent().getExtras() == null) {
            return;
        }

        ArrayList<String> voiceResults = Objects.requireNonNull(getIntent().getExtras())
                .getStringArrayList(RecognizerIntent.EXTRA_RESULTS);
        if (voiceResults != null) {
            String query = voiceResults.get(0);
            callHomeAssistant(query);

            getIntent().removeExtra(RecognizerIntent.EXTRA_RESULTS);
        }
    }

    private void callHomeAssistant(String query) {
        new PostTask().execute(query);

        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText(R.string.processing);
    }

    private class PostTask extends AsyncTask<String, Void, String> {
        @SuppressLint("WrongThread")
        @Override
        protected String doInBackground(String... params) {
            String query = params[0];
            try {
                SharedPreferences preferences = getSharedPreferences("HA", MODE_PRIVATE);
                String urlStr = preferences.getString("url", null);
                String token = preferences.getString("token", null);

                if (urlStr == null || token == null) {
                    return "Please sign in first!";
                }

                URL url = new URL(urlStr.replaceAll("/$", "").concat("/api/conversation/process"));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);

                JSONObject jsonInput = new JSONObject();
                jsonInput.put("text", query);
                jsonInput.put("language", "en");

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInput.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    return jsonResponse.getJSONObject("response")
                            .getJSONObject("speech")
                            .getJSONObject("plain")
                            .getString("speech");
                }
            } catch (Exception e) {
                TextView textView = (TextView) findViewById(R.id.textView);
                textView.setText(e.toString());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            TextView textView = (TextView) findViewById(R.id.textView);
            if (result != null) {
                textView.setText(result);
            }
        }
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS ||
                featureId == Window.FEATURE_OPTIONS_PANEL) {
            getMenuInflater().inflate(R.menu.glasshome, menu);
            return true;
        }
        // Pass through to super to setup touch menu.
        return super.onCreatePanelMenu(featureId, menu);
        // Pass through to super to setup touch menu.
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.glasshome, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS ||
                featureId == Window.FEATURE_OPTIONS_PANEL) {
            switch (item.getItemId()) {
                case R.id.stop_this:
                    // stop the current activity
                    finish();
                    break;
                case R.id.control_home:
                    Intent voiceIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    voiceIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "What do you want to do?");
                    startActivityForResult(voiceIntent, SPEECH_REQUEST);
                    break;
                case R.id.sign_in:
                    // open CaptureActivity
                    Intent intent = new Intent(this, CaptureActivity.class);
                    startActivityForResult(intent, QR_CODE_REQUEST);

                    break;
                default:
                    return true;
            }
            return true;
        }
        // Good practice to pass through to super if not handled
        return super.onMenuItemSelected(featureId, item);
    }

}