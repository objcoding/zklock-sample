package com.objcoding.zklock;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.Stat;

import java.util.List;

/**
 * zk客户端基类实现
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 2019-07-17
 */
@Slf4j
public class CuratorClient {

    private CuratorFramework client;

    CuratorClient(CuratorFramework client) {
        this.client = client;
    }

    public CuratorFramework getClient() {
        return client;
    }

    protected String createEphemeralSequential(String ourPath) throws Exception {
        return getClient().
                create()
                .creatingParentsIfNeeded()
                // 临时节点
                .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
                .forPath(ourPath);

    }

    protected void delete(String ourPath) throws Exception {
        getClient().delete().forPath(ourPath);
    }

    protected boolean checkExists(String lockKey) throws Exception {
        Stat stat = getClient().checkExists().forPath(lockKey);
        log.info("stat:{}", stat);
        return stat != null;
    }

    protected List<String> getChildren(String basePath) throws Exception {
        return getClient().getChildren().forPath(basePath);
    }

}
