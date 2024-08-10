<div align="center">
    <br />
    <img alt="MiniLPA" src="https://github.com/EsimMoe/MiniLPA/raw/main/assets/logo.svg">
</div>

<h3 align="center">Elegant Modern LPA UI</h4>

<p align="center">
    <span>English</span> |
    <a href="README_zh-CN.md">简体中文</a>
</p>

<p align="center">
    <a href="#features">Features</a> •
    <a href="#installation">Installation</a> •
    <a href="#tips">Tips</a> •
    <a href="#building">Building</a> •
    <a href="#todo-list">Todo List</a> •
    <a href="#feedback">Feedback</a> •
    <a href="#faq">FAQ</a> •
    <a href="#special-thanks">Special Thanks</a>
</p>

<div align="center">
    <img alt="Theme Display" src="https://github.com/EsimMoe/MiniLPA/raw/main/assets/主题展示.apng">
</div>

## Promotion
The MiniLPA development team recommends Estkme eUICC cards here, with SiP advanced packaging technology that is less susceptible to damage from external forces.
When unable to use conventional LPA management cards, you can also use the STK menu for card management, which is convenient and user-friendly.  
Use `MiniLPA` as a coupon code to get 10% off on your order.
Click here to enter the product page -> [ESTKme-ECO](https://www.estk.me/product/estkme-eco/?aid=MiniLPA)


## Features
- Good cross-platform support (Windows, Linux, macOS)
- More user-friendly interface
- i18n multi-language support
- Search and quick navigation
- Manage esim notifications freely
- Support drag and drop and paste esim QR code or activation code
- Multiple Emoji design choices
- Multiple themes with automatic day and night theme switching

## Installation
> [!NOTE]
> PCSC smart card services are required for this project.  
> Windows and macOS should already come with and enable it by default.  
> If you are a Linux user, please check and install [PCSC-Lite](https://pcsclite.apdu.fr/).

#### Simple Installation
This project is written in `Kotlin`, so you need to install `Java` runtime to use it.  
But for convenience, pre-packaged JVM versions will be provided for easy installation and use out of the box.  
Simply download according to your system and architecture. [MiniLPA Releases](https://github.com/EsimMoe/MiniLPA/releases/latest)

#### Using Package Managers
> ##### macOS Homebrew
> ```
> brew install EsimMoe/homebrew-cask/minilpa
> ```
Feel free to contribute more package manager support options!

#### Manual Java Installation
> [!IMPORTANT]  
> MiniLPA requires at least **Java 21** to run

The recommended `Java` distribution for this project is [Azul Zulu](https://www.azul.com/downloads/#zulu).  
For Linux, it is recommended to use  [JetBrainsRuntime](https://github.com/JetBrains/JetBrainsRuntime) (as it can correctly adapt to different display scaling solutions)  
Other distributions are also viable, but there's no guarantee that they will work as well.

> [!TIP]  
> If you are using Wayland on Linux with JetBrainsRuntime, you can enable native Wayland application support by adding the following startup parameter:
> ```
> -Dawt.toolkit.name=WLToolkit
> ```
> However, native Wayland application support is still in its early access and may have some issues  
> Please do not report problems with this to MiniLPA.

## Tips
> ![Drag & Drop - Activation Code QR Code - Configuration Area](https://github.com/EsimMoe/MiniLPA/raw/main/assets/拖拽-激活码二维码-配置区域.apng)
> Quickly parse information by dragging and dropping the activation code QR code image into the configuration area.

> ![Drag & Drop - Activation Code QR Code - Download Window](https://github.com/EsimMoe/MiniLPA/raw/main/assets/拖拽-激活码二维码-下载窗口.apng)
> Dropping into the download window is also feasible, and the same applies to other drag and drop actions.

> ![Drag & Drop - Activation Code - Configuration Area](https://github.com/EsimMoe/MiniLPA/raw/main/assets/拖拽-激活码-配置区域.apng)
> You can also drag and drop the activation code text.

> ![Paste - Activation Code QR Code - Configuration Area](https://github.com/EsimMoe/MiniLPA/raw/main/assets/粘贴-激活码二维码-配置区域.apng)
> Use the `Ctrl + V` (or `Command + V` for macOS) or `Shift + Insert` shortcut to paste and parse activation code QR images from the clipboard.

> ![Notification - Selector Usage](https://github.com/EsimMoe/MiniLPA/raw/main/assets/通知-选择器使用.apng)
> Click the checkbox on the right side of the notification item in the notification page to enter selection mode.
> In selection mode, click other items to select or deselect them. You can also use the quick selector in the upper right corner to select all, batch select, exit selection mode.  
> By pressing `Shift` and clicking, you can select the items between the last click and the current click.  
> The operation will be applied to the selected item. (Manipulating an unselected item via the context menu will target the item itself, not the selected items.)

## Building
> [!NOTE]  
> This project requires using PowerShell scripts for building, please install in advance.

```shell
pwsh
# -NativeExecutable    Build binary executables
# -NativeExecutableType [app-image, exe, msi, rpm, deb, pkg, dmg]    Specify the type of executable to build
# -NativeWayland    Enable Native Wayland Support
# -SkipSetupResources    Skip initializing resources
# -GithubToken    Specify the Github token to use when initializing the resource
scripts/Build.ps1
```

## Todo List
- [ ] MiniRemoteLPA
- [ ] Camera scanning

## Feedback
We welcome any feedback. If you have any questions or suggestions, please go to [Issues](https://github.com/EsimMoe/MiniLPA/issues) section to open a new ticket for support.  
Please note that this project is a public welfare project. Your feedback may be closed or not processed.

> [!IMPORTANT]  
> #### Things to Confirm Before Providing Feedback
> - Find out if there are similar [issues](https://github.com/EsimMoe/MiniLPA/issues)
> - Use the recommended `Java` runtime distribution for this project
> - Use the latest builds from CI
> - Describe your problem **completely and in detail**
> - Provide log and environment information when necessary
> - Try to reproduce the problem and find the pattern of error reproduction

## FAQ
> #### Why can't I use the server-side smart card when using Remote Desktop connection?
> For security and convenience, Microsoft uses smart card redirection technology.
> When using Remote Desktop, the PCSC smart card service still accesses the smart card on the local client.
> If you need to access the smart card on the server, please use other remote control software.
> See also: [Smart Card and Remote Desktop Services](https://learn.microsoft.com/en-us/windows/security/identity-protection/smart-cards/smart-card-and-remote-desktop-services)

> #### Why does MiniLPA have the ability to manage notifications while other LPA software does not? Is this in compliance with standards?
> According to GSMA's standardized behavior, notifications should be sent and removed immediately after being generated.
> Some operators determine the card status for activation or reinstallation based on `install` and `delete` notifications.  
> However, according to feedback from some users, there may be cases where notifications are correctly sent but delayed or not handled correctly by a few operators.  
> Repeated sending may be required to resolve, so `install` and `delete` notifications are kept by default.  
> Meanwhile, `enable` and `disable` notifications, in most cases, do not have a significant impact even if not sent or removed.  
> To improve switching speed and protect privacy, sending and removal are not performed by default.  
> If users wish to comply with GSMA standards, they can go to `Settings` - `Behavior` and check all options related to notification sending and removal.

> #### Why do software windows look small on Linux with HiDPI screens?
> On Linux, there are various display solutions, and the mainline `Java` does not finely adapt to HiDPI scaling in all solutions.
> You can switch to [JetBrainsRuntime](https://github.com/JetBrains/JetBrainsRuntime), which can handle most scaling scenarios.
> Or add the following startup parameters:
> ```
> -Dsun.java2d.uiScale=2
> ```
> Or use environment variables:
> ```
> GDK_SCALE=2
> ```

## Special Thanks
**In no particular order**  
[@ShiinaSekiu](https://www.github.com/ShiinaSekiu) Main code writer, language proofreader  
[@Shiroki-uika](https://www.github.com/Shiroki-uika) Japanese translator  
[@quul](https://www.github.com/quul) Simplified Chinese translator, macOS and Linux tester, maintainer of macOS Homebrew installation sources  
[@ous50](https://www.github.com/ous50) macOS and Linux tester  
[@sekaiacg](https://www.github.com/sekaiacg) Linux tester  
And all friends not mentioned here **AND YOU**

Translations are for reference only. If there is any discrepancy or absence of meaning between the Simplified Chinese version and the translated version, the Simplified Chinese version shall prevail.