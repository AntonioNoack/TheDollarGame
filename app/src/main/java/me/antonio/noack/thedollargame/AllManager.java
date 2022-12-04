package me.antonio.noack.thedollargame;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.TintableBackgroundView;

public class AllManager extends AppCompatActivity {

    private SharedPreferences pref;
    private ViewFlipper flipper;
    private View cont;

    private Dialog dialog;
    private Monitor2 monitor;
    private Mode mode = Mode.LEVEL;
    private TextView levelButton;
    private ViewGroup levelButtons;
    private int playingLevel;
    private final boolean betterNets = false;
    static int maxConvolutions = 5;

    enum Mode {
        CUSTOM(){
            @Override void next(AllManager all, int lvl) {
                all.monitor.setNet(new Net(all.verts, all.edges, all.money, all.betterNets, maxConvolutions));
                if(all.dialog != null) all.dialog.dismiss();
                all.levelButton.setText(all.getResources().getString(R.string.level_title)
                        .replace("#v", all.verts+"")
                        .replace("#e", all.edges+"")
                        .replace("#m", all.money+""));
            }
        }, LEVEL(){
            @Override void next(AllManager all, int lvl) {
                int v = (int) Math.pow(lvl, 1.2) + 3, e = (int) Math.pow(lvl, 1.5)+3;
                all.monitor.setNet(new Net(v, e, e-v+1, all.betterNets, 0));
                if(all.dialog != null) all.dialog.dismiss();
                all.levelButton.setText(all.getResources().getString(R.string.level).replace("#level", (lvl+1)+""));
            }
        }, RANDOM(){
            @Override void next(AllManager all, int lvl) {
                int v = (int) (Math.random() * 10) + 3, e = v + (int)(Math.random() * 10);
                int m = e-v+(int)(Math.random() * 5);
                all.monitor.setNet(new Net(v, e, m, all.betterNets, maxConvolutions));
                if(all.dialog != null) all.dialog.dismiss();
                all.levelButton.setText(all.getResources().getString(R.string.level).replace("#level", (lvl+1)+""));
                all.levelButton.setText(all.getResources().getString(R.string.level_title)
                        .replace("#v", v+"")
                        .replace("#e", e+"")
                        .replace("#m", m+""));
            }
        };

        abstract void next(AllManager all, int lvl);
    }

    public void finished(){
        dialog = new AlertDialog.Builder(this)
                .setView(R.layout.m_done)
                .show();
        dialog.findViewById(R.id.back_to_menu).setOnClickListener(v -> {
            previous(flipper);
            monitor.setNet(null);
            dialog.dismiss();
        });
        dialog.findViewById(R.id.next).setOnClickListener(v -> {
            if(mode == Mode.CUSTOM){
                mode.next(AllManager.this, 0);
            } else if(mode == Mode.LEVEL){
                int bestLvl = Math.max(pref.getInt("lvl", 0), ++playingLevel);
                save(bestLvl);
                mode.next(AllManager.this, playingLevel);
            } else {
                mode.next(AllManager.this, 0);
            } dialog.dismiss();
        });
    }

