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


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.yilena.myShortLink.project.common.constant.RedisConstant;
import org.yilena.myShortLink.project.common.convention.errorCode.codes.SystemErrorCodes;
import org.yilena.myShortLink.project.common.convention.errorCode.codes.UserErrorCodes;
import org.yilena.myShortLink.project.common.convention.exception.SystemException;
import org.yilena.myShortLink.project.common.convention.exception.UserException;
import org.yilena.myShortLink.project.dao.ShortLinkMapper;
import org.yilena.myShortLink.project.entry.DO.ShortLinkDO;
import org.yilena.myShortLink.project.entry.DTO.request.RecycleBinRecoverReqDTO;
import org.yilena.myShortLink.project.entry.DTO.request.RecycleBinRemoveReqDTO;
import org.yilena.myShortLink.project.entry.DTO.request.RecycleBinSaveReqDTO;
import org.yilena.myShortLink.project.entry.DTO.request.ShortLinkRecycleBinPageReqDTO;
import org.yilena.myShortLink.project.entry.DTO.result.ShortLinkPageRespDTO;
import org.yilena.myShortLink.project.service.RecycleBinService;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;

/**
 * 回收站管理接口实现层
 * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：link）获取项目资料
 */
@Service
@RequiredArgsConstructor
public class RecycleBinServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements RecycleBinService {

    private final ShortLinkMapper shortLinkMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void saveRecycleBin(RecycleBinSaveReqDTO requestParam) {
        // 将状态改为未启用
        LambdaQueryWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getEnableStatus, 0)
                .eq(ShortLinkDO::getDelFlag, 0);
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .enableStatus(1)
                .build();
        int row = shortLinkMapper.update(shortLinkDO, updateWrapper);

        if(Boolean.FALSE.equals(SqlHelper.retBool(row))){
            throw new UserException(UserErrorCodes.SHORT_LINK_NOT_EXIST_OR_NOT_ENABLE);
        }

