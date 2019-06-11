package com.example.chatbot_dialogflow;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import ai.api.AIListener;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.model.AIError;
import ai.api.model.AIResponse;
import ai.api.model.Result;

public class MainActivity extends AppCompatActivity implements AIListener {

    private final int REQ_CODE_SPEECH_INPUT = 100;
    TextView userText, botText;
    Button speakBtn;

    AIService aiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userText = (TextView)findViewById(R.id.userText);
        botText = (TextView)findViewById(R.id.botText);
        speakBtn = (Button)findViewById(R.id.speakBtn);


//        Dynamic Permissions
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            makeRequest();
        }

//        Api.ai configuration
        final AIConfiguration config = new AIConfiguration("56e9dfec4abf4cb98306456745c0bfcf",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);
        aiService = AIService.getService(this, config);
        aiService.setListener(this);

        speakBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptSpeechInput();
            }
        });

    }

    private void promptSpeechInput() {

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something!");

        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getApplicationContext(), "Sorry! You device doesn't support speech input", Toast.LENGTH_SHORT).show();
        }
    }

    //    For Dynamic Permissions
    protected void makeRequest() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO}, 101);
    }
    //    For Dynamic Permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode) {
            case 101: {
                if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {

                } else {

                }
                return;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String userQuery = result.get(0);
                    userText.setText(userQuery);

                    RetrieveFeedTask task = new RetrieveFeedTask();
                    task.execute(userQuery);
                }
                break;
            }
        }
    }

    public String textGet(String query) throws UnsupportedEncodingException {


        String text = "";
        BufferedReader reader = null;

        try {

//            Send data to this url
            URL url = new URL("https://api.dialogflow.com/v1/query?v=20150910");

//            Send POST data request
            URLConnection conn = url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);

            conn.setRequestProperty("Authorization", "Bearer 56e9dfec4abf4cb98306456745c0bfcf");
            conn.setRequestProperty("Content-Type", "application/json");

//            Create JSON object
            JSONObject jsonParam = new JSONObject();
            JSONArray queryArray = new JSONArray();
            queryArray.put(query);
            jsonParam.put("query", queryArray);
            jsonParam.put("lang", "en");
            jsonParam.put("sessionId", "1234567890");

            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(jsonParam.toString());
            wr.flush();

//            Get the server response
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder ab = new StringBuilder();
            String line = null;

//            Read server response
            while ((line = reader.readLine()) != null) {
                ab.append(line+"\n");
            }

            text = ab.toString();
            JSONObject object1 = new JSONObject(text);
            JSONObject object = object1.getJSONObject("result");
            JSONObject fulfillment = null;
            String speech = null;
            fulfillment = object.getJSONObject("fulfillment");
            speech = fulfillment.optString("speech");

            return speech;

        } catch (Exception e){
        } finally {
            try {
                reader.close();
            } catch (Exception e1) {}
        }
        return null;
    }

    class RetrieveFeedTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            String s = null;
            try {
                s = textGet(strings[0]);
            } catch (UnsupportedEncodingException e2) {
                e2.printStackTrace();
            }
            return s;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            botText.setText(s);
        }
    }



    //    All the Api.ai related Methods
    @Override
    public void onResult(AIResponse result) {
        Result result1 = result.getResult();
        userText.setText(result1.getResolvedQuery());
        botText.setText(result1.getAction());
    }

    @Override
    public void onError(AIError error) {

    }

    @Override
    public void onAudioLevel(float level) {

    }

    @Override
    public void onListeningStarted() {

    }

    @Override
    public void onListeningCanceled() {

    }

    @Override
    public void onListeningFinished() {

    }
}