    private void save(int lvl){
        try {
            pref.edit().putInt("lvl", lvl).apply();
        } catch (Exception|Error e){
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    Toast.makeText(AllManager.this, "couldn't save :(", Toast.LENGTH_SHORT).show();
                }
            });
        } showLevel(lvl);
    }

    private void showLevel(int lvl){
        levelButtons.removeAllViews();
        int bgColor = getResources().getColor(R.color.colorPrimary);
        int margin = (int) (.5f + TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                10,
                getResources().getDisplayMetrics()));
        for(int i=lvl;i>-1;i--){
            final int j = i;
            Button b = new Button(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, margin,0,0);
            b.setLayoutParams(params);
            b.setText("Level "+(i+1));
            b.setBackgroundColor(bgColor);
            b.setOnClickListener(v -> {
                mode = Mode.LEVEL;
                mode.next(AllManager.this, playingLevel = j);
                open(R.id.game);
            });
            levelButtons.addView(b);
        }
    }

    private void showLevel(){
        int level = pref.getInt("lvl", 0);
        showLevel(level);
    }

    private int verts, edges, money;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.p_main);

        pref = getPreferences(MODE_PRIVATE);
        flipper = findViewById(R.id.flipper);
        monitor = findViewById(R.id.monitor);
        monitor.setAll(this);

        levelButtons = findViewById(R.id.level_buttons);
        cont = findViewById(R.id.continu);
        findViewById(R.id.becomingHarder).setOnClickListener(w -> {
            mode = Mode.LEVEL;
            mode.next(AllManager.this, playingLevel = pref.getInt("lvl", 0));
            open(R.id.game);
        });
        findViewById(R.id.random).setOnClickListener(v -> {
            mode = Mode.RANDOM;
            mode.next(AllManager.this, 0);
            open(R.id.game);
        });
        findViewById(R.id.custom).setOnClickListener(v -> {
            // todo open dialog
            dialog = new AlertDialog.Builder(AllManager.this)
                    .setView(R.layout.m_custom)
                    .setCancelable(true)
                    .show();
            dialog.findViewById(R.id.ok).setOnClickListener(v1 -> {
                EditText vert = dialog.findViewById(R.id.vertices), edge = dialog.findViewById(R.id.edges), mone = dialog.findViewById(R.id.money);
                String vs = vert.getText().toString(), es = edge.getText().toString(), ms = mone.getText().toString();
                verts = 3;
                edges = 3;
                money = 1;
                try {
                    verts = Integer.parseInt(vs);
                    edges = Integer.parseInt(es);
                    money = Integer.parseInt(ms);

                    mode = Mode.CUSTOM;
                    mode.next(AllManager.this, 0);

                    open(R.id.game);

                } catch (NumberFormatException e){
                    Toast.makeText(AllManager.this, "Numbers couldn't be parsed!", Toast.LENGTH_SHORT).show();
                }
            });

        });
        findViewById(R.id.back).setOnClickListener(v -> {
            cont.setVisibility(View.VISIBLE);
            previous(flipper);
        });
        levelButton = findViewById(R.id.levelButton);
        /*findViewById(R.id.giveup).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                cont.setVisibility(View.GONE);
                /*if(mode == Mode.LEVEL){
                    int lvl = pref.getInt("lvl", 0);
                    if(lvl > 0){
                        lvl--;
                        save(lvl);
                    }
                } *//*previous(flipper);
            }
        });*/

        final Button button = findViewById(R.id.shuffleButton);
        button.setOnClickListener(v -> {
            shuffleMode = !shuffleMode;
            if(button instanceof TintableBackgroundView){
                ((TintableBackgroundView) button).setSupportBackgroundTintList(
                        ColorStateList.valueOf(getResources().getColor(shuffleMode ? R.color.colorAccent: R.color.colorPrimaryDark)));
            }
            // monitor.selected = null;
        });
        button.performClick();
        cont.setOnClickListener(v -> open(R.id.game));

        // todo continue a game?
        // todo if finding last game, enable continue

        showLevel();
    }

    boolean shuffleMode = true;

    private int index = 0;

    public void open(int id){
        View v = findViewById(id);
        flipper.removeView(v);
        flipper.addView(v, index = 1);
        next(flipper);
    }

    @Override
    public void onBackPressed() {
        if(index == 0) super.onBackPressed();
        else previous(flipper);
    }

    public void next(final ViewFlipper flipper){

        closeKeyboard();

        flipper.setInAnimation(this, R.anim.slide_in_from_right);
        flipper.setOutAnimation(this, R.anim.slide_out_from_right);

        flipper.showNext();
    }

    public void previous(final ViewFlipper flipper){

        closeKeyboard();

        flipper.setInAnimation(this, R.anim.slide_in_from_left);
        flipper.setOutAnimation(this, R.anim.slide_out_from_left);

        flipper.showPrevious();

        index = 0;
    }

    public void closeKeyboard(){
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if(imm != null){
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}
