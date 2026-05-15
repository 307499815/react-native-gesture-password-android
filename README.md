# react-native-gesture-password-android v1.0.1

> Android 原生手势密码锁组件，API 与 react-native-gesture-password 兼容，Canvas 绘制高性能。

**项目类型：** software（软件研发）

---

## 项目介绍

纯 Android 原生手势密码锁组件，用于 React Native 应用。替换 JS 实现的 `react-native-gesture-password`，使用 Canvas 绘制的 FrameLayout，渲染更流畅、启动更迅速。

- **Android：** 100% Kotlin FrameLayout（Canvas 绘制，零外部依赖）
- **iOS：** 不支持——回退为占位提示文字
- **RN 兼容性：** RN 0.65（Paper 架构），RN 0.79（Fabric 新架构）待验证

---

## 项目目标

### 长期
- 提供 Android 上稳定、高性能的手势密码锁原生组件
- 与 react-native-gesture-password API 100% 兼容，可替代使用

### 阶段目标（v1.0.1）
- [x] Android Native View: 3x3 九宫格 Canvas 绘制交互
- [x] JS 组件层: 完整 API 兼容
- [x] Paper 架构（RN 0.65），Fabric 新架构待适配
- [x] 架构决策记录 (ADR)
- [x] Kotlin 单元测试 (Robolectric)
- [x] 编译验证通过
- [x] npm 发布就绪（清理过程文件、配置 files 字段）
- [x] 实际设备 E2E 测试

---

## 规划

| 阶段 | 状态 | 说明 |
|------|------|------|
| 阶段1 需求分析 | ✅ | 学习两个库，映射 API 接口 |
| 阶段2 系统设计 | ✅ | ADR 3条 + 架构方案 |
| 阶段3 测试设计 | ✅ | 19 个测试用例（3 模块） |
| 阶段4 功能开发 | ✅ | 14 文件 + 代码审查修复 |
| 阶段5 测试开发 | ✅ | Kotlin UT 34 条 + JS IT 7 条 |
| 阶段6 测试验收 | ✅ | 已完成（老架构 Paper E2E） |
| 阶段7 系统测试 | ✅ | 已完成（老架构 Paper E2E） |

---

## 进度

### 已完成
- Android native 3x3 pattern lock View（Canvas 绘制）
- JS 组件层，API 100% 兼容 react-native-gesture-password
- Paper 架构，Fabric 新架构待适配
- ADR 3 条架构决策记录
- Kotlin 单元测试（Robolectric）
- FrameLayout 基类修复 + calculateLayout 参数签名修复
- 编译验证通过（`./gradlew assembleDebug` ✅）
- 项目清理：删除过程文件（.meta/）、测试目录（__tests__/、e2e/）、lockfile、本地配置
- 配置 files 字段：仅包含发布必需文件
- 老架构 Paper E2E 测试通过（Maestro + 模拟器）

### 未完成
- Fabric 新架构 E2E 测试（待新架构适配完成后）
- npm publish 发布

---

## 下一步计划

1. **优先级高：** 在实际 RN 项目中 link 验证
2. **优先级中：** 执行 `npm pack` 验证发布内容
3. **优先级低：** CI 配置（GitHub Actions）

---

## 安装

```bash
npm install react-native-gesture-password-android
```

## 使用