        // 删除缓存
        stringRedisTemplate.delete(String.format(RedisConstant.GOTO_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
    }

    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkRecycleBinPageReqDTO requestParam) {
        /*
            原方案肯定会有深分页的性能问题，但这里又涉及到分表查询问题，该如何避免深分页的情况下严格按时间倒叙进行查询？
            第一反应肯定是游标查询，但是分表如何使用游标？
            我认为可以每个分表各查pageSize行数据返回，然后再通过归并排序筛选出前pageSize行数据返回即可，虽然说会进行冗余查询，但是在高数据量的情况下肯定要比深分页性能好得多
            可惜这个参数设计就没有游标传进来，那我们就不得不抛弃掉游标查询方案了。
            那作为替代方案，我们可以使用延迟关联的方案，可惜这个方案虽然比深分页要好一些，但性能远不如游标查询，但没办法，这里就只能用延迟关联了
            延迟关联的核心是使用覆盖索引获取主键，所以我们这里必须给每个link表都加上一个联合索引：
            CREATE INDEX idx_user_created ON t_link_0 (gid, del_flag, enable_status, create_time DESC, id DESC);
            CREATE INDEX idx_user_updated ON t_link_1 (gid, del_flag, enable_status, update_time DESC, id DESC);
            CREATE INDEX idx_user_created ON t_link_2 (gid, del_flag, enable_status, create_time DESC, id DESC);
            CREATE INDEX idx_user_updated ON t_link_3 (gid, del_flag, enable_status, update_time DESC, id DESC);
            CREATE INDEX idx_user_created ON t_link_4 (gid, del_flag, enable_status, create_time DESC, id DESC);
            CREATE INDEX idx_user_updated ON t_link_5 (gid, del_flag, enable_status, update_time DESC, id DESC);
            CREATE INDEX idx_user_created ON t_link_6 (gid, del_flag, enable_status, create_time DESC, id DESC);
            CREATE INDEX idx_user_updated ON t_link_7 (gid, del_flag, enable_status, update_time DESC, id DESC);
            CREATE INDEX idx_user_created ON t_link_8 (gid, del_flag, enable_status, create_time DESC, id DESC);
            CREATE INDEX idx_user_updated ON t_link_9 (gid, del_flag, enable_status, update_time DESC, id DESC);
            CREATE INDEX idx_user_created ON t_link_10 (gid, del_flag, enable_status,create_time DESC, id DESC);
            CREATE INDEX idx_user_updated ON t_link_11 (gid, del_flag, enable_status,update_time DESC, id DESC);
            CREATE INDEX idx_user_created ON t_link_12 (gid, del_flag, enable_status,create_time DESC, id DESC);
            CREATE INDEX idx_user_updated ON t_link_13 (gid, del_flag, enable_status,update_time DESC, id DESC);
            CREATE INDEX idx_user_created ON t_link_14 (gid, del_flag, enable_status,create_time DESC, id DESC);
            CREATE INDEX idx_user_updated ON t_link_15 (gid, del_flag, enable_status,update_time DESC, id DESC);
         */

        // 首先通过覆盖索引获取主键
        List<Map<String, List<Long>>> allIdMapsResult = getMaps(requestParam);

        if(Boolean.TRUE.equals(allIdMapsResult.isEmpty())){
            throw new UserException(UserErrorCodes.NOT_FOUND_ERROR);
        }

        // 二次回表查询完整数据
        List<ShortLinkDO> allDos = listDosByIds(allIdMapsResult);

        /*
            开始排序
         */

        // 使用固定大小的最小堆来维护一个按照更新时间和ID排序的短链接列表
        PriorityQueue<ShortLinkDO> heap = new PriorityQueue<>(
                Comparator.comparing((ShortLinkDO o) -> o.getUpdateTime())
                        .thenComparingLong(ShortLinkDO::getId)
        );

        // 遍历所有短链接，保持堆中仅保留最新的指定数量的短链接
        allDos.forEach(shortLink -> {
            heap.offer(shortLink);
            if (heap.size() > requestParam.getSize()) {
                heap.poll();
            }
        });

        // 将堆中的元素转换为列表，并按时间、id倒序排序
        List<ShortLinkDO> shortLinkDOList = new ArrayList<>(heap);
        shortLinkDOList.sort((o1, o2) -> {
            int timeCmp = o2.getUpdateTime().compareTo(o1.getUpdateTime());
            if (timeCmp != 0) return timeCmp;
            return o2.getId().compareTo(o1.getId());
        });

        // 取前pageSize个数据返回
        List<ShortLinkPageRespDTO> shortLinkPageRespDTOS = shortLinkDOList.stream()
                .map(shortLinkDO -> BeanUtil.toBean(shortLinkDO, ShortLinkPageRespDTO.class))
                .toList();

        // 封装返回
        IPage<ShortLinkPageRespDTO> result = new Page<>(requestParam.getCurrent(), requestParam.getSize(), getTotalCount(requestParam.getGidList()));
        result.setRecords(shortLinkPageRespDTOS);


        /*
            emm，写到这里才发现其实来来回回总共也要发起四次sql分表查询请求，估计跟原方案的深分页也半斤八两了
            这也是延迟关联方案没有得到广泛使用的原因，它比传统的深分页要好一些，但是比起游标分页性能还是要差得多，因为涉及到多次回表查询
            不过此处就当开阔一下思维得了
         */
        return result;
    }

    private long getTotalCount(List<String> gidList) {
        try(var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            List<StructuredTaskScope.Subtask<Long>> countFutures = gidList.stream()
                    .map(gid -> scope.fork(() -> shortLinkMapper.countByGid(gid)))
                    .toList();

            scope.join();
            scope.throwIfFailed();

            return countFutures.stream()
                    .map(StructuredTaskScope.Subtask::get)
                    .mapToLong(Long::longValue)
                    .sum();
        } catch (Exception e) {
            throw new SystemException(SystemErrorCodes.SYSTEM_ERROR);
        }
    }

