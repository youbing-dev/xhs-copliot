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
 * 内容主体实体，对应 contents 表。
 * cover_images 字段为 MySQL JSON 类型，在实体中以 String 存储，
 * 通过 Hutool JSONUtil 在服务层完成 List&lt;String&gt; 与字符串之间的转换。
 */
@Data
@NoArgsConstructor
@TableName("contents")
public class Content {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String body;

    private String tags;

    /**
     * 封面图列表，数据库列为 JSON 类型，此处以字符串形式存储。
     */
    private String coverImages;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
