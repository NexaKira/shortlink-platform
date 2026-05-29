  -- KEYS[1]: 限流的key，比如 rate_limit:shorten:global
  -- ARGV[1]: 窗口大小（秒）
  -- ARGV[2]: 窗口内最大请求数
  -- ARGV[3]: 当前时间戳（毫秒）
  -- ARGV[4]: 本次请求的唯一ID

redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, tonumber(ARGV[3]) - tonumber(ARGV[1]) * 1000)

local count = redis.call('ZCARD', KEYS[1])

if count < tonumber(ARGV[2]) then
    redis.call('ZADD', KEYS[1], ARGV[3], ARGV[4])
    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]) + 1)
    return 1
else
    return 0
end