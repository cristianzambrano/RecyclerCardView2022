package uteq.solutions.recyclercardview;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivityIntro extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_intro);
    }

    public void onClickEntrar(View v){
        Intent intent = new Intent(MainActivityIntro.this,
                UIChatbotUTEQ.class);
        startActivity(intent);
    }

    public void onClickEntrar2(View v){
        Intent intent = new Intent(MainActivityIntro.this,
                MainActivity.class);
        startActivity(intent);
    }
}