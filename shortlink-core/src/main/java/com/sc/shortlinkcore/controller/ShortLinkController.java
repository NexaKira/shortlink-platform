package com.sc.shortlinkcore.controller;

import com.sc.shortlinkcore.common.RateLimit;
import com.sc.shortlinkcore.common.Result;
import com.sc.shortlinkcore.service.ClickLogService;
import com.sc.shortlinkcore.service.ShortLinkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController   // 标记这是一个 REST 风格的控制器
@Validated
public class ShortLinkController {

    @Autowired
    private ShortLinkService shortLinkService;
    @Autowired
    private ClickLogService clickLogService;

    // POST 请求，路径 /shorten，参数名为 url
    @PostMapping("/shorten")
    @RateLimit(permitsPerSecond = 2.0)
    public Result<String> shorten(@RequestParam @NotBlank(message = "URL不能为空") @URL(message = "URL格式不正确") String url) {
        return Result.success(shortLinkService.createShortLink(url));
    }

    // GET 请求，路径 /{shortCode}，比如 /abc123
    @GetMapping("/{shortCode}")
    public void redirect(@PathVariable String shortCode, HttpServletResponse response, HttpServletRequest request) throws IOException {
        String longUrl = shortLinkService.getLongUrl(shortCode);
        shortLinkService.recordClick(shortCode,
                request.getRemoteAddr(),            // 客户端 IP
                request.getHeader("User-Agent"), // 浏览器标识
                request.getHeader("Referer"));   // 来源页面
        response.sendRedirect(longUrl);
    }

    @GetMapping("/stats/{shortCode}")
    public Result<Long> stats(@PathVariable String shortCode) {
        return Result.success(clickLogService.getClickCount(shortCode));
    }
}