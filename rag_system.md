
本项目是一个ai对话的支持个人上传内容的检索增强项目
前端使用    Vue3+Element-plus
后端    java21+redis+mysql
ai    langchain4j框架
		对话模型使用   deepseek
		数据向量化使用的 dashscope(千问)

需要在后端配置文件 application.yaml中
	spring:  
		datasource:填写自己的数据库内容
		data:
			redis:填写自己的redis内容
	spring:
		ai:
			openai:
				api-key填写自己的deepseek Apikey
	aliyun:  
		  dashscope:
			  api-key填写自己的千问 Apikey
			  记得修改  oss的数据 url 和 区域
			  region: cn-beijing  
			  base-url: "https://dashscope.aliyuncs.com/compatible-mode/v1"
		  oss：
			  accessKeyId:  xxxx
			  accessKeySecret:  xxxx
			  记得修改		  


若是启动时提示了**“获取授权失败”**，则需要去**阿里云

	1: 需要把自己的aliyun OSS  Bucket设置为公共读写，才可上传文件

	2: RAM 角色（Role）的权限配置**做相关处理
		后端代码：String roleArn = "acs:ram::1060859887876794:role/ossuploadrole";替换成https://ram.console.aliyun.com/roles -> 角色 -> 基本信息 ARN
	进入 RAM 控制台 -> 用户 -> 找到你的用户 -> **添加权限**
	-> 添加AliyunSTSAssumeRoleAccess。
		检查：
		进入 RAM 控制台 -> 角色 -> 点击 `ossuploadrole` -> **信任策略**。
		{ "Statement": [ 
			{ "Action": "sts:AssumeRole", 
				"Effect": "Allow", 
				"Principal": { "RAM": [ "acs:ram::你的账号ID:root" ] } 
			} 
			], "Version": "1" 
		}
	
	3: 给你的 **RAM 用户**（不是角色）直接添加 **`AdministratorAccess`**（管理员权限）。

	4: 点击上传文件的保存 提示保存失败，f12提示`The termination of the request is abnormal`，是因为aliyun OSS的跨域安全没有打开，
		### OSS 跨域被拦截

		前端是在浏览器里运行的，浏览器出于安全考虑，默认不允许 `localhost` 访问`aliyuncs.com`。你需要在阿里云 OSS 后台给你的 `localhost` 发个“通行证”。
		
		**设置步骤：**
		
		4.1. 登录 **OSS 控制台**，进入你的 Bucket：`rag-system-ccc`。
		    
		4.2. 左侧菜单选择 **【数据服务】 -> 【跨域设置】**。
		    
		4.3. 点击 **【创建规则】**，按下图配置：
		    
		    - **来源 (Allowed Origin)**：直接填 `*` (测试环境最省事) 或者  `http://localhost:5173`（根据你前端端口定）。
		        
		    - **允许 Methods**：全部勾选（GET, PUT, POST, DELETE, HEAD）。
		        
		    - **允许 Headers**：填 `*`。
		        
		    - **暴露 Headers**：填 `ETag`、
							    - `x-oss-request-id`。
		        
		4.4. 点击 **确定** 保存。



<img width="800" height="508" alt="image" src="https://github.com/user-attachments/assets/8eb4e424-5d35-4325-a5ae-14a7206cf1c0" />
出现这个错误请关闭 Node.js 进程，
	重新执行		npm install @vitejs/plugin-vue --save-dev
				npm run dev
