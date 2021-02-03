package com.hiroshi.cimoc.source;

import android.util.Log;
import android.util.Pair;

import com.hiroshi.cimoc.model.Chapter;
import com.hiroshi.cimoc.model.Comic;
import com.hiroshi.cimoc.model.ImageUrl;
import com.hiroshi.cimoc.model.Source;
import com.hiroshi.cimoc.parser.Category;
import com.hiroshi.cimoc.parser.MangaCategory;
import com.hiroshi.cimoc.parser.MangaParser;
import com.hiroshi.cimoc.parser.NodeIterator;
import com.hiroshi.cimoc.parser.SearchIterator;
import com.hiroshi.cimoc.soup.Node;
import com.hiroshi.cimoc.utils.StringUtils;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;


import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.Headers;
import okhttp3.Request;

public class CocoComic extends MangaParser {
    public static final int TYPE = 90;
    public static final String DEFAULT_TITLE = "CoCo漫画";
    public final String Host = "https://www.cocomanhua.com/";
    public CocoComic(Source source) {
        init(source, null);
    }
    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException {
        Log.d("SourceS", String.valueOf(keyword));

        return new Request.Builder().url(Host + "search?searchString=" + keyword + "&page=" + page).addHeader("referer", "https://www.cocomanhua.com/17132/").addHeader("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.96 Safari/537.36")
                .build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) throws JSONException {
        Log.d("SourceS", String.valueOf(page));
//                    Document document = Jsoup.parse(html);
////           document.getElementsByTag("dl");
//            Elements elements = document.getElementsByTag("dl");
        Node body = new Node(html);
        return new NodeIterator(body.list("dl")) {
            @Override
            protected Comic parse(Node node) {
                String cid = node.hrefWithSubString("dt > a",1,-2);
                String title = node.text("h1");
                String cover = node.attr("dt > a", "data-original");
                String Update = node.getParent("span:containsOwn(更新)").text().replace("更新", "");
                String Author = node.getParent("span:containsOwn(作者)").text().replace("作者", "");
                return new Comic(TYPE, cid, title, cover, Update, Author);
            }
        };


    }

    @Override
    public String getUrl(String cid) {
        return Host.concat(cid);
    }

    @Override
    public Request getInfoRequest(String cid) {
        Log.d("SourceS", String.valueOf(cid));

        return new Request.Builder()
                .url(Host.concat(cid)).addHeader("referer", "https://www.cocomanhua.com/17132/").addHeader("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.96 Safari/537.36")
                .build();
    }

    @Override
    public void parseInfo(String html, Comic comic) throws UnsupportedEncodingException {
        Node body = new Node(html).getChild("dl");
        String title = body.text("dd > h1");
        String cover = body.attr("dt > a", "data-original");
        String update = body.text("dd > ul span:containsOwn(更新) + a");
        String author = body.text("dd > ul span:containsOwn(作者) + a");
        String intro = body.getParent("dd > ul span:containsOwn(简介)").text();
        String isFinish = body.text("dd > ul span:containsOwn(更新) + a");
        boolean finish = false;
        if (isFinish.contains("完结")) finish = true;
        comic.setInfo(title, cover, update, intro, author, finish);
    }

    @Override
    public List<Chapter> parseChapter(String html) {
        List<Chapter> list = new LinkedList<>();
        Node body = new Node(html);
        for (Node node : body.list("div.fed-play-item.fed-drop-item.fed-visible div.all_data_list ul > li > a")) {
            String title = node.text();
//            String path = StringUtils.split(node.href(), "/", 3);
            String path = node.hrefWithSubString(1);
            list.add(new Chapter(title, path));
        }
        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        return new Request.Builder()
                .url(Host.concat(path)).addHeader("referer", "https://www.cocomanhua.com/17132/").addHeader("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.96 Safari/537.36")
                .build();
    }

    @Override
    public Headers getHeader() {
        return Headers.of("Referer", "https://www.cocomanhua.com/17132/", "user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.96 Safari/537.36");
    }

    public static String decode(String words, String key) {
        try {
            if (words.length() == 0) {
                return "";
            }
            System.out.println(words);
            byte[] messageByte = Base64.decodeBase64(words);
            return AES.decrypt(new String(messageByte), key);
        } catch (Exception e) {
            e.printStackTrace();
            return "";

        }
    }

