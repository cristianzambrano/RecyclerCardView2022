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
import android.os.Handler;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import uteq.solutions.recyclercardview.Adapter.MensajesAdaptador;
import uteq.solutions.recyclercardview.Helper.GlobalInfo;
import uteq.solutions.recyclercardview.Helper.Utf8StringRequest;
import uteq.solutions.recyclercardview.Models.Mensaje;

public class MainActivity extends AppCompatActivity {
    public RecyclerView recyclerView;


    ArrayList<Mensaje> myList = new ArrayList<>();
    MensajesAdaptador adapatorMensajes;

    public static final Integer RecordAudioRequestCode = 1;
    private SpeechRecognizer speechRecognizer;
    private EditText editText;
    private ImageView micButton;
    private TextView txtEstado;

    TextToSpeech textToSpeech;

    LottieAnimationView logo;


    private RequestQueue requestQueue;
    private Handler handler = new Handler();

    public String ThreadID;
    public String lastMessageID;
    public String Assistant_ID = "asst_dKqNiDGt1ltpNWq0hzIxuYbd";
    public String Instrucitons = "Eres un chatbot que responde preguntas sobre la Carrera de Ingeniería de Software y de la Universidad Técnica Estatal de Quevedo (UTEQ)";
    public String run_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestQueue = Volley.newRequestQueue(this);
        createThread();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            checkPermission();
        }

        editText = findViewById(R.id.text);
        micButton = findViewById(R.id.btrecord);
        micButton.setEnabled(false);
        logo = findViewById(R.id.logoavatar);
        txtEstado = findViewById(R.id.txtEstado);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {         }
            @Override
            public void onBeginningOfSpeech() {
                editText.setText("");
                editText.setHint("Escuchando la pregunta...");
                micButton.setImageResource(R.drawable.baseline_stop_circle_24);
            }

            @Override
            public void onRmsChanged(float v) {        }
            @Override
            public void onBufferReceived(byte[] bytes) {            }

            @Override
            public void onEndOfSpeech() {
                editText.setHint("");
                editText.setText("");
                micButton.setImageResource(R.drawable.ic_mic_black_off);
            }

            @Override
            public void onError(int i) {          }

            @Override
            public void onResults(Bundle bundle) {
                micButton.setImageResource(R.drawable.ic_mic_black_off);
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String texto = data.get(0);
                editText.setHint("");
                editText.setText("");
                if (!texto.equals("")) {
                    myList.add(new Mensaje(texto, "U"));
                    adapatorMensajes.notifyData(myList);
                    recyclerView.scrollToPosition(myList.size() - 1);
                    doQuestion(texto);
                }
            }

            @Override
            public void onPartialResults(Bundle bundle) {           }

            @Override
            public void onEvent(int i, Bundle bundle) {            }
        });

        micButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    speechRecognizer.stopListening();
                }
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    speechRecognizer.startListening(speechRecognizerIntent);
                }
                return false;
            }
        });

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (i != TextToSpeech.ERROR) {
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
                Log.i("TextToSpeech", "On Error");
            }
        });



    }

    public void doQuestion(String Pregunta) {
        createMessage(Pregunta);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        speechRecognizer.destroy();
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RecordAudioRequestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RecordAudioRequestCode && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
        }
    }


    private void createThread() {
        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                GlobalInfo.URL_CreatThread,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResp = new JSONObject(response);
                            ThreadID = jsonResp.getString("id").toString();
                            micButton.setEnabled(true);
                            txtEstado.setText("Thread ID: " + ThreadID);
                            doQuestion("Cuál es el nombre de la coordinadora de la carrera de software");

                        } catch (JSONException e) {
                            txtEstado.setText("Error creating Thread " + e.toString());
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        txtEstado.setText("Error creating Thread " + error.toString());
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return GlobalInfo.getAuthHearders();
            }
        };
        requestQueue.add(stringRequest);
    }


    private void createMessage(String msg) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                GlobalInfo.getUrlCreateMessage(ThreadID),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResp = new JSONObject(response);
                            lastMessageID = jsonResp.getString("id").toString();
                            txtEstado.setText("Message ID: " + lastMessageID);
                            runMessage();
                        } catch (JSONException e) {
                            txtEstado.setText("Error Creating Message: " + e.getMessage());
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                txtEstado.setText("Error Creating Message: " + error.getMessage());
                // Manejar el error
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return GlobalInfo.getAuthHearders();
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                String requestBody = "{" +
                        "      \"role\": \"user\", " +
                        "      \"content\": \"" + msg + "\"}";
                return requestBody.getBytes(StandardCharsets.UTF_8);
            }
        };

        requestQueue.add(stringRequest);
    }


    private void runMessage() {
        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                GlobalInfo.getURLRunMessage(ThreadID),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResp = new JSONObject(response);
                            run_ID = jsonResp.getString("id").toString();
                            txtEstado.setText("RUN ID: " + run_ID);
                            checkRunStatusPeriodically();
                        } catch (JSONException e) {
                            txtEstado.setText("Error on RUN " + e.toString());
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                txtEstado.setText("Error on RUN : " + error.toString());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return GlobalInfo.getAuthHearders();
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                String requestBody = "{ \"assistant_id\": \"" + Assistant_ID + "\"," +
                        "      \"instructions\": \"" + Instrucitons + "\"}";
                return requestBody.getBytes(StandardCharsets.UTF_8);
            }
        };

        requestQueue.add(stringRequest);
    }

    private void checkRunStatusPeriodically() {

        Runnable statusChecker = new Runnable() {
            @Override
            public void run() {
                StringRequest checkStatusRequest = new StringRequest(Request.Method.GET,
                        GlobalInfo.getURLCheckStatus(ThreadID, run_ID),
                        response -> {
                            try {
                                JSONObject jsonResponse = new JSONObject(response);
                                String status = jsonResponse.getString("status");
                                txtEstado.setText(status);
                                if ("completed".equals(status)) {
                                    getFinalMessage();
                                } else {
                                    handler.postDelayed(this, 1000);
                                }
                            } catch (Exception e) {
                                txtEstado.setText("Error getting Status: " + e.toString());
                            }
                        },
                        error -> {
                            txtEstado.setText("Error getting Status: " + error.toString());
                        })
                        {
                            @Override
                            public Map<String, String> getHeaders() throws AuthFailureError {
                                return GlobalInfo.getAuthHearders();
                            }
                        };

                requestQueue.add(checkStatusRequest);
            }
        };

        handler.post(statusChecker);
    }

    private void getFinalMessage() {
        StringRequest getMessageRequest = new Utf8StringRequest(Request.Method.GET,
                GlobalInfo.getURLGetMessage(ThreadID, lastMessageID),
                response -> {
                    //utf8String = new String(response, "UTF-8");
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        JSONArray listaMsg = jsonResponse.getJSONArray("data");
                        JSONObject jsonMsg = listaMsg.getJSONObject(0);
                        JSONArray contentMsgs = jsonMsg.getJSONArray("content");
                        JSONObject contentMsg = contentMsgs.getJSONObject(0);
                        JSONObject contentTextMsg = contentMsg.getJSONObject("text");
                        String resp = contentTextMsg.getString("value").toString();

                        txtEstado.setText("Thread ID: " + ThreadID);
                        myList.add(new Mensaje(resp,"A"));
                        adapatorMensajes.notifyData(myList);
                        recyclerView.scrollToPosition(myList.size()-1);

                        HashMap<String, String> params = new HashMap<String, String>();
                        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"3300");
                        textToSpeech.speak(resp,TextToSpeech.QUEUE_FLUSH, params);

                    } catch (JSONException e) {
                        txtEstado.setText("Error Last Message " + e.toString());

                    }
                },
                error -> {
                    txtEstado.setText("Error Last Message " + error.toString());
                }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return GlobalInfo.getAuthHearders();
            }
        };

        requestQueue.add(getMessageRequest);
    }
}