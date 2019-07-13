package com.sankuai.inf.leaf.client.config;

/**
 * @author qinshuang1998
 * @date 2019/7/9
 */
public class LeafClientConfig {
    private LeafClientConfig() {
    }

    private String leafServer;

    private boolean segmentMode;
    private String bizTag;

    private boolean snowflakeMode;

    private int connectTimeout = 200;
    private int readTimeout = 200;

    private int cachedSize = 1024;

    private static class Instance {
        private static final LeafClientConfig LEAF_CLIENT_CONFIG = new LeafClientConfig();
    }

    public static LeafClientConfig getInstance() {
        return Instance.LEAF_CLIENT_CONFIG;
    }

    public String getLeafServer() {
        return leafServer;
    }

    public void setLeafServer(String leafServer) {
        this.leafServer = leafServer;
    }

    public boolean isSegmentMode() {
        return segmentMode;
    }

    public void setSegmentMode(boolean segmentMode) {
        this.segmentMode = segmentMode;
    }

    public String getBizTag() {
        return bizTag;
    }

    public void setBizTag(String bizTag) {
        this.bizTag = bizTag;
    }

    public boolean isSnowflakeMode() {
        return snowflakeMode;
    }

    public void setSnowflakeMode(boolean snowflakeMode) {
        this.snowflakeMode = snowflakeMode;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getCachedSize() {
        return cachedSize;
    }

    public void setCachedSize(int cachedSize) {
        this.cachedSize = cachedSize;
    }
}
