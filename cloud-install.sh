#!/usr/bin

# 部署脚本, 记得 mvn 的 setting.xml 使用公司内部的
# 清除, 部署, 跳过测试
mvn -q clean install -D"maven.test.skip"=true
cp ./takin-cloud-plugins/plugin-engine-module/target/plugin-engine-module-1.0.2.jar ./plugins
cp ./takin-cloud-plugins/plugin-engine_call-module/target/plugin-engine_call-module-1.0.2.jar ./plugins