package com.example.a3dfps;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Mview extends View {

    public class ZClippingCompare implements Comparator<float[]> {
        @Override
        public int compare(float[] t1, float[] t2) {
            if(t2[7]-t1[7]>0){
                return 1;
            }else if(t2[7]-t1[7]<0){
                return -1;
            }else{
                return 0;
            }
        }
    }

    class Entity{
        float x;
        float y;
        float z;
        float rx=0;
        float ry=0;
        float rz=0;
        public Entity(float x, float y,float z){
            this.x=x;
            this.y=y;
            this.z=z;
        }
    }







    Entity localplayer = new Entity(0, 0, 0);

    private SparseArray<PointF> mActivePointers= new SparseArray<PointF>();
    float touchStartX=-100;
    float touchStartY=-100;
    float touchCurrentX=-100;
    float touchCurrentY=-100;
    float touchRotStartX=-100;
    float touchRotStartY=-100;
    float touchRotCurrentX=-100;
    float touchRotCurrentY=-100;

    public int viewWidth=0;
    public int viewHeight=0;

    ArrayList<float[]> triangles = new ArrayList<>();

    Paint drawPaint =new Paint();
    Bitmap b;


    float sinrx=0;
    float cosrx=1;
    float sinry=0;
    float cosry=1;


    int fpscounter=0;
    int previousfps=0;

    public Mview(Context context, AttributeSet arrs) {
        super(context,arrs);

        b = BitmapFactory.decodeResource(getResources(), R.drawable.mmoiconround);

        setFocusable(true);
        setFocusableInTouchMode(true);
        setupPaint();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();

        display.getSize(size);
        viewHeight = size.y;
        viewWidth = size.x;
        scheduledupdates();
    }
    public void scheduledupdates(){
        ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
        exec.scheduleAtFixedRate(()->{
            if(touchStartX!=-100&&touchCurrentX!=-100&&touchStartY!=-100&&touchCurrentY!=-100){
                localplayer.x+=((touchCurrentX-touchStartX)*0.01)*Math.cos(localplayer.ry)+
                        ((touchCurrentY-touchStartY)*0.01)*Math.sin(localplayer.ry);
                localplayer.z-=(touchCurrentX-touchStartX)*-0.01*Math.sin(localplayer.ry)+
                        ((touchCurrentY-touchStartY)*0.01)*Math.cos(localplayer.ry);
            }
            if(touchRotStartX!=-100&&touchRotCurrentX!=-100&&touchRotStartY!=-100&&touchRotCurrentY!=-100) {
                localplayer.rx += ((float) (touchRotCurrentY - touchRotStartY)) * 0.0001;
                localplayer.ry -= ((float) (touchRotCurrentX - touchRotStartX)) * 0.0001;
                if (localplayer.rx > Math.PI / 2 || localplayer.rx < -Math.PI / 2) {
                    localplayer.rx -= ((float) (touchRotCurrentY - touchRotStartY)) * 0.0001;
                }
            }
            postInvalidate();
        },0,30,TimeUnit.MILLISECONDS);
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        executor.scheduleAtFixedRate(()->{previousfps=fpscounter;fpscounter=0;},0,1,TimeUnit.SECONDS);
    }
    private void setupPaint() {
        drawPaint = new Paint();
        drawPaint.setColor(Color.BLACK);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(20);
        drawPaint.setTextSize(100);
        drawPaint.setStyle(Paint.Style.FILL);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
    }


    public int[] toScreenCoordinates(float px,float py,float pz){
        int[] screenCoordinates = new int[3];
        int screenX;
        int screenY;

        //convert to  relative coordinates
        px=px-localplayer.x;
        py=py-localplayer.y;
        pz=pz-localplayer.z;
        final float pixelPerCM = 100;
        final float F = 10;

        //rotate around y axis
        //float rot_px= (float) (px*Math.cos(localplayer.ry)+pz*Math.sin(localplayer.ry));
        //float rot_pz= (float) (-px*Math.sin(localplayer.ry)+pz*Math.cos(localplayer.ry));
        float rot_px=cosry*px+sinry*pz;
        float rot_py=sinrx*sinry*px+cosrx*py-sinrx*cosry*pz;
        float rot_pz=-cosrx*sinry*px+sinrx*py+cosrx*cosry*pz;
        //float rot_py = py;

        screenX= (int) (
                (rot_px)*((float)F)/((float)(rot_pz+F)) *  pixelPerCM+viewWidth/2
                );

        screenY= (int) (
                (rot_py)*((float)F)/((float)(rot_pz+F))*pixelPerCM+viewHeight/2
        );
        screenCoordinates[0]=screenX;
        screenCoordinates[1]=screenY;
        screenCoordinates[2]=(int)rot_pz;
        return screenCoordinates;
    }

    public void DrawFilledTriangle(float[] t,Canvas canvas){
        Paint trianglePaint=new Paint();
        trianglePaint.setColor((int) t[6]);
        trianglePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        trianglePaint.setAntiAlias(true);

        if(t[7]<0&&t[8]<0&&t[9]<0){
            return;
        }
        if((t[0]<0||t[0]>viewWidth||t[1]<0||t[1]>viewHeight)&&
                (t[2]<0||t[2]>viewWidth||t[3]<0||t[3]>viewHeight)&&
                (t[4]<0||t[4]>viewWidth||t[5]<0||t[5]>viewHeight)){
            return;
        }

        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.moveTo(t[0], t[1]);
        path.lineTo(t[2], t[3]);
        path.lineTo(t[4], t[5]);
        path.close();

        canvas.drawPath(path, trianglePaint);
    }


    public void Draw3DLine(float px1,float py1,float pz1,float px2,float py2,float pz2,Canvas canvas,int color){
        int[] screenP1=toScreenCoordinates(px1,py1,pz1);
        int[] screenP2=toScreenCoordinates(px2,py2,pz2);
        // behind screen clipping
        if ((screenP1[2]<0) || (screenP2[2]<0)) {
            return;
            //color = Color.MAGENTA;
        }
        // screen size clipping
        if((screenP1[0]<-viewWidth|screenP1[0]>2*viewWidth|screenP1[1]<-viewHeight|screenP1[1]>2*viewHeight)&&(
                screenP2[0]<-viewWidth|screenP2[0]>2*viewWidth|screenP2[1]<-viewHeight|screenP2[1]>2*viewHeight)){
            return;
            //color = Color.GREEN;
        }
        Paint linePaint=new Paint();
        linePaint.setColor(color);
        linePaint.setStrokeWidth(5);
        canvas.drawLine(screenP1[0],screenP1[1],screenP2[0],screenP2[1],linePaint);
    }
    public void Draw3DTriangle(float x1, float y1,float z1,float x2,float y2,float z2,float x3,float y3,float z3,Canvas canvas,int color){

        int[] p1=toScreenCoordinates(x1, y1, z1);
        int[] p2=toScreenCoordinates(x2, y2, z2);
        int[] p3=toScreenCoordinates(x3, y3, z3);

        int sx1 = p1[0];
        int sy1 = p1[1];
        int check1 = p1[2];
        int sx2 = p2[0];
        int sy2 = p2[1];
        int check2 = p2[2];
        int sx3 = p3[0];
        int sy3 = p3[1];
        int check3 = p3[2];
        float[] triangle =new float[]{sx1,sy1,sx2,sy2,sx3,sy3,color,check1,check2,check3};

        DrawFilledTriangle(triangle,canvas);
    }



    public void DrawCuboidWireframe(float px1,float py1,float pz1,float px2,float py2,float pz2,Canvas canvas,int color){
        //top rectangle
        Draw3DLine(px1,py1,pz1,px1,py1,pz2,canvas,color);
        Draw3DLine(px1,py1,pz2,px2,py1,pz2,canvas,color);
        Draw3DLine(px2,py1,pz2,px2,py1,pz1,canvas,color);
        Draw3DLine(px2,py1,pz1,px1,py1,pz1,canvas,color);
        //bottom rectangle
        Draw3DLine(px1,py2,pz1,px1,py2,pz2,canvas,color);
        Draw3DLine(px1,py2,pz2,px2,py2,pz2,canvas,color);
        Draw3DLine(px2,py2,pz2,px2,py2,pz1,canvas,color);
        Draw3DLine(px2,py2,pz1,px1,py2,pz1,canvas,color);
        //the center sides
        Draw3DLine(px1,py1,pz1,px1,py2,pz1,canvas,color);
        Draw3DLine(px1,py1,pz2,px1,py2,pz2,canvas,color);
        Draw3DLine(px2,py1,pz2,px2,py2,pz2,canvas,color);
        Draw3DLine(px2,py1,pz1,px2,py2,pz1,canvas,color);
    }
    public void DrawCube(float x, float y,float z, float size,Canvas canvas,int color){
        DrawCuboidWireframe(x,y,z,x+size,y+size,z+size,canvas,color);
    }
    public void DrawSphereViaCubes(float cx,float cy, float cz,float radius,float cubeSize,int Nu, int Nv,Canvas canvas,int color){
        for(int i=0;i<=Nu+1;i++){
            for(int j=0;j<=Nv+1;j++){
                float u = (float) (-0.5 * Math.PI + 1.0*Math.PI*i/(float)(Nu+1));
                float v = (float) (2.0*Math.PI*j/(float)(Nv+1));
                float y= (float) (radius*Math.sin(u))+cy;
                float z= (float) (radius*Math.cos(u)*Math.sin(v))+cz;
                float x= (float) (radius*Math.cos(u)*Math.cos(v))+cx;

                DrawCube(x,y,z,cubeSize,canvas,color);
            }
        }
    }
    public void CreateCylynderViaTriangles(float cx,float cy, float cz,float radius,float length,int Nu,int Nv,Canvas canvas,int color){
        Float previousx=null;
        Float previousy = null;
        Float previousz=null;
        for(int i=0;i<Nv;i++){
            for(int j=0;j<=Nu+1;j++){
                float u = length*i/(Nv+1);
                float v = (float) (Math.PI*j/(float)(Nu+1));
                float y= u+cy-length/2;
                float z= (float) (radius*Math.cos(v)*Math.sin(v))+cz;
                float x= (float) (radius*Math.cos(v)*Math.cos(v))+cx-radius/2;

                float u2 = length*(i+1)/(Nv+1);
                float v2 = (float) (Math.PI*(j-1)/(float)(Nu+1));

                float y1= u2+cy-length/2;
                float z1= (float) (radius*Math.cos(v2)*Math.sin(v2))+cz;
                float x1= (float) (radius*Math.cos(v2)*Math.cos(v2))+cx-radius/2;


                if(previousx!=null||previousy!=null||previousz!=null){

                    int[] p1 =toScreenCoordinates(x,y,z);
                    int[] p2 =toScreenCoordinates(x1,y1,z1);
                    int[] p3 =toScreenCoordinates(previousx,previousy,previousz);
                    int[] p4 =toScreenCoordinates(x,y1,z);
                    triangles.add(new float[]{p1[0],p1[1],p2[0],p2[1],p3[0],p3[1],color,p1[2],p2[2],p3[2]});
                    triangles.add(new float[]{p1[0],p1[1],p2[0],p2[1],p4[0],p4[1],color,p1[2],p2[2],p4[2]});
                }
                previousx=x;
                previousy=y;
                previousz=z;
            }
        }
    }

    public void CreateSphereViaTriangles(float cx, float cy, float cz, float radius, int Nu, int Nv, Canvas canvas, int color){
        Float previousx=null;
        Float previousy = null;
        Float previousz=null;
        Color color1 = Color.valueOf(color);
        float cr = color1.red();
        float cb = color1.blue();
        float cg = color1.green();
        for(int i=0;i<=Nu+1;i++){
            cr*= (double) Nu /(Nu+2);
            cb*= (double) Nu /(Nu+2);
            cg*= (double) Nu /(Nu+2);
            color=Color.rgb(cr,cg,cb);
            for(int j=0;j<=Nv+1;j++){
                float u = (float) (-0.5 * Math.PI + 1.0*Math.PI*i/(float)(Nu+1));
                float v = (float) (2.0*Math.PI*j/(float)(Nv+1));
                float y= (float) (radius*Math.sin(u))+cy;
                float z= (float) (radius*Math.cos(u)*Math.sin(v))+cz;
                float x= (float) (radius*Math.cos(u)*Math.cos(v))+cx;

                float u1 = (float) (-0.5 * Math.PI + 1.0*Math.PI*(i+1)/(float)(Nu+1));
                float v1 = (float) (2.0*Math.PI*(j+1)/(float)(Nv+1));
                float y1= (float) (radius*Math.sin(u1))+cy;
                float z1= (float) (radius*Math.cos(u1)*Math.sin(v1))+cz;
                float x1= (float) (radius*Math.cos(u1)*Math.cos(v1))+cx;

                float z2= (float) (radius*Math.cos(u1)*Math.sin(v))+cz;
                float x2= (float) (radius*Math.cos(u1)*Math.cos(v))+cx;


                float z3= (float) (radius*Math.cos(u)*Math.sin(v1))+cz;
                float x3= (float) (radius*Math.cos(u)*Math.cos(v1))+cx;

                if(previousx!=null||previousy!=null||previousz!=null){
                    int[] p1 =toScreenCoordinates(x,y,z);
                    int[] p2 =toScreenCoordinates(x1,y1,z1);
                    int[] p3 =toScreenCoordinates(x2,y1,z2);
                    int[] p4 =toScreenCoordinates(x3,y,z3);
                    triangles.add(new float[]{p1[0],p1[1],p2[0],p2[1],p3[0],p3[1],color,p1[2],p2[2],p3[2]});
                    triangles.add(new float[]{p1[0],p1[1],p2[0],p2[1],p4[0],p4[1],color,p1[2],p2[2],p4[2]});
                }
                previousx=x;
                previousy=y;
                previousz=z;
            }
        }
    }



    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void draw(Canvas canvas) {
        triangles.clear();
        sinrx= (float) Math.sin(localplayer.rx);
        cosrx= (float) Math.cos(localplayer.rx);
        sinry= (float) Math.sin(localplayer.ry);
        cosry= (float) Math.cos(localplayer.ry);
        super.draw(canvas);
        drawPaint.setColor(Color.BLACK);
        drawPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0,0,viewWidth,viewHeight,drawPaint);


        CreateSphereViaTriangles(70, 0,500,100,10,20,canvas,Color.rgb(230, 120, 0));
        //CreateSphereViaTriangles(-70, 0,500,100,10,20,canvas,Color.rgb(230, 120, 0));
        //CreateCylynderViaTriangles(0,-200,500,100,400,10,20,canvas,Color.rgb(115, 60, 0));


        triangles.sort(new ZClippingCompare());
        for (float[] t:triangles) {
            DrawFilledTriangle(t,canvas);
        }




        //just joystick nothing more
        drawPaint.setColor(Color.BLUE);
        canvas.drawCircle(touchStartX,touchStartY,10,drawPaint);
        canvas.drawCircle(touchCurrentX,touchCurrentY,20,drawPaint);
        canvas.drawCircle(touchRotStartX,touchRotStartY,10,drawPaint);
        canvas.drawCircle(touchRotCurrentX,touchRotCurrentY,20,drawPaint);


        fpscounter++;
        canvas.drawText(String.valueOf(previousfps),100,100,drawPaint);
    }



    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int pointerIndex=event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);
        int maskedAction = event.getActionMasked();
        switch (maskedAction){
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:{
                PointF f = new PointF();
                f.x = event.getX(pointerIndex);
                f.y=event.getY(pointerIndex);
                if(f.x<viewWidth/2){
                    touchStartX=event.getX();
                    touchStartY=event.getY();
                    touchCurrentX=event.getX();
                    touchCurrentY=event.getY();
                }else if(f.x>viewWidth/2){
                    touchRotStartX=event.getX();
                    touchRotStartY=event.getY();
                    touchRotCurrentX=event.getX();
                    touchRotCurrentY=event.getY();
                }
                mActivePointers.put(pointerId,f);
                break;
            }
            case MotionEvent.ACTION_MOVE:{
                float ex = event.getX(pointerIndex);
                if(ex<viewWidth/2){
                    touchCurrentX=event.getX();
                    touchCurrentY=event.getY();
                }else if(ex>viewWidth/2){
                    touchRotCurrentX=event.getX();
                    touchRotCurrentY=event.getY();
                }
                break;
            }case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:{
                mActivePointers.remove(pointerId);
                touchStartX=-100;
                touchStartY=-100;
                touchCurrentX=-100;
                touchCurrentY=-100;
                touchRotStartX=-100;
                touchRotStartY=-100;
                touchRotCurrentX=-100;
                touchRotCurrentY=-100;
            }
        }
        postInvalidate();
        return true;
    }
}
