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
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.yilena.myShortLink.project.dao.ShortLinkMapper;
import org.yilena.myShortLink.project.entry.DO.ShortLinkDO;
import org.yilena.myShortLink.project.entry.DTO.request.RecycleBinRecoverReqDTO;
import org.yilena.myShortLink.project.entry.DTO.request.RecycleBinRemoveReqDTO;
import org.yilena.myShortLink.project.entry.DTO.request.RecycleBinSaveReqDTO;
import org.yilena.myShortLink.project.entry.DTO.request.ShortLinkRecycleBinPageReqDTO;
import org.yilena.myShortLink.project.entry.DTO.result.ShortLinkPageRespDTO;
import org.yilena.myShortLink.project.service.RecycleBinService;

/**
 * 回收站管理接口实现层
 * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：link）获取项目资料
 */
@Service
@RequiredArgsConstructor
public class RecycleBinServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements RecycleBinService {
    @Override
    public void saveRecycleBin(RecycleBinSaveReqDTO requestParam) {

    }

    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkRecycleBinPageReqDTO requestParam) {
        return null;
    }

    @Override
    public void recoverRecycleBin(RecycleBinRecoverReqDTO requestParam) {

    }

    @Override
    public void removeRecycleBin(RecycleBinRemoveReqDTO requestParam) {

    }
}
