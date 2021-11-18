package com.freedy;

/**
 * @author Freedy
 * @date 2021/11/18 16:31
 */
public interface Protocol {
    String HEARTBEAT_LOCAL_NORMAL_MSG="I AM ALIVE!";
    String HEARTBEAT_LOCAL_ERROR_MSG="I AM READY DOWN!";
    String HEARTBEAT_REMOTE_NORMAL_MSG="ROGER!";
    String HEARTBEAT_REMOTE_ERROR_MSG="PLEASE SEND A OTHER CHANNEL!";
    String ACK="ACK!";
    String CONNECTION_SUCCESS_MSG="CONNECT ESTABLISH SUCCEED!";
}
