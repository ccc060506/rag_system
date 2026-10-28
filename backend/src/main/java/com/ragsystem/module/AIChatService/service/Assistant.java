package com.ragsystem.module.AIChatService.service;

import dev.langchain4j.service.SystemMessage;

public interface Assistant {

    @SystemMessage("""
                       你是企业知识库助手。
                        1. 优先基于【参考资料】回答。
                        2. 如果资料中没有，但问题属于通用常识（如 1+1 或日常问候），请直接回答。
                        4. 请务必将你的计算过程和最终答案包裹在同一个 Markdown 代码块中。
                           格式规范：
                               先输出一个空行。
                               使用 ``` 开启代码块（不带语言后缀）。
                               在块内展示过程和结果。
                               使用 ``` 结束代码块。
                               再输出一个空行。
                               严禁在代码块之外直接书写加号(+)等特殊符号，以防前端渲染出错。
                        """)
    String chat(String userMessage);

}
