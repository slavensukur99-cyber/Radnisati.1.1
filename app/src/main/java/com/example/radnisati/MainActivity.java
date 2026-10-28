package com.example.radnisati;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.animation.ObjectAnimator;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public class MainActivity extends Activity {
    private final int BG = Color.rgb(16,19,24), PANEL = Color.rgb(27,32,40), TEXT = Color.rgb(239,243,248), MUTED = Color.rgb(155,166,181), GREEN = Color.rgb(114,224,178), BLUE = Color.rgb(111,174,255);
    private SharedPreferences prefs;
    private YearMonth shownMonth = YearMonth.now();
    private LinearLayout root, calendarBox;
    private TextView monthLabel, dayTotal, nightTotal, allTotal, selectedLabel, selectedShift, selectedNote;
    private LocalDate selectedDate = LocalDate.now();
    private final DateTimeFormatter keyFormat = DateTimeFormatter.ISO_LOCAL_DATE;
    private final DateTimeFormatter titleFormat = DateTimeFormatter.ofPattern("d. MMMM yyyy", new Locale("sr", "BA"));

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("work_shifts", MODE_PRIVATE);
        buildUi();
        renderCalendar();
        updateSummary();
        updateSelected();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(18),dp(18),dp(24)); scroll.addView(root);
        LinearLayout brand = new LinearLayout(this); brand.setOrientation(LinearLayout.HORIZONTAL); brand.setGravity(Gravity.CENTER_VERTICAL); root.addView(brand, params(-1,-2));
        TextView hourglass = text("⌛", 32, GREEN, true); brand.addView(hourglass, params(dp(48),dp(48)));
        ObjectAnimator pulse = ObjectAnimator.ofFloat(hourglass, "rotation", -8f, 8f, -8f); pulse.setDuration(1800); pulse.setRepeatCount(ObjectAnimator.INFINITE); pulse.start();
        LinearLayout brandText = new LinearLayout(this); brandText.setOrientation(LinearLayout.VERTICAL); brand.addView(brandText, new LinearLayout.LayoutParams(0,-2,1));
        TextView title = text("РАДНИ САТИ", 25, TEXT, true); brandText.addView(title);
        TextView subtitle = text("Мјесечна евиденција рада", 14, MUTED, false); LinearLayout.LayoutParams sp = params(-1,-2); sp.topMargin=dp(2); brandText.addView(subtitle,sp);

        LinearLayout summary = new LinearLayout(this); summary.setOrientation(LinearLayout.VERTICAL); summary.setPadding(dp(16),dp(14),dp(16),dp(14)); summary.setBackground(round(PANEL,18));
        LinearLayout.LayoutParams smp=params(-1,-2); smp.topMargin=dp(18); root.addView(summary,smp);
        TextView summaryTitle=text("УКУПНО ОВОГ МЈЕСЕЦА",12,MUTED,true); summary.addView(summaryTitle);
        allTotal=text("0 ч 00 мин",28,TEXT,true); LinearLayout.LayoutParams atp=params(-1,-2); atp.topMargin=dp(5); summary.addView(allTotal,atp);
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); LinearLayout.LayoutParams rp=params(-1,-2); rp.topMargin=dp(12); summary.addView(row,rp);
        LinearLayout dayCol = new LinearLayout(this); dayCol.setOrientation(LinearLayout.VERTICAL); row.addView(dayCol,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout nightCol = new LinearLayout(this); nightCol.setOrientation(LinearLayout.VERTICAL); row.addView(nightCol,new LinearLayout.LayoutParams(0,-2,1));
        dayCol.addView(text("☀ ДНЕВНИ РАД",12,GREEN,true)); dayTotal=text("0 ч 00 мин",17,TEXT,true); LinearLayout.LayoutParams dlp=params(-1,-2); dlp.topMargin=dp(4); dayCol.addView(dayTotal,dlp);
        nightCol.addView(text("☾ НОЋНИ РАД",12,BLUE,true)); nightTotal=text("0 ч 00 мин",17,TEXT,true); LinearLayout.LayoutParams nlp=params(-1,-2); nlp.topMargin=dp(4); nightCol.addView(nightTotal,nlp);

        LinearLayout monthNav = new LinearLayout(this); monthNav.setGravity(Gravity.CENTER_VERTICAL); monthNav.setOrientation(LinearLayout.HORIZONTAL); LinearLayout.LayoutParams mnp=params(-1,dp(54)); mnp.topMargin=dp(12); root.addView(monthNav,mnp);
        Button prev=navButton("‹"); monthNav.addView(prev,params(dp(48),dp(42))); prev.setOnClickListener(v->{shownMonth=shownMonth.minusMonths(1);renderCalendar();updateSummary();});
        monthLabel=text("",18,TEXT,true); monthLabel.setGravity(Gravity.CENTER); monthNav.addView(monthLabel,new LinearLayout.LayoutParams(0,-2,1));
        Button next=navButton("›"); monthNav.addView(next,params(dp(48),dp(42))); next.setOnClickListener(v->{shownMonth=shownMonth.plusMonths(1);renderCalendar();updateSummary();});

        calendarBox = new LinearLayout(this); calendarBox.setOrientation(LinearLayout.VERTICAL); LinearLayout.LayoutParams cbp=params(-1,-2); cbp.topMargin=dp(4); root.addView(calendarBox,cbp);

        LinearLayout selectedCard = new LinearLayout(this); selectedCard.setOrientation(LinearLayout.VERTICAL); selectedCard.setPadding(dp(16),dp(15),dp(16),dp(16)); selectedCard.setBackground(round(PANEL,18)); LinearLayout.LayoutParams scp=params(-1,-2); scp.topMargin=dp(16); root.addView(selectedCard,scp);
        selectedLabel=text("Изабрани дан",18,TEXT,true); selectedCard.addView(selectedLabel);
        selectedShift=text("Није унесена смјена",14,MUTED,false); LinearLayout.LayoutParams ssp=params(-1,-2); ssp.topMargin=dp(6); selectedCard.addView(selectedShift,ssp);
        selectedNote=text("📝 Напомена: није унесена",13,MUTED,false); selectedNote.setLineSpacing(dp(3),1f); LinearLayout.LayoutParams snp=params(-1,-2); snp.topMargin=dp(10); selectedCard.addView(selectedNote,snp);
        Button edit = new Button(this); edit.setText("УНЕСИ / ИЗМИЈЕНИ СМЈЕНУ"); edit.setTextColor(BG); edit.setAllCaps(false); edit.setTypeface(null,Typeface.BOLD); edit.setBackground(round(GREEN,12)); LinearLayout.LayoutParams ep=params(-1,dp(50)); ep.topMargin=dp(14); selectedCard.addView(edit,ep); edit.setOnClickListener(v->editShift());
        Button note = new Button(this); note.setText("📝 ДОДАЈ / ИЗМИЈЕНИ НАПОМЕНУ"); note.setAllCaps(false); note.setTextColor(TEXT); note.setBackground(round(Color.rgb(45,53,67),12)); LinearLayout.LayoutParams notep=params(-1,dp(46)); notep.topMargin=dp(8); selectedCard.addView(note,notep); note.setOnClickListener(v->editNote());
        Button clear = new Button(this); clear.setText("Обриши смјену за овај дан"); clear.setAllCaps(false); clear.setTextColor(TEXT); clear.setBackground(round(Color.rgb(53,59,70),12)); LinearLayout.LayoutParams clp=params(-1,dp(44)); clp.topMargin=dp(8); selectedCard.addView(clear,clp); clear.setOnClickListener(v->clearShift());
        TextView rule=text("Обрачун: ноћни рад је од 22:00 до 06:00. Смјене које прелазе поноћ се рачунају аутоматски.",12,MUTED,false); rule.setLineSpacing(dp(3),1f); LinearLayout.LayoutParams rlp=params(-1,-2); rlp.topMargin=dp(16); root.addView(rule,rlp);
        setContentView(scroll);
    }

    private void renderCalendar() {
        monthLabel.setText(cap(shownMonth.getMonth().getDisplayName(TextStyle.FULL,new Locale("sr","BA")))+" "+shownMonth.getYear());
        calendarBox.removeAllViews();
        GridLayout weekdays = new GridLayout(this); weekdays.setColumnCount(7); calendarBox.addView(weekdays,params(-1,dp(32)));
        String[] names={"ПОН","УТО","СРИ","ЧЕТ","ПЕТ","СУБ","НЕД"};
        for(String n:names){TextView t=text(n,10,MUTED,true);t.setGravity(Gravity.CENTER);weekdays.addView(t,new GridLayout.LayoutParams());}
        GridLayout grid = new GridLayout(this); grid.setColumnCount(7); grid.setRowCount(6); calendarBox.addView(grid,params(-1,-2));
        int offset=shownMonth.atDay(1).getDayOfWeek().getValue()-1;
        int count=shownMonth.lengthOfMonth();
        for(int i=0;i<42;i++){
            int day=i-offset+1; TextView cell;
            if(day<1||day>count){cell=text("",12,MUTED,false);}
            else {
                LocalDate date=shownMonth.atDay(day); String data=prefs.getString(key(date),"");
                String noteData=prefs.getString(noteKey(date),""); String label=String.valueOf(day); if(!data.isEmpty() || !noteData.isEmpty()) label+="\n●";
                cell=text(label,13,date.equals(selectedDate)?BG:TEXT,true); cell.setGravity(Gravity.CENTER); cell.setMinHeight(dp(48));
                int bg=date.equals(selectedDate)?GREEN:(data.isEmpty()?PANEL:Color.rgb(35,58,53)); cell.setBackground(round(bg,10));
                GridLayout.LayoutParams lp=new GridLayout.LayoutParams(); lp.width=0; lp.height=dp(48); lp.columnSpec=GridLayout.spec(i%7,1f); lp.setMargins(dp(2),dp(2),dp(2),dp(2)); grid.addView(cell,lp);
                cell.setOnClickListener(v->{selectedDate=date;renderCalendar();updateSelected();});
            }
            if(day<1||day>count){GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=dp(48);lp.columnSpec=GridLayout.spec(i%7,1f);lp.setMargins(dp(2),dp(2),dp(2),dp(2));grid.addView(cell,lp);}
        }
    }

    private void editShift() {
        String existing=prefs.getString(key(selectedDate),""); String[] parts=existing.isEmpty()?new String[]{"08:00","16:00"}:existing.split("\\|");
        final String[] times={parts[0],parts.length>1?parts[1]:"16:00"};
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(20),dp(10),dp(20),dp(4));
        TextView info=text("Изабери почетак и крај смјене. Ако завршиш након поноћи, унеси вријеме завршетка као што је 06:00.",14,MUTED,false); info.setLineSpacing(dp(3),1f);box.addView(info);
        Button start=new Button(this);start.setAllCaps(false);start.setText("Почетак: "+times[0]);box.addView(start,params(-1,dp(48)));
        Button end=new Button(this);end.setAllCaps(false);end.setText("Завршетак: "+times[1]);LinearLayout.LayoutParams elp=params(-1,dp(48));elp.topMargin=dp(8);box.addView(end,elp);
        start.setOnClickListener(v->pickTime(times,0,start,"Почетак: "));end.setOnClickListener(v->pickTime(times,1,end,"Завршетак: "));
        new AlertDialog.Builder(this).setTitle(selectedDate.format(titleFormat)).setView(box).setNegativeButton("Одустани",null).setPositiveButton("Сачувај",(d,w)->{
            int a=minutes(times[0]), b=minutes(times[1]); if(a==b){Toast.makeText(this,"Почетак и завршетак не могу бити исти.",Toast.LENGTH_LONG).show();return;}
            prefs.edit().putString(key(selectedDate),times[0]+"|"+times[1]).apply();renderCalendar();updateSummary();updateSelected();Toast.makeText(this,"Смјена је сачувана",Toast.LENGTH_SHORT).show();
        }).show();
    }
    private void pickTime(String[] times,int index,Button button,String prefix){int m=minutes(times[index]);new TimePickerDialog(this,(v,h,mi)->{times[index]=String.format(Locale.US,"%02d:%02d",h,mi);button.setText(prefix+times[index]);},m/60,m%60,true).show();}
    private void editNote(){
        EditText input=new EditText(this); input.setText(prefs.getString(noteKey(selectedDate),"")); input.setTextSize(16); input.setTextColor(TEXT); input.setHintTextColor(MUTED); input.setHint("Шта си радио тог дана..."); input.setGravity(Gravity.TOP|Gravity.START); input.setMinLines(4); input.setPadding(dp(12),dp(12),dp(12),dp(12)); input.setBackground(round(Color.rgb(38,44,54),12));
        LinearLayout wrap=new LinearLayout(this); wrap.setPadding(dp(20),dp(8),dp(20),dp(4)); wrap.addView(input,params(-1,dp(140)));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Напомена — "+selectedDate.format(titleFormat)).setView(wrap).setNegativeButton("Одустани",null).setNeutralButton("Обриши",(d,w)->{prefs.edit().remove(noteKey(selectedDate)).apply();renderCalendar();updateSelected();Toast.makeText(this,"Напомена је обрисана",Toast.LENGTH_SHORT).show();}).setPositiveButton("Сачувај",(d,w)->{String value=input.getText().toString().trim();if(value.isEmpty())prefs.edit().remove(noteKey(selectedDate)).apply();else prefs.edit().putString(noteKey(selectedDate),value).apply();renderCalendar();updateSelected();Toast.makeText(this,"Напомена је сачувана",Toast.LENGTH_SHORT).show();}).create(); dialog.show();
    }
    private void clearShift(){if(prefs.contains(key(selectedDate))){prefs.edit().remove(key(selectedDate)).apply();renderCalendar();updateSummary();updateSelected();Toast.makeText(this,"Смјена је обрисана",Toast.LENGTH_SHORT).show();}else Toast.makeText(this,"За овај дан нема сачуване смјене",Toast.LENGTH_SHORT).show();}
    private void updateSelected(){selectedLabel.setText(cap(selectedDate.format(titleFormat)));String s=prefs.getString(key(selectedDate),"");if(s.isEmpty())selectedShift.setText("Није унесена смјена");else{String[] p=s.split("\\|");long duration=duration(p[0],p[1]);long night=nightMinutes(p[0],p[1]);selectedShift.setText(p[0]+" – "+p[1]+"  •  "+fmt(duration)+"\nДневно: "+fmt(duration-night)+"   |   Ноћно: "+fmt(night));}String note=prefs.getString(noteKey(selectedDate),"");selectedNote.setText(note.isEmpty()?"📝 Напомена: није унесена":"📝 Напомена: "+note); }
    private void updateSummary(){long day=0,night=0;for(int d=1;d<=shownMonth.lengthOfMonth();d++){LocalDate date=shownMonth.atDay(d);String s=prefs.getString(key(date),"");if(!s.isEmpty()){String[] p=s.split("\\|");long dur=duration(p[0],p[1]),n=nightMinutes(p[0],p[1]);day+=dur-n;night+=n;}}dayTotal.setText(fmt(day));nightTotal.setText(fmt(night));allTotal.setText(fmt(day+night));}
    private long duration(String start,String end){int a=minutes(start),b=minutes(end);if(b<a)b+=1440;return b-a;}
    private long nightMinutes(String start,String end){int a=minutes(start),b=minutes(end);if(b<a)b+=1440;long n=0;for(int t=a;t<b;t++){int minute=t%1440;int h=minute/60;if(h>=22||h<6)n++;}return n;}
    private int minutes(String s){String[] p=s.split(":");return Integer.parseInt(p[0])*60+Integer.parseInt(p[1]);}
    private String fmt(long mins){return (mins/60)+" ч "+String.format(Locale.getDefault(),"%02d",mins%60)+" мин";}
    private String key(LocalDate d){return "shift_"+d.format(keyFormat);}
    private String noteKey(LocalDate d){return "note_"+d.format(keyFormat);}
    private String cap(String s){if(s==null||s.isEmpty())return s;return s.substring(0,1).toUpperCase(new Locale("sr","BA"))+s.substring(1);}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    private Button navButton(String s){Button b=new Button(this);b.setText(s);b.setTextSize(26);b.setTextColor(TEXT);b.setBackground(round(PANEL,12));b.setPadding(0,0,0,dp(4));return b;}
    private GradientDrawable round(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private LinearLayout.LayoutParams params(int w,int h){return new LinearLayout.LayoutParams(w,h);}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
