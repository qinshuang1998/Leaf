package com.sankuai.inf.leaf.client.factory;

import com.sankuai.inf.leaf.client.Constants;
import com.sankuai.inf.leaf.client.LeafFactory;
import com.sankuai.inf.leaf.client.config.LeafClientConfig;
import com.sankuai.inf.leaf.client.exception.LeafClientException;
import com.sankuai.inf.leaf.client.utils.LeafHttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;

/**
 * @author qinshuang1998
 * @date 2019/7/12
 */
public class DefaultLeafFactoryImpl implements LeafFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLeafFactoryImpl.class);

    protected boolean initOk = false;

    protected String serverUrl;
    protected int connectTimeout;
    protected int readTimeout;

    @Override
    public void init() {
        LeafClientConfig clientConfig = LeafClientConfig.getInstance();
        if (clientConfig.getLeafServer() == null) {
            throw new LeafClientException("Service address is not set");
        }
        if (clientConfig.isSegmentMode() == clientConfig.isSnowflakeMode()) {
            throw new LeafClientException("Can only set one mode");
        }
        if (clientConfig.isSegmentMode() && clientConfig.getBizTag() == null) {
            throw new LeafClientException("Segment mode requires setting the bizTag");
        }
        this.serverUrl = clientConfig.isSegmentMode() ?
                MessageFormat.format(Constants.SEGMENT_URL, clientConfig.getLeafServer(), clientConfig.getBizTag()) :
                MessageFormat.format(Constants.SNOWFLAKE_URL, clientConfig.getLeafServer(), "bizTag");
        this.connectTimeout = clientConfig.getConnectTimeout();
        this.readTimeout = clientConfig.getReadTimeout();
        initOk = true;
    }

    @Override
    public long nextId() {
        if (!initOk) {
            throw new LeafClientException("Must be initialized before use");
        }
        String get = LeafHttpUtils.get(serverUrl, readTimeout, connectTimeout);
        for (int retry = 1; get == null || get.isEmpty(); retry++) {
            LOGGER.warn("Can't get id normally");
            if (retry >= 3) {
                throw new LeafClientException("Unknown leaf-service exception");
            }
            get = LeafHttpUtils.get(serverUrl, readTimeout, connectTimeout);
        }
        return Long.parseLong(get);
    }
}
