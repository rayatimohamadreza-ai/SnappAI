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
    offline.setOnClickListener(v->{mode="offline";status.setText("آفلاین: مدل محلی");showModelManager();});
    send.setOnClickListener(v->{String q=input.getText().toString().trim();if(q.isEmpty())return;input.setText("");addMessage(q,true);if(mode.equals("offline")){if(!OfflineEngine.isReady()){addMessage("ابتدا مدل آفلاین را از مدیریت مدل دانلود و اجرا کن.",false);}else{addMessage("در حال تولید پاسخ آفلاین...",false);OfflineEngine.send(this,q,new OfflineEngine.Callback(){public void onSuccess(String answer){addMessage(answer,false);}public void onError(String error){addMessage(error,false);}});}}else sendOnline(q);});
    if(getApiKey()==null) new Handler().postDelayed(this::askKey,400);
  }

  private static final String SMOL_NAME="SmolLM2_135M_Instruct.litertlm";
  private static final String SMOL_URL="https://huggingface.co/litert-community/SmolLM2-135M-Instruct/resolve/749fc23bc8d7ef89db031d075004289c88f41c2f/SmolLM2_135M_Instruct.litertlm";
  private static final String QWEN_NAME="Qwen3-0.6B.litertlm";
  private static final String QWEN_URL="https://huggingface.co/litert-community/Qwen3-0.6B/resolve/main/Qwen3-0.6B.litertlm";
  private java.io.File modelFile(String name){java.io.File dir=new java.io.File(getFilesDir(),"models");if(!dir.exists())dir.mkdirs();return new java.io.File(dir,name);}
  void showModelManager(){
    String[] choices={"SmolLM2 135M — سبک، حدود ۱۴۳ مگابایت","Qwen3 0.6B — قوی‌تر، حدود ۶۱۴ مگابایت"};
    new AlertDialog.Builder(this).setTitle("انتخاب مدل آفلاین").setItems(choices,(d,which)->manageModel(which==0?SMOL_NAME:QWEN_NAME,which==0?SMOL_URL:QWEN_URL,which==0?143:614)).setNegativeButton("بستن",null).show();
  }
  void manageModel(String name,String url,int sizeMb){
    java.io.File f=modelFile(name);
    if(!f.exists()){
      new AlertDialog.Builder(this).setTitle("دانلود مدل").setMessage("حجم تقریبی: "+sizeMb+" مگابایت\nپس از دانلود، مدل روی خود گوشی اجرا می‌شود.").setNegativeButton("فعلاً نه",null).setPositiveButton("دانلود",(d,w)->downloadOfflineModel(name,url)).show();
      return;
    }
    String[] actions={"اجرای این مدل","حذف مدل برای آزادسازی حافظه"};
    new AlertDialog.Builder(this).setTitle(name).setItems(actions,(d,which)->{
      if(which==0)loadOfflineModel(f);
      else new AlertDialog.Builder(this).setTitle("حذف مدل").setMessage("فایل مدل از حافظه‌ی برنامه حذف شود؟").setNegativeButton("لغو",null).setPositiveButton("حذف",(a,b)->{if(OfflineEngine.isReady())OfflineEngine.close();boolean ok=f.delete();addMessage(ok?"مدل حذف شد و حافظه آزاد شد.":"حذف فایل انجام نشد.",false);status.setText("آفلاین: مدل انتخاب نشده");}).show();
    }).setNegativeButton("بستن",null).show();
  }
  void downloadOfflineModel(String name,String url){
    final android.app.ProgressDialog pd=new android.app.ProgressDialog(this);pd.setTitle("دریافت "+name);pd.setMessage("لطفاً تا پایان دانلود صبر کن...");pd.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);pd.setMax(100);pd.setProgress(0);pd.setCancelable(false);pd.show();
    new Thread(()->{
      java.io.File target=modelFile(name);java.io.File part=new java.io.File(target.getAbsolutePath()+".part");HttpURLConnection con=null;
      try{
        URL u=new URL(url);con=(HttpURLConnection)u.openConnection();con.setInstanceFollowRedirects(true);con.setConnectTimeout(20000);con.setReadTimeout(120000);con.setRequestProperty("User-Agent","DastiarAI-Android");int code=con.getResponseCode();if(code<200||code>=300)throw new Exception("خطای دانلود: HTTP "+code);
        int total=con.getContentLength();long done=0;
        try(InputStream in=con.getInputStream();FileOutputStream out=new FileOutputStream(part)){byte[] buf=new byte[1024*64];int n;while((n=in.read(buf))!=-1){out.write(buf,0,n);done+=n;if(total>0){int p=(int)(done*100/total);runOnUiThread(()->pd.setProgress(p));}}out.getFD().sync();}
        if(done<100L*1024L*1024L)throw new Exception("حجم فایل کمتر از حد انتظار است؛ دانلود کامل نیست.");
        if(target.exists())target.delete();if(!part.renameTo(target))throw new Exception("ذخیره‌ی فایل مدل ناموفق بود.");
        runOnUiThread(()->{pd.dismiss();addMessage("دانلود "+name+" کامل شد. اکنون مدل را بارگذاری می‌کنم.",false);loadOfflineModel(target);});
      }catch(Exception e){part.delete();runOnUiThread(()->{pd.dismiss();new AlertDialog.Builder(this).setTitle("دانلود ناموفق").setMessage(e.getMessage()+"\nاتصال اینترنت را بررسی کن و دوباره تلاش کن.").setPositiveButton("باشه",null).show();});}
      finally{if(con!=null)con.disconnect();}
    }).start();
  }
  void loadOfflineModel(java.io.File file){
    status.setText("آفلاین: در حال بارگذاری مدل...");
    addMessage("در حال آماده‌سازی مدل روی گوشی؛ بار اول ممکن است کمی طول بکشد.",false);
    OfflineEngine.load(this,file.getAbsolutePath(),new OfflineEngine.Callback(){
      public void onSuccess(String message){status.setText("آفلاین: آماده‌ی گفت‌وگو");addMessage(message,false);}
      public void onError(String error){status.setText("آفلاین: بارگذاری ناموفق");addMessage(error,false);}
    });
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
