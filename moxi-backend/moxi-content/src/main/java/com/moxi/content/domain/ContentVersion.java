package com.moxi.content.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 内容版本快照实体，对应 content_versions 表。
 * 每次创建或更新内容时都会写入一条版本记录，便于历史回溯。
 */
@Data
@NoArgsConstructor
@TableName("content_versions")
public class ContentVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long contentId;

    private Integer versionNum;

    private String title;

    private String body;

    private String tags;

    /**
     * 封面图列表，数据库列为 JSON 类型，此处以字符串形式存储。
     */
    private String coverImages;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
