package com.example.demo.repository.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.repository.user.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    /**
     * 根据用户ID获取菜单权限
     */
    @Select("SELECT DISTINCT p.* FROM permission p " +
            "INNER JOIN role_permission rp ON p.perm_id = rp.perm_id " +
            "INNER JOIN user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND p.perm_type = 1 " +
            "ORDER BY p.order_num")
    List<Permission> selectMenusByUserId(@Param("userId") Long userId);

    /**
     * 获取所有菜单权限
     */
    @Select("SELECT * FROM permission WHERE perm_type = 1 ORDER BY order_num")
    List<Permission> selectAllMenus();
}
