#!/usr/bin/env python3
import pymysql
from pymysql import Error

try:
    # 连接到 MySQL 服务器
    connection = pymysql.connect(
        host='localhost',
        user='root',
        password='1234'
    )
    
    cursor = connection.cursor()
    
    # 创建数据库
    create_db_query = "CREATE DATABASE IF NOT EXISTS yinyue_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
    cursor.execute(create_db_query)
    print("✅ 数据库 yinyue_db 创建成功！")
    
    cursor.close()
    connection.close()
        
except Error as e:
    print(f"❌ 错误: {e}")
