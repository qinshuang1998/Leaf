package com.sankuai.inf.leaf.client.factory;

import com.sankuai.inf.leaf.client.LeafFactory;
import com.sankuai.inf.leaf.client.config.LeafClientConfig;
import org.junit.Test;

/**
 * @author qinshuang1998
 * @date 2019/7/12
 */
public class DefaultLeafFactoryImplTest {

    @Test
    public void nextId() {
        LeafClientConfig clientConfig = LeafClientConfig.getInstance();
        clientConfig.setLeafServer("127.0.0.1:8080");
        clientConfig.setSnowflakeMode(true);
        LeafFactory defaultLeafFactory = new DefaultLeafFactoryImpl();
        defaultLeafFactory.init();
        System.out.println(defaultLeafFactory.nextId());
    }
}