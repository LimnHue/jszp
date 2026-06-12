package com.shangan.teacherprep.data

object SampleData {
    fun create(): AppData {
        val scope = LibraryScope()
        val scopeKey = scope.key
        return AppData(
            preferences = AppPreferences(
                selectedScope = scope,
                hasCompletedLibrarySelection = false,
            ),
            scopeConfigs = mapOf(scopeKey to ScopeDefaults.create(scope)),
            trials = listOf(
                TrialLesson(
                    scopeKey = scopeKey,
                    title = "《春》",
                    author = "朱自清",
                    textbook = "七年级上册",
                    genre = "写景散文",
                    courseInfoMarkdown = """
                        # 教学目标
                        1. 有感情地朗读课文，理清文章写景层次。
                        2. 品味准确、生动的语言，学习多感官写景方法。
                        3. 感受春天的生机，培养热爱自然的情感。

                        ## 教学重点
                        品味比喻、拟人等修辞手法的表达效果。
                    """.trimIndent(),
                    bodySections = listOf(
                        ContentSection("title-1", "导入新课", "同学们，一年四季中你最喜欢哪个季节？让我们跟随朱自清先生一起走进江南的春天。"),
                        ContentSection("title-2", "整体感知", "请快速默读课文，找出作者描绘的五幅春景图。\n\n> 春草图 · 春花图 · 春风图 · 春雨图 · 迎春图"),
                        ContentSection("title-3", "品读赏析", "盼望着，盼望着，东风来了，春天的脚步近了。\n\n一切都像刚睡醒的样子，欣欣然张开了眼。"),
                        ContentSection("title-4", "课堂小结", "文章按照盼春、绘春、赞春的思路展开，以细腻笔触写出春天的生机。"),
                    ),
                ),
                TrialLesson(
                    scopeKey = scopeKey,
                    title = "《背影》",
                    author = "朱自清",
                    textbook = "八年级上册",
                    genre = "叙事散文",
                    courseInfoMarkdown = "# 教学目标\n体会朴实语言中蕴含的深沉父爱。",
                    bodySections = listOf(
                        ContentSection(title = "导入新课", markdown = "从生活中一个难忘的背影谈起。"),
                        ContentSection(title = "整体感知", markdown = "梳理四次背影，聚焦望父买橘。"),
                    ),
                    durationMinutes = 16,
                ),
                TrialLesson(
                    scopeKey = scopeKey,
                    title = "《岳阳楼记》",
                    author = "范仲淹",
                    textbook = "九年级上册",
                    genre = "古诗文",
                    courseInfoMarkdown = "# 教学目标\n理解迁客骚人的览物之情与作者的政治抱负。",
                    bodySections = listOf(
                        ContentSection(title = "导入新课", markdown = "由江南三大名楼导入。"),
                        ContentSection(title = "整体感知", markdown = "疏通文意，概括阴晴两幅画面。"),
                    ),
                    durationMinutes = 20,
                ),
            ),
            structuredQuestions = listOf(
                StructuredQuestion(
                    scopeKey = scopeKey,
                    category = "教育教学",
                    question = "有学生在课堂上故意扰乱纪律，你会怎么办？",
                    answerSections = listOf(
                        ContentSection(title = "明确态度", markdown = "保持冷静，保障正常课堂秩序。"),
                        ContentSection(title = "及时处理", markdown = "采用眼神、走近或提问等方式提醒。"),
                        ContentSection(title = "课后沟通", markdown = "了解行为背后的真实原因。"),
                        ContentSection(title = "总结提升", markdown = "家校协同，持续关注学生成长。"),
                    ),
                ),
                StructuredQuestion(
                    scopeKey = scopeKey,
                    category = "应急应变",
                    question = "公开课上多媒体设备突然故障，你如何处理？",
                    answerSections = listOf(
                        ContentSection(title = "稳定课堂", markdown = "不慌乱，迅速切换板书和口头讲解。"),
                        ContentSection(title = "课后复盘", markdown = "检查设备并准备双套教学预案。"),
                    ),
                ),
            ),
            templates = listOf(
                AnswerTemplate(
                    scopeKey = scopeKey,
                    category = "导入语",
                    name = "课堂导入万能模板",
                    summary = "创设情境 · 激发兴趣",
                    contentMarkdown = "同学们，生活中我们都曾遇到……今天，让我们带着这个问题一起走进《课题》。",
                    module = "试讲",
                ),
                AnswerTemplate(
                    scopeKey = scopeKey,
                    category = "过渡语",
                    name = "文本赏析过渡语",
                    summary = "由内容走向语言",
                    contentMarkdown = "读懂内容只是第一步，作者是如何把这种感受写得如此动人的？让我们再回到文字中细细品味。",
                    module = "试讲",
                ),
                AnswerTemplate(
                    scopeKey = scopeKey,
                    category = "答题框架",
                    name = "应急应变答题框架",
                    summary = "态度 · 原因 · 措施",
                    contentMarkdown = "# 答题结构\n1. 稳定局面，明确原则。\n2. 分类分析，妥善处理。\n3. 复盘总结，建立预案。",
                    module = "结构化",
                ),
            ),
        )
    }
}
