-- 获取key
local userLimitKey = KEYS[1]
local usernameLimitKey = KEYS[2]

-- 若userLimitKey不存在
if redis.call('EXISTS', userLimitKey) == 0 then
    -- 自增
    redis.call('INCR', userLimitKey)
    -- 设置过期时间
    redis.call('EXPIRE', userLimitKey, 60)
else
    -- 自增
    redis.call('INCR', userLimitKey)
end

-- 若usernameLimitKey不存在
if redis.call('EXISTS', usernameLimitKey) == 0 then
    -- 自增
    redis.call('INCR', usernameLimitKey)
    -- 设置过期时间
    redis.call('EXPIRE', usernameLimitKey, 30)
else
    -- 自增
    redis.call('INCR', usernameLimitKey)
end