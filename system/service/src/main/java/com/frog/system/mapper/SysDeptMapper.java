package com.frog.system.mapper;

import com.frog.common.dto.dept.DeptDTO;
import com.frog.system.domain.entity.SysDept;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <p>
 * 部门表 Mapper 接口
 * </p>
 *
 * @author author
 * @since 2025-11-07
 */
@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /**
     * 查询部门树（包含负责人姓名）
     */
    @Select("""
            SELECT d.*, u.real_name as leader_name
            FROM sys_dept d
            LEFT JOIN sys_user u ON d.leader_id = u.id
            WHERE d.deleted = 0
            ORDER BY d.sort_order, d.create_time
            """)
    List<DeptDTO> selectDeptTree();

    /**
     * 递归查询部门及其所有子部门
     */
    @Select("""
            WITH RECURSIVE dept_tree AS (
                SELECT id FROM sys_dept 
                WHERE id = #{deptId} AND deleted = 0
                UNION ALL
                SELECT d.id FROM sys_dept d
                INNER JOIN dept_tree dt ON d.parent_id = dt.id
                WHERE d.deleted = 0
            )
            SELECT id FROM dept_tree
            """)
    List<UUID> selectDeptAndChildren(@Param("deptId") UUID deptId);

    /**
     * 统计部门下的用户数
     */
    @Select("""
            SELECT COUNT(*) FROM sys_user
            WHERE dept_id = #{deptId} AND deleted = 0
            """)
    Integer countUsers(@Param("deptId") UUID deptId);

    /**
     * 统计子部门数
     */
    @Select("""
            SELECT COUNT(*) FROM sys_dept
            WHERE parent_id = #{deptId} AND deleted = 0
            """)
    Integer countChildren(@Param("deptId") UUID deptId);

    /**
     * 检查部门编码是否存在
     */
    @Select("""
            SELECT COUNT(*) > 0 FROM sys_dept
            WHERE dept_code = #{deptCode} 
            AND deleted = 0
            <if test='excludeId != null'>
                AND id != #{excludeId}
            </if>
            """)
    boolean existsByDeptCode(@Param("deptCode") String deptCode,
                             @Param("excludeId") UUID excludeId);

    default boolean existsByDeptCode(String deptCode) {
        return existsByDeptCode(deptCode, null);
    }

    /**
     * 查询部门负责人ID
     */
    @Select("""
            SELECT leader_id FROM sys_dept
            WHERE id = #{deptId} AND deleted = 0
            """)
    UUID getLeaderId(@Param("deptId") UUID deptId);

    /**
     * 批量查询部门名称
     */
    @Select("""
            <script>
            SELECT id, dept_name FROM sys_dept
            WHERE id IN
            <foreach collection='deptIds' item='id' open='(' close=')' separator=','>
                #{id}
            </foreach>
            AND deleted = 0
            </script>
            """)
    List<Map<UUID, String>> selectDeptNames(@Param("deptIds") List<UUID> deptIds);
}
