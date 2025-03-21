package com.example.languagetranslator;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private EditText inputText;
    private TextView translatedText;
    private Spinner sourceLanguageSpinner, targetLanguageSpinner;
    private Button translateButton;
    private ImageView mic,speakout;
    private TextToSpeech textToSpeech;
    private String selectedLanguage = "en-US"; // Default English


    private final String API_KEY = "AIzaSyD20wuS7NXqpOnOXkV0M5WfLOcHktuOdTU"; // Replace with your API key
    private final String API_URL = "https://translation.googleapis.com/language/translate/v2";
    private String text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputText = findViewById(R.id.inputText);
        translatedText = findViewById(R.id.translatedText);
        sourceLanguageSpinner = findViewById(R.id.sourceLanguageSpinner);
        targetLanguageSpinner = findViewById(R.id.targetLanguageSpinner);
        translateButton = findViewById(R.id.translateButton);
        mic = findViewById(R.id.microphone); //micbutton
        speakout = findViewById(R.id.speakerid); //speakerbutton


        // Language Options
        String[] languages = {"English", "Telugu","Kannada","Hindi", "French", "German", "Spanish"};
        String[] languageCodes = {"en", "te-IN","kn-IN","hi", "fr", "de", "es"};

        // Set up spinners
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, languages);
        sourceLanguageSpinner.setAdapter(adapter);
        sourceLanguageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedLanguage = languageCodes[position];

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        targetLanguageSpinner.setAdapter(adapter);

        mic.setOnClickListener(v -> startSpeechToText(selectedLanguage));


        translateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                translateText(languageCodes);
            }
        });

        // Initialize Text-to-Speech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.getDefault());
            }
        });

        speakout.setOnClickListener(v -> speakTranslation(selectedLanguage));
    }
    private void startSpeechToText(String languageCode) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode);

        try {
            startActivityForResult(intent, 1);
        } catch (Exception e) {
            Toast.makeText(this, "Speech recognition not supported", Toast.LENGTH_SHORT).show();
        }
    }

    private void translateText(String[] languageCodes) {
        String textToTranslate = inputText.getText().toString().trim();
        int sourceIndex = sourceLanguageSpinner.getSelectedItemPosition();
        int targetIndex = targetLanguageSpinner.getSelectedItemPosition();
        String sourceLang = languageCodes[sourceIndex];
        String targetLang = languageCodes[targetIndex];

        if (textToTranslate.isEmpty()) {
            Toast.makeText(this, "Please enter text", Toast.LENGTH_SHORT).show();
            return;
        }


        String url = API_URL + "?q=" + textToTranslate + "&source=" + sourceLang + "&target=" + targetLang + "&key=" + API_KEY;

        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONArray translations = jsonResponse.getJSONObject("data").getJSONArray("translations");
                    String translatedTextResult = translations.getJSONObject(0).getString("translatedText");

                    translatedText.setText(translatedTextResult);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Translation error", Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "API Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        queue.add(request);
    }

    private void speakTranslation(String langCode) {
        String text = translatedText.getText().toString();
//        if (!text.isEmpty()) {
//            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
//        }
        if (textToSpeech.isLanguageAvailable(new Locale(langCode)) == TextToSpeech.LANG_AVAILABLE) {
            textToSpeech.setLanguage(new Locale(langCode));
        } else {
            Toast.makeText(this, "Selected language not supported for TTS", Toast.LENGTH_SHORT).show();
            return;
        }

        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                inputText.setText(result.get(0));  // Set recognized text to EditText
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
