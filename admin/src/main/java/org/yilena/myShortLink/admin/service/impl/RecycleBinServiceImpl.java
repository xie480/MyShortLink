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


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.yilena.myShortLink.admin.common.convention.result.Result;
import org.yilena.myShortLink.admin.remote.DTO.request.ShortLinkRecycleBinPageReqDTO;
import org.yilena.myShortLink.admin.remote.DTO.result.ShortLinkPageRespDTO;
import org.yilena.myShortLink.admin.service.RecycleBinService;

/**
 * URL 回收站接口实现层
 * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：link）获取项目资料
 */
@Service(value = "recycleBinServiceImplByAdmin")
@RequiredArgsConstructor
public class RecycleBinServiceImpl implements RecycleBinService {
    @Override
    public Result<Page<ShortLinkPageRespDTO>> pageRecycleBinShortLink(ShortLinkRecycleBinPageReqDTO requestParam) {
        return null;
    }
}
