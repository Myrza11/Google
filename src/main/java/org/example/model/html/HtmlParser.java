package org.example.model.html;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlParser {
    public HtmlElement parseHtml(String html, String url){
        HtmlElement root = new HtmlElement("root", "");
        List<HtmlElement> stack = new ArrayList<>();
        stack.add(root);

        Pattern pattern = Pattern.compile("(<[^>]+>)|([^<]+)");
        Matcher matcher = pattern.matcher(html);

        while (matcher.find()) {
            String tag = matcher.group(1);
            String text = matcher.group(2);

            if (tag != null) {
                if (tag.startsWith("<!--") || tag.startsWith("<!DOCTYPE")) {
                    continue;
                }
                else if (tag.startsWith("<") && !tag.startsWith("</")) {
                    String tagName = tag.substring(1, tag.indexOf(" ") != -1 ? tag.indexOf(" ") : tag.length() - (tag.endsWith("/") ? 2 : 1)).trim();
                    HtmlElement element = new HtmlElement(tagName, "");

                    element.setClasses(extractClasses(tag));
                    extractStyles(tag, element);
                    element.setIds(extractIds(tag));

                    if (tagName.equals("img")) {
                        String src = extractAttribute(tag, "src");
                        if (src.startsWith("http") || src.startsWith("https")) {
                            element.setContent(src);
                        } else {
                            if (url != null) {
                                src = url + src;
                                src = src.replace(" ", "%20");
                                element.setContent(src);
                            } else {
                                element.setContent(src);
                            }

                        }
                    } else if (tagName.equals("a")) {
                        String href = extractAttribute(tag, "href");
                        element.setContent(href);
                    }

                    stack.get(stack.size() - 1).addChild(element);

                    if (!tag.endsWith("/>") && !isSelfClosingTag(tagName)) {
                        stack.add(element);
                    }
                } else if (tag.startsWith("</")) {
                    stack.remove(stack.size() - 1);
                }
            }

            if (text != null && !text.trim().isEmpty()) {
                HtmlElement textElement = new HtmlElement("text", text);
                stack.get(stack.size() - 1).addChild(textElement);
            }
        }

        return root;
    }
    private static void extractStyles(String tag, HtmlElement element) {
        String pattern = "style=\"([^\"]*)\"";
        Pattern stylePattern = Pattern.compile(pattern);
        Matcher styleMatcher = stylePattern.matcher(tag);
        if (styleMatcher.find()) {
            String[] properties = styleMatcher.group(1).trim().split(";");
            for (String property : properties) {
                String[] keyValue = property.split(":", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String value = keyValue[1].trim();
                    element.setStyles(key, value);
                }
            }
        }
    }
    private static String[] extractIds(String tag) {
        String pattern = "id=\"([^\"]*)\"";
        Pattern idPattern = Pattern.compile(pattern);
        Matcher idMatcher = idPattern.matcher(tag);
        if (idMatcher.find()) {
            return idMatcher.group(1).split("\\s+");
        }
        return null;
    }
    private static String extractAttribute(String tag, String attributeName) {
        String pattern = attributeName + "=\"([^\"]*)\"";
        Pattern attrPattern = Pattern.compile(pattern);
        Matcher attrMatcher = attrPattern.matcher(tag);
        if (attrMatcher.find()) {
            return attrMatcher.group(1);
        }
        return null;
    }

    private static boolean isSelfClosingTag(String tagName) {
        return tagName.equals("img") || tagName.equals("br") || tagName.equals("hr") || tagName.equals("input") || tagName.equals("meta") || tagName.equals("link");
    }

    private static String[] extractClasses(String tag) {
        String pattern = "class=\"([^\"]*)\"";
        Pattern classPattern = Pattern.compile(pattern);
        Matcher classMatcher = classPattern.matcher(tag);
        if (classMatcher.find()) {
            return classMatcher.group(1).split("\\s+");
        }
        return null;
    }

}