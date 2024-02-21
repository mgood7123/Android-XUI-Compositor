package XUI.Platform.AndroidInternal;

import android.os.HandlerThread;
import android.util.Log;
import android.view.Choreographer;

/** @noinspection Convert2Lambda, Convert2Lambda */
public class RenderThread {
    /** @noinspection Anonymous2MethodRef, Convert2Lambda */
    static class RenderThreadActual extends HandlerThread {
        class RenderPost implements Choreographer.FrameCallback {
            RenderThreadActual renderThread;
            boolean continuous;

            @Override
            public void doFrame(long frameTimeNanos) {
                if (renderThread.OnDoFrame != null) {
                    renderThread.OnDoFrame.run(frameTimeNanos);
                }
                if (continuous) {
                    renderThread.postFrameCallback();
                }
            }
        }

        RenderPost renderPost = new RenderPost();

        public RenderThreadActual(String name) {
            super(name);
            renderPost.renderThread = this;
        }

        public RenderThreadActual(String name, int priority) {
            super(name, priority);
            renderPost.renderThread = this;
        }

        WaitingHandler handler;
        RunnableLong OnDoFrame;

        boolean isContinuous;

        void SetRenderMode(boolean continuous) {
            isContinuous = continuous;
            if (renderPost.continuous != isContinuous) {
                renderPost.continuous = isContinuous;
                Log.e("GL", "updated renderPost.continuous: " + renderPost.continuous + ", isContinuous: " + isContinuous);
                if (isContinuous) {
                    if (handler != null) {
                        Log.e("GL", "render changed from manual to continuous, posting post callback");
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                RenderThreadActual.this.postFrameCallback();
                            }
                        });
                        Log.e("GL", "render changed from manual to continuous, post callback complete");
                    }
                } else {
                    if (handler != null) {
                        Log.e("GL", "render changed from continuous to manual, posting remove callback and wait for completion");
                        handler.postAndWait(new Runnable() {
                            @Override
                            public void run() {
                                RenderThreadActual.this.removeFrameCallback();
                            }
                        });
                        Log.e("GL", "render changed from continuous to manual, remove callback has complete");
                    }
                }
            }
        }

        void postFrameCallback() {
            removeFrameCallback();
            Choreographer.getInstance().postFrameCallback(renderPost);
        }

        void postFrameCallbackInvalidate() {
            if (!isContinuous) {
                postFrameCallback();
            }
        }

        void removeFrameCallback() {
            Choreographer.getInstance().removeFrameCallback(renderPost);
        }
    }

    RenderThreadActual renderThread = new RenderThreadActual("RenderThread");

    public void SetOnDoFrame(RunnableLong value) {
        renderThread.OnDoFrame = value;
    }

    public RunnableLong GetOnDoFrame() {
        return renderThread.OnDoFrame;
    }

    public void Start() {
        renderThread.start();
        renderThread.handler = new WaitingHandler(renderThread.getLooper());
        if (renderThread.isContinuous) {
            renderThread.handler.post(new Runnable() {
                @Override
                public void run() {
                    renderThread.postFrameCallback();
                }
            });
        } else {
            renderThread.handler.postAndWait(new Runnable() {
                @Override
                public void run() {
                    renderThread.removeFrameCallback();
                }
            });
        }
    }

    public void SetRenderMode(boolean continuous) {
        renderThread.SetRenderMode(continuous);
    }

    public void Invalidate() {
        renderThread.postFrameCallbackInvalidate();
    }

    public void CancelInvalidate() {
        renderThread.postFrameCallbackInvalidate();
    }

    public void Stop() {
        Log.e("GL", "quit safely");
        renderThread.quitSafely();
        Log.e("GL", "quit safely done");
        while (true) {
            try {
                renderThread.join();
                return;
            } catch (InterruptedException ignored) {
            }
        }
    }

    public WaitingHandler Handler() {
        return renderThread.handler;
    }

    public void Post(Runnable runnable) {
        Handler().post(runnable);
    }

    public void PostAndWait(Runnable runnable) {
        Handler().postAndWait(runnable);
    }
}
