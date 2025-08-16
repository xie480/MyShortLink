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

package org.yilena.myShortLink.admin.controller;


import cn.hutool.core.bean.BeanUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.yilena.myShortLink.admin.common.convention.result.Result;
import org.yilena.myShortLink.admin.common.convention.result.Results;
import org.yilena.myShortLink.admin.entry.DTO.request.UserLoginReqDTO;
import org.yilena.myShortLink.admin.entry.DTO.request.UserRegisterReqDTO;
import org.yilena.myShortLink.admin.entry.DTO.request.UserUpdateReqDTO;
import org.yilena.myShortLink.admin.entry.DTO.result.UserActualRespDTO;
import org.yilena.myShortLink.admin.entry.DTO.result.UserLoginRespDTO;
import org.yilena.myShortLink.admin.entry.DTO.result.UserRespDTO;
import org.yilena.myShortLink.admin.service.UserService;

/**
 * 用户管理控制层
 * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：link）获取项目资料
 */
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 根据用户名查询用户信息
     */
    @GetMapping("/api/short-link/admin/v1/user/{username}")
    public Result<UserRespDTO> getUserByUsername(@PathVariable("username") String username) {
        return Results.success(userService.getUserByUsername(username));
    }

    /**
     * 根据用户名查询无脱敏用户信息
     */
    @GetMapping("/api/short-link/admin/v1/actual/user/{username}")
    public Result<UserActualRespDTO> getActualUserByUsername(@PathVariable("username") String username) {
        return Results.success(BeanUtil.toBean(userService.getUserByUsername(username), UserActualRespDTO.class));
    }

    /**
     * 查询用户名是否存在
     */
    @GetMapping("/api/short-link/admin/v1/user/has-username")
    public Result<Boolean> hasUsername(@RequestParam("username") String username) {
        return Results.success(userService.isUsernameExist(username));
    }

    /**
     * 注册用户
     */
    @PostMapping("/api/short-link/admin/v1/user")
    public Result<Void> register(@RequestBody UserRegisterReqDTO requestParam) {
        userService.register(requestParam);
        return Results.success();
    }

    /**
     * 修改用户
     */
    @PutMapping("/api/short-link/admin/v1/user")
    public Result<Void> update(@RequestBody UserUpdateReqDTO requestParam) {
        userService.update(requestParam);
        return Results.success();
    }

    /**
     * 用户登录
     */
    @PostMapping("/api/short-link/admin/v1/user/login")
    public Result<UserLoginRespDTO> login(@RequestBody UserLoginReqDTO requestParam) {
        return Results.success(userService.login(requestParam));
    }

    /**
     * 检查用户是否登录
     */
    @GetMapping("/api/short-link/admin/v1/user/check-login")
    public Result<Boolean> checkLogin(@RequestParam("username") String username, @RequestParam("token") String token) {
        return Results.success(userService.checkLogin(username, token));
    }

    /**
     * 用户退出登录
     */
    @DeleteMapping("/api/short-link/admin/v1/user/logout")
    public Result<Void> logout(@RequestParam("username") String username, @RequestParam("token") String token) {
        userService.logout(username, token);
        return Results.success();
    }

//    // 刷新刷新令牌
//    @PostMapping("/refresh-token")
//    public Result<AuthResponse> refreshToken(
//            @RequestBody RefreshTokenRequest refreshRequest,
//            HttpServletRequest request
//    ) throws InterruptedException {
//        // 刷新令牌（返回新双令牌）
//        RefreshResult result = tokenSessionService.refreshTokenState(refreshRequest.getRefreshToken(), request);
//
//        // 解析新访问令牌信息
//        Claims accessClaims = tokenProvider.parseToken(result.getAccessToken());
//
//        // 构建响应
//        AuthResponse response = new AuthResponse(
//                result.getAccessToken(),
//                result.getRefreshToken(),
//                "Bearer",
//                (accessClaims.getExpiration().getTime() - System.currentTimeMillis()) / 1000,
//                tokenProvider.getRefreshExpiration()
//        );
//        return Results.success(response);
//    }
}
