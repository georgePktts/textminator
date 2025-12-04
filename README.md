textminator
-----------

`textminator` is a Java-based command-line tool that sanitizes sensitive data
from text files or streams. It is designed for developers, DevOps engineers,
and teams handling logs or raw text that may contain private or regulated
information such as emails, IP addresses, UUIDs, or any custom pattern.

It supports piping, file-to-file processing, configurable rule sets, detailed
diagnostics, and multiple verbosity levels.  
By using a simple `.properties` configuration model, users can define custom
regex-based transformations without modifying or recompiling the tool.

---

## Features

- **Regex-driven sanitization** through a simple `.properties` file  
- **File input/output** with overwrite protection  
- **stdin pipeline support** (`cat logs | textminator`)  
- **Dry-run mode** to preview replacements  
- **Per-rule statistics** (`--stats`)  
- **Verbose and debug modes** (up to `-vvv`)  
- **Low-level tracing** of rule matches (`--trace`)  
- **Config inspection** (`--config-info`, `--config-example`)  
- **Colorized output**, automatically disabled when piped  
- **Interactive mode exit** using Ctrl+D (Unix) or Ctrl+Z (Windows)  
- **Easily extensible** with new rules—no code changes required  

---

## Installation

Currently, textminator is distributed as source code.

### Requirements
- **Java 17+**
- **Maven**

### Build from Source

```
mvn clean package
```
## Usage
### You can run it using:
```
java -jar textminator.jar
```

### OR by creating an alias:
```
textminator="java -jar ~path/to/textminator.jar"
```

### Input Options
#### Configuration
|Option|Description|
|------|-----------|
|-c, --config <file>|Path to custom config file|
|--config-example|Print an example configuration file and exit|
|--config-info|Print the effective loaded configuration and exit|

### Input / Output
|Option|Description|
|------|-----------|
|-i, --input <file>|Input file (default: stdin)|
|-o, --output <file>|Output file (default: stdout)|
|-f, --force|Overwrite output file if already exists|

### Diagnostics & Logging
|Option|Description|
|------|-----------|
|-s, --stats|Print per-rule match statistics after processing|
|--dry-run|Same as --stats but without writing output|
|-q, --quiet|Suppress all diagnostic output except errors|
|-v|Increase verbosity; repeat up to 3 times (-vvv)|
|--trace|Very verbose low-level rule tracing (independent of -v)|

### Other
|Option|Description|
|------|-----------|
|-h, --help|Print help and exit|
|-V, --version|Print version and exit|


## Examples
Process text from stdin to stdout<br>
```cat input.txt | textminator```

Sanitize a file to stdout<br>
```textminator -i input.txt```

Sanitize and write to a file<br>
```textminator -i input.txt -o clean.txt```

Preview replacements (dry-run)<br>
```textminator --dry-run -i input.txt```

Show per-rule statistics<br>
```textminator --stats -i input.txt```

Use a custom config file<br>
```textminator --config myrules.properties -i input.txt```


## Configuration
The tool uses a simple .properties file that defines sanitization rules.

### Rule Definition Model
Each rule shares the same prefix:
```
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

ipv4.regex=\b(?:\d{1,3}\.){3}\d{1,3}\b
ipv4.replacement=<IPv4>
ipv4.order=3
ipv4.enabled=true

ipv6.regex=\b[0-9a-fA-F:]{2,39}\b
ipv6.replacement=<IPV6>
ipv6.order=4
ipv6.enabled=true
```

### Configuration file locations

`textminator` will use the following paths in order to load config file
1. `--config` option
2. `textminator.properties` file next to .jar
3. default file

## Exit Codes
|Code|Meaning|
|----|-------|
|0|Successful execution|
|1|Processing error|


## Design Philosophy
### `textminator` follows a few core principles:
1. Predictability over magical behavior
Rules apply strictly in numeric order and never overlap unexpectedly.
2. Transparency
`--stats`, `--dry-run`, and `--trace` are designed to show exactly how the tool behaves.
3. Minimal dependencies
Only standard Java & Picocli are used.
4. Fail-fast
Errors stop execution immediately unless overridden by verbosity settings.
5. Cross-platform consistency
Same output on macOS, Linux, and Windows.
Performance Notes
The tool loads all rules into memory once and applies them sequentially.
Regex performance depends on complexity of user-defined expressions.
`--trace` dramatically slows down processing (intended for debugging only).
Piping through stdin avoids I/O overhead for large files.
For extremely large files, prefer streaming via stdin:
cat huge.log | textminator > sanitized.log


## Why textminator?

There are other tools that manipulate text (sed, awk, grep, Perl), but:

### **1. Regex readability**
You can describe sanitization logic in a simple configuration file—not inline,
not escaped inside a shell, and not inside long sed commands.

### **2. Order-based rule execution**
Text is sanitized in deterministic order based on rule priority.  
This avoids cascaded replacements or regex conflicts.

### **3. Multi-platform consistency**
Same behaviour across Linux, macOS, and Windows.

### **4. Diagnostics for debugging**
- Detailed rule hit counts  
- Dry-run mode  
- Tracing of rule-by-rule matching  

### **5. No scripting knowledge required**
Anyone can modify the `.properties` file and instantly change behaviour.
