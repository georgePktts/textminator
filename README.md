textminator (txmtr)
-------------------

`textminator` is a Java-based command-line tool that sanitizes sensitive data
from text files or streams. It is designed for developers, DevOps engineers,
and teams handling logs or raw text that may contain private or regulated
information such as emails, IP addresses, UUIDs, or any custom pattern.

It supports piping, file-to-file processing, configurable rule sets, detailed
diagnostics, and multiple verbosity levels.
By using a simple `.properties` configuration model, users can define custom
regex-based transformations without modifying or recompiling the tool.


## Features
- **Regex-driven sanitization** through a simple `.properties` file
- **File input/output** with overwrite protection
- **stdin pipeline support** (`cat logs | txmtr`)
- **Dry-run mode** to preview replacements
- **Per-rule statistics** (`--stats`)
- **Verbose and debug modes** (up to `-vvv`)
- **Low-level tracing** of rule matches (`--trace`)
- **Config inspection** (`--config-info`, `--config-example`)
- **Colorized output**, automatically disabled when piped
- **Interactive mode exit** using Ctrl+D (Unix) or Ctrl+Z (Windows)
- **Easily extensible** with new rules—no code changes required


## Installation
Currently, textminator is distributed as source code.

### Requirements
- **Java 17+**
- **Maven**

### Build from Source
```bash
mvn clean package
```


## Usage
### Command syntax (from `--help`)
```bash
textminator [[-c=<userConfigFile>] [--config-example] [--config-info]]
            [[-i=<inputFile>] [-o=<outputFile>] [-f]] [[-s] [--dry-run]
            [-q] [-v]... [--trace] [-h] [-V]]
```

### Run directly
```bash
java -jar textminator.jar
```

### Or create a shell alias
```bash
alias textminator="java -jar /path/to/textminator.jar"
```

### For convenience, you can create a short alias:
```bash
alias txmtr="java -jar /path/to/textminator.jar"
```

#### Then you can simply run:
```bash
txmtr -i input.txt -o output.txt
```

## Input Options
### Configuration
|Option|Description|
|------|-----------|
|`-c, --config <file>`|Path to custom config file|
|`--config-example`|Print an example configuration file and exit|
|`--config-info`|Print the effective loaded configuration and exit|

### Input / Output
|Option|Description|
|------|-----------|
|`-i, --input <file>`|Input file (default: stdin)|
|`-o, --output <file>`|Output file (default: stdout)|
|`-f, --force`|Overwrite output file if already exists|

### Diagnostics & Logging
|Option|Description|
|------|-----------|
|`-s, --stats`|Print per-rule match statistics after processing|
|`--dry-run`|Same as --stats but without writing output|
|`-q, --quiet`|Suppress all diagnostic output including errors|
|`-v`|Increase verbosity; repeat up to 3 times (-vvv)|
|`--trace`|Very verbose low-level rule tracing (independent of -v)|

### Other
|Option|Description|
|------|-----------|
|`-h, --help`|Print help and exit|
|`-V, --version`|Print version and exit|


## Examples
Process text from stdin to stdout
```bash
cat input.txt | txmtr
```

Sanitize a file to stdout
```bash
txmtr -i input.txt
```

Sanitize and write to a file
```bash
txmtr -i input.txt -o clean.txt
```

Preview replacements (dry-run)
```bash
txmtr --dry-run -i input.txt
```

Show per-rule statistics
```bash
txmtr --stats -i input.txt
```

Use a custom config file
```bash
txmtr --config myrules.properties -i input.txt
```


## Configuration
The tool uses a simple `.properties` file that defines sanitization rules.

### Rule Definition Model
Each rule shares the same prefix:
```properties
<name>.regex         # Required
<name>.replacement   # Optional (default: <REPLACED>)
<name>.order         # Required (lower = executed first)
<name>.enabled       # Optional (default: true)
```
Rules execute in ascending order based on their `<name>.order` property.
Order values must be unique across rules.

### Default rules

