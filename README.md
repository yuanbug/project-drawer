
## 简介

这个项目可以在开发环境下帮你生成方法关系图，为代码分析和源码阅读提供直观的视觉辅助。

![前端截图](./docs/前端截图.png)

## 使用方法

1. 安装 node.js（版本大于或等于 18），并确保 node 和 npm 全局可访问。
   > 你不需要掌握任何前端技能，只需确保 node.js 已在机器上安装即可。

2. 下载本项目源码，使用 IntelliJ IDEA 打开。

3. （可选）将 `main` 模块的 `src/main/web` 目录标记为已排除，防止 IDEA 对 `node_modules` 目录的文件构建索引，避免卡死。
   > 右键点击目录 -> Mark Directory as（将目录标记为） -> Excluded（已排除）
   >
   > ![标记排除](./docs/标记排除.png)

4. 为 `main` 模块的 `ServerApplication` 启动类添加 VM 参数 `work-path`，取值为你要分析的项目的路径。如果你的项目不是 maven 项目，请参照[下文](#配置)进行模块配置。
   > 例如 `-Dwork-path=C:\code\projects\jackson`
   > 
   > 你也可以直接修改 `MyAppConfig` 类的构造方法，将目标项目的路径传给父类构造方法 `super(xxxxxx)` 

5. 启动项目（`ServerApplication`），待控制台输出前端访问地址（[http://localhost:5173](http://localhost:5173)）后通过浏览器访问。
   > ![控制台输出前端访问地址](./docs/控制台输出前端访问地址.png)

## 配置

TODO
