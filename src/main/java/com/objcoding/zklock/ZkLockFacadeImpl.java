package com.objcoding.zklock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sun.security.util.PendingException;

import java.util.concurrent.TimeUnit;

/**
 * zk 分布式锁门面接口实现
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 2019-07-17
 */
@Slf4j
@Service("zkLockFacade")
public class ZkLockFacadeImpl implements LockFacade {


    @Autowired
    private ZkLockService zkLockService;

    @Override
    public String acquire(String lockKey) throws Exception {
        return zkLockService.lock(lockKey, null, null);
    }

    @Override
    public String acquire(String lockKey, Long waitTime, TimeUnit unit) throws Exception {
        return zkLockService.lock(lockKey, waitTime, unit);
    }

    @Override
    public boolean hasKey(String lockKey) throws Exception {
        return zkLockService.hasKey(lockKey);
    }

    @Override
    public void release(String lockName) throws Exception {
        zkLockService.delete(lockName);
    }

}
