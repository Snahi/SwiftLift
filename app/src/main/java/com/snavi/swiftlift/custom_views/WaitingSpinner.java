package com.snavi.swiftlift.custom_views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.snavi.swiftlift.R;

public class WaitingSpinner extends SurfaceView implements SurfaceHolder.Callback {


    // CONST //////////////////////////////////////////////////////////////////////////////////////
    public static final int ROTATION_ANGLE    = 5;
    public static final int SLEEP_TIME        = 10;
    public static final int DEFAULT_DIMENSION = 50;


    // fields /////////////////////////////////////////////////////////////////////////////////////
    private AnimationThread m_thread;           // thread, that is animating this wheel
    private Bitmap          m_spinner;          // image that is being rotated
    private SurfaceHolder   m_surfaceHolder;
    private int             m_dimension;        // width and height of rotated image
    private Paint           m_imgPaint;         // paint for m_spinner
    private int             m_currAngle;        // angle at which image should be painted at the moment



    public WaitingSpinner(Context context)
    {
        super(context);
        init(context);
    }


    public WaitingSpinner(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }



    public WaitingSpinner(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context);
    }



    public WaitingSpinner(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }



    private void init(Context context)
    {
        m_surfaceHolder = getHolder();
        m_surfaceHolder.addCallback(this);
        m_currAngle = 0;
        m_spinner   = BitmapFactory.decodeResource(context.getResources(), R.drawable.spinner);

        setPaints();
        makeBgTransparent();
    }



    private void setPaints()
    {
        m_dimension = DEFAULT_DIMENSION;
        m_imgPaint = new Paint();
        m_imgPaint.setColor(Color.WHITE);
    }



    private void makeBgTransparent()
    {
        setZOrderOnTop(true);                                   // necessary
        m_surfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
    }



    private void tick()
    {
        m_currAngle = (m_currAngle + ROTATION_ANGLE) % 360;
    }



    private synchronized void render()
    {
        Canvas canvas = m_surfaceHolder.lockCanvas();
        if (canvas == null)
            return;

        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); // remove previous drawings

        canvas.drawBitmap(m_spinner, createRotationMatrix(), m_imgPaint);

        m_surfaceHolder.unlockCanvasAndPost(canvas);
    }



    private Matrix createRotationMatrix()
    {
        Matrix matrix = new Matrix();
        matrix.preRotate(m_currAngle, m_dimension / 2f, m_dimension / 2f);
        return matrix;
    }



    public void start()
    {
        m_thread = new AnimationThread();
        m_thread.start();
    }



    private void clearView()
    {
        Canvas canvas = m_surfaceHolder.lockCanvas();
        if (canvas != null)
        {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            m_surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }



    public void stop()
    {
        m_thread.end();
    }



    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        m_dimension = width;
        m_spinner   = Bitmap.createScaledBitmap(m_spinner, m_dimension, m_dimension, true);
    }




    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        m_thread = new AnimationThread();   // just in case someone created spinner, but didn't start it
    }



    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        m_thread.end();
        boolean joined = false;

        while (!joined)
        {
            try
            {
                m_thread.join();
                joined = true;
            }
            catch (InterruptedException e)
            {
                Log.e(WaitingSpinner.class.getName(), e.getMessage());
            }
        }
    }



    private class AnimationThread extends Thread {


        private boolean m_isRunning;


        @Override
        public void run()
        {
            m_isRunning = true;

            while (m_isRunning)
            {
                WaitingSpinner.this.render();
                WaitingSpinner.this.tick();
                sleep();
            }

            clearView();
        }



        void end()
        {
            m_isRunning = false;
        }



        private void sleep()
        {
            try
            {
                Thread.sleep(SLEEP_TIME);
            }
            catch (InterruptedException e)
            {
                Log.e(WaitingSpinner.class.getName(), e.getMessage());
            }
        }

    }
}
