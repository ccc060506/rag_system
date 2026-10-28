package com.ragsystem.common.result;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.util.List;

/**
 * 分页响应结果
 * @param <T> 数据列表的泛型
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> {
    /**
     * 当前页码
     */
    private Integer pageNum;

    /**
     * 每页条数
     */
    private Integer pageSize;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 总页数
     */
    private Integer totalPages;

    /**
     * 当前页数据列表
     */
    private List<T> list;

    // 构造方法：快速构建分页响应
    public static <T> PageResult<T> of(Integer pageNum, Integer pageSize, Long total, List<T> list) {
        PageResult<T> response = new PageResult<>();
        response.setPageNum(pageNum);
        response.setPageSize(pageSize);
        response.setTotal(total);
        // 计算总页数：向上取整（例如总记录15，每页10条，总页数2）
        response.setTotalPages(total == 0 ? 0 : (int) Math.ceil((double) total / pageSize));
        response.setList(list);
        return response;
    }

    public static <T> PageResult<T> fromPageHelper(Page<T> page) {
        PageResult<T> result = new PageResult<>();
        // 1. 手动对齐核心数据列表：将 records 赋值给 list
        result.setList(page.getRecords());
        // 2. 手动对齐分页基础参数
        result.setTotal(page.getTotal());
        result.setPageNum((int) page.getCurrent());
        result.setPageSize((int) page.getSize());
        // 3. 补全总页数
        result.setTotalPages((int) page.getPages());
        return result;
    }
}