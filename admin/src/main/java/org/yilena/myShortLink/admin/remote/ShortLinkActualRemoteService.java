package org.yilena.myShortLink.admin.remote;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.yilena.myShortLink.admin.common.convention.result.Result;
import org.yilena.myShortLink.admin.remote.DTO.request.ShortLinkCreateReqDTO;
import org.yilena.myShortLink.admin.remote.DTO.request.ShortLinkPageReqDTO;
import org.yilena.myShortLink.admin.remote.DTO.result.ShortLinkCreateRespDTO;
import org.yilena.myShortLink.admin.remote.DTO.result.ShortLinkPageRespDTO;

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
        String result = HttpUtil.get("http://localhost:8001/api/short-link/v1/count", requestParamMap);
        JSONObject jsonObject = JSON.parseObject(result);
        if(ObjectUtil.isNull(jsonObject)){
            return List.of();
        }
        return ObjectUtil.defaultIfNull(jsonObject.getObject("data", new TypeReference<List<Map<String, Integer>>>() {}.getType()), List.of());
    }
}
