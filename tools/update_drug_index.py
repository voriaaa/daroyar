#!/usr/bin/env python3
"""Rebuild app/src/main/assets/drug_index.tsv from the public lists on darooyab.ir.

Only names are taken (generic names in Persian/English, the drug group, herbal plant names and
herbal product brand names); no descriptive text. Pages are fetched slowly (robots.txt allows
them). Usage:  python3 tools/update_drug_index.py [--cache DIR]
"""
import html
import re
import subprocess
import sys
import time
from pathlib import Path

BASE = "https://www.darooyab.ir"
OUT = Path(__file__).resolve().parent.parent / "app/src/main/assets/drug_index.tsv"
DELAY = 1.5

# Groups that people take at home on a schedule. Contrast media, gases, anaesthetics,
# disinfectants, excipients, vaccines, cosmetics and equipment are left out.
SKIP_GROUPS = {22, 23, 27, 30, 32, 35, 37, 38, 40, 41, 43, 45, 47, 48, 51, 53, 61, 62}


CACHE = None


def fetch(path):
    if CACHE:
        cached = CACHE / (re.sub(r"[^A-Za-z0-9]+", "_", path).strip("_") + ".html")
        if cached.exists():
            return cached.read_text(encoding="utf-8")
        data = fetch_live(path)
        cached.write_text(data, encoding="utf-8")
        return data
    return fetch_live(path)


def fetch_live(path):
    # curl, because the TLS of some Python builds on macOS drops this site's connections.
    for attempt in range(4):
        r = subprocess.run(
            ["curl", "-sS", "--fail", "-m", "120", "-A", "DaroYar index builder", BASE + path],
            capture_output=True,
        )
        time.sleep(DELAY * (attempt + 1))
        if r.returncode == 0:
            return r.stdout.decode("utf-8", "replace")
        print(f"  retry {path}: {r.stderr.decode().strip()}", file=sys.stderr)
    raise RuntimeError("could not fetch " + path)


def text(fragment):
    return re.sub(r"\s+", " ", re.sub(r"<[^>]+>", " ", html.unescape(fragment))).strip()


def cell_rows(page):
    for row in re.findall(r"<tr>(.*?)</tr>", page, re.S):
        cells = [text(c) for c in re.findall(r"<td[^>]*>(.*?)</td>", row, re.S)]
        if cells:
            yield cells


def generic_rows(page):
    """(fa, en) pairs from a group table: columns are #, image, Persian name, English name."""
    for cells in cell_rows(page):
        names = [c for c in cells[2:] if c and c != "بدون تصویر"]
        if len(names) >= 2:
            yield names[0], names[1]


FORM_WORDS = (
    "قرص", "کپسول", "شربت", "قطره", "پماد", "کرم", "ژل", "اسپری", "محلول", "ساشه", "پودر", "گرانول",
    "شیاف", "لوسیون", "روغن", "دمنوش", "عرق", "سوسپانسیون", "افروسنت", "جوشان", "نرم", "خوراکی",
    "موضعی", "واژینال", "دهانشویه", "دهان شویه", "عددی", "میلی", "گرم", "لیتر", "گرمی", "سی سی",
    "روکشدار", "روکش دار", "سافت", "سافتژل", "ژلاتینی", "ویال", "لینیمنت", "تابلت", "شامپو", "پچ",
)
# First words too common to stand for one brand on their own.
COMMON_FIRST = {"ویتامین", "مولتی", "کلسیم", "امگا", "گیاهی", "فورت", "پلاس", "طبیعی", "ترکیب", "عصاره", "هربال"}


def clean(name):
    s = re.sub(r"[0-9۰-۹٠-٩]+([.,/][0-9۰-۹]+)?", " ", name)
    for w in FORM_WORDS:
        s = re.sub(r"(^|\s)" + w + r"(?=\s|$)", " ", s)
    return re.sub(r"\s+", " ", s).strip(" -:()")


def brand_names(fa):
    """'قرص آلگوفیل 30 عددی' -> ['آلگوفیل']; 'کاپسیمان-کپسیمان' -> both spellings."""
    # Descriptions in parentheses ("(خلط آور-ضد سرفه)") are not part of the name.
    fa = re.sub(r"\([^)]*\)?", " ", fa)
    full = clean(fa)
    parts = [clean(p) for p in re.split(r"\s*-\s*", fa)] if "-" in fa else []
    return [n for n in dict.fromkeys([full] + parts) if len(n) >= 3]


def main():
    groups_page = fetch("/DrugGroups")
    names = {}
    for href, label in re.findall(r'<a[^>]+href="/DrugGroups/(\d+)"[^>]*>(.*?)</a>', groups_page, re.S):
        gid = int(href)
        name = text(label).split("/")[0].strip()
        if gid not in SKIP_GROUPS and name:
            names.setdefault(gid, name)
    groups = list(names.items())

    rows = {}
    for gid, gname in groups:
        print(f"group {gid} {gname}", file=sys.stderr)
        for fa, en in generic_rows(fetch(f"/DrugGroups/{gid}")):
            rows.setdefault((fa, en), ("G", gname))

    for fa, en in generic_rows(fetch("/DrugHerbal")):
        rows.setdefault((fa, en), ("P", "گیاهان دارویی"))

    brands = []
    for cells in cell_rows(fetch("/HerbalMedicines")):
        if len(cells) >= 5:
            names = brand_names(cells[3])
            if names:
                brands.append((names, cells[4]))

    # The brand's first word works as a short name when no other brand or generic shares it.
    generic_words = {w for fa, _ in rows for w in fa.split()}
    first_words = {}
    for names, _ in brands:
        first_words.setdefault(names[0].split()[0], set()).add(names[0])
    aliases = {}
    for names, maker in brands:
        alts = list(names[1:])
        first = names[0].split()[0]
        if (" " in names[0] and len(first) >= 5 and len(first_words[first]) == 1
                and first not in generic_words and first not in COMMON_FIRST):
            alts.append(first)
        if (names[0], "") not in rows:
            rows[(names[0], "")] = ("B", "فرآوردهٔ گیاهی: " + maker)
            aliases[(names[0], "")] = alts

    OUT.parent.mkdir(parents=True, exist_ok=True)
    with OUT.open("w", encoding="utf-8") as f:
        f.write("# kind\tfa\ten\tgroup\taliases   (G generic, P medicinal plant, B herbal brand; aliases split by |) — names from darooyab.ir\n")
        for (fa, en), (kind, group) in sorted(rows.items(), key=lambda r: (r[1][0], r[0][0])):
            alts = "|".join(aliases.get((fa, en), []))
            cols = [x.replace("\t", " ") for x in (kind, fa, en, group, alts)]
            f.write("\t".join(cols) + "\n")
    print(f"{len(rows)} names -> {OUT}", file=sys.stderr)


if __name__ == "__main__":
    if "--cache" in sys.argv:
        CACHE = Path(sys.argv[sys.argv.index("--cache") + 1])
        CACHE.mkdir(parents=True, exist_ok=True)
    main()
