# -*- coding: utf-8 -*-
"""扫描 Java 源文件中的 GBK->UTF-8 双重编码乱码（Mojibake）。

判定原理：双重编码乱码由「GBK 字节被按 UTF-8 解码」产生，
特征是大段连续 CJK 字符中夹杂 U+FFFD 或乱码高频字（锛/瀵/鏈/閴/鎴 等）。
单个生僻字可能是合法中文，故要求同一行内命中 >=2 个特征字才计为乱码行，
文件含 >=1 个乱码行即计入清单。
"""
import glob
import sys

SIGS = set("锛瀵鏈嶅潈璁閴鎴紩垚锟鏁粯寲鐢樻繃爜渶瘜鍝搧瀹屾參鎵棾鍖")

def scan(patterns):
    bad = []
    for pat in patterns:
        for f in glob.glob(pat, recursive=True):
            if 'target' in f:
                continue
            try:
                with open(f, encoding='utf-8') as fh:
                    lines = fh.readlines()
            except UnicodeDecodeError:
                bad.append((f, -1, 'DECODE_FAIL'))
                continue
            mojibake_lines = 0
            for line in lines:
                hit = sum(1 for ch in line if ch in SIGS)
                if hit >= 2 or '�' in line:
                    mojibake_lines += 1
            if mojibake_lines:
                bad.append((f, mojibake_lines, ''))
    return bad

if __name__ == '__main__':
    roots = sys.argv[1:] or ['dayan-server/**/*.java']
    bad = scan(roots)
    print(f'乱码文件数: {len(bad)}')
    for f, n, note in sorted(bad, key=lambda x: -x[1])[:80]:
        print(n, note, f)
