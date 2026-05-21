package com.sc.shortlinkcore.entity;   // 当前类所在的包

import jakarta.persistence.*;           // 导入 JPA 注解
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;         // 导入时间类型

@Entity                                 // 标记这个类是一个数据库实体
@Data
@NoArgsConstructor

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

    // 为了方便创建对象，我们写一个带两个参数的构造方法
    public ShortLink(String shortCode, String longUrl) {
        this.shortCode = shortCode;
        this.longUrl = longUrl;
        this.createTime = LocalDateTime.now();   // 自动设置为当前时间
    }

}