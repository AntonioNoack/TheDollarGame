package me.antonio.noack.thedollargame;

import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.Stack;

import static java.lang.Math.abs;
import static java.lang.Math.atan2;
import static java.lang.Math.min;

public class Net {

    public Dot[] dots;

    private float sq(float a){
        return a*a;
    }

    private boolean crosses(float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy){

        bx -= ax;
        by -= ay;
        dx -= cx;
        dy -= cy;
        cx -= ax;
        cy -= ay;

        // bx sollte nicht 0 sein...

        float f = by/bx;
        if(Float.isInfinite(f)){
            return false;
        }

        float t = -(cx*f - cy)/(dx*f - dy);
        if(t > -1 && t < 2){
            float s = (t*dx + cx)/bx;
            return s > -1 && s < 2;
        } else return false;
    }

    private float normalDot(float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy){
        bx -= ax;
        by -= ay;
        dx -= cx;
        dy -= cy;
        return (bx * dx + by * dy) / (float) Math.sqrt((sq(bx)+sq(by)) * (sq(dx)+sq(dy)));
    }

    private float crosses(float ax, float ay, float bx, float by){
        float score = 0;
        for(Dot c:dots){
            for(Dot d:c.connected){
                if(c.compareTo(d) < 0){
                    if(crosses(ax, ay, bx, by, c.x, c.y, d.x, d.y)){
                        //float distance = 1 + sq((ax+bx)-(c.x +d.x))+sq((ay+by)-(c.y +d.y));
                        //float radi = (sq(ax-bx) + sq(ay-by) + sq(c.x-d.x) + sq(c.y-d.y));
                        score += Math.pow(abs(normalDot(ax, ay, bx, by, c.x, c.y, d.x, d.y)), 8)/* * radi / distance*/;
                    }
                }
            }
        } return score;
    }

    /*private float arcWeight = 1;

    private float cross2(float ax, float ay, float bx, float by){
        float arc = (float) atan2(ay-by, ax-bx) / 3.1416f;// [-1,1]
        if(arc < 0) arc = 1 - arc;
        int index = (int) (arc * 16);
        return arcs[index & 15] / arcWeight;
    }

    private void addArc(float ax, float ay, float bx, float by){
        float arc = (float) atan2(ay-by, ax-bx) / 3.1416f;// [-1,1]
        if(arc < 0) arc = 1 - arc;
        int index = (int) (arc * 16);
        arcs[(index+15) & 15] ++;
        arcs[index & 15] += 3;
        arcs[(index+1) & 15] ++;
        arcWeight += 5;
    }

    private float[] arcs = new float[16];*/

    public Net(int vertices, int edges, int money, boolean betterNets){

        dots = new Dot[vertices];
        float val = (1f * money / vertices);
        float sqrt = (float) Math.sqrt(vertices);

        for(int i=0;i<vertices;i++){
            int x = (int) Math.round(sqrt*(Math.random()*2-1) + val);
            dots[i] = new Dot(i, vertices, i == vertices-1 ? money : x);
            money -= x;
        }

        int l = vertices-1;
        for(int j=1;j<l && edges>0;j++,edges--){
            dots[j-1].connect(dots[j+1]);
        }// dots[l].connect(dots[0]); edges--;

        int tries = 5 * edges;
        while(edges > 0 && tries-- > 0){

            int i1 = (int)(Math.random() * vertices);
            Dot a = dots[i1];
            float min = Float.POSITIVE_INFINITY;
            int bi = -1;
            for(int i=0;i<vertices;i++){
                if(i!=i1){
                    Dot b = dots[i];
                    if(!b.isConnected(a)){
                        float d;
                        if(min > (d = (sq(a.x -b.x)+sq(a.y -b.y)) * ((float) Math.random() + 2f) + a.edges() * b.edges() * .3f + (betterNets ? crosses(a.x, a.y, b.x, b.y) : 0))){
                            min = d;
                            bi = i;
                        }
                    }
                }
            }

            if(bi > -1){
                if(a.connect(dots[bi])){
                    edges--;
                }// else error somehow ...
            }

            /*if(dots[(int)(Math.random() * vertices)].connect(dots[(int)(Math.random() * vertices)])){
                edges --;
            }*/
        }

        /*if(dots.length > 4){

            /*float s = (.3f + (1f/dots.length + 1)) * .8f * .5f;
            Random random = new Random();

            for(Dot dot: dots){
                dot.x = (float) random.nextGaussian() * s;
                dot.y = (float) random.nextGaussian() * s;
            }*/

        /*    grahamsScan(dots);

            for(int i=0;i<10;i++){
                // todo make the thing better :)
                for(Dot dot: dots){
                    if(!dot.isEdgy && !dot.connected.isEmpty()){
                        float midX = 0f;
                        float midY = 0f;
                        float weight = 0f;
                        for(Dot con: dot.connected){
                            float wi = sq(con.x - dot.x, con.y - dot.y);
                            midX += con.x * wi;
                            midY += con.y * wi;
                            weight += wi;
                        }
                        float f2Length = .5f / weight;
                        // float dX = midX * f2Length - dot.x;
                        // float dY = midY * f2Length - dot.y;
                        dot.x = .5f * dot.x + midX * f2Length;
                        dot.y = .5f * dot.y + midY * f2Length;
                    }
                }
            }
        }*/
    }

