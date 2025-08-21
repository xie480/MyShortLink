package org.yilena.myShortLink.project.mq.consumer;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.Week;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.yilena.myShortLink.project.common.constant.MQConstant;
import org.yilena.myShortLink.project.common.constant.RedisConstant;
import org.yilena.myShortLink.project.common.convention.errorCode.codes.SystemErrorCodes;
import org.yilena.myShortLink.project.common.convention.errorCode.codes.WarningErrorCodes;
import org.yilena.myShortLink.project.common.convention.exception.SystemException;
import org.yilena.myShortLink.project.common.convention.exception.UserException;
import org.yilena.myShortLink.project.dao.*;
import org.yilena.myShortLink.project.entry.DO.*;
import org.yilena.myShortLink.project.entry.DTO.request.ShortLinkStatsRecordDTO;
import org.yilena.myShortLink.project.utils.MessageQueueIdempotentHandler;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MQConstant.SHORT_LINK_STATS_STREAM_TOPIC_KEY,
        consumerGroup = MQConstant.SHORT_LINK_STATS_STREAM_CONSUMER_GROUP_KEY
)
public class ShortLinkStatsSaveConsumer implements RocketMQListener<ShortLinkStatsRecordDTO> {

    /*
        我本来想配置这个消费者的线程池为虚拟线程的，但是一直配不好，
        我认为这个消费者就得用虚拟线程，在这种高吞吐还频繁阻塞的场景，虚拟线程再合适不过了，
        但是无论如何都没有成功，所以就放弃了，
        如果你能配置成功的话请告诉我配置方法
     */
    private final ShortLinkMapper shortLinkMapper;
    private final ShortLinkGotoMapper shortLinkGotoMapper;
    private final RedissonClient redissonClient;
    private final LinkAccessStatsMapper linkAccessStatsMapper;
    private final LinkLocaleStatsMapper linkLocaleStatsMapper;
    private final LinkOsStatsMapper linkOsStatsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;
    private final LinkAccessLogsMapper linkAccessLogsMapper;
    private final LinkDeviceStatsMapper linkDeviceStatsMapper;
    private final LinkNetworkStatsMapper linkNetworkStatsMapper;
    private final LinkStatsTodayMapper linkStatsTodayMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final MessageQueueIdempotentHandler messageQueueIdempotentHandler;

    @Lazy
    @Autowired
    private ShortLinkStatsSaveConsumer shortLinkStatsSaveConsumer;

