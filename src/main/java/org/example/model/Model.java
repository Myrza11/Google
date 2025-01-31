package org.example.model;

import org.example.model.css.CssParser;
import org.example.model.css.cssom.CssRule;
import org.example.model.css.cssom.CssTree;
import org.example.model.html.HtmlElement;
import org.example.model.html.HtmlParser;
import org.example.model.renderTree.MergeCssomDom;
import org.example.model.renderTree.RenderTree;
import org.example.model.socket.HttpResponse;
import org.example.model.socket.Socket;
import org.example.view.Viewer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Model {
    private final Viewer viewer;
    private final Socket socket;
    private final HtmlParser htmlParser;
    private final CssParser cssParser;
    private HttpResponse httpResponse;
    private MergeCssomDom mergeCssomDom;
    private boolean isUrlChanged;

    public Model(Viewer viewer) {
        this.viewer = viewer;
        socket = new Socket();
        htmlParser = new HtmlParser();
        cssParser = new CssParser();
        mergeCssomDom = new MergeCssomDom();
    }

    public void getHtml(String siteUrl) {
        viewer.getController().clearLinkAreas();
        httpResponse = socket.fetchHtmlWithCss(siteUrl);
        isUrlChanged = true;
        viewer.getCanvas().repaint();
    }

    public RenderTree parse() {
        HtmlElement dom = htmlParser.parseHtml(extractBodyContent(getHttpResponse().getHtmlBody()), socket.getBaseUrl());

        CssTree cssTree = cssParser.parse(getHttpResponse().getCssResources());
        RenderTree renderTree = mergeCssomDom.mergeCssomDom(dom, cssTree);

        for (CssRule css : cssTree.getRules()) {
            cssParser.findCssOfHtml(dom, css.getSelector(), css);
        }

        isUrlChanged = false;
        return renderTree;
    }

    private String extractBodyContent(String html) {
        Pattern bodyPattern = Pattern.compile("<body[^>]*>(.*?)</body>", Pattern.DOTALL);
        Matcher matcher = bodyPattern.matcher(html);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        System.err.println("Body not found");
        return null;
    }

    public HttpResponse getHttpResponse() {
        return httpResponse;
    }

    public String getBaseUrl() {
        return socket.getBaseUrl();
    }

    public boolean getIsUrlChanged() {
        return isUrlChanged;
    }
}
