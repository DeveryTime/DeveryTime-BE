package com.dms.deverytime.domain.post.service;

import org.jsoup.parser.Parser;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 본문을 한 번 준비해 삭제 및 정리 검사에서 재사용한다.
final class PostImageReferences {
    private static final Pattern ENTITY = Pattern.compile("&(?:#[xX][0-9A-Fa-f]{1,8}|#[0-9]{1,8}|[A-Za-z][A-Za-z0-9]{1,31});");
    private final List<String> texts = new ArrayList<>();

    static PostImageReferences from(String content) {
        PostImageReferences references = new PostImageReferences();
        if (content == null) return references;
        references.add(content);
        try {
            references.addJson(JsonMapper.shared().readTree(content));
        } catch (JacksonException ignored) {
            // 일반 Markdown 본문이다.
        }
        return references;
    }

    boolean contains(String imageUrl) {
        String normalizedUrl = normalize(imageUrl);
        return texts.stream().anyMatch(text -> text.contains(normalizedUrl));
    }

    private void addJson(JsonNode node) {
        if (node == null) return;
        if (node.isString()) add(node.stringValue());
        if (node.isObject()) {
            for (var property : node.properties()) add(property.getKey());
        }
        for (JsonNode child : node.values()) addJson(child);
    }

    private void add(String text) {
        Matcher matcher = ENTITY.matcher(text);
        StringBuffer decoded = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(decoded, Matcher.quoteReplacement(Parser.unescapeEntities(matcher.group(), false)));
        }
        matcher.appendTail(decoded);
        texts.add(normalize(decoded.toString()));

        // HTML 가능성이 있으면 원문도 보수적으로 검사한다. 주석이나 속성 수 제한으로
        // 파서가 주소를 놓쳐 파일을 삭제하는 것보다 불필요한 보존이 안전하다.
        if (text.indexOf('<') >= 0) {
            texts.add(normalize(Parser.unescapeEntities(text, false)));
        }
    }

    private static String normalize(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length() && isPunctuation(value.charAt(i + 1))) {
                c = value.charAt(++i);
            }
            if (c == '%' && i + 2 < value.length()) {
                int high = Character.digit(value.charAt(i + 1), 16);
                int low = Character.digit(value.charAt(i + 2), 16);
                int decoded = high < 0 || low < 0 ? -1 : (high << 4) + low;
                if ((decoded >= 'A' && decoded <= 'Z') || (decoded >= 'a' && decoded <= 'z')
                        || (decoded >= '0' && decoded <= '9') || decoded == '-' || decoded == '.'
                        || decoded == '_' || decoded == '~') {
                    c = (char) decoded;
                    i += 2;
                }
            }
            result.append(c);
        }
        return result.toString();
    }

    private static boolean isPunctuation(char c) {
        return (c >= '!' && c <= '/') || (c >= ':' && c <= '@')
                || (c >= '[' && c <= '`') || (c >= '{' && c <= '~');
    }
}
