package XUI.Platform.AndroidInternal;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** @noinspection SynchronizationOnLocalVariableOrMethodParameter*/
public class ManualResetEventSlim {
    private final AtomicBoolean set = new AtomicBoolean(false);
    private int spinCount = 10000;
    private final AtomicReference<Object> waitHandle = new AtomicReference<>(null);
    public ManualResetEventSlim() {
    }

    public ManualResetEventSlim(boolean set) {
        this.set.set(set);
    }

    public ManualResetEventSlim(boolean set, int spinCount) {
        this.set.set(set);
        this.spinCount = spinCount;
    }

    public boolean IsSet() {
        return set.get();
    }

    public int SpinCount() {
        return spinCount;
    }

    public Object SpinWaitHandle() {
        if (waitHandle.get() == null) {
            Object o = new Object();
            waitHandle.set(o);
            return o;
        }
        return waitHandle.get();
    }

    public void SpinSet() {
        set.set(true);
        Object o = waitHandle.get();
        if (o != null) {
            synchronized (o) {
                o.notifyAll();
            }
        }
    }

    public void SpinWait() {
        for (int i = 0; i < spinCount; i++) {
            if (set.get()) {
                return;
            }
        }
        Object o = SpinWaitHandle();
        synchronized (o) {
            while (true) {
                try {
                    o.wait();
                    break;
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    public void SpinReset() {
        set.set(false);
        waitHandle.set(null);
    }
}
