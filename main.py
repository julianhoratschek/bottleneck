import re
import subprocess
from pathlib import Path
from dataclasses import dataclass
from enum import Enum
import argparse


@dataclass
class WhiskyBase:
    name: str

@dataclass
class Whisky(WhiskyBase):
    type: str
    region: str
    distillery: str
    age: str
    abv: str
    casks: str
    chill_filtered: str
    coloured: str

    def is_chill_filtered(self) -> bool:
        return self.chill_filtered != " "

    def is_coloured(self) -> bool:
        return self.coloured != " "

    def to_tex_str(self) -> str:
        return (f"\\whiskey{{{self.name}}}"
                f"{{{self.type}}}"
                f"{{{self.abv.replace(',', '.')}}}"
                f"{{{'Kühlgefiltert' if self.is_chill_filtered() else 'Nicht kühlgefiltert'}, {'mit Zuckercouleur' if self.is_coloured() else 'ohne Farbstoff'}}}"
                f"{{{self.distillery.strip('[]')} ({self.region})}}"
                f"{{{self.age}}}"
                f"{{{self.casks}}}")

@dataclass
class WhiskyError(WhiskyBase):
    class ErrorType(Enum):
        FileNotFound = 0
        NoPatternMatch = 1

    error_type: ErrorType

    def _get_error_msg(self) -> str:
        match self.error_type:
            case WhiskyError.ErrorType.FileNotFound:
                return "File does not exist"
            case WhiskyError.ErrorType.NoPatternMatch:
                return "File does not match pattern"
            case _:
                return "Unknown Error"

    def __str__(self):
        return f"[!!] For dataset {self.name}: {self._get_error_msg()}"


def extract_obsidian_links(file_name: Path) -> list[str]:
    """Extracts the content of all obsidian links from obsidian lists
    :param file_name: File to read links from
    :return: List of strings of all obsidian links in lists"""
    return [it[1] for it in re.finditer(r"- \[\[([^]]+)]]", file_name.read_text("utf-8"))]


def read_whisky_data(file_name: Path) -> WhiskyBase:
    if not file_name.is_file():
        return WhiskyError(file_name.stem, WhiskyError.ErrorType.FileNotFound)

    pattern: re.Pattern = re.compile(
        r"# (?P<name>.*)\s+"
        r"- \[(?P<chill_filtered>.?)] Kühlgefiltert\s+"
        r"- \[(?P<coloured>.)] Gefärbt\s+"
        r"- Typ: (?P<type>.*?)\s+"
        r"- Region: (?P<region>.*?)\s+"
        r"- Destillerie: (?P<distillery>.*?)\s+"
        r"- Alter: (?P<age>.*?)\s+"
        r"- Stärke: (?P<abv>.*?)%.*?\s+"
        r"- Reifung: (?P<casks>.*)\s+")

    if data := pattern.search(file_name.read_text("utf-8")):
        return Whisky(**data.groupdict())

    return WhiskyError(file_name.stem, WhiskyError.ErrorType.NoPatternMatch)


def collect_whisky_data(vault_dir: Path, whisky_names: list[str]) -> list[WhiskyBase]:
    return [read_whisky_data(vault_dir / f"{name}.md") for name in whisky_names]


def generate_tex_file(template_path: Path, output_path: Path, insert_text: str):
    output_path.write_text(
        template_path
            .read_text("utf-8")
            .replace("% bottleneck_insert", insert_text, 1),
        "utf-8"
    )


def generate_pdf(tex_file: Path):
    subprocess.run(["pdflatex", "-synctex=1", "-output-format=pdf",
                    f"-output-directory={tex_file.parent}", f"-aux-directory={tex_file.parent / 'aux'}",
                    f"-include-directory={Path(__file__).parent}", tex_file])


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        prog="bottleneck",
        description="Easily create beautiful pages for whisky tastings")

    parser.add_argument("filename")
    parser.add_argument("-v", "--vault", nargs=1, type=Path, default=Path.home() / "Documents/vaults/whiskey/")
    parser.add_argument("-o", "--output", nargs=1, type=Path, default=Path.cwd() / "whisky_tasting.tex")
    parser.add_argument("-t", "--tex-only", action="store_true")

    args = parser.parse_args()

    tasting_file_path: Path = args.vault / args.filename
    print(f"Extracting data from: {tasting_file_path}")
    if not tasting_file_path.exists():
        print("[!!] The file does not exist")
        exit(0)

    whisky_link_list: list[str] = extract_obsidian_links(tasting_file_path)

    if not whisky_link_list:
        print(f"[!!] No file links were found in {tasting_file_path}")
        exit(0)

    print(f"Looking for relevant data")
    whisky_list: list[WhiskyBase] = collect_whisky_data(args.vault, whisky_link_list)

    print("Validating read data")
    if error_list := [str(w) for w in whisky_list if isinstance(w, WhiskyError)]:
        print("\n".join(error_list))
        exit(0)

    for w in whisky_list:
        print(f"\tFound: {w.name}")

    print(f"Generating TEX file {args.output}")
    generate_tex_file(
        template_path=Path(__file__).parent / "template.tex",
        output_path=args.output,
        insert_text="\n\n".join([w.to_tex_str() for w in whisky_list if isinstance(w, Whisky)])
    )

    if args.tex_only:
        exit(0)

    print("Generating PDF file")
    generate_pdf(args.output)



