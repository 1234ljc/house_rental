package com.example.demo.repository.comment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("house_comment")
public class HouseComment {
    @TableId(type = IdType.AUTO)
    private Long commentId;

    private Long houseId;
    private Long userId;
    private Long parentId;           // 父帖子ID，null=顶层
    private Long replyToUserId;      // 回复目标用户ID
    private String content;
    private Boolean hasRented;
    private Integer likeCount;
    private Integer status;          // 0正常 1已删除

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
