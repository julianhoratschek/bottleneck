import re
import subprocess
from pathlib import Path
from whisky import *
import argparse


def extract_obsidian_links(file_name: Path) -> list[str]:
    """Extracts the content of all obsidian links from obsidian lists
    :param file_name: File to read links from
    :return: List of strings of all obsidian links in lists"""
    return [it[1] for it in re.finditer(r"- \[\[([^]]+)]]", file_name.read_text("utf-8"))]


def read_whisky_data(file_name: Path) -> WhiskyBase:
    """Extracts whiskey data from a well-formed data file provided by file_name
    :param file_name: Path to the file to read
    :return: Whisky on success, otherwise WhiskyError"""
    if not file_name.is_file():
        return WhiskyError(file_name.stem, WhiskyError.ErrorType.FileNotFound)

    pattern: re.Pattern = re.compile(
        r"^\s*# (?P<name>.*)\s+(?:## Eigenschaften\s+)?"
        r"- \[(?P<chill_filtered>.?)] Kühlgefiltert\s+"
        r"- \[(?P<coloured>.)] Gefärbt\s+"
        r"- Typ: (?P<type>.*?)\s+"
        r"- Region: (?P<region>.*?)\s+"
        r"- Destillerie: (?:\[+[^|]+\|)?(?P<distillery>.*?)]*\s+"
        r"- Alter: (?P<age>.*?)\s+"
        r"- Stärke: (?P<abv>.*?)%.*?\s+"
        r"- Reifung: (?P<casks>.*)\s+", re.RegexFlag.MULTILINE)

    if data := pattern.search(file_name.read_text("utf-8")):
        return Whisky(**data.groupdict())

    return WhiskyError(file_name.stem, WhiskyError.ErrorType.NoPatternMatch)


def collect_whisky_data(vault_dir: Path, whisky_names: list[str]) -> list[WhiskyBase]:
    """Reads all files in whisky_names in vault_dir and tries to extract whisky data
    :param vault_dir: Path to obsidian vault containing files listed in whisky_names
    :param whisky_names: List of file names to read
    :return: List containing Whisky-instances for each successfully read dataset, WhiskyError-instances for all other"""
    return [read_whisky_data(vault_dir / f"{name}.md") for name in whisky_names]


def generate_tex_file(template_path: Path, output_path: Path, insert_text: str):
    """Copies content from template_path into output_path while replacing the first occurrence
    of '% bottleneck_insert' with insert_text"""
    output_path.write_text(
        template_path
            .read_text("utf-8")
            .replace("% bottleneck_insert", insert_text, 1),
        "utf-8"
    )


def generate_pdf(tex_file: Path):
    """Simply calls pdflatex with fitting parameters to generate pdf from previously generated tex file"""
    subprocess.run(["pdflatex", "-synctex=1", "-output-format=pdf",
                    f"-output-directory={tex_file.parent}", f"-aux-directory={tex_file.parent / 'aux'}",
                    f"-include-directory={Path(__file__).parent}", tex_file])


if __name__ == "__main__":

    # Setup argument parser

    parser = argparse.ArgumentParser(
        prog="bottleneck",
        description="Easily create beautiful pages for whisky tastings")

    parser.add_argument("filename")
    parser.add_argument("-v", "--vault",
                        nargs=1, type=Path, default=Path.home() / "Documents/vaults/whiskey/")
    parser.add_argument("-o", "--output",
                        nargs=1, type=Path, default=Path.cwd() / "whisky_tasting.tex")
    parser.add_argument("-t", "--tex-only",
                        action="store_true")

    args = parser.parse_args()

    # Read from input file

    tasting_file_path: Path = args.vault / args.filename
    print(f"Extracting data from: {tasting_file_path}")
    if not tasting_file_path.exists():
        print("[!!] The file does not exist")
        exit(0)

    whisky_link_list: list[str] = extract_obsidian_links(tasting_file_path)

    # Validate data

    if not whisky_link_list:
        print(f"[!!] No file links were found in {tasting_file_path}")
        exit(0)

    # Load from all found associated files

    print(f"Looking for relevant data")
    whisky_list: list[WhiskyBase] = collect_whisky_data(args.vault, whisky_link_list)

    print("Validating read data")
    if error_list := [str(w) for w in whisky_list if isinstance(w, WhiskyError)]:
        print("\n".join(error_list))
        exit(0)

    for w in whisky_list:
        print(f"\tFound: {w.name}")

    # Create TEX file

    print(f"Generating TEX file {args.output}")
    generate_tex_file(
        template_path=Path(__file__).parent / "template.tex",
        output_path=args.output,
        insert_text="\n\n".join([w.to_tex_str() for w in whisky_list if isinstance(w, Whisky)])
    )

    # Optionally create a PDF document

    if args.tex_only:
        exit(0)

    print("Generating PDF file")
    generate_pdf(args.output)



