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


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yilena.myShortLink.admin.dao.GroupMapper;
import org.yilena.myShortLink.admin.entry.DO.GroupDO;
import org.yilena.myShortLink.admin.entry.DTO.request.ShortLinkGroupSortReqDTO;
import org.yilena.myShortLink.admin.entry.DTO.request.ShortLinkGroupUpdateReqDTO;
import org.yilena.myShortLink.admin.entry.DTO.result.ShortLinkGroupRespDTO;
import org.yilena.myShortLink.admin.service.GroupService;

import java.util.List;

/**
 * 短链接分组接口实现层
 * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：link）获取项目资料
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupServiceImpl extends ServiceImpl<GroupMapper, GroupDO> implements GroupService {
    @Override
    public void saveGroup(String groupName) {

    }

    @Override
    public void saveGroup(String username, String groupName) {

    }

    @Override
    public List<ShortLinkGroupRespDTO> listGroup() {
        return List.of();
    }

    @Override
    public void updateGroup(ShortLinkGroupUpdateReqDTO requestParam) {

    }

    @Override
    public void deleteGroup(String gid) {

    }

    @Override
    public void sortGroup(List<ShortLinkGroupSortReqDTO> requestParam) {

    }
}
