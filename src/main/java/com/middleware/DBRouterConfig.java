package com.middleware;

/**
 * 分库分表配置
 *
 * @author whx
 */
public class DBRouterConfig {

    /**
     * 分库数量
     */
    private final int dbCount;

    /**
     * 分表数量
     */
    private final int tbCount;

    /**
     * 路由字段
     */
    private final String routerKey;

    public DBRouterConfig(int dbCount, int tbCount, String routerKey) {
        this.dbCount = dbCount;
        this.tbCount = tbCount;
        this.routerKey = routerKey;
    }

    public int getDbCount() {
        return dbCount;
    }

    public int getTbCount() {
        return tbCount;
    }

    public String getRouterKey() {
        return routerKey;
    }
}
