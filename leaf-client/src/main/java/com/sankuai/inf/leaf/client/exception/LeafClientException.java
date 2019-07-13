package com.sankuai.inf.leaf.client.exception;

/**
 * @author qinshuang1998
 * @date 2019/7/12
 */
public class LeafClientException extends RuntimeException {

    /**
     * Serial Version UID
     */
    private static final long serialVersionUID = 5010911274259630672L;

    /**
     * Default constructor
     */
    public LeafClientException() {
        super();
    }

    /**
     * Constructor with message & cause
     *
     * @param message
     * @param cause
     */
    public LeafClientException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with message
     *
     * @param message
     */
    public LeafClientException(String message) {
        super(message);
    }

    /**
     * Constructor with message format
     *
     * @param msgFormat
     * @param args
     */
    public LeafClientException(String msgFormat, Object... args) {
        super(String.format(msgFormat, args));
    }

    /**
     * Constructor with cause
     *
     * @param cause
     */
    public LeafClientException(Throwable cause) {
        super(cause);
    }

}
