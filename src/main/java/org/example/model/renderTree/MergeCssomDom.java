package org.example.model.renderTree;

import org.example.model.css.cssom.CssRule;
import org.example.model.css.cssom.CssTree;
import org.example.model.html.HtmlElement;

import javax.imageio.ImageIO;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.awt.Canvas;
import java.awt.FontMetrics;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MergeCssomDom {
    private RenderTree renderTree;
    private final HashMap<String, Integer> tagSize = new HashMap<>() {{
        put("h1", 32);
        put("h2", 24);
        put("h3", 18);
        put("h4", 16);
        put("h5", 13);
        put("h6", 10);
    }};
    private final Set<String> blockElements = Set.of(
            "div", "section", "article", "aside", "header", "footer", "main", "nav",
            "p", "h1", "h2", "h3", "h4", "h5", "h6", "pre", "blockquote",
            "ul", "ol", "li", "dl", "dt", "dd",
            "figure", "figcaption", "hr", "table", "caption", "thead", "tbody", "tfoot", "tr", "th", "td",
            "form", "fieldset", "legend", "textarea", "address", "details", "summary"
    );
    Set<String> inheritableStyles = new HashSet<>(Arrays.asList(
            "color",
            "cursor",
            "direction",
            "visibility",
            "quotes",
            "letter-spacing",
            "word-spacing",
            "line-height",
            "font",
            "font-family",
            "font-size",
            "font-style",
            "font-variant",
            "font-weight"
    ));

    public RenderTree mergeCssomDom(HtmlElement htmlElement, CssTree cssTree) {
        renderTree = new RenderTree();
        RenderNode rootRenderNode = new RenderNode(htmlElement.getTag());
        rootRenderNode.setWidth(renderTree.getWindowWidth());
        rootRenderNode.setHeight(renderTree.getWindowHeight());
        createRenderTree(htmlElement, rootRenderNode);

        for (CssRule cssRule : cssTree.getRules()) {
            findCssOfRenderTree(htmlElement, rootRenderNode, cssRule.getSelector(), cssRule);
        }

        setCssToChildren(rootRenderNode);
        setWidthHeightToChildren(rootRenderNode);
        hTagsInHeight(rootRenderNode);

        finalCalculationOfHeight(rootRenderNode);
        setXY(rootRenderNode);


        renderTree.setRoot(rootRenderNode);

        return renderTree;
    }

    @SuppressWarnings("unused")
    private void finalCalculationOfWidth(RenderNode rootRenderNode) {
        double currentWidth = 0;
        if (!blockElements.contains(rootRenderNode.getTagName())){
            for (RenderNode child : rootRenderNode.getChildren()) {
                if (child.getWidth() == 0){
                    finalCalculationOfWidth(child);
                }
                currentWidth += child.getWidth();
            }
            rootRenderNode.setWidth(currentWidth);
        }
        if (!rootRenderNode.getChildren().isEmpty()){
            for (RenderNode child : rootRenderNode.getChildren()) {
                finalCalculationOfWidth(child);
            }
        }
    }

    private void setXY(RenderNode renderNode) {
        double parentRemainWidth = renderNode.getWidth();
        double lastX = renderNode.getX();
        double lastY = renderNode.getY();
        double rowHeight = 0;

        for (RenderNode child : renderNode.getChildren()) {
            HashMap<String, String> styles = child.getAppliedStyles();
            double marginTop = parseStyleValue(styles.getOrDefault("margin-top", "0px"), parentRemainWidth);
            double marginBottom = parseStyleValue(styles.getOrDefault("margin-bottom", "0px"), parentRemainWidth);
            double marginLeft = parseStyleValue(styles.getOrDefault("margin-left", "0px"), parentRemainWidth);
            double marginRight = parseStyleValue(styles.getOrDefault("margin-right", "0px"), parentRemainWidth);
            double paddingTop = parseStyleValue(styles.getOrDefault("padding-top", "0px"), parentRemainWidth);
            double paddingBottom = parseStyleValue(styles.getOrDefault("padding-bottom", "0px"), parentRemainWidth);
            double paddingLeft = parseStyleValue(styles.getOrDefault("padding-left", "0px"), parentRemainWidth);
            double paddingRight = parseStyleValue(styles.getOrDefault("padding-right", "0px"), parentRemainWidth);

            if (parentRemainWidth >= child.getWidth() + marginLeft + marginRight + paddingLeft + paddingRight) {
                child.setX(lastX + marginLeft + paddingLeft);
                child.setY(lastY + marginTop + paddingTop);

                lastX += child.getWidth() + marginLeft + marginRight + paddingLeft + paddingRight;
                parentRemainWidth -= child.getWidth() + marginLeft + marginRight + paddingLeft + paddingRight;

                rowHeight = Math.max(rowHeight, child.getHeight() + marginTop + marginBottom + paddingTop + paddingBottom);
            } else {
                lastX = renderNode.getX();
                lastY += rowHeight;

                parentRemainWidth = renderNode.getWidth();

                child.setX(lastX + marginLeft + paddingLeft);
                child.setY(lastY + marginTop + paddingTop);

                lastX += child.getWidth() + marginLeft + marginRight + paddingLeft + paddingRight;
                parentRemainWidth -= child.getWidth() + marginLeft + marginRight + paddingLeft + paddingRight;

                rowHeight = child.getHeight() + marginTop + marginBottom + paddingTop + paddingBottom;
            }

            setXY(child);
        }
    }

    private double parseStyleValue(String value, double parentDimension) {
        if (value.endsWith("px")) {
            return Math.max(Double.parseDouble(value.replace("px", "")), 0);
        } else if (value.endsWith("%")) {
            return Math.max(Double.parseDouble(value.replace("%", "")) / 100 * parentDimension, 0);
        } else {
            return 0;
        }
    }


    private void finalCalculationOfHeight(RenderNode renderNode) {
        for (RenderNode child : renderNode.getChildren()){
            child.setHeight(calculateHeightOfChildren(child));
            finalCalculationOfHeight(child);
        }
    }
    private double calculateHeightOfChildren(RenderNode renderNode) {
        double height = 0;
        double remainingWidth = renderNode.getWidth();
        ArrayList<Double> rowHeights = new ArrayList<>();
        ArrayList<RenderNode> children = renderNode.getChildren();

        if ("img".equals(renderNode.getTagName())) {
            return getImageHeight(renderNode);
        }
        if (children == null) {
            return renderNode.getHeight();
        }
        for (RenderNode child : children) {
            if (child.getHeight() < 5) {
                child.setHeight(calculateHeightOfChildren(child));
            }

            if (remainingWidth >= child.getWidth()) {
                rowHeights.add(child.getHeight());
                remainingWidth -= child.getWidth();
            } else {
                if (!rowHeights.isEmpty()) {
                    height += getMaxHeight(rowHeights);
                    rowHeights.clear();
                }
                rowHeights.add(child.getHeight());
                remainingWidth = renderNode.getWidth() - child.getWidth();
            }
        }

        if (!rowHeights.isEmpty()) {
            height += getMaxHeight(rowHeights);
        }

        return height;
    }

    private double getImageHeight(RenderNode imgNode) {
        String imagePath = imgNode.getTextContent();

        if (imagePath == null || imagePath.isEmpty()) {
            return 0;
        }

        if (imagePath.toLowerCase().endsWith(".svg")) {
            return getSVGImageHeight(imagePath, imgNode);
        }

        try {


                URL url = new URL(imagePath);
                BufferedImage img = ImageIO.read(url);


            if (img != null) {
                HashMap<String, String> styles = imgNode.getAppliedStyles();
                if (styles.get("width") != null) {
                    double styleWidth = Double.parseDouble(styles.get("width").replace("px", "").replace("%", "").trim());
                    if (styles.get("width").contains("px") || styles.get("width").contains("%")) {
                        if (styles.get("width").contains("px")) {
                            imgNode.setWidth(styleWidth);
                            return img.getHeight() / (img.getWidth() / styleWidth);
                        } else if (styles.get("width").contains("%")) {
                            double nodeWidth = (imgNode.getParent().getWidth() / 100) * styleWidth;
                            imgNode.setWidth(nodeWidth);
                            return img.getHeight() / (img.getWidth() / nodeWidth);
                        }
                    }
                }
                imgNode.setWidth(img.getWidth());
                return img.getHeight();
            }

            return 0;
        } catch (IOException e) {
            e.getMessage();
            return 0;
        }
    }

    private double getSVGImageHeight(String imagePath, RenderNode imgNode) {
        double[] size = getSVGSize(imagePath);
        imgNode.setWidth(size[0]);
        return size[1];
    }

    private double[] getSVGSize(String imagePath) {
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc;

            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                @SuppressWarnings("deprecation")
                URL url = new URL(imagePath);
                doc = builder.parse(url.openStream());
            } else {
                doc = builder.parse(new File(imagePath));
            }

            org.w3c.dom.Element svgElement = doc.getDocumentElement();

            String widthStr = svgElement.getAttribute("width");
            String heightStr = svgElement.getAttribute("height");

            double width = parseSize(widthStr);
            double height = parseSize(heightStr);

            if (width == 0 || height == 0) {
                String viewBox = svgElement.getAttribute("viewBox");
                if (!viewBox.isEmpty()) {
                    String[] values = viewBox.split(" ");
                    if (values.length == 4) {
                        width = Double.parseDouble(values[2]);
                        height = Double.parseDouble(values[3]);
                    }
                }
            }

            return new double[]{width, height};
        } catch (Exception e) {
            e.printStackTrace();
            return new double[]{0, 0};
        }
    }

    private double parseSize(String size) {
        if (size == null || size.isEmpty()) return 0;
        return Double.parseDouble(size.replaceAll("[^0-9.]", ""));
    }

    private double getMaxHeight(ArrayList<Double> heights) {
        double maxHeight = 0;
        for (double h : heights) {
            if (h > maxHeight) {
                maxHeight = h;
            }
        }
        return maxHeight;
    }

    private void hTagsInHeight(RenderNode rootRenderNode) {
        for (RenderNode child : rootRenderNode.getChildren()) {
            if (tagSize.get(child.getTagName()) != null) {
                calculateHTags(child, child.getTagName());
            }
            hTagsInHeight(child);
        }
    }

    private void calculateHTags(RenderNode renderNode, String tagNameForSize) {
        int size = tagSize.get(tagNameForSize);
        if (size != -1) {
            renderNode.setHeight(size);
            for (RenderNode child : renderNode.getChildren()) {
                calculateHTags(child, tagNameForSize);
            }
        };
    }

    private void createRenderTree(HtmlElement htmlElement, RenderNode parentRenderNode) {
        for (HtmlElement child : htmlElement.getChildren()) {
            RenderNode renderNode = new RenderNode(child.getTag());
            parentRenderNode.addChild(renderNode);
            renderNode.setTextContent(child.getContent());
            renderNode.setStyles(child.getStyles());
            renderNode.setParent(parentRenderNode);
            createRenderTree(child, renderNode);
        }
    }

    private void setWidthHeightToChildren(RenderNode renderNode) {
        for (RenderNode child : renderNode.getChildren()) {
            setHeightToChildren(child);
            setWidthToChildren(renderNode, child);
            setWidthHeightToChildren(child);
        }
    }

    private void setHeightToChildren(RenderNode renderNode) {
        String strHeight = renderNode.getAppliedStyles().get("height");

        if (strHeight != null && (strHeight.endsWith("px") || strHeight.endsWith("%"))) {
            try {
                int intHeight = Integer.parseInt(strHeight.replace("px", "").replace("%", "").trim());
                if (strHeight.endsWith("px")) {
                    renderNode.setHeight(intHeight);
                } else {
                    renderNode.setHeight(getHeightFromParent(renderNode, intHeight));
                }
            } catch (NumberFormatException e) {
                // Игнорируем некорректные значения (например, "auto", "inherit" и т. д.)
            }
        } else if (renderNode.getTagName().equals("text")) {
            setHeightOfText(renderNode);
        }
    }


    private void setHeightOfText(RenderNode renderNode) {
        String textContent = renderNode.getTextContent();
        if (textContent == null || textContent.isEmpty()) {
            return;
        }

        @SuppressWarnings("unused")
        String tagName = renderNode.getTagName();

        String fontName = renderNode.getAppliedStyles().getOrDefault("font-family", "Arial");
        int fontSize;

        fontSize = Integer.parseInt(renderNode.getAppliedStyles().getOrDefault("font-size", "12").replace("px", "").trim());

        int fontStyle = Font.PLAIN;
        String fontWeight = renderNode.getAppliedStyles().getOrDefault("font-weight", "normal");
        if (fontWeight.equalsIgnoreCase("bold")) {
            fontStyle = Font.BOLD;
        }

        Font font = new Font(fontName, fontStyle, fontSize);

        Canvas dummyCanvas = new Canvas();
        FontMetrics metrics = dummyCanvas.getFontMetrics(font);

        int textHeight = metrics.getHeight();
        int textWidth = metrics.stringWidth(textContent);
        if (!Objects.equals(renderNode.getParent().getTagName(), "div")){
            double parentWidth = renderNode.getWidth();
            renderNode.getParent().setWidth(textWidth + parentWidth);
            renderNode.getParent().setHeight(textHeight);
        }
        renderNode.setHeight(textHeight);
        renderNode.setWidth(textWidth);
    }

    private Integer getHeightFromParent(RenderNode renderNode, int devider) {
        String strHeight = renderNode.getAppliedStyles().get("height");
        if (strHeight == null || devider == 0) {
            return renderTree.getWindowHeight() / (100 / Math.max(devider, 1));
        }
        int height = Integer.parseInt(strHeight.replace("px", "").replace("%", "").trim());
        if (renderNode.getParent() == null){
            if (renderNode.getAppliedStyles().get("height") != null){
                return height / (100 / devider);
            }
            return renderTree.getWindowHeight() / (100 / devider);
        }
        if (strHeight.contains("%")){
            getHeightFromParent(renderNode.getParent(), height / (100 / devider));
        } else if (strHeight.contains("px")) {
            return height / (100 / devider);
        }
        return 0;
    }

    private void setWidthToChildren(RenderNode renderNode, RenderNode child) {
        HashMap<String, String> styles = child.getAppliedStyles();

        if (styles.get("width") != null && !styles.get("width").isEmpty()) {
            if (styles.get("width") != null && !styles.get("width").isEmpty()) {
                if (styles.get("width").contains("px")) {
                    child.setWidth(Double.parseDouble(styles.get("width").replace("px", "").trim()));
                } else if (styles.get("width").contains("%")) {
                    double widthPercent = Double.parseDouble(styles.get("width").replace("%", "").trim());
                    child.setWidth((renderNode.getWidth() / 100.0) * widthPercent);
                }
            }
        } else {
            if (styles.get("max-width") != null && !styles.get("max-width").isEmpty()) {
                double maxWidth = Double.parseDouble(styles.get("max-width").replace("px", "").replace("%", "").trim());
                if (maxWidth < renderNode.getWidth()){
                    child.setWidth(maxWidth);
                } else {
                    child.setWidth(renderNode.getWidth());
                }
            } else{
                child.setWidth(renderNode.getWidth());
            }
        }
    }



    public void findCssOfRenderTree(HtmlElement htmlElement, RenderNode renderNode, List<String> selectors, CssRule cssRule) {
        if (htmlElement.getClasses() == null || selectors.isEmpty() || cssRule == null) {
            return;
        }
        Set<String> elementClasses = new HashSet<>(Arrays.asList(htmlElement.getClasses()));
        String currentSelector = selectors.get(0);
        if (currentSelector.equals("div")){
            return;
        }

        if (htmlElement.getIds() != null){
            Set<String> elementIds = new HashSet<>(Arrays.asList(htmlElement.getIds()));
            if (currentSelector.startsWith("#") && elementIds.contains(currentSelector.substring(1))) {
                selectors = selectors.subList(1, selectors.size());
                if (selectors.isEmpty()) {
                    if (cssRule.getMedia() == null && cssRule.getMedia().isEmpty()){
                        computeStyles(cssRule, renderNode);
                    }  else {
                        System.out.println(matchMedia(cssRule.getMedia()));
                        System.out.println("hello world");
                        System.out.println(cssRule.getMedia());
                        if (matchMedia(cssRule.getMedia())){
                            System.out.println("Miside");
                        }
                    }
                    return;
                }
            }
        }

        if (elementClasses.contains(currentSelector) || htmlElement.getTag().equals(currentSelector)) {
            selectors = selectors.subList(1, selectors.size());
            if (selectors.isEmpty()) {
                if (cssRule.getMedia() != null && !cssRule.getMedia().isEmpty()) {
                    System.out.println(matchMedia(cssRule.getMedia()));;
                    if (matchMedia(cssRule.getMedia())){
                        computeStyles(cssRule, renderNode);
                    }
                } else {
                    computeStyles(cssRule, renderNode);
                }
                return;
            }
        }

        for (int i = 0; i < htmlElement.getChildren().size(); i++) {
            findCssOfRenderTree(htmlElement.getChildren().get(i), renderNode.getChildren().get(i), selectors, cssRule);
        }
    }
    public boolean matchMedia(String media) {
        int height = renderTree.getWindowHeight();
        int width = renderTree.getWindowWidth();

        String[] conditions = media.split("and");

        for (String condition : conditions) {
            condition = condition.trim();

            Pattern pattern = Pattern.compile("\\(([^)]*)\\)");
            Matcher matcher = pattern.matcher(condition);

            if (matcher.find()) {
                String keyValue = matcher.group(1).trim();
                String[] listOfKeyValue = keyValue.split(":", 2);

                if (listOfKeyValue.length != 2) {
                    return false;
                }

                String key = listOfKeyValue[0].trim();
                String value = listOfKeyValue[1].trim();

                int cssValue;
                try {
                    cssValue = Integer.parseInt(value.replace("px", "").replace("%", "").trim());
                } catch (NumberFormatException e) {
                    return false;
                }

                switch (key) {
                    case "max-width":
                        if (width > cssValue) {
                            return false;
                        }
                        break;
                    case "min-width":
                        if (width < cssValue) {
                            return false;
                        }
                        break;
                    case "max-height":
                        if (height > cssValue) {
                            return false;
                        }
                        break;
                    case "min-height":
                        if (height < cssValue) {
                            return false;
                        }
                        break;
                    case "orientation":
                        String orientation = cssValue == 0 ? "portrait" : "landscape";
                        if (!value.equalsIgnoreCase(orientation)) {
                            return false;
                        }
                        break;
                    default:
                        return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    private void setCssToChildren(RenderNode renderNode) {
        for (RenderNode child : renderNode.getChildren()) {
            HashMap<String, String> parentStyles = renderNode.getAppliedStyles();
            for (Map.Entry<String, String> entry : parentStyles.entrySet()) {
                if (!inheritableStyles.contains(entry.getKey())) {
                    continue;
                }
                child.getAppliedStyles().putIfAbsent(entry.getKey(), entry.getValue());
            }
            setCssToChildren(child);
        }
    }

    private void computeStyles(CssRule cssRule, RenderNode renderNode) {
        HashMap<String, String> styles = new HashMap<>(cssRule.getPropertiesMap());

        Set<String> allowedStyles = Set.of(
                // Размеры и отступы
                "width", "height", "min-width", "max-width", "min-height", "max-height",
                "margin", "margin-top", "margin-right", "margin-bottom", "margin-left",
                "padding", "padding-top", "padding-right", "padding-bottom", "padding-left",
                "border", "border-width", "border-color", "border-style",
                "border-top", "border-right", "border-bottom", "border-left",
                "border-radius",
                "outline", "outline-width", "outline-color", "outline-style",

                // Цвета и фон
                "color", "background", "background-color", "background-image",
                "background-size", "background-position", "background-repeat",

                // Шрифты и текст
                "font-size", "font-family", "font-weight", "font-style", "font-variant",
                "line-height", "letter-spacing", "word-spacing",
                "text-align", "text-indent", "text-decoration", "text-transform",
                "white-space", "vertical-align",

                // Расположение и отображение
                "display", "visibility", "z-index", "position",
                "top", "right", "bottom", "left", "overflow",
                "overflow-x", "overflow-y", "float", "clear",

                // Тени и градиенты
                "box-shadow", "text-shadow",

                // Переходы и анимации
                "transition", "transition-property", "transition-duration",
                "transition-timing-function", "transition-delay",
                "animation", "animation-name", "animation-duration",
                "animation-timing-function", "animation-delay",
                "animation-iteration-count", "animation-direction",

                // Flexbox
                "flex", "flex-grow", "flex-shrink", "flex-basis",
                "justify-content", "align-items", "align-self",
                "align-content", "order",

                // Grid
                "grid-template-rows", "grid-template-columns", "grid-template-areas",
                "grid-gap", "grid-row", "grid-column", "grid-area",
                "justify-items", "justify-self", "place-items",

                // Другие свойства
                "cursor", "clip", "opacity", "content", "quotes"
        );

        styles.keySet().retainAll(allowedStyles);

        renderNode.setStyles(styles);
    }

}

