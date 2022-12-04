package me.antonio.noack.thedollargame;

import static java.lang.Math.abs;

public class Net {

    public Dot[] dots;

    private float sq(float a) {
        return a * a;
    }

    private boolean crosses(float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy) {

        bx -= ax;
        by -= ay;
        dx -= cx;
        dy -= cy;
        cx -= ax;
        cy -= ay;

        // bx sollte nicht 0 sein...

        float f = by / bx;
        if (Float.isInfinite(f)) {
            return false;
        }

        float t = -(cx * f - cy) / (dx * f - dy);
        if (t > -1 && t < 2) {
            float s = (t * dx + cx) / bx;
            return s > -1 && s < 2;
        } else return false;
    }

    private float normalDot(float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy) {
        bx -= ax;
        by -= ay;
        dx -= cx;
        dy -= cy;
        return (bx * dx + by * dy) / (float) Math.sqrt((sq(bx) + sq(by)) * (sq(dx) + sq(dy)));
    }

    private float crosses(float ax, float ay, float bx, float by) {
        float score = 0;
        for (Dot c : dots) {
            for (Dot d : c.connected) {
                if (c.compareTo(d) < 0) {
                    if (crosses(ax, ay, bx, by, c.x, c.y, d.x, d.y)) {
                        //float distance = 1 + sq((ax+bx)-(c.x +d.x))+sq((ay+by)-(c.y +d.y));
                        //float radi = (sq(ax-bx) + sq(ay-by) + sq(c.x-d.x) + sq(c.y-d.y));
                        score += Math.pow(abs(normalDot(ax, ay, bx, by, c.x, c.y, d.x, d.y)), 8)/* * radi / distance*/;
                    }
                }
            }
        }
        return score;
    }

    public Net(int vertices, int edges, int money, boolean betterNets, int maxConvolutions) {

        dots = new Dot[vertices];
        float val = (1f * money / vertices);
        float sqrt = (float) Math.sqrt(vertices);

        for (int i = 0; i < vertices; i++) {
            int x = maxConvolutions > 0 ? (int) (val + Math.random()) : (int) Math.round(sqrt * (Math.random() * 2 - 1) + val);
            dots[i] = new Dot(i, vertices, i == vertices - 1 ? money : x);
            money -= x;
        }

        int l = vertices - 1;
        for (int j = 1; j < l && edges > 0; j++, edges--) {
            dots[j - 1].connect(dots[j + 1]);
        }

        int tries = 5 * edges;
        while (edges > 0 && tries-- > 0) {

            int i1 = (int) (Math.random() * vertices);
            Dot a = dots[i1];
            float min = Float.POSITIVE_INFINITY;
            int bi = -1;
            for (int i = 0; i < vertices; i++) {
                if (i != i1) {
                    Dot b = dots[i];
                    if (!b.isConnected(a)) {
                        float d;
                        if (min > (d = (sq(a.x - b.x) + sq(a.y - b.y)) * ((float) Math.random() + 2f) + a.edges() * b.edges() * .3f + (betterNets ? crosses(a.x, a.y, b.x, b.y) : 0))) {
                            min = d;
                            bi = i;
                        }
                    }
                }
            }

            if (bi > -1) {
                if (a.connect(dots[bi])) {
                    edges--;
                }// else error somehow ...
            }

        }

        if (maxConvolutions > 0) {
            for (Dot dot : dots) {
                double rand = Math.random();
                double plusMinus = rand * 2 - 1;
                int convolutions = (int) Math.round(plusMinus);
                dot.value += convolutions * dot.edges();
                for (Dot edge : dot.connected) {
                    edge.value -= convolutions;
                }
            }
        }
    }

}
