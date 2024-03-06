package uteq.solutions.recyclercardview;

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
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

import uteq.solutions.recyclercardview.Adapter.MensajesAdaptador;
import uteq.solutions.recyclercardview.Helper.GlobalInfo;
import uteq.solutions.recyclercardview.Helper.Utf8StringRequest;
import uteq.solutions.recyclercardview.Models.Mensaje;

public class UIChatbotUTEQ extends AppCompatActivity {
    public RecyclerView recyclerView;

    String TokenOpenAI;
    ArrayList<Mensaje> myList = new ArrayList<>();
    MensajesAdaptador adapatorMensajes;

    public static final Integer RecordAudioRequestCode = 1;
    private SpeechRecognizer speechRecognizer;
   private ImageView micButton;
   private Button btsend;
    private TextView txtEstado;
    private TextInputEditText txtPregunta;
    private TextInputLayout lyPregunta;
    TextToSpeech textToSpeech;

    private RequestQueue requestQueue;
    private Handler handler = new Handler();
    private Handler hVerificadorSpeaking = new Handler();

    public String ThreadID;
    public String lastMessageID;
    public String run_ID;

    Runnable rVerificadorSpeaking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.uichatbot);

        TokenOpenAI = GlobalInfo.getOpenAIApiKey(this);

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


        btsend = findViewById(R.id.btsend);
        micButton = findViewById(R.id.btrecord);
        micButton.setEnabled(false); btsend.setEnabled(false);
        txtEstado = findViewById(R.id.txtEstado);
        txtPregunta = findViewById(R.id.txtPregunta);
        lyPregunta = findViewById(R.id.txtlyPregunta);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {         }
            @Override
            public void onBeginningOfSpeech() {
                txtPregunta.setText("");
                lyPregunta.setHint("Escuchando..");
                micButton.setImageResource(R.drawable.baseline_stop_circle_24);
            }

            @Override
            public void onRmsChanged(float v) {        }
            @Override
            public void onBufferReceived(byte[] bytes) {            }

            @Override
            public void onEndOfSpeech() {
                lyPregunta.setHint("Ingrese una pregunta");
                micButton.setImageResource(R.drawable.ic_mic_black_off);
            }

            @Override
            public void onError(int i) {          }

            @Override
            public void onResults(Bundle bundle) {
                micButton.setImageResource(R.drawable.ic_mic_black_off);

                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String texto = data.get(0);
                if (!texto.equals("")) {
                    /*myList.add(new Mensaje(texto, "U"));
                    adapatorMensajes.notifyData(myList);
                    recyclerView.scrollToPosition(myList.size() - 1);
                    doQuestion(texto);*/
                    txtPregunta.setText(texto);
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

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Locale locSpanish = new Locale("spa", "MEX");
                textToSpeech.setLanguage(locSpanish);
            }
        });

        requestQueue = Volley.newRequestQueue(this);
        createThread();

        rVerificadorSpeaking = new Runnable() {
            public void run() {
                if (textToSpeech.isSpeaking()) {
                    //if(!sounds.isAnimating()) sounds.playAnimation();
                    hVerificadorSpeaking.postDelayed(this, 100);
                }else{
                    //sounds.resumeAnimation();
                    hVerificadorSpeaking.removeCallbacks(rVerificadorSpeaking);
                    micButton.setEnabled(true); btsend.setEnabled(true);
                }
            }
        };


    }

    public void doQuestion(String Pregunta) {
        micButton.setEnabled(false); btsend.setEnabled(false);
        createMessage(Pregunta);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        speechRecognizer.destroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }

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
                            micButton.setEnabled(true); btsend.setEnabled(true);
                            txtEstado.setText("Chat ID: " + ThreadID);
                            myList.add(new Mensaje("Bienvenido al ChatBot de la UTEQ, " +
                                    "presiona el micrófono para enviar un mensaje","A"));
                            adapatorMensajes.notifyData(myList);
                            recyclerView.scrollToPosition(myList.size()-1);

                        } catch (JSONException e) {
                            txtEstado.setText("Error Creating Thread: " + e.toString());
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        txtEstado.setText("Error CreatingThread " + error.toString());
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return GlobalInfo.getAuthHearders(TokenOpenAI);
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
                            txtEstado.setText("Mensaje ID: " + lastMessageID);
                            runMessage();
                        } catch (JSONException e) {
                            txtEstado.setText("Error CreatingMessage: " + e.getMessage());
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                txtEstado.setText("Error CreatingMessage: " + error.getMessage());
                micButton.setEnabled(true); btsend.setEnabled(true);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return GlobalInfo.getAuthHearders(TokenOpenAI);
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
                            txtEstado.setText("Ejecución ID: " + run_ID);
                            checkRunStatusPeriodically();
                        } catch (JSONException e) {
                            txtEstado.setText("Error onRUN " + e.toString());
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                txtEstado.setText("Error onRUN : " + error.toString());
                micButton.setEnabled(true); btsend.setEnabled(true);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return GlobalInfo.getAuthHearders(TokenOpenAI);
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                String requestBody = "{ \"assistant_id\": \"" + GlobalInfo.Assistant_ID + "\"," +
                        "      \"instructions\": \"" + GlobalInfo.Instructions + "\"}";
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
                                if ("completed".equals(status)) {
                                    getFinalMessage();
                                    txtEstado.setText("Respuesta Lista!");
                                } else {
                                    txtEstado.setText("Estado: " + status);
                                    handler.postDelayed(this, 1000);

                                }
                            } catch (Exception e) {
                                txtEstado.setText("Error GettingStatus: " + e.toString());
                            }
                        },
                        error -> {
                            txtEstado.setText("Error GettingStatus: " + error.toString());
                            micButton.setEnabled(true); btsend.setEnabled(true);
                        })
                        {
                            @Override
                            public Map<String, String> getHeaders() throws AuthFailureError {
                                return GlobalInfo.getAuthHearders(TokenOpenAI);
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
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        JSONArray listaMsg = jsonResponse.getJSONArray("data");
                        JSONObject jsonMsg = listaMsg.getJSONObject(0);
                        JSONArray contentMsgs = jsonMsg.getJSONArray("content");
                        JSONObject contentMsg = contentMsgs.getJSONObject(0);
                        JSONObject contentTextMsg = contentMsg.getJSONObject("text");
                        String resp = contentTextMsg.getString("value").toString();

                        resp = resp.replaceAll("\\【.*?\\】", "");
                        txtEstado.setText("Chat ID: " + ThreadID);
                        myList.add(new Mensaje(resp,"A"));
                        adapatorMensajes.notifyData(myList);
                        recyclerView.scrollToPosition(myList.size()-1);

                        Bundle params = new Bundle();
                        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "3300");
                        textToSpeech.speak(resp, TextToSpeech.QUEUE_FLUSH, params, "3300");
                        hVerificadorSpeaking.postDelayed(rVerificadorSpeaking, 100);

                    } catch (JSONException e) {
                        txtEstado.setText("Error LastMessage: " + e.toString());

                    }
                },
                error -> {
                    txtEstado.setText("Error LastMessage: " + error.toString());
                    micButton.setEnabled(true);btsend.setEnabled(true);
                }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return GlobalInfo.getAuthHearders(TokenOpenAI);
            }
        };

        requestQueue.add(getMessageRequest);
    }

    public void sendPregunta(View v){
        String texto = txtPregunta.getText().toString();
        if(!texto.equals("")){
            myList.add(new Mensaje(texto, "U"));
            adapatorMensajes.notifyData(myList);
            recyclerView.scrollToPosition(myList.size() - 1);
            txtPregunta.setText("");
            doQuestion(texto);
        }
    }

}