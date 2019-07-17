package com.objcoding.zklock;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * zk 分布式锁业务实现的核心
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 2019-07-17
 */
@Slf4j
@Service("zkLockService")
public class ZkLockService extends CuratorClient {

    private static final String LOCK_NAME_SPACE = "marketing_lock_namespace";

    @Autowired
    ZkLockService(CuratorFramework client) {
        super(client);
    }

    /**
     * 为每个锁创建一个永久性节点，作锁的根目录
     *
     * @see LockTypeEnum
     */
    @PostConstruct
    private void init() throws Exception {

        CuratorFramework client = getClient().usingNamespace(LOCK_NAME_SPACE);

        for (LockTypeEnum lockType : LockTypeEnum.values()) {
            if (client.checkExists().forPath(lockType.getValue()) == null) {
                log.info("正在为[{}]创建永久节点", lockType.getValue());
                client.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.PERSISTENT)
                        .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
                        .forPath(lockType.getValue());
            }
        }
    }

    /**
     * 获取锁，并设置超时时间
     *
     * @param waitTime 等待时间
     * @param timeUnit 时间类型
     * @param lockKey  锁 /releaseCouponStuckNum/123/1234-
     * @return lockName
     */
    public String lock(String lockKey, Long waitTime, TimeUnit timeUnit) throws Exception {

        // 创建临时序列节点
        String ourPath = super.createEphemeralSequential(lockKey);
        // /releaseCouponStuckNum/123/1234/lock-0000000000
        log.info("ourPath:{}", ourPath);

        return doLock(waitTime, timeUnit, ourPath);
    }

    /**
     * 进程需要访问共享数据时, 就在"/locks"节点下创建一个sequence类型的子节点, 称为ourPath.
     * 当ourPath在所有子节点中最小时, 说明该进程获得了锁. 进程获得锁之后, 就可以访问共享资源了.
     * 访问完成后, 需要将ourPath删除. 锁由新的最小的子节点获得.
     */
    private String doLock(Long waitTime, TimeUnit timeUnit, String ourPath) throws Exception {
        boolean haveLock = false;
        String lockName = null;
        boolean doDelete = false;

        // /releaseCouponStuckNum/123/1234/lock-0000000000
        String[] s = ourPath.split(ZkLockBuilder.DISTRIBUTE_LOCK);
        // /releaseCouponStuckNum/123/1234
        String basePath = s[0];
        // lock-0000000000
        String lockNumber = (ZkLockBuilder.DISTRIBUTE_LOCK + s[1]).replace("/", "");
        log.info("basePath:{}, lockNumber:{}", basePath, lockNumber);

        try {
            while (!haveLock) {
                // 根据锁根目录获取所有子节点序列号
                // [lock-0000000001, lock-0000000002, lock-0000000003, lock-0000000004]
                List<String> children = getChildrenAndSortThem(basePath);

                // 当前节点所在位置
                int index = children.indexOf(lockNumber);
                log.info("lockNumber index:{}", index);

                if (index < 0) {
                    throw new Exception("节点不存在");
                }
                // 如果该临时节点为最小，则获取锁成功
                if (index == 0) {
                    haveLock = true;
                    lockName = ourPath;
                } else {
                    // 检查锁目录的子节点是否有序列比它小，若有则监听比它小的上一个节点，当前锁处于等待状态
                    String frontLockNumber = children.get(index - 1);
                    CountDownLatch countDownLatch = new CountDownLatch(1);

                    // /releaseCouponStuckNum/123 + / + lock-0000000001
                    String frontPath = basePath + "/" + frontLockNumber;

                    // 核心 -> 监听前一个节点
                    super.getClient()
                            .getData()
                            .usingWatcher(
                                    (CuratorWatcher) watchedEvent -> {
                                        countDownLatch.countDown();
                                        log.info(frontPath + " 完成");
                                    })
                            .forPath(frontPath);

                    // 超时等待
                    if (waitTime != null && waitTime > 0 && timeUnit != null) {
                        log.info("[" + ourPath + "]" + "waiting[" + waitTime + "]" + frontPath);
                        countDownLatch.await(waitTime, timeUnit);
                    } else {
                        countDownLatch.await();
                    }
                }
            }
        } catch (Exception e) {
            doDelete = true;
            throw new Exception("获取分布式锁失败", e);
        } finally {
            if (doDelete) {
                delete(ourPath);
            }
        }
        return lockName;
    }

    /**
     * 判断分布式锁是否存在
     *
     * @param lockKey
     * @return true 存在 false 不存在
     */
    protected boolean hasKey(String lockKey) throws Exception {
        String basePath = lockKey.replace(ZkLockBuilder.DISTRIBUTE_LOCK, "");
        if (checkExists(basePath)) {
            List<String> children = getChildren(basePath);
            log.info("basePath:{}, children:{}", basePath, children);
            return children != null && !children.isEmpty();
        }

        return false;
    }

    private List<String> getChildrenAndSortThem(String basePath) throws Exception {

        // [lock-0000000001, lock-0000000002, lock-0000000003, lock-0000000004]

        List<String> children;
        children = getChildren(basePath);
        log.info("before sort children:[{}]", children);
        // 排序(升序)
        children.sort(Comparator.comparing(this::getLockNumber));
        log.info("after sort children:[{}]", children);
        return children;
    }

    private int getLockNumber(String lockNumber) {
        lockNumber = lockNumber.replace(ZkLockBuilder.DISTRIBUTE_LOCK.replace("/", ""), "");
        log.info("lockNumber:{}", lockNumber);
        return Integer.parseInt(lockNumber);
    }
}
