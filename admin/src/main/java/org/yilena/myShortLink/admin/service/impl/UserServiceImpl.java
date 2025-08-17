package org.yilena.myShortLink.admin.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.yilena.myShortLink.admin.common.biz.user.UserContext;
import org.yilena.myShortLink.admin.common.constant.RedisConstant;
import org.yilena.myShortLink.admin.common.convention.errorCode.codes.SystemErrorCodes;
import org.yilena.myShortLink.admin.common.convention.errorCode.codes.UserErrorCodes;
import org.yilena.myShortLink.admin.common.convention.errorCode.codes.WarningErrorCodes;
import org.yilena.myShortLink.admin.common.convention.exception.SystemException;
import org.yilena.myShortLink.admin.common.convention.exception.UserException;
import org.yilena.myShortLink.admin.dao.UserMapper;
import org.yilena.myShortLink.admin.entry.DO.UserDO;
import org.yilena.myShortLink.admin.entry.DTO.request.UserLoginReqDTO;
import org.yilena.myShortLink.admin.entry.DTO.request.UserRegisterReqDTO;
import org.yilena.myShortLink.admin.entry.DTO.request.UserUpdateReqDTO;
import org.yilena.myShortLink.admin.entry.DTO.result.UserLoginRespDTO;
import org.yilena.myShortLink.admin.entry.DTO.result.UserRespDTO;
import org.yilena.myShortLink.admin.service.GroupService;
import org.yilena.myShortLink.admin.service.UserService;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 用户接口实现层
 * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：link）获取项目资料
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {

    private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;
    private final RedissonClient redissonClient;
    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final GroupService groupService;

    // 一个用户一分钟内使用搜索模块最多为10次
    private static final int ONE_USER_LIMIT = 10;
    // 一个用户名10s内使用搜索模块最多为5次
    private static final int USERNAME_LIMIT = 5;
    // 相同用户1分钟内最多登录3次
    private static final int USER_LOGIN_LIMIT = 3;

    private static final DefaultRedisScript<List> USER_QUERY_ONE_USER_LIMIT_RETURN_SCRIPT;
    private static final DefaultRedisScript<Void> USER_QUERY_ONE_USER_LIMIT_ADD_SCRIPT;
    static {
        USER_QUERY_ONE_USER_LIMIT_RETURN_SCRIPT = new DefaultRedisScript<>();
        USER_QUERY_ONE_USER_LIMIT_RETURN_SCRIPT.setLocation(new ClassPathResource("lua/user_query_user_limit_return.lua"));
        USER_QUERY_ONE_USER_LIMIT_RETURN_SCRIPT.setResultType(List.class);

        USER_QUERY_ONE_USER_LIMIT_ADD_SCRIPT = new DefaultRedisScript<>();
        USER_QUERY_ONE_USER_LIMIT_ADD_SCRIPT.setLocation(new ClassPathResource("lua/user_query_user_limit_add.lua"));
        USER_QUERY_ONE_USER_LIMIT_ADD_SCRIPT.setResultType(Void.class);
    }

    @Override
    public UserRespDTO getUserByUsername(String username) {

        // 检测用户使用搜索模块的次数
        String oneUserLimitKey = String.format(RedisConstant.USER_QUERY_ONE_USER_LIMIT, UserContext.getUserId());
        // 检测当前用户名的搜索次数
        String usernameLimitKey = String.format(RedisConstant.USER_QUERY_USERNAMES_LIMIT, username);

        // 使用lua脚本一次性传回两个数据
        List<String> keys = Arrays.asList(oneUserLimitKey, usernameLimitKey);
        List<String> values = stringRedisTemplate.execute(
                USER_QUERY_ONE_USER_LIMIT_RETURN_SCRIPT,
                keys
        );

        int oneUserLimitCount = Integer.parseInt(values.get(0));
        int usernameLimitCount = Integer.parseInt(values.get(1));

        // 检测
        if(oneUserLimitCount >= ONE_USER_LIMIT){
            throw new UserException(UserErrorCodes.USER_QUERY_ONE_USER_LIMIT);
        }

        // 检查用户是否存在
        if(Boolean.FALSE.equals(isUsernameExist(username))){
            throw new UserException(UserErrorCodes.USER_NOT_EXIST);
        }

        /*
            其实这个项目感觉还是存在问题，这个impl注释说是用户管理，那如果是给内部人员专用的话，正常来说这里可以直接查DB，
            但是问题就出在这个controller存在用户注册的功能，甚至后续还会讲高并发调用注册接口，
            所以我认为这个项目也像牛券一样，是不严谨的，所以我们统一当作面向用户的接口就行了，
            但如果面向用户的话通过用户名查找用户通常是搜索模块的接口，一般要使用es，
            但是本项目并没有接入es，而且嫌麻烦，只要心里知道这里要用es就行，
            所以此处直接查DB即可
         */

        /*
            这里做个延迟锁，最大化提升并发性能
         */

        UserDO userDO;
        if(usernameLimitCount >= USERNAME_LIMIT){
            RLock lock = redissonClient.getLock(String.format(RedisConstant.USER_QUERY_LOCK, username));
            try {
                // 等待时间为1s
                if(lock.tryLock(1, TimeUnit.SECONDS)){
                    throw new SystemException(UserErrorCodes.USER_QUERY_ONE_USER_LIMIT);
                }
            } catch (InterruptedException e) {
                throw new SystemException(UserErrorCodes.USER_QUERY_ONE_USER_LIMIT);
            }

            /*
                竞争成功
             */

            try{
                LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                        .eq(UserDO::getUsername, username);
                userDO = userMapper.selectOne(queryWrapper);
                if (ObjectUtil.isNull(userDO)) {
                    throw new UserException(UserErrorCodes.USER_NOT_EXIST);
                }
            }catch (Exception ex){
                throw new SystemException(UserErrorCodes.USER_QUERY_ONE_USER_LIMIT);
            }finally {
                lock.unlock();
            }
        }else {
            LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                    .eq(UserDO::getUsername, username);
            userDO = userMapper.selectOne(queryWrapper);
            if (ObjectUtil.isNull(userDO)) {
                throw new UserException(UserErrorCodes.USER_NOT_EXIST);
            }
        }

        // 使用lua脚本，先查询key是否存在，若存在则直接加过期时间，不存在则直接自增即可
        stringRedisTemplate.execute(
                USER_QUERY_ONE_USER_LIMIT_ADD_SCRIPT,
                keys
        );

        return BeanUtil.toBean(userDO, UserRespDTO.class);
    }

    @Override
    public Boolean isUsernameExist(String username) {
        // 检测布隆过滤器中是否存在
        if(Boolean.FALSE.equals(userRegisterCachePenetrationBloomFilter.contains(username))){
            // 判定不存在则一定不存在
            return false;
        }

        // 判定存在则继续查DB，需要使用分布式锁
        RLock lock = redissonClient.getLock(String.format(RedisConstant.USERNAME_ALREADY_EXIST_LOCK, username));


        try {
            // 如果竞争锁失败则进行1s等待，启用看门狗模式
            if(Boolean.FALSE.equals(lock.tryLock(1000, TimeUnit.MILLISECONDS))){
                // 没抢到锁则提醒用户稍后再试
                throw new UserException(UserErrorCodes.USER_ERROR);
            }
        } catch (InterruptedException e) {
            throw new UserException(UserErrorCodes.USER_ERROR);
        }

        /*
            竞争成功
         */

        try{
            // 查询 DB
            LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                    .eq(UserDO::getUsername, username);
            UserDO userDO = userMapper.selectOne(queryWrapper);

            // 用户名不存在
            if(ObjectUtil.isNull(userDO)){
                return false;
            }
        }finally {
            // 解锁
            lock.unlock();
        }

        // 用户名已存在
        return true;
    }

    @Override
    public void register(UserRegisterReqDTO requestParam) {

        /*
            由于注册功能是公开接口，所以此处无法使用延迟锁进行限流，只能通过sentinel进行限流
         */

        // 参数有效性校验省略

        // 检测用户名是否已存在
        if(Boolean.TRUE.equals(isUsernameExist(requestParam.getUsername()))){
            throw new UserException(UserErrorCodes.USERNAME_ALREADY_EXIST.formatMessage(requestParam.getUsername()));
        }

        // 使用分布式锁防止并发问题
        RLock lock = redissonClient.getLock(String.format(RedisConstant.USER_REGISTER_LOCK, requestParam.getUsername()));


        // 尝试获取锁，不设置等待时间
        if(Boolean.FALSE.equals(lock.tryLock())){
            // 没抢到锁则证明已经有人在注册了，但是因为其不一定注册成功，所以此处不返回 ”用户名已存在“ 的错误
            throw new UserException(UserErrorCodes.USER_ERROR);
        }

        /*
            竞争成功
         */

        try{
            // 二次检查用户名是否存在，防止在一判到竞争成功的这段时间被人抢先注册
            if(Boolean.TRUE.equals(isUsernameExist(requestParam.getUsername()))){
                throw new UserException(UserErrorCodes.USERNAME_ALREADY_EXIST);
            }

            // 建立DO对象
            UserDO userDO = UserDO.builder()
                    .username(requestParam.getUsername())
                    .password(requestParam.getPassword())
                    .realName(requestParam.getRealName())
                    .phone(requestParam.getPhone())
                    .mail(requestParam.getMail())
                    .deletionTime(new Date().getTime())
                    .build();

            // 插入
            int row = userMapper.insert(userDO);
            // 插入失败
            if(Boolean.FALSE.equals(SqlHelper.retBool(row))){
                throw new UserException(UserErrorCodes.USER_ERROR);
            }

            // 添加短链接默认分组
            groupService.saveGroup(requestParam.getUsername(), "默认分组");

            // 添加到布隆过滤器
            userRegisterCachePenetrationBloomFilter.add(requestParam.getUsername());
        }finally {
            // 解锁
            lock.unlock();
        }
    }

    @Override
    public void update(UserUpdateReqDTO requestParam) {
        // 防止越权
        if(Boolean.FALSE.equals(UserContext.getUsername().equals(requestParam.getUsername()))){
            // 这种情况只可能是恶意攻击，直接抛出告警错误码
            throw new UserException(WarningErrorCodes.USER_UPDATE_ERROR);
        }
        // 加锁
        RLock lock = redissonClient.getLock(String.format(RedisConstant.USER_UPDATE_LOCK, requestParam.getUsername()));
        if (Boolean.FALSE.equals(lock.tryLock())) {
            // 没抢到锁则提醒用户稍后再试
            throw new UserException(UserErrorCodes.USER_ERROR);
        }
        try {
            LambdaUpdateWrapper<UserDO> updateWrapper = Wrappers.lambdaUpdate(UserDO.class)
                    .eq(UserDO::getUsername, requestParam.getUsername());
            int row = userMapper.update(BeanUtil.toBean(requestParam, UserDO.class), updateWrapper);
            if (Boolean.FALSE.equals(SqlHelper.retBool(row))) {
                throw new SystemException(SystemErrorCodes.SYSTEM_ERROR);
            }
        }finally {
            lock.unlock();
        }
    }

    @Override
    public UserLoginRespDTO login(UserLoginReqDTO requestParam) {
        // 参数校验逻辑省略

        // 短时间重复请求拦截
        String userLoginLimitKey = String.format(RedisConstant.USER_LOGIN_LIMIT_KEY, requestParam.getUsername());
        String loginCount = stringRedisTemplate.opsForValue().get(userLoginLimitKey);
        if (ObjectUtil.isNotNull(loginCount) && Integer.parseInt(loginCount) >= USER_LOGIN_LIMIT) {
            throw new UserException(UserErrorCodes.USER_LOGIN_LIMIT);
        }

        // 查询该用户是否存在
        if (Boolean.FALSE.equals(isUsernameExist(requestParam.getUsername()))) {
            throw new UserException(UserErrorCodes.USER_NOT_EXIST);
        }

        // 加锁
        RLock lock = redissonClient.getLock(String.format(RedisConstant.USER_LOGIN_LOCK, requestParam.getUsername()));

        if (Boolean.FALSE.equals(lock.tryLock())) {
            // 没抢到锁则提醒用户稍后再试
            throw new UserException(UserErrorCodes.USER_ERROR);
        }
        UserDO userDO;
        try {
            LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                    .eq(UserDO::getUsername, requestParam.getUsername())
                    .eq(UserDO::getPassword, requestParam.getPassword());
             userDO = userMapper.selectOne(queryWrapper);

            if (ObjectUtil.isNull(userDO)) {
                // 密码错误
                throw new UserException(UserErrorCodes.USER_LOGIN_PASSWORD_ERROR);
            }
        } finally {
            // 解锁
            lock.unlock();
        }

        String key = String.format(RedisConstant.USER_LOGIN_KEY , requestParam.getUsername());
        Map<Object, Object> hasLoginMap = stringRedisTemplate.opsForHash().entries(key);
        if (CollUtil.isNotEmpty(hasLoginMap)) {
            //stringRedisTemplate.expire(key, 30L, TimeUnit.MINUTES);
            String token = hasLoginMap.keySet().stream()
                    .findFirst()
                    .map(Object::toString)
                    .orElseThrow(() -> new UserException(UserErrorCodes.USER_ERROR));
            return new UserLoginRespDTO(token);
        }
        // 为了测试方便就不设过期时间了，而且就算要设过期时间也应该使用lua脚本
        String uuid = UUID.randomUUID().toString();
        stringRedisTemplate.opsForHash().put(key, uuid, JSON.toJSONString(userDO));
        //stringRedisTemplate.expire(key + requestParam.getUsername(), 30L, TimeUnit.MINUTES);
        return new UserLoginRespDTO(uuid);
//        /*
//            这里需要返回token，但是我们使用双令牌方式来搞点花样
//         */
//        // 创建新token版本
//        String tokenVersion = tokenSessionService.createNewSession(requestParam.getUsername());
//
//        // 生成双令牌
//        String accessToken = tokenProvider.generateToken(requestParam.getUsername(), tokenVersion);
//        String refreshToken = tokenProvider.generateRefreshToken(requestParam.getUsername(), tokenVersion);
//
//        // 构建响应
//        AuthResponse response = new AuthResponse(
//                accessToken,
//                refreshToken,
//                "Bearer",
//                tokenProvider.getRemainingTime(accessToken) / 1000,
//                tokenProvider.getRefreshExpiration()
//        );
//
//        // 添加到缓存
//        stringRedisTemplate.opsForValue().increment(userLoginLimitKey);
    }


    @Override
    public Boolean checkLogin(String username, String token) {
        return stringRedisTemplate.opsForHash().get(String.format(RedisConstant.USER_LOGIN_KEY , username), token) != null;
    }

    @Override
    public void logout(String username, String token) {
        // 检测当前账户登陆状态
        if (Boolean.FALSE.equals(checkLogin(username, token))) {
            throw new UserException(UserErrorCodes.USER_LOGOUT_ERROR);
        }
        // 删除
        stringRedisTemplate.opsForHash().delete(String.format(RedisConstant.USER_LOGIN_KEY , username), token);


//        // 获取访问令牌
//        String token = extractToken(request);
//        if (token != null) {
//            try {
//                // 解析
//                Claims claims = tokenProvider.parseToken(token);
//                String username = claims.getSubject();
//                // 吊销刷新令牌
//                tokenSessionService.revokeRefreshToken(username);
//
//                // 将当前访问令牌加入黑名单
//                String jti = tokenProvider.getJtiFromToken(token);
//                long ttl = tokenProvider.getRemainingTime(token);
//                tokenSessionService.addToShortBlacklist(jti, ttl);
//            } catch (JwtException ignored) {
//                throw new UserException(UserErrorCodes.USER_ERROR);
//            }
//        }
    }

//    private String extractToken(HttpServletRequest request) {
//        String header = request.getHeader("Authorization");
//        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
//            return header.substring(7);
//        }
//        return null;
//    }
}
