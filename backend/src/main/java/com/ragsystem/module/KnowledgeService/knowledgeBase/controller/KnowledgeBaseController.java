package com.ragsystem.module.KnowledgeService.knowledgeBase.controller;

import com.ragsystem.common.result.PageResult;
import com.ragsystem.common.result.Result;
import com.ragsystem.common.util.RateLimit;
import com.ragsystem.module.KnowledgeService.knowledgeBase.domain.entity.KnowledgeFile;
import com.ragsystem.module.KnowledgeService.knowledgeBase.domain.entity.ParsedDocument;
import com.ragsystem.module.KnowledgeService.knowledgeBase.domain.vo.KnowledgeFileVO;
import com.ragsystem.module.KnowledgeService.knowledgeBase.service.IKnowledgeBaseService;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/KnowledgeBase")
public class KnowledgeBaseController {
    /*
    用户上传文件 → 保存元数据 → 得到 KnowledgeFile
    读取文件内容 + 解析 → 得到 ParsedDocument
    文本切块 + 生成 embedding → 得到多个 DocumentChunk
    */
    @Qualifier("knowledgeBaseImpl")
    @Autowired
    private IKnowledgeBaseService knowledgeBaseService;
    @Autowired
    private EmbeddingStore embeddingStore;

    /**
     * 上传文件并完整处理（解析 + 切块 + embedding + 存 Redis）
     */
    @PostMapping("/upload")
    @Operation(summary = "上传文件并完整处理")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file,
                                     @RequestParam(value = "isPublic",required = false, defaultValue = "false") Boolean isPublic
    ) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        String result = knowledgeBaseService.uploadAndProcess(file,isPublic);
        return Result.success(result);
    }

    /**
     * 解析文件
     */
    @PostMapping("/parse")
    @Operation(summary = "解析文件")
    public Result<ParsedDocument> parseFile(@RequestParam("file") MultipartFile file) {
        ParsedDocument doc = knowledgeBaseService.parseFile(file);
        return Result.success(doc);
    }

    /**
     * 分页查询所有文件
     */
    @GetMapping("/page/{pageNum}/{pageSize}")
    @Operation(summary = "分页查询所有文件")
    @RateLimit(key = "pageSelectFileLimit" , count = 10 , message = "分页查询文件服务繁忙，请稍后再次尝试")
    public Result<PageResult<KnowledgeFile>> pageSelect(@PathVariable Integer pageNum, @PathVariable Integer pageSize){
        PageResult<KnowledgeFile> list = knowledgeBaseService.pageSelect(pageNum,pageSize);
        return Result.success(list);
    }

    /**
     * 带条件分页查询
     */
    @GetMapping("/pageByOffer/{pageNum}/{pageSize}")
    @Operation(summary = "分页查询所有文件")
    @RateLimit(key = "pageSelectByOfferLimit" , count = 10 , message = "分页查询文件服务繁忙，请稍后再次尝试")
    public Result<PageResult<KnowledgeFile>> pageSelectByOffer(@PathVariable Integer pageNum,@PathVariable Integer pageSize,@RequestParam String text){
        PageResult<KnowledgeFile> list = knowledgeBaseService.pageSelectByOffer(pageNum,pageSize,text);
        return Result.success(list);
    }

    /**
     * 删除 KnowledgeFile
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除 KnowledgeFile")
    @RateLimit(key = "deleteFileLimit" , count = 3 , message = "删除文件服务繁忙，请稍后再次尝试")
    public Result<String> deleteById(@PathVariable Long id){
        knowledgeBaseService.deleteById(id);
        return Result.success("已删除id为"+id+"的文件");
    }
}
