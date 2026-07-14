<h1 align="center">JDA-Forge-Plus</h1>

<p align="center">
An extra <b>feature pack</b> for <a href="https://github.com/drgnbld7/JDA-Forge">JDA-Forge</a>.<br/>
Admin utility commands, database backups delivered to Discord, retention, and ops placeholders — driven by one config file.
</p>

<p align="center">
<code>Requires JDA-Forge</code> &nbsp;•&nbsp; <code>Java 21</code> &nbsp;•&nbsp; <code>MIT License</code>
</p>

---

## ✨ Features

| | Feature | |
|---|---|---|
| 🛡️ | **Admin commands** | `/botinfo`, `/serverinfo`, `/userinfo`, `/avatar`, `/ping`, `/uptime` — admin-only. |
| 📦 | **Backup → Discord** | Auto-uploads new database backups to a channel (upload-size aware). |
| 🧹 | **Retention** | Prunes old files from `backups/` and `logs/` by age and/or count. |
| ♻️ | **Module management** | `/reload` reloads all modules; `/modules` lists them with status. |
| 🏷️ | **Ops placeholders** | `%uptime%`, `%ram_used%`, `%latency%`, `%guild_count%` and more. |

---

## 🚀 Installation

1. Download **`jda-forge-plus.jar`** from the [Releases](../../releases) page.
2. Drop it into your bot's **`modules/`** folder.
3. Start the bot once — it generates **`config/jda-forge-plus.yml`**.
4. Edit that file (channel IDs, toggles, colors…), then restart.

---

## 💬 Commands

All commands are **admin-only** by default and reply ephemerally.

| Command | Description |
|---------|-------------|
| `/botinfo` | Bot status: uptime, RAM, CPU, guild count, gateway ping, Java & JDA versions. |
| `/ping` | Gateway and REST latency. |
| `/uptime` | How long the bot has been running. |
| `/serverinfo` | Information about the current server. |
| `/userinfo [user]` | Information about a user (defaults to you). |
| `/avatar [user]` | A user's avatar. |
| `/backup` | Upload the most recent database backup to Discord. |
| `/reload` | Reload all modules. |
| `/modules` | List loaded modules and their status. |

> Global slash commands can take up to ~1 hour to appear the first time (a Discord limitation).

---

## ⚙️ Configuration (`config/jda-forge-plus.yml`)

Every feature can be toggled and tuned. Overview:

```yaml
commands:
  require-admin: true          # commands are admin-only
  allowed-role-ids: []         # extra roles allowed to use them
  ephemeral: true              # replies visible only to the invoker

info-commands:
  embed-color: "#9bb0cc"       # embed accent color (hex)
  botinfo: true                # toggle each command individually
  ping: true
  uptime: true
  serverinfo: true
  userinfo: true
  avatar: true

backup-to-discord:
  enabled: false
  channel: "0"                 # target text channel ID
  max-size-mb: 0               # 0 = auto-detect from the guild's boost tier
  delete-after-upload: false
  watch-interval-seconds: 30
  message: "New database backup"
  command-enabled: true        # enable the /backup command

backup-retention:
  enabled: false
  max-age-days: 7              # delete archives older than N days (0 = ignore)
  max-files: 0                 # keep only the newest N (0 = ignore)
  run-interval-minutes: 60

log-retention:
  enabled: false
  max-age-days: 14
  max-files: 0
  include-crash-dumps: true    # also prune dump_*.txt
  run-interval-minutes: 360

placeholders:
  enabled: true
  date-format: "yyyy-MM-dd"
  time-format: "HH:mm:ss"
```

> **Backup → Discord** requires the host bot to have the database enabled with `auto-backup` on, so archives appear in `backups/`. The `/backup` command works with any existing archive.

---

## 🏷️ Placeholders

Registered when `placeholders.enabled` is true:

`%uptime%` · `%ram_used%` · `%ram_max%` · `%ram_percent%` · `%cpu_cores%` · `%guild_count%` · `%latency%` · `%thread_count%` · `%date%` · `%time%`

Use them in your `jda-forge.yml` presence text or in any module that resolves placeholders.

---

## 🛠️ Building from source

Requires JDK 21 and the JDA-Forge dependency (via JitPack or a local install).

```bash
mvn clean package
```

The module jar is placed in the `target/` directory — copy it into your bot's `modules/` folder.

---

## 📄 License

Released under the MIT License. See [LICENSE](LICENSE).
