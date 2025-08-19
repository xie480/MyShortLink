package org.yilena.myShortLink.admin.remote;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.yilena.myShortLink.admin.common.convention.result.Result;
import org.yilena.myShortLink.admin.entry.DTO.request.RecycleBinRecoverReqDTO;
import org.yilena.myShortLink.admin.entry.DTO.request.RecycleBinRemoveReqDTO;
import org.yilena.myShortLink.admin.entry.DTO.request.RecycleBinSaveReqDTO;
import org.yilena.myShortLink.admin.remote.DTO.request.ShortLinkCreateReqDTO;
import org.yilena.myShortLink.admin.remote.DTO.request.ShortLinkPageReqDTO;
import org.yilena.myShortLink.admin.remote.DTO.result.ShortLinkCreateRespDTO;
import org.yilena.myShortLink.admin.remote.DTO.result.ShortLinkPageRespDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface ShortLinkActualRemoteService {

    // 创建短链接
    default Result<ShortLinkCreateRespDTO> createShortLink(ShortLinkCreateReqDTO requestParam) {
        Map<String, Object> requestParamMap = BeanUtil.beanToMap(requestParam);
        String result = HttpUtil.post("http://127.0.0.1:8001/api/short-link/v1/create", requestParamMap);
        return JSON.parseObject(result, new TypeReference<>() {});
    }

    // 分页查询短链接
    default Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO requestParam) {
        Map<String, Object> requestParamMap = BeanUtil.beanToMap(requestParam);
        String result = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/page", requestParamMap);
        return JSON.parseObject(result, new TypeReference<>() {});
    }

    // 统计分组内短链接的数量
    default List<Map<String, Integer>> listGroupShortLinkCount(List<String> requestParam) {
        Map<String, Object> requestParamMap = BeanUtil.beanToMap(requestParam);
        String result = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/count", requestParamMap);
        JSONObject jsonObject = JSON.parseObject(result);
        if(ObjectUtil.isNull(jsonObject)){
            return List.of();
        }
        return ObjectUtil.defaultIfNull(jsonObject.getObject("data", new TypeReference<List<Map<String, Integer>>>() {}.getType()), List.of());
    }

    // 获取网站标题
    default Result<String> getTitleByUrl(String url) {
        Map<String, Object> requestParamMap = BeanUtil.beanToMap(url);
        String result = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/title", requestParamMap);
        return JSON.parseObject(result, new TypeReference<>() {});
    }

    /**
     * 保存回收站
     */
    default Result<Void> saveRecycleBin(@RequestBody RecycleBinSaveReqDTO requestParam) {
        Map<String, Object> requestParamMap = BeanUtil.beanToMap(requestParam);
        String result = HttpUtil.post("http://127.0.0.1:8001/api/short-link/v1/recycle-bin/save", requestParamMap);
        return JSON.parseObject(result, new TypeReference<>() {});
    }

    /**
     * 分页查询回收站短链接
     */
    default Result<IPage<ShortLinkPageRespDTO>> pageRecycleShortLink(@RequestParam("gidList") List<String> gidList,
                                                                     @RequestParam("current") Long current,
                                                                     @RequestParam("size") Long size) {
        Map<String, Object> requestParamMap = new HashMap<>();
        requestParamMap.put("gidList", gidList);
        requestParamMap.put("current", current);
        requestParamMap.put("size", size);
        String result = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/recycle-bin/page", requestParamMap);
        // 真是想提一嘴，这个接口写的又垃圾又垃圾，真的是垃圾中的垃圾，原方案的设计也太不经大脑思考了
        return JSON.parseObject(result, new TypeReference<>() {});
    }

    /**
     * 恢复短链接
     */
    default Result<Void> recoverRecycleBin(@RequestBody RecycleBinRecoverReqDTO requestParam) {
        Map<String, Object> requestParamMap = BeanUtil.beanToMap(requestParam);
        String result = HttpUtil.post("http://127.0.0.1:8001/api/short-link/v1/recycle-bin/recover", requestParamMap);
        return JSON.parseObject(result, new TypeReference<>() {});
    }

    /**
     * 移除短链接
     */
    default Result<Void> removeRecycleBin(@RequestBody RecycleBinRemoveReqDTO requestParam) {
        Map<String, Object> requestParamMap = BeanUtil.beanToMap(requestParam);
        String result = HttpUtil.post("http://127.0.0.1:8001/api/short-link/v1/recycle-bin/remove", requestParamMap);
        return JSON.parseObject(result, new TypeReference<>() {});
    }
}
