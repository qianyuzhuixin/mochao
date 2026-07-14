package com.mochao.module.book.util;

import com.mochao.module.book.dto.ChapterItem;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 章节拆分工具类
 * 将小说全文按章节标题或分隔线拆分为 ChapterItem 列表
 */
public class ChapterSplitUtil {

    /**
     * 按章节拆分文本
     * 支持格式：第X章 / 第 X 章 / 第一章 / 第001章 / 第 二 章（含全角空格/Tab）
     *   第X卷第X章 / 第X节 / 第X集 / 第X部 / 第X篇 / 第X回
     *   Chapter X / CHAPTER X / chapter x
     *   纯数字：1. / 1、 / 1 / 一、 / 二.
     *   特殊章节：楔子 / 序章 / 序言 / 前言 / 引子 / 引言 / 终章 / 大结局 / 尾声 / 后记 / 番外 / 番外篇
     *   scraper 格式：=== 第X章 标题 ===
     */
    public static List<ChapterItem> split(String text) {
        List<ChapterItem> chapters = new ArrayList<>();
        if (!StringUtils.hasText(text)) {
            return chapters;
        }

        // 统一换行符
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");

        // 策略0：scraper 专用的 === 第X章 标题 === 格式
        List<ChapterItem> scraperChapters = splitByScraperSeparator(normalized);
        if (!scraperChapters.isEmpty()) {
            return scraperChapters;
        }

        // 策略1：分隔线拆分（如 --- 或 === 分隔线）
        Pattern separatorPattern = Pattern.compile("^[-=]{10,}$", Pattern.MULTILINE);
        String[] separatorParts = separatorPattern.split(normalized);

        // 提取前言：保留所有元信息（书名、作者、book_id、评分、字数等）+ 引言正文
        ChapterItem prefaceItem = null;
        if (separatorParts.length > 0) {
            String firstPart = separatorParts[0].trim();
            if (firstPart.length() > 30) {
                StringBuilder prefaceContent = new StringBuilder();
                for (String line : firstPart.split("\n")) {
                    String l = line.trim();
                    if (l.isEmpty()) continue;
                    if (l.matches("^[-=]{10,}$")) continue;
                    prefaceContent.append(l).append("\n");
                }
                String prefaceText = prefaceContent.toString().trim();
                if (prefaceText.length() > 30) {
                    prefaceItem = new ChapterItem();
                    prefaceItem.setIndex(0);
                    prefaceItem.setTitle("前言/引子");
                    prefaceItem.setContent(prefaceText);
                    prefaceItem.setWordCount(prefaceText.replaceAll("\\s+", "").length());
                    prefaceItem.setPreview(getPreview(prefaceText, 100));
                }
            }
        }

        // 过滤有效段落：跳过纯元数据块（前端元数据行≥3视为无效章节）
        List<String> metaTags = Arrays.asList(
            "书名：", "作者：", "book_id=", "评分：", "字数：", "章节：",
            "分类：", "标签：", "在读：", "简介：", "状态：",
            "book_name=", "content="
        );
        List<String> validParts = new ArrayList<>();
        for (String part : separatorParts) {
            String trimmed = part.trim();
            if (trimmed.length() < 30) continue;
            String[] previewLines = trimmed.split("\n", 6);
            int metaTagCount = 0;
            for (String line : previewLines) {
                String l = line.trim();
                boolean isMeta = false;
                for (String tag : metaTags) {
                    if (l.startsWith(tag)) { isMeta = true; break; }
                }
                if (isMeta) metaTagCount++;
            }
            if (metaTagCount >= 3) continue;
            String firstLine = previewLines.length > 0 ? previewLines[0].trim() : "";
            if (firstLine.startsWith("书名：") || firstLine.startsWith("作者：")
                || firstLine.startsWith("book_id=")) {
                continue;
            }
            validParts.add(trimmed);
        }

        if (validParts.size() >= 1 || prefaceItem != null) {
            String titlePattern = "^\\s*(?:第\\s*[零一二两三四五六七八九十百千万\\d０-９]+\\s*[章节卷集部篇回]|" +
                "Chapter\\s+\\d+|CHAPTER\\s+\\d+|" +
                "楔子|序章|序言|前言|引子|引言|终章|大结局|尾声|后记|番外|番外篇)";
            Pattern titleRegex = Pattern.compile(titlePattern, Pattern.CASE_INSENSITIVE);

            if (prefaceItem != null) {
                chapters.add(prefaceItem);
            }

            for (int i = 0; i < validParts.size(); i++) {
                String content = validParts.get(i);
                String[] lines = content.split("\n", 6);
                String title = null;
                String body = content;
                int titleLineEnd = -1;

                for (int li = 0; li < lines.length; li++) {
                    String line = lines[li].trim();
                    if (line.isEmpty()) continue;
                    if (line.length() < 50 && titleRegex.matcher(line).find()) {
                        title = line;
                        int pos = 0;
                        for (int j = 0; j <= li; j++) {
                            pos += lines[j].length();
                            if (j < li) pos += 1;
                        }
                        titleLineEnd = pos;
                        break;
                    }
                    if (li >= 3) break;
                }

                if (title == null) {
                    title = "第" + (i + 1) + "章";
                } else {
                    body = content.substring(titleLineEnd).trim();
                    if (body.isEmpty()) body = content;
                }

                ChapterItem item = new ChapterItem();
                item.setIndex(i + 1);
                item.setTitle(title);
                item.setContent(body);
                item.setWordCount(body.replaceAll("\\s+", "").length());
                item.setPreview(getPreview(body, 100));
                chapters.add(item);
            }
            return chapters;
        }

        // 策略2：正则匹配"第X章"等传统章节标题
        String numPattern = "[零一二两三四五六七八九十百千万\\d０１２３４５６７８９]";
        String chSuffix = "[章节卷集部篇回]";

        String chapterPattern =
            "^\\s*(?:" +
            "第\\s*" + numPattern + "+\\s*" + chSuffix +
            "|第\\s*" + numPattern + "+\\s*卷\\s*第\\s*" + numPattern + "+\\s*" + chSuffix +
            "|Chapter\\s+\\d+|CHAPTER\\s+\\d+|chapter\\s+\\d+" +
            "|\\d+\\s*[、\\.\\s]\\s*" + chSuffix +
            "|[一二三四五六七八九十百千]+\\s*[、\\.\\s]\\s*" + chSuffix +
            "|楔子|序章|序言|前言|引子|引言|终章|大结局|尾声|后记|番外|番外篇" +
            ")[^\\n]*$";

        Pattern pattern = Pattern.compile(chapterPattern, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(normalized);

        List<int[]> chapterPositions = new ArrayList<>();
        while (matcher.find()) {
            chapterPositions.add(new int[]{matcher.start(), matcher.end()});
        }

        if (chapterPositions.isEmpty()) {
            String trimmed = normalized.trim();
            ChapterItem item = new ChapterItem();
            item.setIndex(1);
            item.setTitle("全文");
            item.setContent(trimmed);
            item.setWordCount(trimmed.replaceAll("\\s+", "").length());
            item.setPreview(getPreview(trimmed, 100));
            chapters.add(item);
            return chapters;
        }

        for (int i = 0; i < chapterPositions.size(); i++) {
            int start = chapterPositions.get(i)[0];
            int titleEnd = chapterPositions.get(i)[1];

            int lineEnd = normalized.indexOf('\n', titleEnd);
            if (lineEnd == -1) lineEnd = normalized.length();
            int contentStart = lineEnd < normalized.length() ? lineEnd + 1 : lineEnd;
            int contentEnd = (i + 1 < chapterPositions.size()) ? chapterPositions.get(i + 1)[0] : normalized.length();

            String titleLine = normalized.substring(start, lineEnd).trim();
            String content = normalized.substring(contentStart, contentEnd).trim();

            if (content.isEmpty()) continue;

            ChapterItem item = new ChapterItem();
            item.setIndex(i + 1);
            item.setTitle(titleLine.isEmpty() ? "第" + (i + 1) + "章" : titleLine);
            item.setContent(content);
            item.setWordCount(content.replaceAll("\\s+", "").length());
            item.setPreview(getPreview(content, 100));
            chapters.add(item);
        }

        if (!chapters.isEmpty() && chapterPositions.get(0)[0] > 0) {
            String prefaceText = normalized.substring(0, chapterPositions.get(0)[0]).trim();
            if (prefaceText.length() > 20) {
                ChapterItem item = new ChapterItem();
                item.setIndex(0);
                item.setTitle("前言/引子");
                item.setContent(prefaceText);
                item.setWordCount(prefaceText.replaceAll("\\s+", "").length());
                item.setPreview(getPreview(prefaceText, 100));
                chapters.add(0, item);
                for (int j = 1; j < chapters.size(); j++) {
                    chapters.get(j).setIndex(j);
                }
            }
        }

        return chapters;
    }

    /**
     * 策略0：识别 scraper 生成的 === 第X章 标题 === 格式
     *
     * 格式示例：
     *   === 第88章 幸存者 ===
     *   
     *   章节内容...
     *   
     *   === 第89章 破碎世界的馈赠 ===
     */
    private static List<ChapterItem> splitByScraperSeparator(String text) {
        List<ChapterItem> chapters = new ArrayList<>();

        // 匹配 === 标题 === 格式，两侧等号数量 ≥ 3，中间为标题
        Pattern pattern = Pattern.compile(
                "^\\s*={3,}\\s+(.+?)\\s+={3,}\\s*$",
                Pattern.MULTILINE
        );
        Matcher matcher = pattern.matcher(text);

        List<int[]> positions = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        while (matcher.find()) {
            positions.add(new int[]{matcher.start(), matcher.end()});
            titles.add(matcher.group(1).trim());
        }

        if (positions.isEmpty()) {
            return chapters;
        }

        // 如果匹配数量太少（只有 1 个），可能是巧合，交给其他策略处理
        if (positions.size() < 2) {
            return chapters;
        }

        for (int i = 0; i < positions.size(); i++) {
            int titleLineEnd = positions.get(i)[1];

            // 跳过标题行后的空行
            int contentStart = titleLineEnd;
            while (contentStart < text.length() && text.charAt(contentStart) == '\n') {
                contentStart++;
            }

            int contentEnd = (i + 1 < positions.size()) ? positions.get(i + 1)[0] : text.length();
            String content = text.substring(contentStart, contentEnd).trim();

            if (content.isEmpty()) {
                continue;
            }

            ChapterItem item = new ChapterItem();
            // scraper 格式的索引通常就是章节顺序，1-based
            item.setIndex(i + 1);
            item.setTitle(titles.get(i));
            item.setContent(content);
            item.setWordCount(content.replaceAll("\\s+", "").length());
            item.setPreview(getPreview(content, 100));
            chapters.add(item);
        }

        return chapters;
    }

    private static String getPreview(String text, int maxLen) {
        if (text == null || text.isEmpty()) return "";
        String cleaned = text.replaceAll("\\s+", "");
        if (cleaned.length() <= maxLen) return cleaned;
        return cleaned.substring(0, maxLen) + "...";
    }
}
