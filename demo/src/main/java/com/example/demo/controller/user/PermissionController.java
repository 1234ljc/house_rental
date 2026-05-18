package com.example.demo.controller.user;

import com.example.demo.repository.user.entity.Permission;
import com.example.demo.entity.Result;
import com.example.demo.repository.user.PermissionMapper;
import com.example.demo.utils.JwtUtil;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 权限菜单接口
 */
@RestController
@RequestMapping("/api/admin/permission")
public class PermissionController {

    private final PermissionMapper permissionMapper;
    private final JwtUtil jwtUtil;

    public PermissionController(PermissionMapper permissionMapper, JwtUtil jwtUtil) {
        this.permissionMapper = permissionMapper;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 获取当前用户的菜单权限（树形结构）
     */
    @GetMapping("/menus")
    public Result<List<Permission>> getUserMenus(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);

        if (userId == null) {
            return Result.failure(401, "无效的token");
        }

        // 获取用户菜单权限
        List<Permission> menus = permissionMapper.selectMenusByUserId(userId);

        // 如果用户没有分配角色，返回所有菜单（管理员默认拥有所有权限）
        if (menus.isEmpty()) {
            menus = permissionMapper.selectAllMenus();
        }

        // 构建树形结构
        List<Permission> menuTree = buildMenuTree(menus);

        return Result.success(menuTree);
    }

    /**
     * 获取所有菜单（树形结构）
     */
    @GetMapping("/all-menus")
    public Result<List<Permission>> getAllMenus() {
        List<Permission> menus = permissionMapper.selectAllMenus();
        List<Permission> menuTree = buildMenuTree(menus);
        return Result.success(menuTree);
    }

    /**
     * 构建菜单树
     */
    private List<Permission> buildMenuTree(List<Permission> menus) {
        // 按parentId分组
        Map<Long, List<Permission>> menuMap = menus.stream()
                .collect(Collectors.groupingBy(Permission::getParentId));

        // 设置子菜单
        menus.forEach(menu -> {
            List<Permission> children = menuMap.get(menu.getPermId());
            menu.setChildren(children != null ? children : new ArrayList<>());
        });

        // 返回顶级菜单（parentId = 0）
        return menus.stream()
                .filter(menu -> menu.getParentId() == 0)
                .collect(Collectors.toList());
    }
}
