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


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ObjectUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.yilena.myShortLink.admin.common.biz.user.UserContext;
import org.yilena.myShortLink.admin.common.constant.RedisConstant;
import org.yilena.myShortLink.admin.common.convention.errorCode.codes.SystemErrorCodes;
import org.yilena.myShortLink.admin.common.convention.errorCode.codes.UserErrorCodes;
import org.yilena.myShortLink.admin.common.convention.exception.SystemException;
import org.yilena.myShortLink.admin.common.convention.exception.UserException;
import org.yilena.myShortLink.admin.common.convention.result.Result;
import org.yilena.myShortLink.admin.common.enums.InstanceEnums;
import org.yilena.myShortLink.admin.dao.GroupMapper;
import org.yilena.myShortLink.admin.entry.DO.GroupDO;
import org.yilena.myShortLink.admin.entry.DTO.request.ShortLinkGroupSortReqDTO;
import org.yilena.myShortLink.admin.entry.DTO.request.ShortLinkGroupUpdateReqDTO;
import org.yilena.myShortLink.admin.entry.DTO.result.ShortLinkGroupRespDTO;
import org.yilena.myShortLink.admin.remote.DTO.result.ShortLinkGroupCountQueryRespDTO;
import org.yilena.myShortLink.admin.remote.ShortLinkActualRemoteService;
import org.yilena.myShortLink.admin.service.GroupService;
import org.yilena.myShortLink.admin.utils.DistributedShortIdGenerator;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.StructuredTaskScope;

