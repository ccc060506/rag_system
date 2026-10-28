package com.ragsystem.module.AIChatService;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import com.ragsystem.module.AIChatService.service.StreamingResponseHandler;
import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;
import io.reactivex.functions.Action;
@Slf4j
@Component
public class CustomDashScopeStreamingModel{
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    private final Generation generation = new Generation();

    public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
        try {
            // 转换消息格式
            List<Message> dashMessages = messages.stream()
                    .map(this::convertMessage)
                    .collect(Collectors.toList());

            GenerationParam param = GenerationParam.builder()
                    .apiKey(apiKey)
                    .model(model)
                    .messages(dashMessages)
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .incrementalOutput(true)  // 流式输出
                    .build();

            // 调用流式接口
            Flowable<GenerationResult> resultFlow = generation.streamCall(param);

            resultFlow.subscribe(
                    (io.reactivex.functions.Consumer<? super GenerationResult>) result -> {
                        String token = result.getOutput().getChoices().get(0).getMessage().getContent();
                        if (token != null && !token.isEmpty()) {
                            handler.onNext(token);
                        }
                    },
                    (Consumer<? super Throwable>) error -> {
                        log.error("DashScope 流式调用失败", error);
                        handler.onError(error);
                    },
                    (Action) () -> {
                        handler.onComplete();
                    }
            );

        } catch (Exception e) {
            log.error("构建 DashScope 请求失败", e);
            handler.onError(e);
        }
    }

    private Message convertMessage(ChatMessage msg) {
        if (msg instanceof SystemMessage) {
            return Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content(((SystemMessage) msg).text())
                    .build();
        } else if (msg instanceof UserMessage) {
            return Message.builder()
                    .role(Role.USER.getValue())
                    .content(((UserMessage) msg).singleText())
                    .build();
        } else if (msg instanceof AiMessage) {
            return Message.builder()
                    .role(Role.ASSISTANT.getValue())
                    .content(((AiMessage) msg).text())
                    .build();
        }
        throw new IllegalArgumentException("不支持的消息类型: " + msg.getClass());
    }

}