    @Override
    public List<ImageUrl> parseImages(String html) {
        List<ImageUrl> list = new ArrayList<>();

        Matcher matcher = Pattern.compile("var C_DATA='(.+?)'").matcher(html);
        if (!matcher.find()) {
            return null;
        }
        String CDATA = matcher.group(1);
        Log.d("SourcePic",CDATA);

        String info = decode(CDATA, "fw122587mkertyui");
        Matcher matcherInfo= null;
        try {
            Pattern matcherP = Pattern.compile("mh_info=\\{startimg:(.*?),enc_code1:\"(.+?)\",mhid:\"(.*?)\",enc_code2:\"(.*?)\",mhname:\"(.*?)\",pageid:(.+?),pagename:\"(.+?)\",pageurl:\"(.*?)\",(.*?),domain:\"(.*?)\"");
             matcherInfo=matcherP.matcher(info);
        }catch (Exception e){
            Log.e("Error","Err",e);
        }

        if (!matcherInfo.find()) {
            return null;
        }

        int imgStart = matcherInfo.group(1).equals("") ? 1 : Integer.parseInt(matcherInfo.group(1));
        String enc1 = matcherInfo.group(2);
        String enc2 = matcherInfo.group(4);
        String BookName = matcherInfo.group(5);
        String CName = matcherInfo.group(7);
        String Domain = null;
        try {
            Domain = matcherInfo.group(10);
        } catch (Exception e) {
            e.printStackTrace();
            Domain = "img.cocomanhua.com";
        }
        int PageNum = 0;
        try {
            PageNum = Integer.parseInt(decode(enc1, "fw122587mkertyui"));
            System.out.println(PageNum);

        } catch (Exception e) {
            e.printStackTrace();
            // todo

        }
        String urlPart = "";
        try {
            urlPart = "https://" + Domain + "/comic/" + decode(enc2, "fw125gjdi9ertyui");
            System.out.println(urlPart);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("SourcePic",urlPart);

        for (int i = imgStart; i <= PageNum; i++) {
            list.add(new ImageUrl(i, StringUtils.format("%s%04d.jpg", urlPart, i), false));
            System.out.println();
        }


        return list;
    }
// TODO 目录功能是瞎写的
    private static class Category extends MangaCategory {

        @Override
        public boolean isComposite() {
            return true;
        }

        @Override
        public String getFormat(String... args) {
            String path = args[CATEGORY_SUBJECT].concat(" ").concat(args[CATEGORY_AREA]).concat(" ").concat(args[CATEGORY_PROGRESS])
                    .concat(" ").concat(args[CATEGORY_ORDER]).trim();
            path = path.replaceAll("\\s+", "-");
            return StringUtils.format("http://www.dm5.com/manhua-list-%s-p%%d", path);
        }

        @Override
        protected List<Pair<String, String>> getSubject() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", ""));
            list.add(Pair.create("热血", "tag31"));
            list.add(Pair.create("恋爱", "tag26"));
            list.add(Pair.create("校园", "tag1"));
            list.add(Pair.create("百合", "tag3"));
            list.add(Pair.create("耽美", "tag27"));
            list.add(Pair.create("冒险", "tag2"));
            list.add(Pair.create("后宫", "tag8"));
            list.add(Pair.create("科幻", "tag25"));
            list.add(Pair.create("战争", "tag12"));
            list.add(Pair.create("悬疑", "tag17"));
            list.add(Pair.create("推理", "tag33"));
            list.add(Pair.create("搞笑", "tag37"));
            list.add(Pair.create("奇幻", "tag14"));
            list.add(Pair.create("魔法", "tag15"));
            list.add(Pair.create("恐怖", "tag29"));
            list.add(Pair.create("神鬼", "tag20"));
            list.add(Pair.create("历史", "tag4"));
            list.add(Pair.create("同人", "tag30"));
            list.add(Pair.create("运动", "tag34"));
            list.add(Pair.create("绅士", "tag36"));
            list.add(Pair.create("机战", "tag40"));
            return list;
        }

        @Override
        protected boolean hasArea() {
            return true;
        }

        @Override
        protected List<Pair<String, String>> getArea() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", ""));
            list.add(Pair.create("港台", "area35"));
            list.add(Pair.create("日韩", "area36"));
            list.add(Pair.create("内地", "area37"));
            list.add(Pair.create("欧美", "area38"));
            return list;
        }

        @Override
        public boolean hasProgress() {
            return true;
        }

        @Override
        public List<Pair<String, String>> getProgress() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", ""));
            list.add(Pair.create("连载", "st1"));
            list.add(Pair.create("完结", "st2"));
            return list;
        }

        @Override
        protected boolean hasOrder() {
            return true;
        }

        @Override
        protected List<Pair<String, String>> getOrder() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("更新", "s2"));
            list.add(Pair.create("人气", ""));
            list.add(Pair.create("新品上架", "s18"));
            return list;
        }

    }
}

class AES {
    static {
        if (Security.getProvider("BC") == null) {
            Security
                    .addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        } else {
            Security.removeProvider("BC");
            Security
                    .addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        }
    }

    /**
     * 密钥算法
     */
    private static final String KEY_ALGORITHM = "AES";
    /**
     * 加密/解密算法 / 工作模式 / 填充方式
     */
    private static final String CIPHER_ALGORITHM = "AES/ECB/PKCS7Padding";


    public static byte[] keyFormat(String key) {
        byte[] keyBytes = key.getBytes();
        //java不支持pkcs7 补齐方式，因此需要人为补齐
        int base = 16;
        if (keyBytes.length % base != 0) {
            int groups = keyBytes.length / base + (keyBytes.length % base != 0 ? 1 : 0);
            byte[] temp = new byte[groups * base];
            Arrays.fill(temp, (byte) 0);
            System.arraycopy(keyBytes, 0, temp, 0, keyBytes.length);
            keyBytes = temp;
        }
        // 初始化

        return keyBytes;
    }


    /**
     * AES 解密操作
     *
     * @param content
     * @param key
     * @return
     */
    public static String decrypt(String content, String key) {
        try {
            byte[] sourceBytes = Base64.decodeBase64(content);
            byte[] keyBytes = keyFormat(key);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, KEY_ALGORITHM));
            byte[] decoded = cipher.doFinal(sourceBytes);
            return new String(decoded, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return "err";
    }


}
