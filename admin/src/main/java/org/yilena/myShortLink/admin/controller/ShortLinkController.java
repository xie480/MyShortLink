package org.yilena.myShortLink.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.yilena.myShortLink.admin.common.convention.result.Result;
import org.yilena.myShortLink.admin.remote.DTO.request.ShortLinkCreateReqDTO;
import org.yilena.myShortLink.admin.remote.DTO.request.ShortLinkPageReqDTO;
import org.yilena.myShortLink.admin.remote.DTO.result.ShortLinkCreateRespDTO;
import org.yilena.myShortLink.admin.remote.DTO.result.ShortLinkPageRespDTO;
import org.yilena.myShortLink.admin.remote.ShortLinkActualRemoteService;

@RestController(value = "shortLinkControllerByAdmin")
@RequiredArgsConstructor
public class ShortLinkController {

    private final ShortLinkActualRemoteService shortLinkActualRemoteService = new ShortLinkActualRemoteService(){};


    // 创建短链接
    @PostMapping("/api/short-link/admin/v1/create")
    public Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody ShortLinkCreateReqDTO requestParam) {
        return shortLinkActualRemoteService.createShortLink(requestParam);
    }

    // 分页查询短链接
    @GetMapping("/api/short-link/admin/v1/page")
    public Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO requestParam) {
        return shortLinkActualRemoteService.pageShortLink(requestParam);
    }

    // 获取短链接标题
    @GetMapping("/api/short-link/v1/title")
    public Result<String> getTitleByUrl(@RequestParam("url") String url) {
        return shortLinkActualRemoteService.getTitleByUrl(url);
    }
}
