<div align="center">
    <br />
    <img alt="MiniLPA" src="https://github.com/EsimMoe/MiniLPA/raw/main/assets/logo.svg">
</div>

<h3 align="center">精美的现代化 LPA UI</h4>

<p align="center">
    <a href="README.md">English</a> |
    <span>简体中文</span>
</p>

<p align="center">
    <a href="#特性">特性</a> •
    <a href="#安装">安装</a> •
    <a href="#技巧">技巧</a> •
    <a href="#构建">构建</a> •
    <a href="#待办事项清单">待办事项清单</a> •
    <a href="#反馈">反馈</a> •
    <a href="#常见问题">常见问题</a> •
    <a href="#特别鸣谢">特别鸣谢</a> •
    <a href="#相关读物">相关读物</a>
</p>

<div align="center">
    <img alt="主题展示" src="https://github.com/EsimMoe/MiniLPA/raw/main/assets/主题展示.apng">
</div>

## 推广
MiniLPA 开发组在这里推荐 Estkme eUICC 卡片, SiP 先进封装工艺更不易因外力损坏  
无法使用常规 LPA 管理卡片时也可使用 STK 菜单进行卡片管理, 方便好用  
现在使用 `MiniLPA` 作为优惠码可享九折优惠哦, 欢迎下单购买~  
点此进入商品页 -> [ESTKme-ECO](https://www.estk.me/product/estkme-eco/?aid=MiniLPA)


## 特性
- 良好的跨平台支持 (Windows, Linux, macOS)
- 更友好的用户界面
- i18n 多语言支持
- 搜索与快捷跳转
- 自由地管理 esim 通知
- 支持拖放与粘贴 esim 二维码或激活码
- 多种表情设计选择
- 拥有多种主题并支持自动切换日夜主题

## 安装
> [!NOTE]
> 本项目需要 PCSC 智能卡服务  
> Windows 与 macOS 应已经默认自带并启用  
> 如果你是 Linux 用户请自行检查安装 [PCSC-Lite](https://pcsclite.apdu.fr/)
#### 简易安装

本项目使用 `Kotlin` 编写 因此需要安装 `Java` 运行时才可以使用  
但为了方便使用构建将会提供已经打包 jvm 的一键安装版本 安装后开箱即用  
请根据您的系统与架构自行下载即可 [MiniLPA Releases](https://github.com/EsimMoe/MiniLPA/releases/latest)

#### 使用包管理器
> ##### macOS Homebrew
> ```
> brew install EsimMoe/homebrew-cask/minilpa
> ```
欢迎贡献更多的包管理器支持选项

#### 自行安装 Java
> [!IMPORTANT]  
> MiniLPA 需要至少 **Java 21** 才能运行

本项目御用推荐 `Java` 发行版为 [Azul Zulu](https://www.azul.com/downloads/#zulu)  
而在 Linux 更推荐使用 [JetBrainsRuntime](https://github.com/JetBrains/JetBrainsRuntime) (因为其可以正确适配不同的显示方案的缩放)  
其他发行版尚可 但并不保证其他发行版的运行效果

> [!TIP]  
> 如果你正在 Linux 使用 Wayland 与 JetBrainsRuntime 你可以通过加入以下启动参数以启用原生 Wayland 应用支持
> ```
> -Dawt.toolkit.name=WLToolkit
> ```
> 不过 Wayland 原生应用支持仍处于早期 可能仍存在一些问题  
> 请不要向 MiniLPA 反馈由此导致的相关问题

## 技巧
> ![拖拽-激活码二维码-配置区域](https://github.com/EsimMoe/MiniLPA/raw/main/assets/拖拽-激活码二维码-配置区域.apng)
> 通过拖拽激活码二维码图片至配置区域以快速解析信息

> ![拖拽-激活码二维码-下载窗口](https://github.com/EsimMoe/MiniLPA/raw/main/assets/拖拽-激活码二维码-下载窗口.apng)
> 拖拽到下载窗口也是可行的, 同理其他拖拽也可以如此操作

> ![拖拽-激活码-配置区域](https://github.com/EsimMoe/MiniLPA/raw/main/assets/拖拽-激活码-配置区域.apng)
> 也可拖拽激活码文本信息

> ![粘贴-激活码二维码-配置区域](https://github.com/EsimMoe/MiniLPA/raw/main/assets/粘贴-激活码二维码-配置区域.apng)
> 通过按下 `Ctrl + V`(macOS 为 `Command + V`) 或 `Shift + Insert` 粘贴快捷键以解析剪贴板内的激活码二维码图片
 
> ![通知-选择器使用](https://github.com/EsimMoe/MiniLPA/raw/main/assets/通知-选择器使用.apng)
> 点击通知页内通知条目右侧的复选框 可以进入选择模式  
> 在选择模式下 点击其他条目可以选择或者取消选择 也可使用右上方快捷选择器进行全选 批量选择 退出选择模式等  
> 通过在按下 Shift 时点击 可以区选上次点击和本次点击之间的条目  
> 此时进行操作将会应用于所选对象(通过右键菜单操作一个未选择的条目将会针对条目本身 而不是已选择的条目)

## 构建
> [!NOTE]  
> 本项目需要使用 PowerShell 脚本进行构建, 请提前安装

```shell
pwsh
# -NativeExecutable 构建二进制可执行文件
# -NativeExecutableType [app-image, exe, msi, rpm, deb, pkg, dmg] 指定构建的可执行文件类型
# -NativeWayland 启用原生 Wayland 支持
# -SkipSetupResources 跳过初始化资源
# -GithubToken 指定初始化资源时使用的 Github 令牌
scripts/Build.ps1
```

## 待办事项清单
- [ ] MiniRemoteLPA
- [ ] 摄像头扫描

## 反馈
我们欢迎任何反馈 如果有任何问题或建议  
欢迎前往 [议题](https://github.com/EsimMoe/MiniLPA/issues) 开启新的工单以获取支持  
不过请注意 本项目为公益项目  
您的反馈可能会被关闭或不被处理

> [!IMPORTANT]  
> #### 反馈前需要确认的事
> - 查找是否有类似的 [议题](https://github.com/EsimMoe/MiniLPA/issues)
> - 使用本项目御用推荐的 `Java` 发行版运行时
> - 使用 CI 内最新构建
> - **完整并详细** 的描述你的问题
> - 在有需要情况下提供日志与环境信息
> - 尝试复现问题并找到错误复现的规律

## 常见问题
> #### 为什么使用远程桌面连接时无法使用服务器端的智能卡?
> 为了安全和便利 微软使用了智能卡重定向技术  
> 远程桌面下使用 PCSC 智能卡服务 访问到的仍为客户机本地的智能卡  
> 如需访问服务器端的智能卡 请使用其他的远程控制软件  
> 另见: [智能卡和远程桌面服务](https://learn.microsoft.com/zh-cn/windows/security/identity-protection/smart-cards/smart-card-and-remote-desktop-services)

> #### 为什么别的 LPA 软件没有管理通知的能力 MiniLPA 却有? 这符合规范么?
> 根据 GSMA 的规范化行为通知应在产生后立即发送并移除  
> 部分运营商会根据 `安装` 与 `删除` 通知决定卡片状态以激活或允许重新安装  
> 但据部分用户反馈 极少数运营商有时会出现通知正确发送时延迟处理或不能正确处理的情况  
> 需要多次发送方可解决 遂默认保留 `安装` 与 `删除` 的通知  
> 而 `启用` 与 `禁用` 通知在大部分情况下即使不发送也不会产生显著影响  
> 为了提高切卡速度 及保护隐私 遂默认不进行发送与移除  
> 如用户希望遵守 GSMA 规范请前往 `设置` - `行为` 将关于通知发送与移除相关选项全部勾选即可

> #### 为什么 Linux 下使用 HiDPI 屏幕时 软件窗口看起来很小?
> Linux 下显示方案较多 `Java` 主线并未细化兼容各种方案下的 HiDPI 缩放  
> 你可以切换至 [JetBrainsRuntime](https://github.com/JetBrains/JetBrainsRuntime) 其可以处理大部分情况的缩放  
> 或加入以下启动参数
> ```
> -Dsun.java2d.uiScale=2
> ```
> 或使用环境变量
> ```
> GDK_SCALE=2
> ```

## 特别鸣谢
**排名不分前后**  
[@ShiinaSekiu](https://www.github.com/ShiinaSekiu) 主要代码编写, 语言校对  
[@Shiroki-uika](https://www.github.com/Shiroki-uika) 日语译者  
[@quul](https://www.github.com/quul) 简体中文译者, macOS 与 Linux 端测试者, macOS Homebrew 安装源维护者  
[@ous50](https://www.github.com/ous50) macOS 与 Linux 端测试者  
[@sekaiacg](https://www.github.com/sekaiacg) Linux 端测试者  
以及那些未在此提及的朋友们 **还有你**

## 相关读物
「[DIY eSIM写卡神器: 如何使用MiniLPA给速易卡、5ber、eSTK、9esim、DIY ST33写卡](https://www.wenziwanka.com/simcard/e-sim/102.html)」  

各语言译本仅供参考, 文义如与简体中文有歧义或缺失, 应以简体中文版本为准