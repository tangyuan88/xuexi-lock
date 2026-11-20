#!/usr/bin/env python3
"""
学习锁题库打包工具
把一个目录下的题目 JSON 文件打包成可导入的题库 zip。

用法：
    python pack_questions.py <题目目录> <输出zip> [--note "备注"]

题目目录中每个 <id>.json 是一个题目数组，脚本自动生成 index.json。
也可在题目目录放一个 index.json 手动指定 grade/subject/semester 等元信息。
"""

import argparse
import json
import os
import sys
import zipfile


def read_metadata(question_dir):
    """读取可选的 index.json 元信息，返回 {id: meta}"""
    path = os.path.join(question_dir, "index.json")
    if not os.path.exists(path):
        return {}
    try:
        with open(path, encoding="utf-8") as f:
            idx = json.load(f)
        return {p["id"]: p for p in idx.get("packs", [])}
    except (ValueError, KeyError):
        return {}


def parse_questions(path):
    """读取并校验题目文件，返回标准化后的题目列表"""
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    if not isinstance(data, list):
        raise ValueError("题目文件顶层必须是 JSON 数组")

    out = []
    for i, q in enumerate(data):
        stem = q.get("q") or q.get("question")
        options = q.get("options")
        if not options:
            options = [q.get(k) for k in ("optiona", "optionb", "optionc", "optiond")]
            options = [o for o in options if o]
        answer = q.get("answer")
        if isinstance(answer, str):
            answer = {"A": 0, "B": 1, "C": 2, "D": 3}.get(answer.upper(), -1)
        if not stem or len(options) < 2 or answer not in range(len(options)):
            print(f"  跳过第 {i + 1} 题：格式不完整", file=sys.stderr)
            continue
        item = {"q": stem, "options": options, "answer": answer}
        for src, dst in (("py", "py"), ("explain", "explain")):
            if q.get(src):
                item[dst] = q[src]
        out.append(item)
    return out


def main():
    ap = argparse.ArgumentParser(description="学习锁题库打包工具")
    ap.add_argument("question_dir", help="题目 JSON 所在目录")
    ap.add_argument("output_zip", help="输出的 zip 文件路径")
    ap.add_argument("--note", default="", help="索引备注")
    args = ap.parse_args()

    qdir = args.question_dir
    if not os.path.isdir(qdir):
        sys.exit(f"目录不存在: {qdir}")

    meta = read_metadata(qdir)
    packs = []
    files_to_add = []

    for name in sorted(os.listdir(qdir)):
        if not name.endswith(".json") or name == "index.json":
            continue
        pid = name[:-5]
        path = os.path.join(qdir, name)
        try:
            with open(path, encoding="utf-8") as f:
                probe = json.load(f)
            if not isinstance(probe, list):
                continue  # 非数组 JSON（如 legacy_index.json）是元信息文件，跳过
            questions = parse_questions(path)
        except (ValueError, OSError) as e:
            print(f"跳过 {name}: {e}", file=sys.stderr)
            continue
        if not questions:
            print(f"跳过 {name}: 没有有效题目", file=sys.stderr)
            continue

        m = meta.get(pid, {})
        pack = {
            "id": pid,
            "grade": m.get("grade", 0),
            "subject": m.get("subject", 0),
            "semester": m.get("semester", 0),
            "count": len(questions),
            "bytes": os.path.getsize(path),
        }
        packs.append(pack)
        files_to_add.append((name, json.dumps(questions, ensure_ascii=False)))

    if not packs:
        sys.exit("没有找到任何有效题目文件")

    index = {"version": 5, "rev": 1, "note": args.note, "packs": packs}
    with zipfile.ZipFile(args.output_zip, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("index.json", json.dumps(index, ensure_ascii=False, indent=2))
        for name, content in files_to_add:
            z.writestr(name, content)

    print(f"已打包 {len(packs)} 个题库包 -> {args.output_zip}")
    for p in packs:
        print(f"  - {p['id']}: {p['count']} 题")


if __name__ == "__main__":
    main()
