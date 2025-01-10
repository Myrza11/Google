package org.example.model.html;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HtmlElement {
    private String tag;
    private String content;
    private String[] classes;
    private String[] ids;
    private HashMap<String, String> styles;
    private List<HtmlElement> children;
    private HashMap<String, String> css;

    public HtmlElement(String tag, String content) {
        this.tag = tag;
        this.content = content;
        this.children = new ArrayList<>();
        this.ids = new String[0];
        this.classes = new String[0];
        this.styles = new HashMap<>();
        this.css = new HashMap<>();
    }

    public void setCssRule(String key, String value) {
        this.css.put(key, value);
    }

    public HashMap<String, String> getCssRule() {
        return css;
    }

    public void setStyles(String key, String value) {
        this.styles.put(key, value);
    }

    public HashMap<String, String> getStyles() {
        return styles;
    }

    public void addChild(HtmlElement child) {
        children.add(child);
    }

    public String getTag() {
        return tag;
    }

    public String getContent() {
        return content;
    }

    public String[] getClasses() {
        return classes;
    }

    public void setClasses(String[] classes) {
        this.classes = classes;
    }

    public String[] getIds() {
        return ids;
    }

    public HtmlElement setIds(String[] ids) {
        this.ids = ids;
        return this;
    }

    public List<HtmlElement> getChildren() {
        return children;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