```properties
email.regex=[\w.+-]+@[\w-]+\.[\w.-]+
email.replacement=<EMAIL>
email.order=1
email.enabled=true

uuid.regex=\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}\b
uuid.replacement=<UUID>
uuid.order=2
uuid.enabled=true

ipv4.regex=\b(?:(?:25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)\.){3}(?:25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)\b
ipv4.replacement=<IPv4>
ipv4.order=3
ipv4.enabled=true

# Default: fast & broad IPv6 detection
ipv6.regex=\\b[0-9a-fA-F:]{2,39}\\b
# Alternative: stricter RFC-like IPv6
# NOTE: This is significantly slower (~40–50% slower in benchmarks)
#ipv6.regex=\b(?:fe80:(?::[0-9A-Fa-f]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(?:ffff(?::0{1,4}){0,1}:){0,1}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(?:\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3}|(?:[0-9A-Fa-f]{1,4}:){1,4}:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(?:\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3}|(?:[0-9A-Fa-f]{1,4}:){7}[0-9A-Fa-f]{1,4}|(?:[0-9A-Fa-f]{1,4}:){1,7}:|(?:[0-9A-Fa-f]{1,4}:){1,6}:[0-9A-Fa-f]{1,4}|(?:[0-9A-Fa-f]{1,4}:){1,5}(?::[0-9A-Fa-f]{1,4}){1,2}|(?:[0-9A-Fa-f]{1,4}:){1,4}(?::[0-9A-Fa-f]{1,4}){1,3}|(?:[0-9A-Fa-f]{1,4}:){1,3}(?::[0-9A-Fa-f]{1,4}){1,4}|(?:[0-9A-Fa-f]{1,4}:){1,2}(?::[0-9A-Fa-f]{1,4}){1,5}|[0-9A-Fa-f]{1,4}:(?:(?::[0-9A-Fa-f]{1,4}){1,6})|:(?:(?::[0-9A-Fa-f]{1,4}){1,7}|:))\b
ipv6.replacement=<IPV6>
ipv6.order=4
ipv6.enabled=true
```

### Configuration file locations

`textminator` will use the following paths, in order to, load config file:
1. `--config` option
2. `textminator.properties` file next to `.jar`
3. The built-in default configuration


## Exit Codes
|Code|Meaning|
|----|-------|
|0|Successful execution|
|1|Processing error|


## Design Philosophy
`textminator` follows a few core principles:

### 1. Predictability over magical behavior
Rules apply strictly in numeric order and never overlap unexpectedly.

### 2. Transparency
`--stats`, `--dry-run`, and `--trace` are designed to show exactly how the tool behaves.

### 3. Minimal dependencies
Only standard Java & Picocli are used.

### 4. Fail-fast
Errors stop execution immediately.

### 5. Cross-platform consistency
Same output on macOS, Linux, and Windows.


## Performance Notes
- The tool loads all rules into memory once and applies them sequentially.
- Regex performance depends on complexity of user-defined expressions.
- `--trace` dramatically slows down processing (intended for debugging only).
- Piping through stdin avoids I/O overhead for large files.


## Benchmark
The following benchmark was executed on a MacBook Pro M1 (16GB RAM) using a synthetic log file generated specifically for performance testing.

### Test File:
* Size: 2.1 GB
* Lines: 5,000,000
* Content: Random text containing UUIDs, IPv4, IPv6 and email patterns
* Rules: 4 regex replacement rules (from built-in config) applied to every line

### Command used:
```bash
time txmtr -i big_test_file.log -o output.log -sf
```
### Results
![textminator stats](assets/images/stats.png)
**Note:** Benchmark numbers may vary depending on JVM version, system load and OS-level caching.<br>
**Benchmark environment:** macOS 15, MacBook Pro M1 (16GB), OpenJDK 23. `txtminator` started with default rules.<br>
**Benchmark configuration:** `txtminator` running with the 4 default rules (email, UUID, IPv4, IPv6)<br>

### Interpretation
- Total (real) time: 206.7 s
- User CPU time: ~200 s → the majority of the processing time is spent inside the regex engine
- System time: ~4.4 s → very low I/O overhead
- CPU usage: 98% → fully saturates one CPU core (single-threaded stream processing)
- End-to-end throughput: ~10.4 MiB/s
- Lines processed: ~24,000 lines/s
- Total regex operations: >10 million replacements

### Summary
`textminator` is CPU-bound rather than I/O-bound.<br>
Almost all processing time is spent in the regex engine, which is expected for a single-threaded Java CLI applying multiple regex-based replacement operations per line of a multi-gigabyte file.

These results demonstrate solid real-world performance and confirm that `textminator` can efficiently process large log files without significant memory overhead.


## Why textminator?
There are other tools that manipulate text (sed, awk, grep), but:

### 1. Regex readability
You can describe sanitization logic in a simple configuration file—not inline,
not escaped inside a shell, and not inside long sed commands.

### 2. Order-based rule execution
Text is sanitized in deterministic order based on rule priority.  
This avoids cascaded replacements or regex conflicts.

### 3. Multi-platform consistency
Same behaviour across Linux, macOS, and Windows.

### 4. Diagnostics for debugging
- Detailed rule hit counts  
- Dry-run mode  
- Tracing of rule-by-rule matching  

### 5. No scripting knowledge required
Anyone can modify the `.properties` file and instantly change behaviour.
