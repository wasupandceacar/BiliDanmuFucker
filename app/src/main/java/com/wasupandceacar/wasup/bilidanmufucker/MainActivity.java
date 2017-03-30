package com.wasupandceacar.wasup.bilidanmufucker;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class MainActivity extends AppCompatActivity {
    //进度条
    ProgressDialog dialog;

    //弹幕
    String danmu="";

    //关键词
    String keyword="";

    //匹配结果
    List<String> result=new ArrayList<>();

    //UID列表
    List<String> uidList=new ArrayList<>();

    //昵称列表
    List<String> nameList=new ArrayList<>();

    //匹配弹幕列表
    List<String> danmuList=new ArrayList<>();

    //判断整数
    public boolean isNumeric(String str){
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        if( !isNum.matches() ){
            return false;
        }
        return true;
    }

    //转换hash码
    public String unHash(String hash){
        String hashurl="http://biliquery.typcn.com/api/user/hash/"+hash;
        try {
            Document document = Jsoup.connect(hashurl).ignoreContentType(true).get();
            String hashdata=document.toString();
            Pattern pattern = Pattern.compile("id\":(.*?)\\}");
            Matcher matcher = pattern.matcher(hashdata);
            if(matcher.find()){
                return matcher.group(1);
            }else{
                return  "";
            }
        } catch (IOException e) {
            Toast toast = Toast.makeText(getApplicationContext(), "未知错误", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            e.printStackTrace();
            return null;
        }
    }

    //设置昵称
    public String getName(String uid){
        if(uid.equals("")){
            return "非会员弹幕";
        }
        String mainpageurl="http://space.bilibili.com/"+uid;
        try {
            Document document = Jsoup.connect(mainpageurl).ignoreContentType(true).get();
            String mainpagedata=document.toString();
            Pattern pattern = Pattern.compile("<title>(.*?)的个人空间");
            Matcher matcher = pattern.matcher(mainpagedata);
            if(matcher.find()){
                return matcher.group(1);
            }else{
                return  null;
            }
        } catch (IOException e) {
            Toast toast = Toast.makeText(getApplicationContext(), "未知错误", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            e.printStackTrace();
            return null;
        }
    }

    //表格填充
    public void setTable() throws InterruptedException {
        LoadingTask T=new LoadingTask();
        T.execute(100);
    }

    class LoadingTask extends AsyncTask<Integer, Integer, String> {
        //列表元素
        List<HashMap<String, String>> viewList = new ArrayList<>();
        //后面尖括号内分别是参数（例子里是线程休息时间），进度(publishProgress用到)，返回值 类型

        @Override
        protected void onPreExecute() {
            //加载弹幕信息进度条
            dialog=new ProgressDialog(MainActivity.this);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setIndeterminate(false);
            dialog.setCancelable(false);
            dialog.setMessage("正在加载弹幕");
            dialog.setMax(result.size());
            dialog.setProgress(0);
            dialog.show();
            //设置整个列表
            uidList.clear();
            nameList.clear();
            viewList.clear();
        }

        @Override
        protected String doInBackground(Integer... params) {
            for(int i=0;i<result.size();i++){
                //获取uid
                String uid=unHash(result.get(i));
                uidList.add(uid);
                //获取昵称
                String name=getName(uid);
                nameList.add(name);
                //加入列表
                HashMap<String, String> map = new HashMap<>();
                if(uidList.get(i).equals("")){
                    map.put("itemTitle", name);
                }else{
                    map.put("itemTitle", name+"("+uid+")");
                }
                map.put("itemText", danmuList.get(i));
                viewList.add(map);
                //更新进度条
                dialog.setProgress(dialog.getProgress()+1);
            }
            return "";
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
        }

        @Override
        protected void onPostExecute(String result) {
            if (dialog.getProgress() >= uidList.size()) {
                //消失
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                dialog.dismiss();
            }
            SimpleAdapter adapter = new SimpleAdapter(MainActivity.this,
                    viewList,//数据源
                    R.layout.my_listitem,//显示布局
                    new String[] {"itemTitle", "itemText"}, //数据源的属性字段
                    new int[] {R.id.itemTitle,R.id.itemText}); //布局里的控件id
            //添加并且显示
            ListView Danmuview=(ListView)findViewById(R.id.DanmuView);
            Danmuview.setAdapter(adapter);
            Danmuview.setOnItemClickListener(new OnItemClickListener(){
                //list点击事件
                @Override
                public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4)
                {
                    for(int i=0;i<uidList.size();i++){
                        if(p3==i){
                            if(uidList.get(i).equals("")){
                                Toast toast = Toast.makeText(getApplicationContext(), "该弹幕为非会员弹幕！", Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.CENTER, 0, 0);
                                toast.show();
                            }else{
                                Uri uri = Uri.parse("http://space.bilibili.com/"+uidList.get(i));
                                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                startActivity(intent);
                            }
                        }
                    }
                }
            });
        }

    }

    public void Read(View view){
        EditText ReadLine=(EditText)findViewById(R.id.ReadLine);
        String avid=ReadLine.getText().toString();
        if(isNumeric(avid)){
            String cidurl="http://biliquery.typcn.com/api/cid/"+avid+"/1";
            try {
                Document document=Jsoup.connect(cidurl).ignoreContentType(true).get();
                String ciddata=document.toString();
                Pattern pattern = Pattern.compile("cid\":\"(.*?)\"\\}");
                Matcher matcher = pattern.matcher(ciddata);
                if (matcher.find()) {
                    String cid=matcher.group(1);
                    String xmlurl="http://comment.bilibili.com/"+cid+".xml";
                    URL url = new URL(xmlurl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("Accept-Encoding", "gzip,deflate");
                    conn.connect();

                    InputStream in = conn.getInputStream();
                    InputStream flin = new InflaterInputStream(in, new Inflater(true));

                    Scanner sc = new Scanner(flin, "utf-8");
                    if(!danmu.equals("")){
                        danmu="";
                    }
                    while(sc.hasNext())
                        danmu += sc.nextLine()+"\n";
                    sc.close();
                    in.close();
                    if (!danmu.equals("")){
                        TextView ReadText=(TextView)findViewById(R.id.ReadText);
                        ReadText.setText("已读取弹幕");
                        ReadText.setTextColor(Color.BLUE);
                    }else{
                        TextView ReadText=(TextView)findViewById(R.id.ReadText);
                        ReadText.setText("读取弹幕失败");
                        ReadText.setTextColor(Color.RED);
                    }
                }
            } catch (Exception e) {
                Toast toast = Toast.makeText(getApplicationContext(), "未知错误", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                e.printStackTrace();
            }
        }else{
            Toast toast = Toast.makeText(getApplicationContext(), "请输入纯数字的av号！", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }

    public void Search(View view) throws InterruptedException {
        if(!keyword.equals("")){
            keyword="";
        }
        EditText SearchLine=(EditText)findViewById(R.id.SearchLine);
        //获取hash码
        keyword=SearchLine.getText().toString();
        Pattern pattern = Pattern.compile(",0,(.*?),.*?"+keyword+".*?</d>");
        Matcher matcher = pattern.matcher(danmu);
        result.clear();
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        if(result.isEmpty()){
            Toast toast = Toast.makeText(getApplicationContext(), "未搜索到包含该关键词的弹幕！", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }else{
            //获取弹幕列表
            danmuList.clear();
            Pattern danmupattern = Pattern.compile("\">(.*?"+keyword+".*?)</d>");
            Matcher danmumatcher = danmupattern.matcher(danmu);
            while (danmumatcher.find()) {
                danmuList.add(danmumatcher.group(1));
            }
            //设置弹幕列表
            setTable();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
    }
}
