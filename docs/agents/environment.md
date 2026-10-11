# 工作环境与多代理纪律

本仓库在 Codex 沙箱中的固定事实与约定。开始执行 git/mvn 前先读这里，避免重复探测。

## 沙箱与权限

- 沙箱内**无网络**：Maven 一律离线运行（`mvn -o ...`），本机 `~/.m2` 已缓存 Boot 2.7.18 依赖链。
- `.git` 与 `~/.m2` 不在可写沙箱内（`.git` 是受保护的只读路径，属 Codex 默认行为）：git 写操作（commit/merge/worktree）与 `mvn` 需以 `require_escalated` 运行；会话内通常已有 `git`、`mvn -o` 前缀审批规则。
- 只读命令（`git log/status/diff`、`cat`、`rg`）在沙箱内直接运行即可。
- 若在 Codex 配置里放开沙箱网络（`[sandbox_workspace_write] network_access = true`），仍需给 Maven 追加可写根（`writable_roots = ["~/.m2"]`）才能在沙箱内直接跑 `mvn`；`.git` 的只读保护不受该配置影响。

## 构建事实

- 本机 JDK 21 + Maven 3.9.16；主代码以 `--release 8` 编译（class 文件 major 52）。
- 本地缓存**没有** `spring-boot-starter-parent:2.7.18`：pom 通过导入 `spring-boot-dependencies` BOM 管理版本，勿改用 parent。
- 检查命令：`mvn -o clean test`；产出构件：`mvn -o clean install`。
- 验收基线：Spring Boot 2.7.18；Boot 3 为尽力兼容（不写进验收）。主代码不得引用 `javax.servlet`/`jakarta.servlet`（由 `ServletNamespaceGuardTest` 守卫，见 ADR-0004）。

## 多代理工作树纪律

- 每个实现者**只在自己被分配的 worktree** 里工作；主检出（repo 根）不是工作区，不是提交目标。
- 任何 `git`/`mvn` 命令前先执行 `pwd && git branch --show-current`，确认目录与分支符合分配；提交前再确认一次。
- `main` 受 pre-commit hook 保护：在 main 上提交会被拒绝（逃生：`ALLOW_MAIN_COMMIT=1`）。新克隆需激活一次：`git config core.hooksPath .githooks`。
- 子代理开工先回报一行握手（cwd + 分支 + 计划）；编排方探活优先看文件系统证据（worktree 内的新文件、`git log`），静默等待不超过几分钟就要主动核查。
