package com.objcoding.zklock;

/**
 * zk 分布式锁类型枚举
 * 在zk中对应一个节点
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 2019-07-17
 */
public enum LockTypeEnum {

    /**
     * 获取优惠券领券数量
     */
    getReceiveQuantity("/getReceiveQuantity"),

    /**
     * 释放优惠券库存量
     */
    releaseCouponStuckNum("/releaseCouponStuckNum");

    private String value;

    LockTypeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
