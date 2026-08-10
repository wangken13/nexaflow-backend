# 短信环境变量

认证服务默认使用 Spug 推送助手发送短信。在 Spug 选择平台官方短信模板后，从模板详情复制完整调用 URL。调用 URL 包含凭证，不要提交到 Git。

```powershell
[Environment]::SetEnvironmentVariable('SMS_PROVIDER', 'spug', 'User')
[Environment]::SetEnvironmentVariable('SPUG_SMS_NAME', 'NexaFlow', 'User')
[Environment]::SetEnvironmentVariable('SPUG_SMS_LOGIN_URL', 'https://push.spug.cc/sms/替换为登录调用凭证', 'User')
[Environment]::SetEnvironmentVariable('SPUG_SMS_REGISTER_URL', 'https://push.spug.cc/sms/替换为注册调用凭证', 'User')
```

同一个官方模板可同时用于登录和注册。`/sms/` 接口传递 `to`（手机号）、`code`（六位验证码）和 `number`（有效分钟数，当前为 5）。设置后请完全重启 IDEA 和 `auth-service`。

如需切回阿里云短信，将 `SMS_PROVIDER` 设置为 `aliyun`，并配置以下变量：

```powershell
[Environment]::SetEnvironmentVariable('ALIBABA_CLOUD_ACCESS_KEY_ID', '替换为 RAM 用户的 AccessKey ID', 'User')
[Environment]::SetEnvironmentVariable('ALIBABA_CLOUD_ACCESS_KEY_SECRET', '替换为 RAM 用户的 AccessKey Secret', 'User')
[Environment]::SetEnvironmentVariable('ALIYUN_SMS_SIGN_NAME', '替换为已审核通过的短信签名', 'User')
[Environment]::SetEnvironmentVariable('ALIYUN_SMS_LOGIN_TEMPLATE_CODE', 'SMS_XXXXXXXXX', 'User')
[Environment]::SetEnvironmentVariable('ALIYUN_SMS_REGISTER_TEMPLATE_CODE', 'SMS_XXXXXXXXX', 'User')
```

阿里云短信模板必须包含变量 `${code}`，并使用 `code` 作为参数名。生产环境请使用仅有短信发送权限的 RAM 用户或 RAM Role，不使用阿里云主账号。
