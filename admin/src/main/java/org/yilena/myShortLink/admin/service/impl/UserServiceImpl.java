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
import org.springframework.stereotype.Service;
import org.yilena.myShortLink.admin.dao.UserMapper;
import org.yilena.myShortLink.admin.entry.DO.UserDO;
import org.yilena.myShortLink.admin.entry.DTO.request.UserLoginReqDTO;
import org.yilena.myShortLink.admin.entry.DTO.request.UserRegisterReqDTO;
import org.yilena.myShortLink.admin.entry.DTO.request.UserUpdateReqDTO;
import org.yilena.myShortLink.admin.entry.DTO.result.UserLoginRespDTO;
import org.yilena.myShortLink.admin.entry.DTO.result.UserRespDTO;
import org.yilena.myShortLink.admin.service.UserService;

/**
 * 用户接口实现层
 * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：link）获取项目资料
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {
    @Override
    public UserRespDTO getUserByUsername(String username) {
        return null;
    }

    @Override
    public Boolean hasUsername(String username) {
        return null;
    }

    @Override
    public void register(UserRegisterReqDTO requestParam) {

    }

    @Override
    public void update(UserUpdateReqDTO requestParam) {

    }

    @Override
    public UserLoginRespDTO login(UserLoginReqDTO requestParam) {
        return null;
    }

    @Override
    public Boolean checkLogin(String username, String token) {
        return null;
    }

    @Override
    public void logout(String username, String token) {

    }
}
