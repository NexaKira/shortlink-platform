package com.sc.shortlinkcore.controller;

import com.sc.shortlinkcore.common.Result;
import com.sc.shortlinkcore.service.ShortLinkService;
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

    // POST 请求，路径 /shorten，参数名为 url
    @PostMapping("/shorten")
    public Result<String> shorten(@RequestParam @NotBlank(message = "URL不能为空") @URL(message = "URL格式不正确") String url) {
        return Result.success(shortLinkService.createShortLink(url));
    }

    // GET 请求，路径 /{shortCode}，比如 /abc123
    @GetMapping("/{shortCode}")
    public void redirect(@PathVariable String shortCode, HttpServletResponse response) throws IOException {
        String longUrl = shortLinkService.getLongUrl(shortCode);
        response.sendRedirect(longUrl);   // 重定向到原始长链接
    }
}