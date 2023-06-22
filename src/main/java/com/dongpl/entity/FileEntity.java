package com.dongpl.entity;

import lombok.Data;

@Data
public class FileEntity {
    /**
     * 文件名称
     */
    private String fileName;
    /**
     * 文件路径
     */
    private String path;
    /**
     * 文件父级路径
     */
    private String parentPath;
    /**
     * 服务端口号
     */
    private String port;
    /**
     * 服务是否开启
     */
    private boolean isOpen;
    /**
     * 服务开启的时间戳
     */
    private long startTime;
}
