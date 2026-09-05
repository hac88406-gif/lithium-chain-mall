package com.greenchain.dto.response;

import com.greenchain.entity.SysPermission;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限树响应VO
 */
@Data
public class PermissionTreeVO {

    /**
     * 权限ID
     */
    private Long id;

    /**
     * 权限名称
     */
    private String label;

    /**
     * 权限标识
     */
    private String permissionKey;

    /**
     * 类型 1菜单 2按钮
     */
    private Integer type;

    /**
     * 是否选中
     */
    private Boolean checked;

    /**
     * 子权限列表
     */
    private List<PermissionTreeVO> children = new ArrayList<>();

    /**
     * 从实体转换
     */
    public static PermissionTreeVO fromEntity(SysPermission permission) {
        PermissionTreeVO vo = new PermissionTreeVO();
        vo.setId(permission.getId());
        vo.setLabel(permission.getName());
        vo.setPermissionKey(permission.getPermissionKey());
        vo.setType(permission.getType());
        vo.setChecked(false);
        return vo;
    }
}