    private List<Map<String, List<Long>>> getMaps(ShortLinkRecycleBinPageReqDTO requestParam) {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            // 计算真实偏移量（页码转行数）
            long offset = (requestParam.getCurrent() - 1) * requestParam.getSize();
            // 以gid为分片键进行并行查询
            List<StructuredTaskScope.Subtask<Map<String, List<Long>>>> idFutures = requestParam.getGidList().stream()
                    .map(gid -> scope.fork(() -> {
                                // 只查询ID（利用覆盖索引）
                                List<Long> result = shortLinkMapper.listIdsByGid(gid, offset, requestParam.getSize());
                                return Map.of(gid, result);
                            }
                    ))
                    .toList();

            try {
                // 等待所有任务完成或失败
                scope.join();
                // 抛异常
                scope.throwIfFailed();
            } catch (InterruptedException e) {
                // 恢复中断
                Thread.currentThread().interrupt();
                throw new SystemException(SystemErrorCodes.SYSTEM_ERROR);
            } catch (ExecutionException e) {
                throw new SystemException(SystemErrorCodes.SYSTEM_ERROR);
            }

            // 获取结果
            List<Map<String, List<Long>>> allIdMaps = idFutures.stream()
                    .map(StructuredTaskScope.Subtask::get)
                    .filter(map -> {
                        // 获取 Map 中的 value
                        List<Long> ids = map.values().iterator().next();
                        // 过滤掉 null 或空的 List
                        return ids != null && !ids.isEmpty();
                    })
                    .toList();
            return allIdMaps;
        }
    }

    private List<ShortLinkDO> listDosByIds(List<Map<String, List<Long>>> allIdMaps) {
        try(var scope = new StructuredTaskScope.ShutdownOnFailure()){
            List<StructuredTaskScope.Subtask<List<ShortLinkDO>>> dosFutures = allIdMaps.stream()
                    .map(idMap -> scope.fork(() -> {
                        // 获取gid
                        String gid = idMap.keySet().iterator().next();
                        // 获取ID列表
                        List<Long> ids = idMap.get(gid);
                        // 查询完整数据
                        List<ShortLinkDO> dos = shortLinkMapper.listByIds(ids,gid);
                        return dos;
                    }))
                    .toList();

            try {
                scope.join();
                scope.throwIfFailed();
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                throw new SystemException(SystemErrorCodes.SYSTEM_ERROR);
            }

            // 收集结果
            List<ShortLinkDO> result = dosFutures.stream()
                    .map(StructuredTaskScope.Subtask::get)
                    .flatMap(List::stream)
                    .toList();
            return result;
        }
    }

    /*
        下面两个接口都是很普通的crud，所以写的很敷衍，但还是在原方案上有一定更改，因为原方案更敷衍
     */

    @Override
    public void recoverRecycleBin(RecycleBinRecoverReqDTO requestParam) {
        // 将状态改为启用
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getEnableStatus, 1)
                .eq(ShortLinkDO::getDelFlag, 0);
        ShortLinkDO shortLinkDO = shortLinkMapper.selectOne(queryWrapper);

        if(Boolean.TRUE.equals(ObjectUtil.isNull(shortLinkDO))){
            throw new UserException(UserErrorCodes.SHORT_LINK_NOT_EXIST_OR_ENABLE);
        }

        // 修改
        shortLinkMapper.update(
                ShortLinkDO.builder()
                        .enableStatus(0)
                        .build(),
                queryWrapper
        );

        // 重建缓存
        stringRedisTemplate.opsForValue().set(
                String.format(RedisConstant.GOTO_SHORT_LINK_KEY, shortLinkDO.getFullShortUrl()),
                shortLinkDO.getOriginUrl(),
                shortLinkDO.getValidDate().getTime() - new Date().getTime(),
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void removeRecycleBin(RecycleBinRemoveReqDTO requestParam) {
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getDelFlag, 0);
        ShortLinkDO shortLinkDO = shortLinkMapper.selectOne(queryWrapper);
        if(Boolean.TRUE.equals(ObjectUtil.isNull(shortLinkDO))){
            throw new UserException(UserErrorCodes.SHORT_LINK_NOT_EXIST);
        }
        if(Boolean.FALSE.equals(Objects.equals(shortLinkDO.getEnableStatus(), 0))){
            throw new UserException(UserErrorCodes.SHORT_LINK_IS_ENABLE);
        }

        // 逻辑删除
        shortLinkDO.setDelFlag(1);
        shortLinkDO.setDelTime(new Date().getTime());
        shortLinkMapper.update(shortLinkDO, queryWrapper);

        // 删除缓存
        stringRedisTemplate.delete(String.format(RedisConstant.GOTO_SHORT_LINK_KEY, shortLinkDO.getFullShortUrl()));
    }
}
