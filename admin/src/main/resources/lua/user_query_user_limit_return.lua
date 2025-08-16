-- 获取两个键的值脚本
local userLimitKey = KEYS[1]
local usernameLimitKey = KEYS[2]

-- 获取两个键的值
local userCount = redis.call('GET', userLimitKey)
local usernameCount = redis.call('GET', usernameLimitKey)

-- 将 nil 转换为 '0'（空值）
if userCount == false then
    userCount = '0'
end

if usernameCount == false then
    usernameCount = '0'
end

-- 返回两个值（字符串形式）
return {userCount, usernameCount}