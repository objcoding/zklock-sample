package com.objcoding.zklock;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁门面接口
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 2019-07-17
 */
public interface LockFacade {

    /**
     * 获取锁，如果没有得到就等待
     *
     * @param lockKey 锁
     * @return lockName
     */
    String acquire(String lockKey) throws Exception;

    /**
     * 获取锁，直到超时
     *
     * @param lockKey 锁
     * @param time    超时时间
     * @param unit    time参数的单位
     * @return lockName
     */
    String acquire(String lockKey, Long time, TimeUnit unit) throws Exception;

    /**
     * 判断分布式锁是否存在
     *
     * @param lockKey 锁
     * @return true 存在 false 不存在
     */
    boolean hasKey(String lockKey) throws Exception;

    /**
     * 释放锁
     *
     * @param lockName 锁名
     */
    void release(String lockName) throws Exception;
}
