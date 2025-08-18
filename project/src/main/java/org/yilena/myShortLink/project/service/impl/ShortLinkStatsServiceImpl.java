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

package org.yilena.myShortLink.project.service.impl;


import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.yilena.myShortLink.project.entry.DTO.request.ShortLinkGroupStatsAccessRecordReqDTO;
import org.yilena.myShortLink.project.entry.DTO.request.ShortLinkGroupStatsReqDTO;
import org.yilena.myShortLink.project.entry.DTO.request.ShortLinkStatsAccessRecordReqDTO;
import org.yilena.myShortLink.project.entry.DTO.request.ShortLinkStatsReqDTO;
import org.yilena.myShortLink.project.entry.DTO.result.ShortLinkStatsAccessRecordRespDTO;
import org.yilena.myShortLink.project.entry.DTO.result.ShortLinkStatsRespDTO;
import org.yilena.myShortLink.project.service.ShortLinkStatsService;

/**
 * 短链接监控接口实现层
 * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：link）获取项目资料
 */
@Service
@RequiredArgsConstructor
public class ShortLinkStatsServiceImpl implements ShortLinkStatsService {
    @Override
    public ShortLinkStatsRespDTO oneShortLinkStats(ShortLinkStatsReqDTO requestParam) {
        return null;
    }

    @Override
    public ShortLinkStatsRespDTO groupShortLinkStats(ShortLinkGroupStatsReqDTO requestParam) {
        return null;
    }

    @Override
    public IPage<ShortLinkStatsAccessRecordRespDTO> shortLinkStatsAccessRecord(ShortLinkStatsAccessRecordReqDTO requestParam) {
        return null;
    }

    @Override
    public IPage<ShortLinkStatsAccessRecordRespDTO> groupShortLinkStatsAccessRecord(ShortLinkGroupStatsAccessRecordReqDTO requestParam) {
        return null;
    }
}
