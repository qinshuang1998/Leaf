package com.sankuai.inf.leaf.client.factory;

import com.sankuai.inf.leaf.client.LeafFactory;
import com.sankuai.inf.leaf.client.config.LeafClientConfig;
import org.junit.Test;

/**
 * @author qinshuang1998
 * @date 2019/7/12
 */
public class CachedLeafFactoryImplTest {

    @Test
    public void nextId() {
        LeafClientConfig clientConfig = LeafClientConfig.getInstance();
        clientConfig.setLeafServer("127.0.0.1:8080");
        clientConfig.setSnowflakeMode(true);
        LeafFactory cachedLeafFactory = new CachedLeafFactoryImpl();
        cachedLeafFactory.init();
        System.out.println(cachedLeafFactory.nextId());
        System.out.println(cachedLeafFactory.nextId());
    }
}