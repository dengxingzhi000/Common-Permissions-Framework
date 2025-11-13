package com.frog.system.service;

import com.frog.common.dto.dept.DeptDTO;
import com.frog.system.domain.entity.SysDept;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.UUID;

/**
 * <p>
 * 部门表 服务类
 * </p>
 *
 * @author author
 * @since 2025-11-07
 */
public interface ISysDeptService extends IService<SysDept> {
    /**
     * 查询部门树
     */
    List<DeptDTO> getDeptTree();

    /**
     * 查询子部门（包含自身）
     */
    List<UUID> getDeptAndChildren(UUID deptId);

    /**
     * 新增部门
     */
    void addDept(DeptDTO deptDTO);

    /**
     * 修改部门
     */
    void updateDept(DeptDTO deptDTO);

    /**
     * 删除部门
     */
    void deleteDept(UUID id);

    /**
     * 检查部门下是否有用户
     */
    boolean hasUsers(UUID deptId);

    /**
     * 检查部门下是否有子部门
     */
    boolean hasChildren(UUID deptId);
}
