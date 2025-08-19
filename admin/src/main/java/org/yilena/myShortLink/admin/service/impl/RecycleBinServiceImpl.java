/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.yilena.myShortLink.admin.service.impl;


import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.yilena.myShortLink.admin.common.biz.user.UserContext;
import org.yilena.myShortLink.admin.common.convention.result.Result;
import org.yilena.myShortLink.admin.dao.GroupMapper;
import org.yilena.myShortLink.admin.remote.DTO.request.ShortLinkRecycleBinPageReqDTO;
import org.yilena.myShortLink.admin.remote.DTO.result.ShortLinkPageRespDTO;
import org.yilena.myShortLink.admin.remote.ShortLinkActualRemoteService;
import org.yilena.myShortLink.admin.service.RecycleBinService;

import java.util.List;

/**
 * URL 回收站接口实现层
 * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：link）获取项目资料
 */
@Service(value = "recycleBinServiceImplByAdmin")
@RequiredArgsConstructor
public class RecycleBinServiceImpl implements RecycleBinService {

    private final GroupMapper groupMapper;
    private final ShortLinkActualRemoteService shortLinkActualRemoteService = new ShortLinkActualRemoteService(){};

    @Override
    public Result<IPage<ShortLinkPageRespDTO>> pageRecycleBinShortLink(ShortLinkRecycleBinPageReqDTO requestParam) {
        /*
            这个接口的设计我都懒得喷了……实在是太垃圾了
            感觉不如将project模块的分页查询接口的实体改成游标，然后第一次查询的时候查询该用户所有的分组并把id以toString()转换成字符串存入缓存中，TTL为5min……
            现在这个admin是service层完全就是意义不明的设计……
         */

        // 根据请求头的username查询用户所拥有的分组
        List<String> groupIdList = groupMapper.listGroupIdByUsername(UserContext.getUsername());
        requestParam.setGidList(groupIdList);
        return shortLinkActualRemoteService.pageRecycleShortLink(requestParam.getGidList(), requestParam.getCurrent(),requestParam.getSize());
    }
}
