package me.antonio.noack.thedollargame;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;

import static java.lang.Math.abs;

public class Dot implements Comparable<Dot> {

    private static int uuidCtr = 0;
    private final int uuid = uuidCtr++;

    public boolean isEdgy;
    public double arc;

    public ArrayList<Dot> connected = new ArrayList<>();

    private static float last = .5f;

    public Dot(int i, int len, int value){

        float this1 = (float) Math.random(), this2 = (float) Math.random()/*, this3 = (float) Math.random()*/;

        if(abs(this1-last) < abs(this2-last)) this1 = this2;
        //if(abs(this1-last) < abs(this3-last)) this1 = this3;
        last = this1;

        float f = 6.28f / len, s = (.3f + (1/len + 1 - sq(this1))) * .8f;
        x = (float) Math.cos(f*(i+.6f*Math.random())) * s;
        y = (float) Math.sin(f*(i+.6f*Math.random())) * s;
        this.value = value;
    }

    private float sq(float v) {
        return v*v;
    }

    public float x, y;
    public int value;

    public boolean connect(Dot b){
        if(b != this && !isConnected(b)){
            connected.add(b);
            Collections.sort(connected);
            b.connected.add(this);
            Collections.sort(b.connected);
            return true;
        } return false;
    }

    public boolean isConnected(Dot b){
        return Collections.binarySearch(connected, b) > -1;
    }

    public int edges(){
        return connected.size();
    }

    public Dot get(int index){
        return connected.get(index);
    }

    @Override public boolean equals(Object obj) {
        return obj instanceof Dot && ((Dot) obj).uuid == uuid;
    }

    @Override public int compareTo(@NonNull Dot o) {
        return o.uuid - uuid;
    }
}