    @Override
    public void onMessage(ShortLinkStatsRecordDTO shortLinkStatsRecord) {
        log.info("消费短链接统计数据：{}", shortLinkStatsRecord.getUv());

        // 拿到uv的唯一标识
        String uv = shortLinkStatsRecord.getUv();

        /*
            幂等性校验，这里虽然比redisStream队列校验的时机要更慢一些，但是我们换取了更多的内存空间以及完备的高可靠性
            再者，重复消费的概率一般很小，只有当用户出现网络波动的情况下多次刷新界面才可能出现
         */

        // 判断当前消息是否正在被消费，若没有则设置为正在消费，反之则进入if块
        if (messageQueueIdempotentHandler.isMessageBeingConsumed(uv)) {
            // 判断当前的这个消息流程是否执行完成
            if (messageQueueIdempotentHandler.isAccomplish(uv)) {
                return;
            }
            // 没执行完成则需要把原本的幂等性校验缓存给删了
            messageQueueIdempotentHandler.delMessageProcessed(uv);
            // 没执行完成的话就抛出异常让RocketMQ的nameserver进行消息重试
            throw new SystemException(SystemErrorCodes.MQ_RETRY.formatMessage(uv));
        }

        /*
            开始消费流程
         */

        try{
            // 处理消息
            shortLinkStatsSaveConsumer.dealMessage(shortLinkStatsRecord);
        }catch (UserException e){
            /*
                这里是当跳过次数超过20次会抛出的异常，对于这类消息我们不放入延迟队列，
                因为首先MQ的匀速消费性质，这个异常几乎不会被抛出，就算真的抛出了也很难会有很多消息因此消费失败
                而且我们的等待锁时间和持有锁时间是相同的，所以这个问题更加不太可能发生了
                不过预防万一我们还是得处理一下的，因为数量不多，而且也是因为没能抢到锁才抛出的异常，
                所以我们直接抛出异常让MQ进行重试即可，注意这里的错误码是告警错误码，
                正如我上面所说，执行到这一块的概率极低，只有在极端情况，比如说redis宕机导致获取不到锁，这类问题肯定得立即告警进行人工处理
             */
            throw e;
        }catch (Exception e){
            log.error("消费短链接统计数据失败：{}", e.getMessage());
            // 删除幂等性缓存
            messageQueueIdempotentHandler.delMessageProcessed(uv);
            /*
               这里抛出异常的话确实会有不断重试导致消息积压的风险，因为我们这个接口的流量是非常非常大的，但是因为我们这种消息并不讲究实时性，所以可以考虑将抛出异常的语句放入延时队列里，
               延时时间可以选择在凌晨流量谷底的时间段，将所有异常在那个时候抛出然后进行重试，这样就可以能避免因业务异常导致的大量重试消息与新消息堆积在一起的积压问题，
               开辟一个新的主题核消费者组，将当前消息延时放入其队列中，然后时间到了后再由那个队列转发给本队列来，
               但是如此又有一个问题，如果二次重试仍旧失败的话又会重复一遍刚才的流程，导致异常空转，
               解决方法就是在消息首次抛出异常的时候将唯一标识存入redis的set结合，代表首次异常，首次异常则放入延时队列等待重试，
               在重试也抛出异常的时候又会执行一遍缓存操作，但此时会返回失败，我们做一个判断，如果失败则直接抛出异常，这样的话就触发了RocketMQ自带的重试机制，
               会每隔不同阶段的时间进行重试，最后放入死信队列，这样一来我们就完成了将需要重试的消息和正常的消息分离的目标
            */
            // 放入set缓存
            Long success = stringRedisTemplate.opsForSet().add(RedisConstant.SHORT_LINK_STATS_STREAM_MESSAGE_ERROR_SET_KEY, uv);
            /*
                这里的TTL应该使用lua脚本，但是我懒得写了，你知我知就行了

                为什么要设置TTL？与我们下面等待获取锁失败直接跳过统计一样的原因，我们这个接口不讲究
             */
            stringRedisTemplate.expire(RedisConstant.SHORT_LINK_STATS_STREAM_MESSAGE_ERROR_SET_KEY, 1, TimeUnit.DAYS);
            // 如果放入成功
            if(Boolean.TRUE.equals(ObjectUtil.isNotNull(success) && success > 0L)){
                // 放入延时队列逻辑省略……
                log.error("消费短链接统计数据失败：{}", e.getMessage());
            }
            // 放入失败则抛出异常立即重试
            else{
                log.error("消费短链接统计数据重试失败：{}", e.getMessage());
                throw new SystemException(SystemErrorCodes.MQ_RETRY.formatMessage(e.getMessage()));
            }
        }
        // 如果是重试消息消费成功的话则需要删除原本的缓存，这样的话拿set的缓存也可以作为死信队列来看待
        stringRedisTemplate.opsForSet().remove(RedisConstant.SHORT_LINK_STATS_STREAM_MESSAGE_ERROR_SET_KEY, uv);

        // 消费完成
        messageQueueIdempotentHandler.setAccomplish(uv);
    }

