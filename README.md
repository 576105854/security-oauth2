# Spring Security Oauth2

# 授权码模式
## 获取授权码
http://localhost:5320/oauth/authorize?client_id=oauth&response_type=code&scope=all&redirect_uri=http://www.baidu.com

![img_2.png](img_2.png)

## 根据得到的code获取token
![img.png](img.png)


# 简化模式
http://localhost:5320/oauth/authorize?client_id=oauth&response_type=token&scope=all&redirect_uri=http://www.baidu.com

授权之后结果

![img_1.png](img_1.png)


# 密码模式
http://localhost:5320/oauth/token?client_id=oauth&grant_type=password&username=admin&password=111111&client_secret=oauth

![img_3.png](img_3.png)

# 客户端模式

http://localhost:5320/oauth/token?client_id=oauth&client_secret=oauth&grant_type=client_credentials

![img_4.png](img_4.png)