    private void grahamsScan(Dot[] pts){

        // suche den Punkt mit der kleinsten y-Koordinate
        // bei mehreren den mit der kleinsten x-Koordinate
        float minY = Float.POSITIVE_INFINITY;
        Dot minPt = pts[0];
        minPt.isEdgy = false;
        for(int i=1;i<pts.length;i++){
            Dot pt = pts[i];
            pt.isEdgy = false;
            if(pt.y < minY){
                minPt = pt;
                minY = pt.y;
            } else if(pt.y == minY && pt.x < minPt.x){
                minPt = pt;
            }
        }

        // sortiere die Punkte nach Winkel zwischen minPt und x-Achse
        ArrayList<Dot> andere = new ArrayList<>(pts.length-1);
        for(Dot pt: pts){
            if(pt != minPt){
                andere.add(pt);
                pt.arc = Math.atan2(pt.y-minPt.y, pt.x-minPt.x);
            }
        }

        Collections.sort(andere, new Comparator<Dot>() {
            @Override public int compare(Dot o1, Dot o2) {
                return Double.compare(o1.arc, o2.arc);
            }
        });

        for(int i=1;i<andere.size();i++){
            Dot a = andere.get(i-1);
            Dot b = andere.get(i);
            if(a.arc == b.arc){
                // der Punkt näher an p0 wird verworfen
                if(sq(a.x-minPt.x, a.y-minPt.y) > sq(b.x-minPt.x, b.y-minPt.y)){
                    // behalte a
                    andere.remove(i);
                } else {
                    // behalte b
                    andere.remove(i-1);
                };i--;
            }
        }

        // der Stack von Punkten der Hülle
        Stack<Dot> conv = new Stack<>();
        conv.add(minPt);
        conv.add(andere.get(0));
        for(int i=1;i<andere.size();){
            int stackLength = conv.size();
            Dot pt1 = conv.get(stackLength-1);
            Dot pt2 = conv.get(stackLength-2);
            Dot ai = andere.get(i);
            if(istClinksVonAB(pt2, pt1, ai) || stackLength == 2){
                conv.push(ai);
                i++;
            } else {
                conv.pop();
            }
        }

        for(Dot pt: conv){
            pt.isEdgy = true;
        }
    }

    private boolean istClinksVonAB(Dot a, Dot b, Dot c){
        return (b.x-a.x)*(c.y-a.y) > (c.x-a.x)*(b.y-a.y);
    }

    private double sq(double x, double y){
        return x*x+y*y;
    }

    private float sq(float x, float y){
        return x*x+y*y;
    }

    public void connect(int a, int b){
        Dot da = dots[a], db = dots[b];
    }
}
