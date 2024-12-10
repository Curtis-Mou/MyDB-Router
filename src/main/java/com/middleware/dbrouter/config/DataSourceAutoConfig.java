package com.middleware.dbrouter.config;

import com.middleware.DBRouterAspect;
import com.middleware.DBRouterConfig;
import com.middleware.dbrouter.dynamic.DynamicDataSource;
import com.middleware.dbrouter.dynamic.DynamicMybatisPlugin;
import com.middleware.dbrouter.strategy.IDBRouterStrategy;
import com.middleware.dbrouter.strategy.impl.DBRouterStrategyHashCode;
import com.middleware.dbrouter.util.PropertyUtil;
import org.apache.ibatis.plugin.Interceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 数据源自动加载配置类: 配置加载和创建数据源
 *
 * @author whx
 */
@Configuration
public class DataSourceAutoConfig implements EnvironmentAware {

    /**
     * 分库数量
     */
    private int dbCount;

    /**
     * 分表数量
     */
    private int tbCount;

    /**
     * 路由字段
     */
    private String routerKey;

    /**
     * 数据源配置组
     */
    private final Map<String, Map<String, Object>> dataSourceMap = new HashMap<>();

    /**
     * 默认数据源配置
     */
    private Map<String, Object> defaultDataSourceConfig;

    @Bean(name = "db-router-point")
    @ConditionalOnMissingBean
    public DBRouterAspect point(DBRouterConfig dbRouterConfig, IDBRouterStrategy dbRouterStrategy) {
        return new DBRouterAspect(dbRouterConfig, dbRouterStrategy);
    }

    /**
     * 主要是为了把分库分表配置生成一个Bean对象
     * 方便在切面类中进行注入使用
     */
    @Bean
    public DBRouterConfig dbRouterConfig() {
        return new DBRouterConfig(dbCount, tbCount, routerKey);
    }

    @Bean
    public Interceptor plugin() {
        return new DynamicMybatisPlugin();
    }

    /**
     * 创建动态数据源
     * 该数据源会被 MyBatis 使用
     */
    @Bean
    public DataSource dataSource() {
        //创建数据源
        Map<Object, Object> targetDataSources = new HashMap<>();
        for (String dbInfo : dataSourceMap.keySet()) {
            Map<String, Object> objectMap = dataSourceMap.get(dbInfo);
            targetDataSources.put(dbInfo, new DriverManagerDataSource(objectMap.get("url").toString(),
                    objectMap.get("username").toString(), objectMap.get("password").toString()));
        }

        //设置数据源
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        dynamicDataSource.setTargetDataSources(targetDataSources);
        dynamicDataSource.setDefaultTargetDataSource(new DriverManagerDataSource(
                defaultDataSourceConfig.get("url").toString(),
                defaultDataSourceConfig.get("username").toString(),
                defaultDataSourceConfig.get("password").toString()
        ));

        return dynamicDataSource;
    }

    @Bean
    public IDBRouterStrategy dbRouterStrategy(DBRouterConfig dbRouterConfig) {
        return new DBRouterStrategyHashCode(dbRouterConfig);
    }

    @Bean
    public TransactionTemplate transactionTemplate(DataSource dataSource) {
        DataSourceTransactionManager dataSourceTransactionManager = new DataSourceTransactionManager();
        dataSourceTransactionManager.setDataSource(dataSource);

        TransactionTemplate transactionTemplate = new TransactionTemplate();
        transactionTemplate.setTransactionManager(dataSourceTransactionManager);
        transactionTemplate.setPropagationBehaviorName("PROPAGATION_REQUIRED");
        return transactionTemplate;
    }

    /**
     * 读取自定义配置, 因为我们要设置的是在yml配置多组数据源, 该方法就是为了解析数据源的配置信息
     */
    @SuppressWarnings("unchecked")
    @Override
    public void setEnvironment(Environment environment) {
        String prefix = "mini-db-router.jdbc.datasource.";

        dbCount = Integer.parseInt(Objects.requireNonNull(environment.getProperty(prefix + "dbCount")));
        tbCount = Integer.parseInt(Objects.requireNonNull(environment.getProperty(prefix + "tbCount")));
        routerKey = environment.getProperty(prefix + "routerKey");

        //分库分表数据源
        String dataSources = environment.getProperty(prefix + "list");
        assert dataSources != null;
        for (String dbInfo : dataSources.split(",")) {
            Map<String, Object> dataSourceProps = PropertyUtil.handle(environment, prefix, Map.class);
            dataSourceMap.put(dbInfo, dataSourceProps);
        }

        //默认数据源
        String defaultData = environment.getProperty(prefix + "default");
        defaultDataSourceConfig = PropertyUtil.handle(environment, prefix + defaultData, Map.class);
    }
}
