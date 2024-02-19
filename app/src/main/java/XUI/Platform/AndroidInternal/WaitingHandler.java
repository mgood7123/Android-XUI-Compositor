package XUI.Platform.AndroidInternal;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class WaitingHandler extends Handler {

    Runnable Target;
    ManualResetEventSlim TargetHandled = new ManualResetEventSlim();

    public WaitingHandler(@NonNull Looper looper) {
        super(looper);
    }

    public WaitingHandler(@NonNull Looper looper, @Nullable Callback callback) {
        super(looper, callback);
    }

    @Override
    public void dispatchMessage(@NonNull Message msg) {
        if (Target != null) {
            Runnable callback = msg.getCallback();
            if (callback != null && Target == callback) {
                super.dispatchMessage(msg);
                TargetHandled.Set();
                return;
            }
        }
        super.dispatchMessage(msg);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        if (Target != null) {
            Runnable callback = msg.getCallback();
            if (callback != null && Target == callback) {
                super.handleMessage(msg);
                TargetHandled.Set();
                return;
            }
        }
        super.handleMessage(msg);
    }

    public void postAndWait(Runnable runnable) {
        Target = runnable;
        post(runnable);
        // The caller of this method blocks indefinitely until the current instance is set.
        //   The caller will return immediately if the event is currently in a set state.
        TargetHandled.Wait();
        TargetHandled.Reset();
        Target = null;
    }
}
