package com.example.demo.repository.house.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("favorite")
public class Favorite {
    @TableId(type = IdType.AUTO)
    private Long favoriteId;
    
    private Long userId;
    private Long houseId;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
