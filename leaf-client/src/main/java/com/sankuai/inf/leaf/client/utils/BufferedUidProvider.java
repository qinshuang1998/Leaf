package com.sankuai.inf.leaf.client.utils;

import java.util.List;

/**
 * @author qinshuang1998
 * @date 2019/7/11
 */
public interface BufferedUidProvider {

    /**
     * Provides UID in one second
     *
     * @return
     */
    List<Long> provide();
}
