#!/usr/bin/env python3
import sys, csv, re, urllib.request, urllib.parse, json, base64

def classify_smell(rule_key):
    if rule_key == "custom-java:ExcessiveNullCheck":
        return "Null Check"
    if rule_key == "custom-java:TypeChecking":
        return "Conditional Complexity"
    if rule_key == "java:S1067":
        return "Complicated Boolean Expression"
    if rule_key == "java:S2301":
        return "Flag Argument"
    return f"Other ({rule_key})"

RULES = "java:S1067,java:S2301,custom-java:ExcessiveNullCheck,custom-java:TypeChecking"

def fetch_all_issues(base_url, token, project_key):
    issues = []
    page = 1
    while True:
        params = {"componentKeys": project_key, "types": "CODE_SMELL",
                  "rules": RULES, "statuses": "OPEN,CONFIRMED,REOPENED",
                  "ps": 500, "p": page}
        url = f"{base_url}/api/issues/search?{urllib.parse.urlencode(params)}"
        req = urllib.request.Request(url)
        auth = base64.b64encode(f"{token}:".encode()).decode()
        req.add_header("Authorization", f"Basic {auth}")
        with urllib.request.urlopen(req) as resp:
            data = json.loads(resp.read().decode())
        page_issues = data.get("issues", [])
        issues.extend(page_issues)
        total = data.get("total", 0)
        if page * 500 >= total or not page_issues:
            break
        page += 1
    return issues

def extract_class_name(file_path):
    filename = file_path.rsplit("/", 1)[-1]
    if filename.endswith(".java"):
        filename = filename[:-5]
    return filename

METHOD_RE = re.compile(r'^\s*(public|private|protected|static|final|synchronized|abstract|\s)*[\w\<\>\[\]\., ]+\s+(\w+)\s*\([^;{]*\)\s*(\{|throws)')
CLASS_RE = re.compile(r'^\s*(public|private|protected|final|abstract|static|\s)*(class|interface|enum)\s+(\w+)')

def find_enclosing_method_and_class(local_file_path, line_no):
    try:
        with open(local_file_path, "r", errors="ignore") as f:
            lines = f.readlines()
    except FileNotFoundError:
        return "N/A", "N/A"
    if line_no is None or line_no < 1 or line_no > len(lines):
        return "N/A", "N/A"
    method = "N/A"
    cls = "N/A"
    for i in range(min(line_no, len(lines)) - 1, -1, -1):
        line = lines[i]
        if method == "N/A":
            m = METHOD_RE.match(line)
            if m and m.group(2) not in ("if", "for", "while", "switch", "catch"):
                method = m.group(2)
        if cls == "N/A":
            c = CLASS_RE.match(line)
            if c:
                cls = c.group(3)
        if method != "N/A" and cls != "N/A":
            break
    return method, cls

def main():
    if len(sys.argv) != 6:
        print("Usage: extract_smells.py <project_key> <sonar_url> <token> <local_repo_path> <output_csv>")
        sys.exit(1)
    project_key, base_url, token, repo_path, out_csv = sys.argv[1:6]
    print(f"Fetching issues for project: {project_key} ...")
    issues = fetch_all_issues(base_url, token, project_key)
    print(f"Total CODE_SMELL issues fetched: {len(issues)}")
    rows = []
    for idx, issue in enumerate(issues, start=1):
        component = issue.get("component", "")
        file_path = component.split(":", 1)[-1] if ":" in component else component
        line_no = issue.get("line")
        rule_key = issue.get("rule", "")
        smell = classify_smell(rule_key)
        severity = issue.get("severity", "N/A")
        message = issue.get("message", "")
        effort = issue.get("effort", "N/A")
        local_file = f"{repo_path.rstrip('/')}/{file_path}"
        method, cls_from_code = find_enclosing_method_and_class(local_file, line_no)
        class_name = cls_from_code if cls_from_code != "N/A" else extract_class_name(file_path)
        if smell == "Null Check":
            method = "N/A (class-level smell)"
        rows.append({"Sr.": idx, "File Path": file_path, "Class": class_name, "Method": method,
                     "Line Number": line_no if line_no is not None else "N/A", "Smell": smell,
                     "Severity": severity, "Rule ID": rule_key, "Message": message, "Effort": effort})
    with open(out_csv, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=["Sr.", "File Path", "Class", "Method", "Line Number", "Smell", "Severity", "Rule ID", "Message", "Effort"])
        writer.writeheader()
        writer.writerows(rows)
    print(f"Report written to: {out_csv}")
    print(f"Total rows: {len(rows)}")

if __name__ == "__main__":
    main()
