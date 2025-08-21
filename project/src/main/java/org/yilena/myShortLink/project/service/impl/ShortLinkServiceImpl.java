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
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yilena.myShortLink.project.common.constant.RedisConstant;
import org.yilena.myShortLink.project.common.convention.errorCode.codes.SystemErrorCodes;
import org.yilena.myShortLink.project.common.convention.errorCode.codes.UserErrorCodes;
import org.yilena.myShortLink.project.common.convention.exception.SystemException;
import org.yilena.myShortLink.project.common.convention.exception.UserException;
import org.yilena.myShortLink.project.common.enums.VailDateTypeEnum;
import org.yilena.myShortLink.project.dao.ShortLinkGotoMapper;
import org.yilena.myShortLink.project.dao.ShortLinkMapper;
import org.yilena.myShortLink.project.entry.DO.ShortLinkDO;
import org.yilena.myShortLink.project.entry.DO.ShortLinkGotoDO;
import org.yilena.myShortLink.project.entry.DTO.request.*;
import org.yilena.myShortLink.project.entry.DTO.result.ShortLinkBatchCreateRespDTO;
import org.yilena.myShortLink.project.entry.DTO.result.ShortLinkCreateRespDTO;
import org.yilena.myShortLink.project.entry.DTO.result.ShortLinkGroupCountQueryRespDTO;
import org.yilena.myShortLink.project.entry.DTO.result.ShortLinkPageRespDTO;
import org.yilena.myShortLink.project.mq.producer.ShortLinkStatsSaveProducer;
import org.yilena.myShortLink.project.service.ShortLinkService;
import org.yilena.myShortLink.project.service.UrlTitleService;
import org.yilena.myShortLink.project.utils.LinkUtil;
import org.yilena.myShortLink.project.utils.ShortCodeGenerator;
import org.yilena.myShortLink.project.utils.UVSymbolGenerator;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 短链接接口实现层
 * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：link）获取项目资料
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements ShortLinkService {

    private final ShortCodeGenerator shortCodeGenerator;
    private final ShortLinkMapper shortLinkMapper;
    private final RBloomFilter<String> shortLinkFullUrlCreatePenetrationBloomFilter;
    private final ShortLinkGotoMapper shortLinkGotoMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final UrlTitleService urlTitleService;
    private final UVSymbolGenerator uvSymbolGenerator = new UVSymbolGenerator(1L);
    private final ShortLinkStatsSaveProducer shortLinkStatsSaveProducer;

    // 本地域名
    @Value("${short-link.domain.default}")
    private String createShortLinkDefaultDomain;

    @Transactional
    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        /*
            原始链接的校验

            关于这个方法我一开始还以为能学到一些什么东西，结果就只是通过遍历合法域名来判断是否是白名单内的域名而已……

            但我也基本查了一下，如果想要自己写的话基本也就只能是比对本地的黑白名单
            想要更加精准的话必须引入第三方API，所以这个其实并不是多高级的知识，但我们也可以分析一下引入API的方案思路：
            当请求打进来时：
            1. 判断网址格式是否正确
            1.1 不正确：直接打回
            1.2 继续下一步
            2. 提前将黑名单域名进行缓存预热，然后在这里我们进行匹配
            2.1 匹配成功：直接打回
            2.2 继续下一步
            3. 接着就是使用第三方API进行校验
            3.1 判断为危险，域名放入黑名单并打回
            3.2 通过
         */

        /*
            生成随机后缀，这里我们采用哈希 + 雪花 + Base62 方案生成6位随机后缀
            虽然我觉得这里也可以采用分组的GID那种方案，不依赖于原始网址直接生成即可，因为不需要哈希所以也不用担心冲突的可能，
            但是有些时候我们需要依赖的场景的话则需要哈希，所以这里就当学习一种新方法，
            过程大致如下：
            1. 对原始网址进行哈希
            2. 取哈希值前8位
            3. 使用雪花算法生成唯一id
            4. 用位运算将两个值混合运算
            5. 取模映射到6位空间
            6. 进行Base62编码
            重试策略也在里面
         */
        String suffix = shortCodeGenerator.generateUniqueShortCode(requestParam.getOriginUrl());
        // 合并成完整url
        String fullShortUrl = STR."\{createShortLinkDefaultDomain}/\{suffix}";
        // 转化成DO
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(requestParam.getDomain())
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .createdType(requestParam.getCreatedType())
                .validDateType(requestParam.getValidDateType())
                .validDate(requestParam.getValidDate())
                .describe(requestParam.getDescribe())
                .shortUri(suffix)
                .enableStatus(0)
                .fullShortUrl(fullShortUrl)
                .favicon(urlTitleService.getFaviconByUrl(requestParam.getOriginUrl()))
                .build();

        /*
            这里直接把DB操作暴露在外，有雪崩的风险
            但是如果加锁，锁的粒度不好把控，
            所以这里最好做拦截限流，像之前的redis方案，
            但是也可以用sentinel，这种新增的接口最好用sentinel会更好一些
         */

        // 插入
        int row = shortLinkMapper.insert(shortLinkDO);
        if(Boolean.FALSE.equals(SqlHelper.retBool(row))){
            throw new SystemException(SystemErrorCodes.SYSTEM_ERROR);
        }

        /*
            原方案把上面的insert操作用try块包裹，catch到重复插入异常后，查询DB是否真的重复，若重复就不加入布隆过滤器
            原方案之所以这样做，是因为原方案的后缀生成策略是网址 + 时间戳然后对哈希取模后进行base62编码，这种策略再并发场景下是可能出现重复的，
            但是我们这里不需要，因为我这个方案引入了雪花算法，所以我们几乎不需要考虑重复的问题，可见我这个方案的优越性，而且性能一点也不必原方案差，
            为什么雪花算法几乎不会重复？因为该算法是每毫秒为单位处理请求，对于同一毫秒打进来的请求会进行序列号分配，当序列号耗尽后则会让后续请求等待下一毫秒的序列号。
            那么雪花算法在什么场景下会重复呢？
            1. 时钟回拨，对于这个场景我们做了拦截，所以不需要担心
            2. 机器ID或数据中心ID冲突，这个则是一开始配置的时候没配置好，跟我们代码无关
            3. 服务在同一毫秒内崩溃并重启成功，这个场景是理论上会重复，实际上这个场景怎么可能会发生，Java的缺点之一就是服务启动时间长
            4. 时间戳溢出，这个场景是最可能发生的，因为我们的雪花算法最多支持69年内不重复，不过解决方案也很简单，再要到期的时候将起始时间戳更新就行了
            所以综上，雪花算法几乎不可能重复且支持百万级QTS，所以我们这里也不用担心MySQL重复插入问题
         */

        /*
            原方案这里需要放入布隆过滤器，但是我们在生成后缀的时候使用了原方案的布隆过滤器，所以这里我们就不需要使用了
         */

        // 放入goto隐射表
        ShortLinkGotoDO shortLinkGotoDO = ShortLinkGotoDO.builder()
                .gid(shortLinkDO.getGid())
                .fullShortUrl(shortLinkDO.getFullShortUrl())
                .build();
        shortLinkGotoMapper.insert(shortLinkGotoDO);

        // 放入redis缓存
        stringRedisTemplate.opsForValue().set(
                String.format(RedisConstant.GOTO_SHORT_LINK_KEY, fullShortUrl),
                requestParam.getOriginUrl(),
                requestParam.getValidDate().getTime() - new Date().getTime(),
                TimeUnit.MILLISECONDS
        );

        // 这里的布隆过滤器是防止路由跳转时的缓存穿透问题
        shortLinkFullUrlCreatePenetrationBloomFilter.add(fullShortUrl);

        return ShortLinkCreateRespDTO.builder()
                .fullShortUrl(STR."http://\{shortLinkDO.getFullShortUrl()}")
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .build();
    }

    @Override
    public ShortLinkCreateRespDTO createShortLinkByLock(ShortLinkCreateReqDTO requestParam) {
        /*
            这个接口就是把原方案中上面的那个创建接口中的布隆过滤器拦截改成锁来保证生成的ID唯一且不会重复插入
            但是我们修改后的方案完全不需要考虑这些问题，具体原因上面也已经说了，
            然后对于分布式锁性能远低于布隆过滤器的原因其实也很简单，就是使用锁的话锁的粒度会很小，并行区域就变得很小，大部分替换为串行处理了，
            所以这个接口也是废弃了
         */
        return null;
    }

    @Override
    public ShortLinkBatchCreateRespDTO batchCreateShortLink(ShortLinkBatchCreateReqDTO requestParam) {
        /*
            这个接口的话，原方案大概就是逐个遍历短链接，然后调用上面那个创建单条短链接的接口，最后封装返回记录，不得不说设计的很垃圾
            要我重构的话，我会先对传参的短链接list进行分片，然后在structuredTaskScope里并行化处理，
            具体处理流程则是先创建一个队列或者直接用异步回调，并行代码块里逐个生成唯一后缀封装成DO放入队列中，当队列长度到达一定长度后，
            批量地插入DB然后存缓存，再将该批结果封装放入结果集，再清空队列等待下一批次……
            这才是叫批量创建，而不是一个套皮的接口，
            这个跟之前那个牛券的Excel导入接口很像，所以这里就不写了，因为也学不到新东西
         */
        return null;
    }

    @Transactional
    @Override
    public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
        // 查询原本短链接DO
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getOriginGid())
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 0);
        ShortLinkDO oldShortLinkDO = shortLinkMapper.selectOne(queryWrapper);
        // 如果短链接不存在
        if(oldShortLinkDO == null){
            throw new UserException(UserErrorCodes.SHORT_LINK_NOT_EXIST);
        }

        // 如果没有变更分组
        if(Boolean.FALSE.equals(Objects.equals(requestParam.getGid(), oldShortLinkDO.getGid()))){
            // 直接构造实体进行修改
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                    .eq(ShortLinkDO::getGid, requestParam.getGid())
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 0)
                    .set(Objects.equals(requestParam.getValidDateType(), VailDateTypeEnum.PERMANENT.getType()), ShortLinkDO::getValidDate, null);
            ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                    .favicon(Objects.equals(requestParam.getOriginUrl(), oldShortLinkDO.getOriginUrl()) ? oldShortLinkDO.getFavicon() : urlTitleService.getFaviconByUrl(requestParam.getOriginUrl()))
                    .domain(oldShortLinkDO.getDomain())
                    .shortUri(oldShortLinkDO.getShortUri())
                    .createdType(oldShortLinkDO.getCreatedType())
                    .gid(requestParam.getGid())
                    .originUrl(requestParam.getOriginUrl())
                    .describe(requestParam.getDescribe())
                    .validDateType(requestParam.getValidDateType())
                    .validDate(requestParam.getValidDate())
                    .build();
            shortLinkMapper.update(shortLinkDO, updateWrapper);
        }
        // 如果变更了分组
        else{
            // 上写锁，避免修改正在统计的短链接的gid
            RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(String.format(RedisConstant.Short_LINK_READ_WRITE_LOCK, requestParam.getFullShortUrl()));
            RLock writeLock = readWriteLock.writeLock();
            // 尝试获取锁，3s超时
            try {
                if(Boolean.FALSE.equals(writeLock.tryLock(3, TimeUnit.SECONDS))){
                    // 没抢到锁则证明正在统计中
                    throw new SystemException(SystemErrorCodes.SYSTEM_ERROR);
                }
            } catch (InterruptedException e) {
                throw new SystemException(SystemErrorCodes.SYSTEM_ERROR);
            }

            /*
                竞争成功
             */

            try {
                // 先删除原本的分组，但是这里的话删除到新增之间会存在短暂的空白期，使得用户访问原本的网址会访问不到
                // 所以解决方法就是先增后删
                ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                        .favicon(Objects.equals(requestParam.getOriginUrl(), oldShortLinkDO.getOriginUrl()) ? oldShortLinkDO.getFavicon() : urlTitleService.getFaviconByUrl(requestParam.getOriginUrl()))
                        .domain(oldShortLinkDO.getDomain())
                        .originUrl(requestParam.getOriginUrl())
                        .gid(requestParam.getGid())
                        .createdType(oldShortLinkDO.getCreatedType())
                        .validDateType(requestParam.getValidDateType())
                        .validDate(requestParam.getValidDate())
                        .describe(requestParam.getDescribe())
                        .shortUri(oldShortLinkDO.getShortUri())
                        .enableStatus(oldShortLinkDO.getEnableStatus())
                        .totalPv(oldShortLinkDO.getTotalPv())
                        .totalUv(oldShortLinkDO.getTotalUv())
                        .totalUip(oldShortLinkDO.getTotalUip())
                        .fullShortUrl(oldShortLinkDO.getFullShortUrl())
                        .delTime(0L)
                        .build();
                shortLinkMapper.insert(shortLinkDO);
                // 逻辑删除
                LambdaUpdateWrapper<ShortLinkDO> linkUpdateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                        .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(ShortLinkDO::getGid, oldShortLinkDO.getGid())
                        .eq(ShortLinkDO::getDelFlag, 0)
                        .eq(ShortLinkDO::getDelTime, 0L)
                        .eq(ShortLinkDO::getEnableStatus, 0);
                ShortLinkDO delShortLinkDO = ShortLinkDO.builder()
                        .delTime(System.currentTimeMillis())
                        .build();
                delShortLinkDO.setDelFlag(1);
                shortLinkMapper.update(delShortLinkDO, linkUpdateWrapper);

                // 更新goto路由映射表
                LambdaQueryWrapper<ShortLinkGotoDO> linkGotoQueryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(ShortLinkGotoDO::getGid, oldShortLinkDO.getGid());
                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(linkGotoQueryWrapper);
                shortLinkGotoMapper.delete(linkGotoQueryWrapper);
                shortLinkGotoDO.setGid(requestParam.getGid());
                shortLinkGotoMapper.insert(shortLinkGotoDO);
            }catch (Exception e){
                throw new SystemException(SystemErrorCodes.SYSTEM_ERROR);
            }finally {
                writeLock.unlock();
            }
        }

        // 如果对应的条件有变动的话也需要删除缓存，避免数据不一致

        /*
            对于有效缓存需要判断一下条件：
            1. 永久或限时类型的变更
            2. 有效期的变更
            3. 原链接的变更
         */
        if(Boolean.FALSE.equals(
                Objects.equals(requestParam.getValidDateType(), oldShortLinkDO.getValidDateType())
                || Objects.equals(requestParam.getValidDate(), oldShortLinkDO.getValidDate())
                || Objects.equals(requestParam.getOriginUrl(), oldShortLinkDO.getOriginUrl())
        )){
            stringRedisTemplate.delete(String.format(RedisConstant.GOTO_SHORT_LINK_KEY, requestParam.getFullShortUrl()));

            /*
                因为当前链接可能是由未启用改为启用，所以或许存在恶意攻击的空缓存也得删除掉
             */

            Date now = new Date();

            // 如果原本过期了，或者根本就未启用
            if(Boolean.TRUE.equals(
                    (ObjectUtil.isNotNull(oldShortLinkDO.getValidDate())
                    && oldShortLinkDO.getValidDate().before(now))
            )){
                // 进入if块的话就代表原本的短链接是无法使用的，所以可能会存在空缓存
                // 判断此次更改是否把短链接启用了
                if (Objects.equals(requestParam.getValidDateType(), VailDateTypeEnum.PERMANENT.getType()) || requestParam.getValidDate().after(now)) {
                    stringRedisTemplate.delete(String.format(RedisConstant.GOTO_IS_NULL_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
                }
            }
        }
    }

    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {
        // redis限流省略……

        IPage<ShortLinkDO> resultPage = shortLinkMapper.pageShortLink(requestParam);
        return resultPage.convert(each -> {
            ShortLinkPageRespDTO result = BeanUtil.toBean(each, ShortLinkPageRespDTO.class);
            result.setDomain(STR."http://\{result.getDomain()}");
            return result;
        });
    }

    @Override
    public List<ShortLinkGroupCountQueryRespDTO> listGroupShortLinkCount(List<String> requestParam) {
        // 注意此处的参数是 gids
        // 这里是提供给分页查询短链接分组的，那边已经做了限流了
        List<Map<String,Object>> resultList = shortLinkMapper.listGroupShortLinkCount(requestParam);
        return resultList.stream().map(each -> {
                ShortLinkGroupCountQueryRespDTO result = new ShortLinkGroupCountQueryRespDTO();
                result.setGid(each.get("gid").toString());
                result.setShortLinkCount(Integer.parseInt(each.get("count").toString()));
                return result;
                })
                .toList();
    }

    @SneakyThrows
    @Override
    public void restoreUrl(String shortUri, ServletRequest request, ServletResponse response) {
        // 获取域名
        String domain = request.getServerName();
        // 获取端口，过滤掉80端口
        String port = Optional.of(request.getServerPort())
                .filter(each -> !Objects.equals(each, 80))
                .map(String::valueOf)
                .map(each -> STR.":\{each}")
                .orElse("");
        // 拼接完整短链接
        String fullShortUrl = STR."\{domain}\{port}/\{shortUri}";

        // lua脚本获取缓存中的原网址以及是否存在空缓存
        String luaScript = """
                -- KEYS[1] 是短链接的缓存键
                -- KEYS[2] 是空缓存标识键
                
                local originUrl = redis.call('GET', KEYS[1])
                if originUrl then
                    return originUrl  -- 缓存存在
                end
                
                local isNull = redis.call('GET', KEYS[2])
                if isNull then
                    return '1'  -- 空缓存存在
                end
                
                return nil  -- 都不存在，需要后续处理
                
                """;
        String cacheKey = String.format(RedisConstant.GOTO_SHORT_LINK_KEY, fullShortUrl);
        String nullKey = String.format(RedisConstant.GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl);

        // 执行lua脚本
        List<String> keys = Arrays.asList(cacheKey, nullKey);
        Object result = stringRedisTemplate.execute(
                new DefaultRedisScript<>(luaScript, List.class),
                keys
        );

        // 如果返回结果不为空，说明至少存在一个
        if(Boolean.FALSE.equals(ObjectUtil.isNull(result))){

            // 如果缓存存在
            if(Boolean.FALSE.equals(String.valueOf(result).equals("1"))){
                // 统计
                shortLinkStats(buildLinkStatsRecordAndSetUser(fullShortUrl, request, response));
                ((HttpServletResponse) response).sendRedirect(String.valueOf(result));
            }
            // 那么就是恶意攻击
            else{
                ((HttpServletResponse) response).sendRedirect("/page/notfound");
            }

            return;
        }

        // 查布隆过滤器
        if(shortLinkFullUrlCreatePenetrationBloomFilter.contains(shortUri)){
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return;
        }

        /*
            执行到这里的请求一共有两种：
            1. 被布隆过滤器误判为存在的新一轮的恶意攻击
            2. 正常的缓存重建请求
         */

        // 上锁
        RLock lock = redissonClient.getLock(String.format(RedisConstant.GOTO_SHORT_LINK_LOCK, fullShortUrl));
        // 尝试获取锁
        if(Boolean.FALSE.equals(lock.tryLock())){
            // 如果获取不到锁的话就代表正在重建缓存，返回404让用户重新访问
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return;
        }

        /*
            获取到锁
         */
        try{
            // 二次确认是否为空缓存以及是否缓存重建成功
            Object result1 = stringRedisTemplate.execute(
                    new DefaultRedisScript<>(luaScript, List.class),
                    keys
            );
            // 如果返回结果不为空，说明至少存在一个
            if(Boolean.FALSE.equals(ObjectUtil.isNull(result1))) {
                if(Boolean.FALSE.equals(String.valueOf(result).equals("1"))){
                    // 统计
                    shortLinkStats(buildLinkStatsRecordAndSetUser(fullShortUrl, request, response));
                    ((HttpServletResponse) response).sendRedirect(String.valueOf(result));
                }
                // 那么就是恶意攻击
                else{
                    ((HttpServletResponse) response).sendRedirect("/page/notfound");
                }

                return;
            }

            // 开始DB层操作
            // 访问映射表获取到gid
            LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                    .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
            ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(queryWrapper);

            // 如果不存在说明是恶意攻击，需要空缓存
            if(Boolean.TRUE.equals(Objects.isNull(shortLinkGotoDO))){
                stringRedisTemplate.opsForValue().set(
                        String.format(RedisConstant.GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl),
                        "",
                        60,
                        TimeUnit.SECONDS);
                ((HttpServletResponse) response).sendRedirect("/page/notfound");
                return;
            }

            // 查询短链接实体获取原始链接
            LambdaQueryWrapper<ShortLinkDO> queryWrapper1 = Wrappers.lambdaQuery(ShortLinkDO.class)
                    .eq(ShortLinkDO::getGid, shortLinkGotoDO.getGid())
                    .eq(ShortLinkDO::getFullShortUrl, fullShortUrl)
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 0);
            ShortLinkDO shortLinkDO = baseMapper.selectOne(queryWrapper1);

            // 如果不存在或者短链接有效期已经过了，说明也是恶意攻击
            if(Boolean.TRUE.equals(Objects.isNull(shortLinkDO)) ||
                    Boolean.TRUE.equals(Objects.nonNull(shortLinkDO.getValidDate()) && shortLinkDO.getValidDate().before(new Date()))){
                stringRedisTemplate.opsForValue().set(
                        String.format(RedisConstant.GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl),
                        "",
                        60,
                        TimeUnit.SECONDS);
                ((HttpServletResponse) response).sendRedirect("/page/notfound");
                return;
            }

            // 重建缓存
            stringRedisTemplate.opsForValue().set(
                    String.format(RedisConstant.GOTO_SHORT_LINK_KEY, fullShortUrl),
                    shortLinkDO.getOriginUrl(),
                    shortLinkDO.getValidDate().getTime() - new Date().getTime(),
                    TimeUnit.MILLISECONDS
            );

            // 统计
            shortLinkStats(buildLinkStatsRecordAndSetUser(fullShortUrl, request, response));
            // 跳转
            ((HttpServletResponse) response).sendRedirect(shortLinkDO.getOriginUrl());
        }catch (Exception e){
            throw new SystemException(SystemErrorCodes.SYSTEM_ERROR);
        }finally {
            lock.unlock();
        }
    }

    // 封装统计DO
    private ShortLinkStatsRecordDTO buildLinkStatsRecordAndSetUser(String fullShortUrl, ServletRequest request, ServletResponse response) {
        /*
            这里需要提一嘴，原方案使用了原子类，因为用到了lambda表达式，但是这完全是不必要的开销，
            因为这个栈帧中我们并没有使用到共享变量，而且所有操作皆是同步，使用原子类就是画蛇添足，而且原方案判断的uv层操作也是有些多余，
            有很多不必要的操作，简化之后我们直接抛弃原子类，使用基元性能会更高，而且代码也更简单易读
         */

        // 类型转换一次避免重复操作
        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final HttpServletResponse httpResponse = (HttpServletResponse) response;

        /*
            UV层检测
         */

        // 是否需要统计UV
        boolean isNeedUv = false;

        // 获取 Cookie
        Cookie cookie = getCookie(httpRequest);

        // UV value
        String uv = null;

        // 如果存在
        if(Boolean.TRUE.equals(ObjectUtil.isNotNull(cookie))) {
            // 取出
            uv = cookie.getValue();
            // 存入缓存
            Long uvAdded = stringRedisTemplate.opsForSet().add(String.format(RedisConstant.SHORT_LINK_STATS_UV_KEY, fullShortUrl), uv);
            // 如果插入成功则代表是新用户访问，需要统计UV
            if(Boolean.TRUE.equals(ObjectUtil.isNotNull(uvAdded) && uvAdded > 0L)){
                isNeedUv = true;
            }
        }
        // 不存在则创建
        else{
            // 为了节省空间，我们这里采用雪花算法生成7位唯一标识
            uv = uvSymbolGenerator.nextId();
            // 设置cookie
            Cookie newCookie = new Cookie("uv", uv);
            // 设置有效期为一年
            newCookie.setMaxAge(60 * 60 * 24 * 365);
            // 设置路径
            newCookie.setPath(StrUtil.sub(fullShortUrl, fullShortUrl.indexOf("/"), fullShortUrl.length()));
            // 绑定到响应头
            httpResponse.addCookie(newCookie);
            // 存入缓存
            stringRedisTemplate.opsForSet().add(String.format(RedisConstant.SHORT_LINK_STATS_UV_KEY, fullShortUrl), uv);
            // 首次访问肯定要统计UV
            isNeedUv = true;
        }

        /*
            UIP层检测
         */

        boolean isNeedUip = false;
        String remoteAddr = LinkUtil.getActualIp(httpRequest);
        Long uipAdded = stringRedisTemplate.opsForSet().add(String.format(RedisConstant.SHORT_LINK_STATS_UIP_KEY, fullShortUrl), remoteAddr);
        if(Boolean.TRUE.equals(ObjectUtil.isNotNull(uipAdded) && uipAdded > 0L)){
            isNeedUip = true;
        }

        /*
            其他无需判定的属性的取出
         */

        // 获取操作系统类型
        String os = LinkUtil.getOs(httpRequest);
        // 获取浏览器类型
        String browser = LinkUtil.getBrowser(httpRequest);
        // 获取设备信息
        String device = LinkUtil.getDevice(httpRequest);
        // 获取网络信息
        String network = LinkUtil.getNetwork(httpRequest);

        // 封装DO
        ShortLinkStatsRecordDTO result = ShortLinkStatsRecordDTO.builder()
                .fullShortUrl(fullShortUrl)
                .uv(uv)
                .uvFirstFlag(isNeedUv)
                .uipFirstFlag(isNeedUip)
                .remoteAddr(remoteAddr)
                .os(os)
                .browser(browser)
                .device(device)
                .network(network)
                .currentDate(new Date())
                .build();

        return result;
    }

    private Cookie getCookie(HttpServletRequest request) {
        // 获取cookie
        Cookie[] cookies = request.getCookies();

        // uv Cookie
        Cookie cookie = null;
        // 判断是否有cookie
        if (Boolean.FALSE.equals(ArrayUtil.isEmpty(cookies))) {
            for (Cookie ck : cookies) {
                // 存在则取出
                if (Boolean.TRUE.equals(ck.getName().equals("uv"))) {
                    cookie = ck;
                }
            }
        }
        return cookie;
    }

    @Override
    public void shortLinkStats(ShortLinkStatsRecordDTO shortLinkStatsRecord) {
        // DB层进行异步处理
        //shortLinkStatsSaveProducer.send(shortLinkStatsRecord);
    }
}
