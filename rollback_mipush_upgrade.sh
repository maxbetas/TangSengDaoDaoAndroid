#!/bin/bash
# 小米推送SDK升级回滚脚本
# 使用说明: 如果升级后发现问题，运行此脚本回滚到原版本

echo "开始回滚小米推送SDK升级..."

# 删除新版本AAR文件
rm -f wkpush/libs/MiPush_SDK_Client_6_0_1-C_3rd.aar

# 恢复备份文件
cp backup_mipush_upgrade_final/build.gradle wkpush/
cp backup_mipush_upgrade_final/AndroidManifest.xml wkpush/src/main/
cp backup_mipush_upgrade_final/libs/MiPush_SDK_Client_3_7_5.jar wkpush/libs/

echo "回滚完成！已恢复到JAR版本 3.7.5"
echo "备份文件保存在: backup_mipush_upgrade_final/"
