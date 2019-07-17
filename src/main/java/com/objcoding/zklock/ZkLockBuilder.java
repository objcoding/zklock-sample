package com.objcoding.zklock;

import java.util.Arrays;

/**
 * 分布式锁key 构建器
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 2019-07-17
 */
public class ZkLockBuilder {

    /**
     * zk 锁后缀, 用于分割 basePath 和临时节点序列号
     */
    public static String DISTRIBUTE_LOCK = "/lock-";

    /**
     * 获取优惠券领券数量
     *
     * @param lockType   zk分布式锁节点枚举
     * @param activityNo 活动号
     * @param couponNo   优惠券号
     * @return lock path in zk
     */
    public static String getReceiveQuantityLockPath(LockTypeEnum lockType, String activityNo, String couponNo) {
        // /getReceiveQuantity/123/1234/lock-
        return buildLockPath(lockType.getValue(), activityNo, couponNo);
    }

    /**
     * 释放优惠券库存量
     *
     * @param lockType   zk分布式锁节点枚举
     * @param activityNo 活动号
     * @return lock path in zk
     */
    public static String getReleaseCouponStuckNumLockPath(LockTypeEnum lockType, String activityNo) {
        // /releaseCouponStuckNum/123/lock-
        return buildLockPath(lockType.getValue(), activityNo);
    }

    private static String buildLockPath(String... keys) {
        return Arrays.toString(keys)
                .replace("[", "")
                .replace("]", "")
                .replace(",", "/")
                .replace(" ", "") + DISTRIBUTE_LOCK;
    }

    public static void main(String[] args) {
        String[] split = getReceiveQuantityLockPath(LockTypeEnum.getReceiveQuantity, "123123", "23423423").split("/");
        System.out.println("/" + split[1]);
    }

}
