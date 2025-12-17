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
- **Colorized output**, automatically disabled when piped
- **Easily extensible** with new rules—no code changes required


## Installation
Currently, `textminator` is distributed as source code.

### Requirements
- **Java 17+**
- **Maven**

### Build from Source
```bash
mvn clean package
```


## Usage
### Run directly
```bash
java -jar textminator.jar
```

### Or create a shell alias
```bash
alias textminator="java -jar /path/to/textminator.jar"

# For convenience, you can create a short alias:
alias txmtr="java -jar /path/to/textminator.jar"
```

#### Then you can simply run:
```bash
txmtr -i input.txt -o output.txt
```

### Behavior
- Reads from files or stdin
- Writes sanitized output to stdout
- Logs and diagnostics go to stderr
- Input and output are processed as UTF-8 encoded text


## Examples
Process text from stdin to stdout
```bash
cat input.txt | txmtr
```

Capture logs separately
```bash
cat input.txt | txmtr -s > sanitized.log 2> textminator.log
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
For full Configuration see [MANUAL.md](MANUAL.md).

### Rule Definition Model
Each rule shares the same prefix:
```properties
<name>.regex         # Required
<name>.replacement   # Optional (default: <REPLACED>)
<name>.order         # Required (lower = executed first)
<name>.enabled       # Optional (default: true)
```
Rules are applied in ascending order by `<name>.order`. If multiple rules share the same order, the tool emits a warning and applies those rules in alphabetical order by `<name>`.

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


## Disclaimer

`textminator` performs anonymization based on user-defined rules (e.g. regular expressions). The effectiveness and correctness of the anonymization fully depend on the quality and completeness of these rules.

`textminator` is provided "as is", without warranty of any kind. It is a best-effort tool and may not detect or anonymize all sensitive data. Always review and validate the output before using it in production or sharing logs externally.


## Third-party libraries

This project uses the following third-party libraries:

- **Picocli** – Command line parsing for Java  
Licensed under the Apache License, Version 2.0  
https://picocli.info

For advanced usage, benchmarks, design notes and full configuration, see [MANUAL.md](MANUAL.md)