    @Transactional
    public void dealMessage(ShortLinkStatsRecordDTO statsRecord) {
        // 获取短链接
        String fullShortUrl = statsRecord.getFullShortUrl();
        // 使用读锁，阻塞其他写操作，防止在处理消息的过程中短链接的gid被篡改导致插入的统计数据失效
        RReadWriteLock rReadWriteLock = redissonClient.getReadWriteLock(String.format(RedisConstant.Short_LINK_READ_WRITE_LOCK, fullShortUrl));
        RLock readLock = rReadWriteLock.readLock();
        try {
            /*
                尝试获取锁，等待时间为3s，不使用看门狗，防止阻塞线程

                视频有一节将这里的无等待无超时的tryLock去掉了，原因是MQ为匀速消费，并行的线程固定，所以竞争的压力会小一些，
                原方案在获取锁失败时会将其放入redis的延时队列，等待后续拿到锁为止，这是为了担心消费者线程被阻塞导致吞吐量断崖式下降，
                但实际锁竞争力度并不大，所以可以通过lock()阻塞获取锁，因为阻塞的时间很短，没必要引入一个延迟队列

                那为什么我没有去掉呢，因为原方案忘记考虑一个问题了，假设拿到锁的线程在执行逻辑的过程中网络突然变得很差，使网络IO请求耗时过长，
                那这个时候不还是导致了消费者线程阻塞时间过久吗？
                所以合理的方案就是使用tryLock()，设置超时和等待时间，不使用看门狗模式，这样就完美解决阻塞问题了

                至于修改短链接那边占用写锁时间过长导致这里获取不到读锁的问题就交给那边的逻辑来解决
             */
            if(Boolean.FALSE.equals(readLock.tryLock(1, 1, TimeUnit.SECONDS))){
                // 还没抢到的话就直接抛弃掉，因为我们这个并不讲究精准性
                // 监控一分钟内的跳过次数，如果超过20次则进行告警处理
                Long count = stringRedisTemplate.opsForValue().increment(String.format(RedisConstant.SHORT_LINK_STATS_SKIP_COUNT_KEY, fullShortUrl));
                if(count > 20){
                    log.error("短链接跳过次数超过20次，请及时处理：{}", fullShortUrl);
                    throw new UserException(WarningErrorCodes.MQ_SKIP_COUNT_EXCEED_20.formatMessage(MQConstant.SHORT_LINK_STATS_STREAM_TOPIC_KEY));
                }
                return;
            }
        } catch (InterruptedException e) {
            // 被打断一般是宕机了，抛出异常放入延迟队列
            throw new SystemException(SystemErrorCodes.MQ_NEED_RETRY.formatMessage(e.getMessage()));
        }

        /*
            竞争成功
         */
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()){

            Date now = statsRecord.getCurrentDate();

            /*
                pv、uv、uip
             */


             /*
                使用数组来避免创建原子类，减少开销
                为什么能替代？因为数组加上了final之后在引用不可变的情况下可以修改属性值，符合lambda的变量使用规则
             */
            final String[] gid = new String[1];

            // 查询短链接对应的全局ID
            scope.fork(() -> {
                        LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                                .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
                        ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(queryWrapper);
                        gid[0] = shortLinkGotoDO.getGid();
                        // 获取当前日期和小时信息
                        int hour = DateUtil.hour(now, true);
                        Week week = DateUtil.dayOfWeekEnum(now);
                        int weekValue = week.getIso8601Value();
                        // 构建并保存链接访问统计信息
                        LinkAccessStatsDO linkAccessStatsDO = LinkAccessStatsDO.builder()
                                .pv(1)
                                .uv(statsRecord.getUvFirstFlag() ? 1 : 0)
                                .uip(statsRecord.getUipFirstFlag() ? 1 : 0)
                                .hour(hour)
                                .weekday(weekValue)
                                .fullShortUrl(fullShortUrl)
                                .date(now)
                                .build();
                        linkAccessStatsMapper.shortLinkStats(linkAccessStatsDO);
                        return null;
                    });

            /*
                地理位置
             */


            final String[] actualProvince = new String[1];
            final String[] actualCity = new String[1];

            scope.fork(() -> {
                // 获取地理位置信息
                Map<String, Object> localeParamMap = new HashMap<>();
                // 将高德地图的API密钥放入参数映射中
                localeParamMap.put("key", "");
                // 将用户的IP地址放入参数映射中，用于获取用户的大致位置信息
                localeParamMap.put("ip", statsRecord.getRemoteAddr());
                // 通过HTTP GET请求，使用高德地图的API获取位置信息
                String localeResultStr = HttpUtil.get("https://restapi.amap.com/v3/ip", localeParamMap);
                // 将获取到的JSON格式位置信息字符串转换为JSONObject对象，便于处理
                JSONObject localeResultObj = JSON.parseObject(localeResultStr);
                // 从解析的JSON对象中获取信息代码，用于判断请求是否成功
                String infoCode = localeResultObj.getString("infocode");
                // 初始化实际省份名称，默认为"未知"
                actualProvince[0] = ("未知");
                // 初始化实际城市名称，默认为"未知"
                actualCity[0] = "未知";
                // 根据地理位置信息构建并保存链接地域统计信息
                if (StrUtil.isNotBlank(infoCode) && StrUtil.equals(infoCode, "10000")) {
                    String province = localeResultObj.getString("province");
                    boolean unknownFlag = StrUtil.equals(province, "[]");
                    LinkLocaleStatsDO linkLocaleStatsDO = LinkLocaleStatsDO.builder()
                            .province(actualProvince[0] = unknownFlag ? actualProvince[0] : province)
                            .city(actualCity[0] = unknownFlag ? actualCity[0] : localeResultObj.getString("city"))
                            .adcode(unknownFlag ? "未知" : localeResultObj.getString("adcode"))
                            .cnt(1)
                            .fullShortUrl(fullShortUrl)
                            .country("中国")
                            .date(now)
                            .build();
                    linkLocaleStatsMapper.shortLinkLocaleState(linkLocaleStatsDO);
                }
                return null;
            });

            /*
                操作系统
             */

            scope.fork(() -> {
                // 构建并保存操作系统统计信息
                LinkOsStatsDO linkOsStatsDO = LinkOsStatsDO.builder()
                        .os(statsRecord.getOs())
                        .cnt(1)
                        .fullShortUrl(fullShortUrl)
                        .date(now)
                        .build();
                linkOsStatsMapper.shortLinkOsState(linkOsStatsDO);
                return null;
            });


            /*
                浏览器
             */

            scope.fork(() -> {
                // 构建并保存浏览器统计信息
                LinkBrowserStatsDO linkBrowserStatsDO = LinkBrowserStatsDO.builder()
                        .browser(statsRecord.getBrowser())
                        .cnt(1)
                        .fullShortUrl(fullShortUrl)
                        .date(now)
                        .build();
                linkBrowserStatsMapper.shortLinkBrowserState(linkBrowserStatsDO);
                return null;
            });
            // 构建并保存设备统计信息
            scope.fork(() -> {
                        LinkDeviceStatsDO linkDeviceStatsDO = LinkDeviceStatsDO.builder()
                                .device(statsRecord.getDevice())
                                .cnt(1)
                                .fullShortUrl(fullShortUrl)
                                .date(now)
                                .build();
                        linkDeviceStatsMapper.shortLinkDeviceState(linkDeviceStatsDO);
                        return null;
                    });
            // 构建并保存网络统计信息
            scope.fork(() -> {
                LinkNetworkStatsDO linkNetworkStatsDO = LinkNetworkStatsDO.builder()
                        .network(statsRecord.getNetwork())
                        .cnt(1)
                        .fullShortUrl(fullShortUrl)
                        .date(now)
                        .build();
                linkNetworkStatsMapper.shortLinkNetworkState(linkNetworkStatsDO);
                return null;
            });
            // 构建并保存链接访问日志信息
            scope.fork(() -> {
                LinkAccessLogsDO linkAccessLogsDO = LinkAccessLogsDO.builder()
                        .user(statsRecord.getUv())
                        .ip(statsRecord.getRemoteAddr())
                        .browser(statsRecord.getBrowser())
                        .os(statsRecord.getOs())
                        .network(statsRecord.getNetwork())
                        .device(statsRecord.getDevice())
                        .locale(StrUtil.join("-", "中国", actualProvince[0], actualCity[0]))
                        .fullShortUrl(fullShortUrl)
                        .build();
                linkAccessLogsMapper.insert(linkAccessLogsDO);
                return null;
            });
            // 更新短链接的总体统计信息
            scope.fork(() -> {
                shortLinkMapper.incrementStats(gid[0], fullShortUrl, 1, statsRecord.getUvFirstFlag() ? 1 : 0, statsRecord.getUipFirstFlag() ? 1 : 0);
                return null;
            });
            // 构建并保存今日统计信息
            scope.fork(() -> {
                LinkStatsTodayDO linkStatsTodayDO = LinkStatsTodayDO.builder()
                        .todayPv(1)
                        .todayUv(statsRecord.getUvFirstFlag() ? 1 : 0)
                        .todayUip(statsRecord.getUipFirstFlag() ? 1 : 0)
                        .fullShortUrl(fullShortUrl)
                        .date(now)
                        .build();
                linkStatsTodayMapper.shortLinkTodayState(linkStatsTodayDO);
                return null;
            });

            /*
                等待执行结果
                其实我们这里也可以选择不等待直接释放平台线程，因为我们这个访问量的精准度要求并不高，缺失一些数据其实没什么问题，
                相对的，释放平台线程能够换取更加高的吞吐量，收益更大
             */
            scope.join();
            scope.throwIfFailed();
            log.info("处理短链接跳转统计数据成功：{}", statsRecord);
        } catch (Exception e) {
            log.error("处理短链接跳转统计数据失败：{}", e.getMessage());
            throw new SystemException(SystemErrorCodes.MQ_NEED_RETRY.formatMessage(e.getMessage()));
        }finally {
            readLock.unlock();
        }
    }
}