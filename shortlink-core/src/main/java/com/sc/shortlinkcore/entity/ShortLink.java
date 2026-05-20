package com.sc.shortlinkcore.entity;   // 当前类所在的包

import jakarta.persistence.*;           // 导入 JPA 注解
import java.time.LocalDateTime;         // 导入时间类型

@Entity                                 // 标记这个类是一个数据库实体
@Table(name = "short_link")             // 指定数据库表名（默认类名小写也可，但显式写更清晰）
public class ShortLink {

    @Id                                 // 标记主键
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // 主键自增
    private Long id;

    @Column(name = "short_code", unique = true, nullable = false, length = 16)
    private String shortCode;

    @Column(name = "long_url", nullable = false, length = 2048)
    private String longUrl;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    // JPA 要求必须有一个无参构造方法（可以不写内容）
    public ShortLink() {
    }

    // 为了方便创建对象，我们写一个带两个参数的构造方法
    public ShortLink(String shortCode, String longUrl) {
        this.shortCode = shortCode;
        this.longUrl = longUrl;
        this.createTime = LocalDateTime.now();   // 自动设置为当前时间
    }

    // 下面是每个字段的 Getter 和 Setter 方法
    // Getter：获取字段的值；Setter：设置字段的值
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public void setLongUrl(String longUrl) {
        this.longUrl = longUrl;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}