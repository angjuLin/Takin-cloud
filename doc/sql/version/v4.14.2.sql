-- 新增pod策略配置
INSERT INTO `trodb_cloud`.`t_strategy_config`(`strategy_name`, `strategy_config`, `status`, `is_deleted`, `create_time`, `update_time`)
VALUES ('私有化版本策略', '{\n \"threadNum\":\"4000\",\n \"cpuNum\":\"2\",\n \"memorySize\":\"3072\",\n \"tpsNum\":\"8000\",\n\"deploymentMethod\":0\n}', 0, 0, now(), now());