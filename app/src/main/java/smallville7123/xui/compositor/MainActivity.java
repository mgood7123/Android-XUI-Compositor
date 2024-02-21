package smallville7123.xui.compositor;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import java.util.ArrayList;

import XUI.Platforms.Android.EGLView;

public class MainActivity extends AppCompatActivity {

    public static ArrayList<Runnable> OnPauseActions = new ArrayList<>();
    public static ArrayList<Runnable> OnResumeActions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new EGLView(this));
    }

    @Override
    protected void onPause() {
        for (Runnable onPauseAction : OnPauseActions) {
            onPauseAction.run();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        for (Runnable onResumeAction : OnResumeActions) {
            onResumeAction.run();
        }
    }
}