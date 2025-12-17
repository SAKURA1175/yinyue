#!/bin/bash

echo "=========================================="
echo "网易云链接导入完整流程测试"
echo "=========================================="
echo ""

# 测试网址
NETEASE_URL="https://music.163.com/song?id=1415945617"
API_BASE="http://localhost:8080/api"

echo "[步骤 1] 测试后端网易云解析 API"
echo "请求: POST $API_BASE/ai/netease/parse"
echo "数据: {\"url\": \"$NETEASE_URL\"}"
echo ""

RESPONSE=$(curl -s -X POST "$API_BASE/ai/netease/parse" \
  -H "Content-Type: application/json" \
  -d "{\"url\": \"$NETEASE_URL\"}")

echo "响应:"
echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"
echo ""

echo "[步骤 2] 前端将接收到的数据展示在上传区域"
echo "数据流向: Hero.jsx -> App.jsx (handleNeteaseImportSuccess) -> Upload.jsx (neteaseData prop)"
echo ""

echo "[步骤 3] Upload 组件展示以下信息:"
echo "- 网易云链接解析结果卡片 (.netease-result-card)"
echo "  - 音乐标题"
echo "  - 艺术家"
echo "  - 专辑名称"
echo "  - 音乐类型"
echo "  - 专辑封面图片（如果有）"
echo "- 已选中音乐提示 (.netease-selected-hint)"
echo ""

echo "[步骤 4] 用户可以:"
echo "✓ 在上传区域上传音频文件"
echo "✓ 点击关闭按钮清除网易云数据"
echo "✓ 开始处理生成专辑封面"
echo ""

echo "=========================================="
echo "测试完成！"
echo "=========================================="
