package com.example.demo.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.Result;
import com.example.demo.repository.chat.entity.*;
import com.example.demo.repository.comment.entity.*;
import com.example.demo.repository.house.entity.*;
import com.example.demo.repository.notification.entity.*;
import com.example.demo.repository.rental.entity.*;
import com.example.demo.repository.user.entity.*;
import com.example.demo.repository.chat.*;
import com.example.demo.repository.comment.*;
import com.example.demo.repository.house.*;
import com.example.demo.repository.notification.*;
import com.example.demo.repository.rental.*;
import com.example.demo.repository.user.*;
import com.example.demo.utils.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/permission")
public class AdminPermissionController {

    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private PermissionMapper permissionMapper;
    @Autowired
    private RolePermissionMapper rolePermissionMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private UserMapper userMapper;

    // ==================== 角色管理 ====================

    /**
     * 获取角色列表
     */
    @GetMapping("/role/list")
    public Result getRoleList(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        Page<Role> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Role::getRoleName, keyword).or().like(Role::getRoleCode, keyword);
        }
        wrapper.orderByAsc(Role::getRoleId);
        
        Page<Role> result = roleMapper.selectPage(pageParam, wrapper);
        
        List<Map<String, Object>> records = new ArrayList<>();
        for (Role role : result.getRecords()) {
            Map<String, Object> map = new HashMap<>();
            map.put("roleId", role.getRoleId());
            map.put("roleCode", role.getRoleCode());
            map.put("roleName", role.getRoleName());
            map.put("roleDesc", role.getRoleDesc());
            map.put("createTime", role.getCreateTime());
            
            // 统计该角色的用户数
            long userCount = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, role.getRoleId()));
            map.put("userCount", userCount);
            
            // 获取权限数量
            long permCount = rolePermissionMapper.selectCount(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, role.getRoleId()));
            map.put("permCount", permCount);
            
            records.add(map);
        }
        
        return Result.success(Map.of(
                "records", records,
                "total", result.getTotal(),
                "current", result.getCurrent(),
                "size", result.getSize()
        ));
    }

    /**
     * 获取所有角色（下拉选择用）
     */
    @GetMapping("/role/all")
    public Result getAllRoles() {
        List<Role> roles = roleMapper.selectList(new LambdaQueryWrapper<Role>().orderByAsc(Role::getRoleId));
        return Result.success(roles);
    }

    /**
     * 获取角色详情（含权限）
     */
    @GetMapping("/role/{id}")
    public Result getRoleDetail(@PathVariable Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            return Result.failure("角色不存在");
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("roleId", role.getRoleId());
        data.put("roleCode", role.getRoleCode());
        data.put("roleName", role.getRoleName());
        data.put("roleDesc", role.getRoleDesc());
        
        // 获取角色权限ID列表
        List<RolePermission> rps = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id));
        List<Long> permIds = rps.stream().map(RolePermission::getPermId).collect(Collectors.toList());
        data.put("permIds", permIds);
        
        return Result.success(data);
    }

    /**
     * 添加角色
     */
    @PostMapping("/role")
    @Transactional
    public Result addRole(@RequestBody Map<String, Object> body) {
        String roleCode = (String) body.get("roleCode");
        String roleName = (String) body.get("roleName");
        String roleDesc = (String) body.get("roleDesc");
        List<Integer> permIds = (List<Integer>) body.get("permIds");
        
        if (roleCode == null || roleName == null) {
            return Result.failure("角色编码和名称不能为空");
        }
        
        // 检查编码是否重复
        if (roleMapper.selectCount(new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, roleCode)) > 0) {
            return Result.failure("角色编码已存在");
        }
        
        Role role = new Role();
        role.setRoleCode(roleCode);
        role.setRoleName(roleName);
        role.setRoleDesc(roleDesc);
        role.setCreateTime(LocalDateTime.now());
        roleMapper.insert(role);
        
        // 分配权限
        if (permIds != null && !permIds.isEmpty()) {
            for (Integer permId : permIds) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(role.getRoleId());
                rp.setPermId(permId.longValue());
                rp.setCreateTime(LocalDateTime.now());
                rolePermissionMapper.insert(rp);
            }
        }
        
        return Result.success("添加成功");
    }

    /**
     * 编辑角色
     */
    @PutMapping("/role/{id}")
    @Transactional
    public Result updateRole(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            return Result.failure("角色不存在");
        }
        
        String roleName = (String) body.get("roleName");
        String roleDesc = (String) body.get("roleDesc");
        List<Integer> permIds = (List<Integer>) body.get("permIds");
        
        if (roleName != null) role.setRoleName(roleName);
        if (roleDesc != null) role.setRoleDesc(roleDesc);
        roleMapper.updateById(role);
        
        // 更新权限
        if (permIds != null) {
            // 删除旧权限
            rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id));
            // 添加新权限
            for (Integer permId : permIds) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(id);
                rp.setPermId(permId.longValue());
                rp.setCreateTime(LocalDateTime.now());
                rolePermissionMapper.insert(rp);
            }
        }
        
        return Result.success("更新成功");
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/role/{id}")
    @Transactional
    public Result deleteRole(@PathVariable Long id) {
        // 检查是否有用户使用该角色
        long userCount = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, id));
        if (userCount > 0) {
            return Result.failure("该角色下有用户，无法删除");
        }
        
        // 删除角色权限关联
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id));
        // 删除角色
        roleMapper.deleteById(id);
        
        return Result.success("删除成功");
    }

    /**
     * 复制角色
     */
    @PostMapping("/role/{id}/copy")
    @Transactional
    public Result copyRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Role sourceRole = roleMapper.selectById(id);
        if (sourceRole == null) {
            return Result.failure("源角色不存在");
        }
        
        String newCode = body.get("roleCode");
        String newName = body.get("roleName");
        
        if (newCode == null || newName == null) {
            return Result.failure("新角色编码和名称不能为空");
        }
        
        if (roleMapper.selectCount(new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, newCode)) > 0) {
            return Result.failure("角色编码已存在");
        }
        
        // 创建新角色
        Role newRole = new Role();
        newRole.setRoleCode(newCode);
        newRole.setRoleName(newName);
        newRole.setRoleDesc(sourceRole.getRoleDesc());
        newRole.setCreateTime(LocalDateTime.now());
        roleMapper.insert(newRole);
        
        // 复制权限
        List<RolePermission> perms = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id));
        for (RolePermission rp : perms) {
            RolePermission newRp = new RolePermission();
            newRp.setRoleId(newRole.getRoleId());
            newRp.setPermId(rp.getPermId());
            newRp.setCreateTime(LocalDateTime.now());
            rolePermissionMapper.insert(newRp);
        }
        
        return Result.success("复制成功");
    }


    // ==================== 权限菜单 ====================

    /**
     * 获取权限树
     */
    @GetMapping("/menu/tree")
    public Result getPermissionTree() {
        List<Permission> allPerms = permissionMapper.selectList(
                new LambdaQueryWrapper<Permission>().orderByAsc(Permission::getOrderNum));
        
        // 构建树形结构
        List<Permission> tree = buildPermissionTree(allPerms, 0L);
        return Result.success(tree);
    }

    private List<Permission> buildPermissionTree(List<Permission> all, Long parentId) {
        return all.stream()
                .filter(p -> parentId.equals(p.getParentId()))
                .peek(p -> p.setChildren(buildPermissionTree(all, p.getPermId())))
                .collect(Collectors.toList());
    }

    // ==================== 管理员管理 ====================

    /**
     * 获取管理员列表
     */
    @GetMapping("/admin/list")
    public Result getAdminList(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "-1") Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        Page<User> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUserType, 3); // 只查管理员
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getRealName, keyword)
                    .or().like(User::getPhone, keyword));
        }
        if (status != -1) {
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByDesc(User::getCreateTime);
        
        Page<User> result = userMapper.selectPage(pageParam, wrapper);
        
        List<Map<String, Object>> records = new ArrayList<>();
        for (User user : result.getRecords()) {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", user.getUserId());
            map.put("username", user.getUsername());
            map.put("realName", user.getRealName());
            map.put("phone", user.getPhone());
            map.put("email", user.getEmail());
            map.put("status", user.getStatus());
            map.put("createTime", user.getCreateTime());
            
            // 获取用户角色
            List<UserRole> userRoles = userRoleMapper.selectList(
                    new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getUserId()));
            List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());
            map.put("roleIds", roleIds);
            
            // 获取角色名称
            if (!roleIds.isEmpty()) {
                List<Role> roles = roleMapper.selectBatchIds(roleIds);
                List<String> roleNames = roles.stream().map(Role::getRoleName).collect(Collectors.toList());
                map.put("roleNames", roleNames);
            } else {
                map.put("roleNames", new ArrayList<>());
            }
            
            records.add(map);
        }
        
        return Result.success(Map.of(
                "records", records,
                "total", result.getTotal(),
                "current", result.getCurrent(),
                "size", result.getSize()
        ));
    }

    /**
     * 添加管理员
     */
    @PostMapping("/admin")
    @Transactional
    public Result addAdmin(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        String password = (String) body.get("password");
        String realName = (String) body.get("realName");
        String phone = (String) body.get("phone");
        String email = (String) body.get("email");
        List<Integer> roleIds = (List<Integer>) body.get("roleIds");
        
        if (username == null || password == null) {
            return Result.failure("用户名和密码不能为空");
        }
        
        // 检查用户名是否重复
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0) {
            return Result.failure("用户名已存在");
        }
        
        User user = new User();
        user.setUsername(username);
        user.setPassword(PasswordUtil.encode(password));
        user.setRealName(realName);
        user.setPhone(phone);
        user.setEmail(email);
        user.setUserType(3); // 管理员
        user.setRealnameStatus(1); // 管理员默认已认证
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        
        // 分配角色
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Integer roleId : roleIds) {
                UserRole ur = new UserRole();
                ur.setUserId(user.getUserId());
                ur.setRoleId(roleId.longValue());
                ur.setCreateTime(LocalDateTime.now());
                userRoleMapper.insert(ur);
            }
        }
        
        return Result.success("添加成功");
    }

    /**
     * 编辑管理员
     */
    @PutMapping("/admin/{id}")
    @Transactional
    public Result updateAdmin(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        User user = userMapper.selectById(id);
        if (user == null || user.getUserType() != 3) {
            return Result.failure("管理员不存在");
        }
        
        String realName = (String) body.get("realName");
        String phone = (String) body.get("phone");
        String email = (String) body.get("email");
        String password = (String) body.get("password");
        List<Integer> roleIds = (List<Integer>) body.get("roleIds");
        
        if (realName != null) user.setRealName(realName);
        if (phone != null) user.setPhone(phone);
        if (email != null) user.setEmail(email);
        if (password != null && !password.isEmpty()) user.setPassword(PasswordUtil.encode(password));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        
        // 更新角色
        if (roleIds != null) {
            userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id));
            for (Integer roleId : roleIds) {
                UserRole ur = new UserRole();
                ur.setUserId(id);
                ur.setRoleId(roleId.longValue());
                ur.setCreateTime(LocalDateTime.now());
                userRoleMapper.insert(ur);
            }
        }
        
        return Result.success("更新成功");
    }

    /**
     * 启用/禁用管理员
     */
    @PutMapping("/admin/{id}/status")
    public Result toggleAdminStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null || (status != 0 && status != 1)) {
            return Result.failure("状态值无效");
        }
        
        User user = userMapper.selectById(id);
        if (user == null || user.getUserType() != 3) {
            return Result.failure("管理员不存在");
        }
        
        // 不能禁用自己（这里简化处理，实际应该获取当前登录用户ID）
        if (id == 1 && status == 0) {
            return Result.failure("不能禁用超级管理员");
        }
        
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getUserId, id)
                .set(User::getStatus, status)
                .set(User::getUpdateTime, LocalDateTime.now());
        userMapper.update(null, wrapper);
        
        return Result.success(status == 1 ? "已启用" : "已禁用");
    }

    /**
     * 删除管理员
     */
    @DeleteMapping("/admin/{id}")
    @Transactional
    public Result deleteAdmin(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null || user.getUserType() != 3) {
            return Result.failure("管理员不存在");
        }
        
        if (id == 1) {
            return Result.failure("不能删除超级管理员");
        }
        
        // 删除用户角色关联
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id));
        // 删除用户
        userMapper.deleteById(id);
        
        return Result.success("删除成功");
    }
}
