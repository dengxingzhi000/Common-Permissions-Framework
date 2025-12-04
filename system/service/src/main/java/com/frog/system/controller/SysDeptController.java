package com.frog.system.controller;

import com.frog.common.dto.dept.DeptDTO;
import com.frog.common.log.annotation.AuditLog;
import com.frog.common.response.ApiResponse;
import com.frog.system.service.ISysDeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * <p>
 * 部门表 前端控制器
 * </p>
 *
 * @author author
 * @since 2025-11-07
 */
@RestController
@RequestMapping("/api/system/depts")
@RequiredArgsConstructor
@Tag(name = "部门管理")
public class SysDeptController {
    private final ISysDeptService deptService;

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:dept:list')")
    @Operation(summary = "查询部门树")
    public ApiResponse<List<DeptDTO>> tree() {
        List<DeptDTO> tree = deptService.getDeptTree();

        return ApiResponse.success(tree);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:dept:add')")
    @AuditLog(
            operation = "新增部门",
            businessType = "DEPT",
            riskLevel = 3
    )
    public ApiResponse<Void> add(@Validated @RequestBody DeptDTO deptDTO) {
        deptService.addDept(deptDTO);

        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept:edit')")
    @AuditLog(
            operation = "修改部门",
            businessType = "DEPT",
            riskLevel = 3
    )
    public ApiResponse<Void> update(@PathVariable UUID id,
                                    @Validated @RequestBody DeptDTO deptDTO) {
        deptDTO.setId(id);
        deptService.updateDept(deptDTO);

        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept:delete')")
    @AuditLog(
            operation = "删除部门",
            businessType = "DEPT",
            riskLevel = 4
    )
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        deptService.deleteDept(id);

        return ApiResponse.success();
    }
}
