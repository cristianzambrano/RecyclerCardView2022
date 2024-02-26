package uteq.solutions.recyclercardview;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import Adapter.MensajesAdaptador;
import Models.Mensaje;
import WebServices.Asynchtask;
import WebServices.WebService;

public class MainActivity extends AppCompatActivity implements Asynchtask {
    public RecyclerView recyclerView;


    ArrayList<Mensaje> myList = new ArrayList<>();
    MensajesAdaptador adapatorMensajes;

    public static final Integer RecordAudioRequestCode = 1;
    private SpeechRecognizer speechRecognizer;
    private EditText editText;
    private ImageView micButton;

    TextToSpeech textToSpeech;

    LottieAnimationView logo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            checkPermission();
        }

        recyclerView = (RecyclerView) findViewById(R.id.rcLista);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        int resId = R.anim.layout_animation_down_to_up;
        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(getApplicationContext(),
                resId);
        recyclerView.setLayoutAnimation(animation);

        adapatorMensajes = new MensajesAdaptador(getApplicationContext(), myList);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapatorMensajes);

        if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            checkPermission();
        }

        editText = findViewById(R.id.text);
        micButton = findViewById(R.id.btrecord);
        logo = findViewById(R.id.logoavatar);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {    }

            @Override
            public void onBeginningOfSpeech() {
                editText.setText("");
                editText.setHint("Escuchando la pregunta...");
                micButton.setImageResource(R.drawable.baseline_stop_circle_24);
            }

            @Override
            public void onRmsChanged(float v) {    }
            @Override
            public void onBufferReceived(byte[] bytes) {    }
            @Override
            public void onEndOfSpeech() {
                editText.setHint("");
                editText.setText("");
                micButton.setImageResource(R.drawable.ic_mic_black_off);
            }
            @Override
            public void onError(int i) {    }

            @Override
            public void onResults(Bundle bundle) {
                micButton.setImageResource(R.drawable.ic_mic_black_off);
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String texto=data.get(0);
                editText.setHint("");
                editText.setText("");
                if(!texto.equals("")) {
                    myList.add(new Mensaje(texto,"U"));
                    adapatorMensajes.notifyData(myList);
                    recyclerView.scrollToPosition(myList.size()-1);
                    doQuestion(texto);
                }
            }
            @Override
            public void onPartialResults(Bundle bundle) {    }
            @Override
            public void onEvent(int i, Bundle bundle) {    }
        });

        micButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP){
                    speechRecognizer.stopListening();
                }
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                    speechRecognizer.startListening(speechRecognizerIntent);
                }
                return false;
            }
        });

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {

                if(i!=TextToSpeech.ERROR){
                    Locale locSpanish = new Locale("spa", "MEX");
                    textToSpeech.setLanguage(locSpanish);


                }
            }
        });

        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                logo.playAnimation();
            }

            @Override
            public void onDone(String utteranceId) {
                logo.pauseAnimation();
            }

            @Override
            public void onError(String utteranceId) {
                Log.i("TextToSpeech","On Error");
            }
        });




    }

    public void doQuestion(String Pregunta) {

        Map<String, String> datos = new HashMap<String, String>();
        WebService ws= new WebService("http://cristianzambranovega.pythonanywhere.com/query?text=" + Pregunta,
                datos, MainActivity.this, MainActivity.this);
        ws.execute("GET");


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        speechRecognizer.destroy();
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO},RecordAudioRequestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RecordAudioRequestCode && grantResults.length > 0 ){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this,"Permission Granted",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void processFinish(String result) throws JSONException {
        try {
            JSONObject JSONResp =  new JSONObject(result);
            //myList.add(new Mensaje(JSONResp.getString("pregunta"),"U"));
            String resp=JSONResp.getString("respuesta");
            if(resp.startsWith("\n")) resp = resp.substring(1);
            myList.add(new Mensaje(resp,"A"));
            adapatorMensajes.notifyData(myList);
            recyclerView.scrollToPosition(myList.size()-1);

            HashMap<String, String> params = new HashMap<String, String>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"3300");
            textToSpeech.speak(resp,TextToSpeech.QUEUE_FLUSH, params);

        }
        catch (JSONException e)
        {
            Toast.makeText(this.getApplicationContext(),e.getMessage(),Toast.LENGTH_LONG);
        }
    }

}