package lucas.vegi.ecopoints;

//import com.google.analytics.tracking.android.EasyTracker;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Intent;
import android.widget.TextView;

public class Splash extends Activity {

    private static int SPLASH_TIME_OUT = 2500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                TextView txt = (TextView) findViewById(R.id.idProgressoSplash);

                for (int i = 0; i < 3; i++){
                    txt.setText(txt.getText().toString() + ". ");
                }
                Intent i = new Intent(getApplicationContext(), Principal.class);
                startActivity(i);
                finish();

            }
        }, SPLASH_TIME_OUT);
    }

    @Override
    protected void onStart() {
        super.onStart();

        //Google Analytics
        //EasyTracker.getInstance(this).activityStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        //Google Analytics
        //EasyTracker.getInstance(this).activityStop(this);  // Add this method.
    }


}
