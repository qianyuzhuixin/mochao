package com.mochao.common.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mochao.common.constant.Constants;
import com.mochao.module.book.dto.ChapterItem;
import com.mochao.module.book.entity.Book;
import com.mochao.module.book.entity.BookChapter;
import com.mochao.module.book.mapper.BookChapterMapper;
import com.mochao.module.book.mapper.BookMapper;
import com.mochao.module.book.util.ChapterSplitUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 应用启动数据初始化器
 * 首次启动时自动播种内置书库素材
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final BookMapper bookMapper;
    private final BookChapterMapper bookChapterMapper;

    @Override
    public void run(String... args) {
        initBuiltinBooks();
    }

    private void initBuiltinBooks() {
        long existingCount = bookMapper.selectCount(
                new LambdaQueryWrapper<Book>().eq(Book::getSourceType, Constants.SOURCE_TYPE_BUILTIN));
        if (existingCount > 0) {
            log.info("[DataInitializer] 内置书库已初始化，跳过（已有 {} 本书）", existingCount);
            return;
        }

        log.info("[DataInitializer] 开始初始化内置书库素材...");
        List<BuiltinBook> books = getBuiltinBooks();
        int successCount = 0;

        for (BuiltinBook bb : books) {
            try {
                Book book = new Book();
                book.setTitle(bb.title);
                book.setBookName(bb.bookName);
                book.setAuthor(bb.author);
                book.setCategory(bb.category);
                book.setTags(bb.tags);
                book.setContent(bb.content);
                book.setWordCount(bb.content.replaceAll("\\s+", "").length());
                book.setDifficulty(bb.difficulty);
                book.setSourceType(Constants.SOURCE_TYPE_BUILTIN);
                book.setStatus(1);
                bookMapper.insert(book);

                // 拆分章节并写入章节表
                List<ChapterItem> chapters = ChapterSplitUtil.split(bb.content);
                if (!chapters.isEmpty()) {
                    for (ChapterItem ci : chapters) {
                        BookChapter chapter = new BookChapter();
                        chapter.setBookId(book.getId());
                        chapter.setChapterIdx(ci.getIndex());
                        chapter.setTitle(ci.getTitle());
                        chapter.setContent(ci.getContent());
                        chapter.setWordCount(ci.getWordCount() != null ? ci.getWordCount() :
                                ci.getContent().replaceAll("\\s+", "").length());
                        bookChapterMapper.insert(chapter);
                    }
                }

                successCount++;
                log.info("[DataInitializer] 已导入: {} - {}", bb.bookName, bb.title);
            } catch (Exception e) {
                log.error("[DataInitializer] 导入失败: {} - {}", bb.bookName, bb.title, e);
            }
        }

        log.info("[DataInitializer] 内置书库初始化完成: 成功 {}/{} 本", successCount, books.size());
    }

    private List<BuiltinBook> getBuiltinBooks() {
        return Arrays.asList(
                new BuiltinBook("剑九黄", "雪中悍刀行", "烽火戏诸侯", "玄幻", "剑客,江湖,豪迈",
                        "老黄背着剑匣，走入了北莽。他说，这一剑，叫六千里。他说这六千里路，他走了三十年。他说，他这辈子只出了一剑。但这天下，都知道这一剑。\n\n剑九，六千里。这是他走遍天下的路。他是条狗，一条替世子殿下挡了三十年风雨的老狗。老狗也有咬人的时候。当那天上的剑仙如雨落，当那地上的大宗师纷纷出手，老黄拔出了他那把已经锈蚀的铁剑。\n\n黄阵图，剑九黄。这个天下，没有人能让他拔第二次剑。",
                        2),
                new BuiltinBook("齐静春", "剑来", "烽火戏诸侯", "玄幻", "文圣,道理,守护",
                        "齐静春，这个在小镇上住了很多年的读书人，看上去一点都不像传说中的圣人。他喜欢喝黄酒，喜欢坐在门槛上晒太阳，喜欢跟小镇的孩子们讲一些似是而非的道理。\n\n没有人知道，这座小镇，是这位读书人最后的底线。也没有人知道，那些高高在上的神仙，为何要忌惮一个看上去平平无奇的读书人。\n\n齐先生笑着说：\"天下道理千万，我认的死理只有一条——护住身后这些人。\"\n\n然后他死了。为了让小镇里那些普通人，能够继续过着普通的日子。",
                        2),
                new BuiltinBook("许七安", "大奉打更人", "卖报小郎君", "玄幻", "打更人,破案,热血",
                        "打更人，大奉王朝最令人忌惮的职业。他们身着皂衣，腰悬铜锣，夜巡京城，看似只是报时打更的差役，实则手握监察百官、先斩后奏之权。\n\n许七安坐在司天监的屋顶上，看着京城的万家灯火。他刚破了一桩大案，本该高兴，但心底却沉甸甸的。\n\n\"这世上的真相，就像京城的雾，\"他自言自语，\"你以为拨开了一层就够了，可后面还有一层，一层又一层，无穷无尽。\"\n\n他摸了摸腰间的铜锣，那冰凉的触感提醒着他——他是打更人，他的职责是敲响警钟，让那些沉浸在美梦中的人醒来。",
                        2),
                new BuiltinBook("萧炎", "斗破苍穹", "天蚕土豆", "玄幻", "少年,逆袭,热血",
                        "萧炎站在悬崖边缘，望着远方连绵的山脉，心中涌起一股难以言喻的感觉。\n\n\"三年之约，今日该了结了。\"他低声说道，语气中带着几分坚定。\n\n三年前，他被云岚宗逐出家族，被未婚妻当众退婚，被所有人视为废物。三年后，他带着一身惊天修为归来。\n\n风起云涌，他的衣袍猎猎作响。身后，药老悬浮在骨灵冷火之中，看着自己的弟子，嘴角微微上扬。\n\n\"小子，怕吗？\"\n\n萧炎笑了：\"怕？从走出乌坦城那天起，我就没怕过。\"\n\n他抬脚迈出，一步踏空，身形如流星般掠向云岚宗。",
                        1),
                new BuiltinBook("韩立", "凡人修仙传", "忘语", "仙侠", "修仙,谨慎,炼丹",
                        "韩立屏住呼吸，小心翼翼地将灵草投入炼丹炉中。这是他第三次尝试炼制筑基丹，前两次都以失败告终。\n\n修仙界弱肉强食，一步落后便是万劫不复。他深知自己资质平庸，能走到今天，全靠一个\"稳\"字。\n\n炉中丹液翻滚，灵气氤氲。他凝神聚气，不敢有丝毫懈怠。手上的法诀变换不停，每一道灵力都精准无比。\n\n\"成了！\"他心中暗喜，却面色不变，依旧保持着冷静。丹炉中一颗圆润的丹药缓缓成型，散发出淡淡药香。\n\n韩立没有急着取出丹药，而是默默收起法诀，又等了片刻，确认再无变数后，才小心翼翼地将丹药收入玉瓶。",
                        2),
                new BuiltinBook("范闲", "庆余年", "猫腻", "架空", "穿越,权谋,少年",
                        "范闲站在庆庙的门槛前，看着庙里那尊泥塑神像，忽然笑了。\n\n\"我这个人，从来不信命。\"他说。\n\n他是带着另一个世界的记忆来到这里的。四岁练功，六岁杀人，十二岁写下\"红楼梦\"，十六岁进京。每一步都像是被一只无形的手推着走。\n\n但他偏要走自己的路。\n\n京都的权谋如同一张大网，父亲、皇帝、长公主、宰相……每个人都在下棋，而他，是那颗最不受控的棋子。\n\n\"我范闲，宁可掀翻棋盘，也不做别人手中的子。\"\n\n他转身离去，背影潇洒。少年的衣袍在风中翻飞，像是某种宣言。",
                        2),
                new BuiltinBook("宁缺", "将夜", "猫腻", "玄幻", "少年,复仇,书院",
                        "宁缺坐在书院后山的石阶上，看着远处长安城的万家灯火，一言不发。\n\n他杀过人，偷过东西，说过谎，在边塞军中摸爬滚打多年。他不是什么好人，也不打算做好人。他只想活下去，然后找到当年那些人，把他们一个一个杀掉。\n\n但书院的老头子说，他身上有浩然气。那个邋遢酒鬼说，他可以修大道。那个绝美的女子说，她愿意嫁给他。\n\n宁缺摸了摸怀里的那把短刀，心想：这个世道，连活着都这么难，你们跟我说什么大道？\n\n\"先活下去，\"他对自己说，\"其他的，以后再说。\"",
                        3),
                new BuiltinBook("序列", "诡秘之主", "爱潜水的乌贼", "奇幻", "克苏鲁,蒸汽朋克,神秘学",
                        "在这个蒸汽与机械的时代，在维多利亚风格的伦敦雾中，隐藏着另一个世界。\n\n序列。从九到零，每一条序列都通向一个不同的神。占卜家、读心者、学徒、偷盗者……每饮下一份魔药，就向神性更近一步，但也离人性更远一步。\n\n克莱恩坐在书桌前，翻阅着那份古老的手稿。煤油灯的火焰在他眼中跳动，映出一种不属于这个世界的智慧。\n\n\"超凡之路，从来不是馈赠，而是代价。\"他在笔记本上写道。\n\n窗外，蒸汽火车的汽笛声划破夜空。远处教堂的钟声敲响十二下。他合上笔记本，走向那扇半开的门——门后，是另一个维度的星空。",
                        3)
        );
    }

    /**
     * 内置书籍数据模型
     */
    private static class BuiltinBook {
        final String title;
        final String bookName;
        final String author;
        final String category;
        final String tags;
        final String content;
        final int difficulty;

        BuiltinBook(String title, String bookName, String author, String category,
                    String tags, String content, int difficulty) {
            this.title = title;
            this.bookName = bookName;
            this.author = author;
            this.category = category;
            this.tags = tags;
            this.content = content;
            this.difficulty = difficulty;
        }
    }
}
