package com.sankuai.inf.leaf.client;

/**
 * @author qinshuang1998
 * @date 2019/7/10
 */
public interface LeafFactory {
    void init();
    long nextId();
}