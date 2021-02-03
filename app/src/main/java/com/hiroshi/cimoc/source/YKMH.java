package com.hiroshi.cimoc.source;

import android.util.Log;

import com.hiroshi.cimoc.model.Chapter;
import com.hiroshi.cimoc.model.Comic;
import com.hiroshi.cimoc.model.ImageUrl;
import com.hiroshi.cimoc.model.Source;
import com.hiroshi.cimoc.parser.MangaParser;
import com.hiroshi.cimoc.parser.NodeIterator;
import com.hiroshi.cimoc.parser.SearchIterator;
import com.hiroshi.cimoc.soup.Node;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.util.List;

import okhttp3.Request;

public class YKMH extends MangaParser {
    public static final int TYPE = 91;
    public static final String DEFAULT_TITLE = "优酷漫画";
    public final String Host = "https://www.ykmh.com/";
    public final String mHost = "https://m.ykmh.com/";

    public YKMH(Source source) {
        init(source, null);
    }
    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException {
        Log.d("SourceSearch:", String.valueOf(keyword));

        return new Request.Builder().url(mHost + "search/?keywords=" + keyword + "&page=" + page).addHeader("referer", "https://www.cocomanhua.com/17132/").addHeader("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.96 Safari/537.36")
                .build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) throws JSONException {
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
    public Request getInfoRequest(String cid) {
        return null;
    }

    @Override
    public void parseInfo(String html, Comic comic) throws UnsupportedEncodingException {

    }

    @Override
    public List<Chapter> parseChapter(String html) {
        return null;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        return null;
    }

    @Override
    public List<ImageUrl> parseImages(String html) {
        return null;
    }
}