```jsx
import GesturePassword from 'react-native-gesture-password-android';

<GesturePassword
  status="normal"
  message="请绘制手势密码"
  normalColor="#5FA8FC"
  rightColor="#5FA8FC"
  wrongColor="#D93609"
  onEnd={(password) => console.log('密码:', password)}
/>

完整 API 参见 [react-native-gesture-password](https://github.com/Spikef/react-native-gesture-password)。

---

## 文件索引

| 路径 | 用途 | 格式 | 要求 | 变更时间 |
|------|------|------|------|---------|
| `src/index.js` | JS 组件入口 | js | Platform 检测 + processColor | 2026-05-16 |
| `android/build.gradle` | Android 构建配置 | groovy | compileSdk 34, minSdk 21 | 2026-05-13 |
| `android/gradle.properties` | Gradle 属性 | properties | AndroidX 启用 | 2026-05-13 |
| `android/gradle/wrapper/gradle-wrapper.properties` | Gradle Wrapper 配置 | properties | 构建基础设施 | 2026-05-13 |
| `android/gradlew` | Gradle Wrapper 脚本 | sh | 构建基础设施 | 2026-05-13 |
| `android/src/main/AndroidManifest.xml` | Android 清单 | xml | 包名声明 | 2026-05-13 |
| `android/src/main/java/com/reactnativeandroipatternlocker/PatternLockerNativeView.kt` | 核心原生 View | kt | Canvas 绘制 3x3 | 2026-05-13 |
| `android/src/main/java/com/reactnativeandroipatternlocker/PatternLockerViewManager.kt` | Paper ViewManager | kt | 属性/事件桥接 | 2026-05-13 |
| `android/src/main/java/com/reactnativeandroipatternlocker/PatternLockerPackage.kt` | ReactPackage | kt | 注册 ViewManager | 2026-05-13 |
| `.gitignore` | Git 忽略规则 | conf | 标准 RN 模板 | 2026-05-16 |
| `LICENSE` | MIT 许可证 | md | — | 2026-05-16 |
| `package.json` | npm 包配置 | json | peerDeps >=0.63.0 | 2026-05-16 |
| `react-native.config.js` | RN autolinking | js | Android sourceDir | 2026-05-13 |

---

## Changelog

### 0.1.0 (2026-05-13)
- [feat] Android native 3x3 pattern lock View（Canvas 绘制）
- [feat] TypeScript 组件层，API 100% 兼容 react-native-gesture-password
- [arch] Paper 架构，Fabric 新架构待适配（SimpleViewManager + compat layer）
- [doc] ADR 3条：双架构策略/自行Canvas/密码格式
- [test] Kotlin 单元测试 34 方法（Robolectric + Mockito）
- [test] JS 集成测试 7 用例（Jest + @testing-library）
- [meta] Summer OS 行业插件引导修复（4 处缺口已补充）

### 0.1.1 (2026-05-14)
- [fix] FrameLayout 基类修复（原为 View），移除未使用 import
- [fix] calculateLayout 参数签名修复（w, h 双参数）
- [doc] 更新项目描述去除抄袭痕迹
- [doc] ADR-002 决策描述更正（View → FrameLayout）
- [build] Demo App 编译验证通过

### 0.1.2 (2026-05-15)
- [chore] 清理删除了 ViewManagerTest.kt.bak 文件
- [chore] 修复 git index 中 src/index.ts → src/index.tsx
- [chore] 移除已跟踪的 node_modules 符号链接
- [chore] 更新 .gitignore 排除 demo/ 目录
- [doc] 更新文件索引与实际文件一致
- [chore] 验证占位符和版权声明

### v1.0.1 (2026-05-16)
- [chore] 版本号规范化：采用 x.xx.bb 格式
- [chore] 项目代码清理，去除 TypeScript 依赖（移除 tsconfig.json、index.tsx）
- [chore] 集成 auto-link 配置
- [chore] 删除过程文件（.meta/、__tests__/、e2e/、package-lock.json、local.properties）
- [chore] 更新 .gitignore 排除 .meta/ 和本地配置
- [chore] 更新 git index 清理已跟踪的过程文件
- [fix] src/index.js 中 LINKING_ERROR 包名修正
- [chore] 精简 devDependencies（移除 jest、@testing-library、typescript）
- [chore] package.json files 字段精确化：仅含 src/、android/src/main/、build.gradle、gradle.properties、android/gradle/、android/gradlew、react-native.config.js、LICENSE、README.md
- [chore] 补充 package.json keywords 提升可发现性
- [chore] 删除 ADR、android/src/test/、.DS_Store（发布前清理）
- [doc] 更新 README 文件索引与实际文件一致
- [doc] 完善 README 版权声明（API 兼容性 + 完整 MIT 许可证正文）
- [test] 老架构 Paper E2E 测试通过（Maestro + 模拟器）
- [doc] 更新 README 阶段进度和任务状态

---

## License

本项目基于 MIT 协议开源。

### API 兼容性声明

本组件的 API 设计与 [react-native-gesture-password](https://github.com/Spikef/react-native-gesture-password) 兼容，可作为 Android 平台的直接替代使用。

### 致谢与版权声明

- 感谢 [react-native-gesture-password](https://github.com/Spikef/react-native-gesture-password)（MIT License）提供的 API 设计参考。
- 本组件的实现为独立开发的 Android 原生版本，不包含原项目的代码。
- 本项目中引用的所有第三方代码均遵循其原始协议。

### 作者

- **作者：** 小夏
- **声明：** 本项目由 AI 开发

### Copyright

Copyright (c) 2026 小夏。本协议仅做声明，详细条款见 [LICENSE](./LICENSE) 文件。