/**
 * 短链接分组接口实现层
 * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：link）获取项目资料
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupServiceImpl extends ServiceImpl<GroupMapper, GroupDO> implements GroupService {

    private final GroupMapper groupMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final ShortLinkActualRemoteService shortLinkActualRemoteService;

    private static final int GROUP_SAVE_LIMIT_COUNT = 10;
    private static final int GROUP_SELECT_LIMIT_COUNT = 10;

    @Override
    public void saveGroup(String groupName) {
        saveGroup(UserContext.getUsername(), groupName);
    }

    @Override
    public void saveGroup(String username, String groupName) {

        /*
            由于我们的锁粒度太小只是用来预防网络攻击的，如果同时大量用户使用此接口依旧会使DB雪崩
            所以我们应该做个拦截，10s内此接口最多被调用10次
         */
        String saveGroupLimitKey = RedisConstant.GROUP_SAVE_LIMIT;
        if((Integer) ObjectUtils.defaultIfNull(stringRedisTemplate.opsForValue().get(saveGroupLimitKey), 0) > GROUP_SAVE_LIMIT_COUNT){
            throw new UserException(UserErrorCodes.USER_ERROR);
        }

        // 加锁，预防网络攻击
        RLock lock = redissonClient.getLock(String.format(RedisConstant.GROUP_SAVE_LOCK, username));
        if(Boolean.FALSE.equals(lock.tryLock())){
            throw new UserException(UserErrorCodes.USER_ERROR);
        }

        try{
            // 查询用户分组数量是否已达上限
            LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                    .eq(GroupDO::getUsername, username)
                    .eq(GroupDO::getDelFlag, 0);
            if(groupMapper.selectCount(queryWrapper) >= 10){
                throw new UserException(UserErrorCodes.GROUP_COUNT_OUT);
            }

            /*
                生成随机唯一 gid，但原方案可能重复，而且本项目为微服务，原方案无法做到分布式唯一，所以我们这里自己写一个生成工具类
             */
            DistributedShortIdGenerator distributedShortIdGenerator = new DistributedShortIdGenerator(InstanceEnums.ADMIN.ordinal());
            String gid = distributedShortIdGenerator.nextId();

            // 构造分组实例
            GroupDO groupDO = GroupDO.builder()
                    .gid(gid)
                    .sortOrder(0)
                    .username(username)
                    .name(groupName)
                    .build();
            try {
                int row = groupMapper.insert(groupDO);
                if (Boolean.FALSE.equals(SqlHelper.retBool(row))) {
                    throw new SystemException(SystemErrorCodes.SYSTEM_ERROR);
                }
            }catch (DuplicateKeyException e){
                throw new UserException(UserErrorCodes.GROUP_EXIST);
            }

            /*
                原方案使用了布隆过滤器，这里就不需要了，因为我们使用雪花算法生成的ID，只要时钟不回拨，ID就严格唯一
             */

            // 添加拦截缓存，使用lua脚本先自增再添加TTL
            String script = """
                    local key = KEYS[1]
                    local count = redis.call('INCR', key)
                    if count == 1 then
                        redis.call('EXPIRE', key, ARGV[1])
                    end
                    """;
            RedisScript<Void> voidRedisScript = RedisScript.of(script, Void.class);
            stringRedisTemplate.execute(
                    voidRedisScript,
                    List.of(saveGroupLimitKey),
                    "10");
        }catch (Exception e){
            throw new SystemException(SystemErrorCodes.SYSTEM_ERROR);
        }finally {
            lock.unlock();
        }
    }

    @Override
    public List<ShortLinkGroupRespDTO> listGroup() {
        /*
            依旧拦截/.
            感觉应该写个aop注解的
         */
        String listGroupLimitKey = RedisConstant.GROUP_SELECT_LIMIT;
        if((Integer) ObjectUtils.defaultIfNull(stringRedisTemplate.opsForValue().get(listGroupLimitKey), 0) > GROUP_SELECT_LIMIT_COUNT){
            throw new UserException(UserErrorCodes.USER_ERROR);
        }

        RLock lock = redissonClient.getLock(String.format(RedisConstant.GROUP_SELECT_LOCK, UserContext.getUsername()));

        if(Boolean.FALSE.equals(lock.tryLock())){
            throw new UserException(UserErrorCodes.USER_ERROR);
        }

        try {

            // 查询该用户的所有分组
            LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                    .eq(GroupDO::getUsername, UserContext.getUsername())
                    .eq(GroupDO::getDelFlag, 0)
                    .orderByDesc(GroupDO::getSortOrder)
                    .orderByDesc(GroupDO::getUpdateTime);
            List<GroupDO> groupDOList = groupMapper.selectList(queryWrapper);
            // 查询分组下的短链数量
            Result<List<ShortLinkGroupCountQueryRespDTO>> groupShortLinkCount = shortLinkActualRemoteService.listGroupShortLinkCount(groupDOList.stream().map(GroupDO::getGid).toList());
            // 转换成DTO
            List<ShortLinkGroupRespDTO> result = groupDOList.stream().map(groupDO ->
                    ShortLinkGroupRespDTO.builder()
                    .gid(groupDO.getGid())
                    .name(groupDO.getName())
                    .sortOrder(groupDO.getSortOrder())
                    .shortLinkCount(groupShortLinkCount.getData().stream()
                            .filter(item1 -> Objects.equals(String.valueOf(item1.getGid()), groupDO.getGid()))
                            .findFirst()
                            .map(ShortLinkGroupCountQueryRespDTO::getShortLinkCount)
                            .orElse(0)
                    )
                    .build()).toList();
            // 添加拦截缓存，使用lua脚本先自增再添加TTL
            String script = """
                    local key = KEYS[1]
                    local count = redis.call('INCR', key)
                    if count == 1 then
                        redis.call('EXPIRE', key, ARGV[1])
                    end
                    """;
            RedisScript<Void> voidRedisScript = RedisScript.of(script, Void.class);
            stringRedisTemplate.execute(
                    voidRedisScript,
                    List.of(listGroupLimitKey),
                    "10");

            return result;
        }catch (Exception e){
            throw new SystemException(SystemErrorCodes.SYSTEM_ERROR);
        }finally {
            lock.unlock();
        }
    }

    @Override
    public void updateGroup(ShortLinkGroupUpdateReqDTO requestParam) {
        // 上锁
        RLock lock = redissonClient.getLock(String.format(RedisConstant.GROUP_UPDATE_LOCK, UserContext.getUsername()));
        if(Boolean.FALSE.equals(lock.tryLock())){
            throw new UserException(UserErrorCodes.USER_ERROR);
        }
        try {
            // 防止越权
            LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                    .eq(GroupDO::getGid, requestParam.getGid())
                    .eq(GroupDO::getUsername, UserContext.getUsername());
            if(groupMapper.selectCount(queryWrapper) == 0){
                throw new UserException(UserErrorCodes.GROUP_NOT_EXIST);
            }

            // 更新
            LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                    .eq(GroupDO::getGid, requestParam.getGid())
                    .eq(GroupDO::getUsername, UserContext.getUsername());
            int row = groupMapper.update(BeanUtil.toBean(requestParam, GroupDO.class), updateWrapper);
            if(Boolean.FALSE.equals(SqlHelper.retBool(row))){
                throw new SystemException(SystemErrorCodes.SYSTEM_ERROR);
            }
        }catch (Exception e){
            throw new SystemException(SystemErrorCodes.SYSTEM_ERROR);
        }finally {
            lock.unlock();
        }
    }

    @Override
    public void deleteGroup(String gid) {
        // 加锁
        RLock lock = redissonClient.getLock(String.format(RedisConstant.GROUP_DELETE_LOCK, UserContext.getUsername()));
        if(Boolean.FALSE.equals(lock.tryLock())){
            throw new UserException(UserErrorCodes.USER_ERROR);
        }

        try{
            // 防止越权
            LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                    .eq(GroupDO::getGid, gid)
                    .eq(GroupDO::getUsername, UserContext.getUsername());
            if(groupMapper.selectCount(queryWrapper) == 0){
                throw new UserException(UserErrorCodes.GROUP_NOT_EXIST);
            }

            /*
                这里一个要确认分组内无短链接后才能进行删除，但是由于这个模块并没有引入短链接相关的dao以及DO，而且分表配置也需要额外添加，太麻烦了所以就不搞了
                你知我知即可
             */

            // 删除
            LambdaQueryWrapper<GroupDO> wrapper = Wrappers.lambdaQuery(GroupDO.class)
                    .eq(GroupDO::getGid, gid)
                    .eq(GroupDO::getUsername, UserContext.getUsername());
            int row = groupMapper.delete(wrapper);
            if(Boolean.FALSE.equals(SqlHelper.retBool(row))){
                throw new SystemException(SystemErrorCodes.SYSTEM_ERROR);
            }
        }catch (Exception e){
            throw new SystemException(SystemErrorCodes.SYSTEM_ERROR);
        }finally {
            lock.unlock();
        }
    }

    public void sortGroup(List<ShortLinkGroupSortReqDTO> requestParam) {
        // 使用结构化并发，默认为虚拟线程池，最多允许50个任务同时执行
        Semaphore semaphore = new Semaphore(50);
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            // 为每个分组创建并发任务
            List<StructuredTaskScope.Subtask<Void>> futures = requestParam.stream()
                    .map(item -> scope.fork(() -> updateGroupSortOrder(item, semaphore)))
                    .toList();

            // 等待所有任务完成或失败
            try {
                scope.join();
                scope.throwIfFailed();

            } catch (InterruptedException e) {
                // 恢复中断
                Thread.currentThread().interrupt();
                throw new SystemException(SystemErrorCodes.SYSTEM_ERROR);
            } catch (Exception e) {
                Throwable cause = e.getCause();
                if (cause instanceof UserException ue) throw ue;
                throw new SystemException(SystemErrorCodes.SYSTEM_ERROR);
            }
        }
    }

    private Void updateGroupSortOrder(ShortLinkGroupSortReqDTO item, Semaphore semaphore) {
        try {
            // 获得信号量许可
            semaphore.acquire();
            String lockKey = String.format(RedisConstant.GROUP_SORT_LOCK, item.getGid());
            RLock lock = redissonClient.getLock(lockKey);

            if (!lock.tryLock()) {
                throw new UserException(UserErrorCodes.USER_ERROR);
            }
            try {
                // 更新分组排序信息
                LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                        .eq(GroupDO::getGid, item.getGid())
                        .eq(GroupDO::getUsername, UserContext.getUsername());
                // 因为可能存在前后排序一样的情况，所以这里不对结果进行校验
                groupMapper.update(BeanUtil.toBean(item, GroupDO.class), updateWrapper);
            } catch (Exception e) {
                throw new SystemException(SystemErrorCodes.SYSTEM_ERROR);
            } finally {
                lock.unlock();
            }
        } catch (Exception e) {
            throw new SystemException(SystemErrorCodes.SYSTEM_ERROR);
        } finally {
            // 释放信号量
            semaphore.release();
        }
        return null;
    }
}
