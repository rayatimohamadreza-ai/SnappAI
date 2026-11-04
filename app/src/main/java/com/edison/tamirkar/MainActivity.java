package com.edison.tamirkar;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import org.json.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
  private final int bg=Color.rgb(11,18,34), panel=Color.rgb(23,34,56), cyan=Color.rgb(62,218,216), white=Color.WHITE;
  private LinearLayout root, messages;
  private EditText input;
  private TextView status;
  private String mode="online";
  private static final String PREF="secure_config", KEY_ALIAS="dastiar_master_key";
  @Override public void onCreate(Bundle b){super.onCreate(b); getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);showHome();}
  int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
  GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
  TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
  void showHome(){
    root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(16),dp(12),dp(16),dp(10));root.setBackgroundColor(bg);setContentView(root);
    TextView title=label("دستیار اس ممد",24,white);title.setTypeface(null,Typeface.BOLD);title.setGravity(Gravity.CENTER);root.addView(title,new LinearLayout.LayoutParams(-1,dp(54)));
    TextView sub=label("دستیار هوشمند مستقل",13,0xffaabbd2);sub.setGravity(Gravity.CENTER);root.addView(sub,new LinearLayout.LayoutParams(-1,dp(25)));
    LinearLayout modes=new LinearLayout(this);modes.setOrientation(0);modes.setPadding(0,dp(12),0,dp(10));root.addView(modes);
    Button online=button("آنلاین");Button offline=button("آفلاین");modes.addView(online,new LinearLayout.LayoutParams(0,dp(46),1));modes.addView(offline,new LinearLayout.LayoutParams(0,dp(46),1));
    status=label("آنلاین: اتصال از طریق OpenRouter",12,0xff91a7c5);root.addView(status,new LinearLayout.LayoutParams(-1,dp(30)));
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,0,1);root.addView(scroll,sp);
    messages=new LinearLayout(this);messages.setOrientation(1);messages.setPadding(0,dp(8),0,dp(8));scroll.addView(messages);
    addMessage("سلام! من دستیار اس ممد هستم. حالت آنلاین یا آفلاین را انتخاب کن.",false);
    LinearLayout composer=new LinearLayout(this);composer.setGravity(Gravity.CENTER_VERTICAL);composer.setPadding(0,dp(8),0,0);root.addView(composer);
    input=new EditText(this);input.setSingleLine(false);input.setMinLines(1);input.setMaxLines(3);input.setTextColor(white);input.setHintTextColor(0xff8193ad);input.setHint("پیامت را بنویس...");input.setPadding(dp(12),dp(8),dp(12),dp(8));input.setBackground(shape(panel,14));composer.addView(input,new LinearLayout.LayoutParams(0,dp(52),1));
    Button send=button("ارسال");LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(dp(82),dp(52));bp.leftMargin=dp(8);composer.addView(send,bp);
    online.setOnClickListener(v->{mode="online";status.setText("آنلاین: اتصال از طریق OpenRouter");addMessage("حالت آنلاین انتخاب شد.",false);});
    offline.setOnClickListener(v->{mode="offline";status.setText("آفلاین: موتور مدل محلی هنوز در مرحله اتصال است");new AlertDialog.Builder(this).setTitle("حالت آفلاین").setMessage("در این نسخه، مدیریت و اجرای مدل محلی هنوز تکمیل نشده است. برای پاسخ آفلاین باید مدل سازگار دانلود و موتور LiteRT-LM به برنامه متصل شود.").setPositiveButton("متوجه شدم",null).show();});
    send.setOnClickListener(v->{String q=input.getText().toString().trim();if(q.isEmpty())return;input.setText("");addMessage(q,true);if(mode.equals("offline")){addMessage("موتور پاسخ‌گویی آفلاین هنوز در این نسخه فعال نشده است. برای پاسخ، حالت آنلاین را انتخاب کن.",false);}else sendOnline(q);});
    if(getApiKey()==null) new Handler().postDelayed(this::askKey,400);
  }
  Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(bg);b.setAllCaps(false);b.setBackground(shape(cyan,14));return b;}
  void addMessage(String s,boolean user){TextView t=label((user?"شما: ":"دستیار: ")+s,15,white);t.setGravity(Gravity.RIGHT);t.setPadding(dp(13),dp(11),dp(13),dp(11));t.setBackground(shape(user?0xff254b63:panel,15));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(9);messages.addView(t,p);}
  void askKey(){
    EditText e=new EditText(this);e.setSingleLine(true);e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);e.setHint("کلید OpenRouter");
    new AlertDialog.Builder(this).setTitle("راه‌اندازی اتصال آنلاین").setMessage("کلید API را فقط یک‌بار وارد کن. کلید در فضای خصوصی برنامه به‌صورت رمزگذاری‌شده ذخیره می‌شود.").setView(e).setPositiveButton("ذخیره", (d,w)->{String k=e.getText().toString().trim();if(!k.isEmpty())try{saveApiKey(k);addMessage("کلید به‌صورت رمزگذاری‌شده ذخیره شد.",false);}catch(Exception ex){addMessage("ذخیره کلید ناموفق بود: "+ex.getMessage(),false);}}).setNegativeButton("بعداً",null).show();
  }
  SecretKey getOrCreateKey() throws Exception {
    java.security.KeyStore ks=java.security.KeyStore.getInstance("AndroidKeyStore");ks.load(null);
    if(ks.containsAlias(KEY_ALIAS))return (SecretKey)ks.getKey(KEY_ALIAS,null);
    KeyGenerator kg=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
    kg.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
    return kg.generateKey();
  }
  void saveApiKey(String key)throws Exception{
    Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,getOrCreateKey());byte[] iv=c.getIV();byte[] enc=c.doFinal(key.getBytes(StandardCharsets.UTF_8));
    getSharedPreferences(PREF,0).edit().putString("iv",android.util.Base64.encodeToString(iv,0)).putString("data",android.util.Base64.encodeToString(enc,0)).apply();
  }
  String getApiKey(){
    try{android.content.SharedPreferences p=getSharedPreferences(PREF,0);String ivs=p.getString("iv",null),ds=p.getString("data",null);if(ivs==null||ds==null)return null;Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,getOrCreateKey(),new GCMParameterSpec(128,android.util.Base64.decode(ivs,0)));return new String(c.doFinal(android.util.Base64.decode(ds,0)),StandardCharsets.UTF_8);}catch(Exception e){return null;}
  }
  void sendOnline(String prompt){
    String api=getApiKey();if(api==null){askKey();return;}
    addMessage("در حال دریافت پاسخ...",false);
    new Thread(()->{
      HttpURLConnection con=null;
      try{
        URL url=new URL("https://openrouter.ai/api/v1/chat/completions");con=(HttpURLConnection)url.openConnection();con.setRequestMethod("POST");con.setConnectTimeout(25000);con.setReadTimeout(60000);con.setDoOutput(true);con.setRequestProperty("Authorization","Bearer "+api);con.setRequestProperty("Content-Type","application/json");
        JSONObject body=new JSONObject();body.put("model","openrouter/free");JSONArray arr=new JSONArray();arr.put(new JSONObject().put("role","user").put("content",prompt));body.put("messages",arr);
        try(OutputStream os=con.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));}
        int code=con.getResponseCode();InputStream is=code>=200&&code<300?con.getInputStream():con.getErrorStream();StringBuilder out=new StringBuilder();try(BufferedReader r=new BufferedReader(new InputStreamReader(is,StandardCharsets.UTF_8))){String line;while((line=r.readLine())!=null)out.append(line);}
        JSONObject json=new JSONObject(out.toString());String answer=code>=200&&code<300?json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content","پاسخ خالی دریافت شد"): "خطای سرویس ("+code+"): "+json.optString("error",out.toString());
        runOnUiThread(()->addMessage(answer,false));
      }catch(Exception ex){runOnUiThread(()->addMessage("خطا در ارتباط: "+ex.getMessage(),false));}finally{if(con!=null)con.disconnect();}
    }).start();
  }
}
