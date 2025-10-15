package com.frog.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.frog.util.SecurityUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

/**
 * 字段自动填充处理器
 *
 * @author Deng
 * createData 2025/10/15 14:37
 * @version 1.0
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", Date.class, new Date());
        this.strictInsertFill(metaObject, "createBy", UUID.class, SecurityUtils.getCurrentUserId());
        this.strictInsertFill(metaObject, "updateTime", Date.class, new Date());
        this.strictInsertFill(metaObject, "updateBy", UUID.class, SecurityUtils.getCurrentUserId());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", Date.class, new Date());
        this.strictUpdateFill(metaObject, "updateBy", UUID.class, SecurityUtils.getCurrentUserId());
    }
}
