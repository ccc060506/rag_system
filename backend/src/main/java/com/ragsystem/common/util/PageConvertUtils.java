package com.ragsystem.common.util;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ragsystem.common.result.PageResult;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分页实体转 VO 通用工具类
 */
public class PageConvertUtils {

    /**
     * 将 MyBatis-Plus 的 Page<实体> 转换为 PageResult<VO>
     * @param mpPage MyBatis-Plus 分页实体对象
     * @param voClass 目标 VO 类的字节码
     * @return 包含 VO 列表的分页结果
     */
    public static <T, V> PageResult<V> convert(Page<T> mpPage, Class<V> voClass) {
        // 1. 转换实体列表为 VO 列表
        List<V> voList = mpPage.getRecords().stream()
                .map(entity -> {
                    try {
                        // 创建 VO 实例
                        V vo = voClass.getDeclaredConstructor().newInstance();
                        // 拷贝同名字段（实体 → VO）
                        BeanUtils.copyProperties(entity, vo);
                        return vo;
                    } catch (Exception e) {
                        throw new RuntimeException("分页实体转 VO 失败", e);
                    }
                })
                .collect(Collectors.toList());

        // 2. 封装为自定义 PageResult
        return PageResult.of(
                (int) mpPage.getCurrent(),  // 当前页码（Long → Integer）
                (int) mpPage.getSize(),     // 每页条数
                mpPage.getTotal(),          // 总记录数
                voList                      // VO 列表
        );
    }

    /**
     * @param mpPage MyBatis-Plus 分页实体对象
     * @param converter 自定义转换函数（实体 → VO）
     * @return 包含 VO 列表的分页结果
     */
    public static <T, V> PageResult<V> convert(Page<T> mpPage, java.util.function.Function<T, V> converter) {
        List<V> voList = mpPage.getRecords().stream()
                .map(converter)
                .collect(Collectors.toList());

        return PageResult.of(
                (int) mpPage.getCurrent(),
                (int) mpPage.getSize(),
                mpPage.getTotal(),
                voList
        );
    }

    public static <T, V> List<V> convertList(List<T> sourceList, Class<V> voClass) {
        List<V> targetList = new ArrayList<>();
        if (sourceList == null || sourceList.isEmpty()) {
            return targetList;
        }
        for (T source : sourceList) {
            try {
                V target = voClass.getDeclaredConstructor().newInstance();
                BeanUtils.copyProperties(source, target);
                targetList.add(target);
            } catch (Exception e) {
                throw new RuntimeException("列表转换失败", e);
            }
        }
        return targetList;
    }
}